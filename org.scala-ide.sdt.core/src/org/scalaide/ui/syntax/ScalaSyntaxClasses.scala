package org.scalaide.ui.syntax

import scalariform.lexer.Tokens._
import scalariform.lexer._

object ScalaSyntaxClasses {

  val SingleLineComment = ScalaSyntaxClass("Single-line comment", "syntaxColouring.singleLineComment")
  val MultiLineComment = ScalaSyntaxClass("Multi-line comment", "syntaxColouring.multiLineComment")
  val Scaladoc = ScalaSyntaxClass("Scaladoc comment", "syntaxColouring.scaladoc")
  val ScaladocCodeBlock = ScalaSyntaxClass("Scaladoc code block", "syntaxColouring.scaladocCodeBlock")
  val ScaladocAnnotation = ScalaSyntaxClass("Scaladoc annotation", "syntaxColouring.scaladocAnnotation")
  val ScaladocMacro = ScalaSyntaxClass("Scaladoc macro", "syntaxColouring.scaladocMacro")
  val TaskTag = ScalaSyntaxClass("Task Tag", "syntaxColouring.taskTag")
  val Operator = ScalaSyntaxClass("Operator", "syntaxColouring.operator")
  val Keyword = ScalaSyntaxClass("Keywords (excluding 'return')", "syntaxColouring.keyword")
  val Return = ScalaSyntaxClass("Keyword 'return'", "syntaxColouring.return")
  val StringClass = ScalaSyntaxClass("Strings", "syntaxColouring.string")
  val Character = ScalaSyntaxClass("Characters", "syntaxColouring.character")
  val MultiLineString = ScalaSyntaxClass("Multi-line string", "syntaxColouring.multiLineString")
  val Bracket = ScalaSyntaxClass("Brackets", "syntaxColouring.bracket")
  val Default = ScalaSyntaxClass("Others", "syntaxColouring.default")
  val Symbol = ScalaSyntaxClass("Symbol", "syntaxColouring.symbol")
  val NumberLiteral = ScalaSyntaxClass("Number literals", "syntaxColouring.numberLiteral")
  val EscapeSequence = ScalaSyntaxClass("Escape sequences", "syntaxColouring.escapeSequence")

  val XmlComment = ScalaSyntaxClass("Comments", "syntaxColouring.xml.comment")
  val XmlAttributeValue = ScalaSyntaxClass("Attribute values", "syntaxColouring.xml.attributeValue")
  val XmlAttributeName = ScalaSyntaxClass("Attribute names", "syntaxColouring.xml.attributeName")
  val XmlAttributeEquals = ScalaSyntaxClass("Attribute equal signs", "syntaxColouring.xml.equals")
  val XmlTagDelimiter = ScalaSyntaxClass("Tag delimiters", "syntaxColouring.xml.tagDelimiter")
  val XmlTagName = ScalaSyntaxClass("Tag names", "syntaxColouring.xml.tagName")
  val XmlPi = ScalaSyntaxClass("Processing instructions", "syntaxColouring.xml.processingInstruction")
  val XmlCdataBorder = ScalaSyntaxClass("CDATA delimiters", "syntaxColouring.xml.cdata")

  val Annotation = ScalaSyntaxClass("Annotation", "syntaxColouring.semantic.annotation", canBeDisabled = true)
  val CaseClass = ScalaSyntaxClass("Case class", "syntaxColouring.semantic.caseClass", canBeDisabled = true)
  val CaseObject = ScalaSyntaxClass("Case object", "syntaxColouring.semantic.caseObject", canBeDisabled = true)
  val Class = ScalaSyntaxClass("Class", "syntaxColouring.semantic.class", canBeDisabled = true)
  val LazyLocalVal = ScalaSyntaxClass("Lazy local val", "syntaxColouring.semantic.lazyLocalVal", canBeDisabled = true)
  val LazyTemplateVal = ScalaSyntaxClass("Lazy template val", "syntaxColouring.semantic.lazyTemplateVal", canBeDisabled = true)
  val LocalVal = ScalaSyntaxClass("Local val", "syntaxColouring.semantic.localVal", canBeDisabled = true)
  val LocalVar = ScalaSyntaxClass("Local var", "syntaxColouring.semantic.localVar", canBeDisabled = true)
  val Method = ScalaSyntaxClass("Method", "syntaxColouring.semantic.method", canBeDisabled = true)
  val Object = ScalaSyntaxClass("Object", "syntaxColouring.semantic.object", canBeDisabled = true)
  val Package = ScalaSyntaxClass("Package", "syntaxColouring.semantic.package", canBeDisabled = true)
  val Param = ScalaSyntaxClass("Parameter", "syntaxColouring.semantic.methodParam", canBeDisabled = true)
  val TemplateVal = ScalaSyntaxClass("Template val", "syntaxColouring.semantic.templateVal", canBeDisabled = true)
  val TemplateVar = ScalaSyntaxClass("Template var", "syntaxColouring.semantic.templateVar", canBeDisabled = true)
  val Trait = ScalaSyntaxClass("Trait", "syntaxColouring.semantic.trait", canBeDisabled = true)
  val Type = ScalaSyntaxClass("Type", "syntaxColouring.semantic.type", canBeDisabled = true)
  val TypeParameter = ScalaSyntaxClass("Type parameter", "syntaxColouring.semantic.typeParameter", canBeDisabled = true)
  val IdentifierInInterpolatedString = ScalaSyntaxClass("Identifier in interpolated string", "syntaxColouring.semantic.identifierInInterpolatedString", hasForegroundColour = false, canBeDisabled = true)

  val DynamicSelect = ScalaSyntaxClass("Call of selectDynamic", "syntaxColouring.semantic.selectDynamic", canBeDisabled = true)
  val DynamicUpdate = ScalaSyntaxClass("Call of updateDynamic", "syntaxColouring.semantic.updateDynamic", canBeDisabled = true)
  val DynamicApply = ScalaSyntaxClass("Call of applyDynamic", "syntaxColouring.semantic.applyDynamic", canBeDisabled = true)
  val DynamicApplyNamed = ScalaSyntaxClass("Call of applyDynamicNamed", "syntaxColouring.semantic.applyDynamicNamed", canBeDisabled = true)

  case class Category(name: String, children: List[ScalaSyntaxClass])

  val scalaSyntacticCategory = Category("Syntactic", List(
    Bracket, Keyword, Return, MultiLineString, Operator, Default, StringClass, Character, NumberLiteral, EscapeSequence, Symbol))

  val scalaSemanticCategory = Category("Semantic", List(
    Annotation, CaseClass, CaseObject, Class, LazyLocalVal, LazyTemplateVal,
    LocalVal, LocalVar, Method, Object, Package, Param, TemplateVal, TemplateVar,
    Trait, Type, TypeParameter, IdentifierInInterpolatedString))

  val dynamicCategory = Category("Dynamic", List(
    DynamicSelect, DynamicUpdate, DynamicApply, DynamicApplyNamed))

  val commentsCategory = Category("Comments", List(
    SingleLineComment, MultiLineComment, Scaladoc, ScaladocCodeBlock, ScaladocAnnotation, ScaladocMacro, TaskTag))

  val xmlCategory = Category("XML", List(
    XmlAttributeName, XmlAttributeValue, XmlAttributeEquals, XmlCdataBorder, XmlComment, XmlTagDelimiter,
    XmlTagName, XmlPi))

  val categories = List(scalaSyntacticCategory, scalaSemanticCategory, dynamicCategory, commentsCategory, xmlCategory)

  val AllSyntaxClasses = categories.flatMap(_.children)

  val EnabledSuffix = ".enabled"
  val ForegroundColourSuffix = ".colour"
  val BackgroundColourSuffix = ".backgroundColour"
  val BackgroundColourEnabledSuffix = ".backgroundColourEnabled"
  val BoldSuffix = ".bold"
  val ItalicSuffix = ".italic"
  val UnderlineSuffix = ".underline"

  val EnableSemanticHighlighting = "syntaxColouring.semantic.enabled"

  val UseSyntacticHints = "syntaxColouring.semantic.useSyntacticHints"

  val StrikethroughDeprecated = "syntaxColouring.semantic.strikeDeprecated"

}

object ScalariformToSyntaxClass {

  import org.scalaide.ui.syntax.{ ScalaSyntaxClasses => ssc }

  // TODO: Distinguish inside from outside of CDATA; distinguish XML tag and attribute name

  /**
   * If one wants to tokenize source code by Scalariform, one probably also needs to translate the
   * token to a format the UI-Classes of Eclipse can understand. If this the case than this method
   * should be used.
   *
   * Because Scalariform does not treat all token the way the IDE needs them, for some of them they
   * are replaced with a different kind of token.
   */
  def apply(token: Token): ScalaSyntaxClass = token.tokenType match {
    case LPAREN | RPAREN | LBRACE | RBRACE | LBRACKET | RBRACKET         => ssc.Bracket
    case STRING_LITERAL                                                  => ssc.StringClass
    case TRUE | FALSE | NULL                                             => ssc.Keyword
    case RETURN                                                          => ssc.Return
    case t if t.isKeyword                                                => ssc.Keyword
    case LINE_COMMENT                                                    => ssc.SingleLineComment
    case MULTILINE_COMMENT if token.isScalaDocComment                    => ssc.Scaladoc
    case MULTILINE_COMMENT                                               => ssc.MultiLineComment
    case PLUS | MINUS | STAR | PIPE | TILDE | EXCLAMATION                => ssc.Operator
    case DOT | COMMA | COLON | USCORE | EQUALS | SEMI | LARROW |
         ARROW | SUBTYPE | SUPERTYPE | VIEWBOUND | AT | HASH             => ssc.Operator
    case VARID if Chars.isOperatorPart(token.text(0))                    => ssc.Operator
    case FLOATING_POINT_LITERAL | INTEGER_LITERAL                        => ssc.NumberLiteral
    case SYMBOL_LITERAL                                                  => ssc.Symbol
    case XML_START_OPEN | XML_EMPTY_CLOSE | XML_TAG_CLOSE | XML_END_OPEN => ssc.XmlTagDelimiter
    case XML_NAME                                                        => ssc.XmlTagName
    case XML_ATTR_EQ                                                     => ssc.XmlAttributeEquals
    case XML_PROCESSING_INSTRUCTION                                      => ssc.XmlPi
    case XML_COMMENT                                                     => ssc.XmlComment
    case XML_ATTR_VALUE                                                  => ssc.XmlAttributeValue
    case XML_CDATA                                                       => ssc.XmlCdataBorder
    case XML_UNPARSED | XML_WHITESPACE | XML_PCDATA | VARID | _          => ssc.Default
  }

}
