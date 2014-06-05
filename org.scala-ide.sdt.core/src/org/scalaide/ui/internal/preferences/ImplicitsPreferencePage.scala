package org.scalaide.ui.internal.preferences

import org.eclipse.jface.preference._
import org.eclipse.ui.IWorkbenchPreferencePage
import org.eclipse.ui.IWorkbench
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer
import org.eclipse.jface.preference.IPreferenceStore
import org.scalaide.core.ScalaPlugin
import org.eclipse.swt.widgets.Link
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.jdt.internal.ui.preferences.PreferencesMessages
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.ui.dialogs.PreferencesUtil

class ImplicitsPreferencePage extends FieldEditorPreferencePage with IWorkbenchPreferencePage {
  import ImplicitsPreferencePage._
  import org.scalaide.util.internal.eclipse.SWTUtils._

  setPreferenceStore(ScalaPlugin.plugin.getPreferenceStore)
  setDescription("""
Set the highlighting for implicit conversions and implicit parameters.
  """)

  override def createContents(parent: Composite): Control = {
    val control = super.createContents(parent).asInstanceOf[Composite]
    val link = new Link(control, SWT.NONE)
    link.setText("""More options for highlighting for implicit conversions on the <a href="org.eclipse.ui.editors.preferencePages.Annotations">Text Editors/Annotations</a> preference page.""")
    link.addSelectionListener { e: SelectionEvent =>
      PreferencesUtil.createPreferenceDialogOn(parent.getShell, e.text, null, null)
    }

    control
  }

  override def createFieldEditors() {
    addField(new BooleanFieldEditor(Active, "Enabled", getFieldEditorParent))
    addField(new BooleanFieldEditor(Bold, "Bold", getFieldEditorParent))
    addField(new BooleanFieldEditor(Italic, "Italic", getFieldEditorParent))
    addField(new BooleanFieldEditor(ConversionsOnly, "Only highlight implicit conversions", getFieldEditorParent))
    addField(new BooleanFieldEditor(FirstLineOnly, "Only highlight the first line in an implicit conversion", getFieldEditorParent))
  }

  def init(workbench: IWorkbench) {}

}

object ImplicitsPreferencePage {
  val Base = "scala.tools.eclipse.ui.preferences.implicit."
  val Active = Base + "enabled"
  val Bold = Base + "text.bold"
  val Italic = Base + "text.italic"
  val ConversionsOnly = Base + "conversions.only"
  val FirstLineOnly  = Base + "firstline.only"
}

class ImplicitsPagePreferenceInitializer extends AbstractPreferenceInitializer {

  import ImplicitsPreferencePage._

  override def initializeDefaultPreferences() {
    val store = ScalaPlugin.plugin.getPreferenceStore
    store.setDefault(Active, true)
    store.setDefault(Bold, false)
    store.setDefault(Italic, false)
    store.setDefault(ConversionsOnly, true)
    store.setDefault(FirstLineOnly, true)
  }
}
