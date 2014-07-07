package org.scalaide.extensions

import org.scalaide.core.text.Document
import org.scalaide.core.text.Change
import scala.tools.refactoring.common.Selections
import scala.tools.refactoring.common.InteractiveScalaCompiler

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

trait DocumentSupport extends ScalaIdeExtension {
  override type Parameter = Document
  override type Result = Seq[Change]
}

trait CompilerSupport extends ScalaIdeExtension with Selections with InteractiveScalaCompiler {
  override type Parameter = Selection
  override type Result = Seq[Change]
}