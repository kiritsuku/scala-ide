package org.scalaide.debug.internal

import java.io.File

import org.eclipse.core.resources.IncrementalProjectBuilder
import org.eclipse.core.runtime.NullProgressMonitor
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.scalaide.core.testsetup.SDTTestUtils
import org.scalaide.core.testsetup.TestProjectSetup

object SourcePathInStackFrameTest extends TestProjectSetup("source-path", bundleName = "org.scala-ide.sdt.debug.tests") with ScalaDebugRunningTest {
  final val BP_TYPENAME = "test.a.Main"

  var initialized = false

  def initDebugSession(launchConfigurationName: String): ScalaDebugTestSession = ScalaDebugTestSession(file(launchConfigurationName + ".launch"))

  @AfterClass
  def deleteProject(): Unit = {
    SDTTestUtils.deleteProjects(project)
  }
}

class SourcePathInStackFrameTest {
  import SourcePathInStackFrameTest._

  var session: ScalaDebugTestSession = null

  @Before
  def initializeTests(): Unit = {
    SDTTestUtils.enableAutoBuild(false)
    if (!initialized) {
      project.underlying.build(IncrementalProjectBuilder.CLEAN_BUILD, new NullProgressMonitor)
      project.underlying.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new NullProgressMonitor)
      initialized = true
    }
  }

  @After
  def cleanDebugSession(): Unit = {
    if (session ne null) {
      session.terminate()
      session = null
    }
  }

  private val mainBreakpoint = 16
  private val expectedSourcePath = File.separator + "test" + File.separator + "SourcePath.scala"
  @Test
  def simpleCheckForSourcePathInTopMostStackFrameForClassesWithPkgsWhichDontReflectSourceFoldersStruct(): Unit = {
    session = initDebugSession("SourcePath")
    session.runToLine(BP_TYPENAME, mainBreakpoint)
    Assert.assertTrue(session.currentStackFrame.getSourcePath == expectedSourcePath)

    val bp4 = session.addLineBreakpoint("test.b.B", 4)
    val bp8 = session.addLineBreakpoint("test.b.c.C", 8)
    try {
      session.waitForBreakpointsToBeEnabled(bp4, bp8)

      session.resumeToSuspension()
      Assert.assertTrue(session.currentStackFrame.getSourcePath == expectedSourcePath)

      session.resumeToSuspension()
      Assert.assertTrue(session.currentStackFrame.getSourcePath == expectedSourcePath)

      session.resumeToCompletion()
    } finally {
      bp4.delete()
      bp8.delete()
    }
  }
}
