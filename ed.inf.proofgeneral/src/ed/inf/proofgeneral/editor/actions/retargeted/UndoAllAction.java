/*
 *  $RCSfile: UndoAllAction.java,v $
 *
 *  Created on 21 Apr 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.actions.retargeted;

import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.editor.ProofScriptEditor;
import ed.inf.proofgeneral.sessionmanager.SessionManager;

/**
 * Uses GotoAction to unroll a script. @see GotoAction
 * @author Daniel Winterstein
 * @author David Aspinall
 */
public class UndoAllAction extends GotoAction {

	private static class UndoAllTarget implements GotoAction.IDocumentTarget {
        public int getTargetOffset(ProofScriptDocument doc) {
			return -1;
        }
		static UndoAllTarget instance = new UndoAllTarget();
	}
 
	/**
	 * Default constructor.
	 */
	public UndoAllAction(ProofScriptEditor editor) {
		super(editor,UndoAllTarget.instance);
	}

	/**
	 * Sets up a non-toplevel (i.e. back-end) version of this action.
	 * @param doc the document upon which this action should be performed.
	 */
	public UndoAllAction(ProofScriptDocument doc, SessionManager sm) {
		super(doc, sm, UndoAllTarget.instance);
	}
}
