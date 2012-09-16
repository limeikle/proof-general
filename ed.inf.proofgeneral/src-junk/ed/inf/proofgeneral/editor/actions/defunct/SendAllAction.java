/*
 *  $RCSfile: SendAllAction.java,v $
 *
 *  Created on 21 Apr 2004
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.actions.defunct;

import ed.inf.proofgeneral.document.ProofScriptDocument;

/**
 * Repeat SendCommandAction until we get an error
 */
public class SendAllAction extends GotoAction {

	/**
	 * Default constructor.
	 * Use {@link #SendAllAction(ProofScriptDocument)} to set up a
	 * non-top-level instance.
	 */
	public SendAllAction() {
		this.setToolTipText("Process the rest of this file");
		this.setText("Send All");
		this.setDescription("");
	}

	/**
	 * Sets up a non-toplevel (i.e. back-end) version of this action.
	 * @param doc the document upon which this action should be performed.
	 */
	public SendAllAction(ProofScriptDocument doc) {
		this();
		setTopLevel(false);
		setDocument(doc);
	}

	public void runSingly() {
		try {
			ProofScriptDocument doc = getAndAssociateDocument();
			if (doc==null) {
				throw new Exception("no document available");
			}
			targetOffset = doc.getLength()-1;
			super.runSingly();
		} catch (Exception e) {
			updateStatus(STATUS_FAILED);
			e.printStackTrace();
			error(e);
		}
	}
}
