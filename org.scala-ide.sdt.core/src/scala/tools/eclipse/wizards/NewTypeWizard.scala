package scala.tools.eclipse.wizards

import org.eclipse.jdt.internal.ui.wizards.NewElementWizard
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage
import scala.tools.eclipse.logging.HasLogger
import org.eclipse.jface.resource.ImageDescriptor
import org.eclipse.core.runtime.FileLocator
import org.eclipse.core.runtime.Path
import scala.tools.eclipse.ScalaPlugin
import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.swt.widgets.Composite

class NewTypeWizard extends NewElementWizard with HasLogger {

  val page = new NewScalaTypeWizardPage

  setWindowTitle("Create a new Scala Type")

  override def addPages() {
    super.addPages();
    addPage(page)
    page.init(getSelection())
  }

  override def performFinish(): Boolean = {
    val ret = super.performFinish()
    logger.debug("finish")
    ret
  }

  def finishPage(x: org.eclipse.core.runtime.IProgressMonitor) {
    ???
  }

  def getCreatedElement(): org.eclipse.jdt.core.IJavaElement = ???
}

class NewScalaTypeWizardPage extends NewTypeWizardPage(1, "") with HasLogger {

  setImageDescriptor(ImageDescriptor.createFromURL(
      FileLocator.find(ScalaPlugin.plugin.getBundle, new Path("icons/full/wizban/newtrait_wiz.gif"), null)))

  setTitle("Scala Type")
  setDescription("Create a new Scala Type")

  def init(selection: IStructuredSelection): Unit = {
    val jelem = getInitialJavaElement(selection)
    initContainerPage(jelem)
    initTypePage(jelem)
  }

  def createControl(parent: Composite) {

  }

}