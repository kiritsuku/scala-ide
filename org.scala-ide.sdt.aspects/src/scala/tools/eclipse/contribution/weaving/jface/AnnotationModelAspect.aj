package scala.tools.eclipse.contribution.weaving.jface;

import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;

public aspect AnnotationModelAspect {

  pointcut replaceAnnotations(Annotation[] annotationsToRemove, Map annotationsToAdd, boolean fireModelChanged):
    execution(void AnnotationModel.replaceAnnotations(Annotation[], Map, boolean)) &&
    args(annotationsToRemove, annotationsToAdd, fireModelChanged);
 
  void around(Annotation[] annotationsToRemove, Map annotationsToAdd, boolean fireModelChanged)  throws BadLocationException:
    replaceAnnotations(annotationsToRemove, annotationsToAdd, fireModelChanged) {
    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>TEST");
    proceed(annotationsToRemove, annotationsToAdd, fireModelChanged);
  }
}