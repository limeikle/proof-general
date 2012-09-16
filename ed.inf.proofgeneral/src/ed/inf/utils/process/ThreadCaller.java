/*
 *  $RCSfile: ThreadCaller.java,v $
 *
 *  Created on 14 Jan 2002 by heneveld
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.process;

import java.io.PrintStream;
import java.io.PrintWriter;

import ed.inf.utils.datastruct.NumericStringUtils;
/**
 * starts to implement a session which can be run in a thread.
 * "runSession" is the method descendants should overwrite (??? or Callable ?):
 * it should return the XmlObject result and/or throw errors
 * in the normal way one desires of methods;
 * this ancestor sits around that method and ensures that
 * the result / exception / ThreadNotFinishedException are returned.
 * The method 'call' is the main way to execute this code,
 * with an optional parameter which specifies the amount of time to wait on the thread.
 * All objects waiting on this will receive notification when the run is about to finish.
 * @author heneveld
 */
public class ThreadCaller implements Runnable {

	// included for compatibility
    // public interface Callable extends ed.inf.utils.process.Callable {}

    public ThreadCaller(ed.inf.utils.process.Callable target, String name) {
        this.target = target;
        this.name = name;
    }

    public ThreadCaller(ed.inf.utils.process.Callable target) {
        this.target = target;
    }

    public static final int NO_WAIT = -1;
    public static final int WAIT_FOREVER = 0;

    public ed.inf.utils.process.Callable target = null;
    String name = null;

    public Object call(Object params[]) { return call(params, 0); }
    public Object call(int timeout) { return call(null, timeout); }
    public Object call() { return call(null, 0); }

    Thread pt = null;

  /** calls the target, with the specified parameters, in the specified timeout mode (could be NO_WAIT or WAIT_FOREVER);
   *  if NO_WAIT, returns null; otherwise returns result (if it finishes) or throws ThreadTimeoutException
   */
    public Object call(Object params[], int timeout) {
        pt =  name==null ? new Thread(this) : new Thread(this,name);
        setParams(params);
        synchronized (this) {  //not sure it needs to be synchronized, but it doesn't hurt
            runFinishedTime=-1;
        }
        pt.start();
      if (timeout==NO_WAIT) {
		return null;
	}
        try {
            if (timeout!=NO_WAIT) {
				join(timeout);
			}
        } catch (InterruptedException e) {
            throw new RuntimeException(e); //new InterruptedException("ThreadCaller.call was interrupted")); @todo should be own class
        }
        if (!isFinished()) {
			throw new RuntimeException //ThreadTimeoutException   @todo should be own class
			   ("The thread "+pt+" did not finish its task within "+NumericStringUtils.makeTimeString(timeout)+"; running in background");
		}
        return getResult(timeout);
    }

    Object[] params = null;
    public void setParams(Object[] params) { this.params = params; } // EI
    public Object[] getParams() { return params; } // EI

    public void run() {
        synchronized (this) {  //not sure it needs to be synchronized, but it doesn't hurt
            runFinishedTime=-1;
            runStartTime=System.currentTimeMillis();
        }
        //System.out.println("ThreadCaller "+this+": starting run");
        try {
            this.result = target.run(params);
        } catch (Throwable exc) {
            //System.out.println("ThreadCaller "+this+": setting exception "+exc);
            this.exc = exc;
        }
        //System.out.println("ThreadCaller "+this+": ending run");
        synchronized (this) {
            runFinishedTime=System.currentTimeMillis();
            this.notifyAll();
        }
    }

    public void interrupt() {
        if (pt!=null) {
        	pt.interrupt();
        }
    }

    public void join() throws InterruptedException {
        join(0);
    }
    public void join(int ms) throws InterruptedException {
        if (pt!=null && !isFinished()) {
        	pt.join(ms);
        }
    }

    long runFinishedTime = -1;
    public boolean isFinished() {
    	return (runFinishedTime>-1);
    }

    long runStartTime = -1;
    public boolean isStarted() {
    	return (runStartTime>-1);
    }

    /** returns the time this has spent running, in ms
     *  (check isFinished to determine if this is active); -1 if not started yet */
    public long getRunningTime() {
    	if (isFinished()) {
    		return runFinishedTime - runStartTime;
    	}
    	if (isStarted()) {
    		return System.currentTimeMillis() - runStartTime;
    	}
    	return -1;
    }

    Object result = null;
    public Object getResult() {
    		return getResult(0);
    }

    public Object getResult(int allowUnfinished) {
    	if (allowUnfinished==0 && !isFinished()) {
    		throw new RuntimeException("ThreadCaller result being accessed before finished.");
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
        if (allowUnfinished==0 && !isFinished()) {
            throw new RuntimeException("ThreadCaller being cleared before finished.");
        }
        runFinishedTime = -1;
        runStartTime = -1;
        result = null;
        exc = null;
    }

    Throwable exc = null;
    public Throwable getException() {
    	return exc;
    }

    public static class RuntimeExceptionWrapper extends RuntimeException {
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
