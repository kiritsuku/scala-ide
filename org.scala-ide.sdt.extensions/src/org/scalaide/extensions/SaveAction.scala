package org.scalaide.extensions

import scala.tools.refactoring.common.InteractiveScalaCompiler
import scala.tools.refactoring.common.Selections
import org.scalaide.core.text.Change
import org.scalaide.core.text.Document
import scala.reflect.internal.util.SourceFile

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
  type Parameter
  type Result

  def perform(param: Parameter): Result
}

trait SimpleSaveAction extends ScalaIdeExtension {
  override type Parameter = Document
  override type Result = Seq[Change]
}

trait SaveAction
    extends AnyRef
    with ScalaIdeExtension
    with Selections
    with InteractiveScalaCompiler {

  override type Result = Seq[Change]
  override type Parameter = Selection

  private var doc: Document = _

  def document = doc

  def prepare(doc: Document): Unit = {
    this.doc = doc
  }

  def enabled: Boolean = {
    // how to get pref store here?
    true
  }
}
