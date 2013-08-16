package scala.tools.eclipse.ui.indentation

import org.junit.Test
import org.junit.Ignore

class DefIndentationTests extends ScalaAutoIndentStrategyTest {

  @Test
  def defWithoutBraces() {
    """
    class X {
      def y =^
    }
    """ becomes
    """
    class X {
      def y =
        ^
    }
    """ after linebreak
  }

  @Test
  def defWithBraces() {
    """
    class X {
      def y = {^
    }
    """ becomes
    """
    class X {
      def y = {
        ^
      }
    }
    """ after linebreak
  }

  @Test
  def defWithTypeAscription() {
    """
    class X {
      def y: Int =^
    }
    """ becomes
    """
    class X {
      def y: Int =
        ^
    }
    """ after linebreak
  }

  @Test
  def defWithTypeAscriptionAndBraces() {
    """
    class X {
      def y: Int = {^
    }
    """ becomes
    """
    class X {
      def y: Int = {
        ^
      }
    }
    """ after linebreak
  }

  @Test
  def defWithParams() {
    """
    class X {
      def y(i: Int)(j: Int): Int =^
    }
    """ becomes
    """
    class X {
      def y(i: Int)(j: Int): Int =
        ^
    }
    """ after linebreak
  }

  @Test
  def defWithTypeParams() {
    """
    class X {
      def y[A]: A =^
    }
    """ becomes
    """
    class X {
      def y[A]: A =
        ^
    }
    """ after linebreak
  }

  @Test
  def defWithFullTypeSignature() {
    """
    class X {
      def y[A](a: A): A =^
    }
    """ becomes
    """
    class X {
      def y[A](a: A): A =
        ^
    }
    """ after linebreak
  }

  @Test
  def defWithFullTypeSignatureAndBraces() {
    """
    class X {
      def y[A](a: A): A = {^
    }
    """ becomes
    """
    class X {
      def y[A](a: A): A = {
        ^
      }
    }
    """ after linebreak
  }

  @Ignore
  @Test
  def noIndentAfterKeywords() {
    """
    class X {
      private def^ y[A](a: A): A = ???
    }
    """ becomes
    """
    class X {
      private def
      ^y[A](a: A): A = ???
    }
    """ after linebreak
  }

  @Ignore
  @Test
  def doubleIndentAfterIdentifier() {
    """
    class X {
      def y^[A](a: A): A = ???
    }
    """ becomes
    """
    class X {
      def y
          ^[A](a: A): A = ???
    }
    """ after linebreak
  }

  @Ignore
  @Test
  def doubleIndentAfterTypeParamList() {
    """
    class X {
      def y[A]^(a: A): A = ???
    }
    """ becomes
    """
    class X {
      def y[A]
          ^(a: A): A = ???
    }
    """ after linebreak
  }

  @Ignore
  @Test
  def doubleIndentAfterParamList() {
    """
    class X {
      def y[A](a: A)^: A = ???
    }
    """ becomes
    """
    class X {
      def y[A](a: A)
          ^: A = ???
    }
    """ after linebreak
  }

  @Ignore
  @Test
  def doubleIndentAfterSecondParamList() {
    """
    class X {
      def y[A](a1: A)(a2: A)^: A = ???
    }
    """ becomes
    """
    class X {
      def y[A](a1: A)(a2: A)
          ^: A = ???
    }
    """ after linebreak
  }

  @Ignore
  @Test
  def doubleIndentAfterReturnType() {
    """
    class X {
      def y[A](a: A): A^ = ???
    }
    """ becomes
    """
    class X {
      def y[A](a: A): A
          ^= ???
    }
    """ after linebreak
  }

  @Ignore
  @Test
  def doubleIndentInTypeParamList() {
    """
    class X {
      def y[^A](a: A): A = ???
    }
    """ becomes
    """
    class X {
      def y[
          ^A](a: A): A = ???
    }
    """ after linebreak
  }

  @Test
  def doubleIndentInParamList() {
    """
    class X {
      def y[A](^a: A): A = ???
    }
    """ becomes
    """
    class X {
      def y[A](
          ^a: A): A = ???
    }
    """ after linebreak
  }

  @Test
  def quadIndentInParamListAfterDoubleIndent() {
    """
    class X {
      def y[A]
          (^a: A): A = ???
    }
    """ becomes
    """
    class X {
      def y[A]
          (
              ^a: A): A = ???
    }
    """ after linebreak
  }

  @Ignore("tabs are recognized as tabs, but spaces should be inserted")
  @Test
  def doubleIndentBeforeTypeParamList() {
    """
    class X {
      def y
      ^[A](a: A): A = ???
    }
    """ becomes
    """
    class X {
      def y
          ^[A](a: A): A = ???
    }
    """ after tab
  }

  @Ignore("tabs are recognized as tabs, but spaces should be inserted")
  @Test
  def doubleIndentBeforeParamList() {
    """
    class X {
      def y[A]
      ^(a: A): A = ???
    }
    """ becomes
    """
    class X {
      def y[A]
          ^(a: A): A = ???
    }
    """ after tab
  }

  @Ignore("tabs are recognized as tabs, but spaces should be inserted")
  @Test
  def doubleIndentBeforeReturnType() {
    """
    class X {
      def y[A](A: A)
      ^: A = ???
    }
    """ becomes
    """
    class X {
      def y[A](a: A)
          ^: A = ???
    }
    """ after tab
  }

//  @Ignore("tabs are recognized as tabs, but spaces should be inserted")
  @Test
  def indentInMethodBody() {
    """
    class X {
      def y() {
      ^val z = 0
      }
    }
    """ becomes
    """
    class X {
      def y() {
        val z = 0
      }
    }
    """ after tab
  }
}