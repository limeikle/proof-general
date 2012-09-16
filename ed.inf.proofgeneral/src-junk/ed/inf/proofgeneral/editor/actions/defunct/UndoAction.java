/*
 *  $RCSfile: UndoAction.java,v $
 *
 *  Created on 21 Apr 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.actions.defunct;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.editor.ProofScriptEditor;
import ed.inf.proofgeneral.sessionmanager.ScriptingException;
import ed.inf.utils.eclipse.EclipseMethods;
import ed.inf.utils.process.RunnableWithParams;

/**
 * @author Daniel Winterstein
 */
public class UndoAction extends PGProverAction {

	/** For use in {@link #reallyRetract()} */
	private static final String[] opts = new String[] {"Retract", "Cancel"};

	/**
	 * Create an undo action.
	 */
	public UndoAction() {
		setStatusDoneTrigger(STATUS_RUNNING | STATUS_DONE_PROVER);   //display thread is too hard to wait on
		this.setToolTipText("Undo last proof command");
		this.setText("Undo");
		this.setDescription("Undo the last proof command");
	}
	Exception error = null;

	/**
	 * @see ed.inf.proofgeneral.editor.actions.defunct.PGProverAction#onRunFinished(int)
	 */
	@Override
    protected void onRunFinished(int finalStatus) {
		super.onRunFinished(finalStatus);
		if (error!=null) {
			System.out.println("UndoAction had an error: ");  //should we display a dialog?
			if (ProofGeneralPlugin.debug(this)) {
				error.printStackTrace();
			}
			error(error);
			// clear error once we are done?
		}
	}

	@Override
    public synchronized void runSingly() {
		try {
			final ProofScriptEditor editor = getActiveEditor();	
			if (editor != null) {

				//undo needs to run in display thread ... see BUG whether undoAction might mess up
				//actually now it doesn't i don't think; cmd updates can be done elsewhere
				error = null;

				new Action(this).callDefaultDisplayAsyncExec();  // run in display thread

			} else {
				throw new Exception("No active editor!");
			}
		} catch (Exception e) {
			error = e;
			updateStatus(STATUS_FAILED);
		}
	}


	class Action extends RunnableWithParams {
		private final PGAction gA;

		Action(PGAction gA) { super(null); this.gA = gA; }

		public void run() {
			try {
				ProofScriptDocument d = gA.getAndAssociateDocument();
				ProofScriptEditor editor = (gA.getActiveEditor());	

				// -------- can't "undo" a locked file; instead retract it.
				if (d.isLocked()) {
					// TODO da: we might only issue the dialogue if there are dependent
					// files known about.  Otherwise it's close to an undo which we don't
					// hassle the user over.
					if (reallyRetract()) {
						getSessionManager().retractfile(d.getResource().getLocationURI());
					}
					updateStatus(STATUS_DONE_PROVER);


				// -------- can't undo at all -- nothing to undo!
				} else if (d.isFullyUnprocessed()) {
					// NB: this dialog should be prevented by button enablers once they're working
					error(new ScriptingException("Cannot undo: no commands have been processed!"));
					updateStatus(STATUS_IDLE);

				// -------- just undo one step.  Must have right script active firsft.
				} else {
					// FIXME da: I want to use the code below to switch to active script,
					// but it seems a bit tricky.  At the moment the active script is
					// set when the command gets queued, which is too low-level.
					// getSessionManager().setActiveScript(getDocument());
// method removed:					
//					getSessionManager().undoLastScriptCommand(d, gA);
					// da: this is buggy, and we should not do this eagerly.
					// if (d.isFullyUnprocessed()) getSessionManager().retractfile(d.getResource().getLocationURI());
					updateStatus(STATUS_DONE_PROVER);
				}
				editor.scrollToProcessedOffset();
			} catch (Exception e) {
				error = e;
				updateStatus(STATUS_FAILED);
			}
		}
	};

	/**
	 * Prompts the user to confirm that they really want to retract the whole script
	 * (for use when the 'retract' option has not explicitly been called).
	 * @return true if the user agrees; false otherwise.
	 */
	public static boolean reallyRetract() {
		String answer = EclipseMethods.messageDialog("Really retract?", "Because this script has been locked," +
				" this operation will retract the whole file (and any files which depend upon it).\n\nAre " +
				"you sure you wish to retract this script?", opts);
		        // FIXME da: add toggle for this dialog (seems to create problems):
				// true,MessageDialog.QUESTION);
		return (answer.equals(opts[0]));
	}


	/**
	 * Disable if the document has no history
	 */
	@Override
    public boolean decideEnabled() {
		return super.decideEnabled(); // TODO: disable if there is no history
	}

}
