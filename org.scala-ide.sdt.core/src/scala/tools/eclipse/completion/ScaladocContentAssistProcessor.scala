package scala.tools.eclipse.completion

import org.eclipse.jface.text.contentassist.IContentAssistProcessor
import org.eclipse.jface.text.contentassist.ICompletionProposal
import org.eclipse.jface.text.contentassist.IContextInformationValidator
import org.eclipse.jface.text.ITextViewer
import org.eclipse.jface.text.contentassist.IContextInformation
import org.eclipse.jface.text.contentassist.{CompletionProposal => JCompletionProposal}
import org.eclipse.jface.text.contentassist.ContextInformation
import org.eclipse.jface.text.contentassist.ContextInformationValidator
import scala.tools.eclipse.ui.ScaladocAnnotationAutoEditStrategy

class ScaladocContentAssistProcessor extends IContentAssistProcessor with ScaladocAnnotationAutoEditStrategy {

  assistProcessor =>

  def computeCompletionProposals(viewer: ITextViewer, offset: Int): Array[ICompletionProposal] = {
    val doc = viewer.getDocument()
    val selectedRange = viewer.getSelectedRange()

    val tags = findTags(offset, isCommentClosed = true)
    println(">>>>>>>>>>>>>>>>>>>>>>>>>>tags:")
    tags foreach (t => t foreach println)

    val r = "replacement"
    val displayStr = s"This is JCP"
    val ci = new ContextInformation("context display str", displayStr + " | Style")
    val c1 = new JCompletionProposal(r, selectedRange.x, selectedRange.y, r.length(), null, displayStr, ci, s"$r + additional")
    val c2 = new JCompletionProposal(r+"4", selectedRange.x, selectedRange.y, r.length()+1, null, displayStr, ci, s"$r + additional")
//    val c3 = new JCompletionProposal(r+"3", selectedRange.x, selectedRange.y, r.length()+1, null, displayStr, ci, s"$r + additional")
//    Array(c1, c2, c3)
    Array(c1, c2)
  }

  def computeContextInformation(viewer: ITextViewer, offset: Int): Array[IContextInformation] = {
    val selectedRange = viewer.getSelectedRange()
//    if (selectedRange.y > 0)
    Array(new ContextInformation("context text", "context information"))
  }

  def getCompletionProposalAutoActivationCharacters(): Array[Char] =
    Array(/*why not working?*/'<', 'r')

  def getContextInformationAutoActivationCharacters(): Array[Char] =
    Array('>')

  def getErrorMessage(): String = null

  def getContextInformationValidator(): IContextInformationValidator =
    new IContextInformationValidator {
      var p: IContentAssistProcessor = assistProcessor
      var ci: IContextInformation = _
      var v: ITextViewer = _
      def install(contextInformation: IContextInformation, viewer: ITextViewer, offset: Int) {
        ci = contextInformation
        v = viewer
      }
      def isContextInformationValid(offset: Int) = true
    }
}