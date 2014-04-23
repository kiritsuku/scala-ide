package org.scalaide.core.ui.completion

import org.scalaide.core.completion.ScalaCompletions
import org.scalaide.core.ui.CompilerSupport
import org.scalaide.core.ui.TextEditTests
import org.scalaide.util.internal.ScalaWordFinder
import org.scalaide.core.internal.jdt.model.ScalaCompilationUnit
import org.junit.After
import scala.reflect.internal.util.SourceFile

/**
 * This provides a test suite for the code completion functionality.
 * It can not only find out which completion exists, it also checks if the source
 * file after the insertion of a completion is inserted is correct.
 *
 * It can also handle Eclipse linked mode model. To depict such a model in the test
 * simply surround the identifiers that should be considered by the linked model
 * with [[ and ]]. The cursor is always represented by a ^.
 */
abstract class CompletionTests extends TextEditTests with CompilerSupport {

  /**
   * These are all the possible options that are considered by the test suite:
   *
   * @param completionToApply
   *        The completion that should be applied to the document
   * @param enableOverwrite
   *        If `true` the completion overwrite feature is enabled
   * @param expectedCompletions
   *        All the completions that are _at least_ expected to be found. If `Nil`
   *        this option can be seen as not considered by the test.
   * @param expectedNumberOfCompletions
   *        The number of completions that are expected to be found. A negative
   *        value means that this option is not considered by the test.
   */
  case class Completion(
      completionToApply: String,
      enableOverwrite: Boolean = false,
      expectedCompletions: Seq[String] = Nil,
      expectedNumberOfCompletions: Int = -1)
        extends Operation {

    def execute() = {
      val r = ScalaWordFinder.findWord(doc, caretOffset)

      val unit = mkScalaCompilationUnit(doc.get())
      val src = unit.sourceFile()
      val completions = new ScalaCompletions().findCompletions(r)(caretOffset, unit)(src, compiler)
      val completion = completions.find(_.display == completionToApply)

      val missingCompletions = expectedCompletions.filter(c => !completions.exists(_.display == c))
      if (missingCompletions.nonEmpty)
        throw new IllegalArgumentException(s"the following completions do not exist:\n\t${missingCompletions.mkString("\n\t")}")

      lazy val completionList = completions.sortBy(-_.relevance).map(_.display).mkString("\n\t", "\n\t", "")

      if (expectedNumberOfCompletions >= 0
          && completions.size != expectedNumberOfCompletions) {
        throw new IllegalArgumentException(
            s"There were '$expectedNumberOfCompletions' completions expected, but '${completions.size}' found, namely:$completionList")
      }

      completion.fold(
          throw new IllegalArgumentException(
              s"the completion '$completionToApply' does not exist, but:$completionList")) {
        completion => completion.applyCompletionToDocument(doc, unit, caretOffset, enableOverwrite) foreach {
          case (cursorPos, applyLinkedMode) =>
            if (!applyLinkedMode)
              caretOffset = cursorPos
            else {
              val groups = completion.linkedModeGroups.sortBy(-_._1)
              val cursorOffset = groups.takeWhile(_._1 < cursorPos).size*4

              groups foreach {
                case (offset, length) =>
                  doc.replace(offset+length, 0, "]]")
                  doc.replace(offset, 0, "[[")
              }
              caretOffset = cursorPos + cursorOffset
            }
        }
      }
    }
  }
}