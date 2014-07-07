package org.scalaide.extensions
package saveactions

import org.scalaide.core.text.Add
import org.scalaide.extensions.SaveAction
import org.scalaide.core.text.Document

trait AddNewLineAtEndOfFile extends SaveAction with DocumentSupport {

  def perform(document: Document): Result =
    if (document.text.last == '\n')
      Seq(Add(document.length, "\n"))
    else
      Seq()
}