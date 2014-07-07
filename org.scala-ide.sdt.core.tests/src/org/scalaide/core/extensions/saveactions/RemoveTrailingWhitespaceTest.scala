package org.scalaide.core.extensions.saveactions

import org.scalaide.core.compiler.ScalaPresentationCompiler
import org.scalaide.core.internal.project.ScalaProject
import org.scalaide.core.EclipseUserSimulator
import org.scalaide.util.internal.eclipse.EclipseUtils
import org.scalaide.core.testsetup.SDTTestUtils
import org.eclipse.jdt.core.ICompilationUnit
import org.scalaide.core.internal.jdt.model.ScalaCompilationUnit
import org.eclipse.core.runtime.NullProgressMonitor
import org.scalaide.core.ScalaPlugin
import org.junit.AfterClass
import org.scalaide.core.text.Add
import org.scalaide.core.text.Replace
import org.scalaide.core.text.Remove
import org.scalaide.core.text.InternalDocument
import org.scalaide.extensions.saveactions.RemoveTrailingWhitespace
import org.scalaide.extensions._
import org.scalaide.core.internal.text.TextDocument

trait CompilerSupport {

  /** Can be overwritten in a subclass if desired. */
  val projectName: String = getClass().getSimpleName()

  private val project: ScalaProject = {
    val simulator = new EclipseUserSimulator
    simulator.createProjectInWorkspace(projectName)
  }

  def withCompiler(f: ScalaPresentationCompiler => Unit): Unit =
    project.presentationCompiler { compiler =>
      f(compiler)
    }

  /**
   * Creates a compilation unit whose underlying source file physically exists
   * in the test project of the test workspace. The file is placed in a unique
   * package name to prevent name clashes between generated files.
   *
   * The newly generated file is made available to the Eclipse platform and the
   * Scala compiler to allow the usage of the full non GUI feature set of the IDE.
   */
  final def mkCompilationUnit(source: String): ICompilationUnit = {
    val p = SDTTestUtils.createSourcePackage("testpackage" + System.nanoTime())(project)
    new EclipseUserSimulator().createCompilationUnit(p, "testfile.scala", source)
  }

  final def mkScalaCompilationUnit(source: String): ScalaCompilationUnit =
    mkCompilationUnit(source).asInstanceOf[ScalaCompilationUnit]

  @AfterClass
  final def deleteProject(): Unit = {
    EclipseUtils.workspaceRunnableIn(ScalaPlugin.plugin.workspaceRoot.getWorkspace()) { _ =>
      project.underlying.delete(/* force */ true, new NullProgressMonitor)
    }
  }
}

object RemoveTrailingWhitespaceTest extends CompilerSupport
class RemoveTrailingWhitespaceTest {

  import RemoveTrailingWhitespaceTest._

  def performSaveAction(cu: ScalaCompilationUnit, saveAction: SaveAction) = {
    import saveAction._
    import saveAction.global._

    def selection = {
      val r = new Response[Tree]
      askLoadedTyped(cu.sourceFile(), r)
      r.get.fold(new FileSelection(cu.file, _, 0, 0), throw _)
    }

    perform(selection)
  }

  def test(source: String) {
    EclipseUtils.workspaceRunnableIn(SDTTestUtils.workspace) { _ =>
      withCompiler { compiler =>
        val cu = mkScalaCompilationUnit(source)
        val saveAction = new RemoveTrailingWhitespace { val global = compiler }
        val edits = performSaveAction(cu, saveAction)
        val sorted = edits.sortBy {
          case Add(start, text) =>
            -start
          case Replace(start, end, text) =>
            -start
          case Remove(start, end) =>
            -start
        }
        val doc = new TextDocument(source)
        sorted.foreach {
          case Add(start, text) =>
            doc.replace(start, 0, text)
          case Replace(start, end, text) =>
            doc.replace(start, end, text)
          case Remove(start, end) =>
            doc.replace(start, end, "")
        }
      }
    }
  }
}