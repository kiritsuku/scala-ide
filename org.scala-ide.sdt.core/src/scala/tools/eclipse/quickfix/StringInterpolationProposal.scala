package scala.tools.eclipse.quickfix

import scala.tools.eclipse.refactoring.StringToInterpolationRefactoring
import scala.tools.eclipse.refactoring.InterpolationToStringRefactoring

object StringToInterpolationProposal
  extends ProposalRefactoringActionAdapter(
      new StringToInterpolationRefactoring, "Transform to string interpolation")

object InterpolationToStringProposal
  extends ProposalRefactoringActionAdapter(
      new InterpolationToStringRefactoring, "Transform to normal string")