/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jan 20, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *
 */

package ed.inf.proofgeneral.sessionmanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import ed.inf.proofgeneral.Constants;
import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.preferences.PreferenceNames;
import ed.inf.proofgeneral.sessionmanager.events.IPGIPListener;
import ed.inf.proofgeneral.sessionmanager.events.PGIPEvent;
import ed.inf.utils.datastruct.MutableInteger;
import ed.inf.utils.datastruct.NumericStringUtils;
import ed.inf.utils.process.RunnableWithParams;

/**
 *
 */
public class PGIPEventQueue {

	private final SessionManager sm;  // da: can avoid?

	public PGIPEventQueue (SessionManager sm) {
		this.sm = sm;
	}

	//i've changed firePGIPEvent to launch all events in separate threads -AH
	//synchronized on a lock object, and staggered so that they only run once the previous messages have all been handled
	// da: not sure that's a good idea, doesn't it lead to a lot of threads?  Potential
	// slowdown if we're waiting for them all to finish before processing the next
	// lot of events?  Would rather explicitly assign lengthy subtasks within events
	// to separate threads (using Eclipse job API/Display queueing) where necessary

	protected MutableInteger firingSequence = new MutableInteger(0);
	protected ArrayList<PGIPEventQueue.FirePGIPEventThread> queuedEvents = new ArrayList<PGIPEventQueue.FirePGIPEventThread>();
	protected int firingQueueSequence = 0;


	/**
	 * This should normally be null. Setting it to non-null means that PGIP events are
	 * *only* sent to this listener.
	 */
	//this has now become a param on queue command, etc; because otherwise if the queue is long could become inconsistent -AH
	//TODO ideally we can do without the privateListener field, just pass it from the CommandQueued if necessary
	IPGIPListener privateListener = null;

	Collection<IPGIPListener> listeners = new ArrayList<IPGIPListener>(); // Objects who listen to our events.

	public void clear() {
		queuedEvents.clear();
		firingQueueSequence = 0;
		firingSequence.set(0);
	}

	/**
	 * returns a pair [# of messages handled, # of messages generated];
	 * second arg should 'catch-up' to first arg by bg threads, in causal order
	 * (if backlog of messages have been generated)
	 */
	public int[] getFiringQueueInfo() {
		return new int[] { firingSequence.get(), firingQueueSequence };
	}

	/**
	 * waits up to ms millis for all fired events to be dispensed (indefinitely if negative,
	 * and doesn't wait, just checks, if 0);
	 * false on timeout (assuming ms greater than 0), true otherwise
	 */
	public boolean waitForProver(long ms) throws InterruptedException {
		long endTime = System.currentTimeMillis() + ms;
		synchronized (firingSequence) {
			while (firingSequence.get()<firingQueueSequence) {
				if (ms==0) {
					return false;
				}
				if (ms<0) {
					firingSequence.wait();
				} else {
					ms = endTime - System.currentTimeMillis();
					if (ms<=0) {
						return false;
					}
					firingSequence.wait(ms);
				}
			}
		}
		return true;
	}

//	/** waits for all fired events to be dispensed, queue to empty, and notification;
//	 *  on success, gives a lock to this thread,
//	 *  which is cleared when the thread sends an event (or which the caller can clear)
//	 * @param ms time to wait in ms, 0 doesn't wait, -1 waits indefinitely
//	 * @throws InterruptedException if interrupt() is called on the SessionManager
//	 * @return true if we got the prover, false if the time limit expired
//	 */
//  public boolean waitForProver(long ms) throws InterruptedException {
//		synchronized (firingSequence) {
//			if (isClear() && hasProverThread==null) {
//				hasProverThread = Thread.currentThread();
//				return true;
//			}
//			if (ms==0) return false;
//			waitingThreads.add(Thread.currentThread());
//			try {
//			  while (!isClear()) firingSequence.wait((ms>0 ? ms : 0));
//			} finally {
//				waitingThreads.remove(Thread.currentThread());
//			}
//			if (isClear()) return true;
//		}
//		return false;
//	}
//  public void releaseProverLock() {
//  	hasProverThread = null;
//  	synchronized (firingSequence) {
//  		firingSequence.notify();
//  	}
//  }
//  //MutableObject proverLock = new MutableObject(null);
//  Thread hasProverThread = null;
//	ArrayList waitingThreads = new ArrayList();


	//public static final ThreadPool isabelleThreadPool = new ThreadPool("isabelle events pool");

	protected ArrayList<PGIPEventQueue.FirePGIPEventThread> activeFiredEvents = new ArrayList<PGIPEventQueue.FirePGIPEventThread>();

	/**
	 * Sends a PGIP event (containing the incoming pgip message)
	 * to all registered listeners.
	 */
	public void firePGIPEvent(PGIPEvent event) {
		assert event!=null : "firePGIPEvent was given a null event";
		// if (logging) {
		//  -- log to system file
	    //}

		// FIXME da: I think there's conc'y bug here: if the debug message is
		// enabled things seem OK, otherwise I sometimes see clearmessages being processed
		// by the display *after* the message which sets the display contents.
		if (ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_LOG_EVENTS_FIRING)) {
			System.out.println(NumericStringUtils.makeDateString()+"  firing event "+firingQueueSequence+": "+event);
		}
		synchronized (queuedEvents) {      // only one event fires at a time
			if (privateListener != null) { // only send this to one listener
				queuedEvents.add(new PGIPEventQueue.FirePGIPEventThread(firingSequence, firingQueueSequence++, privateListener, event, sm));
			} else if (listeners != null && listeners.size() > 0) {
				//copy the listeners in case an event handler modifies it (guessing)  -AH
				ArrayList<IPGIPListener> listenersCopy = new ArrayList<IPGIPListener>(listeners);
				int fQSconst = firingQueueSequence;
				for (IPGIPListener listener : listenersCopy) {
					queuedEvents.add(new PGIPEventQueue.FirePGIPEventThread(firingSequence, fQSconst, listener, event, sm));
					firingQueueSequence++;
				}
			}
			//also send to updater
			queuedEvents.add(new PGIPEventQueue.FirePGIPEventThread(firingSequence, firingQueueSequence++, sm.qUpdater, event, sm));
			fireNextFromQueue();
		}
	}

	protected void fireNextFromQueue() {
		synchronized (queuedEvents) {
		  //if (queuedEvents.size()==0) return;
		  //FirePGIPEventThread t = (FirePGIPEventThread) queuedEvents.get(0);
		  Iterator fi = queuedEvents.iterator();
		  while (fi.hasNext()) {
		  	PGIPEventQueue.FirePGIPEventThread t = (PGIPEventQueue.FirePGIPEventThread) fi.next();
		  	if (firingSequence.get()>=t.mySequenceNumber) {
		  		fi.remove();
		  		t.start();
		  	} else {
		  		break;
		  	}
		  }
		}
	}


	/**
	 * a class to run event notifications in separate threads, synchronized on a lock object
	 */
	// da: WHY do we do this?  Much better to ask that listeners return quickly, mostly,
	// and if they're going to take a long time they create their own threads.  Even
	// with thread pools this looks overkill and likely to introduce more concurrency
	// bugs.   One thread for firing the events wouldn't be so bad.
	// (Does the locking even prevent these running together?)
	class FirePGIPEventThread extends RunnableWithParams {
		MutableInteger firingSequence;
		int mySequenceNumber;
		IPGIPListener listener;
		PGIPEvent event;
		SessionManager sm;
		public FirePGIPEventThread(MutableInteger firingSequence, int mySequenceNumber, IPGIPListener listener,
				PGIPEvent event, SessionManager sm) {
			super(null, "FireEvent");
			this.firingSequence = firingSequence;
			this.mySequenceNumber = mySequenceNumber;
			this.listener = listener;
			this.event = event;
			this.sm = sm;
		}
		//		public ThreadPool getThreadPool() {
		//			return isabelleThreadPool;
		//		}
		Thread myThread = null;
		boolean localInterrupt = false;

		/**
		 * Interrupts the thread where this task is running (if it is still running
		 * or still waiting for its sequence number)
		 * @return true if we were able to interrupt something without error
		 */
		public boolean interruptTask() {
			try {
				if (myThread!=null) {
					synchronized (myThread) {
						localInterrupt = true;
						myThread.interrupt();
					}
				}
				return true;
			} catch (Exception e) {
				localInterrupt = false;  //if we got an error
			}
			return false;
		}

		public void run() {
			try {
				if (firingSequence==null) {
					System.err.println("event "+this+" can't run with null sequence");
					return;
				}
				myThread = Thread.currentThread();
				synchronized (firingSequence) {
					// TODO this approach gets very messy if there's a long backlog
					while (firingSequence.get()<mySequenceNumber) {
						firingSequence.wait();
					}
					activeFiredEvents.add(this);
				}
				long ts = System.nanoTime();
				if (ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_LOG_EVENT_RUNNING)) {
					System.out.println(NumericStringUtils.makeDateString()+"  event ["+mySequenceNumber+"] sending to "+listener);
				}
				listener.pgipEvent(event);
				ts = System.nanoTime() - ts;
				if (ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_LOG_EVENT_RUNNING)) {
					System.out.println(NumericStringUtils.makeDateString()+"  event ["+mySequenceNumber+"] done in "+
						NumericStringUtils.makeTimeStringNano(ts)+" from "+listener);
				}
			} catch (InterruptedException e) {
				if (Constants.LOG_INTERRUPT_ACTIONS || 
						ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_LOG_EVENT_RUNNING)) {
					System.out.println("event was interrupted: ["+mySequenceNumber+"] "+event);
				}
				if (localInterrupt) {
					//clear the interrupt flag if this task was interrupted
					//(but not if the pool was interrupted)
					Thread.interrupted();
					localInterrupt = false;
				}
			} catch (Throwable e) {
				//we ignore these
				System.err.println("event firing error: "+event);
				e.printStackTrace();
			} finally {
				myThread = null;
				synchronized (firingSequence) {
					activeFiredEvents.remove(this);
					firingSequence.inc();
					firingSequence.notifyAll();
					fireNextFromQueue();
// da: CLEANUP event refactor: removed ProverClear, nobody uses it.					
//					if ((!(event instanceof ProverClear)) && sm.isEmptyQueueAndEvents() &&
//							!sm.startUpBumpfFlag &&
//							// FIXME da: race condition: seems possible to get here without tpOutputGobbler set.
//							(sm.tpOutputGobbler != null) &&
//							!sm.tpOutputGobbler.bytesAvailable()) {
//						firePGIPEvent(new ProverClear("just discharged last PGIPEvent"));
//					}
				}
			}
		}
	} // end class

}
