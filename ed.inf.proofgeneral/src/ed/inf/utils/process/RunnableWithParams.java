/*
 *  $RCSfile: RunnableWithParams.java,v $
 *
 *  Created on 01 Nov 2004 by heneveld
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.process;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;

import ed.inf.proofgeneral.ProofGeneralPlugin;

/**
 * RunnableWithParams --
 * like Runnable but allows setting parameters (as an Object array) in the constructor
 * which can then be used in the run() method.
 * implementations of this class should define their own run() method,
 * but often this is done inline, with a call to callDefaultDisplayAsyncExec
 * if the method should be executed by the Eclipse UI thread.
 * <p/>
 * DocElement command = ((CommandProcessed) e).getCommand(); <br/>
 * new RunnableWithParams(new Object[] {command}) {<br/>
 *   public void run() {<br/>
 *     ((DocElement)p[0]).fDocument.commandProcessed((DocElement)p[0]);<br/>
 * }}.callDefaultDisplayAsyncExec();
 *
 * @author Alex Heneveld
 */
// TODO * @deprecated ---   use DisplayCallable(name) { ... }.runDisplay()   instead
public abstract class RunnableWithParams implements Runnable {

	protected Object[] p;
	protected String name;

	/**
	 * Starts a new unnamed thread with given parameters.
	 * @param p the objects to use as parameters
	 */
	public RunnableWithParams(Object[] p) {
		setParams(p);
	}

	/**
	 * @param p the objects to use as parameters
	 * @param name name for the thread of his runnable to take
	 */
	public RunnableWithParams(Object[] p, String name) {
		setParams(p);
		setName(name);
	}

	/**
	 * Overwrites the thread parameters with the given ones.
	 * @param p the parameters to set
	 */
	public void setParams(Object[] p) {
		this.p = p;
	}

	/** @param name The name to set. */
	public void setName(String name) {
		this.name = name;
	}

	/** should be overridden to use a specific ThreadPool */
	public ThreadPool getThreadPool() {
		return ThreadPool.get();
	}

	/**
	 * Runs this thread.
	 */
	public void start() {
		ThreadPool.get().runNamed(this, name);
	}

	/**
	 * Runs the code in this thread if this is the display thread (or there is no display);
	 * otherwise runs this in the display thread without waiting.
	 * @return true if task is completed in the caller's thread
	 * 	(i.e. if this is the display thread, or there is no display thread)
	 */
	public boolean callDefaultDisplayAsyncExec() {
		if (ProofGeneralPlugin.getDefault()==null ||
		    org.eclipse.swt.widgets.Display.getDefault()==null) {
			run();
			return true;
		}
		else if (isDisplayThread()) {
  		//System.out.println("running in current/display thread "+this);
			run();
			return true;
		} else {
			//System.out.println("queueing for async "+this);
			org.eclipse.swt.widgets.Display.getDefault().asyncExec(this);
			return false;
		}
	}

	/**
	 * Is the current thread the display ui thread?
	 */
	public static boolean isDisplayThread() {
		//this is a hack method to check whether we are already in the async method
		//we'd really like to call Display.isValidThread()
		Exception val = null;
		//addListener throws the thread exception if we're not the display thread, otherwise it throws illegal arg (because we pass it null)
		try { org.eclipse.swt.widgets.Display.getDefault().addListener(0, null); }
		catch (SWTException e) {
			if (e.code == SWT.ERROR_THREAD_INVALID_ACCESS) {
				return false;
			}
			val = e;
		} catch (IllegalArgumentException e) {
			return true;
		} catch (Exception e) {
			val = e;
		}
		//should never come here...
		System.err.println("RunnableWithParams.isDisplayThread() isn't getting the expected behaviour from Display.addListener(0,null) [got "+val+"]");
		assert false;
		return false;
	}
}
