package org.scalaide.extensions.saveactions

import org.scalaide.core.text.Remove
import org.scalaide.extensions.SaveAction

trait RemoveTrailingWhitespace extends SaveAction {

  def perform(selection: Selection): Result = {
    document.lines flatMap { line =>
      val trimmed = line.trimRight

      if (trimmed.length != line.length)
        Seq(Remove(trimmed.end, line.end))
      else
        Seq()
    }
  }
}