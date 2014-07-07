package org.scalaide.extensions
package saveactions

import org.scalaide.extensions.SaveAction

trait AddMissingOverride extends SaveAction with CompilerSupport {

  def perform(selection: Selection): Result = {
    import global._

    Nil
  }
}