/*
 *  $RCSfile: DisplayCallable.java,v $
 *
 *  Created on 09 Jun 2005 by Alex Heneveld
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.eclipse;

import org.eclipse.swt.widgets.Display;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.utils.datastruct.MutableObject;
import ed.inf.utils.process.Callable;
import ed.inf.utils.process.ThreadPoolCaller;

/**
 * DisplayCallable -- to replace all calls to RunnableWithParams.callDefaultDisplayThread;
 * create this, overriding run() or run (params[]) as desired, then invoke
 * start(), start(params), runDisplay(), runDisplay(params).
 * or runDisplayWaiting(), runDisplayWaiting(params).
 * use the 'getInfo()' method to get the ThreadCaller object used to run this;
 * with that you can inspect the result, error, etc.
 * @author Alex Heneveld
 */
public class DisplayCallable implements Callable {

	public DisplayCallable() {}

	public DisplayCallable(String name) {
		this.name = name;
	}

	String name;
	ThreadPoolCaller caller = null;
	public ThreadPoolCaller getInfo() {
		if (caller!=null) {             // da: must *not* sync on caller!
			return caller;
		}
		caller = new ThreadPoolCaller(this, name);
		caller.setExceptionLogging(true);
		return caller;
	}

	/** is the current thread the display ui thread? */
	public static boolean isDisplayThread() {
		return Display.getCurrent() != null;
	}

	/**
	 * Runs this in our thread if there is no display or we are the display thread;
	 * otherwise runs this in the display thread without waiting
	 * @return whether this task has been completed in the caller's thread (ie we are the display thread);
	 * (to get result of 'run', poll getInfo() for completion then getInfo().getResult()
	 */
	public boolean runDisplay(Object ... params) {
		getInfo().setParams(params);
		if (ProofGeneralPlugin.getDefault()==null || Display.getDefault()==null) {
			//System.err.println(General.makeDateString()+"  display task without PG, running in current thread, "+this);
			getInfo().run();
			return true;
		}
		else if (isDisplayThread()) {
			//System.err.println(General.makeDateString()+"  display subtask in current thread, "+this);
			getInfo().run();
			//System.err.println(General.makeDateString()+"  display subtask ended in current thread, "+this);
			return true;
		} else {
			//System.out.println("queueing for async "+this);
			//getInfo().setParams(params);  da: why was this repeated?
			//org.eclipse.swt.widgets.Display.getDefault().asyncExec(getInfo());
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
					//System.err.println(General.makeDateString()+"  display task starting, "+DisplayCallable.this);
					getInfo().run();
					//System.err.println(General.makeDateString()+"  display task ending, "+DisplayCallable.this);
				}
			});
			return false;
		}
	}

	/**
	 * runs this in our thread if there is no display or we are the display thread;
	 * otherwise runs this in the display thread without waiting.
	 * Equivalent to {@link #run(Object[])} with null argument.
	 * @return whether this task has been completed in the caller's thread (ie we are the display thread).
	 */
	public boolean runDisplay() {
		return runDisplay((Object[]) null);
	}

	@Override
    public String toString() {
		return (name==null ? "DisplayCallable" : name);
	}

	/** runs this in our thread if there is no display or we are the display thread;
	 *  otherwise runs this in the display thread, and waiting for that to complete before returning its value
	 * @return the result
	 */
	// da: new version copied as above but uses syncExec instead of asyncExec.
	// Not quite same as old version below; no possibility for interruption.
	// But we should use Eclipse API methods for user-interruption (i.e. progress
	// monitor with performOperation(monitor) call).
	// sigh... this is isn't even used in present code.  Should have checked that.
	// re-instated (with cleanups above) -- it is useful for testing. -AH
	public Object runDisplayWaiting(final Object params[]) throws InterruptedException {
		getInfo().setParams(params);
		if (ProofGeneralPlugin.getDefault()==null ||
		    org.eclipse.swt.widgets.Display.getDefault()==null) {
			getInfo().run();
			return getInfo().getResult();
		}
		else if (isDisplayThread()) {
  		//System.out.println("running in current/display thread "+this);
			getInfo().run();
			return getInfo().getResult();
		} else {
			//System.out.println("queueing for async "+this);
			final Object runningLock = new Object();
			final MutableObject done = new MutableObject(Boolean.FALSE);
			synchronized (runningLock) {
				org.eclipse.swt.widgets.Display.getDefault().syncExec(new Runnable() {
					public void run() {
						try {
							getInfo().run();
						} finally {
							synchronized (runningLock) {
								done.set(Boolean.TRUE);
								runningLock.notifyAll(); // da: to kill warnings, notify() seems safe here
							}
						}
					}
				});
				while (done.get().equals(Boolean.FALSE)) {
					runningLock.wait();
				}
			}
			return getInfo().getResult();
		}
	}

	/** runs this in our thread if there is no display or we are the display thread;
	 *  otherwise runs this in the display thread, blocking until it completes
	 * @return result of run
	 */
	public Object runDisplayWaiting() throws InterruptedException {
			return runDisplayWaiting(null);
	}



	//--- methods for user to to override
	/** user overridable method for what is to be run (callers should use runDisplay{,Waiting});
	 *  calls run() by default */
	public Object run(Object[] param) throws Exception {
		return run();
	}

	/** user overridable method for what is to be run (callers should use runDisplay{,Waiting});
	 *  does nothing and returns null by default */
	public Object run() throws Exception {
		return null;
	}


}
