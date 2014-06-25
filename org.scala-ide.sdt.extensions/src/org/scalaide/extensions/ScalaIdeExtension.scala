package org.scalaide.extensions

import org.scalaide.core.text.Document
import org.scalaide.core.text.Change
import scala.tools.refactoring.common.Selections
import scala.tools.refactoring.common.InteractiveScalaCompiler
import scala.reflect.internal.util.SourceFile
import scala.tools.refactoring.transformation.Transformations
import scala.tools.refactoring.transformation.TreeFactory
import scala.tools.refactoring.common.PimpedTrees
import scala.tools.refactoring.transformation.TreeTransformations
import scala.tools.refactoring.sourcegen.SourceGenerator
import scala.tools.refactoring.common.SilentTracing
import scala.tools.refactoring.sourcegen.Fragment
import scala.tools.refactoring.sourcegen.CommentsUtils
import org.scalaide.core.text.Add
import org.scalaide.core.text.Replace
import org.scalaide.core.text.TextChange

/**
 * Parameterization on:
 * - how to invoke extension
 *   - implicitly on save
 *   - implicitly on text edit
 *   - explicitly invoked
 *   - automatically while program runs in debug mode
 * - the result of the extension
 *   - text changes for auto edits, save actions, refactorings etc.
 *   - warnings for linter
 */

trait ScalaIdeExtension

trait DocumentSupport extends ScalaIdeExtension {

  /**
   *
   * '''ATTENTION''':
   * Do not implement this value by any means! It is automatically implemented
   * by the IDE.
   */
  val document: Document

  def perform(): Seq[Change]
}

trait CompilerSupport
    extends ScalaIdeExtension
    with Selections
    with InteractiveScalaCompiler
    with Transformations
    with TreeTransformations
    with PimpedTrees
    with TreeFactory
    with SourceGenerator
    with SilentTracing {

  import global._

  val selection: Selection
  val sourceFile: SourceFile

  def perform(): Seq[Change]

  def transformFile(trans: Transformation[Tree, Tree]): Seq[Change] =
    refactor(trans(abstractFileToTree(sourceFile.file)).toList)

  def refactor(changed: List[Tree]): Seq[Change] = context("main") {
    createChanges2(changed) map minimizeChange
  }

  private def createChanges2(ts: List[Tree]): Seq[Change] = context("Create changes") {
    val fragments = generateFragmentsFromTrees2(ts) map {
      case (file, tree, range, fragment) =>

        /*
         * We need to fix the end position because the Scala compiler often doesn't
         * have correct ranges for top-level trees.
         * */

        def replacesCuRoot = {
          compilationUnitOfFile(file) exists (_.body.samePos(tree.pos))
        }

        lazy val trailingSrc = {
          val src = range.source.content.slice(range.end, range.source.length)
          CommentsUtils.stripComment(src)
        }

        def hasTrailingBraceAndSomething = {
          trailingSrc.contains('}') && trailingSrc.length > 1
        }

        val actualEnd = {
          if(replacesCuRoot && hasTrailingBraceAndSomething) {
            // The RangePosition ends before the } that closes the top-level
            // tree, so we include this additional offset in the source code
            // the change replaces, otherwise we sometimes get stray } after
            // a refactoring.
            val offsetBelongingToCuRoot = trailingSrc.takeWhile(_ != '}').size + 1
            range.end + offsetBelongingToCuRoot
          } else {
            endPositionAtEndOfSourceFile(range)
          }
        }

        Replace(range.start, actualEnd, fragment.center.asText)
    }
    fragments.toSeq
  }

  /**
   * Makes a generated change as small as possible by eliminating the
   * common pre- and suffix between the change and the source file.
   */
  private def minimizeChange(change: Change): Change = change match {
    case TextChange(from, to, changeText) =>

      def commonPrefixLength(s1: Seq[Char], s2: Seq[Char]) =
        (s1 zip s2 takeWhile scala.Function.tupled(_ == _)).length

      val original = sourceFile.content.subSequence(from, to).toString
      val replacement = changeText

      val commonStart = commonPrefixLength(original, replacement)
      val commonEnd = commonPrefixLength(original.substring(commonStart).reverse, replacement.substring(commonStart).reverse)

      val minimizedChangeText = changeText.subSequence(commonStart, changeText.length - commonEnd).toString
      TextChange(from + commonStart, to - commonEnd, minimizedChangeText)
  }

  private def generate2(tree: Tree, changeset: ChangeSet = AllTreesHaveChanged, sourceFile: Option[SourceFile]): Fragment = {

    val initialIndentation = if(tree.hasExistingCode) indentationString(tree) else ""
    val in = new Indentation(defaultIndentationStep, initialIndentation)

    print(tree, PrintingContext(in, changeset, tree, sourceFile))
  }

  private def generateFragmentsFromTrees2(ts: List[Tree]): List[(tools.nsc.io.AbstractFile, Tree, Position, Fragment)] = {

    if(ts.exists(_.pos == NoPosition)) {
      throw new IllegalArgumentException("Top-level trees cannot have a NoPosition because we need to get the source file: "+ ts.filter(_.pos == NoPosition).mkString(", "))
    }

    val changesByFile = ts groupBy (_.pos.source)

    val topLevelTreesByFile = changesByFile map {
      case (source, ts) => (source, findTopLevelTrees(ts))
    }

    val changesPerFile = topLevelTreesByFile flatMap {
      case (source, ts) => ts flatMap findAllChangedTrees map {
        case (topLevel, replaceRange, changes) =>
          (source, replaceRange, topLevel, changes)
      }
    }

    if(changesPerFile.isEmpty) {
      trace("No changes were found.")
    }

    changesPerFile.map {
      case (source, replaceRange, tree, changes) =>
        trace("Creating code for %s. %d tree(s) in changeset.", getSimpleClassName(tree), changes.size)
        val f = generate2(tree, new ChangeSet {
          def hasChanged(t: Tree) = changes.exists {
            /*
             * NameTrees have a position that is calculated from their name's length, because their position
             * is not part of the AST. So if we rename a NameTree, the end-position changes whenever the
             * length of the name changes. To be able to find the original of a renamed tree, we just compare
             * names, position start and source.
             *
             * */
            case o: NameTree if o.pos.isRange => t match {
              case t: NameTree if t.pos.isRange =>
                o.nameString == t.nameString && o.pos.source == t.pos.source && o.pos.start == t.pos.start
              case _ =>
                false
            }
            case o => o samePosAndType t
          }
        }, Some(source))

        trace("Change: %s", f.center.asText)

        val pos = adjustedStartPosForSourceExtraction(tree, replaceRange)

        // In some cases the replacement source starts with a space, and the replacing range
        // also has a space leading up to it. In that case, we drop the leading space from the
        // replacement fragment.
        val replacementWithoutLeadingDuplicateSpace = {
          if(pos.start > 0 && source.content(pos.start - 1) == ' ' && f.center.matches("(?ms) [^ ].*")) {
            Fragment(f.center.asText.tail)
          } else {
            f
          }
        }
        (source.file, tree, pos, replacementWithoutLeadingDuplicateSpace)
    }.toList
  }

  private def getSimpleClassName(o: Object): String = try {
    o.getClass.getSimpleName
  } catch {
    case _: InternalError | _: NoClassDefFoundError => o.getClass.getName
  }
}