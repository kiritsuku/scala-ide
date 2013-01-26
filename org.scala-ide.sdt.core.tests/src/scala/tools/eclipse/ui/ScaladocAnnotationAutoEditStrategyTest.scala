package scala.tools.eclipse.ui

import scala.tools.eclipse.testsetup.TestProjectSetup
import scala.tools.eclipse.javaelements.ScalaSourceFile
import scala.tools.eclipse.testsetup.SDTTestUtils
import org.junit.Test

object ScaladocAnnotationAutoEditStrategyTest extends TestProjectSetup("scaladoc-anno") {

  def testFileUnit(path: String, i: Int) {
    val unit = scalaCompilationUnit(path)
    loadTestUnit(unit, i)
  }

  def testSourceUnit(source: String) {
    val path = "generated-runtime-tests/Test"+System.nanoTime()+".scala"
    val s = source.replaceAll("\\^", "")
    val i = source.indexOf("^")
    val p = SDTTestUtils.addFileToProject(project.underlying, "src/"+path, s)
    testFileUnit(path, i)
  }

  def loadTestUnit(unit: ScalaSourceFile, i: Int) {
    reload(unit)
    val strategy = new ScaladocAnnotationAutoEditStrategy {}
//    val tags = strategy.x(unit, i)
    val tags = strategy.findTags(i)
    println(tags)
  }

  def test(actual: String, expected: String) = {

  }
}

class ScaladocAnnotationAutoEditStrategyTest {
  import ScaladocAnnotationAutoEditStrategyTest._

  @Test
  def method_params() {
    val source =
      """
      object X {
        /**
         * ^
         */
        def meth[A, B, C](a: A, b: B)(c: C) = 0
      }
      """
    val expected =
      """
      object X {
        /**
         * ^
         * @param a
         * @param b
         * @param c
         * @tparam A
         * @tparam B
         * @tparam C
         * @return
         */
        def meth[A, B, C](a: A, b: B)(c: C) = 0
      }
      """
    testSourceUnit(source)
  }

  @Test
  def no_return_param_for_unit() {
    val source =
      """
      object X {
        /**
         * ^
         */
        def meth = {}
      }
      """
    testSourceUnit(source)
  }

  @Test
  def class_params() {
    val source =
      """
      /**
       * ^
       */
      class X[A, B, C](a: A, b: B)(c: C) {
        def this(a: A) = this(a, null.asInstanceOf[B])(null.asInstanceOf[C])
      }
      """
    testSourceUnit(source)
  }

  @Test
  def no_ctor_tag_for_parameterless_ctor() {
    val source =
      """
      /**^
      class X
      """
    testSourceUnit(source)
  }

  @Test
  def type_params() {
    val source =
      """
      object X {
        /**
         * ^
         */
        type T[A, B] = Either[A, B]
      }
      """
    testSourceUnit(source)
  }

}