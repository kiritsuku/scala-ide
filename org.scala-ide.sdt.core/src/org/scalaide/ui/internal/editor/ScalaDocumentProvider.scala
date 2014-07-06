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
import org.eclipse.jface.text.Document
import org.scalaide.util.internal.eclipse.EditorUtils
import org.scalaide.util.internal.eclipse.FileUtils
import scala.tools.refactoring.common.TextChange
import org.eclipse.jdt.internal.ui.JavaPlugin
import org.eclipse.jdt.core.JavaCore
import org.eclipse.e4.ui.workbench.modeling.EModelService
import org.eclipse.ui.PlatformUI

class ScalaDocumentProvider extends CompilationUnitDocumentProvider with HasLogger {

  // TODO make save actions static, otherwise they are created every time a new editor is created
  def saveActions(udoc: IDocument): IPostSaveListener = {
    new IPostSaveListener {
      override def getName = "scala save action name"
      override def getId = "scala save action id"
      override def needsChangedRegions(cu: ICompilationUnit) = false
      override def saved(cu: ICompilationUnit, changedRegions: Array[IRegion], monitor: IProgressMonitor): Unit = {

        // do not apply save actions in extension project
        if (cu.getPath().segment(0) == XRuntime.ProjectName)
          return

//        val service = PlatformUI.getWorkbench().getAdapter(classOf[EModelService]).asInstanceOf[EModelService]
//        val x: EModelService = null
//        val persp = x.getPerspectiveFor(null)


        def applyChange(udoc: IDocument)(c: Change): Unit = c match {
          case Add(start, text) =>
            udoc.replace(start, 0, text)
          case Replace(start, end, text) =>
            udoc.replace(start, end-start, text)
          case Remove(start, end) =>
            udoc.replace(start, end-start, "")
        }

        val sa = XRuntime.loadSaveActions()
        //val src = ""
        //val udoc = new Document(src)
        val doc = new TextDocument(udoc.get())

//        val f = FileUtils.toIFile(cu.getPath()).get
        EditorUtils.withScalaSourceFileAndSelection { (ssf, sel) =>
          val sv = ssf.sourceFile()
          val edits = sa.flatMap(_.perform(doc)) map {
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

//        val edits = sa.flatMap(_.perform(doc)).sortBy {
//          case Add(start, text) => -start
//          case Replace(start, end, text) => -start
//          case Remove(start, end) => -start
//        }
//        edits foreach applyChange(udoc)
      }
    }
  }

  private var udoc: IDocument = _

  override def createSaveOperation(elem: AnyRef, doc: IDocument, overwrite: Boolean): DocumentProviderOperation = {
    udoc = doc
//    super.createSaveOperation(elem, doc, overwrite)
    val info = getFileInfo(elem)
    info match {
      case info: CompilationUnitInfo =>
        val cu = info.fCopy
        if (cu != null && !JavaModelUtil.isPrimary(cu))
          return super.createSaveOperation(elem, doc, overwrite)

        if (info.fTextFileBuffer.getDocument() != doc) {
          val status = new Status(IStatus.WARNING, EditorsUI.PLUGIN_ID, IStatus.ERROR, "CompilationUnitDocumentProvider_saveAsTargetOpenInEditor", null);
          throw new CoreException(status);
        }
    }

    new DocumentProviderOperation {
      override def execute(monitor: IProgressMonitor) = {
        commitWorkingCopy(monitor, elem, info.asInstanceOf[CompilationUnitInfo], overwrite)
      }
      override def getSchedulingRule = {
        info match {
          case info: IFileEditorInput =>
            val f = info.fElement.asInstanceOf[IFileEditorInput].getFile()
            computeSchedulingRule(f)
          case _ =>
            null
        }
      }
    }
  }

  /**
   * The implementation of this method is copied/adapted from the class
   * [[org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider]].
   *
   * `super.createSaveOperation` is the method that should be overwritten, but
   * this method does too many useful things. I'm afraid when reimplementing
   * it I break too many things.
   */
  override def commitWorkingCopy(m: IProgressMonitor, element: AnyRef, info: CompilationUnitInfo, overwrite: Boolean): Unit = {

    val monitor = if (m == null) new NullProgressMonitor() else m

    monitor.beginTask("", 100);

    try {
      val document= info.fTextFileBuffer.getDocument();
      val resource= info.fCopy.getResource();

      Assert.isTrue(resource.isInstanceOf[IFile]);

      val isSynchronized= resource.isSynchronized(IResource.DEPTH_ZERO);

      /* https://bugs.eclipse.org/bugs/show_bug.cgi?id=98327
       * Make sure file gets save in commit() if the underlying file has been deleted */
      if (!isSynchronized && isDeleted(element))
        info.fTextFileBuffer.setDirty(true);

      if (!resource.exists()) {
        // underlying resource has been deleted, just recreate file, ignore the rest
        createFileFromDocument(monitor, resource.asInstanceOf[IFile], document);
        return;
      }

      if (fSavePolicy != null)
        fSavePolicy.preSave(info.fCopy);

      var subMonitor: IProgressMonitor = null;
      try {
        fIsAboutToSave= true;

        // the Java editor call [[CleanUpPostSaveListener]] here
        val listeners = try Array(saveActions(udoc)) catch { case e: Exception =>
          e.printStackTrace(); Array[IPostSaveListener]()}

        var changedRegionException: CoreException= null;
        var needsChangedRegions= false;
        try {
          if (listeners.length > 0)
            needsChangedRegions= false // XXX original code: SaveParticipantRegistry.isChangedRegionsRequired(info.fCopy);
        } catch {
          case ex: CoreException => changedRegionException= ex;
        }

        var changedRegions: Array[IRegion]= null;
        if (needsChangedRegions) {
          try {
            changedRegions= EditorUtility.calculateChangedLineRegions(info.fTextFileBuffer, getSubProgressMonitor(monitor, 20));
          } catch  {
            case ex: CoreException => changedRegionException= ex;
          } finally {
            subMonitor= getSubProgressMonitor(monitor, 50);
          }
        } else
          subMonitor= getSubProgressMonitor(monitor, if (listeners.length > 0) 70 else 100);

        info.fCopy.commitWorkingCopy(overwrite || isSynchronized, subMonitor);
        if (listeners.length > 0)
          notifyPostSaveListeners(info, changedRegions, listeners, getSubProgressMonitor(monitor, 30));

        if (changedRegionException != null) {
          throw changedRegionException;
        }
      } catch {
        // inform about the failure
        case x: JavaModelException => fireElementStateChangeFailed(element);
        if (IJavaModelStatusConstants.UPDATE_CONFLICT == x.getStatus().getCode())
          // convert JavaModelException to CoreException
          throw new CoreException(new Status(IStatus.WARNING, JavaUI.ID_PLUGIN, IResourceStatus.OUT_OF_SYNC_LOCAL, "CompilationUnitDocumentProvider_error_outOfSync", null));
        throw x;
        case x: CoreException =>
        // inform about the failure
        fireElementStateChangeFailed(element);
        throw x;
        case x: RuntimeException =>
        // inform about the failure
        fireElementStateChangeFailed(element);
        throw x;
      } finally {
        fIsAboutToSave= false;
        if (subMonitor != null)
          subMonitor.done();
      }

      // If here, the dirty state of the editor will change to "not dirty".
      // Thus, the state changing flag will be reset.
      if (info.fModel.isInstanceOf[AbstractMarkerAnnotationModel]) {
        val model= info.fModel.asInstanceOf[AbstractMarkerAnnotationModel];
        model.updateMarkers(document);
      }

      if (fSavePolicy != null) {
        val unit= fSavePolicy.postSave(info.fCopy);
        if (unit != null && info.fModel.isInstanceOf[AbstractMarkerAnnotationModel]) {
          val r= unit.getResource();
          val markers= r.findMarkers(IMarker.MARKER, true, IResource.DEPTH_ZERO);
          if (markers != null && markers.length > 0) {
            val model= info.fModel.asInstanceOf[AbstractMarkerAnnotationModel];
            for (i <- 0 until markers.length)
              model.updateMarker(document, markers(i), null);
          }
        }
      }
    } finally {
      monitor.done();
    }
  }

  def getSubProgressMonitor(monitor: IProgressMonitor, ticks: Int): IProgressMonitor =
    if (monitor != null)
      new SubProgressMonitor(monitor, ticks, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK)
    else
      new NullProgressMonitor()

  def reflectionAccess[A](name: String)(f: Field => A) = {
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

  def fSavePolicy: ISavePolicy =
    reflectionAccess("fSavePolicy")(_.get(this).asInstanceOf[ISavePolicy]).orNull

  def fIsAboutToSave: Boolean =
    reflectionAccess("fIsAboutToSave")(_.get(this).asInstanceOf[Boolean]).getOrElse(false)

  def fIsAboutToSave_=(b: Boolean): Unit =
    reflectionAccess("fIsAboutToSave")(_.set(this, b))

}