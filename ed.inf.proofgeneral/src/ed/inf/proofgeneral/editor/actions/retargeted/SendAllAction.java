/*
 *  $RCSfile: SendAllAction.java,v $
 *
 *  Created on 21 Apr 2004
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.actions.retargeted;

import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.editor.ProofScriptEditor;
import ed.inf.proofgeneral.sessionmanager.SessionManager;

/**
 * Process the whole document, using GotoAction.
 * @author David Aspinall
 */
public class SendAllAction extends GotoAction {

	private static class SendAllTarget implements GotoAction.IDocumentTarget {
        public int getTargetOffset(ProofScriptDocument doc) {
			return doc.getLength()-1;
        }
		static SendAllTarget instance = new SendAllTarget();
	}

	/**
	 * Default constructor.
	 */
	public SendAllAction(ProofScriptEditor editor) {
		super(editor, SendAllTarget.instance);
		this.setToolTipText("Process the rest of this file");
		this.setText("Send All");
		this.setDescription("");
	}

	/**
	 * Sets up a non-toplevel (i.e. back-end) version of this action.
	 * @param doc the document upon which this action should be performed.
	 */
	public SendAllAction(ProofScriptDocument doc, SessionManager sm) {
		super(doc, sm, SendAllTarget.instance);
	}
}
