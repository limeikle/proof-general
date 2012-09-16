/*
 *  $RCSfile: InterruptAction.java,v $
 *
 *  Created on 21 Apr 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.actions.defunct;

import ed.inf.proofgeneral.sessionmanager.SessionManager;

/**
 * @author Daniel Winterstein
 * TODO all
 */
public class InterruptAction extends PGProverAction {

	/* (non-Javadoc)
	 * @see ed.inf.actions.DanAction#run(ed.inf.actions.Program, java.util.Map)
	 */
	public InterruptAction() {
		super();
		//Signal.raise(new Signal("hello"));
		/*char c;
		 CharSet co = new CharSet();*/
		requiredProverStatusLevel = PROVER_ALIVE;
		requiredScriptStatusLevel = IGNORE_SCRIPT;
		setProverOwnershipObject(null);
		this.setToolTipText("Interrupt command being processed");
		this.setText("Interrupt");
		this.setDescription("Interrupt the command being processed");
	}


	protected static final String INTERRUPT_COMMAND = "<interruptprover interruptlevel='interrupt' />";


	public void run() {
		//doesn't need to set any status
		try {
			if (getActiveEditor() != null) {
				SessionManager sm = getSessionManager();
				sm.doInterrupt();
			} else {
				throw new Exception("No active editor!");
			}
		} catch (Exception e) {
			error(e);
		}
	}

	/**
	 * Does nothing but call super at present.
	 * TODO Disable if the prover is free and there is no queue?
	 */
	public boolean decideEnabled() {
		return super.decideEnabled();
		//maybe disable if prover is free and no queue--
		//not sure we get enough events for this to be reliably updated;
		//and it's still buggy enough that we want to be able to interrupt
		//even when (or because) the internal model is screwy
		//if (!super.decideEnabled()) return false;
//		try {
//			if (!getSessionManager().proverState.isBusy() && getSessionManager().isEmptyQueueAndEvents()) {
//				return false;
//			}
//		} catch (Exception x) {
//			x.printStackTrace();
//			return false;
//		}
		//return true;
	}

}
