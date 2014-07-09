package org.scalaide.core.internal.extensions

import java.net.URLClassLoader
import java.io.File
import scala.tools.nsc.interactive.Global
import org.scalaide.core.compiler.ScalaPresentationCompiler
import org.scalaide.extensions._
import org.scalaide.core.text.Document
import org.scalaide.util.internal.eclipse.EditorUtils

object ExtensionBuilder {
  import scala.reflect.runtime.universe.runtimeMirror
  import scala.tools.reflect.ToolBox

  val path = XRuntime.classpathValuesToEnrich()

  val cl = new URLClassLoader(
      path.map(p => new File(p).toURI().toURL()).toArray,
      getClass().getClassLoader())

  private val tb = runtimeMirror(cl).mkToolBox()
  import tb._, u._

  def startup(): Unit = ()

  private def findExtensions(t: Tree): Seq[Extension] = {
    var exts = Seq[Extension]()
    new Traverser {
      override def traverse(t: Tree): Unit = {
        t match {
          case cd @ ClassDef(_, _, _, Template(parents, _, _)) =>

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

  def createExtension(ext: String): Seq[ScalaIdeExtension] = {
    val name = createInternalExtensionName()
    val extension = parse(s"""
      object $name {
        $ext
      }""")
    val exts =
      try findExtensions(typecheck(extension))
      catch {
        case e: Exception =>
          e.printStackTrace()
          Seq()
      }

    // TODO
    println("--- extensions")
    exts foreach println

    exts flatMap {
      case Extension(DocumentSupportBuilder, cls) =>
        Seq(DocumentSupportBuilder.create(extension, name)(cls))
      case Extension(CompilerSupportBuilder, cls) =>
        EditorUtils.withScalaSourceFileAndSelection { (ssf, sel) =>
          ssf.withSourceFile { (file, compiler) =>
            CompilerSupportBuilder.create(extension, name)(compiler, cls)
          }
        }.toSeq
    }
  }

  private case class Extension(builder: ExtensionBuilder, ext: ClassDef)

  trait ExtensionBuilder {

  }

  case object DocumentSupportBuilder extends ExtensionBuilder {
    def create(extension: Tree, extName: String)(extTree: ClassDef): DocumentSupport = {
      val instance = q"""
        $extension

        new ${TermName(extName)}.${extTree.name}
      """
      eval(instance).asInstanceOf[DocumentSupport]
    }
  }

  case object CompilerSupportBuilder extends ExtensionBuilder {
    def create(extension: Tree, extName: String)(compiler: ScalaPresentationCompiler, extTree: ClassDef): CompilerSupport = {
      val compilerType = typeOf[Global]
      val compilerSupportType = typeOf[CompilerSupport]
      val instance = q"""
        $extension

        def create(c: $compilerType): $compilerSupportType =
          new ${TermName(extName)}.${extTree.name} {
            override val global: $compilerType = c
          }

        create _
      """
      eval(instance).asInstanceOf[Global => CompilerSupport](compiler)
    }
  }

}