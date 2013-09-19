package scala.tools.eclipse.refactoring

import scala.tools.eclipse.javaelements.ScalaSourceFile
import scala.tools.refactoring.implementations

class StringToInterpolationRefactoring extends RefactoringActionWithoutWizard {

  def createRefactoring(selectionStart: Int, selectionEnd: Int, file: ScalaSourceFile) =
    new Refactoring(selectionStart, selectionEnd, file)

  class Refactoring(start: Int, end: Int, file: ScalaSourceFile)
    extends ParameterlessScalaIdeRefactoring("StringToInterpolationRefactoring", file, start, end) {

    val refactoring = file.withSourceFile { (sourceFile, compiler) =>
      new implementations.StringToInterpolationRefactoring {
        val global = compiler
      }
    }()
  }
}

class InterpolationToStringRefactoring extends RefactoringActionWithoutWizard {

  def createRefactoring(selectionStart: Int, selectionEnd: Int, file: ScalaSourceFile) =
    new Refactoring(selectionStart, selectionEnd, file)

  class Refactoring(start: Int, end: Int, file: ScalaSourceFile)
    extends ParameterlessScalaIdeRefactoring("InterpolationToStringRefactoring", file, start, end) {

    val refactoring = file.withSourceFile { (sourceFile, compiler) =>
      new implementations.InterpolationToStringRefactoring {
        val global = compiler
      }
    }()
  }
}