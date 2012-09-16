/*
 *  $RCSfile: PooledRunnable.java,v $
 *
 *  Created on 07 Mar 2004 by heneveld
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.process;

/**
 * PooledRunnable pretends to be a thread but calling its start() method gets a
 * thread from the thread pool
 * @author heneveld
 */
public abstract class PooledRunnable implements Runnable {

	/** The thread's name */
	private String name;
	/** This runnable's thread. */
	private ThreadPool.PooledThread currentThread;

	/**
	 * Creates an un-named PooledRunnable.
	 */
	public PooledRunnable() {
		// empty.
	}

	/**
	 * Creates a named PooledRunnable
	 * @param name the thread's name.
	 */
	public PooledRunnable(String name) {
		this.name = name;
	}

	/**
	 * Returns the set name, or a sensible auto-generated identifier if not.
	 * TODO make the anonymous name less anonymous.
	 */
	public String getName() {
		if (name != null) {
			return name;
		}
		if (currentThread != null && !currentThread.getName().startsWith("Thread")) {
			return currentThread.getName();
		}
		return "Unnamed-Pooled-Runnable";
	}

	/**
	 * Sets the runnable's name.
	 * @param name the new name.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Sets the thread associated with this object to null.
	 */
	public synchronized void clearCurrentThread() {
		currentThread = null;
	}

	/**
	 * Indicates that the thread pool should run this object.
	 */
	public void start() {
		currentThread = ThreadPool.get().runNamed(this, getName());
	}

	/**
	 * interrupts this task; the thread returns to the pool
	 */
	public synchronized void interrupt() {
		if (currentThread != null && currentThread.isCurrentTask(this)) {
			currentThread.allowThisTaskToBeInterrupted();
			currentThread.interrupt();
		}
	}
}
