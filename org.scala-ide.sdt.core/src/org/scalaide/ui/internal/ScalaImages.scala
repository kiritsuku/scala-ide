package org.scalaide.ui.internal

import org.eclipse.core.runtime.FileLocator
import org.eclipse.core.runtime.Path
import org.eclipse.core.runtime.Platform
import org.eclipse.jface.resource.ImageDescriptor
import org.osgi.framework.Bundle
import org.scalaide.core.ScalaPlugin

object ScalaImages {
  val MissingIcon = ImageDescriptor.getMissingImageDescriptor

  val ScalaFile = fromCoreBundle("/icons/full/obj16/scu_obj.gif")
  val ScalaClassFile = fromCoreBundle("/icons/full/obj16/sclassf_obj.gif")
  val ExcludedScalaFile = fromCoreBundle("/icons/full/obj16/scu_resource_obj.gif")

  val ScalaClass = fromCoreBundle("/icons/full/obj16/class_obj.gif")
  val ScalaTrait = fromCoreBundle("/icons/full/obj16/trait_obj.gif")
  val ScalaObject = fromCoreBundle("/icons/full/obj16/object_obj.gif")
  val ScalaPackageObject = fromCoreBundle("/icons/full/obj16/package_object_obj.png")

  val PublicDef = fromCoreBundle("/icons/full/obj16/defpub_obj.gif")
  val PrivateDef = fromCoreBundle("/icons/full/obj16/defpri_obj.gif")
  val ProtectedDef = fromCoreBundle("/icons/full/obj16/defpro_obj.gif")

  val PublicVal = fromCoreBundle("/icons/full/obj16/valpub_obj.gif")
  val ProtectedVal = fromCoreBundle("/icons/full/obj16/valpro_obj.gif")
  val PrivateVal = fromCoreBundle("/icons/full/obj16/valpri_obj.gif")

  val ScalaType = fromCoreBundle("/icons/full/obj16/typevariable_obj.gif")

  val ScalaProjectWizard = fromCoreBundle("/icons/full/wizban/newsprj_wiz.png")

  val RefreshReplToolbar = fromCoreBundle("/icons/full/etool16/refresh_interpreter.gif")

  val NewClass = fromCoreBundle("/icons/full/etool16/newclass_wiz.gif")
  val CorrectionRename = fromCoreBundle("/icons/full/obj16/correction_rename.gif")

  private def fromCoreBundle(path: String): ImageDescriptor =
    imageDescriptor(ScalaPlugin.plugin.pluginId, path) getOrElse MissingIcon

  /**
   * Creates an `Option` holding an `ImageDescriptor` of an image located in an
   * arbitrary bundle. The bundle has at least to be resolved and it may not be
   * stopped. If that is not the case or if the the path to the image is invalid
   * `None` is returned.
   */
  private def imageDescriptor(bundleId: String, path: String): Option[ImageDescriptor] =
    Option(Platform.getBundle(bundleId)) flatMap { bundle =>
      val state = bundle.getState()
      if (state != Bundle.ACTIVE && state != Bundle.STARTING && state != Bundle.RESOLVED)
        None
      else {
        val url = FileLocator.find(bundle, new Path(path), null)
        Option(url) map ImageDescriptor.createFromURL
      }
    }
}
