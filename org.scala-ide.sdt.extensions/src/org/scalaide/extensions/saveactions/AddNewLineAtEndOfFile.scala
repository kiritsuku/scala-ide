package org.scalaide.extensions.saveactions

import org.scalaide.core.text.Add
import org.scalaide.extensions.SaveAction

trait AddNewLineAtEndOfFile extends SaveAction {

  def perform(selection: Selection): Result =
    if (document.text.last == '\n')
      Seq(Add(document.length, "\n"))
    else
      Seq()
}