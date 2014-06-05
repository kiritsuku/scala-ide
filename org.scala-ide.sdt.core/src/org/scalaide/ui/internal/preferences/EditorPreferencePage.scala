package org.scalaide.ui.internal.preferences

import scala.collection.mutable.ListBuffer
import org.scalaide.util.internal.eclipse.SWTUtils.CheckBox

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer
import org.eclipse.jface.preference.ColorFieldEditor
import org.eclipse.jface.preference.PreferencePage
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Group
import org.eclipse.ui.IWorkbench
import org.eclipse.ui.IWorkbenchPreferencePage
import org.scalaide.core.ScalaPlugin

import EditorPreferencePage._

class EditorPreferencePage extends PreferencePage with IWorkbenchPreferencePage {

  private val store = ScalaPlugin.prefStore

  private val preferencesToSave = ListBuffer[() => Unit]()

  override def performOk(): Boolean = {
    preferencesToSave foreach (_())
    super.performOk()
  }

  override def init(workbench: IWorkbench): Unit = {}

  override def createContents(parent: Composite): Control = {
    setPreferenceStore(store)

    val base = new Composite(parent, SWT.NONE)
    base.setLayout(new GridLayout(1, true))
    base.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true))

    createSettingsGroup(base)
    createIndentGuideGroup(base)

    base
  }

  private def createSettingsGroup(base: Composite): Unit = {
    val surround = group("Automatically surround selection", base)
    checkBox(EnableSmartBrackets, "With [brackets]", surround)
    checkBox(EnableSmartBraces, "With {braces}", surround)
    checkBox(EnableSmartParens, "With (parenthesis)", surround)
    checkBox(EnableSmartQuotes, "With \"quotes\"", surround)

    val insertion = group("Enable smart insertion mode", base)
    checkBox(EnableSmartInsertionSemicolons, "For Semicolons", insertion)
    checkBox(EnableSmartInsertionBraces, "Close braces but close only when necessary", insertion)

    val autoClose = group("Automatically close", base)
    checkBox(EnableAutoClosingStrings, "\"Strings\", \"\"\"Multi line strings\"\"\" and 'Chars'", autoClose)
    checkBox(EnableAutoClosingParens, "(Parentheses)", autoClose)
    checkBox(EnableAutoClosingSquareBrackets, "[Square] brackets", autoClose)
    checkBox(EnableAutoClosingAngleBrackets, "<Angle> brackets", autoClose)
    checkBox(EnableAutoClosingBraces, "{Braces}", autoClose)
    checkBox(EnableAutoClosingComments, "/*Multi line comments*/ and /**Scaladoc*/", autoClose)

    val typing = group("Typing", base)
    checkBox(EnableAutoEscapeLiterals, "Automatically escape \" signs in string literals", typing)
    checkBox(EnableAutoEscapeSign, "Automatically escape \\ signs in string and character literals", typing)
    checkBox(EnableAutoRemoveEscapedSign, "Automatically remove complete escaped sign in string and character literals", typing)
    checkBox(EnableAutoBreakingComments, "Automatically break multi-line comments and Scaladoc after the Print Margin", typing)

    val indent = group("Indentation", base)
    checkBox(EnableAutoIndentOnTab, "Enable auto indent when tab is pressed", indent)
    checkBox(EnableAutoIndentMultiLineString, "Enable auto indent for multi line string literals", indent)
    checkBox(EnableAutoStripMarginInMultiLineString, "Automatically add strip margins when multi line string starts with a |", indent)

    val highlighting = group("Highlighting", base)
    checkBox(EnableMarkOccurrences, "Mark Occurences of the selected element in the current file", highlighting)
    checkBox(ShowInferredSemicolons, "Show inferred semicolons", highlighting)
  }

  private def group(text: String, parent: Composite): Group = {
    val g = new Group(parent, SWT.NONE)
    g.setText(text)
    g.setLayout(new GridLayout(1, true))
    g.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true, false))
    g
  }

  private def checkBox(preference: String, labelText: String, parent: Composite): CheckBox = {
    val b = new CheckBox(store, preference, labelText, parent)
    preferencesToSave += { () => b.store() }
    b
  }

  private def createIndentGuideGroup(base: Composite): Unit = {
    val indentGuide = group("Indent Guide", base)
    val enable = checkBox(IndentGuideEnable, "Enable the indent guide", indentGuide)
    val color = new Composite(indentGuide, SWT.NONE)
    val c = new ColorFieldEditor(IndentGuideColor, "Color:", color)

    c.setPreferenceStore(store)
    c.load()
    preferencesToSave += { () => c.store() }

    def enableControls(b: Boolean) = c.setEnabled(b, color)

    enable += (_ => enableControls(enable.isChecked))
    enableControls(enable.isChecked)
  }

}

object EditorPreferencePage {
  private final val Base = "scala.tools.eclipse.editor."

  final val EnableSmartBrackets = Base + "smartBrackets"
  final val EnableSmartBraces = Base + "smartBraces"
  final val EnableSmartParens = Base + "smartParens"
  final val EnableSmartQuotes = Base + "smartQuotes"

  final val EnableSmartInsertionSemicolons = Base + "smartInsertionSemicolons"
  final val EnableSmartInsertionBraces = Base + "smartAutoClosingBraces"

  final val EnableAutoClosingStrings = Base + "autoClosingStrings"
  final val EnableAutoClosingParens = Base + "autoClosingParens"
  final val EnableAutoClosingSquareBrackets = Base + "autoClosingSquareBrackets"
  final val EnableAutoClosingAngleBrackets = Base + "autoClosingAngleBrackets"
  final val EnableAutoClosingBraces = Base + "autoClosingBraces"
  final val EnableAutoClosingComments = Base + "autoClosingComments"

  final val EnableAutoEscapeLiterals = Base + "autoEscapeLiterals"
  final val EnableAutoEscapeSign = Base + "autoEscapeSign"
  final val EnableAutoRemoveEscapedSign = Base + "autoRemoveEscapedSign"
  final val EnableAutoIndentOnTab = Base + "autoIndent"
  final val EnableAutoIndentMultiLineString = Base + "autoIndentMultiLineString"
  final val EnableAutoStripMarginInMultiLineString = Base + "autoStringMarginInMultiLineString"
  final val EnableAutoBreakingComments = Base + "autoBreakingComments"

  final val EnableMarkOccurrences = Base + "markOccurences"
  final val ShowInferredSemicolons = Base + "showInferredSemicolons"

  final val IndentGuideEnable = Base + "indentGuideEnable"
  final val IndentGuideColor = Base + "indentGuideColor"
}

class EditorPreferenceInitializer extends AbstractPreferenceInitializer {

  override def initializeDefaultPreferences() {
    val store = ScalaPlugin.plugin.getPreferenceStore
    store.setDefault(EnableSmartBrackets, false)
    store.setDefault(EnableSmartBraces, false)
    store.setDefault(EnableSmartParens, false)
    store.setDefault(EnableSmartQuotes, false)

    store.setDefault(EnableSmartInsertionSemicolons, false)

    store.setDefault(EnableAutoClosingStrings, true)
    store.setDefault(EnableAutoClosingParens, true)
    store.setDefault(EnableAutoClosingSquareBrackets, true)
    store.setDefault(EnableAutoClosingAngleBrackets, false)
    store.setDefault(EnableAutoClosingBraces, true)
    store.setDefault(EnableAutoClosingComments, true)

    store.setDefault(EnableAutoEscapeLiterals, false)
    store.setDefault(EnableAutoEscapeSign, false)
    store.setDefault(EnableAutoRemoveEscapedSign, false)
    store.setDefault(EnableAutoIndentOnTab, true)
    store.setDefault(EnableAutoIndentMultiLineString, false)
    store.setDefault(EnableAutoStripMarginInMultiLineString, false)
    store.setDefault(EnableAutoBreakingComments, false)

    store.setDefault(EnableMarkOccurrences, false)
    // TODO This preference is added in 4.0. Delete the former preference once support for the former release is dropped.
    store.setDefault(ShowInferredSemicolons, store.getBoolean("actions.showInferredSemicolons"))

    store.setDefault(IndentGuideEnable, false)
    store.setDefault(IndentGuideColor, "72,72,72")
  }
}
