package org.scalaide.ui.internal.actions

import org.eclipse.core.resources.IProject
import org.eclipse.jface.action.IAction
import org.eclipse.jface.viewers.ISelection
import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.core.runtime.Platform
import org.eclipse.ui.IObjectActionDelegate
import org.eclipse.ui.IWorkbenchPart
import org.scalaide.core.ScalaPlugin.plugin
import org.scalaide.util.internal.Utils
import org.scalaide.core.internal.project.ScalaLibraryPluginDependencyUtils

object ToggleScalaNatureAction {
  val PdePluginNature = "org.eclipse.pde.PluginNature" /* == org.eclipse.pde.internal.core.natures.PDE.PLUGIN_NATURE */
  val PdeBundleName = "org.eclipse.pde.ui"
}

class ToggleScalaNatureAction extends AbstractPopupAction {
  import ToggleScalaNatureAction._

  override def performAction(project: IProject) {
    toggleScalaNature(project)
  }

  private def toggleScalaNature(project: IProject) =
    Utils tryExecute {
      if (project.hasNature(plugin.natureId)) {
        doIfPdePresent(project) { ScalaLibraryPluginDependencyUtils.removeScalaLibraryRequirement(project) }
        updateNatureIds(project) { _ filterNot (_ == plugin.natureId) }
      } else {
        doIfPdePresent(project) { ScalaLibraryPluginDependencyUtils.addScalaLibraryRequirement(project) }
        updateNatureIds(project) { plugin.natureId +: _ }
      }
    }

  private def doIfPdePresent(project: IProject)(proc: => Unit) =
    if (project.hasNature(PdePluginNature) && Platform.getBundle(PdeBundleName) != null)
      proc

  private def updateNatureIds(project: IProject)(natureIdUpdater: Array[String] => Array[String]) {
    val projectDescription = project.getDescription
    val currentNatureIds = projectDescription.getNatureIds
    val updatedNatureIds = natureIdUpdater(currentNatureIds)
    projectDescription.setNatureIds(updatedNatureIds)
    project.setDescription(projectDescription, null)
    project.touch(null)
  }
}
