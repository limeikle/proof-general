/*
 *  $RCSfile: UndoAllAction.java,v $
 *
 *  Created on 21 Apr 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.actions.defunct;

import ed.inf.proofgeneral.document.ProofScriptDocument;

/**
 * Uses GotoAction to unroll a script. @see GotoAction
 * @author Daniel Winterstein
 */
public class UndoAllAction extends GotoAction {

	/**
	 * Default constructor.
	 * Use {@link #UndoAllAction(ProofScriptDocument)} to set up a
	 * non-top-level instance.
	 */
	public UndoAllAction() {
		super();
		requiredScriptStatusLevel = CURRENT_SCRIPT_ACTIVE_MAYBE_LOCKED;
		setToolTipText("Undo to the beginning of this file");
		setText("Undo All");
		setDescription("");
	}

	/**
	 * Sets up a non-toplevel (i.e. back-end) version of this action.
	 * @param doc the document upon which this action should be performed.
	 */
	public UndoAllAction(ProofScriptDocument doc) {
		this();
		setTopLevel(false);
		setDocument(doc);
	}

	/**
	 * @see ed.inf.proofgeneral.editor.actions.defunct.GotoAction#runSingly()
	 */
	public void runSingly() {
		try {
			setTargetOffset(0);
			super.runSingly();
		} catch (Exception e) {
			updateStatus(STATUS_FAILED);
			e.printStackTrace();
			error(e);
		}
	}

	/**
	 * TODO Enhance (Currently just returns true)
	 */
	public boolean decideEnabled() {
		// FIXME da: disable these buggy checks for now.
		// Should err on side of enabling.  Buggy disabled buttons are seriously
		// annoying!
		//		if (!super.decideEnabled()) return false;
		//		if (getSessionManager()==null) return false;
		//		//seems 'null' can happen temporarily while active editor swaps out -AH
		//		ProofScriptDocument doc = getSessionManager().getActiveScript();
		//		if (doc==null) doc = getDocument();
		//		if (doc==null) return false;
		//		if (doc.fLockOffset<1) return false;
		return true;
	}

}
