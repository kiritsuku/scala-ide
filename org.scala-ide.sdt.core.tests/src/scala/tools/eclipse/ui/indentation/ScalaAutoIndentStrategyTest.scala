package scala.tools.eclipse.ui.indentation

import scala.tools.eclipse.logging.HasLogger
import org.eclipse.jdt.internal.core.JavaProject
import org.eclipse.jdt.ui.PreferenceConstants
import org.junit.Test
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jdt.core.formatter.{DefaultCodeFormatterConstants => DCFC}
import scala.tools.eclipse.ui.AutoEditStrategyTests
import scala.tools.eclipse.ui.ScalaAutoIndentStrategy
import scala.tools.eclipse.ui.ScalaIndenter

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

  val preferenceProvider = {

    val p = new PreferenceProvider {
      // Nothing to do - we rely on someone setting up this cache
      def updateCache() {}
    }

    import org.eclipse.jdt.core.formatter.{ DefaultCodeFormatterConstants => DCFC }

    for { (k, v) <- Seq(
      PreferenceConstants.EDITOR_CLOSE_BRACES -> "true",
      PreferenceConstants.EDITOR_SMART_TAB -> "true",
      DCFC.FORMATTER_INDENT_STATEMENTS_COMPARE_TO_BLOCK -> "true",
      DCFC.FORMATTER_BRACE_POSITION_FOR_BLOCK -> DCFC.END_OF_LINE,
      DCFC.FORMATTER_INDENT_BODY_DECLARATIONS_COMPARE_TO_TYPE_HEADER -> "true",
      DCFC.FORMATTER_BRACE_POSITION_FOR_TYPE_DECLARATION -> DCFC.END_OF_LINE,
      DCFC.FORMATTER_TAB_CHAR -> "space",
      DCFC.FORMATTER_INDENT_STATEMENTS_COMPARE_TO_BODY -> "true",
      DCFC.FORMATTER_BRACE_POSITION_FOR_METHOD_DECLARATION -> DCFC.END_OF_LINE,
      ScalaIndenter.TAB_SIZE -> "4",
      ScalaIndenter.INDENT_SIZE -> "2",
      ScalaIndenter.INDENT_WITH_TABS -> "false")
    } p.put(k, v)

    p
  }

}

abstract class ScalaAutoIndentStrategyTest extends AutoEditStrategyTests(
    new ScalaAutoIndentStrategy(
        null, ScalaAutoIndentStrategyTest.project,
        null, ScalaAutoIndentStrategyTest.preferenceProvider) with MockUiHandler {
      override def computeSmartMode = true
    }) {

  val linebreak = Add("\n")
  val tab = Add("\t")
}