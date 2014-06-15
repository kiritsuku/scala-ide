package org.scalaide.extensions.saveactions

import scala.tools.refactoring.common.InteractiveScalaCompiler
import scala.tools.refactoring.common.Selections

import org.scalaide.core.text.Change
import org.scalaide.core.text.Document

/**
 * Parameterization on:
 * - how to invoke extension
 *   - implicitly on save
 *   - implicitly on text edit
 *   - explicitly invoked
 * - the result of the extension
 *   - text changes for auto edits, save actions, refactorings etc.
 *   - warnings for linter
 */

trait ScalaIdeExtension {
  type Result
}

trait SaveAction
    extends AnyRef
    with ScalaIdeExtension
    with Selections
    with InteractiveScalaCompiler {

  override type Result = Seq[Change]

  private var doc: Document = _

  def document = doc

  def prepare(doc: Document): Unit = {
    this.doc = doc
  }

  def enabled: Boolean = {
    // how to get pref store here?
    true
  }

  def perform(selection: Selection): Result
}
