package scala.tools.eclipse

import org.eclipse.swt.widgets.Composite
import org.eclipse.compare.CompareConfiguration
import org.eclipse.compare.contentmergeviewer.TextMergeViewer
import org.eclipse.swt.SWT
import org.eclipse.jface.text.TextViewer
import org.eclipse.jface.text.source.SourceViewer
import org.eclipse.ui.IEditorInput

class ScalaMergeViewer(parent: Composite, styles: Int, cc: CompareConfiguration)
    extends JavaMergeViewer(parent, styles, cc) {
}
//  extends TextMergeViewer(parent, styles | SWT.LEFT_TO_RIGHT, cc) {
//
//  println(">>>>>>>> cretae merge viewer")
//
//  override def getTitle() = "Scala Source Comparison"
//
//  override def configureTextViewer(viewer: TextViewer): Unit = viewer match {
//    case sv: SourceViewer =>
//      val ei = getEditorInput(sv)
//      sv.unconfigure()
//
//      println(s"<<<<<<<< configure: $sv, $ei")
//
//      if (ei == null)
//        sv.configure(ssvcOf(sv, null))
//      else
//        ssvcOf(sv, ei)
//      sv.invalidateTextPresentation()
//    case _ =>
//  }
//
//  override def createSourceViewer(parent: Composite, textOrientation: Int): SourceViewer = {
//    ???
//  }
//
//  private def ssvcOf(sv: SourceViewer, ei: IEditorInput): ScalaSourceViewerConfiguration = {
//    val ssvc = new ScalaSourceViewerConfiguration(null, null, null)
//    ssvc
//  }
//}