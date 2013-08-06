package scala.tools.eclipse.refactoring

import scala.tools.eclipse.javaelements.ScalaSourceFile
import org.eclipse.jface.action.IAction
import scala.tools.refactoring.implementations.InsertInferredType
import scala.tools.refactoring.analysis.GlobalIndexes

class InsertInferredTypeAction extends RefactoringAction {

  def createRefactoring(selectionStart: Int, selectionEnd: Int, file: ScalaSourceFile) =
    new Refactoring(selectionStart, selectionEnd, file)

  class Refactoring(start: Int, end: Int, file: ScalaSourceFile)
    extends ScalaIdeRefactoring("Insert inferred type", file, start, end) {

    val refactoring = file.withSourceFile((sourceFile, compiler) =>
      new InsertInferredType with GlobalIndexes {
        val global = compiler
        val index = {
          val tree = askLoadedAndTypedTreeForFile(sourceFile).left.get
          global.ask(() => GlobalIndex(tree))
        }
      }
    )()

    def refactoringParameters = ""
  }

//  override def run(action: IAction) {
//
//  }

}