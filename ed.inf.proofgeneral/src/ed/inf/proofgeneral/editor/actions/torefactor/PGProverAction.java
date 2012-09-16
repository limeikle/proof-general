/*
 *  $RCSfile: PGProverAction.java,v $
 *
 *  Created on 20 Apr 2005 by Alex Heneveld
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.actions.torefactor;

import org.eclipse.ui.texteditor.ITextEditor;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.sessionmanager.events.CommandCausedErrorEvent;
import ed.inf.proofgeneral.sessionmanager.events.CommandProcessed;
import ed.inf.proofgeneral.sessionmanager.events.PGIPEvent;
import ed.inf.utils.exception.KnownException;

/**
 * PGProverAction -- specialised event listeners which update the status for error and prover messages
 * <p/>
 * override runSingly to have this action try to get runnable status first,
 * otherwise override run
 * <p/>
 * typical single-entry status model is to:
 * setStatusDoneTrigger(NEEDED_BITS) on initialisation;
 * then start run method with trySetStatus(STATUS_RUNNING),
 * then make sure all exit paths either set all bits or set DONE_FAILED.
 *
 */
// TODO da: if we keep this ownership notion, it might be better implemented in
// the SM than the GUI code.
public class PGProverAction extends PGAction {

	public PGProverAction() {
		super();
		requiredProverStatusLevel = PROVER_AVAILABLE;
		requiredScriptStatusLevel = CURRENT_SCRIPT_ACTIVE;
		afterPGProverActionConstructor();
	}
	
	public PGProverAction(ITextEditor editor) {
		super(editor);
		requiredProverStatusLevel = PROVER_AVAILABLE;
		requiredScriptStatusLevel = CURRENT_SCRIPT_ACTIVE;
		afterPGProverActionConstructor();
	}


	/**
	 * Whether this action should is disabled when the action is running.
	 * True by default.
	 */
	protected boolean disabledOnPositiveStatus = true;

	/** an overridable method called after PGProverAction construction; does nothing by default;
	 *  mainly used for inline subclasses to set required levels */
	public void afterPGProverActionConstructor() {}

	protected boolean resetOnShutdown = true;

	@Override
    public boolean handleProverShutdown(PGIPEvent event) {
		if (resetOnShutdown) {
			if (getStatus()!=0) {
				updateStatus(0);
				return true;
			}
		}
		super.handleProverShutdown(event);
		return true;
	}

	/** handles the event if it's one of a certain type; default calls pgipEventXXX methods */
	@Override
    public boolean handleOurPgipEvent(PGIPEvent event) {
		if (event instanceof CommandCausedErrorEvent) {
			pgipEventCausedError((CommandCausedErrorEvent)event);
			return true;
		}
		if (event instanceof CommandProcessed) {
			pgipEventCommandProcessed((CommandProcessed)event);
			return true;
		}
		return false;
	}


	/**
	 * Does nothing, but could be overridden.
	 * <b>Careful</b>: if this causes the command to end,
	 * ensure that later commands don't affect this.
	 */
	// used to updates status field with 'failed'
	public void pgipEventCausedError(CommandCausedErrorEvent event) {
		//updateStatus(STATUS_DONE_FAILED);
	}

	/**
	 * Updates status field with 'done prover'
	 */
	public void pgipEventCommandProcessed(CommandProcessed event) {
		updateStatus(STATUS_DONE_PROVER);
	}

	/**
	 * Informs whether or not this action should be enabled right now.
	 * Does nothing except call super at present.
	 * @return false if the action should be disabled, true otherwise.
	 * @see ed.inf.proofgeneral.editor.actions.defunct.PGAction#decideEnabled()
	 */
	@Override
    public boolean decideEnabled() {
	  // if (disabledOnPositiveStatus && getStatus()>0) return false;
	  return super.decideEnabled();
	}

	/**
	 * the object to use for setting and getting ownership; set null not to take ownership.
	 * value == this by default, but could be overridden if using an action in multiple ways.
	 */
	private Object proverOwnershipObject = this;

	protected boolean gotOwnership = false;

	/**
	 * Sets ownership to the specified object.
	 * @param proverOwnershipObject The new ownership object
	 * @see #proverOwnershipObject
	 */
	public void setProverOwnershipObject(Object proverOwnershipObject) {
		this.proverOwnershipObject = proverOwnershipObject;
	}

	/**
	 * Gets the current ownership object.
	 * @see #proverOwnershipObject
	 */
	public Object getProverOwnershipObject() {
		return proverOwnershipObject;
	}

	public void onProblemsStartingRun(String message) {
		System.err.println(message);
	}

	/** default prover action run body; sets status and optionally takes ownership
	 *  (shouldTakeProverOwnership, true by default), then calls runSingly();
	 * and finally updates status to DONE_ALL (unless a higher done trigger is set)
	 * @see org.eclipse.jface.action.IAction#run()
	 * @see ed.inf.proofgeneral.editor.actions.defunct.EclipseAction#run()
	 */
	@Override
    public void run() {
		// FIXME da: is this next block correct?  Maybe it checks in the wrong context?
		// Anyway, checks are buggy at the moment.
		//  	if (!decideEnabled()) {
		//  		onProblemsStartingRun("cannot run "+this.getClass().getName()+"; shouldn't have been enabled");
		//			setEnabled();
		//			return;
		//  	}
		if (!trySetStatus(STATUS_RUNNING)) {
			onProblemsStartingRun("cannot run "+this.getClass().getName()+"; it is already busy with status "+getStatus());
			setEnabled();
			return;
		}
		try {
//			if (getProverOwnershipObject()!=null) {
//				if (!getSessionManager().proverOwner.hasOwnership(getProverOwnershipObject())) {
//					if (!getSessionManager().proverOwner.tryGetProverOwnership(getProverOwnershipObject())) {
//						onProblemsStartingRun("cannot run "+this.getClass().getName()+"; someone else owns the prover");
//						updateStatus(STATUS_FAILED);
//						return;
//					}
//					gotOwnership = true;
//					//System.out.println(this+" got ownership");
//				}
//			}
			if (associatedDocument==null) {
				try {
					if (requiredScriptStatusLevel==ANY_SCRIPT_ACTIVE) {
						// da: I'd rather session manager did not expose the active script,
						// so would like to rework this next somehow (all other calls can be avoided)
						// Moreover, this seems to have the wrong behaviour: I'm looking at script A.thy but B.thy is active,
						// sometimes the buttons affect B.thy, not A.thy.  (Needs repeatable test case).
						associatedDocument = getSessionManager().getActiveScript();
						//should run in the active script, if there is one... else any is okay
						if (associatedDocument==null || associatedDocument.isFullyUnprocessed()) {
							associatedDocument = getAndAssociateDocument();
						}
					} else if (requiredScriptStatusLevel==CURRENT_SCRIPT_ACTIVE) {
						associatedDocument = getAndAssociateDocument();
					}
				} catch (KnownException e) {
					onProblemsStartingRun("Cannot find associated document for action "+this.getClass().getName());
				}
			}
			exception = null;  //clear any exception set
		  runSingly();
		} finally {
			if (STATUS_triggerDoneAll==STATUS_RUNNING) {
				updateStatus(STATUS_DONE_ALL);
			}
		}
  }

  /**
   * Main run body for things that should only be run singly (and which should take ownership);
   * Does nothing by default; intended always to be overridden (unless run is overridden).
   */
  public void runSingly() {}

  /**
   * Called automatically when the run is finished, either by something calling
   * updateStatus(-) explicitly, or by enough updateStatus(FLAG_i) being called
   * to trigger it being set done.
   *
   * Should not be called directly.
   *
   * Default implementation:<ul>
   * <li>clears associatedDocument,</li>
   * <li>resets scroll value (if it was set),</li>
   * <li>releases prover (if it was taken)</li>
   * </ul>
   * Subclasses can override but should at minimum do the same (or call super);
   * Also, classes should NOT call getDocument after this method in a single iteration.
   **/
  protected void onRunFinished(int finalStatus) {
  	associatedDocument = null;
  	if (gotOwnership) {
  		//System.out.println(this+" releasing ownership");
  		try {
//  			try {
////				getSessionManager().proverOwner.releaseOwnership(getProverOwnershipObject());
//			} catch (ProverOwnedBySomeoneElseException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
  		} catch (NullPointerException e) {
  			if (ProofGeneralPlugin.debug(this)) {
  				System.err.println("couldn't release ownership");
  				// e.printStackTrace();
  			}
  			// if we are shutting down, there is no session manager
  		} finally {
  			// if we were interrupted
  		  gotOwnership = false;
  		}
  	}
  }

	//---- status fields

  	protected int status = STATUS_IDLE;

	/** 0 means idle, negative is finished (and so available), positive is busy */
	public int getStatus() { return status; }

	/** {@value 0} */
	public static final int STATUS_IDLE = 0;
	/** {@value 1} */
	public static final int STATUS_RUNNING = 1;
	/** {@value -1} */
	public static final int STATUS_DONE_ALL = -1;
	/** {@value -2} */
	public static final int STATUS_FAILED = -2;
	/** {@value 2} */
	public static final int STATUS_DONE_PROVER = 2;
	/** {@value 4} */
	public static final int STATUS_DONE_DISPLAY = 4;

	private final Object statusLock = new Object();

	/** returns true or false depending whether the caller was allowed to change the status;
	 * typically used by passing a positive value (eg STATUS_RUNNING) to try to initiate a session */
	public boolean trySetStatus(int newStatus) {
		if (status>0) {
			return false;
		}
		synchronized (statusLock) {
			if (status>0) {
				return false;
			}
			status=0;
			updateStatus(newStatus);
			return true;
		}
	}

	/** forces the status to be this value, with no checking, then calls setEnabled();
	 * 0 means idle, negative is finished (and so available), positive is busy;
	 * this doesn't do ownership checks, use trySetStatus(+N) for new processes to check ownership;
	 * callers should use updateStatus which may be overridden */
	private void forceStatus(int newStatus) {
		// 		if (status==0 || newStatus!=0) {
		// 			Exception e = new Exception("call to setStatus("+newStatus+") when status is 0; continuing, but should use trySetStatus()!");
		// 			e.printStackTrace();
		// 		}
		status = newStatus;
		setEnabled();
	}

	private int STATUS_triggerDoneAll = STATUS_RUNNING; // | STATUS_DONE_PROVER | STATUS_DONE_DISPLAY;
	/** sets the value which indicates that all items are done running;
	 * used because often actions have two components, eg prover part and display part.
	 * <p/>
	 * the default is STATUS_RUNNING, which means status is cleared as soon as it is set (ie status flag not used);
	 * more commonly useful value is STATUS_RUNNING | STATUS_DONE_PROVER | STATUS_DONE_DISPLAY.
	 * <p/>
	 * for more than two components, use flags eg STATUS_DONE_NEW_PART{1,2,3} = {8,16,32}.
	 */
	public void setStatusDoneTrigger(int t) {
		STATUS_triggerDoneAll = t;
	}

	/**
	 * Updates the status with the appropriate value given this status indicator
	 * (using forceStatus); normal semantics is:
	 * <ul>
	 * <li>non-neg are bit-flags</li>
	 * <li>negative is finished indicators.</li>
	 * <li>negative parameter and 0 take effect immediately</li>
	 * <li>positive parameter is ignored if status is negative</li>
	 * <li>otherwise XORed with status, and then if == STATUS_DONE_TRIGGER set DONE_ALL, else set XORed value.</li>
	 * </ul>
	 * Doesn't do ownership checks.
	 * Callers should use trySetStatus(STATUS_RUNNING) for new processes to check ownership.
	 */
	public void updateStatus(int newStatus) {
		synchronized (statusLock) {
			if (newStatus<=0) {
				try {
				  if (status>0) {
					  onRunFinished(newStatus);
				  }
				} finally {
				  forceStatus(newStatus);
				}
			} // else if (status<0) {
				//nothing
			// }
			else {
				//newStatus positive, status non-negative
				int newS2 = status | newStatus;
				if (((newS2 & STATUS_triggerDoneAll)==STATUS_triggerDoneAll) && STATUS_triggerDoneAll>1) {
					updateStatus(STATUS_DONE_ALL);
				} else {
					forceStatus(newS2);
				}
			}
		}
	}


	//------------- blocking

	/**
	 * If true, wait for the action to finish before returning control.
	 * Only implemented by some sub-classes.
	 */
	protected boolean blocking = false;
	/**
	 * If true, wait for the action to finish before returning control.
	 * Only implemented by some sub-classes.
	 *     * @param b
	 */
	// FIXME da: this doesn't seem to be used now and blocking is never
	// set to true.  We may as well remove the blocking
	// code that examines this field.
	/*public void setBlocking(boolean b) {
		blocking = b;
	}*/




}
