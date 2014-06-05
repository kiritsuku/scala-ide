package org.scalaide.ui.internal.editor.decorators.semantichighlighting

import org.scalaide.ui.syntax.ScalaSyntaxClasses
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.text.TextAttribute

class Preferences(val store: IPreferenceStore) {
  def isEnabled(): Boolean =
    store.getBoolean(ScalaSyntaxClasses.EnableSemanticHighlighting)

  def isStrikethroughDeprecatedDecorationEnabled(): Boolean =
    store.getBoolean(ScalaSyntaxClasses.StrikethroughDeprecated)

  def isUseSyntacticHintsEnabled(): Boolean =
    store.getBoolean(ScalaSyntaxClasses.UseSyntacticHints)

  def isInterpolatedStringCodeDecorationEnabled(): Boolean =
    ScalaSyntaxClasses.IdentifierInInterpolatedString.getStyleInfo(store).enabled

  def interpolatedStringTextAttribute(): TextAttribute =
    ScalaSyntaxClasses.IdentifierInInterpolatedString.getTextAttribute(store)
}

object Preferences {
  def apply(store: IPreferenceStore): Preferences = new Preferences(store)
}
