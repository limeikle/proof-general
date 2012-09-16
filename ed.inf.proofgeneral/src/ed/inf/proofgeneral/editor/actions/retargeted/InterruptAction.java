/*
 *  $RCSfile: InterruptAction.java,v $
 *
 *  Created on 21 Apr 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.actions.retargeted;

import ed.inf.proofgeneral.editor.ProofScriptEditor;
import ed.inf.proofgeneral.sessionmanager.SessionManager;

/**
 * Action for interrupting the session manager associated with the active editor.
 * @author Daniel Winterstein
 */
public class InterruptAction extends PGRetargetableAction {

	public InterruptAction(ProofScriptEditor editor) {
		super(editor);
	}

	@Override
    public void run() {
		setBusy();
		try {
			SessionManager sm = getSessionManagerForRunningAction();
			if (sm != null) {
				sm.doInterrupt();
			} else {
				throw new Exception("No active editor!");
			}
		} catch (Exception e) {
			error(e);
		} finally {
			clearBusy();
		}
	}
}
