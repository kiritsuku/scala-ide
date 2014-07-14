package org.scalaide.extensions
package saveactions

import org.scalaide.core.text.Add

trait AddNewLineAtEndOfFile extends SaveAction with DocumentSupport {

  def perform() =
    if (document.text.last == '\n')
      Seq(Add(document.length, "\n"))
    else
      Seq()
}