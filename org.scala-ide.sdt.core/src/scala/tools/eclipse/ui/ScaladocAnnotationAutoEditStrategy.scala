package scala.tools.eclipse.ui

import scala.tools.eclipse.InteractiveCompilationUnit
import scala.reflect.internal.util.SourceFile
import scala.reflect.internal.Flags
import scala.tools.eclipse.ScalaPresentationCompiler
import org.eclipse.ui.texteditor.ITextEditor
import scala.tools.eclipse.util.EditorUtils
import scala.tools.eclipse.refactoring.EditorHelpers
import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy
import scala.tools.eclipse.logging.HasLogger
import org.eclipse.jface.text.DocumentCommand
import org.eclipse.jface.text.IDocument

trait ScaladocAnnotationAutoEditStrategy extends HasLogger {

  import ScaladocAnnotationAutoEditStrategy._

//  def x(icu: InteractiveCompilationUnit, index: Int): Option[List[ScaladocTag]] = {
  def findTags(index: Int, isCommentClosed: Boolean = true): Option[List[ScaladocTag]] = {
    EditorHelpers.withScalaFileAndSelection { (icu, _) =>
      // check if there is already a Scaladoc comment
      icu.withSourceFile({ (sourceFile, compiler) =>
        findStartOfExpr(sourceFile.content, index, isCommentClosed) flatMap { index =>
          logger.debug(new StringBuilder(sourceFile.content.mkString).insert(index, "^").subSequence(index-50, index+50).toString())
          import compiler.{ sourceFile => _, _ }

          val pos = compiler.rangePos(sourceFile, index, index, index + 1)

          val response = new Response[Tree]
          askTypeAt(pos, response)
          val typed = response.get

          val tags = askOption { () =>
            typed.left.toOption map findTagsInTree(compiler)
          }
          tags.flatten
        }
      })(None)
    }
  }

  private def findTagsInTree(compiler: ScalaPresentationCompiler)(tree: compiler.Tree): List[ScaladocTag] = {
    import compiler._
    tree match {
      case DefDef(_, _, tparams, paramss, ret, _) =>
        val paramTags = paramss flatMap { _ map (t => ParamTag(t.name.toString)) }
        val tparamTags = tparams map (t => TParamTag(t.name.toString))
        val retTag = if (ret.tpe =:= typeOf[Unit]) Nil else List(RetTag)
        paramTags ++ tparamTags ++ retTag

      case ClassDef(_, _, tparams, impl) =>
        val paramTags = impl.body collect {
          case ValDef(mods, name, _, _) if mods.hasFlag(Flags.PARAMACCESSOR) =>
            ParamTag(name.toString)
        }
        val tparamTags = tparams map (t => TParamTag(t.name.toString))
        List(CtorTag) ++ paramTags ++ tparamTags

      case TypeDef(_, _, tparams, _) =>
        tparams map (t => TParamTag(t.name.toString))

      case _ =>
        Nil
    }
  }

  private def findStartOfExpr(content: Array[Char], index: Int, isCommentClosed: Boolean): Option[Int] = {
    def isWhitespaceAt(i: Int) =
      " \t\n\r" contains content(i)

    def isCommentEndAt(i: Int) =
      i < content.length - 1 && content(i) == '*' && content(i + 1) == '/'

    def isScaladocStartAt(i: Int) =
      i < content.length - 2 && content(i) == '/' && content(i + 1) == '*' && content(i + 2) == '*'

    def eatComment(i: Int): Option[Int] =
      if (i >= content.length) None
      else if (isCommentEndAt(i)) Some(i + 2)
      else eatComment(i + 1)

    def eatWhitespace(i: Int): Option[Int] =
      if (i >= content.length) None
      else if (isWhitespaceAt(i)) eatWhitespace(i + 1)
      else Some(i)

    for {
      i <- if (isCommentClosed) eatComment(index) else Some(index)
      j <- eatWhitespace(i)
      // eat single and multi line comments here
      if !isScaladocStartAt(j)
    } yield j
  }
}

object ScaladocAnnotationAutoEditStrategy {

  sealed abstract class ScaladocTag(tagName: String) {
    def name: String
    override def toString = "@" + tagName + (if (name.isEmpty) "" else " " + name)
  }
  case class ParamTag(name: String) extends ScaladocTag("param")
  case class TParamTag(name: String) extends ScaladocTag("tparam")
  case object RetTag extends ScaladocTag("return") {
    val name = ""
  }
  case object CtorTag extends ScaladocTag("constructor") {
    val name = ""
  }

  def insertAtIndex/*(textEditor: ITextEditor, index: Int)*/ {
    println("<<<<<<<")
    EditorHelpers.withCurrentEditor{ e =>
      println(">>>>>>"+e.getViewer.getDocument())

      None
    }
//    EditorUtils.getEditorCompilationUnit(textEditor) match {
//      case Some(icu) =>
//      case None =>
//    }
  }
}