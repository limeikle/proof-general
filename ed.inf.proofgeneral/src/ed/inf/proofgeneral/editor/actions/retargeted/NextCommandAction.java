/*
 *  $RCSfile: NextCommandAction.java,v $
 *
 *  Created on 21 Apr 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.actions.retargeted;

import ed.inf.proofgeneral.document.DocElement;
import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.editor.ProofScriptEditor;
import ed.inf.proofgeneral.sessionmanager.PGIPSyntax;
import ed.inf.proofgeneral.sessionmanager.SessionManager;


/**
 * Send the next command in the script out to the prover.
 * @author David Aspinall
 */

public class NextCommandAction extends GotoAction {
	
	private static class SendNextTarget implements GotoAction.IDocumentTarget {
        public int getTargetOffset(ProofScriptDocument doc) {
			int lock = doc.getLockOffset();
			int next = doc.skipSpacesForward(lock+1)+1;
			DocElement nextElement = doc.findNext(PGIPSyntax.ANYITEM, lock+1);
			if (nextElement!=null) {
				int nextReal = nextElement.getStartOffset() + 1;
				if (next!=nextReal) {
					System.out.println("unknown whitespace found (comment); next non-whitespace at "+next+"; next command at "+nextReal+"; skipping to next command");
					next = nextReal;
				}
			}
			if (next<=doc.getLength()) {
				return next;
			}
			return lock; // at end of document already
        }
        static SendNextTarget instance = new SendNextTarget();
	}
	
 
	/**
	 * Default constructor.
	 */
	public NextCommandAction(ProofScriptEditor editor) {
		super(editor, SendNextTarget.instance);
		this.setToolTipText("Process the next command");
		this.setText("Send Next");
		this.setDescription("");
	}

	/**
	 * Sets up a non-toplevel (i.e. back-end) version of this action.
	 * @param doc the document upon which this action should be performed.
	 */
	public NextCommandAction(ProofScriptDocument doc, SessionManager sm) {
		super(doc, sm, SendNextTarget.instance);
	}
}
