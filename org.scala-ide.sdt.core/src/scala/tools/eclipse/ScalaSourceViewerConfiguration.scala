/*
 * Copyright 2005-2010 LAMP/EPFL
 */
// $Id$

package scala.tools.eclipse;

import org.eclipse.jface.text.formatter.MultiPassContentFormatter
import org.eclipse.jface.util.PropertyChangeEvent
import scala.tools.eclipse.semicolon.InferredSemicolonPainter
import org.eclipse.jface.text.ITextViewerExtension2
import org.eclipse.jdt.core.{ IJavaProject, IJavaElement, ICodeAssist }
import org.eclipse.jdt.internal.ui.JavaPlugin
import org.eclipse.jdt.internal.ui.javaeditor.{ IClassFileEditorInput, ICompilationUnitDocumentProvider, JavaElementHyperlinkDetector }
import org.eclipse.jdt.internal.ui.text.ContentAssistPreference
import org.eclipse.jdt.internal.ui.text.java.{ JavaAutoIndentStrategy, JavaStringAutoIndentStrategy, SmartSemicolonAutoEditStrategy }
import org.eclipse.jdt.internal.ui.text.java.hover.{ AbstractJavaEditorTextHover, BestMatchHover }
import org.eclipse.jdt.internal.ui.text.javadoc.JavaDocAutoIndentStrategy
import org.eclipse.jdt.ui.text.{ JavaSourceViewerConfiguration, IJavaPartitions }
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.text.{ IAutoEditStrategy, IDocument, ITextHover }
import org.eclipse.jface.text.formatter.ContentFormatter
import org.eclipse.jface.text.contentassist.ContentAssistant
import org.eclipse.jface.text.contentassist.IContentAssistant
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector
import org.eclipse.jface.text.presentation.PresentationReconciler
import org.eclipse.jface.text.rules.{ DefaultDamagerRepairer, RuleBasedScanner, ITokenScanner }
import org.eclipse.jface.text.source.ISourceViewer
import org.eclipse.jface.util.PropertyChangeEvent
import org.eclipse.ui.texteditor.{ HyperlinkDetectorDescriptor, ITextEditor }
import org.eclipse.swt.SWT
import scala.tools.eclipse.ui.{ JdtPreferenceProvider, ScalaAutoIndentStrategy, ScalaIndenter }
import scala.tools.eclipse.util.ReflectionUtils
import scala.tools.eclipse.lexical._
import scala.tools.eclipse.formatter.ScalaFormattingStrategy
import scala.tools.eclipse.ui.BracketAutoEditStrategy
import scala.tools.eclipse.properties.syntaxcolouring.ScalaSyntaxClasses
import scala.tools.eclipse.hyperlink.text.detector.{CompositeHyperlinkDetector, DeclarationHyperlinkDetector, ImplicitHyperlinkDetector}
import scalariform.ScalaVersions
import org.eclipse.jface.text.DefaultTextHover
import scala.tools.eclipse.javaelements.ScalaCompilationUnit
import scala.tools.eclipse.ui.CommentAutoIndentStrategy
import org.eclipse.jface.text.hyperlink.URLHyperlinkDetector
import scala.tools.eclipse.ui.LiteralAutoEditStrategy
import scala.tools.eclipse.ui.StringAutoEditStrategy
import scala.tools.eclipse.ui.MultiLineStringAutoEditStrategy
import org.eclipse.jdt.internal.ui.text.HTMLAnnotationHover
import org.eclipse.jface.text.source.Annotation
import org.eclipse.jface.internal.text.html.HTMLPrinter
import org.eclipse.jface.text.source.ISourceViewerExtension2
import org.eclipse.jface.text.Position
import org.eclipse.jface.text.source.projection.AnnotationBag

class ScalaSourceViewerConfiguration(store: IPreferenceStore, scalaPreferenceStore: IPreferenceStore, editor: ITextEditor)
   extends JavaSourceViewerConfiguration(JavaPlugin.getDefault.getJavaTextTools.getColorManager, store, editor, IJavaPartitions.JAVA_PARTITIONING) {

  private val codeHighlightingScanners = {
    val scalaCodeScanner = new ScalaCodeScanner(getColorManager, scalaPreferenceStore, ScalaVersions.DEFAULT)
    val singleLineCommentScanner = new SingleTokenScanner(ScalaSyntaxClasses.SINGLE_LINE_COMMENT, getColorManager, scalaPreferenceStore)
    val multiLineCommentScanner = new SingleTokenScanner(ScalaSyntaxClasses.MULTI_LINE_COMMENT, getColorManager, scalaPreferenceStore)
    val scaladocScanner = new ScaladocTokenScanner(ScalaSyntaxClasses.SCALADOC, ScalaSyntaxClasses.SCALADOC_ANNOTATION, ScalaSyntaxClasses.SCALADOC_MACRO, getColorManager, scalaPreferenceStore)
    val scaladocCodeBlockScanner = new SingleTokenScanner(ScalaSyntaxClasses.SCALADOC_CODE_BLOCK, getColorManager, scalaPreferenceStore)
    val stringScanner = new StringTokenScanner(ScalaSyntaxClasses.ESCAPE_SEQUENCE, ScalaSyntaxClasses.STRING, getColorManager, scalaPreferenceStore)
    val characterScanner = new StringTokenScanner(ScalaSyntaxClasses.ESCAPE_SEQUENCE, ScalaSyntaxClasses.CHARACTER, getColorManager, scalaPreferenceStore)
    val multiLineStringScanner = new SingleTokenScanner(ScalaSyntaxClasses.MULTI_LINE_STRING, getColorManager, scalaPreferenceStore)
    val xmlTagScanner = new XmlTagScanner(getColorManager, scalaPreferenceStore)
    val xmlCommentScanner = new XmlCommentScanner(getColorManager, scalaPreferenceStore)
    val xmlCDATAScanner = new XmlCDATAScanner(getColorManager, scalaPreferenceStore)
    val xmlPCDATAScanner = new SingleTokenScanner(ScalaSyntaxClasses.DEFAULT, getColorManager, scalaPreferenceStore)
    val xmlPIScanner = new XmlPIScanner(getColorManager, scalaPreferenceStore)

    Map(
      IDocument.DEFAULT_CONTENT_TYPE -> scalaCodeScanner,
      IJavaPartitions.JAVA_DOC -> scaladocScanner,
      ScalaPartitions.SCALADOC_CODE_BLOCK -> scaladocCodeBlockScanner,
      IJavaPartitions.JAVA_SINGLE_LINE_COMMENT -> singleLineCommentScanner,
      IJavaPartitions.JAVA_MULTI_LINE_COMMENT -> multiLineCommentScanner,
      IJavaPartitions.JAVA_STRING -> stringScanner,
      IJavaPartitions.JAVA_CHARACTER -> characterScanner,
      ScalaPartitions.SCALA_MULTI_LINE_STRING -> multiLineStringScanner,
      ScalaPartitions.XML_TAG -> xmlTagScanner,
      ScalaPartitions.XML_COMMENT -> xmlCommentScanner,
      ScalaPartitions.XML_CDATA -> xmlCDATAScanner,
      ScalaPartitions.XML_PCDATA -> xmlPCDATAScanner,
      ScalaPartitions.XML_PI -> xmlPIScanner
    )
  }

  override def getAnnotationHover(sourceViewer: ISourceViewer) = {
    new DefaultAnnotationHover(false)
  }

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

   override def getTextHover(sv: ISourceViewer, contentType: String, stateMask: Int) = {
//     new ScalaHover(getCodeAssist _)
     val scuOption = getCodeAssist match {
       case Some(scu: ScalaCompilationUnit) => Some(scu)
       case _ => None
     }
     scuOption match {
       case Some(scu) => new ScalaHover(scu)
       case None => new DefaultTextHover(sv)
     }
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
         case (_, icfei: IClassFileEditorInput) => icfei.getClassFile
         case _ => null
      }
   }

   def getProject: IJavaProject = {
      getCodeAssist map (_.asInstanceOf[IJavaElement].getJavaProject) orNull
   }

   /**
    * Replica of JavaSourceViewerConfiguration#getAutoEditStrategies that returns
    * a ScalaAutoIndentStrategy instead of a JavaAutoIndentStrategy.
    *
    * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getAutoEditStrategies(org.eclipse.jface.text.source.ISourceViewer, java.lang.String)
    */
   override def getAutoEditStrategies(sourceViewer: ISourceViewer, contentType: String): Array[IAutoEditStrategy] = {
      val partitioning = getConfiguredDocumentPartitioning(sourceViewer)
      contentType match {
         case IJavaPartitions.JAVA_DOC | IJavaPartitions.JAVA_MULTI_LINE_COMMENT | ScalaPartitions.SCALADOC_CODE_BLOCK =>
           Array(new CommentAutoIndentStrategy(ScalaPlugin.prefStore, partitioning))
         case ScalaPartitions.SCALA_MULTI_LINE_STRING =>
           Array(new SmartSemicolonAutoEditStrategy(partitioning), new ScalaAutoIndentStrategy(partitioning, getProject, sourceViewer, new JdtPreferenceProvider(getProject)), new MultiLineStringAutoEditStrategy(partitioning, ScalaPlugin.prefStore))
         case IJavaPartitions.JAVA_STRING =>
            Array(new SmartSemicolonAutoEditStrategy(partitioning), new JavaStringAutoIndentStrategy(partitioning), new StringAutoEditStrategy(partitioning, ScalaPlugin.prefStore))
         case IJavaPartitions.JAVA_CHARACTER | IDocument.DEFAULT_CONTENT_TYPE =>
            Array(new SmartSemicolonAutoEditStrategy(partitioning), new ScalaAutoIndentStrategy(partitioning, getProject, sourceViewer, new JdtPreferenceProvider(getProject)), new BracketAutoEditStrategy(ScalaPlugin.prefStore), new LiteralAutoEditStrategy(ScalaPlugin.prefStore))
         case _ =>
            Array(new ScalaAutoIndentStrategy(partitioning, getProject, sourceViewer, new JdtPreferenceProvider(getProject)))
      }
   }

  override def getContentFormatter(sourceViewer: ISourceViewer) = {
    val formatter = new MultiPassContentFormatter(getConfiguredDocumentPartitioning(sourceViewer), IDocument.DEFAULT_CONTENT_TYPE)
    formatter.setMasterStrategy(new ScalaFormattingStrategy(editor))
    formatter
  }

   override def handlePropertyChangeEvent(event: PropertyChangeEvent) {
      super.handlePropertyChangeEvent(event)
      codeHighlightingScanners.values foreach (_ adaptToPreferenceChange event)
   }

   /**
    * Adds Scala related partition types to the list of configured content types,
    * in order that they are available for several features of the IDE.
    */
   override def getConfiguredContentTypes(sourceViewer: ISourceViewer): Array[String] =
     super.getConfiguredContentTypes(sourceViewer) ++
       Seq(ScalaPartitions.SCALA_MULTI_LINE_STRING, ScalaPartitions.SCALADOC_CODE_BLOCK)

   override def affectsTextPresentation(event: PropertyChangeEvent) = true

//   class ScalaAnnotationHover extends HTMLAnnotationHover(false) {
//
//
////    def annotationsForLine(viewer: ISourceViewer, line: Int): List[_] = {
////      val doc = viewer.getDocument()
////      val model = viewer match {
////        case v: ISourceViewerExtension2 => v.getVisualAnnotationModel()
////        case v => v.getAnnotationModel()
////      }
////      if (model == null) return null
////
////      def isRulerLine(pos: Position) = {
////        // TODO refactoring
////        if (pos.getOffset() > -1 && pos.getLength() > -1 && pos.getOffset() < doc.getLength())
////          line == doc.getLineOfOffset(pos.getOffset())
////        else false
////      }
////
////      def includeAnnotation(a: Annotation, pos: Position) = {
////        a.getText()
////      }
////
////      import scala.collection.JavaConverters._
////
////      def check(a: Annotation) = a match {
////        case bag: AnnotationBag =>
////          for {
////            a <- bag.iterator().asInstanceOf[java.util.Iterator[Annotation]].asScala
////            pos = model.getPosition(a)
////            if pos != null
////          }
////
////      }
////
////      for {
////        a <- model.getAnnotationIterator().asInstanceOf[java.util.Iterator[Annotation]].asScala
////        pos = model.getPosition(a)
////        if pos != null && isRulerLine(pos)
////
////      } {
////
////      }
////
////      ???
////    }
//
//
//
//    override def getHoverInfo(sourceViewer: ISourceViewer, lineNumber: Int) = {
////      import scala.collection.JavaConverters._
////
////      val method = this.getClass().getMethods().find(_.getName() == "getAnnotationsForLine").get
////      method.setAccessible(true)
////
////      val javaAnnotations = method.invoke(this, sourceViewer, lineNumber.asInstanceOf[Integer]).asInstanceOf[java.util.List[Annotation]].asScala
////
////      val field = this.getClass().getFields().find(_.getName() == "fShowLineNumber").get
////      field.setAccessible(true)
////      val showLineNumber = field.getBoolean(this)
////
////      def formatMessage = {
////        if (javaAnnotations.size == 1) {
////          val msg = javaAnnotations.head.getText()
////          if (msg != null && msg.trim().length() > 0) {
////            val method = this.getClass().getMethods().find(_.getName() == "formatSingleMessage").get
////            method.setAccessible(true)
////            method.invoke(this, msg).asInstanceOf[String]
////          }
////          else null
////        } else {
////          val msgs = for {
////            a <- javaAnnotations
////            msg = a.getText()
////            if msg != null && msg.trim().length() > 0
////          } yield msg.trim()
////          if (msgs.size == 1) {
////            val method = this.getClass().getMethods().find(_.getName() == "formatSingleMessage").get
////            method.setAccessible(true)
////            method.invoke(this, msgs.head).asInstanceOf[String]
////          }
////        }
////
////      }
////
////      if (javaAnnotations != null)
////        formatMessage
////      else if (showLineNumber && lineNumber > -1)
////        s"Line: ${lineNumber+1}"
////      else
////        null
//
//      super.getHoverInfo(sourceViewer, lineNumber)
//    }
//
//    override def isIncluded(annotation: Annotation) = {
//      isShowInVerticalRuler(annotation)
//    }
//
//    override def formatSingleMessage(message: String) = {
//      println(message)
//      import HTMLPrinter._
//      val buffer = new StringBuffer(message.length())
//      addPageProlog(buffer)
//      addParagraph(buffer, "<pre><code>"+message+"</code></pre>")
//      addPageEpilog(buffer)
//      buffer.toString()
//    }
//  }
}
