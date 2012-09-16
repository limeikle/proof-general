/*
 *  $RCSfile: ProofScriptEditor.java,v $
 *
 *  Created on 29 Mar 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor;


import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.ResourceBundle;

import org.dom4j.Element;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.TextEditorAction;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import ed.inf.proofgeneral.Constants;
import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.document.ProofScriptDocumentProvider;
import ed.inf.proofgeneral.document.ProofScriptMarkers;
import ed.inf.proofgeneral.document.outline.PGContentOutlinePage;
import ed.inf.proofgeneral.editor.actions.retargeted.ActivateAction;
import ed.inf.proofgeneral.editor.actions.retargeted.EnterCommandAction;
import ed.inf.proofgeneral.editor.actions.retargeted.GotoAction;
import ed.inf.proofgeneral.editor.actions.retargeted.IClearableAction;
import ed.inf.proofgeneral.editor.actions.retargeted.InterruptAction;
import ed.inf.proofgeneral.editor.actions.retargeted.NextCommandAction;
import ed.inf.proofgeneral.editor.actions.retargeted.OpenDefinition;
import ed.inf.proofgeneral.editor.actions.retargeted.ParseDocAction;
import ed.inf.proofgeneral.editor.actions.retargeted.RestartAction;
import ed.inf.proofgeneral.editor.actions.retargeted.SendAllAction;
import ed.inf.proofgeneral.editor.actions.retargeted.ToggleSymbolsAction;
import ed.inf.proofgeneral.editor.actions.retargeted.UndoAllAction;
import ed.inf.proofgeneral.editor.actions.retargeted.UndoCommandAction;
import ed.inf.proofgeneral.preferences.PreferenceNames;
import ed.inf.proofgeneral.sessionmanager.IProverStateObserver;
import ed.inf.proofgeneral.sessionmanager.ProverState;
import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.proofgeneral.symbols.SymbolTable;
import ed.inf.utils.datastruct.MutableObject;
import ed.inf.utils.datastruct.StringManipulation;
import ed.inf.utils.eclipse.DisplayCallable;
import ed.inf.utils.eclipse.EclipseMethods;
import ed.inf.utils.eclipse.ErrorUI;
import ed.inf.utils.process.ThreadCaller.RuntimeExceptionWrapper;

/**
 * The editor for Proof Script files.
 *
 * @author Daniel Winterstein
 * @author David Aspinall
 */

// ideas TODO: add createCompositeRuler to make another ruler for highlighting
// the processed region, cf. the selection ruler part of the vertical
// ruler (unless we can use that?).  Marker support for vertical ruler
// seems to only display icons there.

public class ProofScriptEditor extends TextEditor implements SymbolTable.SymbolChangeListener {

	/** Indicates that no offset has been specified. */
	public static int NOTARGET = -2; // NB: -1 is a valid target offset!

	/** The connected session manager. Non-null and final after init() has succeeded. */
	private SessionManager sm;

	/** The connected document.  Non-null and final after init() has succeeded. */
	private ProofScriptDocument doc;

	public ProofScriptEditor() {
		super();
	}
		
    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
    	super.init(site, input); // NB: this triggers sm startup, preference loading, etc.

    	this.doc= getProvidedDocument();
    	if (doc == null) {
    		throw new PartInitException("Proof Script Editor: cannot find Proof Script Document.");
    	}

    	try {
    		this.sm = getConnectedSessionManager();
    	} catch (CoreException e) {
    		throw new PartInitException("Proof Script Editor: no associated session manager for editor.\n" + 
    					e.getLocalizedMessage());
    	}

    	// sm.addListener(this); // listen on: errors (undo them[why here?]); 

    	// listen for symbol definition changes
    	doc.getProver().getSymbols().addSymbolChangeListener(this);

    	setSourceViewerConfiguration(
    			new PGSourceViewerConfiguration(sm,doc));

    	// Parse the document a wee while after document opening, to 
    	// not delay it opening.
    	//autoParseTask = new AutoParseActionTimerTask();
    	//autoParseTimer.schedule(autoParseTask,1000);
    	// experiment: schedule it every 2 secs.  FIXME: bit expensive for large
    	// files, don't want to do this really.
    	//autoParseTimer.schedule(autoParseTask, 1000, 2000);
    	//autoParseTimer.schedule(autoParseTask, 1000);
    	// TODO: actually, running the parser in a separate thread at all causes
    	// bad problems.  Needs further investigation.
    	//sm.autoParseDoc(doc);
    }


    /**
	 * A timer for automatically parsing documents in the background
	 */
	// private final Timer autoParseTimer = new Timer("autoParse timer");
	// private final TimerTask autoParseTask = null;
	// TODO: way of interrupting background parsing 
	//private volatile boolean parseCancelled;

//    private class AutoParseActionTimerTask  extends TimerTask {
//    	@Override
//        public void run() {
//    		if (sm != null && doc != null) {
//    			// This schedules a workspace runnable: we ought to track
//    			// whether there is an outstanding parse scheduled or not.
//    			// FIXME: maybe move this timer to session manager to do that.
//    			sm.autoParseDoc(doc);
//    			}
//    	}
//    }
		
	/**
	 * (NB: performSave is called in AbstractTextEditor (implemented by TextEditor, which we extend).)
	 * If symbol support is on, this converts symbols back into ASCII, saves, then converts back into symbols.
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#performSave(boolean, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
    protected void performSave(boolean overwrite, IProgressMonitor progressMonitor) {
		if (doc.isUsingSymbols()) {
			// Do this in a runnable to avoid visual glitches. 
			final boolean fOverwrite = overwrite;
			IWorkspaceRunnable r = new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor1) {
					int pos = getSelectionOffset();	// save resets cursor position, store it
					doc.setUsingSymbols(false);
					ProofScriptEditor.super.performSave(fOverwrite,monitor1);
					doc.setUsingSymbols(true);
					setSelectionOffset(pos);			// and restore it
					ProofScriptDocumentProvider p= (ProofScriptDocumentProvider) getDocumentProvider();
					p.setClean(getEditorInput());
				}
			};
			try {
				doc.getResource().getWorkspace().run(r, null, IWorkspace.AVOID_UPDATE, null);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		} else {
			// The easy case
			super.performSave(overwrite, progressMonitor);
		}
	}

	/**
	 * Return the session manager associated to this editor, or null if none.
	 */
	public SessionManager getSessionManager() {
		return sm;
	}
	
	public SessionManager getConnectedSessionManager() throws CoreException {
	    IEditorInput ei = getEditorInput();
	    if (ei==null) {
	    	if (ProofGeneralPlugin.debug(this)) {
	    		System.out.println("Editor without an input!");
			}
	    	return null;
	    }
	    return ProofGeneralPlugin.getSessionManagerForFile(ei.getName());
	}

	
    /** Support for folding in this editor. */
    private ProjectionSupport projectionSupport;
    
	/**
     * Configure the controls for proof script editors
     * (including folding view, mouse listener, key listener).  
     * 
     @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);

		/*
		 * Projection viewer for folding
		 */
        if (ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_USE_FOLDING)) {
        	ProjectionViewer viewer = (ProjectionViewer)getSourceViewer();
        	projectionSupport = new ProjectionSupport(viewer,getAnnotationAccess(),getSharedColors());
        	//projectionSupport.addSummarizableAnnotationType(PGMarkerMethods.PGPROBLEM_MARKER);
        	projectionSupport.install();
        	//viewer.enableProjection();
        	viewer.doOperation(ProjectionViewer.TOGGLE);
        	if (doc != null) { // no document for external files
        		doc.setProjectionAnnotationModel(viewer.getProjectionAnnotationModel());
        	}
        }
		
        /* 
         * Add a mouse listener to track the mouse location
         * (used to provide location specific commands, e.g. IsaPlanner link-up on sorrys).
         */
        MouseAdapter mouseListener = new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
            	  //System.out.println("click at "+mouseDownLocation);
                mouseDownLocation.x = e.x;
                mouseDownLocation.y = e.y;
            }
        };
        
        /*
         * A key listener which calls autoSymbolise when a space is typed.
         */
        KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.character==' ') {
					autoSymbolise();
				}
            }
        };
        getSourceViewer().getTextWidget().addKeyListener(keyListener);
		getSourceViewer().getTextWidget().addMouseListener(mouseListener);
		
		//add a verify listener for blocking replacements
		VerifyListener verifyListener = new VerifyListener() {
			public void verifyText(VerifyEvent e) {
				if (doc!=null) {
					if (!doc.isEditAllowed(e.start, e.end - e.start, e.text))
						e.doit = false;
				}
            }			
		};
		getSourceViewer().getTextWidget().addVerifyListener(verifyListener);
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(
		//WorkbenchHelp.setHelp(
				getSourceViewer().getTextWidget(),"ed.inf.proofgeneral.editor");
		
    }


	/**
	 * Convert text into symbols as the user types.
	 * Called by a key listener attached to the text widget.
	 */
	void autoSymbolise() {
	    if (!doc.isUsingSymbols() ||
	    		!ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_SYMBOL_SHORTCUTS)) {
			return;
		}
	    int cp=-256;
        try {
            // detect shortcuts and symbols
        	// da: StringBuffer seems wasteful here, could be just char array
            StringBuffer sb = new StringBuffer(Constants.MAX_SYMBOL_LENGTH);
            int pos = getCaretOffset()-1;
                       char c; cp = pos;
            //don't go past the beginning of the line, -AH
            while (cp>0) {
                c = doc.getChar(cp-1);
                if (c==' ' 
                	|| doc.getProverSyntax().isStringDelimiter(c)
                	|| c==StringManipulation.LINEEND.charAt(StringManipulation.LINEEND.length()-1)
                	|| (pos-cp) > Constants.MAX_SYMBOL_LENGTH) {
                	break;
				}
                cp--;
                sb.append(c);
            }
            String word = sb.reverse().toString();
            String sword = doc.getProver().getSymbols().useUnicodeForTyping(word);
            if (word.equals(sword)) {
				return;
			}
			// TODO: insert into symbolisation map in SymbolisedDocument
			// (maybe not necessary if we update it only on every save:
			// although positions will be wrong for searches in edited documents)
            doc.replace(cp,word.length(),sword);
            //System.err.println("autoSymbolise, setCaret "+(cp+sword.length()+1));
            getSourceViewer().getTextWidget().setCaretOffset(cp+sword.length()+1);
            int length = Math.min(word.length(),doc.getLength()-cp);
            getSourceViewer().getTextWidget().redrawRange(cp,length,false);
            getSourceViewer().getTextWidget().update();
        } catch (Exception x) {
        	System.err.println("ERROR in ProofScriptEditor.autoSymbolise, cp="+cp);
        	if (ProofGeneralPlugin.debug(this)) {
        		x.printStackTrace();
        	}
        }
	}

	
	/** Open definition action for this editor. */
	private OpenDefinition openDefnAction;

	/** The paste action for this editor. */
	private PasteAction pasteAction;

	/** A goto action for the context menu */
	private GotoAction gotoHereAction;

	/** Retargetable actions: share instances between editors */
	/* TODO:
	 *  - keep these here; we should only need one instance per editor, right?

	 *  - investigate use of proper Eclipse mechanism for retargeting (see
	 *     RetargetTextEditorAction and property listeners)
	 *     
	 * private static ActivateAction activateAction = new ActivateAction(null) 
	 * 
	 */
	
	/* A record of our actions */
	List<IAction> ourActions = new ArrayList<IAction>();

	private void setAndAddAction(String actionId, IAction action) {
		setAction(actionId, action);
		ourActions.add(action);
	}
	
	/**
	 * Create our specialised actions for this editor, and link the global (retargeted) actions.
	 * @see org.eclipse.ui.editors.text.TextEditor#createActions()
	 */
	@Override
    protected void createActions() {
		super.createActions();

		// Rargeted actions

		setAction(Constants.ACTIVATE_ACTIONID, new ActivateAction(this));
		setAndAddAction(Constants.SEND_ACTIONID, new NextCommandAction(this));
		setAndAddAction(Constants.RESTART_ACTIONID, new RestartAction(this));
		setAndAddAction(Constants.GOTO_ACTIONID, new GotoAction(this));
		setAndAddAction(Constants.SENDALL_ACTIONID, new SendAllAction(this));
		setAndAddAction(Constants.INTERRUPT_ACTIONID, new InterruptAction(this));
		setAndAddAction(Constants.UNDO_ACTIONID, new UndoCommandAction(this));
		setAndAddAction(Constants.UNDOALL_ACTIONID, new UndoAllAction(this));
		setAndAddAction(Constants.PARSE_ACTIONID, new ParseDocAction(this));
		setAndAddAction(Constants.SYMBOLS_ACTIONID, new ToggleSymbolsAction(this));
		setAndAddAction(Constants.ENTERCOMMAND_ACTIONID, new EnterCommandAction(this));
		
		gotoHereAction = new GotoAction(this);
		gotoHereAction.setText("Goto this point");
		ourActions.add(gotoHereAction);
		
		openDefnAction = new OpenDefinition(this);
		openDefnAction.setText("&Open Definition");
		ourActions.add(openDefnAction);

		
		// TODO: try to map the standard "Open Declaration" element
		//setAction("org.eclipse.jdt.ui.edit.text.java.open.editor", openDefnAction);

		ResourceBundle bundle = ProofGeneralPlugin.getResourceBundle();

		IAction standardPaste = new TextOperationAction(
				ResourceBundle.getBundle("org.eclipse.ui.texteditor.ConstructedEditorMessages"),
				"Editor.Paste.", this, ITextOperationTarget.PASTE);
 		pasteAction = new PasteAction(standardPaste,this);
		setAction(ITextEditorActionConstants.PASTE, pasteAction);

		setAction("ContentFormatProposal",
				new TextOperationAction(
						bundle,
						"ContentFormatProposal.",
						this,
						ISourceViewer.FORMAT));

		TextOperationAction action = new TextOperationAction(
				bundle,
				"ContentAssistProposal.",
				this,
				ISourceViewer.CONTENTASSIST_PROPOSALS);

		// Set DefinitionId so that key binding works.
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		setAction(
				"ContentAssistProposal",
				action);

		setAction("ContentAssistTip",
				new TextOperationAction(
						bundle,
						"ContentAssistTip.",
						this,
						ISourceViewer.CONTENTASSIST_CONTEXT_INFORMATION)
		);
		
		// Add prover state change listener to reset actions (NB: this *shouldn't* be
		// necessary for properly implemented actions, but it is a robustness measure).
    	if (sm != null) {
    		sm.getProverState().addObserver(actionClearer);
    	}
	}

	/** An observer for prover state changes which clears the busy status of our actions. */
	private final IProverStateObserver actionClearer = new IProverStateObserver() {
		public void update(Observable o, Object arg) {
		   	assert o instanceof ProverState : "Wrong Observer type";
			ProverState ps = (ProverState)o;
			if (!ps.isAlive()) {
				// The prover has exited, clear busy status of all actions.
				for (IAction action : ourActions) {
					if (action instanceof IClearableAction) {
						((IClearableAction) action).clearBusy();
					}
				}
			}
        }
	};
	

// CLEANUP: old undo.  Document state is responsibility of session manager.
//	/**
//	 * @see ed.inf.proofgeneral.sessionmanager.events.IPGIPListener#pgipEvent(PGIPEvent)
//     */
//    public void pgipEvent(PGIPEvent event) {
//        if (event instanceof CommandCausedErrorEvent) {
//        	try {
//        		final DocElement badCommand = (DocElement) event.parseTree;
//        		// da: NB we can get here with non-document commands
//        		if (badCommand.getProofScript() != null) {
//         			ResourcesPlugin.getWorkspace().run(
//        					new IWorkspaceRunnable() {
//        						public void run(IProgressMonitor monitor) {
//        							ProofScriptDocument doc = badCommand.getProofScript();
//        							// FIXME da: does no-one else do this??
//        							doc.commandUndone(badCommand);
//        						}
//        					},null);
//        		}
//        	} catch (Exception e) {
//        		e.printStackTrace();
//        	}
//        }
//    }

    /**
     * @return the document associated with this editor in the document provider.
     * We assume this document does _not_ change during the editor lifetime (no editor
     * re-use yet for ProofScriptEditor).
     */
     // FIXME: we have lost "polite" behaviour on File->Open with proof script files outside
     // workspace.  This should be restored.  A patch has been made here, but now the first
     // caller of this method throws an exception.  Want to fix things higher up, maybe with
     // extension configuration for document initialisation.
    private ProofScriptDocument getProvidedDocument() {
    	// return this.doc;  da: this seems to be kept in a field, why not use it?
    	IDocumentProvider idp = getDocumentProvider();
        if (idp==null) {
            // da: this happens quite often, perhaps during editor shut-down
            if (ProofGeneralPlugin.debug(this)) {
				System.err.println("Editor without a document provider!");
			}
            return null;
        }
        if (idp instanceof ProofScriptDocumentProvider) {
        	ProofScriptDocumentProvider dp = (ProofScriptDocumentProvider)idp;
        	IDocument doc = dp.getDocument(getEditorInput());
        	if (doc==null || !(doc instanceof ProofScriptDocument)) {
        		if (ProofGeneralPlugin.debug(this)) {
					System.err.println("No Proof Script document found!");
				}
        		return null;
        	}
        	return (ProofScriptDocument) doc;
        }
		if (ProofGeneralPlugin.debug(this)) {
			System.err.println("No Proof Script document found!");
		}
		return null;
    }
    
    /**
     * @return the ProofScript document being edited, or null if non-existent/not yet set (before init() called).
     */
    public ProofScriptDocument getDocument() {
    	return this.doc;
    }


    /** The outline page */
	private PGContentOutlinePage fOutlinePage;


	/**
	 * Returns {@link #fOutlinePage} if requested, else <code>super{@link #getAdapter(Class)}</code>.
	 * @see TextEditor#getAdapter(Class) for details.
	 * TODO reinstate content outline
	 */
	@Override
    public Object getAdapter(Class required) {
		if (IContentOutlinePage.class.equals(required)) {
			if (fOutlinePage == null) {
				fOutlinePage= new PGContentOutlinePage(getDocumentProvider(), this);
				if (getEditorInput() != null) {
					fOutlinePage.setInput(getEditorInput());
				}
			}
			// DEPGRAPH: associate here 
			// GraphMultiPageEditor.fOutlinePage=fOutlinePage;
			return fOutlinePage;
		}
		return super.getAdapter(required);
	}

	/**
	 * @see AbstractDecoratedTextEditor#createSourceViewer(org.eclipse.swt.widgets.Composite, org.eclipse.jface.text.source.IVerticalRuler, int)
	 */
	@Override
    protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
		fAnnotationAccess= createAnnotationAccess();
		fOverviewRuler= createOverviewRuler(getSharedColors());
		ProjectionViewer viewer = new ProjectionViewer(parent,ruler,getOverviewRuler(),true, styles);
		// ensure decoration support has been created and configured.
		viewer.enableProjection();
		getSourceViewerDecorationSupport(viewer);
		return viewer;
	}

    @Override
    public void dispose() {
    	if (doc != null) {
    		doc.getProver().getSymbols().removeSymbolChangeListener(this);
    	}

    	//TODO we'd like to allow 'cancel' but that's hard
    	//we got the info about whether we're shutting down
    	//there are classes WorkbenchAdvisor, EditorManager, PaneFolder.wrapCloseListener, where 'cancel' is allowed
    	//but some of these aren't accessible (WorkbenchAdvisor is internal,
    	//   EditorManager is only called on individual closing editor but is static and called by Workbench with window as parameter;
    	//   PageListener doesn't allow control)
    	//could maybe wrap a CloseListener in the PaneFolder
    	//but it's probably easier to allow re-opening


    	// Try to shut down active script open in this editor (may be less than
    	// ideal if it's open also in another editor -- FIXME).  
    	// Note that dispose() can happen due to errors in startup, in which
    	// case we probably don't have a SM or doc.
    	if (doc != null && sm != null &&
    			sm.getActiveScript() == doc
    			&& !ProofGeneralPlugin.isShuttingDown()) {
    		// NB: this seems to get called even when shutting down...
    		ActivateAction asa = new ActivateAction(this);
    		try {
    			asa.closeDownScript(doc, sm,  false);
    		} catch (Exception e) {
    			ErrorUI.getDefault().signalError(e);
    		}
    	}
    	
    	if (sm != null) {
    		sm.getProverState().deleteObserver(actionClearer);
    	}
    	super.dispose();
    }

    /**
     * !HACK: A workaround for the linux/eclipse bug where unicode characters paste as codes.
     * Can be switched on/off via advanced preferences.
     * TODO: check whether this is still required or not.
     */
    class PasteAction extends TextEditorAction {
    	/**
    	 * The normal paste action.
    	 */
    	public IAction paste;
    	/**
    	 * Create a new paste action with (possible) unicode protection.
    	 * @param paste
    	 */
    	public PasteAction(IAction paste, ITextEditor editor) {
    		super(ProofGeneralPlugin.getResourceBundle(),null,editor);
    		this.paste=paste;
    		setText(paste.getText());
    		setToolTipText(paste.getToolTipText());
    		setAccelerator(paste.getAccelerator());
    		setActionDefinitionId(ITextEditorActionDefinitionIds.PASTE);
    	}

    	@Override
        public void run() {
    		if (!ProofGeneralPlugin.getBooleanPref("Correct for Linux paste bug")) {
    			paste.run();
    			return;
    		}
    		TextTransfer transfer = TextTransfer.getInstance();
    		StyledText widget = getSourceViewer().getTextWidget();
    		Clipboard clipboard = new Clipboard(widget.getDisplay());
    		String clip = (String) clipboard.getContents(transfer);
    		clipboard.dispose();
    		if (clip == null || clip.length()==0) {
    			return;
    		}
    		int si;
    		int ei;
    		// TODO (minor) improve efficiency by keeping track of location & using a stringbuffer
    		do {
    			si = clip.indexOf("\\x{");
    			if (si != -1) {
    				ei = clip.indexOf('}',si);
    				String uc = clip.substring(si+3,ei);
    				char hexCode = (char) Integer.decode("#"+uc).intValue();
    				String unicode = Character.toString(hexCode);
    				String newclip = clip.substring(0,si)+ unicode;
    				if (ei+1<clip.length()) {
    					newclip = newclip + clip.substring(ei+1);
    				}
    				clip = newclip;
    			}
    		} while (si != -1);

    		int offset;
    		int rl;
    		try {
    			ITextSelection s = getSelection();
    			if (s!=null) {
    				offset = s.getOffset();
    				rl = s.getLength();
    			} else {
    				offset = getCaretOffset();
    				rl = 0;
    			}
    			doc.replace(offset,rl,clip);
    			// update stuff
//  			Point pt = widget.getSelectionRange();
//  			getSourceViewer().fireSelectionChanged(pt.x, pt.y);
    			widget.setCaretOffset(offset+clip.length());
    			int length = Math.min(clip.length(),doc.getLength()-offset);
    			widget.redrawRange(offset,length,false);
    			widget.update();
    		} catch (Exception x) {
    			x.printStackTrace();
    		}
    	}
    }

	//////////////////////////////////////////////////////////////////////////////////////
	//
	// Symbols
	//

	public void symbolPreChangeNotification(Element e) {
		if (doc.isUsingSymbols()) {
			setUsingSymbols(false);  // switch to ASCII before symbol changes
		}
	}
	
	public void symbolChangeNotification(Element e) {
		if (ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_SYMBOL_SUPPORT)) {
			doc.setUsingSymbols(true);  // Switch to symbols if desired by default
		}
	}


	/**
	 * Change whether we're using symbols in the display of the document for this editor.
	 * This preserves the dirty status of the editor and attempts to preserve
	 * the editing line.  If the document is active for scripting, it has no effect.
	 * @param newUsingSymbols
	 */
	public void setUsingSymbols(final boolean newUsingSymbols) {
		if (doc != null) { 
			boolean dirty = isDirty();

			int caret = getCaretOffset();
			Position caretpos = new Position(caret,0); 
			try {
	            doc.addPosition(caretpos);
            } catch (BadLocationException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
            }
			doc.setUsingSymbols(newUsingSymbols);
			// FIXME (very minor): we loose here if the caret was in
			// the middle of a token and we symbolise, then the position is marked as deleted
			// and seems to miss subsequent updating to track edits (Q: would this be fixed if
			// processed edits in order?).
			setCaretOffset(caretpos.getOffset());
			doc.removePosition(caretpos);

			ProofScriptDocumentProvider p= (ProofScriptDocumentProvider) getDocumentProvider();
			if (!dirty) {
				p.setClean(getEditorInput());
			} else {
				p.setDirtyAgain(getEditorInput());
			}
		}
	}


	Point mouseDownLocation = new Point(0,0);

	/**
	 * Add actions for trigger words, if we are over one.
	   @see org.eclipse.ui.texteditor.AbstractTextEditor#editorContextMenuAboutToShow(org.eclipse.jface.action.IMenuManager)
	 */
	@Override
    protected void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		int offset= computeOffsetAtLocation(mouseDownLocation.x, mouseDownLocation.y);

		if (offset==-1) {
			return;
		}

		menu.add(new Separator());

		// 1. Goto here action:
		if (doc.isActiveForScripting()) {
			gotoHereAction.setTargetOffset(offset);
			gotoHereAction.setEnabled(true);
		} else {
			gotoHereAction.setEnabled(false);
		}
		menu.add(gotoHereAction);
		
		// 2. Open definition item:  
		IRegion r = PGTextHover.getWordRegion(doc,offset);
		String help = null;
		String word = null;
		if (r != null) {
			try {
				word = doc.get(r.getOffset(),r.getLength());
			} catch (BadLocationException e) {
				// 	do nothing, we haven't got a word
			}
		}
		if (word!=null && word.length()>0) {
			help = sm.hoverHelp.getHelpString(word, doc, null);
		}
		if (help != null) {
			// TODO: would be better if we can prune this on whether we're likely
			// to find the definition of the word
			openDefnAction.setText("&Find Definition of \""+word+"\"");
			openDefnAction.setTerm(word);
			openDefnAction.setEnabled(true);
		} else {
			openDefnAction.setText("&Find Definition ... ");
			openDefnAction.setEnabled(false);
		}
		menu.add(openDefnAction);
	}

	/**
	 * Get the offset where the typing caret is currently sitting.
	 * @return the offset in question (from beginning of file)
	 */
	public int getCaretOffset() {
	    StyledText styledText= getSourceViewer().getTextWidget();
		int caret= widgetOffset2ModelOffset(getSourceViewer(), styledText.getCaretOffset());
	    return caret;
	}

	/**
	 * Set a new offset for the typing caret.
	 * @param offset the new offset (in the document model)
	 */
	public void setCaretOffset(int offset) {
		ProjectionViewer viewer = (ProjectionViewer)getSourceViewer();
		if (viewer != null) {
			viewer.getTextWidget().setCaretOffset(viewer.modelOffset2WidgetOffset(offset));
		}
	}

	/**
	 * Returns the offset of the document's current selection. 
	 * @return an integer representing the selection offset, or {@value #NOTARGET} if none.
	 */
	public int getSelectionOffset() {
		ISelectionProvider sprovider = getSelectionProvider();
		if (sprovider != null) {
			ITextSelection select = getSelection();
			if (select != null) {
				return select.getOffset();
			}
		}
		return NOTARGET;
	}
	
	/**
	 * Moves the cursor to a specific offset
	 * @param offset the offset to which the cursor should be moved
	 */
	public final void setSelectionOffset(int offset) {
		if (offset != NOTARGET) {
			ISelectionProvider sprovider = getSelectionProvider();
			sprovider.setSelection(new TextSelection(offset, 0));
		}
	}
	
	/**
	 * Get the current selection, if there is one.
	 * @return current selection
	 */
	public ITextSelection getSelection() {
	    return (ITextSelection) getSelectionProvider().getSelection();
	}

	/**
	 * Get the display object for this editor. Just exposes the widget's getDisplay.
	 * @return the editor's Display.
	 */
	public Display getDisplay() {
	    return getSourceViewer().getTextWidget().getDisplay();
	}

	@Override
    protected void initializeKeyBindingScopes() {
		super.initializeKeyBindingScopes();
		setKeyBindingScopes(new String[] {
				"ed.inf.proofgeneral.context"
				//, "org.eclipse.ui.textEditorScope"
				});
	}

	/**
	 * Copied from @see org.eclipse.jface.text.TextViewerHoverManager
	 *
	 * Computes the document offset underlying the given text widget coordinates.
	 * This method uses a linear search as it cannot make any assumption about
	 * how the document is actually presented in the widget. (Covers cases such
	 * as bidirectional text.)
	 *
	 * @param x the horizontal coordinate inside the text widget
	 * @param y the vertical coordinate inside the text widget
	 * @return the document offset corresponding to the given point, or -1 for a point not in text
	 */
	public int computeOffsetAtLocation(int x, int y) {
		try {
			TextViewer fTextViewer = (SourceViewer) getSourceViewer();
			StyledText styledText= fTextViewer.getTextWidget();
			int widgetOffset= styledText.getOffsetAtLocation(new Point(x, y));

			if (fTextViewer instanceof ITextViewerExtension5) {
				ITextViewerExtension5 extension= (ITextViewerExtension5) fTextViewer;
				return extension.widgetOffset2ModelOffset(widgetOffset);
			}
			return widgetOffset;
		} catch (IllegalArgumentException ex) {
			return -1;  //indicates we aren't actually in the document
		}
	}

	/**
	 * TODO: we want to overload dirty to cover (a) text edits (ie. really dirty) and (b) being the active script
	 * This would allow the user to cancel closing an editor.
	 */
	public boolean reallyDirty = false;

	/**
	 * Returns the path of the file currently open in the Editor.
	 * @return currently edited file path.
	 */
    public String getFilePath() {
    	IEditorInput ei = getEditorInput();

        if (ei instanceof IFileEditorInput) {
        	return EclipseMethods.getOSPath(((IFileEditorInput)ei).getFile());
        } else if (ei instanceof IPathEditorInput) {
        	return ((IPathEditorInput)ei).getPath().toOSString();
        }
        System.err.println("Unrecognised editor input type: "+ei.toString());
        return null;
    }

    /**
     * Scrolls the view of the current file, such that the topmost line of the
     * view is the one containing the given offset.
     * @param offset the offset to make topmost in the new view
     * @throws BadLocationException
     */
    private void scrollToViewOffset(int offset) throws BadLocationException {
    	ProjectionViewer viewer = (ProjectionViewer)getSourceViewer();
		if (viewer != null) {
			int startline = viewer.getTopIndex();
			int endline = viewer.getBottomIndex();
			int targetline = doc.getLineOfOffset(offset);
			if (targetline <= startline || targetline >= endline) {
				int newtop = targetline - ((endline - startline) / 2);
				if (newtop < 0) {
					newtop = 0;
				}
				viewer.setTopIndex(newtop);
			}
		}
    }


	public void scrollToProcessedOffset() {
		int lockpos = doc.getProcessedOffset() + 1;
		try {
			scrollToViewOffset(lockpos); // FIXME: center of editor would be better
			setCaretOffset(lockpos);
		} catch (BadLocationException e) {
			// silent failure
		}
	}

	public void scrollToQueuedOffset() {
		int lockpos = doc.getLockOffset() + 1;
		try {
			scrollToViewOffset(lockpos); // FIXME: center of editor would be better
			setCaretOffset(lockpos);
		} catch (BadLocationException e) {
			// silent failure
		}
	}


// ====================================================================

	// da: stuff moved from PSD.  Belongs here, breaks circular dependency.

	private final static boolean useMarkersForScrolling = true;  //AH, false might be faster to getSourceViewer, see below
	int lastScrollOffset = -1;

	/** cross-thread updateable reference so that display thread uses most recent value
	 * and thereafter bails out quickly, rather than step through old positions
	 */
	final MutableObject manualSetCaretOffsetFromPosition = new MutableObject(null);

	/** Scrolls the document so the given position is visible ---
	 * moves the cursor to do this;
	 * can be called from background thread.  No action in case the active editor
	 * is viewing a different document.  Only recommended to call this
	 * from immediate UI actions: moving the document when the user
	 * may be typing somewhere else is pretty anti-social!
	 * @param offset
	 */
	public void scrollToViewPosition(int offset) {
		assert offset >= 0 : "Negative offset given";
		assert offset < doc.getLength() : "Offset beyond end of document";
		IResource resource = doc.getResource();
	    if (resource == null) {
			return; // this method doesn't work without underlying resource
		}
//		if (!((IProofScriptEditor)ActiveScriptEditor.getActiveEditor()).getDocument().equals(doc)) {
//			// da: this can happen if the user has changed the active editor.  Don't fret about it.
//			// Exception e = new BadLocationException("trying to scroll '"+title+"' when '"+
//			//	 	((IProofScriptEditor)ProofGeneralPlugin.getActiveEditor()).getTitle()+"' is active");
//			// throw e;
//			// System.err.println(e.getMessage());
//			return;
//		}
		if (useMarkersForScrolling) {
			int line;
			try {
				line = doc.getLineOfOffset(offset);
			} catch (BadLocationException g) {
				return; // shouldn't happen by asserts above, unless document changes meanwhile 
			}
			try {
				IMarker m1 = doc.getResource().createMarker(ProofScriptMarkers.PG_GOTO_MARKER);
					m1.setAttribute(IMarker.LINE_NUMBER, line);
					final IMarker m = m1;
					final IGotoMarker gm = (IGotoMarker)(ProofScriptEditor.this.getAdapter(IGotoMarker.class));
					new DisplayCallable("ProofScriptDocument.scrollToViewPosition/gotoMarker") {
						@Override
                        public Object run() {
							gm.gotoMarker(m);
							try {
								m.delete();
							} catch (Exception e) {
								throw new RuntimeExceptionWrapper(e);
							}
							return null;
						}
					}.runDisplay();
				} catch (CoreException f) {
					// do nothing
				}
		} else {
			final int foff = offset;
			lastScrollOffset = foff;
			final ProofScriptEditor editor = this;
			new DisplayCallable("ProofScriptDocument.scrollToViewPosition/editorScroll") {
				@Override
                public Object run() {
					if (foff!=lastScrollOffset) {
						return null;  //if multiple scrolls submitted
					}
					try {
						editor.scrollToViewOffset(foff);
					} catch (BadLocationException e) {
						throw new RuntimeException(e);
					}
					return null;
				}
			}.runDisplay();
		}
	}
	
	/**
	 * @return the active proof script editor, if one is currently active
	 */
	public static ProofScriptEditor getActiveProofScriptEditor() {
		IEditorPart ed = EclipseMethods.getActiveEditor();
		if (ed instanceof ProofScriptEditor) {
			return (ProofScriptEditor)ed;
		}
		return null;
	}
}
