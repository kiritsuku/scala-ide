package org.scalaide.core.ui.indentation

import org.junit.Test
import org.junit.Ignore

class ClassIndentationTests extends ScalaAutoIndentStrategyTest {

  @Test
  def classWithBraces() = """
    class A {^
    """ becomes """
    class A {
      ^
    }
    """ after newline

  @Test
  def classWithoutBraces() = """
    class A^
    """ becomes """
    class A
    ^
    """ after newline

  @Test
  def innerClass() = """
    class A {
      class B {^
    }
    """ becomes """
    class A {
      class B {
        ^
      }
    }
    """ after newline

  @Test
  def classInMethod() = """
    class A {
      def f() {
        class B {^
      }
    }
    """ becomes """
    class A {
      def f() {
        class B {
          ^
        }
      }
    }
    """ after newline

  @Test
  def ctor() = """
    class A(^)
    """ becomes """
    class A(
        ^)
    """ after newline

  @Test
  def ctorWithArg() = """
    class A(i: Int,^)
    """ becomes """
    class A(i: Int,
        ^)
    """ after newline

  @Ignore("wrong indentation; space not deleted")
  @Test
  def extendClass() = """
    class A(i: Int)
    class B(i: Int)^ extends A(i)
    """ becomes """
    class A(i: Int)
    class B(i: Int)
      ^extends A(i)
    """ after newline

  @Ignore("wrong indentation; space not deleted")
  @Test
  def mixinTrait() = """
    trait T
    class A(i: Int)
    class B(i: Int)
      extends A(i)^ with T
    """ becomes """
    trait T
    class A(i: Int)
    class B(i: Int)
      extends A(i)
      ^with T
    """ after newline
}