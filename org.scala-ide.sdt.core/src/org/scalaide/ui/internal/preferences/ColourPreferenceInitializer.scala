package org.scalaide.ui.internal.preferences

import org.scalaide.core.ScalaPlugin
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.resource.StringConverter
import org.eclipse.swt.graphics.RGB
import org.scalaide.ui.syntax.ScalaSyntaxClasses._
import org.scalaide.ui.syntax.ScalaSyntaxClass

class ColourPreferenceInitializer extends AbstractPreferenceInitializer {

  override def initializeDefaultPreferences() {
    if (!ScalaPlugin.plugin.headlessMode) {
      doInitializeDefaultPreferences()
    }
  }

  private def doInitializeDefaultPreferences() {
    val scalaPrefStore = ScalaPlugin.prefStore

    scalaPrefStore.setDefault(EnableSemanticHighlighting, true)
    scalaPrefStore.setDefault(UseSyntacticHints, true)
    scalaPrefStore.setDefault(StrikethroughDeprecated, true)

    setDefaultsForSyntaxClasses(scalaPrefStore)
  }

  private def setDefaultsForSyntaxClasses(implicit scalaPrefStore: IPreferenceStore) {
    // Scala syntactic
    setDefaultsForSyntaxClass(SingleLineComment, new RGB(63, 127, 95))
    setDefaultsForSyntaxClass(MultiLineComment, new RGB(63, 127, 95))
    setDefaultsForSyntaxClass(Scaladoc, new RGB(63, 95, 191))
    setDefaultsForSyntaxClass(ScaladocCodeBlock, new RGB(63, 95, 191), italic = true)
    setDefaultsForSyntaxClass(ScaladocAnnotation, new RGB(63, 95, 191), bold = true)
    setDefaultsForSyntaxClass(ScaladocMacro, new RGB(63, 95, 191), bold = true)
    setDefaultsForSyntaxClass(TaskTag, new RGB(127, 159, 191), bold = true)
    setDefaultsForSyntaxClass(Keyword, new RGB(127, 0, 85), bold = true)
    setDefaultsForSyntaxClass(StringClass, new RGB(42, 0, 255))
    setDefaultsForSyntaxClass(Character, new RGB(42, 0, 255))
    setDefaultsForSyntaxClass(MultiLineString, new RGB(42, 0, 255))
    setDefaultsForSyntaxClass(Default, new RGB(0, 0, 0))
    setDefaultsForSyntaxClass(Operator, new RGB(0, 0, 0))
    setDefaultsForSyntaxClass(Bracket, new RGB(0, 0, 0))
    setDefaultsForSyntaxClass(Return, new RGB(127, 0, 85), bold = true)
    setDefaultsForSyntaxClass(Bracket, new RGB(0, 0, 0))
    setDefaultsForSyntaxClass(NumberLiteral, new RGB(196, 140, 255))
    setDefaultsForSyntaxClass(EscapeSequence, new RGB(196, 140, 255))
    setDefaultsForSyntaxClass(Symbol, new RGB(173, 142, 0))

    // XML, see org.eclipse.wst.xml.ui.internal.preferences.XMLUIPreferenceInitializer
    setDefaultsForSyntaxClass(XmlComment, new RGB(63, 85, 191))
    setDefaultsForSyntaxClass(XmlAttributeValue, new RGB(42, 0, 255), italic = true)
    setDefaultsForSyntaxClass(XmlAttributeName, new RGB(127, 0, 127))
    setDefaultsForSyntaxClass(XmlAttributeEquals, new RGB(0, 0, 0))
    setDefaultsForSyntaxClass(XmlTagDelimiter, new RGB(0, 128, 128))
    setDefaultsForSyntaxClass(XmlTagName, new RGB(63, 127, 127))
    setDefaultsForSyntaxClass(XmlPi, new RGB(0, 128, 128))
    setDefaultsForSyntaxClass(XmlCdataBorder, new RGB(0, 128, 128))

    // Scala semantic:
    setDefaultsForSyntaxClass(Annotation, new RGB(222, 0, 172), enabled = true)
    setDefaultsForSyntaxClass(CaseClass, new RGB(162, 46, 0), bold = true, enabled = false)
    setDefaultsForSyntaxClass(CaseObject, new RGB(162, 46, 0), bold = true, enabled = false)
    setDefaultsForSyntaxClass(Class, new RGB(50, 147, 153), enabled = false)
    setDefaultsForSyntaxClass(LazyLocalVal, new RGB(94, 94, 255), enabled = true)
    setDefaultsForSyntaxClass(LazyTemplateVal, new RGB(0, 0, 192), enabled = true)
    setDefaultsForSyntaxClass(LocalVal, new RGB(94, 94, 255), enabled = true)
    setDefaultsForSyntaxClass(LocalVar, new RGB(255, 94, 94), enabled = true)
    setDefaultsForSyntaxClass(Method, new RGB(76, 76, 76), italic = true, enabled = false)
    setDefaultsForSyntaxClass(Param, new RGB(100, 0, 103), enabled = false)
    setDefaultsForSyntaxClass(TemplateVal, new RGB(0, 0, 192), enabled = true)
    setDefaultsForSyntaxClass(TemplateVar, new RGB(192, 0, 0), enabled = true)
    setDefaultsForSyntaxClass(Trait, new RGB(50, 147, 153), enabled = false)
    setDefaultsForSyntaxClass(Object, new RGB(50, 147, 153), enabled = false)
    setDefaultsForSyntaxClass(Package, new RGB(0, 110, 4), enabled = false)
    setDefaultsForSyntaxClass(Type, new RGB(50, 147, 153), italic = true, enabled = false)
    setDefaultsForSyntaxClass(TypeParameter, new RGB(23, 0, 129), underline = true, enabled = false)
    setDefaultsForSyntaxClass(IdentifierInInterpolatedString, new RGB(0, 0, 0), underline = true, enabled = false)

    // Dynamic calls
    setDefaultsForSyntaxClass(DynamicSelect, new RGB(192, 0, 0), enabled = false)
    setDefaultsForSyntaxClass(DynamicUpdate, new RGB(192, 0, 0), enabled = false)
    setDefaultsForSyntaxClass(DynamicApply, new RGB(76, 76, 76), enabled = false)
    setDefaultsForSyntaxClass(DynamicApplyNamed, new RGB(76, 76, 76), enabled = false)
  }

  private def setDefaultsForSyntaxClass(
    syntaxClass: ScalaSyntaxClass,
    foregroundRGB: RGB,
    enabled: Boolean = true,
    backgroundRGBOpt: Option[RGB] = None,
    bold: Boolean = false,
    italic: Boolean = false,
    strikethrough: Boolean = false,
    underline: Boolean = false)(implicit scalaPrefStore: IPreferenceStore) =
    {
      lazy val White = new RGB(255, 255, 255)
      scalaPrefStore.setDefault(syntaxClass.enabledKey, enabled)
      scalaPrefStore.setDefault(syntaxClass.foregroundColourKey, StringConverter.asString(foregroundRGB))
      val defaultBackgroundColour = StringConverter.asString(backgroundRGBOpt getOrElse White)
      scalaPrefStore.setDefault(syntaxClass.backgroundColourKey, defaultBackgroundColour)
      scalaPrefStore.setDefault(syntaxClass.backgroundColourEnabledKey, backgroundRGBOpt.isDefined)
      scalaPrefStore.setDefault(syntaxClass.boldKey, bold)
      scalaPrefStore.setDefault(syntaxClass.italicKey, italic)
      scalaPrefStore.setDefault(syntaxClass.underlineKey, underline)
    }

}
