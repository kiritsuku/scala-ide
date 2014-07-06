package org.scalaide.extensions.saveactions

import org.scalaide.extensions.SaveAction

trait AddMissingOverride extends SaveAction {

  def perform(selection: Selection): Result = {
    import global._

    Nil
  }
}