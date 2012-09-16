/*
 *  $RCSfile: ThreadPoolCaller.java,v $
 *
 *  Created on 02 Nov 2004
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.process;

import java.io.PrintStream;
import java.io.PrintWriter;

import ed.inf.utils.datastruct.NumericStringUtils;

/**
 * starts to implement a session which can be run in a thread, getting the thread from the ThreadPool
 * (as opposed to ThreadCaller which uses new threads).
 * <p>
 * Example: <code>new ThreadPoolCaller(Callable code).call(Object[] params, int timeout)</code>
 * <p>
 * timeout is WAIT_FOREVER by default, callers may prefer to pass NO_WAIT,
 * then parse <code>isFinished()</code> and <code>getResult()</code>
 * <p>
 * All objects waiting on this will receive notification when the run is about to finish.
 */
public class ThreadPoolCaller implements Runnable {

	public ThreadPoolCaller(Callable target, String name) {
		this.target = target;
		this.name = name;
	}

	public ThreadPoolCaller(Callable target) {
		this.target = target;
	}

	public static final int NO_WAIT = -1;
	public static final int WAIT_FOREVER = 0;

	public Callable target = null;
	String name = null;

	/** runs the object in a separate thread with the specified parameters, waiting forever
	 *  (not so useful on its own...) */
	public Object call(Object params[]) {
		return call(params, WAIT_FOREVER);
	}
	/** runs the object in a separate thread with no parameters (null), waiting for the specified time
	 *  and returning null if not finished within that time;
	 *  NB do not pass 0, use one of the fields <code>NO_WAIT</code> or <code>WAIT_FOREVER</code>  */
	public Object call(int timeout) {
		return call(null, timeout);
	}
	/** runs the object in a separate thread with no parameters, waiting forever
	 *  (not so useful on its own...) */
	public Object call() {
		return call(null, WAIT_FOREVER);
	}

	ThreadPool.PooledThread pt = null;

	/** <b>This is the main method to use.</b>
	 *  <p>
	 *  runs the object in a separate thread with the specified parameters,
	 *  waiting for the specified time
	 *  and returning null if not finished within that time;
	 *  NB do not pass 0, use one of the fields <code>NO_WAIT</code> or <code>WAIT_FOREVER</code>  */
	public Object call(Object params[], int timeout) {
		//pt = (name!=null ? new Thread(this, name) : new Thread(this));
		setParams(params);
		pt = ThreadPool.get().popAvailableThread();

		synchronized (this) {  //not sure it needs to be synchronized, but it doesn't hurt
			runFinished=false;
		}
		pt.startNamed(this, name);
		try {
			if (timeout!=NO_WAIT) {
				join(timeout);
			}
		} catch (InterruptedException e) {
			throw new Error("ThreadCaller.call was interrupted");
		}
		return getResult(timeout!=WAIT_FOREVER);
	}

	Object[] params = null;
	public void setParams(Object[] params) {
		this.params = params;
	} // EI

	/** internal method wrapping the called parameter */
	public void run() {
		synchronized (this) {  //not sure it needs to be synchronized, but it doesn't hurt
			runFinished=false;
			runStarted=true;
		}
		//System.out.println("ThreadCaller "+this+": starting run");
		try {
			this.result = target.run(params);
		} catch (Throwable exc) {
			//System.out.println("ThreadCaller "+this+": setting exception "+exc);
			this.exc = exc;
			if (logExceptions) {
				System.err.println(NumericStringUtils.makeDateString()+"  uncaught exception while running ThreadPool task "+this);
				exc.printStackTrace();
			}
		}
		//System.out.println("ThreadCaller "+this+": ending run");
		synchronized (this) {
			runFinished=true;
			this.notifyAll();
		}
	}

	/** tries to interrupt the thread */
	public void interrupt() {
		pt.interrupt();
	}

	/** tries to join the thread */
	public void join() throws InterruptedException {
		join(0);
	}
	/** joins the thread if it exists and is not finished */
	public void join(int ms) throws InterruptedException {
		if (pt!=null && !runFinished) {
			pt.softJoin(ms);
		}
	}

	boolean runFinished=true;
	/** returns true if the run has finished (also true trivially before it starts) */
	public boolean isFinished() {
		return runFinished;
	}

	boolean runStarted=false;
	/** returns true if the run has started */
	public boolean isStarted() {
			return runStarted;
	}

	Object result = null;
	/** returns the result or throw the error (wrapped in a RuntimeExceptionWrapper), throwing an error if it has not finished */
	public Object getResult() {
		return getResult(false);
	}

	/** returns the result, with the param corresponding to the wait time (ie if WAIT_FOREVER, throws an error if not finished, otherwise not)
	 * @deprecated replaced by the boolean arguments
	 */
	public Object getResult(int allowUnfinished) {
		return getResult(allowUnfinished!=WAIT_FOREVER);
	}
	/** returns the result if there is one (or an exception wrapped in a RuntimeExceptionWrapper if it threw an exception);
	 *  throws an error it is not finished and allowUnfinished is false */
	public Object getResult(boolean allowUnfinished) {
		if (!allowUnfinished && !runFinished) {
			throw new Error("ThreadCaller result being accessed before finished.");
		}
		if (getException()!=null) {
			throw new RuntimeExceptionWrapper(getException());
		}
		return result;
	}

	public void clear() {
		clear(0);
	}

	public void clear(int allowUnfinished) {
		if (allowUnfinished==0 && !runFinished) {
			throw new Error("ThreadCaller being cleared before finished.");
		}
		runStarted=false;
		result = null;
		exc = null;
	}

	Throwable exc = null;
	public Throwable getException() {
		return exc;
	}

	boolean logExceptions = false;
	public void setExceptionLogging(boolean b) {
		logExceptions = b;
	}

	public String toString() {
		return "Caller["+(name!=null ? name+"," : "")+Thread.currentThread()+"]";
	}

	public static class RuntimeExceptionWrapper extends RuntimeException {
		private static final long serialVersionUID = -6531332808436072542L;
		Throwable parent = null;
		public RuntimeExceptionWrapper(Throwable parent) {
			if (parent==null) {
				throw new RuntimeException("Cannot create RuntimeExceptionWrapper without an exception");
			}
			this.parent = parent;
		}

		public String getMessage() {
			if (parent==null) {
				return "ThreadCaller not initialised yet";
			}
			return parent.getMessage();
		}

		public int hashCode() {
			if (parent==null) {
				return 0;
			}
			return parent.hashCode();
		}

		public String getLocalizedMessage() {
			if (parent==null) {
				return "ThreadCaller not initialised yet";
			}
			return parent.getLocalizedMessage();
		}

		public Throwable getParent() { return parent; }

		public boolean equals(Object obj) {
			if (parent==null) {
				if ((obj!=null) && (obj instanceof RuntimeExceptionWrapper) &&
						(((RuntimeExceptionWrapper)obj).getParent()==null)) {
					return true;
				}
				return false;
			}
			return parent.equals(obj);
		}

		public String toString() {
			if (parent==null) {
				return "ThreadCaller not initialised yet";
			}
			return parent.toString();
		}

		public void printStackTrace() {
			if (parent==null) {
				System.err.println("ThreadCaller not initialised yet");
			} else {
				parent.printStackTrace();
			}
		}

		public void printStackTrace(PrintStream s) {
			if (parent==null) {
				s.println("ThreadCaller not initialised yet");
			} else {
				parent.printStackTrace(s);
			}
		}

		public void printStackTrace(PrintWriter s) {
			if (parent==null) {
				s.println("ThreadCaller not initialised yet");
			} else {
				parent.printStackTrace(s);
			}
		}

		public synchronized Throwable fillInStackTrace() {
			if (parent==null) {
				return super.fillInStackTrace();
			}
			return parent.fillInStackTrace();
		}
	}

}
