package org.scalaide.core.internal.lexical

import org.eclipse.jface.text._
import org.eclipse.jface.text.rules._
import scala.collection.mutable.ListBuffer
import org.eclipse.jface.util.PropertyChangeEvent
import org.scalaide.ui.syntax.ScalaSyntaxClasses._
import org.eclipse.jface.preference.IPreferenceStore

class XmlCDATAScanner(val preferenceStore: IPreferenceStore) extends AbstractScalaScanner {

  import XmlCDATAScanner._

  case class RegionToken(getOffset: Int, getLength: Int, token: Token)

  private var regionTokens: List[RegionToken] = Nil

  def setRange(document: IDocument, offset: Int, length: Int) {
    val buffer = new ListBuffer[RegionToken]
    /**
     * Dummy token to sit on top of the stack until the first call to nextToken() removes it
     */
    buffer += RegionToken(0, 0, getToken(Default))
    buffer += RegionToken(offset, CdataStart.length, getToken(XmlCdataBorder))
    if (length > CdataStart.length) {
      if (length < CdataStart.length + CdataEnd.length - 1)
        buffer += RegionToken(offset + CdataStart.length, length - CdataStart.length, getToken(Default))
      else {
        val contentStart = offset + CdataStart.length
        val contentEnd = offset + length - CdataEnd.length - 1
        val contentLength = contentEnd - contentStart + 1
        val endText = document.get(contentEnd + 1, CdataEnd.length)
        if (endText == CdataEnd) {
          if (contentLength > 0)
            buffer += RegionToken(contentStart, contentLength, getToken(Default))
          buffer += RegionToken(contentEnd + 1, CdataEnd.length, getToken(XmlCdataBorder))
        } else
          buffer += RegionToken(contentStart, contentLength + CdataEnd.length, getToken(Default))
      }
    }
    regionTokens = buffer.toList
  }

  def nextToken(): IToken =
    regionTokens match {
      case Nil | (_ :: Nil) => Token.EOF
      case _ :: remainderTokens =>
        regionTokens = remainderTokens
        remainderTokens.head.token
    }

  def getTokenOffset = regionTokens.head.getOffset

  def getTokenLength = regionTokens.head.getLength

}

object XmlCDATAScanner {

  val CdataStart = "<![CDATA["

  val CdataEnd = "]]>"

}
