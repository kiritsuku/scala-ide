package scala.tools.eclipse.util

import org.eclipse.jface.text.source.ISourceViewer
import org.eclipse.jface.text.source.IAnnotationModel
import org.eclipse.jface.text.ISynchronizable
import org.eclipse.jface.text.Position
import org.eclipse.jface.text.source.Annotation
import scala.tools.eclipse.util.RichAnnotationModel._
import org.eclipse.jdt.internal.ui.javaeditor.JavaMarkerAnnotation
import org.eclipse.core.internal.resources.MarkerAttributeMap
import scala.collection.JavaConversions
import scala.collection.convert.Wrappers

object AnnotationUtils {

  def update(sourceViewer: ISourceViewer, annotationType: String, newAnnotations: Map[Annotation, Position]) {
    for (annotationModel <- Option(sourceViewer.getAnnotationModel))
      update(annotationModel, annotationType, newAnnotations)
  }

  /**
   *  Replace annotations of the given annotationType with the given new annotations
   */
  private def update(model: IAnnotationModel, annotationType: String, newAnnotations: Map[Annotation, Position]) {
    println(s"update2>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>new:$newAnnotations,$annotationType")
    println(s"update2>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>existing:${model.getAnnotations}")
    println(s"update2>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>class:$model,${model.getClass()}")
    model.getAnnotations foreach {
      case a: JavaMarkerAnnotation =>
        import scala.collection.JavaConverters._
        val x = a.getMarker().getAttributes()
        println(x.size())
        x.asScala foreach println
      case _ =>
    }
    model.withLock {
      val annotationsToRemove = model.getAnnotations.filter(_.getType == annotationType)
      model.replaceAnnotations(annotationsToRemove, newAnnotations)
    }
  }
}