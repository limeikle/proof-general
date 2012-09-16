/*
 *  $RCSfile: ActivateScriptingAction.java,v $
 *
 *  Created on 21 Apr 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.actions.defunct;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.editor.ProofScriptEditor;
import ed.inf.proofgeneral.sessionmanager.ScriptingException;
import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.utils.eclipse.EclipseMethods;

/**
 * Activates a script for scripting (only one editor can be active at a time).
 * @author Daniel Winterstein
 */
public class ActivateScriptingAction extends PGProverAction {

	//private static final String[] noswitch_opts = new String[] { "Raise active (not implemented)", "Cancel" };
	private static final String[] close_active_opts = new String[] { "Retract", "Process", "Cancel" };
	private static final String[] close_active_nocancel = new String[] { close_active_opts[0], close_active_opts[1]};

	public ActivateScriptingAction() {
		setStatusDoneTrigger(STATUS_RUNNING | STATUS_DONE_PROVER);
		setProverOwnershipObject(null);  //let the action we call take ownership
		// da: this seems wrong, we don't require a script active to turn on
		// scripting?!.  I'm not sure if the default is right either.
		// requiredScriptStatusLevel = ANY_SCRIPT_ACTIVE;
		this.setToolTipText("Find/toggle active script");
		this.setText("Activate");
		this.setDescription("Activate this editor for scripting or find the current active editor.");
	}

	/**
	 * Does nothing but call super at present.
	 * @see ed.inf.proofgeneral.editor.actions.defunct.PGProverAction#decideEnabled()
	 */
	@Override
    public boolean decideEnabled() {
		return super.decideEnabled();
	}

	@Override
    public void runSingly() {
		try {
				activateAction();
				updateStatus(STATUS_DONE_ALL);
		} catch (ScriptingException e) {
			error(e,true);
			updateStatus(STATUS_FAILED);
		}
	}


    private void activateAction() throws ScriptingException {
		if (getActiveEditor() == null) {
			throw new ScriptingException("Activate script: no active editor!");
		}
		final SessionManager sm = getSessionManager();
		final ProofScriptEditor editor = getActiveEditor();
		final ProofScriptDocument document = (ProofScriptDocument)(editor.getDocumentProvider().getDocument(editor.getEditorInput()));
		// da: are we thread safe inside here?  Needs protecting if not.
		// Beware that logic here is closely abound to semantics of activeScriptChangePossible().
		if (sm.activeScriptChangePossible()) {
			if (sm.isActiveScript(document)) {
				sm.clearActiveScript();
				return;
			}
			if (!sm.hasActiveScript()) {
				// FIXME da: if the script has been fully processed, this triggers an automatic
				// retraction inside Isabelle.  As in the Undo case we should give a warning
				// action that this is about to happen.  At the moment, what happens in fact
				// is that an error
				// ISABELLE SPECIFIC maybe
				sm.setActiveScript(document);
				return;
			}
		}
		if (sm.isActiveScript(document)) {
			editor.scrollToProcessedOffset();
			return;
		}
		// Another script is active, let's try to switch to it
        // INVARIANT: the active script should always be displayed in some editor,
        // so this should succeed.
        ProofScriptDocument activedoc = sm.getActiveScript();
        IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IEditorPart edpart = EclipseMethods.findEditorForDoc(activedoc,activePage);
        if (edpart !=null) {
        	activePage.activate(edpart);
        	editor.scrollToProcessedOffset();
        } else {
        	// This is potentially a bad situation: although the editor may be open in another
        	// Workbench page, and we don't go as far as searching them.
        	throw new ScriptingException("Can't find an editor on the active script (in the current window)");
        }
	}


// FIXME: cleanup junk here.

		// da: let's avoid these problematic dialogs for now and just fail silently.
		// That's simple and reliable.  The user isn't allowed to switch, let them
		// fix it themselves without being prompted with questions (too many dialogs
		// makes for bad UI anyway).
		/*
				String response = EclipseMethods.messageDialog("Could not switch active script", "Could not select this script as active.\n\n" +
						"This is likely because another script is active.  " +
						"Please fully process or retract the active script before switching.\n\n" +
						"What would you like to do now?", noswitch_opts);
				updateStatus(STATUS_FAILED);
		 */
		/*
				if (response == noswitch_opts[0]) {
					// find the active buffer and unbury it
					// TODO: this actually gets the active editor, not the editor of the active script -- whoops!
					// need to tie it to ProofGeneralPlugin.getActiveSessionManager().getActiveScript() somehow.
					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					// page.bringToTop(page.getActivePart());
					// page.activate(ProofGeneralPlugin.getActiveEditor());
				}

			}
			//put in background thread
			// FIXME da: I think this case can be removed for simplicity for time being.
			// We just want a simple interface that *works* at the moment.
			// Use the button now to toggle a script on/off, forget about
			// the process/undo thing.
			/*
				new PooledRunnable() {
					public void run() {
						try {
							if (closeDownScript(sm.getActiveScript(),sm,true))
								sm.setActiveScript(document);
							updateStatus(STATUS_DONE_PROVER);
						} catch (Exception e) {
							updateStatus(STATUS_FAILED);
							e.printStackTrace();
						}
					}
				}.start();
			} */


	/**
	 * Seeks confirmation of a user's decision to work in a script which is not the active one.
	 * Options are to process current script completely, to retract it completely, or to do nothing.
	 * @return <b>true</b> to process active script<br/>
	 * 			<b>false</b> to retract active script<br/>
	 * 			<b>null</b> if the user has cancelled.
	 */
	@SuppressWarnings("boxing")
    public static Boolean switchScriptOptions(boolean allowCancel) {
		String message = "You already have an active" +
		" script.  In order to work on a different script must either completely" +
		" process the active script, or fully retract it.\n\nWhat would you like to do?";
		String response = EclipseMethods.messageDialog("Really switch scipts?", message, allowCancel?close_active_opts:close_active_nocancel);
		if (response.equals(close_active_nocancel[1])) {
			return true;
		} else if (response.equals(close_active_nocancel[0])) {
			return false;
		}
		return allowCancel ? null : false;
	}

	/**
	 * Checks for the best course of action when an active script is closed.
	 * @param script the script being closed
	 * @param sm active session manager
	 * @param canCancel a hack which lets us know when it's not possible to cancel our shutdown
	 * @return true if the script has been released successfully and close can continue; false otherwise.
	 */
	public boolean closeDownScript(ProofScriptDocument script, SessionManager sm, boolean canCancel) {
		// should use sm method here.  But this code is disabled for now anyway.
		if (script.isFullyUnprocessed() || script.isLocked() || script.isFullyProcessed()) {
			//sm.freeDocument(script); // nothing to do
            try {
            	sm.clearActiveScript();
    			return true;
            } catch (ScriptingException e) { // if something bad happened while closing, prevent close to be safe.
            	e.printStackTrace();
            	return false;
            }
		}

		try {
			if (ProofGeneralPlugin.debug(this)) {
				System.out.println("Retracting part-processed script "+script.getTitle()+".");
			}
			new UndoAllAction(script).run();
			return true;
			// -- Allows user to choose to retract or process a part-processed script (or cancel) --
			/*
			Boolean process = switchScriptOptions(canCancel);
			if (process == null) {	// user changed his/her mind.
				return false;
			} else { // do something
				// either fully process
				if (process) new SendAllAction(script).run();
				// or fully retract -- FIXME retracting from here does not work!
				new UndoAllAction(script).run();
				// now show we've done something
				return true;
			}
			*/
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		/*
		if (choice.startsWith("Force")) {
			if (!canCancel) return true;
			choice = EclipseMethods.messageDialogAsync("Warning",
					//	    			"Forcing switches between proof scripts may result in faulty scripts."
					"Forcing a switch from a proof script in this state will probably result in an inconsistent prover state."
					,new String[] {"I don't care","Cancel"},true,MessageDialog.WARNING);

			try {
			{
				//final IAction action = this;
				synchronized (this) {
					new RunnableWithParams(null) {
						public void run() {
							String choice = EclipseMethods.messageDialog("Warning",
									//	    			"Forcing switches between proof scripts may result in faulty scripts."
									"Forcing a switch from a proof script in this state will probably result in an inconsistent prover state."
									,new String[] {"I don't care","Cancel"},true,MessageDialog.WARNING);
							result.set(choice);
							synchronized (action) {
								action.notify();
							}
						}
					}.callDefaultDisplayAsyncExec();
					this.wait();
				}
			}
			} catch (InterruptedException e) {
				System.err.println("ActivateSwitchAction thread interrupted");
			}
			choice = (String)result.get();
			*/

	}

}
