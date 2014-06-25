package org.scalaide.extensions
package saveactions

import org.scalaide.core.text.Change

//import org.scalaide.refactoring.internal

trait OrganizeImports extends SaveAction with CompilerSupport {

  def perform(): Seq[Change] = {
    ???
//    new internal.OrganizeImports
  }
}