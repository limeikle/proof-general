/*
 *  $RCSfile: ScriptManager.java,v $
 *
 *  Created on Dec 2, 2006 by da
 *  part of Proof General Eclipse
 */

package ed.inf.proofgeneral.sessionmanager;

import ed.inf.proofgeneral.document.ProofScriptDocument;

/**
 * This class implements script management methods for controlling the processing
 * of a ProofScriptDocument.
 */
public class ScriptManager {

	private ProofScriptDocument doc;
	// FIXME: sm to become prover command queue
	private SessionManager sm;

	public ScriptManager(final ProofScriptDocument doc, final SessionManager sm) {
		this.doc = doc;
		this.sm = sm;
	}

	public ProofScriptDocument getDocument() {
		return doc;
	}

	public SessionManager getManager() {
		return sm;
	}


}
