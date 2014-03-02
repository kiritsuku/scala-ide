package org.scalaide.core.ui.indentation

import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.formatter.{DefaultCodeFormatterConstants => DCFC}
import org.eclipse.jdt.internal.core.JavaProject
import org.eclipse.jdt.ui.PreferenceConstants
import org.eclipse.jface.preference.IPreferenceStore
import org.scalaide.ui.internal.editor.indentation.UiHandler
import org.scalaide.logging.HasLogger
import org.scalaide.ui.internal.editor.indentation.PreferenceProvider
import org.scalaide.ui.internal.editor.indentation.ScalaIndenter
import org.scalaide.core.ui.AutoEditStrategyTests
import org.scalaide.ui.internal.editor.indentation.ScalaAutoIndentStrategy
import org.junit.Before

import AutoEditStrategyTests._

trait MockUiHandler extends UiHandler with HasLogger {

  def log(e: Throwable) {
    logger.error("caught a throwable in MockUiHandler", e)
  }

  // TODO find out the right implementations. See [[JdtUiHandler]] for the actual implementations.
  def getPreferenceStore: IPreferenceStore = ???
  def computeSmartMode: Boolean = ???
  def getIndentWidth(project: IJavaProject): Int = ???
  def getTabWidth(project: IJavaProject): Int = ???
}

object ScalaAutoIndentStrategyTest {

  val project = new JavaProject()
}

abstract class ScalaAutoIndentStrategyTest extends AutoEditStrategyTests(
    new ScalaAutoIndentStrategy(
        prefStore, null,
        ScalaAutoIndentStrategyTest.project, null) with MockUiHandler {
      override def computeSmartMode = true
    }) {

  @Before
  def startup() = {
    enable(PreferenceConstants.EDITOR_CLOSE_BRACES, true)
    enable(PreferenceConstants.EDITOR_SMART_TAB, true)
    enable(DCFC.FORMATTER_INDENT_STATEMENTS_COMPARE_TO_BLOCK, true)
    setStringPref(DCFC.FORMATTER_BRACE_POSITION_FOR_BLOCK, DCFC.END_OF_LINE)
    enable(DCFC.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_TYPE_HEADER, true)
    setStringPref(DCFC.FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION, DCFC.END_OF_LINE)
    setStringPref(DCFC.FORMATTER_TAB_CHAR, "space")
    enable(DCFC.FORMATTER_INDENT_STATEMENTS_COMPARE_TO_BODY, true)
    setStringPref(DCFC.FORMATTER_BRACE_POSITION_FOR_METHOD_DECLARATION, DCFC.END_OF_LINE)
    // TODO Find out the exact value that has to be returned here
    setStringPref(DCFC.FORMATTER_ALIGNMENT_FOR_ARGUMENTS_IN_METHOD_INVOCATION, "0")
    setIntPref(ScalaIndenter.TAB_SIZE, 2)
    setIntPref(ScalaIndenter.INDENT_SIZE, 2)
    enable(ScalaIndenter.INDENT_WITH_TABS, false)
  }

  val newline = Add("\n")
  val tab = Add("\t")
}