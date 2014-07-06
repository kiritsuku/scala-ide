package org.scalaide.core.internal.extensions

import java.net.URLClassLoader
import java.io.File
import scala.tools.nsc.interactive.Global
import org.scalaide.core.compiler.ScalaPresentationCompiler
import org.scalaide.extensions._
import org.scalaide.core.text.Document

object ExtensionBuilder {
  import scala.reflect.runtime.universe.runtimeMirror
  import scala.tools.reflect.ToolBox

  val path = XRuntime.classpathValuesToEnrich()

  val cl = new URLClassLoader(
      path.map(p => new File(p).toURI().toURL()).toArray,
      getClass().getClassLoader())

  private val tb = runtimeMirror(cl).mkToolBox()
  import tb._, u._

  private def findSaveAction(t: Tree): Option[ClassDef] = {
    var ret: ClassDef = null
    new Traverser {
      override def traverse(t: Tree): Unit = {
        t match {
          case c: ClassDef => ret = c
          case _ => super.traverse(t)
        }
      }
    }.traverse(t)
    Option(ret)
  }

  def createExtensions(compiler: ScalaPresentationCompiler, exts: Seq[String]): Seq[ScalaIdeExtension] = {
    exts map { ext =>
      val extName = "Extension" + System.nanoTime()
      val extension = parse(s"""
        object $extName {
          $ext
        }""")
      val saveAction = findSaveAction(extension).get
//      val compilerType = typeOf[Global]
//      val saveActionType = typeOf[ScalaIdeExtension]
      val instance = q"""
        $extension

        new ${TermName(extName)}.${saveAction.name}
      """
      println(instance)
      eval(instance).asInstanceOf[ScalaIdeExtension]
//      val instance = q"""
//        $extension
//
//        def create(c: $compilerType): $saveActionType =
//          new ${TermName(extName)}.${saveAction.name} {
//            override val global: $compilerType = c
//          }
//
//        create _
//      """
//      println(instance)
//      eval(instance).asInstanceOf[Global => ScalaIdeExtension](compiler)
    }
  }
}