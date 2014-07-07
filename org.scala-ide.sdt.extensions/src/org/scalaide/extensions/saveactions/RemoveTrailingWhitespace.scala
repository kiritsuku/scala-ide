package org.scalaide.extensions
package saveactions

import org.scalaide.core.text.Remove
import org.scalaide.core.text.Document

trait RemoveTrailingWhitespace extends SaveAction with DocumentSupport {

  def perform(document: Document): Result = {
    document.lines flatMap { line =>
      val trimmed = line.trimRight

      if (trimmed.length != line.length)
        Seq(Remove(trimmed.end, line.end))
      else
        Seq()
    }
  }
}