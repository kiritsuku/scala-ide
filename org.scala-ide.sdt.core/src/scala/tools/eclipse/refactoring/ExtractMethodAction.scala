/*
 * Copyright 2005-2010 LAMP/EPFL
 */

package scala.tools.eclipse.refactoring

import scala.tools.eclipse.javaelements.ScalaSourceFile
import scala.tools.eclipse.refactoring.ui.NewNameWizardPage
import scala.tools.refactoring.analysis.GlobalIndexes
import scala.tools.refactoring.analysis.NameValidation
import scala.tools.refactoring.implementations.ExtractMethod
import org.eclipse.jface.action.IAction
import org.eclipse.ui.PlatformUI
import scala.tools.refactoring.common.TextChange

/**
 * Extracts a series of statements into a new method, passing the needed
 * parameters and return values.
 *
 * The implementation found for example in the JDT offers much more configuration
 * options, for now, we only require the user to provide a name.
 */
class ExtractMethodAction extends RefactoringAction {

  def createRefactoring(selectionStart: Int, selectionEnd: Int, file: ScalaSourceFile) =
    new ExtractMethodScalaIdeRefactoring(selectionStart, selectionEnd, file)

  class ExtractMethodScalaIdeRefactoring(start: Int, end: Int, file: ScalaSourceFile)
    extends ScalaIdeRefactoring("Extract Method", file, start, end) {

    val refactoring = file.withSourceFile((sourceFile, compiler) =>
      new ExtractMethod with GlobalIndexes with NameValidation {
        val global = compiler
        val index = {
          val tree = askLoadedAndTypedTreeForFile(sourceFile).left.get
          global.ask(() => GlobalIndex(tree))
        }
      })()

    var name = "extracted"

    def refactoringParameters = name

//    override def getPages = List(new NewNameWizardPage(
//        s => name = s,
//        refactoring.isValidIdentifier,
//        "extractedMethod",
//        "refactoring_extract_method"))

  }

  override def run(action: IAction) {

    /**
     * Inline extracting is implemented by extracting to a new name
     * that does not exist and then looking up the position of these
     * names in the generated change.
     */
    def doInlineExtraction(change: TextChange, name: String) {
      EditorHelpers.doWithCurrentEditor { editor =>

        EditorHelpers.applyRefactoringChangeToEditor(change, editor)

        val occurrences = {
          val firstOccurrence  = change.text.indexOf(name)
          val secondOccurrence = change.text.indexOf(name, firstOccurrence + 1)
          List(firstOccurrence, secondOccurrence) map (o => (change.from + o, name.length))
        }

        EditorHelpers.enterLinkedModeUi(occurrences, selectFirst = true)
      }
    }

    val shell = PlatformUI.getWorkbench.getActiveWorkbenchWindow.getShell

    createScalaIdeRefactoringForCurrentEditorAndSelection() match {
      case Some(r: ExtractMethodScalaIdeRefactoring) =>

        r.preparationResult.right.map(_ => r.performRefactoring()) match {
          case Right((change: TextChange) :: Nil) =>
            doInlineExtraction(change, r.name)
          case _ =>
            runRefactoring(createWizardForRefactoring(Some(r)), shell)
        }

      case _ => runRefactoring(createWizardForRefactoring(None), shell)
    }
  }


}
