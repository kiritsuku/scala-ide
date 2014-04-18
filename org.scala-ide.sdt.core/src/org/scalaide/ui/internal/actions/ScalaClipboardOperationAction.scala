package org.scalaide.ui.internal.actions

import java.util.ResourceBundle
import scala.reflect.internal.util.BatchSourceFile
import scala.tools.refactoring.common.TextChange
import org.eclipse.core.runtime.NullProgressMonitor
import org.eclipse.jface.text.ITextOperationTarget
import org.eclipse.jface.text.ITextSelection
import org.eclipse.swt.custom.BusyIndicator
import org.eclipse.swt.dnd.Clipboard
import org.eclipse.swt.dnd.TextTransfer
import org.eclipse.swt.widgets.Display
import org.eclipse.ui.texteditor.ITextEditor
import org.eclipse.ui.texteditor.TextEditorAction
import org.scalaide.core.compiler.ScalaPresentationCompiler
import org.scalaide.core.internal.jdt.model.ScalaSourceFile
import scala.tools.refactoring.analysis.CompilationUnitDependencies
import scala.tools.refactoring.common.InteractiveScalaCompiler
import scala.tools.refactoring.common.TreeExtractors
import scala.tools.refactoring.common.Selections
import scala.tools.refactoring.common.TextChange
import org.scalaide.refactoring.internal.clipboard.ClipboardOperation
import org.eclipse.jface.text.TextSelection
import org.eclipse.jface.text.IDocument
import org.scalaide.util.internal.eclipse.EditorUtils

/**
 * The implementation of this class is losely based on
 * [[org.eclipse.jdt.internal.ui.javaeditor.ClipboardOperationAction]]. It
 * provides tasks that are required when one does copy and paste operations in
 * the editor.
 *
 * TODO
 * More info:
 * - ScalaCompletionProposal: how to add imports
 * - ScalaSourceFileEditor/JavaEditor: how to install actions
 * - CCPActionGroup: Installs copy and paste operations for views
 */
class ScalaClipboardOperationAction(
    bundle: ResourceBundle,
    prefix: String,
    editor: ITextEditor,
    operationCode: Int)
  extends TextEditorAction(bundle, prefix, editor) {

  override def run(): Unit = withDisplay { display =>
    BusyIndicator.showWhile(display, new Runnable() {
      override def run() = doAction()
    })
  }

  private def doAction(): Unit = operationCode match {
    case ITextOperationTarget.COPY  => handleCopyAction()
    case ITextOperationTarget.PASTE => handlePasteAction()
    case _                          => forwardActionToEditor()
  }

  private def handleCopyAction(): Unit = withClipboard { cb =>

    EditorUtils.withScalaSourceFileAndSelection { (sourceFile, selection) =>
      sourceFile.withSourceFile { (internalSourceFile, compiler) =>
        val start = selection.getOffset()
        val end = start+selection.getLength()

        compiler.askOption { () =>
          val op = new ClipboardOperation(compiler)
          val content = op.copyContent(
              internalSourceFile,
              new TextSelection(doc, start, end-start))
          println(s"copy content: $content")
        }
      }
    }

    // TODO serialize TextChanges
    cb.setContents(Array(selectedText), Array(TextTransfer.getInstance()))
  }

  private def handlePasteAction(): Unit = withClipboard { cb =>
    // TODO deserialize TextChanges
    val data = cb.getContents(TextTransfer.getInstance()).asInstanceOf[String]
    val content = Nil

    EditorUtils.withScalaSourceFileAndSelection { (sourceFile, selection) =>
      sourceFile.withSourceFile { (internalSourceFile, compiler) =>

        EditorUtils.applyChangesToFileWhileKeepingSelection(
            doc, selection, internalSourceFile.file, content)
      }
    }
  }

//  /*
//   * TODO:
//   * - unreload generated file
//   */
//  private def x(sourceFile: ScalaSourceFile, compiler: ScalaPresentationCompiler, change: TextChange): Unit = {
//    import OrganizeImportsAction._
//    val name = "$internal$" + System.currentTimeMillis()
//    val s = new BatchSourceFile(name, s"object $name { ${change.text} }")
//    val r = new compiler.Response[Unit]
//    compiler.askReload(List(s), r)
//    r.get
//    val problems = compiler.problemsOf(s.file)
//
//    val missingTypes = getMissingTypeErrors(problems.toArray)
//    val pm = new NullProgressMonitor()
//    addMissingImportsToFile(missingTypes, sourceFile, pm, iterations = 1)
//  }

  private def doc: IDocument =
    editor.getDocumentProvider().getDocument(editor.getEditorInput())

  private def selectedText: String =
    editor.getSelectionProvider()
      .getSelection()
      .asInstanceOf[ITextSelection]
      .getText()

  // TODO remove this method
  private def forwardActionToEditor(): Unit =
    editor.getAdapter(classOf[ITextOperationTarget])
      .asInstanceOf[ITextOperationTarget]
      .doOperation(operationCode)

  private def withDisplay(f: Display => Unit): Unit =
    Option(editor.getSite().getShell())
      .filter(s => !s.isDisposed())
      .map(_.getDisplay())
      .foreach(f)

  private def withClipboard(f: Clipboard => Unit): Unit = withDisplay { display =>
    f(new Clipboard(display))
  }
}