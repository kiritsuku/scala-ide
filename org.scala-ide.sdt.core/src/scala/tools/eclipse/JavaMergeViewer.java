package scala.tools.eclipse;


/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.compare.JavaTokenComparator;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.text.PreferencesAdapter;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.PartEventAction;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorExtension3;

/**
 * Copied from org.eclipse.jdt.internal.ui.compare
 */
public class JavaMergeViewer extends TextMergeViewer {

  private IPropertyChangeListener fPreferenceChangeListener;
  private IPreferenceStore fPreferenceStore;
  private Map <SourceViewer, JavaSourceViewerConfiguration> fSourceViewerConfiguration;
  private Map <SourceViewer, CompilationUnitEditorAdapter> fEditor;
  private ArrayList <SourceViewer> fSourceViewer;

  private IWorkbenchPartSite fSite;


  public JavaMergeViewer(final Composite parent, final int styles, final CompareConfiguration mp) {
    super(parent, styles | SWT.LEFT_TO_RIGHT, mp);
  }

  private IPreferenceStore getPreferenceStore() {
    if (fPreferenceStore == null) {
      setPreferenceStore(createChainedPreferenceStore(null));
    }
    return fPreferenceStore;
  }

  @Override
  protected void handleDispose(final DisposeEvent event) {
    setPreferenceStore(null);
    fSourceViewer= null;
    if (fEditor != null) {
      for (final CompilationUnitEditorAdapter editor : fEditor.values()) {
        editor.dispose();
      }
      fEditor= null;
    }
    fSite= null;
    super.handleDispose(event);
  }

  public IJavaProject getJavaProject(final ICompareInput input) {

    if (input == null) {
      return null;
    }

    IResourceProvider rp= null;
    ITypedElement te= input.getLeft();
    if (te instanceof IResourceProvider) {
      rp= (IResourceProvider) te;
    }
    if (rp == null) {
      te= input.getRight();
      if (te instanceof IResourceProvider) {
        rp= (IResourceProvider) te;
      }
    }
    if (rp == null) {
      te= input.getAncestor();
      if (te instanceof IResourceProvider) {
        rp= (IResourceProvider) te;
      }
    }
    if (rp != null) {
      final IResource resource= rp.getResource();
      if (resource != null) {
        final IJavaElement element= JavaCore.create(resource);
        if (element != null) {
          return element.getJavaProject();
        }
      }
    }
    return null;
  }

    @Override
  public void setInput(final Object input) {
      if (input instanceof ICompareInput) {
        final IJavaProject project= getJavaProject((ICompareInput)input);
      if (project != null) {
        setPreferenceStore(createChainedPreferenceStore(project));
      }
    }
      super.setInput(input);
    }

    private ChainedPreferenceStore createChainedPreferenceStore(final IJavaProject project) {
      final ArrayList<IPreferenceStore> stores= new ArrayList<IPreferenceStore>(4);
      if (project != null) {
        stores.add(new EclipsePreferencesAdapter(new ProjectScope(project.getProject()), JavaCore.PLUGIN_ID));
      }
    stores.add(JavaPlugin.getDefault().getPreferenceStore());
    stores.add(new PreferencesAdapter(JavaPlugin.getJavaCorePluginPreferences()));
    stores.add(EditorsUI.getPreferenceStore());
    return new ChainedPreferenceStore(stores.toArray(new IPreferenceStore[stores.size()]));
    }

  private void handlePropertyChange(final PropertyChangeEvent event) {
    if (fSourceViewerConfiguration != null) {
      for (final Map.Entry<SourceViewer, JavaSourceViewerConfiguration> entry : fSourceViewerConfiguration.entrySet()) {
        final JavaSourceViewerConfiguration configuration= entry.getValue();
        if (configuration.affectsTextPresentation(event)) {
          configuration.handlePropertyChangeEvent(event);
          final ITextViewer viewer= entry.getKey();
          viewer.invalidateTextPresentation();
        }
      }
    }
  }

  @Override
  public String getTitle() {
    return "Scala Source Comparison";
  }

  @Override
  public ITokenComparator createTokenComparator(final String s) {
    return new JavaTokenComparator(s);
  }

  @Override
  protected IDocumentPartitioner getDocumentPartitioner() {
    return createJavaPartitioner();
  }

  IDocumentPartitioner createJavaPartitioner() {
    final JavaTextTools tools= getJavaTextTools();
    if (tools != null) {
      return tools.createDocumentPartitioner();
    }
    return null;
  }

  @Override
  protected String getDocumentPartitioning() {
    return IJavaPartitions.JAVA_PARTITIONING;
  }

  @Override
  protected void configureTextViewer(final TextViewer viewer) {
    if (viewer instanceof SourceViewer) {
      final SourceViewer sourceViewer= (SourceViewer)viewer;
      if (fSourceViewer == null) {
        fSourceViewer= new ArrayList<SourceViewer>();
      }
      if (!fSourceViewer.contains(sourceViewer)) {
        fSourceViewer.add(sourceViewer);
      }
      final JavaTextTools tools= getJavaTextTools();
      if (tools != null) {
        final IEditorInput editorInput= getEditorInput(sourceViewer);
        sourceViewer.unconfigure();
        if (editorInput == null) {
          sourceViewer.configure(getSourceViewerConfiguration(sourceViewer, null));
          return;
        }
        getSourceViewerConfiguration(sourceViewer, editorInput);
      }
    }
  }

  /*
   * @see org.eclipse.compare.contentmergeviewer.TextMergeViewer#setEditable(org.eclipse.jface.text.source.ISourceViewer, boolean)
   * @since 3.5
   */
  @Override
  protected void setEditable(final ISourceViewer sourceViewer, final boolean state) {
    super.setEditable(sourceViewer, state);
    if (fEditor != null) {
      final Object editor= fEditor.get(sourceViewer);
      if (editor instanceof CompilationUnitEditorAdapter) {
        ((CompilationUnitEditorAdapter)editor).setEditable(state);
      }
    }
  }

  /*
   * @see org.eclipse.compare.contentmergeviewer.TextMergeViewer#isEditorBacked(org.eclipse.jface.text.ITextViewer)
   * @since 3.5
   */
  @Override
  protected boolean isEditorBacked(final ITextViewer textViewer) {
    return getSite() != null;
  }


  @Override
  protected IEditorInput getEditorInput(final ISourceViewer sourceViewer) {
    final IEditorInput editorInput= super.getEditorInput(sourceViewer);
    if (editorInput == null) {
      return null;
    }
    if (getSite() == null) {
      return null;
    }
    if (!(editorInput instanceof IStorageEditorInput)) {
      return null;
    }
    return editorInput;
  }

  private IWorkbenchPartSite getSite() {
    if (fSite == null) {
      final IWorkbenchPart workbenchPart= getCompareConfiguration().getContainer().getWorkbenchPart();
      fSite= workbenchPart != null ? workbenchPart.getSite() : null;
    }
    return fSite;
  }

  private JavaSourceViewerConfiguration getSourceViewerConfiguration(final SourceViewer sourceViewer, final IEditorInput editorInput) {
    if (fSourceViewerConfiguration == null) {
      fSourceViewerConfiguration= new HashMap<SourceViewer, JavaSourceViewerConfiguration>(3);
    }
    if (fPreferenceStore == null) {
      getPreferenceStore();
    }
    final JavaTextTools tools= getJavaTextTools();
    JavaSourceViewerConfiguration configuration= new JavaSourceViewerConfiguration(tools.getColorManager(), fPreferenceStore, null, getDocumentPartitioning());
    if (editorInput != null) {
      // when input available, use editor
      final CompilationUnitEditorAdapter editor= fEditor.get(sourceViewer);
      try {
        editor.init((IEditorSite)editor.getSite(), editorInput);
        editor.createActions();
        configuration= new JavaSourceViewerConfiguration(tools.getColorManager(), fPreferenceStore, editor, getDocumentPartitioning());
      } catch (final PartInitException e) {
        JavaPlugin.log(e);
      }
    }
    fSourceViewerConfiguration.put(sourceViewer, configuration);
    return fSourceViewerConfiguration.get(sourceViewer);
  }

  JavaTextTools getJavaTextTools() {
    final JavaPlugin plugin= JavaPlugin.getDefault();
    if (plugin != null) {
      return plugin.getJavaTextTools();
    }
    return null;
  }

//  @Override
//  protected int findInsertionPosition(char type, ICompareInput input) {
//
//    int pos= super.findInsertionPosition(type, input);
//    if (pos != 0)
//      return pos;
//
//    if (input instanceof IDiffElement) {
//
//      // find the other (not deleted) element
//      JavaNode otherJavaElement= null;
//      ITypedElement otherElement= null;
//      switch (type) {
//      case 'L':
//        otherElement= input.getRight();
//        break;
//      case 'R':
//        otherElement= input.getLeft();
//        break;
//      }
//      if (otherElement instanceof JavaNode)
//        otherJavaElement= (JavaNode) otherElement;
//
//      // find the parent of the deleted elements
//      JavaNode javaContainer= null;
//      IDiffElement diffElement= (IDiffElement) input;
//      IDiffContainer container= diffElement.getParent();
//      if (container instanceof ICompareInput) {
//
//        ICompareInput parent= (ICompareInput) container;
//        ITypedElement element= null;
//
//        switch (type) {
//        case 'L':
//          element= parent.getLeft();
//          break;
//        case 'R':
//          element= parent.getRight();
//          break;
//        }
//
//        if (element instanceof JavaNode)
//          javaContainer= (JavaNode) element;
//      }
//
//      if (otherJavaElement != null && javaContainer != null) {
//
//        Object[] children;
//        Position p;
//
//        switch (otherJavaElement.getTypeCode()) {
//
//        case JavaNode.PACKAGE:
//          return 0;
//
//        case JavaNode.IMPORT_CONTAINER:
//          // we have to find the place after the package declaration
//          children= javaContainer.getChildren();
//          if (children.length > 0) {
//            JavaNode packageDecl= null;
//            for (int i= 0; i < children.length; i++) {
//              JavaNode child= (JavaNode) children[i];
//              switch (child.getTypeCode()) {
//              case JavaNode.PACKAGE:
//                packageDecl= child;
//                break;
//              case JavaNode.CLASS:
//                return child.getRange().getOffset();
//              }
//            }
//            if (packageDecl != null) {
//              p= packageDecl.getRange();
//              return p.getOffset() + p.getLength();
//            }
//          }
//          return javaContainer.getRange().getOffset();
//
//        case JavaNode.IMPORT:
//          // append after last import
//          p= javaContainer.getRange();
//          return p.getOffset() + p.getLength();
//
//        case JavaNode.CLASS:
//          // append after last class
//          children= javaContainer.getChildren();
//          if (children.length > 0) {
//            for (int i= children.length-1; i >= 0; i--) {
//              JavaNode child= (JavaNode) children[i];
//              switch (child.getTypeCode()) {
//              case JavaNode.CLASS:
//              case JavaNode.IMPORT_CONTAINER:
//              case JavaNode.PACKAGE:
//              case JavaNode.FIELD:
//                p= child.getRange();
//                return p.getOffset() + p.getLength();
//              }
//            }
//          }
//          return javaContainer.getAppendPosition().getOffset();
//
//        case JavaNode.METHOD:
//          // append in next line after last child
//          children= javaContainer.getChildren();
//          if (children.length > 0) {
//            JavaNode child= (JavaNode) children[children.length-1];
//            p= child.getRange();
//            return findEndOfLine(javaContainer, p.getOffset() + p.getLength());
//          }
//          // otherwise use position from parser
//          return javaContainer.getAppendPosition().getOffset();
//
//        case JavaNode.FIELD:
//          // append after last field
//          children= javaContainer.getChildren();
//          if (children.length > 0) {
//            JavaNode method= null;
//            for (int i= children.length-1; i >= 0; i--) {
//              JavaNode child= (JavaNode) children[i];
//              switch (child.getTypeCode()) {
//              case JavaNode.METHOD:
//                method= child;
//                break;
//              case JavaNode.FIELD:
//                p= child.getRange();
//                return p.getOffset() + p.getLength();
//              }
//            }
//            if (method != null)
//              return method.getRange().getOffset();
//          }
//          return javaContainer.getAppendPosition().getOffset();
//        }
//      }
//
//      if (javaContainer != null) {
//        // return end of container
//        Position p= javaContainer.getRange();
//        return p.getOffset() + p.getLength();
//      }
//    }
//
//    // we give up
//    return 0;
//  }

//  private int findEndOfLine(JavaNode container, int pos) {
//    int line;
//    IDocument doc= container.getDocument();
//    try {
//      line= doc.getLineOfOffset(pos);
//      pos= doc.getLineOffset(line+1);
//    } catch (BadLocationException ex) {
//      // silently ignored
//    }
//
//    // ensure that position is within container range
//    Position containerRange= container.getRange();
//    int start= containerRange.getOffset();
//    int end= containerRange.getOffset() + containerRange.getLength();
//    if (pos < start)
//      return start;
//    if (pos >= end)
//      return end-1;
//
//    return pos;
//  }

  private void setPreferenceStore(final IPreferenceStore ps) {
    if (fPreferenceChangeListener != null) {
      if (fPreferenceStore != null) {
        fPreferenceStore.removePropertyChangeListener(fPreferenceChangeListener);
      }
      fPreferenceChangeListener= null;
    }
    fPreferenceStore= ps;
    if (fPreferenceStore != null) {
      fPreferenceChangeListener= new IPropertyChangeListener() {
        public void propertyChange(final PropertyChangeEvent event) {
          handlePropertyChange(event);
        }
      };
      fPreferenceStore.addPropertyChangeListener(fPreferenceChangeListener);
    }
  }

  /*
   * @see org.eclipse.compare.contentmergeviewer.TextMergeViewer#createSourceViewer(org.eclipse.swt.widgets.Composite, int)
   * @since 3.5
   */
  @Override
  protected SourceViewer createSourceViewer(final Composite parent, final int textOrientation) {
    SourceViewer sourceViewer;
    if (getSite() != null) {
      final CompilationUnitEditorAdapter editor= new CompilationUnitEditorAdapter(textOrientation);
      editor.createPartControl(parent);

      final ISourceViewer iSourceViewer= editor.getViewer();
      Assert.isTrue(iSourceViewer instanceof SourceViewer);
      sourceViewer= (SourceViewer)iSourceViewer;
      if (fEditor == null) {
        fEditor= new HashMap<SourceViewer, CompilationUnitEditorAdapter>(3);
      }
      fEditor.put(sourceViewer, editor);
    } else {
      sourceViewer= super.createSourceViewer(parent, textOrientation);
    }

    if (fSourceViewer == null) {
      fSourceViewer= new ArrayList<SourceViewer>();
    }
    fSourceViewer.add(sourceViewer);

    return sourceViewer;
  }

  @Override
  protected void setActionsActivated(final SourceViewer sourceViewer, final boolean state) {
    if (fEditor != null) {
      final Object editor= fEditor.get(sourceViewer);
      if (editor instanceof CompilationUnitEditorAdapter) {
        final CompilationUnitEditorAdapter cuea = (CompilationUnitEditorAdapter)editor;
        cuea.setActionsActivated(state);

        final IAction saveAction= cuea.getAction(ITextEditorActionConstants.SAVE);
        if (saveAction instanceof IPageListener) {
          final PartEventAction partEventAction = (PartEventAction) saveAction;
          final IWorkbenchPart compareEditorPart= getCompareConfiguration().getContainer().getWorkbenchPart();
          if (state) {
            partEventAction.partActivated(compareEditorPart);
          } else {
            partEventAction.partDeactivated(compareEditorPart);
          }
        }
      }
    }
  }

  @Override
  protected void createControls(final Composite composite) {
    super.createControls(composite);
    final IWorkbenchPart workbenchPart = getCompareConfiguration().getContainer().getWorkbenchPart();
    if (workbenchPart != null) {
      final IContextService service = (IContextService)workbenchPart.getSite().getService(IContextService.class);
      if (service != null) {
        service.activateContext("org.eclipse.jdt.ui.javaEditorScope"); //$NON-NLS-1$
      }
    }
  }

  @Override
  public Object getAdapter(final Class adapter) {
    if (adapter == ITextEditorExtension3.class) {
      final IEditorInput activeInput= (IEditorInput)super.getAdapter(IEditorInput.class);
      if (activeInput != null) {
        for (final CompilationUnitEditorAdapter editor : fEditor.values()) {
          if (activeInput.equals(editor.getEditorInput())) {
            return editor;
          }
        }
      }
      return null;
    }
    return super.getAdapter(adapter);
  }

  private class CompilationUnitEditorAdapter extends CompilationUnitEditor {
    private boolean fInputSet = false;
    private final int fTextOrientation;
    private boolean fEditable;

    CompilationUnitEditorAdapter(final int textOrientation) {
      super();
      fTextOrientation = textOrientation;
      // TODO: has to be set here
      setPreferenceStore(createChainedPreferenceStore(null));
    }
    private void setEditable(final boolean editable) {
      fEditable= editable;
    }
    @Override
    public IWorkbenchPartSite getSite() {
      return JavaMergeViewer.this.getSite();
    }
    @Override
    public void createActions() {
      if (fInputSet) {
        super.createActions();
        // to avoid handler conflicts disable extra actions
        // we're not handling by CompareHandlerService
        getCorrectionCommands().deregisterCommands();
        getRefactorActionGroup().dispose();
        getGenerateActionGroup().dispose();
      }
      // else do nothing, we will create actions later, when input is available
    }
    @Override
    public void createPartControl(final Composite composite) {
      final SourceViewer sourceViewer= (SourceViewer)createJavaSourceViewer(composite, new CompositeRuler(), null, false, fTextOrientation | SWT.H_SCROLL | SWT.V_SCROLL, createChainedPreferenceStore(null));
      setSourceViewer(this, sourceViewer);
      createNavigationActions();
      getSelectionProvider().addSelectionChangedListener(getSelectionChangedListener());
    }
    @Override
    protected void doSetInput(final IEditorInput input) throws CoreException {
      super.doSetInput(input);
      // the editor input has been explicitly set
      fInputSet = true;
    }
    // called by org.eclipse.ui.texteditor.TextEditorAction.canModifyEditor()
    @Override
    public boolean isEditable() {
      return fEditable;
    }
    @Override
    public boolean isEditorInputModifiable() {
      return fEditable;
    }
    @Override
    public boolean isEditorInputReadOnly() {
      return !fEditable;
    }
    @Override
    protected void setActionsActivated(final boolean state) {
      super.setActionsActivated(state);
    }
    @Override
    public void close(final boolean save) {
      getDocumentProvider().disconnect(getEditorInput());
    }
  }

  // no setter to private field AbstractTextEditor.fSourceViewer
  private void setSourceViewer(final ITextEditor editor, final SourceViewer viewer) {
    Field field= null;
    try {
      field= AbstractTextEditor.class.getDeclaredField("fSourceViewer"); //$NON-NLS-1$
    } catch (final SecurityException ex) {
      JavaPlugin.log(ex);
    } catch (final NoSuchFieldException ex) {
      JavaPlugin.log(ex);
    }
    field.setAccessible(true);
    try {
      field.set(editor, viewer);
    } catch (final IllegalArgumentException ex) {
      JavaPlugin.log(ex);
    } catch (final IllegalAccessException ex) {
      JavaPlugin.log(ex);
    }
  }
}

