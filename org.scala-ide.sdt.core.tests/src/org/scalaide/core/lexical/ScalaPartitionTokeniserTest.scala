package org.scalaide.core.lexical

import scala.xml.Elem

import org.junit.Test
import org.scalaide.core.internal.lexical.ScalaPartitionRegion
import org.scalaide.core.internal.lexical.ScalaPartitionTokeniser
import org.scalaide.core.internal.lexical.ScalaPartitions._

class ScalaPartitionTokeniserTest {
  import ScalaPartitionTokeniserTest._

  @Test
  def bug2522() {
    // 000000000011111111112222222222333333333344444444445
    // 012345678901234567890123456789012345678901234567890
    """def dev = <div class="menu">...</div>""" ==>
      ((ScalaDefaultContent, 0, 9), (XmlTag, 10, 27), (XmlPcdata, 28, 30), (XmlTag, 31, 36))
  }

  @Test
  def defaultContent() {
    // 000000000011111111112222222222333333333344444444445
    // 012345678901234567890123456789012345678901234567890
    """package foo""" ==> ((ScalaDefaultContent, 0, 10))
  }

  @Test
  def comments() {
    // 000000000011111111112222222222333333333344444444445
    // 012345678901234567890123456789012345678901234567890
    """package /* comment */ foo // comment""" ==>
      ((ScalaDefaultContent, 0, 7), (ScalaMultiLineComment, 8, 20), (ScalaDefaultContent, 21, 25), (ScalaSingleLineComment, 26, 35))

    // 000000000011111111112222222222333333333344444444445
    // 012345678901234567890123456789012345678901234567890
    """/* comment /* nested */ */""" ==>
      ((ScalaMultiLineComment, 0, 25))

    // 000000000011111111112222222222333333333344444444445
    // 012345678901234567890123456789012345678901234567890
    """/** comment /** nested **/ */""" ==>
      ((Scaladoc, 0, 28))

  }

  @Test
  def basicXml() {
    // 000000000011111111112222222222333333333344444444445
    // 012345678901234567890123456789012345678901234567890
    """<foo/>""" ==> ((XmlTag, 0, 5))

    // 000000000011111111112222222222333333333344444444445
    // 012345678901234567890123456789012345678901234567890
    """<![CDATA[ <foo/> ]]>""" ==> ((XmlCdata, 0, 19))

    // 000000000011111111112222222222333333333344444444445
    // 012345678901234567890123456789012345678901234567890
    """<!-- comment -->""" ==> ((XmlComment, 0, 15))

    // 000000000011111111112222222222333333333344444444445
    // 012345678901234567890123456789012345678901234567890
    """<?xml version='1.0' encoding='UTF-8'?>""" ==> ((XmlPi, 0, 37))
  }

  @Test
  def strings() {
    // 000000000011111111112222222222333333333344444444445
    // 012345678901234567890123456789012345678901234567890
    <t>"ordinary string"</t> ==> ((ScalaString, 0, 16));

    // 000000000011111111112222222222333333333344444444445
    // 012345678901234567890123456789012345678901234567890
    <t>"""scala multiline string"""</t> ==> ((ScalaMultiLineString, 0, 27))
  }

  @Test
  def stringInterpolation() {
    // 000000000011111111112222222222333333333344444444445
    // 012345678901234567890123456789012345678901234567890
    <t>s"my name is $name"</t> ==>
      ((ScalaDefaultContent, 0, 0), (ScalaString, 1, 13), (ScalaDefaultContent, 14, 17), (ScalaString, 18, 18))

    // 000000000011111111112222222222333333333344444444445
    // 012345678901234567890123456789012345678901234567890
    <t>s"""my name is $name"""</t> ==>
      ((ScalaDefaultContent, 0, 0), (ScalaMultiLineString, 1, 15), (ScalaDefaultContent, 16, 19), (ScalaMultiLineString, 20, 22))

    // 000000000011111111112222222222333333333344444444445
    // 012345678901234567890123456789012345678901234567890
    """s"my name is ${person.name}"""" ==>
      ((ScalaDefaultContent, 0, 0), (ScalaString, 1, 13), (ScalaDefaultContent, 14, 26), (ScalaString, 27, 27))

    // 0 0 00000001111111111222222222 2 3 33333333344444444445
    // 1 2 34567890123456789012345678 9 0 12345678901234567890
    "s\"\"\"my name is ${person.name}\"\"\"" ==>
      ((ScalaDefaultContent, 0, 0), (ScalaMultiLineString, 1, 15), (ScalaDefaultContent, 16, 28), (ScalaMultiLineString, 29, 31))

  }

  @Test
  def simple_scaladoc() {
    "/**doc*/" ==> ((Scaladoc, 0, 7))
  }

  @Test
  def scaladoc_with_normal_code() {
    "val i = 0; /**doc*/ val j = 0" ==>
      ((ScalaDefaultContent, 0, 10), (Scaladoc, 11, 18), (ScalaDefaultContent, 19, 28))
  }

  @Test
  def scaladoc_with_codeblock() {
    "/**{{{val i = 0}}}*/" ==>
      ((Scaladoc, 0, 2), (ScaladocCodeBlock, 3, 17), (Scaladoc, 18, 19))
  }

  @Test
  def scaladoc_code_block_terminated_early() {
    """/**{{{ "abc" */ val i = 0""" ==>
      ((Scaladoc, 0, 2), (Scaladoc, 3, 14), (ScalaDefaultContent, 15, 24))
  }

  @Test
  def scaladoc_after_invalid_code_block() {
    "/**}}}{{{*/" ==>
      ((Scaladoc, 0, 5), (Scaladoc, 6, 10))
  }

  @Test
  def scaladoc_code_block_with_second_code_block_start() {
    "/**{{{ {{{ }}}*/" ==>
      ((Scaladoc, 0, 2), (ScaladocCodeBlock, 3, 13), (Scaladoc, 14, 15))
  }

  @Test
  def scaladoc_code_block_opening_after_another_block() {
    "/**{{{foo}}}{{{*/" ==>
      ((Scaladoc, 0, 2), (ScaladocCodeBlock, 3, 11), (Scaladoc, 12, 16))
  }
  @Test
  def scaladoc_code_block_closing_after_another_block() {
    "/**{{{foo}}}}}}*/" ==>
      ((Scaladoc, 0, 2), (ScaladocCodeBlock, 3, 11), (Scaladoc, 12, 16))
  }

  @Test
  def multiple_scaladoc_code_blocks() {
    "/**{{{foo}}}{{{foo}}}*/" ==>
      ((Scaladoc, 0, 2), (ScaladocCodeBlock, 3, 11), (ScaladocCodeBlock, 12, 20), (Scaladoc, 21, 22))
  }

  @Test
  def scaladoc_code_block_nested_in_multi_line_comment() {
    "/*/**{{{/**/" ==>
      ((ScalaMultiLineComment, 0, 11))
  }

  @Test
  def char_literal() {
    "'a'" ==> ((ScalaCharacter, 0, 2))
  }

  @Test
  def char_literal_containing_escape_sequence() {
    """'\n'""" ==> ((ScalaCharacter, 0, 3))
  }

  @Test
  def char_literal_containing_unicode_sequence() {
    "'\\u0000'" ==> ((ScalaCharacter, 0, 7))
  }

  @Test
  def char_literal_containing_octal_sequence() {
    """'\123'""" ==> ((ScalaCharacter, 0, 5))
  }

}

object ScalaPartitionTokeniserTest {
  import scala.language.implicitConversions
  implicit def string2PimpedString(from: String): PimpedString = new PimpedString(from)
  implicit def element2PimpedString(from: Elem): PimpedString = new PimpedString(from.text)

  class PimpedString(source: String) {
    def ==>(expectedPartitions: List[(String, Int, Int)]) {
      val actualPartitions = ScalaPartitionTokeniser.tokenise(source)
      val expected = expectedPartitions.map(ScalaPartitionRegion.tupled)
      if (actualPartitions != expected)
        throw new AssertionError("""Expected != Actual
          |Expected: %s
          |Actual:   %s""".stripMargin.format(expected, actualPartitions))
    }
    def ==>(expectedPartitions: (String, Int, Int)*) { this ==> expectedPartitions.toList }
  }

  def partitions(expectedPartitions: (String, Int, Int)*) = expectedPartitions.toList

}
