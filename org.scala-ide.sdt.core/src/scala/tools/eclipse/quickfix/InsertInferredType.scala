package scala.tools.eclipse.quickfix

import scala.tools.eclipse.refactoring.InsertInferredTypeAction

object InsertInferredType
  extends ProposalRefactoringActionAdapter(
    new InsertInferredTypeAction, "Insert inferred type")