package org.scalaide.ui.internal.editor

import org.scalaide.core.internal.formatter.ScalaFormattingStrategy
import org.scalaide.core.hyperlink.detector.CompositeHyperlinkDetector
import org.scalaide.core.hyperlink.detector.DeclarationHyperlinkDetector
import org.scalaide.core.hyperlink.detector.ImplicitHyperlinkDetector
import org.scalaide.core.internal.jdt.model.ScalaCompilationUnit
import org.scalaide.core.internal.lexical._
import org.scalaide.core.internal.lexical.ScalaPartitions._
import org.scalaide.ui.syntax.{ScalaSyntaxClasses => SSC}
import org.scalaide.ui.internal.reconciliation.ScalaReconcilingStrategy
import org.scalaide.ui.internal.editor.autoedits._
import org.eclipse.core.runtime.NullProgressMonitor
import org.eclipse.jdt.core.ICodeAssist
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.internal.ui.JavaPlugin
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput
import org.eclipse.jdt.internal.ui.javaeditor.ICompilationUnitDocumentProvider
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.text.DefaultTextHover
import org.eclipse.jface.text.IAutoEditStrategy
import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.ITextHover
import org.eclipse.jface.text.formatter.IContentFormatter
import org.eclipse.jface.text.formatter.MultiPassContentFormatter
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector
import org.eclipse.jface.text.hyperlink.URLHyperlinkDetector
import org.eclipse.jface.text.reconciler.IReconciler
import org.eclipse.jface.text.reconciler.MonoReconciler
import org.eclipse.jface.text.rules.DefaultDamagerRepairer
import org.eclipse.jface.text.source.ISourceViewer
import org.eclipse.jface.util.PropertyChangeEvent
import scalariform.ScalaVersions
import org.scalaide.core.internal.formatter.FormatterPreferences._
import scalariform.formatter.preferences._
import org.eclipse.ui.texteditor.ChainedPreferenceStore


class ScalaSourceViewerConfiguration(
  javaPreferenceStore: IPreferenceStore,
  scalaPreferenceStore: IPreferenceStore,
  editor: ScalaEditor)
    extends JavaSourceViewerConfiguration(
      JavaPlugin.getDefault.getJavaTextTools.getColorManager,
      javaPreferenceStore,
      editor,
      ScalaPartitioning) {

  private val combinedPrefStore = new ChainedPreferenceStore(
      Array(scalaPreferenceStore, javaPreferenceStore))

  private val codeHighlightingScanners = {
    val scalaCodeScanner = new ScalaCodeScanner(combinedPrefStore, ScalaVersions.DEFAULT)
    val singleLineCommentScanner = new ScalaCommentScanner(combinedPrefStore, SSC.SingleLineComment, SSC.TaskTag)
    val multiLineCommentScanner = new ScalaCommentScanner(combinedPrefStore, SSC.MultiLineComment, SSC.TaskTag)
    val scaladocScanner = new ScaladocTokenScanner(combinedPrefStore, SSC.Scaladoc, SSC.ScaladocAnnotation, SSC.ScaladocMacro, SSC.TaskTag)
    val scaladocCodeBlockScanner = new SingleTokenScanner(combinedPrefStore, SSC.ScaladocCodeBlock)
    val stringScanner = new StringTokenScanner(combinedPrefStore, SSC.EscapeSequence, SSC.StringClass)
    val characterScanner = new StringTokenScanner(combinedPrefStore, SSC.EscapeSequence, SSC.Character)
    val multiLineStringScanner = new SingleTokenScanner(combinedPrefStore, SSC.MultiLineString)
    val xmlTagScanner = new XmlTagScanner(combinedPrefStore)
    val xmlCommentScanner = new XmlCommentScanner(combinedPrefStore)
    val xmlCDATAScanner = new XmlCDATAScanner(combinedPrefStore)
    val xmlPCDATAScanner = new SingleTokenScanner(combinedPrefStore, SSC.Default)
    val xmlPIScanner = new XmlPIScanner(combinedPrefStore)

    Map(
      ScalaDefaultContent -> scalaCodeScanner,
      Scaladoc -> scaladocScanner,
      ScaladocCodeBlock -> scaladocCodeBlockScanner,
      ScalaSingleLineComment -> singleLineCommentScanner,
      ScalaMultiLineComment -> multiLineCommentScanner,
      ScalaString -> stringScanner,
      ScalaCharacter -> characterScanner,
      ScalaMultiLineString -> multiLineStringScanner,
      XmlTag -> xmlTagScanner,
      XmlComment -> xmlCommentScanner,
      XmlCdata -> xmlCDATAScanner,
      XmlPcdata -> xmlPCDATAScanner,
      XmlPi -> xmlPIScanner
    )
  }

  override def getTabWidth(sourceViewer: ISourceViewer): Int =
    combinedPrefStore.getInt(IndentSpaces.eclipseKey)

  /**
   * Indent prefixes are all possible variations of strings of a given
   * 'indent' length that can be inserted as indent.
   *
   * As an example, when the indent depth is 4, these are the prefixes:
   *
   * '\t', ' \t', '  \t', '   \t', '    ', '' (when only tabs should be inserted)
   * '    ', '\t', ' \t', '  \t', '   \t', '' (when only spaces should be inserted)
   *
   * The array always contains 2 + indent depth elements, where the last element
   * is always the empty string. The first element describes a full indent depth,
   * whereas the remaining elements describe a combination of spaces + a tab to
   * fill a full indent depth.
   */
  override def getIndentPrefixes(sourceViewer: ISourceViewer, contentType: String): Array[String] = {
    val spaceWidth = combinedPrefStore.getInt(IndentSpaces.eclipseKey)
    val useTabs = combinedPrefStore.getBoolean(IndentWithTabs.eclipseKey)

    val spacePrefix = " " * spaceWidth
    val prefixes = 0 until spaceWidth map (i => " " * i + "\t")

    if (useTabs)
      (prefixes :+ spacePrefix :+ "").toArray
    else
      (spacePrefix +: prefixes :+ "").toArray
  }

  override def getReconciler(sourceViewer: ISourceViewer): IReconciler =
    if (editor ne null) {
      val reconciler = new MonoReconciler(new ScalaReconcilingStrategy(editor), /*isIncremental = */ false)
      // FG: I don't know any better that to defer this to the MonoReconciler constructor's default value
      // reconciler.setDelay(500)
      reconciler.install(sourceViewer)
      reconciler.setProgressMonitor(new NullProgressMonitor())
      reconciler
    } else
      null // the editor is null for the Syntax coloring previewer pane, (so no reconciliation)

  override def getPresentationReconciler(sourceViewer: ISourceViewer): ScalaPresentationReconciler = {
    val reconciler = new ScalaPresentationReconciler()
    reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer))

    for ((partitionType, tokenScanner) <- codeHighlightingScanners) {
      val dr = new DefaultDamagerRepairer(tokenScanner)
      reconciler.setDamager(dr, partitionType)
      reconciler.setRepairer(dr, partitionType)
    }
    reconciler
  }

  override def getTextHover(sv: ISourceViewer, contentType: String, stateMask: Int): ITextHover =
    getCodeAssist match {
      case Some(scu: ScalaCompilationUnit) => new ScalaHover(scu)
      case _                               => new DefaultTextHover(sv)
    }

  override def getHyperlinkDetectors(sv: ISourceViewer): Array[IHyperlinkDetector] = {
    val strategies = List(DeclarationHyperlinkDetector(), ImplicitHyperlinkDetector())
    val detector = new CompositeHyperlinkDetector(strategies)
    if (editor != null) detector.setContext(editor)
    Array(detector, new URLHyperlinkDetector())
  }

  def getCodeAssist: Option[ICodeAssist] = Option(editor) map { editor =>
    val input = editor.getEditorInput
    val provider = editor.getDocumentProvider

    (provider, input) match {
      case (icudp: ICompilationUnitDocumentProvider, _) => icudp getWorkingCopy input
      case (_, icfei: IClassFileEditorInput)            => icfei.getClassFile
      case _                                            => null
    }
  }

  def getProject: IJavaProject =
    getCodeAssist.map(_.asInstanceOf[IJavaElement].getJavaProject).orNull

  /**
   * Replica of JavaSourceViewerConfiguration#getAutoEditStrategies that returns
   * a ScalaAutoIndentStrategy instead of a JavaAutoIndentStrategy.
   *
   * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getAutoEditStrategies(org.eclipse.jface.text.source.ISourceViewer, java.lang.String)
   */
  override def getAutoEditStrategies(sourceViewer: ISourceViewer, contentType: String): Array[IAutoEditStrategy] = {
    def prefProvider = new JdtPreferenceProvider(getProject)
    val partitioning = getConfiguredDocumentPartitioning(sourceViewer)
    def si = new SmartInsertionAutoEditStrategy(editor, combinedPrefStore)

    contentType match {
      case ScalaMultiLineComment | Scaladoc | ScaladocCodeBlock =>
        Array(new CommentAutoIndentStrategy(partitioning, combinedPrefStore))

      case ScalaMultiLineString =>
        Array(
          si,
          new MultiLineStringAutoIndentStrategy(partitioning, combinedPrefStore),
          new MultiLineStringAutoEditStrategy(combinedPrefStore))

      case ScalaString =>
        Array(
          si,
          new StringAutoEditStrategy(partitioning, combinedPrefStore))

      case ScalaCharacter | ScalaDefaultContent =>
        Array(
          si,
          new ScalaAutoIndentStrategy(partitioning, getProject, sourceViewer, prefProvider),
          new AutoIndentStrategy(combinedPrefStore),
          new BracketAutoEditStrategy(combinedPrefStore),
          new LiteralAutoEditStrategy(combinedPrefStore))

      case _ =>
        Array(
          new ScalaAutoIndentStrategy(partitioning, getProject, sourceViewer, prefProvider),
          new AutoIndentStrategy(combinedPrefStore))
    }
  }

  override def getContentFormatter(sourceViewer: ISourceViewer): IContentFormatter = {
    val formatter = new MultiPassContentFormatter(getConfiguredDocumentPartitioning(sourceViewer), ScalaDefaultContent)
    formatter.setMasterStrategy(new ScalaFormattingStrategy(editor))
    formatter
  }

  override def handlePropertyChangeEvent(event: PropertyChangeEvent) {
    super.handlePropertyChangeEvent(event)
    codeHighlightingScanners.values foreach (_ adaptToPreferenceChange event)
  }

  override def getConfiguredContentTypes(sourceViewer: ISourceViewer): Array[String] =
    Array(
      ScalaDefaultContent,
      ScalaMultiLineComment, Scaladoc, ScaladocCodeBlock,
      ScalaMultiLineString, ScalaString, ScalaCharacter)

  override def affectsTextPresentation(event: PropertyChangeEvent) = true

}
