package org.scalaide.ui.internal.editor.decorators.semantichighlighting

import org.scalaide.ui.syntax.ScalaSyntaxClasses
import org.scalaide.core.internal.decorators.semantichighlighting.Position
import org.scalaide.core.internal.decorators.semantichighlighting.classifier.SymbolTypes
import org.scalaide.core.internal.decorators.semantichighlighting.classifier.SymbolTypes._
import org.eclipse.jface.text.TextAttribute
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyleRange
import org.eclipse.swt.graphics.Font
import org.scalaide.ui.syntax.ScalaSyntaxClass

case class HighlightingStyle(styledTextAttribute: TextAttribute, enabled: Boolean, unstyledTextAttribute: TextAttribute, deprecation: DeprecationStyle, interpolation: StringInterpolationStyle) {
  val ta = if (enabled) styledTextAttribute else unstyledTextAttribute
  lazy val deprecatedTextAttribute: TextAttribute = deprecation.buildTextAttribute(ta)
  lazy val interpolationTextAttribute: TextAttribute = interpolation.buildTextAttribute(ta)

  def style(position: Position): StyleRange = {
    val textAttribute = if (position.deprecated) deprecatedTextAttribute else if (position.inInterpolatedString) interpolationTextAttribute else ta
    val s = textAttribute.getStyle()
    val fontStyle = s & (SWT.ITALIC | SWT.BOLD | SWT.NORMAL)
    val styleRange = new StyleRange(position.getOffset(), position.getLength(), textAttribute.getForeground(), textAttribute.getBackground(), fontStyle)
    styleRange.strikeout = (s & TextAttribute.STRIKETHROUGH) != 0
    styleRange.underline = (s & TextAttribute.UNDERLINE) != 0
    styleRange
  }
}

object HighlightingStyle {
  def apply(preferences: Preferences, symbolType: SymbolTypes.SymbolType): HighlightingStyle = {
    val syntaxClass = symbolTypeToSyntaxClass(symbolType)
    val enabled = syntaxClass.getStyleInfo(preferences.store).enabled
    val deprecation = DeprecationStyle(preferences.isStrikethroughDeprecatedDecorationEnabled())
    val stringInterpolation = StringInterpolationStyle(preferences.isInterpolatedStringCodeDecorationEnabled(), preferences.interpolatedStringTextAttribute())
    HighlightingStyle(syntaxClass.getTextAttribute(preferences.store), enabled, ScalaSyntaxClasses.Default.getTextAttribute(preferences.store), deprecation, stringInterpolation)
  }

  def symbolTypeToSyntaxClass(symbolType: SymbolTypes.SymbolType): ScalaSyntaxClass = {
    import org.scalaide.ui.syntax.{ScalaSyntaxClasses => ssc}
    symbolType match {
      case Annotation        => ssc.Annotation
      case CaseClass         => ssc.CaseClass
      case CaseObject        => ssc.CaseObject
      case Class             => ssc.Class
      case LazyLocalVal      => ssc.LazyLocalVal
      case LazyTemplateVal   => ssc.LazyTemplateVal
      case LocalVal          => ssc.LocalVal
      case LocalVar          => ssc.LocalVar
      case Method            => ssc.Method
      case Param             => ssc.Param
      case Object            => ssc.Object
      case Package           => ssc.Package
      case TemplateVar       => ssc.TemplateVar
      case TemplateVal       => ssc.TemplateVal
      case Trait             => ssc.Trait
      case Type              => ssc.Type
      case TypeParameter     => ssc.TypeParameter
      case DynamicSelect     => ssc.DynamicSelect
      case DynamicUpdate     => ssc.DynamicUpdate
      case DynamicApply      => ssc.DynamicApply
      case DynamicApplyNamed => ssc.DynamicApplyNamed
    }
  }
}

case class DeprecationStyle(enabled: Boolean) {
  def buildTextAttribute(ta: TextAttribute) = if (enabled) new TextAttribute(ta.getForeground, ta.getBackground, ta.getStyle | TextAttribute.STRIKETHROUGH, ta.getFont) else ta
}

case class StringInterpolationStyle(enabled: Boolean, modifier: TextAttribute) {
  def buildTextAttribute(ta: TextAttribute) = if (enabled) new TextAttribute(ta.getForeground, Option(ta.getBackground).getOrElse(modifier.getBackground), ta.getStyle | modifier.getStyle, ta.getFont) else ta
}
