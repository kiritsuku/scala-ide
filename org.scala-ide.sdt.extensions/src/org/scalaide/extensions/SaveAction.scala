package org.scalaide.extensions

import scala.tools.refactoring.common.InteractiveScalaCompiler
import scala.tools.refactoring.common.Selections
import org.scalaide.core.text.Change
import org.scalaide.core.text.Document
import scala.reflect.internal.util.SourceFile


trait SimpleSaveAction extends ScalaIdeExtension {
  override type Parameter = Document
  override type Result = Seq[Change]
}

trait SaveAction extends ScalaIdeExtension {

//  private var doc: Document = _
//
//  def document = doc
//
//  def prepare(doc: Document): Unit = {
//    this.doc = doc
//  }
//
//  def enabled: Boolean = {
//    // how to get pref store here?
//    true
//  }
}
