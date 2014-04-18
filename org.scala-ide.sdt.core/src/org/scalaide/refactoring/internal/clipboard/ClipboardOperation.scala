package org.scalaide.refactoring.internal.clipboard

import scala.tools.refactoring.common.Selections
import scala.tools.refactoring.common.TreeExtractors
import scala.tools.refactoring.analysis.CompilationUnitDependencies
import scala.tools.refactoring.common.InteractiveScalaCompiler
import scala.tools.refactoring.common.TextChange
import scala.reflect.io.AbstractFile
import org.eclipse.ui.texteditor.ITextEditor
import org.eclipse.jface.text.ITextSelection
import org.scalaide.core.compiler.ScalaPresentationCompiler
import scala.reflect.internal.util.SourceFile
import org.eclipse.jface.text.IDocument
import scala.tools.refactoring.implementations.AddImportStatement

class ClipboardOperation(val global: ScalaPresentationCompiler)
    extends AnyRef
    with InteractiveScalaCompiler
    with Selections
    with TreeExtractors
    with CompilationUnitDependencies {

  outer =>

  import global._

  case class CopyContent(selection: String, importNames: List[String])

  def copyContent(sourceFile: SourceFile, ts: ITextSelection): CopyContent = {
    val from = ts.getOffset()
    val to = ts.getOffset()+ts.getLength()
    val fs = FileSelection(sourceFile.file, compilationUnitOfFile(sourceFile.file).get.body, from, to)

    val deps = fs.inboundDeps
    val symbolsToImport = deps filter {
      case s if s.isModule && !s.isPackage => true
      case _ => false
    }
    val importNames = symbolsToImport map (_.fullName)

    CopyContent(ts.getText(), importNames)
  }

//  def copyContent(sourceFile: SourceFile, ts: ITextSelection): List[TextChange] = {
//    val from = ts.getOffset()
//    val to = ts.getOffset()+ts.getLength()
//    val sourceChange = TextChange(sourceFile, from, to, ts.getText())
//    val fs = FileSelection(sourceFile.file, compilationUnitOfFile(sourceFile.file).get.body, from, to)
//
//    val deps = fs.inboundDeps
//    val symbolsToImport = deps filter {
//      case s if s.isModule && !s.isPackage => true
//      case _ => false
//    }
//    val importNames = symbolsToImport map (_.fullName)
//    val imports = importNames flatMap { name =>
//      val i = new AddImportStatement { val global = outer.global }
//      i.addImport(sourceFile.file, name)
//    }
//
//    sourceChange +: imports
//  }

  def pasteContent(sourceFile: SourceFile, ts: ITextSelection, content: CopyContent): List[TextChange] = {
    val from = ts.getOffset()
    val to = ts.getOffset()+ts.getLength()
    val sourceChange = TextChange(sourceFile, from, to, content.selection)
    val imports = content.importNames flatMap { name =>
      val i = new AddImportStatement { val global = outer.global }
      i.addImport(sourceFile.file, name)
    }

    // TODO find existing imports

    sourceChange +: imports
  }

}