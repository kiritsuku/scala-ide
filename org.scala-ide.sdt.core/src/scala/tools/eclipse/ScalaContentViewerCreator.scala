package scala.tools.eclipse

import org.eclipse.compare.IViewerCreator
import org.eclipse.swt.widgets.Composite
import org.eclipse.compare.CompareConfiguration
import org.eclipse.jface.viewers.Viewer
import org.eclipse.swt.SWT

class ScalaContentViewerCreator extends IViewerCreator {

  def createViewer(parent: Composite, cc: CompareConfiguration): Viewer =
    new ScalaMergeViewer(parent, SWT.NULL, cc)
}