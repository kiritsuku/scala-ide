package org.scalaide.extensions
package saveactions

import org.scalaide.extensions.SaveAction

trait AddMissingOverride extends SaveAction with CompilerSupport {

  import global._

  class MethodTraverser extends Traverser {
    var meths = Seq[DefDef]()
    override def traverse(t: Tree): Unit = {
      t match {
        case dd @ DefDef(mods, _, _, _, _, _) =>
//          dd.symbol.enclClass
          val p1 = dd.symbol.pos.start
          val p2 = dd.pos.start
          println(s"positions: $p1, $p2")
//          if (mods hasFlag Flag.OVERRIDE)
//            meths +:= dd
        case _ =>
      }
      super.traverse(t)
    }
  }

  def perform() = {
    Nil
  }
}