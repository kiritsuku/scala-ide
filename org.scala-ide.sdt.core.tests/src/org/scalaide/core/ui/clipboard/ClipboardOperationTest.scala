package org.scalaide.core.ui.clipboard

import org.junit.Test
import org.scalaide.core.ui.CompilerSupport
import org.scalaide.core.ui.TextEditTests
import org.scalaide.refactoring.internal.clipboard.ClipboardOperation
import org.scalaide.refactoring.internal.EditorHelpers
import org.eclipse.jface.text.TextSelection
import scala.reflect.internal.util.BatchSourceFile
import org.eclipse.jface.text.Document
import scala.reflect.io.PlainFile
import org.scalaide.core.ScalaPlugin
import scala.reflect.io.AbstractFile
import org.eclipse.text.edits.MultiTextEdit
import org.eclipse.text.edits.RangeMarker
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.ITextSelection
import org.scalaide.util.internal.eclipse.FileUtils
import org.eclipse.core.resources.IFile
import org.eclipse.text.edits.ReplaceEdit
import org.eclipse.ltk.core.refactoring.TextFileChange
import org.eclipse.jface.text.IRegion
import scala.tools.refactoring.common.TextChange

class ClipboardOperationTest extends TextEditTests with CompilerSupport {

  case class CopyPaste(rawSource: String) extends Operation {
    def execute() = {
      val selectionStart = rawSource.indexOf("[[")
      val selectionEnd = rawSource.indexOf("]]")

      require(selectionStart >= 0 && selectionEnd > 0 && selectionStart < selectionEnd,
          "please specify a selection range by surrounding it with [[ and ]]")

      val selection = rawSource.substring(selectionStart + 2, selectionEnd)
      val source = rawSource.replaceAll("""\[\[|\]\]""", "")
      val sourceFile = createLoadedSourceFile(source)

      val op = new ClipboardOperation(compiler)
      val copyContent = op.copyContent(sourceFile, new TextSelection(new Document(source), selectionStart, selection.length()))

      val ts = new TextSelection(doc, caretOffset, 0)
      val docSourceFile = createLoadedSourceFile(doc.get())

      val changes = op.pasteContent(docSourceFile, ts, copyContent)

//      /*EditorHelpers.*/applyChangesToFileWhileKeepingSelection(
//        doc, ts, docSourceFile.file, changes)
    }
  }

  @Test
  def autoImportSingleClass() = """
    object Y {
      ^
    }
  """ becomes """
    import scala.collection.mutable.ListBuffer

    object Y {
      val lb = ListBuffer(1)^
    }
  """ after CopyPaste("""
    import scala.collection.mutable.ListBuffer

    object X {
      [[val lb = ListBuffer(1)]]
    }
  """)

  @Test
  def noAutoImportWhenImportAlreadyExists() = """
    object Y {
      ^
    }
  """ becomes """
    import scala.collection.mutable.ListBuffer

    object Y {
      val lb = ListBuffer(1)^
    }
  """ after CopyPaste("""
    import scala.collection.mutable.ListBuffer

    object X {
      [[val lb = ListBuffer(1)]]
    }
  """)


}