package org.scalaide.ui.internal.explorer

import org.scalaide.ui.internal.ScalaImages
import org.eclipse.core.internal.resources.File
import org.eclipse.jface.viewers.DecorationContext
import org.eclipse.jface.viewers.IDecoration
import org.eclipse.jface.viewers.ILabelProviderListener
import org.eclipse.jface.viewers.ILightweightLabelDecorator

class ScalaDecorator extends ILightweightLabelDecorator {

  def decorate(elem: Any, decoration: IDecoration): Unit = elem match {
    case file: File if file.getName().endsWith(".scala") =>
      decoration.getDecorationContext() match {
        case dc: DecorationContext =>
          dc.putProperty(IDecoration.ENABLE_REPLACE, true)
          decoration.addOverlay(ScalaImages.ExcludedScalaFile, IDecoration.REPLACE)
        case _ =>
      }
    case _ =>
  }

  def dispose(): Unit = {}

  def isLabelProperty(elem: Any, property: String): Boolean = false

  def addListener(listener: ILabelProviderListener): Unit = {}

  def removeListener(listener: ILabelProviderListener): Unit = {}

}
