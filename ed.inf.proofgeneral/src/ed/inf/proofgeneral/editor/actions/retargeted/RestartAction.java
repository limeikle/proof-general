/*
 *  $RCSfile: RestartAction.java,v $
 *
 *  Created on 21 Apr 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.actions.retargeted;

import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.ProofScriptMarkers;
import ed.inf.proofgeneral.editor.ProofScriptEditor;
import ed.inf.proofgeneral.sessionmanager.ScriptingException;
import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.proofgeneral.sessionmanager.UserCancelException;
import ed.inf.utils.eclipse.EclipseMethods;

/**
 * Restart the theorem prover. 
 * FIXME: this should be allowed even if other actions are happening
 * (or maybe not: other actions should be returning quickly, but this isn't
 * always happening at the moment).
 */
public class RestartAction extends PGRetargetableAction {

	public RestartAction(ProofScriptEditor editor) {
		super(editor);
//		requiredProverStatusLevel = IGNORE_PROVER;
//		requiredScriptStatusLevel = IGNORE_SCRIPT;
//		setProverOwnershipObject(null);
//		resetOnShutdown = false;
//		setStatusDoneTrigger(STATUS_RUNNING | STATUS_DONE_PROVER);
//		this.setToolTipText("Restart the theorem prover");
//		this.setText("Restart");
//		this.setDescription("Restart the theorem prover");
	}

	@Override
    public void run() {
		if (super.isBusy()) {
			return;
		}
		super.setBusy();
		try {
			final SessionManager sm = 
				getSessionManagerForRunningAction() != null ?
						getSessionManagerForRunningAction() :
							ProofGeneralPlugin.getSomeSessionManager();
			if (sm == null) {
				error(new ScriptingException("Prover not running"));
			}
			String ans = EclipseMethods.messageDialogAsync("Confirm Restart",
					"Are you sure you want to restart the theorem prover?",
					new String[]{IDialogConstants.OK_LABEL,
					IDialogConstants.CANCEL_LABEL},true,MessageDialog.QUESTION);
			if (!ans.equals(IDialogConstants.OK_LABEL)) {
				error(new UserCancelException());
				return;
			}
			new Job("Prover restart") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						restartSession(sm, RestartAction.this);
//				    	updateStatus(STATUS_DONE_PROVER);
				    } catch (Exception e) {
				    	e.printStackTrace();
//				    	updateStatus(STATUS_FAILED);
				    }
				    return Status.OK_STATUS;
				}
			}.schedule();
			//} else throw new Exception("No active editor!");
	  } catch (Exception e) {
		e.printStackTrace();
//	  	updateStatus(STATUS_FAILED);
	  } finally {
			clearBusy();
	  }
	}

	public static void restartSession(SessionManager sm, Action cause) throws IOException, ScriptingException {
		//usually run in background, by caller who wraps this in a thread
		//(DONT run in display thread!)
		sm.stopSession(cause,false);
		ProofScriptMarkers.cleanAllMarkers(); // FIXME da: this is temporary until it's safe to leave markers around between sessions
		sm.startSession();                 // Would be better maybe to have a clean markers method as part of Project -> Clean action.
	}

}
