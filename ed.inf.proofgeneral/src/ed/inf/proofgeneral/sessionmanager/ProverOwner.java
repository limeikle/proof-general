/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jan 20, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *
 */

package ed.inf.proofgeneral.sessionmanager;


// TODO da: ONGOING: I'm trying to make use of this defunct and simplify.  
// It's too fragile/complicated to get right.  I can see that it is useful
// to have a temporary "ownership" of the session, as some action may like
// to engage in a dialog with the prover and act according to its responses
// (e.g. ProverKnowledge, Feasch).  But the code could probably be simpler
// and abstraction shhould be wholly implemented in SessionManager with the 
// prover available mechanism, rather than calls scattered to clear ownership, 
// check it's cleared, etc.

/**
 *
 */
public class ProverOwner {

	private final Object proverOwnerLock = new Object();
    // da: added volatile for happens-before consistency below, maybe this
	// will help solve some of many cases of prover owner being stuck?
	// TODO: probably needs further review of this class for thread-safety.
	private volatile Object proverOwner;

	private final SessionManager sm;

	public ProverOwner(SessionManager manager) {
		this.sm = manager;
	}

	public void clear() {
		proverOwner = null;
	}

	/** allow an object to take "ownership" of the prover;
	 *  this is not a "hard" control, in the sense others can still send and queue commands,
	 *  but it is synced, so there can be only one owner at a time
	 * <p/>
	 *  most prover uses should try to take this ownership first
	 *  (and not run if they cannot)
	 * <p/>
	 *  they MUST release this ownership when done, even if there is an error,
	 *  otherwise no one can take it (unless it is interrupted or closed).
	 * @return whether this object got ownership
	 */
	public boolean tryGetProverOwnership(Object newOwner) {
		if (isOwned()) {
			return false;
		}
		synchronized (proverOwnerLock) {
			if (isOwned()) {
				return false;
			}
			proverOwner = newOwner;
		}
		return true;
	}

	/** whether the query parameter is the current owner of the prover */
	public boolean hasOwnership(Object queryOwner) {
		return (proverOwner!=null && proverOwner==queryOwner);
	}

	/**
	 * Lets an object release ownership of a prover;
	 * @param owner the object owning the prover, for error checking;
	 * if null, then anyone can clear it
	 * @return returns true if we released it, false if no one owned it
	 * @throws ProverOwnedBySomeoneElseException if someone else owns it
	 */
	public boolean releaseOwnership(Object owner) throws ProverOwnedBySomeoneElseException {
		if (proverOwner==null) {
			return false;
		}
		if (proverOwner==owner || owner==null) { // DC [da: added volatile so won't be optimised]
			synchronized (proverOwnerLock) {
				//repeat checks in sync block; did checks first for speed
				// [da: remark on above: this is NOT sufficient for correctness wrt re-ordering]
				if (proverOwner==null) {
					return false;
				}
				if (proverOwner==owner || owner==null) {
					proverOwner=null;
					// da: event cleanups: remove ProverClear, nobody uses it CLEANUP
					//synchronized (sm.getQueueSyncObject()) {
						//if (sm.isEmptyQueueAndEvents() && sm.proverState.isAlive()) {  //we know it isn't owned ... 
						//	sm.firePGIPEvent(new ProverClear("just released ownership"));
						//}
					//}
					//sm.commandQueueNotify();
					return true;
				}
			}
		}
		throw new ProverOwnedBySomeoneElseException(proverOwner, owner);
  }

	public static class ProverOwnedBySomeoneElseException extends Exception {
		public Object owner;
		public Object attemptedOwnerToClear;
		public ProverOwnedBySomeoneElseException(Object owner,
				Object attemptedOwnerToClear) {
			super("The prover is owned by "+owner+" but the parameter "+attemptedOwnerToClear+" was passed to release ownership");
			this.owner = owner;
			this.attemptedOwnerToClear = attemptedOwnerToClear;
		}
	}

	/**
	 * @return whether someone has claimed ownership of the prover
	 */
	public boolean isOwned() {
		return (proverOwner!=null);
	}

}
