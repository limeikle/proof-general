/*
 *  $RCSfile: UndoCommandAction.java,v $
 *
 *  Created on 21 Apr 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.actions.retargeted;

import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.editor.ProofScriptEditor;
import ed.inf.proofgeneral.sessionmanager.SessionManager;

/**
 * Undo the last processed command
 * @author David Aspinall
 */
public class UndoCommandAction extends GotoAction {

	static class UndoLastTarget implements GotoAction.IDocumentTarget {
		public int getTargetOffset(ProofScriptDocument doc) {
			int lock = doc.getLockOffset();
			System.out.println("undoing to reach "+(lock-1));
			//TODO "undo" should retract (or be disallowed) if theory has been "ended"
			//(checking "fully processed" is not enough, e.g. text/comment after "end")
			if (lock>-1) {
				return lock-1;
			}
			return lock; // at start of document already
        }
		static UndoLastTarget instance = new UndoLastTarget();		
	}
 
	/**
	 * Default constructor.
	 */
	public UndoCommandAction(ProofScriptEditor editor) {
		super(editor, UndoLastTarget.instance);
		this.setToolTipText("Process the next command");
		this.setText("Send Next");
		this.setDescription("");
	}

	/**
	 * Sets up a non-toplevel (i.e. back-end) version of this action.
	 * @param doc the document upon which this action should be performed.
	 */
	public UndoCommandAction(ProofScriptDocument doc, SessionManager sm) {
		super(doc, sm, UndoLastTarget.instance);
	}
	
}
