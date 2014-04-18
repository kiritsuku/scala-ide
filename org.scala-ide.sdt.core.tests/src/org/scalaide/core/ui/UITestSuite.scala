package org.scalaide.core.ui

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.scalaide.core.ui.clipboard.ClipboardOperationTest

@RunWith(classOf[Suite])
@Suite.SuiteClasses(Array(
//  classOf[TestScalaIndenter],
  classOf[BracketAutoEditStrategyTest],
  classOf[CommentAutoEditStrategyTest],
  classOf[LiteralAutoEditStrategyTest],
  classOf[StringAutoEditStrategyTest],
  classOf[MultiLineStringAutoEditStrategyTest],
  classOf[IndentGuideGeneratorTest],
  classOf[MultiLineStringAutoIndentStrategyTest],
  classOf[AutoIndentStrategyTest],
  classOf[ClipboardOperationTest]
))
class UITestSuite