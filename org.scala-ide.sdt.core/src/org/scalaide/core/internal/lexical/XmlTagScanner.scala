package org.scalaide.core.internal.lexical

import org.eclipse.jface.text._
import org.eclipse.jface.text.rules._
import scala.annotation.switch
import scala.annotation.tailrec
import org.eclipse.swt.SWT
import org.scalaide.ui.syntax.ScalaSyntaxClass
import org.scalaide.ui.syntax.ScalaSyntaxClasses._
import org.scalaide.ui.syntax.ScalaSyntaxClasses
import org.eclipse.jface.util.PropertyChangeEvent
import org.eclipse.jface.preference.IPreferenceStore

class XmlTagScanner(val preferenceStore: IPreferenceStore) extends AbstractScalaScanner {
  import XmlTagScanner._

  var pos: Int = -1

  var end: Int = -1

  var document: IDocument = _

  private var afterTagStart = false

  var tokenOffset: Int = -1

  var tokenLength: Int = -1

  def getTokenOffset = tokenOffset

  def getTokenLength = tokenLength

  def setRange(document: IDocument, offset: Int, length: Int) {
    this.document = document
    this.pos = offset
    this.end = offset + length - 1
  }

  private def ch = if (pos > end) Eof else document.getChar(pos)

  private def ch(lookahead: Int) = {
    val offset = pos + lookahead
    if (offset > end || offset < 0)
      Eof
    else
      document.getChar(offset)
  }

  private def accept() { pos += 1 }

  private def accept(n: Int) { pos += n }

  def nextToken(): IToken = {
    val start = pos
    val wasAfterTagStart = afterTagStart
    afterTagStart = false
    val token: IToken = ch match {
      case '<' =>
        accept()
        afterTagStart = true
        getToken(XmlTagDelimiter)
      case Eof => Token.EOF
      case '\'' =>
        accept()
        getXmlAttributeValue('\'')
        getToken(XmlAttributeValue)
      case '"' =>
        accept()
        getXmlAttributeValue('"')
        getToken(XmlAttributeValue)
      case '/' if (ch(1) == '>') =>
        accept(2)
        getToken(XmlTagDelimiter)
      case '>' =>
        accept()
        getToken(XmlTagDelimiter)
      case '=' =>
        accept()
        getToken(XmlAttributeEquals)
      case ' ' | '\r' | '\n' | '\t' =>
        accept()
        getWhitespace
        Token.WHITESPACE
      case _ if wasAfterTagStart =>
        accept()
        getXmlName
        getToken(XmlTagName)
      case _ =>
        accept()
        getXmlName
        getToken(XmlAttributeName)
    }
    tokenOffset = start
    tokenLength = pos - start
    token
  }

  @tailrec
  private def getXmlName(): Unit =
    (ch: @switch) match {
      case ' ' | '\r' | '\n' | '\t' | Eof | '\'' | '\"' | '>' | '/' | '<' | '=' =>
      case _ =>
        accept()
        getXmlName
    }

  @tailrec
  private def getWhitespace(): Unit =
    (ch: @switch) match {
      case ' ' | '\r' | '\n' | '\t' =>
        accept()
        getWhitespace
      case _ =>
    }

  @tailrec
  private def getXmlAttributeValue(quote: Char): Unit =
    ch match {
      case Eof =>
      case `quote` =>
        accept()
      case _ =>
        accept()
        getXmlAttributeValue(quote)
    }


}

object XmlTagScanner {

  final val Eof = '\u001A'

}
