package scala.tools.eclipse.ui.indentation

import org.junit.Test

class VariousIndentTests extends ScalaAutoIndentStrategyTest {

  @Test
  def classIndent() {
    """
    class X {^
    """ becomes
    """
    class X {
      ^
    }
    """ after linebreak
  }

  @Test
  def traitIndent() {
    """
    trait X {^
    """ becomes
    """
    trait X {
      ^
    }
    """ after linebreak
  }

  @Test
  def objectIndent() {
    """
    object X {^
    """ becomes
    """
    object X {
      ^
    }
    """ after linebreak
  }

  @Test
  def defaultIndentAfterLinebreak() {
    """
    class X {
    ^
    }
    """ becomes
    """
    class X {
    $
      ^
    }
    """ after linebreak
  }

  // ???

  @Test
  def genericsIndent() {
    """
    class X {
      val xs = List[X]^
    }
    """ becomes
    """
    class X {
      val xs = List[X]
      ^
    }
    """ after linebreak
  }

  @Test
  def genericsIndentOverMultipleLines() {
    """
    class X {
      val xs = List[^
    }
    """ becomes
    """
    class X {
      val xs = List[
        ^
    }
    """ after linebreak
  }

  @Test
  def afterFunctionCall() {
    """
    class X {
      y()^
    }
    """ becomes
    """
    class X {
      y()
      ^
    }
    """ after linebreak
  }
}