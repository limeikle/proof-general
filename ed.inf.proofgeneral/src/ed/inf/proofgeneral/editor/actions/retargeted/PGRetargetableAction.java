/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jul 7, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */

package ed.inf.proofgeneral.editor.actions.retargeted;

import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextEditorAction;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.editor.ProofScriptEditor;
import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.utils.eclipse.ErrorUI;

/**
 * Actions which are retargetable, i.e., are re-associated with different editors.
 * This implements a much simpler action notion than PGAction and PGProverAction.
 * 
 * @author  David Aspinall
 */
public class PGRetargetableAction extends TextEditorAction implements IClearableAction {

	/** Editor that was active when setBusy() invoked.  May be null for a non-UI triggered action. */
	private ProofScriptEditor editorForCurrentAction;

	/** Document active when setBusy() invoked.  Or set by useDocumentForAction() */
	private ProofScriptDocument documentForrunningAction;
	
	/** Flag indicating whether action is currently running */
	private boolean isBusy = false;
	
	/**
	 * The normal constructor for retargetable actions
	 * @param editor the initial editor to associate with.
	 */
	public PGRetargetableAction(ProofScriptEditor editor) {
		super(ProofGeneralPlugin.getResourceBundle(),null,editor);
	}
	
	/**
	 * A constructor which sets the document ready for an action. 
	 * Will have null editor, careful!
	 * Callers should set the editor or a specific session manager to be used.
	 */
	public PGRetargetableAction(ProofScriptDocument doc) {
		// NB: not sure if initialising TextEditorAction with a null editor
		// is a good idea...
		super(ProofGeneralPlugin.getResourceBundle(),null,null);
		this.documentForrunningAction = doc;
	}
	
	
	/**
	 * @return the currently associated ProofScriptEditor, or null
	 * if the action has been associated with another kind of editor (or no editor)
	 */
	public ProofScriptEditor getEditor() {
		ITextEditor editor = super.getTextEditor();
		if (editor instanceof ProofScriptEditor) {
			return (ProofScriptEditor)editor;
		}
		return null;
	}
	
	
	/**
	 * Mark an action as busy, and keep a record of the current editor
	 * and its document.   This allows the document or editor to
	 * be retrieved later even if the action has been retargeted meanwhile.
	 */
    public synchronized void setBusy() {
    	isBusy = true;
    	
    	ProofScriptEditor editor = getEditor();
    	if (editorForCurrentAction==null)
    		editorForCurrentAction = editor;
    	assert editorForCurrentAction==editor;
    	
		if (editor != null) {
			 ProofScriptDocument doc = editor.getDocument();
			 if (documentForrunningAction==null)
				 documentForrunningAction = doc;
			 assert documentForrunningAction==doc;
		}
	}
    
    /**
     * Clear the flag that this action is busy; remove references
     * to editor and document.
     */
    public synchronized void clearBusy() {
    	isBusy = false;
    	editorForCurrentAction = null;
    	documentForrunningAction = null;
    }
	
    public boolean isBusy() {
    	return isBusy; 
    }
    
	/**
	 * @return the editor for the currently running action, or null if not set
	 */
	public ProofScriptEditor getEditorForRunningAction() {
		return editorForCurrentAction;
	}
	
	/**
	 * @return the document for the currently running action; not sure if can be null
	 */
	public ProofScriptDocument getDocumentForRunningAction() {
		return documentForrunningAction;
	}
	
	/** forces the session manager to be used, overriding one specified by editor; 
	 * mainly for use in non-ui contexts (where editor is null);
	 * can set null to clear any specification (ie use SM suggested by editor) */
	protected void setSpecifiedSessionManager(SessionManager sm) {
		this.specifiedSessionManager = sm;
	}
	
	private SessionManager specifiedSessionManager = null;
	
	/** returns a SessionManager specified by caller, if there is one (programmatic usages),
	 * or suggested by editor (normal usage); or null if editor is also null (shouldn't happen)
	 */ 
	public SessionManager getSessionManagerForRunningAction() {
		if (specifiedSessionManager != null)
			return specifiedSessionManager;
		if (editorForCurrentAction != null) {
			return editorForCurrentAction.getSessionManager();
		}
		return null;
	}

	/**
	 * Signal an error (since run can't throw exceptions)
	 * @param e the exception to report;
	 * @param displayDialog whether or not to notify the user.
	 */
	protected void error(Exception e, boolean displayDialog) {
		System.err.println("ACTION ERROR: "+e.getLocalizedMessage()); // since otherwise we miss errors
		ErrorUI.getDefault().signalError(e, displayDialog);
	}
	
	/**
	 * Signal an error (since run can't throw exceptions), giving
	 * a dialog to notify the user.
	 * @param e
	 */
	protected void error(Exception e) {
		error(e,true);
	}

	
}
