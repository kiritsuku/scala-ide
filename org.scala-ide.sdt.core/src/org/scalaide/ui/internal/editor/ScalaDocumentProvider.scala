package org.scalaide.ui.internal.editor

import java.lang.reflect.Field
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IMarker
import org.eclipse.core.resources.IResource
import org.eclipse.core.resources.IResourceStatus
import org.eclipse.core.runtime.Assert
import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.NullProgressMonitor
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.SubProgressMonitor
import org.eclipse.jdt.core.IJavaModelStatusConstants
import org.eclipse.jdt.core.JavaModelException
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider.CompilationUnitInfo
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility
import org.eclipse.jdt.internal.ui.javaeditor.ISavePolicy
import org.eclipse.jdt.internal.ui.javaeditor.saveparticipant.IPostSaveListener
import org.eclipse.jdt.ui.JavaUI
import org.eclipse.jface.text.IRegion
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel
import org.scalaide.logging.HasLogger
import org.eclipse.jface.text.IDocument
import org.eclipse.ui.editors.text.TextFileDocumentProvider.DocumentProviderOperation
import org.scalaide.extensions._
import org.scalaide.core.internal.extensions.XRuntime
import org.eclipse.jdt.internal.corext.util.JavaModelUtil
import org.eclipse.ui.editors.text.EditorsUI
import org.eclipse.ui.IFileEditorInput
import org.eclipse.jdt.core.ICompilationUnit
import org.scalaide.core.internal.text.TextDocument
import org.scalaide.core.text._
import org.scalaide.util.internal.eclipse.EditorUtils
import org.scalaide.util.internal.eclipse.FileUtils
import scala.tools.refactoring.common.TextChange
import org.eclipse.jdt.internal.ui.JavaPlugin
import org.eclipse.jdt.core.JavaCore
import org.eclipse.ui.PlatformUI
import scala.tools.refactoring.common.Selections
import scala.reflect.internal.util.SourceFile
import org.eclipse.jface.text.ITextSelection
import scala.tools.nsc.interactive.Global
import org.scalaide.core.internal.extensions.CompilerSupportBuilder
import org.scalaide.core.internal.extensions.ExtensionBuilder
import org.scalaide.core.internal.extensions.DocumentSupportBuilder
import org.scalaide.util.internal.ReflectAccess

class ScalaDocumentProvider extends CompilationUnitDocumentProvider with HasLogger {

  private def createScalaSaveActionListener(udoc: IDocument): IPostSaveListener = {
    new IPostSaveListener {
      override def getName = "Scala SaveActions"
      override def getId = "Scala SaveActions"
      override def needsChangedRegions(cu: ICompilationUnit) = false
      override def saved(cu: ICompilationUnit, changedRegions: Array[IRegion], monitor: IProgressMonitor): Unit = {
        // do not apply save actions to extension project
        if (cu.getPath().segment(0) != XRuntime.ProjectName) {
          try compilationUnitSaved(cu, udoc)
          catch {
            case e: Exception =>
              logger.error("Error while executing Scala save actions", e)
          }
        }
      }
    }
  }

  trait ExtensionType

  type ?=>[A, B] = PartialFunction[A, B]

  val DocumentType: ExtensionBuilder#Creator ?=> Seq[Change] = {
    case creator: DocumentSupportBuilder.DocumentSupportCreator =>
      val doc = new TextDocument(udoc)
      val ext = creator.create(doc)
      ext.perform()
  }

  val CompilerType: ExtensionBuilder#Creator ?=> Seq[Change] = {
    case creator: CompilerSupportBuilder.CompilerSupportCreator =>
      EditorUtils.withScalaSourceFileAndSelection { (ssf, sel) =>
        ssf.withSourceFile { (sf, compiler) =>
          import compiler._

          val r = new Response[Tree]
          askLoadedTyped(sf, r)
          r.get match {
            case Left(t) =>
              val c = creator.create(compiler)(t, sf, sel.getOffset(), sel.getOffset()+sel.getLength())
              c.perform()
            case Right(e) =>
              logger.error(
                  s"An error occurred while trying to get tree of file '${sf.file.name}'."+
                  s" Aborting save action '${creator.extensionName}'", e)
              Seq()
          }
        }
      }.toSeq.flatten
  }

  private def compilationUnitSaved(cu: ICompilationUnit, udoc: IDocument): Unit = {
    val sas = XRuntime.loadSaveActions()

    val xs = Seq[PartialFunction[ExtensionBuilder#Creator, Seq[Change]]](
        DocumentType, CompilerType)

    val changes = sas.flatMap(sa => xs.find(_.isDefinedAt(sa)).fold(Seq[Change]())(_(sa)))

    EditorUtils.withScalaSourceFileAndSelection { (ssf, sel) =>
      val sv = ssf.sourceFile()
      val edits = changes map {
        case Add(start, text) =>
          new TextChange(sv, start, start, text)
        case Replace(start, end, text) =>
          new TextChange(sv, start, end, text)
        case Remove(start, end) =>
          new TextChange(sv, start, end, "")
      }
      EditorUtils.applyChangesToFileWhileKeepingSelection(udoc, sel, ssf.file, edits.toList)
      None
    }
  }

  private var udoc: IDocument = _

  override def createSaveOperation(elem: AnyRef, doc: IDocument, overwrite: Boolean): DocumentProviderOperation = {
    udoc = doc
    super.createSaveOperation(elem, doc, overwrite)
  }

  override def commitWorkingCopy(m: IProgressMonitor, element: AnyRef, info: CompilationUnitInfo, overwrite: Boolean): Unit = {
    val monitor = if (m == null) new NullProgressMonitor() else m
    monitor.beginTask("", 100)

    try commitWorkingCopy0(monitor, element, info, overwrite)
    catch {
      case e: Exception =>
        logger.error("exception thrown while trying to save document", e)
    }
    finally {
      monitor.done()
    }
  }

  /**
   * The implementation of this method is copied/adapted from the class
   * [[org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider#commitWorkingCopy]].
   *
   * `super.createSaveOperation` is the method that should be overwritten, but
   * this method does too many useful things. I'm afraid when reimplementing
   * it I break too many things.
   */
  private def commitWorkingCopy0(monitor: IProgressMonitor, element: AnyRef, info: CompilationUnitInfo, overwrite: Boolean): Unit = {
    val document = info.fTextFileBuffer.getDocument()
    val resource = info.fCopy.getResource()

    Assert.isTrue(resource.isInstanceOf[IFile])

    val isSynchronized = resource.isSynchronized(IResource.DEPTH_ZERO)

    /* https://bugs.eclipse.org/bugs/show_bug.cgi?id=98327
     * Make sure file gets save in commit() if the underlying file has been deleted */
    if (!isSynchronized && isDeleted(element))
      info.fTextFileBuffer.setDirty(true)

    if (!resource.exists()) {
      // underlying resource has been deleted, just recreate file, ignore the rest
      createFileFromDocument(monitor, resource.asInstanceOf[IFile], document)
      return
    }

    if (fSavePolicy != null)
      fSavePolicy.preSave(info.fCopy)

    var subMonitor: IProgressMonitor = null
    try {
      fIsAboutToSave = true

      // the Java editor calls [[CleanUpPostSaveListener]] here and calculates
      // changed regions which we don't need
      val listeners = Array(createScalaSaveActionListener(udoc))

      subMonitor = getSubProgressMonitor(monitor, if (listeners.length > 0) 70 else 100)

      info.fCopy.commitWorkingCopy(overwrite || isSynchronized, subMonitor)
      if (listeners.length > 0)
        notifyPostSaveListeners(info, Array(), listeners, getSubProgressMonitor(monitor, 30))

    } catch {
      // inform about the failure
      case x: JavaModelException =>
        fireElementStateChangeFailed(element)
        if (IJavaModelStatusConstants.UPDATE_CONFLICT == x.getStatus().getCode())
          // convert JavaModelException to CoreException
          throw new CoreException(new Status(
              IStatus.WARNING, JavaUI.ID_PLUGIN, IResourceStatus.OUT_OF_SYNC_LOCAL,
              "The file is not synchronized with the local file system.", null))
        throw x
      case x: CoreException =>
        // inform about the failure
        fireElementStateChangeFailed(element)
        throw x
      case x: RuntimeException =>
        // inform about the failure
        fireElementStateChangeFailed(element)
        throw x
    } finally {
      fIsAboutToSave = false
      if (subMonitor != null)
        subMonitor.done()
    }

    // If here, the dirty state of the editor will change to "not dirty".
    // Thus, the state changing flag will be reset.
    if (info.fModel.isInstanceOf[AbstractMarkerAnnotationModel]) {
      val model = info.fModel.asInstanceOf[AbstractMarkerAnnotationModel]
      model.updateMarkers(document)
    }

    if (fSavePolicy != null) {
      val unit = fSavePolicy.postSave(info.fCopy)
      if (unit != null && info.fModel.isInstanceOf[AbstractMarkerAnnotationModel]) {
        val r = unit.getResource()
        val markers = r.findMarkers(IMarker.MARKER, true, IResource.DEPTH_ZERO)
        if (markers != null && markers.length > 0) {
          val model = info.fModel.asInstanceOf[AbstractMarkerAnnotationModel]
          for (marker <- markers)
            model.updateMarker(document, marker, null)
        }
      }
    }
  }

  private def getSubProgressMonitor(monitor: IProgressMonitor, ticks: Int): IProgressMonitor =
    if (monitor != null)
      new SubProgressMonitor(monitor, ticks, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK)
    else
      new NullProgressMonitor()

  private def reflectionAccess[A](name: String)(f: Field => A) = {
    try {
      val field = classOf[CompilationUnitDocumentProvider].getDeclaredField(name)
      field.setAccessible(true)
      Some(f(field))
    } catch {
      case e: NoSuchFieldException =>
        logger.error(s"The name of field '$name' has changed", e)
        None
    }
  }

  private def fSavePolicy: ISavePolicy =
    reflectionAccess("fSavePolicy")(_.get(this).asInstanceOf[ISavePolicy]).orNull

  private def fIsAboutToSave: Boolean =
    reflectionAccess("fIsAboutToSave")(_.get(this).asInstanceOf[Boolean]).getOrElse(false)

  private def fIsAboutToSave_=(b: Boolean): Unit =
    reflectionAccess("fIsAboutToSave")(_.set(this, b))

}