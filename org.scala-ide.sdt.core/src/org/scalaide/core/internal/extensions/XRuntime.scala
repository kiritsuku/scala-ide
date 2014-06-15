package org.scalaide.core.internal.extensions

import java.io.File
import scala.tools.nsc.Settings
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.jdt.core.ICompilationUnit
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility
import org.eclipse.jdt.internal.ui.javaeditor.saveparticipant.IPostSaveListener
import org.eclipse.jface.text.Document
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.ITextSelection
import org.eclipse.text.edits.MultiTextEdit
import org.eclipse.text.edits.ReplaceEdit
import org.eclipse.text.edits.TextEdit
import org.eclipse.ui.PlatformUI
import org.scalaide.core.internal.jdt.model.ScalaCompilationUnit
import org.scalaide.core.internal.project.ScalaProject
import org.scalaide.logging.HasLogger
import scala.tools.nsc.Global
import scala.tools.nsc.CompilerCommand
import scala.tools.nsc.reporters.StoreReporter
import org.scalaide.extensions.saveactions.SaveAction
import org.scalaide.core.ScalaPlugin
import java.net.URI
import org.eclipse.core.resources.IFile

object XRuntime extends AnyRef with HasLogger {

//  val compiler = projectByName("extide") map projectAsScalaProject map (new Compiler(_))

  import scala.reflect.runtime.universe._
  import scala.tools.reflect.ToolBox
  val tb = runtimeMirror(getClass.getClassLoader()).mkToolBox()

  def loadSaveActions(): Seq[SaveAction] = {
//    projectByName("extide") map projectAsScalaProject foreach { p =>
//      val srcs = p.allSourceFiles() map fromFile
//
//      srcs map (src => tb.compile(tb.parse(src)))
//    }

//    compiler foreach (_.execute())
    Nil
  }

  def fromFile(path: IFile): String = {
    // hello he
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
        "org.scala-ide.sdt.extensions/bin",
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

class X extends IPostSaveListener with HasLogger {
  import XRuntime._
  def getId(): String = "scalaide-X-id"

  def getName(): String = "scalaide-X-name"

  def needsChangedRegions(cu: ICompilationUnit): Boolean = {
    false
  }

  def removeTrailingWs(doc: IDocument): TextEdit = {
    val infos = (0 until doc.getNumberOfLines()).iterator map doc.getLineInformation

    def trimRight(str: String): String =
      str.reverse.dropWhile(Character.isWhitespace).mkString.reverse

    val transforms = infos map { r =>
      val line = doc.get(r.getOffset(), r.getLength())
      r -> trimRight(line)
    }

    val edits = transforms map {
      case (r, line) =>
        new ReplaceEdit(r.getOffset(), r.getLength(), line)
    }

    val edit = new MultiTextEdit
    edits foreach edit.addChild
    edit
  }

  def withClassLoader[A](cl: ClassLoader)(f: => A): A = {
    val ccl = Thread.currentThread().getContextClassLoader()
    try {
      Thread.currentThread().setContextClassLoader(cl)
      f
    } finally {
      Thread.currentThread().setContextClassLoader(ccl)
    }
  }

  def loadExtensionProject() = for {
    p <- projectByName("extide")
    jp <- projectAsJavaProject(p)
  } {
    val folder = p.getFolder("src")
    val root = jp.getPackageFragmentRoot(folder)
    val pkg = root.getPackageFragment("extide")
    val cus = pkg.getCompilationUnits()
    println("---------- compilation units:")
    cus foreach println
    println("---------- end")

//    val url = p.getFolder("bin").getLocationURI().toURL()
//    val cl = URLClassLoader.newInstance(Array(url))
//    withClassLoader(cl) {
//      import util._
//      Try {
//        val cls = Class.forName("extide.RemoveTrailingWhitespace", true, cl)
//        cls
//      } match {
//        case Success(s) =>
//          println(s"success: $s")
//        case Failure(f) =>
//          println(s"failure: $f")
//      }
//    }


//    for {
//      cu <- cus.find(_.getElementName() == "SaveAction.scala")
//      tps = cu.getTypes()
//      tpeTestClass <- tps.find(_.getElementName() == "TestClass")
////      tpeSaveAction <- tps.find(_.getElementName() == "SaveAction")
////      tpeTextEdit <- tps.find(_.getElementName() == "TextEdit")
////      tpeDocument <- tps.find(_.getElementName() == "Document")
//    } {
//      val r = tpeTestClass.getUnderlyingResource()
//      val url = p.getFolder("bin").getLocationURI().toURL()
////      val url = r.getLocationURI().toURL()
//      println(">>> url: " + url)
////      val bundle = ScalaPlugin.plugin.getBundle()
////      val cl = bundle.adapt(classOf[BundleWiring]).getClassLoader()
//
//
////      println(cls)
////      val o = cls.newInstance()
////      val m = cls.getMethod("hello")
////      val res = m.invoke(o)
////      println(">>>> result: " + res)
//    }
  }

  /*
   * TODO
   * - restore cursor to position before any changes happened
   */
  def saved(cu: ICompilationUnit, changedRegions: Array[IRegion], m: IProgressMonitor): Unit = {
    cu match {
      case sc: ScalaCompilationUnit => // ScalaSourceFile
        val p = ScalaPlugin.plugin.getScalaProject(sc.getJavaProject().getProject())

        p.presentationCompiler { compiler =>
          loadExtensionProject()
          val src = sc.sourceFile()
          val doc = new Document(src.content.mkString)
          val edit = removeTrailingWs(doc)

          edit.apply(doc)
          sc.getBuffer().setContents(doc.get())
        }
      case _ =>
        eclipseLog.debug(s"compilation unit is of type '${cu.getClass()}' and can't be handled")
    }
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