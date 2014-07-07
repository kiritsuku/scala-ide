package org.scalaide.core.internal.extensions

import java.io.File

import scala.tools.nsc.CompilerCommand
import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.StoreReporter

import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.jdt.core.ICompilationUnit
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility
import org.eclipse.jface.text.ITextSelection
import org.eclipse.ui.PlatformUI
import org.scalaide.core.ScalaPlugin
import org.scalaide.core.internal.project.ScalaProject
import org.scalaide.extensions.SimpleSaveAction
import org.scalaide.logging.HasLogger
import org.scalaide.util.internal.eclipse.EditorUtils

object XRuntime extends AnyRef with HasLogger {

  val ProjectName = "extide"

  def loadSaveActions(): Seq[SimpleSaveAction] = {
    try {
      val saveActions = projectByName(ProjectName) map projectAsScalaProject flatMap { sp =>
        EditorUtils.withScalaSourceFileAndSelection { (ssf, sel) =>
          ssf.withSourceFile { (file, compiler) =>
            val sources = sp.allSourceFiles().toSeq map fromFile
            val saveActions = ExtensionBuilder.createExtensions(compiler, sources)
            println("all save actions: " + saveActions)
            saveActions.asInstanceOf[Seq[SimpleSaveAction]]
          }
        }
      }
      saveActions.getOrElse(Seq())
    } catch {
      case e: Exception =>
        logger.error("error in save actions", e)
        Nil
    }
  }

  def fromFile(path: IFile): String = {
    val s = io.Source.fromFile(path.getLocationURI())
    val ret = s.mkString
    s.close()
    ret
  }

  def classpathValuesToEnrich(): Seq[String] = Seq(
    //        "/home/antoras/Software/scala-eclipse/plugins/org.scala-lang.scala-library_2.11.0.v20140324-160523-4aa58d48d2.jar",
//        "/home/antoras/Software/scala-eclipse/plugins/org.scala-lang.scala-reflect_2.11.2.v20140529-233818-da2896c4e5.jar"
        "scala/build/quick/classes/reflect",
        "scala/build/quick/classes/compiler",
//        "scala/build/quick/classes/asm",
        "scala/build/quick/classes/interactive",
//        "scala-ide/org.scala-ide.sdt.core/target/classes",
        "scala-ide/org.scala-ide.sdt.extensions/bin",
        "scala-refactoring/org.scala-refactoring.library/bin"
        ).map("/home/antoras/dev/scala/" + _)
//        :+ "/home/antoras/Software/scala-eclipse/plugins/org.scala-ide.sbt.compiler.interface_0.13.2.local-20140530-1553.jar"
//        :+ "/home/antoras/Software/scala-eclipse/plugins/org.scala-ide.sbt.full.library_0.13.2.local-2_11-20140530-1553.jar"

  def enrichClasspath(settings: Settings): Unit = {
    addToClasspath(settings, classpathValuesToEnrich())
  }

  def addToClasspath(settings: Settings, cps: Seq[String]): Unit = {
    var cp = settings.classpath.value

    for (p <- cps if !cp.contains(p))
      cp = p + File.pathSeparator + cp

    settings.classpath.value = cp
  }

  def projectAsScalaProject(p: IProject): ScalaProject =
    ScalaPlugin.plugin.getScalaProject(p)

  def projectAsJavaProject(p: IProject): Option[IJavaProject] =
    if (p.isOpen() && p.hasNature(JavaCore.NATURE_ID))
      Some(JavaCore.create(p))
    else
      None

  def projectByName(name: String): Option[IProject] = {
    val p = ResourcesPlugin.getWorkspace().getRoot().getProject(name)
    if (p.exists()) Some(p) else None
  }

  /**
   * Returns the selection of the current active editor if its underlying
   * compilation unit is equal to `cu`.
   */
  def initialSelection(cu: ICompilationUnit): Option[ITextSelection] = {
    val activeEditor = activePage.getActiveEditor()
    val activeCu = EditorUtility.getEditorInputJavaElement(activeEditor, /*primaryOnly*/ false)

    if (cu != activeCu)
      None
    else
      activeEditor.getSite().getSelectionProvider().getSelection() match {
        case ts: ITextSelection => Some(ts)
        case _ => None
      }
  }

  def openEditors =
    activePage.getEditorReferences()

  def activePage =
    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
}

class Compiler(p: ScalaProject) extends AnyRef with HasLogger {
  import XRuntime._

  private val reporter = new StoreReporter

  private val settings = {
    val settings = ScalaPlugin.defaultScalaSettings()
    p.initializeCompilerSettings(settings, _ => true)
    p.outputFolderLocations.headOption foreach { bin =>
      settings.d.value = bin.toOSString()
    }
    enrichClasspath(settings)
    settings
  }

  private val compiler = new Global(settings)

  def execute(): Unit = synchronized {
    val files = compileFiles()

    if (!reporter.hasErrors)
      run(files)
    else {
      println("Errors during compilation of save actions:")
      reporter.infos foreach println
      reporter.reset()
    }
  }

  def run(files: Seq[String]): Unit = {
//    val ps = p.allSourceFiles().map(_.getProjectRelativePath())
    val cus = p.javaProject.getPackageFragments().flatMap(_.getCompilationUnits())
    println(cus.toList)
//    val folder = p.underlying.getFolder("src")
//    val root = p.javaProject.getPackageFragmentRoot(folder)
//    val pkg = root.getPackageFragment("")
//    val cus = pkg.getCompilationUnits()
  }

  def compileFiles(): Seq[String] = {
    val srcs = p.allSourceFiles() map (_.getLocation().toOSString())
    val cmd = new CompilerCommand(srcs.toList, settings)
    val run = new compiler.Run()

    logger info s"compiling save actions: ${cmd.files}"
    run compile cmd.files
    cmd.files
  }
}
