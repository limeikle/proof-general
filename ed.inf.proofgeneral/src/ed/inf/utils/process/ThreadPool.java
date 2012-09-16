/*
 *  $RCSfile: ThreadPool.java,v $
 *
 *  Created on 02 Mar 2004 by Alex Heneveld
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.process;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

// da: TODO: we should replace this with the job management and thread pool of Eclipse, if
// at all possible.  Code here has suspect concurrency handling and is not as lazy as
// Eclipse framework in generating threads.  I don't even see why we need so many threads!!

/**
 * ThreadPool -- pools of available threads; this is basically a singleton class through <code>get</code>,
 * though if someone wanted to they could invoke the constructor to make others.
 * <p>
 * Example:<br/>
 * <code>ThreadPool.get().run(Runnable code)</code> will run a runnable in a pooled thread;<br/>
 * it can also be done manually through <code>ThreadPool.PooledThread pt = ThreadPool.get().popAvailableThread()</code>
 * then <code>pt.start(Runnable code)</code>, which basically does the same thing.
 * <p>
 * When a PooledThread finishes a task, it will notifyAll.
 * </p>
 */
public class ThreadPool {

  // CLEANUP this isn't used
  //static Object lock = new Object();

  /** the main singleton item */
  private static class TheThreadPool {  // da: holder class idiom, see Goetz
	  public static ThreadPool main = new ThreadPool("Default thread pool");
  }

  /** returns the main (singleton) instance */
  public static ThreadPool get() {
	  return TheThreadPool.main;
  }

  String name;

  /**
   * Creates a thread pool with a given name
   * @param name
   */
  public ThreadPool(String name) {
	  this.name = name;
  }

  public String getName() { return name; }

  /**
   * All threads we have created.
   */
  ConcurrentLinkedQueue<PooledThread> allThreads = new ConcurrentLinkedQueue<PooledThread>();

  /**
   * those threads not being used; synchronize on this before removing, or better,
   * use popAvailableThread
   */
  LinkedList<PooledThread> availableThreads = new LinkedList<PooledThread>();

  /**
   * sets up a popped available thread running the runnable code
   * @return the thread where the Runnable is running (in case the caller wants to wait on it
   * (preferred method for waiting is <code>softJoin()</code>, which tests that it hasn't already finished
   */
  public PooledThread run(Runnable r) {
    PooledThread t = popAvailableThread();
    t.startNamed(r, "PooledThread-Active");
    return t;
  }

  public PooledThread runNamed(Runnable r, String name) {
//	  return runNamed(r, name, true);
//  }
//
	  //above and below, not needed because PooledRunnable sets taht
//  /** sets up a popped available thread with the given name, running the runnable code
//   *  (see run(Runnable r) ) with the given interruptability level */
//  public PooledThread runNamed(Runnable r, String name, boolean interruptIsOnlyForTask) {
    PooledThread t = popAvailableThread();
//    if (interruptIsOnlyForTask) t.allowThisTaskToBeInterrupted();
    t.startNamed(r, name);
    return t;
  }

  /**
   * @return total number of threads created (allThreads.size())
   */
  public int getThreadCount() { return allThreads.size(); }

  /**
   * @return total number of threads in use (allThreads.size()-availableThreads.size())
   */
  public int getActiveThreadCount() { return allThreads.size()-availableThreads.size(); }

  /**
   * gets (makes if necessary) a PooledThread that the caller can use;
   * the caller should only call run (Runnable code), which will pass the target to the threads main loop;
   * since the thread has already been started via <code>start()</code> and it is running its main loop
   * @return a PooledThread for the caller to use
   */
  public PooledThread popAvailableThread() {
    synchronized (availableThreads) {
      if (availableThreads.size()>0) {
        PooledThread t = availableThreads.get(0);
        if (t==null) {
        	//TODO not sure how a thread gets set null, but it does
        	//probably had to do with concurrent access to list, fixed now
        	if (availableThreads.size()==1) {
        		availableThreads.clear();
        		return popAvailableThread();
        	}
			//System.err.println("ThreadPool.availableThreads contains null; there may be minor errors.");
            try {
            	availableThreads.remove(0);
            } catch (Exception e) {
            	//System.err.println("ThreadPool.availableThreads did throw error: "+e);
            }
        } else {
        	try {
        		availableThreads.remove(0);
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        	return t;
        }
      }
      PooledThread t = new PooledThread(this);
//    System.err.println("Creating new thread");
      allThreads.add(t);
      t.privateStart();
      return t;
    }
  }

  /**
   * indicates that all threads should close, immediately if not in use,
   * or when they finish what they are doing
   */
  public void closeAll() {
    Iterator it = allThreads.iterator();
    while (it.hasNext()) {
      PooledThread pooledThread = (PooledThread) it.next();
      pooledThread.close();
    }
  }

  /**
   * an extension of Thread used by PooledThread, main method for callers is
   * the method <code>run(Runnable r)</code>
   */
  public static class PooledThread extends Thread {
    private final Object taskReady = new Object();
    private boolean closeWhenDone = false;
    private Runnable task = null;

    ThreadPool myThreadPool;
    PooledThread(ThreadPool myThreadPool) {
      this.myThreadPool = myThreadPool;
    }

    /**
     * tells the thread to end when it finishes its current task (if there is one)
     */
    public void close() {
      closeWhenDone = true;
      synchronized (taskReady) { taskReady.notifyAll(); }
    }

    private void privateStart() {
      super.start();
    }

    /**
     * throws an error; thread is started automatically by ThreadPool
     */
    @Override
    public synchronized void start() {
      throw new Error("ThreadPool.PooledThread is started automatically by ThreadPool");
    }

    /**
     * main run loop, waits for tasks to be set via <code>run(Runnable code)</code>
     * then runs them; started and run as needed automatically by the ThreadPool
     * (i.e. you should not normally call this)
     */
    @Override
    public void run() {
    	try {
    		while (!closeWhenDone) {
    			try {
    				setName("PooledThread-waiting");
    				synchronized (taskReady) {
    					//taskReady.wait(1);  //just to collect interrupts
    					while (task==null && !closeWhenDone) {
    						taskReady.wait();
    					}
    				}
    				if (task!=null) {
    					try {
    						task.run();
    					} catch (Throwable t) {
    						if (allowTaskInterrupted && interrupted()) {
    							allowTaskInterrupted = false;
    						}
    						else {
    							System.out.println("ERROR IN POOLED THREAD "+this+": "+t);
    							t.printStackTrace();
    						}
    					}
    					synchronized (this) {
    						task = null;
    						this.notifyAll();
    					}
    					if (myThreadPool!=null && !closeWhenDone) {
    						synchronized (myThreadPool.availableThreads) {
    							myThreadPool.availableThreads.add(this);
    						}
    					}
    				}
    			} catch (InterruptedException e) {
    				if (allowTaskInterrupted) {
    					interrupted();
    					allowTaskInterrupted = false;
    				} else {
						throw e;
					}
    			}
    		}
    	} catch (InterruptedException e) {}
    	if (myThreadPool!=null) {
    		synchronized (myThreadPool.availableThreads) {
    			myThreadPool.availableThreads.remove(this);
    		}
    	}
    }

    /**
     * Checks if a Runnable is the current task
     * @param r the task to check
     * @return true if r == current task.
     */
    public boolean isCurrentTask(Runnable r) {
    	return r==task;
    }

    /**
     * Will wait indefinitely for this thread to finish its current task, or return
     * immediately if one isn't set.
     * Note, one is set by <code>run(Runnable code)</code>
     * and <code>ThreadPool.get().run(Runnable code)</code>, so calling this method on
     * either of those will return when it has finished (is preferred to <code>join</code>
     * since that won't catch it if it returns immediately).
     */
    public void softJoin() throws InterruptedException { softJoin(0); }

    /**
     * Will wait for the set number of ms (or indefinitely if 0 set; error if negative
     * set, as it relies on 'wait') for this thread to finish its current task.
     * Returns immediately if one isn't set.
     * Note, one is set by <code>run(Runnable code)</code>
     * and <code>ThreadPool.get().run(Runnable code)</code>, so calling this method on
     * either of those will return when it has finished (is preferred to <code>join</code>
     * since that won't catch it if it returns immediately).
     */
    public void softJoin(int ms) throws InterruptedException {
      synchronized (this) {
        if (task==null) {
			return;
		}
        try {
          do {
        	  	this.wait(ms);
          } while (ms == 0 && task != null); // da: protect against spurious resume
        } catch (InterruptedException e) {
          throw e;
        }
      }
    }

    /**
     * Starts a new task in this thread.
     * Thread should be popped from the ThreadPool; it is returned when done.
     */
    public void start(Runnable code) {
      if (task!=null) {
    	  throw new Error(""+this+" is already running a task '"+task+"', cannot run '"+code+"'");
      }
      task = code;
      synchronized (taskReady) {
    	  taskReady.notifyAll();
      }
    }

    /**
     * Sets a name and calls run; the name will be cleared after run is complete.
     */
    public void startNamed(Runnable code, String name) {
      if (name!=null) {
        setName("PooledThread-"+name);
      } else {
      	setName("PooledThread-unnamed");
      }
      start(code);
    }

    boolean allowTaskInterrupted = false;

    /**
     * Makes any <code>interrupt()</code> call only apply to the current task,
     * i.e. not the thread.
     */
    public void allowThisTaskToBeInterrupted() {
      allowTaskInterrupted = true;
    }

  }

  /**
   * Returns debugging information about the current ThreadPool, in the format
   * <code>Object: Name [# active threads/# threads]</code>
   */
  @Override
public String toString() {
  	return super.toString()+": "+getName()+" ["+getActiveThreadCount()+"/"+getThreadCount()+"]";
  }

}
