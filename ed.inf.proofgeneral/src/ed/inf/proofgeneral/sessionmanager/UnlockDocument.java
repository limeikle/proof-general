/*
 *  $RCSfile: UnlockDocument.java,v $
 *
 *  Created on 29 Apr 2005 by Alex Heneveld
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.sessionmanager;

import ed.inf.proofgeneral.document.ProofScriptDocument;

//FIXME class not needed anymore
// da: I'm testing without it.
/** 
 * Simple Runnable which unlocks the specified document.
 * put in its own class because on shutdown, SessionManager.unlockAll
 * would throw java.lang.NoClassDefFoundError: ed/inf/proofgeneral/sessionmanager/SessionManager$UnlockDocument
 * (not sure moving here will help at all; probably the ClassLoaders have gone) 
 */
public class UnlockDocument implements Runnable {
	ProofScriptDocument fdoc;		
	public UnlockDocument(ProofScriptDocument fdoc) {
		this.fdoc = fdoc;
	}
	public void run() {
		fdoc.unlock();
	}
	public static void nop() {}
}