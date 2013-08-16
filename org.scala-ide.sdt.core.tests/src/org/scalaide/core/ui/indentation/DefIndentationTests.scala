package org.scalaide.core.ui.indentation

import org.junit.Test
import org.junit.Ignore

class DefIndentationTests extends ScalaAutoIndentStrategyTest {

  @Test
  def defWithoutBraces() = """
    class X {
      def y =^
    }
    """ becomes """
    class X {
      def y =
        ^
    }
    """ after newline

  @Test
  def defWithBraces() = """
    class X {
      def y = {^
    }
    """ becomes """
    class X {
      def y = {
        ^
      }
    }
    """ after newline

  @Test
  def defWithTypeAscription() = """
    class X {
      def y: Int =^
    }
    """ becomes """
    class X {
      def y: Int =
        ^
    }
    """ after newline

  @Test
  def defWithTypeAscriptionAndBraces() = """
    class X {
      def y: Int = {^
    }
    """ becomes """
    class X {
      def y: Int = {
        ^
      }
    }
    """ after newline

  @Test
  def defWithParams() = """
    class X {
      def y(i: Int)(j: Int): Int =^
    }
    """ becomes """
    class X {
      def y(i: Int)(j: Int): Int =
        ^
    }
    """ after newline

  @Test
  def defWithTypeParams() = """
    class X {
      def y[A]: A =^
    }
    """ becomes """
    class X {
      def y[A]: A =
        ^
    }
    """ after newline

  @Test
  def defWithFullTypeSignature() = """
    class X {
      def y[A](a: A): A =^
    }
    """ becomes """
    class X {
      def y[A](a: A): A =
        ^
    }
    """ after newline

  @Test
  def defWithFullTypeSignatureAndBraces() = """
    class X {
      def y[A](a: A): A = {^
    }
    """ becomes """
    class X {
      def y[A](a: A): A = {
        ^
      }
    }
    """ after newline

  @Ignore("whitespace not removed")
  @Test
  def noIndentAfterKeywords() = """
    class X {
      private def^ y[A](a: A): A = ???
    }
    """ becomes """
    class X {
      private def
      ^y[A](a: A): A = ???
    }
    """ after newline

  @Ignore("not implemented")
  @Test
  def doubleIndentAfterIdentifier() = """
    class X {
      def y^[A](a: A): A = ???
    }
    """ becomes """
    class X {
      def y
          ^[A](a: A): A = ???
    }
    """ after newline

  @Ignore("not implemented")
  @Test
  def doubleIndentAfterTypeParamList() = """
    class X {
      def y[A]^(a: A): A = ???
    }
    """ becomes """
    class X {
      def y[A]
          ^(a: A): A = ???
    }
    """ after newline

  @Ignore("not implemented")
  @Test
  def doubleIndentAfterParamList() = """
    class X {
      def y[A](a: A)^: A = ???
    }
    """ becomes """
    class X {
      def y[A](a: A)
          ^: A = ???
    }
    """ after newline

  @Ignore("not implemented")
  @Test
  def doubleIndentAfterSecondParamList() = """
    class X {
      def y[A](a1: A)(a2: A)^: A = ???
    }
    """ becomes """
    class X {
      def y[A](a1: A)(a2: A)
          ^: A = ???
    }
    """ after newline

  @Ignore("not implemented; whitespace not removed")
  @Test
  def doubleIndentAfterReturnType() = """
    class X {
      def y[A](a: A): A^ = ???
    }
    """ becomes """
    class X {
      def y[A](a: A): A
          ^= ???
    }
    """ after newline

  @Ignore("not implemented")
  @Test
  def doubleIndentInTypeParamList() = """
    class X {
      def y[^A](a: A): A = ???
    }
    """ becomes """
    class X {
      def y[
          ^A](a: A): A = ???
    }
    """ after newline

  @Test
  def doubleIndentInParamList() = """
    class X {
      def y[A](^a: A): A = ???
    }
    """ becomes """
    class X {
      def y[A](
          ^a: A): A = ???
    }
    """ after newline

  @Test
  def quadIndentInParamListAfterDoubleIndent() = """
    class X {
      def y[A]
          (^a: A): A = ???
    }
    """ becomes """
    class X {
      def y[A]
          (
              ^a: A): A = ???
    }
    """ after newline

  /*
   * Tests that test the indentation when a tab is pressed are currently not
   * possible.
   *
   * This is due to the fact that somewhere in SWT tabs are sent to a different
   * component than other key events. When a user pressed a tab, this key event
   * is sent to [[org.eclipse.jdt.internal.ui.actions.IndentAction]], all other
   * key events are sent to [[scala.tools.eclipse.ui.ScalaAutoIndentStrategy]].
   *
   * Because of this separation two test suites would be required at the moment.
   * A useful goal to achieve is a merge of the two indentation parts.
   * `IndentAction` is nevertheless a Java component and doesn't correctly work
   * for Scala code.
   *
   * `IndentAction` is defined in [[org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor]]
   * in method `createActions`, whereas `ScalaAutoIndentStrategy` is defined
   * in [[scala.tools.eclipse.ScalaSourceViewerConfiguration]].
   */

  @Ignore("not possible to test at the moment")
  @Test
  def doubleIndentBeforeTypeParamList() = """
    class X {
      def y
      ^[A](a: A): A = ???
    }
    """ becomes """
    class X {
      def y
          ^[A](a: A): A = ???
    }
    """ after tab

  @Ignore("not possible to test at the moment")
  @Test
  def doubleIndentBeforeParamList() = """
    class X {
      def y[A]
      ^(a: A): A = ???
    }
    """ becomes """
    class X {
      def y[A]
          ^(a: A): A = ???
    }
    """ after tab

  @Ignore("not possible to test at the moment")
  @Test
  def doubleIndentBeforeReturnType() = """
    class X {
      def y[A](A: A)
      ^: A = ???
    }
    """ becomes """
    class X {
      def y[A](a: A)
          ^: A = ???
    }
    """ after tab

  @Ignore("not possible to test at the moment")
  @Test
  def indentInMethodBody() = """
    class X {
      def y() {
      ^val z = 0
      }
    }
    """ becomes """
    class X {
      def y() {
        ^val z = 0
      }
    }
    """ after tab
}