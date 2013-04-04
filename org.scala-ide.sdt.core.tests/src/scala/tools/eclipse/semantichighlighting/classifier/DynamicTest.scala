package scala.tools.eclipse.semantichighlighting.classifier

import SymbolTypes._
import org.junit._

class DynamicTest extends AbstractSymbolClassifierTest {

  @Test
  def selectDynamic() {
    checkSymbolClassification("""
      object X {
        (new D).field
      }
      import language.dynamics
      class D extends Dynamic {
        def selectDynamic(name: String) = name
      }
      """, """
      object X {
        (new D).$VAR$
      }
      import language.dynamics
      class D extends Dynamic {
        def selectDynamic(name: String) = name
      }
      """,
      Map("VAR" -> TemplateVar))
  }

  @Test
  def updateDynamic() {
    checkSymbolClassification("""
      object X {
        val d = new D
        d.field = 10
        d.field
      }
      import language.dynamics
      class D extends Dynamic {
        var map = Map.empty[String, Any]
        def selectDynamic(name: String) =
          map(name)
        def updateDynamic(name: String)(value: Any) {
          map += name -> value
        }
      }
      """, """
      object X {
        val d = new D
        d.$VAR$ = 10
        d.$VAR$
      }
      import language.dynamics
      class D extends Dynamic {
        var map = Map.empty[String, Any]
        def selectDynamic(name: String) =
          map(name)
        def updateDynamic(name: String)(value: Any) {
          map += name -> value
        }
      }
      """,
      Map("VAR" -> TemplateVar))
  }

  @Test
  def applyDynamic() {
    checkSymbolClassification("""
      object X {
        val d = new D
        d.method(10)
        d(10)
      }
      import language.dynamics
      class D extends Dynamic {
        def applyDynamic(name: String)(value: Any) = name
      }
      """, """
      object X {
        val d = new D
        d.$METH$(10)
        d(10)
      }
      import language.dynamics
      class D extends Dynamic {
        def applyDynamic(name: String)(value: Any) = name
      }
      """,
      Map("METH" -> Method))
  }

  @Test
  def applyDynamicNamed() {
    checkSymbolClassification("""
      object X {
        val d = new D
        d.method(value = 10)
      }
      import language.dynamics
      class D extends Dynamic {
        def applyDynamicNamed(name: String)(value: (String, Any)) = name
      }
      """, """
      object X {
        val d = new D
        d.$METH$($ARG$ = 10)
      }
      import language.dynamics
      class D extends Dynamic {
        def applyDynamicNamed(name: String)(value: (String, Any)) = name
      }
      """,
      Map("METH" -> Method, "ARG" -> Param))
  }

}