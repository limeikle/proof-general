/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on 26 May 2008 by alex
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */

package ed.inf.utils.eclipse;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * ProgressMonitor implementation which can block until completion,
 * useful for processes (such as tests) which need to wait for 
 * "long-running jobs" (most such jobs aren't long-running, though
 * Eclipse refers to them as such because they may run concurrently
 * and not be finished at the point where in-thread execution is
 * returned to the caller)
 * <p>
 * typical use is to create a new one, pass it to the process,
 * then waitUntilCompletion 
 */
public class SimpleProgressMonitor implements IProgressMonitor {

	private static Logger LOG = Logger.getLogger(SimpleProgressMonitor.class.getName());

	AtomicBoolean busy = new AtomicBoolean(false);

	public void beginTask(String name, int totalWork) {
		if (busy.getAndSet(true))
			throw new IllegalStateException("ProgressMonitor already in use: must be reset before reuse.");
		
		this.currentTaskName = name;
		this.totalWork = totalWork;
		synchronized (busy) {
			busy.notifyAll();
		}
	}

	String currentTaskName = null;
	
	public void setTaskName(String name) {
		this.currentTaskName = name;
	}
		
	boolean done = false;
	boolean cancelled = false;	
	
	public void done() {
		done = true;
		synchronized (busy) {
			busy.notifyAll();
		}
	}

	public void setCanceled(boolean value) {
		cancelled = value;
		synchronized (busy) {
			busy.notifyAll();
		}		
	}

	int totalWork = 0;	
	int work;
	double internalWorked = 0;
	
	public void worked(int work) {
		this.work = work;
	}	
	public void internalWorked(double work) {
		internalWorked = work;
	}

	String currentSubTaskName = null;
	public void subTask(String name) {
		currentSubTaskName = name;
	}

	/** true iff a task has started using this but not called cancelled or done */
	public boolean isActive() {
		return isUsed() && !isCanceled() && !isDone();
	}
	public boolean isDone() {
		return done;
	}	
	public boolean isCanceled() {
		return cancelled;
	}
	/** whether a task has starting using this progress monitor since it was last reset (or created);
	 * returns true even after cancelled or done, see also isActive */
	public boolean isUsed() {
		return busy.get();
	}

	public int getWork() {
		return work;
	}
	public int getTotalWork() {
		return totalWork;
	}
	public double getInternalWorked() {
		return internalWorked;
	}
		
	public String getTaskName() {
		return currentTaskName;
	}
	
	public String getSubTaskName() {
		return currentSubTaskName;
	}


	/** makes this progress monitor available for reuse;
	 * any previous tasks allocated this monitor should have finished before calling 'reset'
	 * (or behaviour is undefined0 */
	public SimpleProgressMonitor reset() {
		this.currentTaskName = null;
		this.currentSubTaskName = null;
		this.totalWork = 0;
		this.work = 0;
		this.internalWorked = 0;		
		cancelled = false;
		done = false;
		synchronized (busy) {
			busy.set(false);
			busy.notifyAll();
		}
		return this;
	}
		
	/** configurable blocking for tasks to start, finish, and reset afterwards;
	 * all times are in millis, with 0 meaning no time, -1 meaning forever */
	public boolean block(final long timeToWaitForStart, final long timeToWaitForDone, final boolean resetAfterBlock) throws InterruptedException {
		synchronized (busy) {
			long startTime = System.currentTimeMillis();
			long timeLeft = 0;
			while (timeToWaitForStart<0 || (timeLeft = startTime+timeToWaitForStart - System.currentTimeMillis())>0) {
				if (busy.get()) break;
				if (timeToWaitForStart<0) busy.wait();
				else if (timeLeft>0) busy.wait(timeLeft);
			}
			if (isActive()) {
				startTime = System.currentTimeMillis();
				while (timeToWaitForDone<0 || (timeLeft = startTime+timeToWaitForDone - System.currentTimeMillis())>0) {
					if (isDone() || !busy.get()) break;
					if (timeToWaitForDone<0) busy.wait();
					else if (timeLeft>0) busy.wait(timeLeft);
				}
			}
			boolean done = isDone();
			if (resetAfterBlock) reset();
			return done;
		}
	}	

	/** as 'block' but will catch and log at sensible default levels */
	@SuppressWarnings("boxing")
	public boolean blockLogging(final long timeToWaitForStart, final long timeToWaitForDone, final boolean resetAfterBlock) {
		try {
			long startTime = System.currentTimeMillis();
			boolean result = block(3*1000, 20*1000, false);
			long elapsedTime = System.currentTimeMillis() - startTime;
			
			String name = getTaskName();
			
			LOG.log(Level.FINEST, "task '{0}', result {1}, elapsed time {2}, " +
					"busy {3}, done {4}, cancelled {5}", new Object[] { 
					name, result, elapsedTime,
					busy.get(), isDone(), isCanceled() });

			if (name==null && !busy.get()) {
				LOG.log(Level.INFO, "expected task but none was begun", new Throwable("trace for expected task but none was begun"));
				return result;
			}

			if (name==null) name = "(unnamed)";

			if (isDone()) {
				LOG.log(Level.FINER, "task '{0}' completed in {1} ms", new Object[] { name, elapsedTime });
				return result;
			}
			
			if (isCanceled()) {
				LOG.log(Level.INFO, "task '{0}' cancelled after {1} ms", new Object[] { name, elapsedTime });
				return result;
			}

			LOG.log(Level.WARNING, "task '"+name+"' timed out", new Throwable("trace for timed out task"));					
			return result;
			
		} catch (InterruptedException e) {
			String name = getTaskName();			
			if (name==null) name = "unnamed task";
			LOG.log(Level.FINE, "task '{0}' interrupted", name);
			return false;
		} finally {
			if (resetAfterBlock) reset();
		}
	}

	/** convenience to block forever then resets when task finishes */
	public void blockUntilDoneThenReset() throws InterruptedException {
		block(-1, -1, true);
	}
	
	/** convenience to block for 3s for start then 20s for completion,
	 * and reset, and to log a warning if a task didn't start or finish */
    public void blockBrieflyAndLog() {		
    	blockLogging(3*1000, 20*1000, true);
	}

}
