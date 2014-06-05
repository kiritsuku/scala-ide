package org.scalaide.ui.internal.diagnostic

import scala.tools.eclipse.contribution.weaving.jdt.configuration.WeavingStateConfigurer
import org.scalaide.logging.HasLogger
import org.scalaide.util.internal.ui.DisplayThread
import org.eclipse.core.runtime.preferences.InstanceScope
import org.eclipse.ui.PlatformUI
import org.eclipse.jface.preference.IPreferenceStore
import org.scalaide.core.ScalaPlugin


object MessageDialog {
  import org.eclipse.jface.dialogs.{ MessageDialog => JFMessageDialog }
  import org.eclipse.swt.widgets.Shell
  def apply(heading: String, message: String, labels: (Int, String)*) =
    new JFMessageDialog(ScalaPlugin.getShell, heading, null, message, JFMessageDialog.QUESTION, labels.map(_._2).toArray, 0).open()
  def confirm(heading: String, message: String) =
    JFMessageDialog.openConfirm(ScalaPlugin.getShell, heading, message)
  def question(heading: String, message: String) =
    JFMessageDialog.openQuestion(ScalaPlugin.getShell, heading, message)
  val CloseAction = -1
}

object StartupDiagnostics extends HasLogger {
  import ScalaPlugin.plugin

  private val InstalledVersionKey = plugin.pluginId + ".diagnostic.currentPluginVersion"
  val AskDiagnostics = plugin.pluginId + ".diagnostic.askOnUpgrade"

  private val weavingState = new WeavingStateConfigurer

  def suggestDiagnostics(insufficientHeap: Boolean, firstInstall: Boolean, ask: Boolean): Boolean =
    ask && firstInstall && insufficientHeap

  def suggestDiagnostics(prefStore: IPreferenceStore): Boolean = {
    val firstInstall = (prefStore getString InstalledVersionKey) == ""
    val ask = prefStore getBoolean AskDiagnostics
    suggestDiagnostics(Diagnostics.insufficientHeap, firstInstall, ask)
  }

  def run() {
    val YesAction = 0
    val NoAction = 1
    val NeverAction = 2
    import MessageDialog.CloseAction

    val prefStore = plugin.getPreferenceStore
    DisplayThread.asyncExec {
      if (suggestDiagnostics(prefStore)) {
        import org.eclipse.jface.dialogs.IDialogConstants._
        MessageDialog(
          "Run Scala Setup Diagnostics?",
          """|We detected that some of your settings are not adequate for the Scala IDE plugin.
             |
             |Run setup diagnostics to ensure correct plugin settings?""".stripMargin,
          YesAction -> YES_LABEL, NoAction -> NO_LABEL, NeverAction -> "Never") match {
            case YesAction =>
              new DiagnosticDialog(weavingState, ScalaPlugin.getShell).open
            case NeverAction =>
              prefStore.setValue(AskDiagnostics, false)
            case NoAction | CloseAction =>
          }
        val currentVersion = plugin.getBundle.getVersion.toString
        prefStore.setValue(InstalledVersionKey, currentVersion)
        InstanceScope.INSTANCE.getNode(ScalaPlugin.plugin.pluginId).flush()
      }
      ensureWeavingIsEnabled()
    }
  }

  private def ensureWeavingIsEnabled(): Unit = {
    if (!weavingState.isWeaving) {
      val forceWeavingOn = MessageDialog.confirm(
        "JDT Weaving is disabled",
        """|JDT Weaving is currently disabled. The Scala IDE needs JDT Weaving to be active, or it will not work as expected.
           |Activate JDT Weaving and restart Eclipse? (Highly Recommended)""".stripMargin)

      if (forceWeavingOn) {
        weavingState.changeWeavingState(true)
        PlatformUI.getWorkbench.restart
      }
    }
  }
}
