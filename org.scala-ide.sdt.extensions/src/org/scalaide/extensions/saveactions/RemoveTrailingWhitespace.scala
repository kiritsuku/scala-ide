package org.scalaide.extensions.saveactions

import org.scalaide.core.text.Remove

trait RemoveTrailingWhitespace extends SaveAction {

  def perform(selection: Selection): Result = {
    document.lines flatMap { line =>
      val trimmed = line.trimRight

      if (trimmed.length != line.length)
        Seq(Remove(trimmed.end, line.end-trimmed.end))
      else
        Seq()
    }
  }
}