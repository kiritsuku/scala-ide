package org.scalaide.ui.internal.editor.indentation

import org.eclipse.jdt.internal.ui.JavaPlugin
import org.eclipse.ui.texteditor.ITextEditorExtension3
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil

/**
 * Contains all UI dependencies of the indentation logic. Contents of this class
 * were formerly contained in [[scala.tools.eclipse.ui.ScalaAutoIndentStrategy]].
 */
trait JdtUiHandler extends UiHandler {

  def log(e: Throwable) {
    JavaPlugin.log(e)
  }

  def getPreferenceStore = JavaPlugin.getDefault().getCombinedPreferenceStore()

  def computeSmartMode: Boolean = {
    val page = JavaPlugin.getActivePage()
    if (page != null) {
      val part = page.getActiveEditor()
      if (part.isInstanceOf[ITextEditorExtension3]) {
        val extension = part.asInstanceOf[ITextEditorExtension3]
        return extension.getInsertMode() == ITextEditorExtension3.SMART_INSERT
      }
    }
    return false
  }

  def getIndentWidth(project: IJavaProject) =
    CodeFormatterUtil.getIndentWidth(project)

  def getTabWidth(project: IJavaProject) =
    CodeFormatterUtil.getTabWidth(project)
}
