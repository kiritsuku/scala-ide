package org.scalaide.core.ui.indentation

import org.junit.Test

class VariousIndentTests extends ScalaAutoIndentStrategyTest {

  @Test
  def traitWithBraces() = """
    trait X {^
    """ becomes """
    trait X {
      ^
    }
    """ after newline

  @Test
  def objectWithBraces() =  """
    object X {^
    """ becomes """
    object X {
      ^
    }
    """ after newline

  @Test
  def defaultIndentAfterLinebreak() = """
    class X {
    ^
    }
    """ becomes """
    class X {
    $
      ^
    }
    """ after newline

  @Test
  def genericsIndent() = """
    class X {
      val xs = List[X]^
    }
    """ becomes """
    class X {
      val xs = List[X]
      ^
    }
    """ after newline

  @Test
  def genericsIndentOverMultipleLines() = """
    class X {
      val xs = List[^
    }
    """ becomes """
    class X {
      val xs = List[
        ^
    }
    """ after newline

  @Test
  def afterFunctionCall() = """
    class X {
      y()^
    }
    """ becomes """
    class X {
      y()
      ^
    }
    """ after newline
}