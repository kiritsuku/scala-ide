package org.scalaide.core.internal.jdt.model

import java.io.DataInputStream
import java.io.InputStream
import java.io.IOException
import scala.annotation.switch
import scala.collection.mutable.HashMap
import org.eclipse.core.runtime.QualifiedName
import org.eclipse.core.runtime.content.IContentDescriber
import org.eclipse.core.runtime.content.IContentDescription
import org.scalaide.logging.HasLogger

object ScalaClassFileDescriber extends HasLogger {
  final val JavaMagic = 0xCAFEBABE
  final val ConstantUtf8 = 1
  final val ConstantUnicode = 2
  final val ConstantInteger = 3
  final val ConstantFloat = 4
  final val ConstantLong = 5
  final val ConstantDouble = 6
  final val ConstantClass = 7
  final val ConstantString = 8
  final val ConstantFieldref = 9
  final val ConstantMethodref = 10
  final val ConstantIntfmethodref = 11
  final val ConstantNameandtype = 12

  def isScala(contents : InputStream) : Option[String] = {
    try {
      val in = new DataInputStream(contents)

      if (in.readInt() != JavaMagic)
        return None
      if (in.skipBytes(4) != 4)
        return None

      var sourceFile : String = null
      var isScala = false

      val pool = new HashMap[Int, String]

      val poolSize = in.readUnsignedShort
      var scalaSigIndex = -1
      var scalaIndex = -1
      var sourceFileIndex = -1
      var i = 1
      while (i < poolSize) {
        (in.readByte().toInt: @switch) match {
          case ConstantUtf8 =>
            val str = in.readUTF()
            pool(i) = str
            if (scalaIndex == -1 || scalaSigIndex == -1 || sourceFileIndex == -1) {
              if (scalaIndex == -1 && str == "Scala")
                scalaIndex = i
              else if (scalaSigIndex == -1 && str == "ScalaSig")
                scalaSigIndex = i
              else if (sourceFileIndex == -1 && str == "SourceFile")
                sourceFileIndex = i
            }
        case ConstantUnicode =>
            val toSkip = in.readUnsignedShort()
            if (in.skipBytes(toSkip) != toSkip) return None
          case ConstantClass | ConstantString =>
            if (in.skipBytes(2) != 2) return None
          case ConstantFieldref | ConstantMethodref | ConstantIntfmethodref
             | ConstantNameandtype | ConstantInteger | ConstantFloat =>
            if (in.skipBytes(4) != 4) return None
          case ConstantLong | ConstantDouble =>
            if (in.skipBytes(8) != 8) return None
            i += 1
          case other =>
            logger.debug("Unknown constant pool id: " + other)
            return None
        }
        i += 1
      }

      if (scalaIndex == -1 && scalaSigIndex == -1)
        return None

      if (in.skipBytes(6) != 6)
        return None

      val numInterfaces = in.readUnsignedShort()
      val iToSkip = numInterfaces*2
      if (in.skipBytes(iToSkip) != iToSkip)
        return None

      def skipFieldsOrMethods() : Boolean = {
        val num = in.readUnsignedShort()
        var i = 0
        while (i < num) {
          i += 1
          if (in.skipBytes(6) != 6)
            return false

          val numAttributes = in.readUnsignedShort()
          var j = 0
          while (j < numAttributes) {
            j += 1
            val attrNameIndex = in.readUnsignedShort()
            isScala ||= (attrNameIndex == scalaIndex || attrNameIndex == scalaSigIndex)
            val numToSkip = in.readInt()
            if (in.skipBytes(numToSkip) != numToSkip)
              return false
          }
        }
        true
      }

      // In this binary parser, skipFieldsOrMethods moves the read pointer
      // by skipping fields/methods definitions in the classfile.
      // This (at-first-glance) repetition is thus important.
      if (!skipFieldsOrMethods())
        return None
      if (!skipFieldsOrMethods())
        return None

      val numAttributes = in.readUnsignedShort()
      var j = 0
      while (j < numAttributes) {
        j += 1
        val attrNameIndex = in.readUnsignedShort()
        if (attrNameIndex == sourceFileIndex) {
          in.readInt()
          val index = in.readUnsignedShort()
          sourceFile = pool(index)
          if (isScala)
            return Some(sourceFile)
        } else {
          isScala ||= (attrNameIndex == scalaIndex || attrNameIndex == scalaSigIndex)
          if (isScala && sourceFile != null)
            return Some(sourceFile)
          val numToSkip = in.readInt()
          if (in.skipBytes(numToSkip) != numToSkip)
            return None
        }
      }
      None
    } catch {
      case ex : IOException => None
    }
  }
}

class ScalaClassFileDescriber extends IContentDescriber {
  import IContentDescriber.INVALID
  import IContentDescriber.VALID
  import ScalaClassFileDescriber._

  override def describe(contents : InputStream, description : IContentDescription) : Int =
    if (isScala(contents).isDefined) VALID else INVALID

  override def getSupportedOptions : Array[QualifiedName] = new Array[QualifiedName](0)
}
