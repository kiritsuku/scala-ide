package org.scalaide.core.internal.extensions

import java.net.URLClassLoader
import java.io.File
import scala.tools.nsc.interactive.Global
import org.scalaide.core.compiler.ScalaPresentationCompiler
import org.scalaide.extensions._
import org.scalaide.core.text.Document
import org.scalaide.util.internal.eclipse.EditorUtils
import scala.tools.refactoring.common.Selections
import scala.reflect.internal.util.SourceFile
import scala.tools.reflect.ToolBox
import org.scalaide.logging.HasLogger

object ExtensionBuilder extends HasLogger {
  import scala.reflect.runtime.universe.runtimeMirror

  val path = XRuntime.classpathValuesToEnrich()

  val cl = new URLClassLoader(
      path.map(p => new File(p).toURI().toURL()).toArray,
      getClass().getClassLoader())

  private[extensions] val toolBox = runtimeMirror(cl).mkToolBox()
  import toolBox._, u._

  def startup(): Unit = ()

  private def findExtensions(t: Tree, extFilter: Seq[String]): Seq[Extension] = {
    var exts = Seq[Extension]()
    new Traverser {
      override def traverse(t: Tree): Unit = {
        t match {
          case cd @ ClassDef(_, _, _, Template(parents, _, _)) =>
            val strs = parents.map(_.toString())
            strs.exists(extFilter.contains)

            if (parents.exists(_.toString() == "org.scalaide.extensions.DocumentSupport"))
              exts +:= Extension(DocumentSupportBuilder, cd)
            else if (parents.exists(_.toString() == "org.scalaide.extensions.CompilerSupport"))
              exts +:= Extension(CompilerSupportBuilder, cd)

//            parents foreach {
//              case tt: TypeTree =>
//                Option(tt.original) foreach { o =>
//                  val ssat = typeOf[SaveAction]
//                  val sym = o.symbol
//                  println(ssat.typeSymbol == sym)
//                  println(sym.asType.toType <:< ssat)
//                }
//              case _ =>
//            }
          case _ =>
        }
        super.traverse(t)
      }
    }.traverse(t)
    exts
  }

  private def createInternalExtensionName(): String =
    "Extension" + System.nanoTime()

  def createSaveAction(ext: String): Seq[ExtensionBuilder#Creator] = {
    val name = createInternalExtensionName()
    val extension = parse(s"""
      object $name {
        $ext
      }""")
    val cls = Seq(typeOf[SaveAction].typeSymbol.asClass.fullName)
    val exts =
      try findExtensions(typecheck(extension), cls)
      catch {
        case e @ (_: Exception | _: AssertionError) =>
          logger.error("Error occurred while typechecking save action.", e)
          Seq()
      }

    // TODO
    println("--- extensions")
    exts foreach println

    try
      exts map {
        case Extension(DocumentSupportBuilder, cls) =>
          import DocumentSupportBuilder._
          new DocumentSupportCreator(name, build(extension, name, cls))
        case Extension(CompilerSupportBuilder, cls) =>
          import CompilerSupportBuilder._
          new CompilerSupportCreator(name, build(extension, name, cls))
      }
    catch {
      case e @ (_: Exception | _: AssertionError) =>
        logger.error("Error occurred while building save action.", e)
        Seq()
    }

  }

//  def createExtension(ext: String): Seq[ExtensionBuilder#Creator] = {
//    val name = createInternalExtensionName()
//    val extension = parse(s"""
//      object $name {
//        $ext
//      }""")
//    val exts =
//      try findExtensions(typecheck(extension))
//      catch {
//        case e: Exception =>
//          e.printStackTrace()
//          Seq()
//      }
//
//    // TODO
//    println("--- extensions")
//    exts foreach println
//
//    exts map {
//      case Extension(DocumentSupportBuilder, cls) =>
//        import DocumentSupportBuilder._
//        new DocumentSupportCreator(build(extension, name, cls))
//      case Extension(CompilerSupportBuilder, cls) =>
//        import CompilerSupportBuilder._
//        new CompilerSupportCreator(build(extension, name, cls))
//    }
//  }

  private case class Extension(builder: ExtensionBuilder, ext: ClassDef)
}

trait ExtensionBuilder {

  val toolBox = ExtensionBuilder.toolBox
  import toolBox._, u._

  type Builder

  trait Creator

  protected def createType(typeCreator: Tree): Builder =
    eval(typeCreator).asInstanceOf[Builder]
}

case object DocumentSupportBuilder extends ExtensionBuilder {
  import toolBox._, u._

  private val documentSupportType = typeOf[DocumentSupport]
  private val documentType = typeOf[Document]

  override type Builder = Document => DocumentSupport

  class DocumentSupportCreator(
      val extensionName: String,
      private val b: Builder) extends Creator {
    def create(document: Document): DocumentSupport =
      b(document)
  }

  def build(extension: Tree, extName: String, extTree: ClassDef): Builder = {
    val instance = q"""
      $extension

      def create(doc: $documentType): $documentSupportType =
        new ${TermName(extName)}.${extTree.name} {
          override val document: $documentType = doc
        }

      create _
    """
    createType(instance)
  }
}

case object CompilerSupportBuilder extends ExtensionBuilder {
  import toolBox._, u._

  private val compilerType = typeOf[ScalaPresentationCompiler]
  private val compilerSupportType = typeOf[CompilerSupport]
//  private val selectionType = typeOf[Selections#Selection]
  private val sourceFileType = typeOf[SourceFile]
  private val treeType = typeOf[ScalaPresentationCompiler#Tree]

  override type Builder = (ScalaPresentationCompiler, ScalaPresentationCompiler#Tree, SourceFile, Int, Int) => CompilerSupport

  class CompilerSupportCreator(
      val extensionName: String,
      private val b: Builder) extends Creator {
    def create(
        compiler: ScalaPresentationCompiler)(
        tree: compiler.Tree,
        sourceFile: SourceFile,
        selectionStart: Int,
        selectionEnd: Int)
        : CompilerSupport =
      b(compiler, tree, sourceFile, selectionStart, selectionEnd)
  }

  def build(extension: Tree, extName: String, extTree: ClassDef): Builder = {
    val instance = q"""
      $extension

      def create(
          c: $compilerType,
          t: $treeType,
          sf: $sourceFileType,
          selectionStart: Int,
          selectionEnd: Int)
          : $compilerSupportType = {

        new ${TermName(extName)}.${extTree.name} {
          override val global: $compilerType = c
          override val sourceFile: $sourceFileType = sf
          override val selection = new FileSelection(
            sf.file, t.asInstanceOf[global.Tree], selectionStart, selectionEnd)
        }
      }

      create _
    """
    createType(instance)
  }
}