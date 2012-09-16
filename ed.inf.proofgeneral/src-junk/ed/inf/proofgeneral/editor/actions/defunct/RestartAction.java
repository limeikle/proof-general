/*
 *  $RCSfile: RestartAction.java,v $
 *
 *  Created on 21 Apr 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.actions.defunct;

import java.io.IOException;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;

import ed.inf.proofgeneral.editor.actions.defunct.PGAction;
import ed.inf.proofgeneral.editor.actions.defunct.PGProverAction;
import ed.inf.proofgeneral.sessionmanager.ScriptingException;
import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.proofgeneral.sessionmanager.UserCancelException;
import ed.inf.proofgeneral.sessionmanager.events.PGIPEvent;
import ed.inf.utils.eclipse.EclipseMethods;
import ed.inf.utils.eclipse.PGMarkerMethods;
import ed.inf.utils.process.PooledRunnable;

/**
 * Restart the theorem prover. 
 * FIXME: this should be allowed even if other actions are happening
 * (or maybe not: other actions should be returning quickly, but this isn't
 * always happening at the moment).
 */
public class RestartAction extends PGProverAction {

	public RestartAction() {
		requiredProverStatusLevel = IGNORE_PROVER;
		requiredScriptStatusLevel = IGNORE_SCRIPT;
		setProverOwnershipObject(null);
		resetOnShutdown = false;
		setStatusDoneTrigger(STATUS_RUNNING | STATUS_DONE_PROVER);
		this.setToolTipText("Restart the theorem prover");
		this.setText("Restart");
		this.setDescription("Restart the theorem prover");
	}

	public void pgipEvent(PGIPEvent event) {
	}

//	public void pgipEvent(PGIPEvent event) {
//		if (isEnabled()) {
//			if (event instanceof PGIPShutdown) {
//				setEnabled();  //shutting down
//			}
//			return ;  //if we're enabled, we only care about shutdown events
//		}
//		//not enabled -- poll on each event
//		setEnabled();
//	}
//
//	public boolean handleOurPgipEvent(PGIPEvent event) {
//		//not called
//		return false;
//	}

	public void runSingly() {
		try {
			//there's no reason we need an active editor, is there?  -AH
			//if (getActiveEditor() != null) {
				String ans = EclipseMethods.messageDialogAsync("Confirm Restart",
						"Are you sure you want to restart the theorem prover?",
						new String[]{IDialogConstants.OK_LABEL,
						IDialogConstants.CANCEL_LABEL},true,MessageDialog.QUESTION);
				if (!ans.equals(IDialogConstants.OK_LABEL)) {
					error(new UserCancelException());
					return;
				}
				new PooledRunnable() {
					public void run() {
				    try {
				    	restartSession(getSessionManager(), RestartAction.this);
				    	updateStatus(STATUS_DONE_PROVER);
				    } catch (Exception e) {
				    	e.printStackTrace();
				    	updateStatus(STATUS_FAILED);
				    }
					}
				}.start();
			//} else throw new Exception("No active editor!");
	  } catch (Exception e) {
		e.printStackTrace();
	  	updateStatus(STATUS_FAILED);
	  }
	}

	public static void restartSession(SessionManager sm, PGAction cause) throws IOException, ScriptingException {
		//usually run in background, by caller who wraps this in a thread
		//(DONT run in display thread!)
		sm.stopSession(cause,false);
		PGMarkerMethods.cleanAllMarkers(); // FIXME da: this is temporary until it's safe to leave markers around between sessions
		sm.startSession();                 // Would be better maybe to have a clean markers method as part of Project -> Clean action.
	}

}
