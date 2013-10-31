package scala.tools.eclipse.ui

import indentation._

import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(classOf[Suite])
@Suite.SuiteClasses(Array(
  classOf[DefIndentationTests],
  classOf[ClassIndentationTests],
  classOf[VariousIndentTests],
  classOf[BracketAutoEditStrategyTest],
  classOf[CommentAutoEditStrategyTest],
  classOf[LiteralAutoEditStrategyTest],
  classOf[StringAutoEditStrategyTest],
  classOf[MultiLineStringAutoEditStrategyTest]))
class UITestSuite