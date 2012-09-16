/*
 *  $RCSfile: WaitReadyAction.java,v $
 *
 *  Created on 28 Apr 2005 by Alex Heneveld
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.actions;

import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.proofgeneral.sessionmanager.events.PGIPEvent;
import ed.inf.proofgeneral.sessionmanager.events.PGIPReady;
import ed.inf.utils.eclipse.EclipseMethods;
import ed.inf.utils.process.PooledRunnable;

/**
 * WaitReadyAction -- an action (not really intended to be clicked, mainly for internal use)
 * which waits a fixed time to receive a 'ready' message, and gives an error if not.
 * Used in SessionManager to help track down errors for when the prover doesn't start right.
 * Right now, only one caller can wait at a time; if a second says to "start",
 * the earlier one will display an error message.
 */
public class WaitReadyAction extends ed.inf.proofgeneral.editor.actions.torefactor.PGAction {

	/**
	 * Instantiates the WaitReadyAction.  This will only be done once.
	 * Does not allow external instantiation.
	 */
	private WaitReadyAction() { super(); }

	private static class WaitReadyActionSingleton { // da: holder class idiom, see Goetz
		public static WaitReadyAction singleton = new WaitReadyAction();
	}

	/** Return the one and only allowed instance of this class. */
	public static WaitReadyAction getDefault() {
		return WaitReadyActionSingleton.singleton;
	}

	int waiting = 0;
	boolean gotReady = false;
	PooledRunnable r;

	/**
	 * Performs the waiting.  Pops up an eclipse messageDialog when the timer expires.
	 * @param title the message dialog title
	 * @param message the content of the message dialog
	 * @param timeout the time to wait.
	 */
	public synchronized void run(String title, String message, long timeout) {
			waiting++;
			try {
				this.wait(timeout); // FIXME Wa: should loop in case of spurious wakeups
				if (!gotReady) {
					EclipseMethods.messageDialogAsync(title, message, new String[] { "OK" });
				}
			} catch (InterruptedException e) {}
			finally {
				waiting--;
				if (waiting==0) {
					gotReady = false;
				}
			}
	}

	//public Object startLock = new Object();

	/**
	 * Starts the timer for this action.
	 * TODO return a cancellable object.
	 */
	public void start(final String title, final String message, final long timeout, final SessionManager mgr) {
		synchronized (this) {
			r = new PooledRunnable() {
				public void run() {
					mgr.addListener(WaitReadyAction.this);   //needs to be registerd as a listener because the ActionBar listeners might not be handled until too late
					try {
						synchronized (WaitReadyAction.this) {
							WaitReadyAction.this.notifyAll();  //this will also notify anyone else waiting, without setting the gotReady flag, so they will display an error ... but as long as only one person is waiting that shouldn't be a problem
							WaitReadyAction.this.run(title, message, timeout);
						}}
					finally {
						mgr.removeListener(WaitReadyAction.this);
					}
				}
			};
			r.start();
			try {
				// FIXME: find bugs reports UW (unconditional wait) here.  What
				// are we waiting on?  Also: wait is not in loop, should patch
				// against spurious wakeups.
				this.wait();
			} catch (Exception e) {}
			//System.out.println("waiting on ready "+Thread.currentThread());
		}
	}

	/**
	 * Cancels the action.
	 */
	//TODO check thread model. previously this was synchronzied but meant would deadlock
	//if dialog was displayed while prefs are updated.
	public void stop() {
		gotReady = true;
		r.interrupt();
	}

	/**
	 * @see ed.inf.proofgeneral.sessionmanager.events.IPGIPListener#pgipEvent(ed.inf.proofgeneral.sessionmanager.events.PGIPEvent)
	 */
	@Override
    public synchronized void pgipEvent(PGIPEvent event) {
		if (waiting<=0) {
			return;
		}
		if (event instanceof PGIPReady) {
			gotReady = true;
			//System.out.println("got ready "+Thread.currentThread());
			this.notifyAll();
		}
	}

}
