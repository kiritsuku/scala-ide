package org.scalaide.core

import java.lang.reflect.Modifier

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.control.Exception

import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runner.Runner
import org.junit.runner.notification.{Failure => JFailure}
import org.junit.runner.notification.RunNotifier

/**
 * Represents a single test with a given `name`, a `testFunction` that
 * executes the test code and `ignore` which needs to be `true` when the test
 * code should not be executed.
 */
final case class ScalaIdeTest(name: String, testFunction: () => Unit, ignore: Boolean)

/**
 * Base behavior to allow the following DSL for tests:
 *
 * {{{
 * class Tests extends ScalaIdeTestSuite {
 *   test("a name for thet test") {
 *     // the test code
 *   }
 *   ignore("this test is ignored") {
 *     ??? // never executed
 *   }
 * }
 * }}}
 *
 * The tests that are registered within this class are executed by the
 * `ScalaIdeRunner`.
 */
@RunWith(classOf[ScalaIdeRunner])
abstract class ScalaIdeTestSuite {

  private var _tests = IndexedSeq[ScalaIdeTest]()

  /**
   * All the available tests. These are the tests that were registered during
   * construction of this test suite.
   */
  final def tests = _tests

  /**
   * Registers a test, in order to execute it later by the test suite.
   */
  final def test(name: String)(f: => Unit): Unit =
    _tests :+= ScalaIdeTest(name, f _, ignore = false)

  /**
   * Registers a test but don't execute it.
   */
  final def ignore(name: String)(f: => Unit): Unit =
    _tests :+= ScalaIdeTest(name, f _, ignore = true)
}

final class ScalaIdeRunner(suiteClass: Class[ScalaIdeTestSuite]) extends Runner {

  require(canInstantiate, "A public no-arg constructor is required by the ScalaIdeTestSuite")

  private val suite = suiteClass.newInstance

  override def run(rn: RunNotifier): Unit = {
    for (ScalaIdeTest(name, f, ignore) <- suite.tests) {
      val d = Description.createTestDescription(suite.getClass(), name)
      if (ignore)
        rn.fireTestIgnored(d)
      else {
        rn.fireTestStarted(d)
        Try { f() } match {
          case Success(_) => rn.fireTestFinished(d)
          case Failure(e) => rn.fireTestFailure(new JFailure(d, e))
        }
      }
    }
  }

  override def testCount(): Int =
    suite.tests.size

  override def getDescription(): Description = {
    val description = Description.createSuiteDescription(suite.getClass())

    for (name <- suite.tests.map(_.name))
      description.addChild(Description.createTestDescription(suite.getClass(), name))

    description
  }

  private def canInstantiate = {
    Exception.failAsValue(classOf[NoSuchMethodException])(false) {
      val ctor = suiteClass.getConstructor(new Array[Class[A] forSome { type A }](0): _*)
      Modifier.isPublic(ctor.getModifiers)
    }
  }
}