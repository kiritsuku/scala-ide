package org.scalaide.ui.internal.editor.indentation

import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jdt.core.IJavaProject

/**
 * An interface to allow the indentation logic to swap out its UI dependencies.
 */
trait UiHandler {

  /**
   * Copied from [[org.eclipse.jdt.ui.PreferenceConstants]]
   */
  object constants {
    val EDITOR_CLOSE_BRACES = "closeBraces"
    val EDITOR_SMART_TAB = "smart_tab"
    val EDITOR_SMART_PASTE = "smartPaste"
  }

  def log(e: Throwable): Unit

  def getPreferenceStore: IPreferenceStore

  def computeSmartMode: Boolean

  def getIndentWidth(project: IJavaProject): Int

  def getTabWidth(project: IJavaProject): Int
}
