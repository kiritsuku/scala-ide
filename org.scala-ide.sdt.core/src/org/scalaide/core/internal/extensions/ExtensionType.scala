package org.scalaide.core.internal.extensions

import org.scalaide.core.internal.text.TextDocument
import org.scalaide.util.internal.eclipse.EditorUtils
import org.scalaide.core.text.Change
import org.scalaide.logging.HasLogger
import org.scalaide.core.text.Document

trait ExtensionType

object ExtensionType extends HasLogger {

  type ?=>[A, B] = PartialFunction[A, B]

  def withDocument[A](f: Document => A): A = {
    // TODO implement this
    val udoc = ???
    val doc = new TextDocument(udoc)
    f(doc)
  }

  val DocumentType: ExtensionBuilder#Creator ?=> Seq[Change] = {
    case creator: DocumentSupportBuilder.DocumentSupportCreator =>
      withDocument { doc =>
        val ext = creator.create(doc)
        ext.perform()
      }
  }

  val CompilerType: ExtensionBuilder#Creator ?=> Seq[Change] = {
    case creator: CompilerSupportBuilder.CompilerSupportCreator =>
      EditorUtils.withScalaSourceFileAndSelection { (ssf, sel) =>
        ssf.withSourceFile { (sf, compiler) =>
          import compiler._

          val r = new Response[Tree]
          askLoadedTyped(sf, r)
          r.get match {
            case Left(t) =>
              val c = creator.create(compiler)(t, sf, sel.getOffset(), sel.getOffset()+sel.getLength())
              c.perform()
            case Right(e) =>
              logger.error(
                  s"An error occurred while trying to get tree of file '${sf.file.name}'."+
                  s" Aborting save action '${creator.extensionName}'", e)
              Seq()
          }
        }
      }.toSeq.flatten
  }

}