/*
 *  $RCSfile: Drudge.java,v $
 *
 *  Created on 01 Dec 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.process;
import java.util.Stack;

// da: NB: this class currently isn't used.

/**
 * A class for doing low priority jobs.
 * TODO use this to do bg parsing.
 * @author Daniel Winterstein
 */
public class Drudge {

    /**
     * A low priority thread for the drudge to work in.
     */
	private Thread drudgeThread = new Thread() {        
        /** 
         The drudge just does whatever jobs are in the queue, in order.
         */
        public void run() {
            doNextJob();
        }
    };
    
    /**
     * Stack of jobs to do.
     */
    private Stack<Runnable> jobQueue = new Stack<Runnable>();
    
    /**
     * Add a job to the list of jobs. 
     * Starts the drudge's thread going if necessary. 
     * @param job
     */
    public void addJob(Runnable job) {
        jobQueue.add(job);
        if (jobQueue.size()==1) {
            drudgeThread.start();
        }
    }
    
    /**
     * Do the next job, and the next, and the next...
     */
    void doNextJob() {
        Runnable job = jobQueue.remove(0);
        job.run();
        if (jobQueue.size() != 0) {
            doNextJob();
        }
    }

    /**
     * Create a new drudge; a job queue with a low priority thread to work in.
     */
    public Drudge() {
        drudgeThread.setPriority(Thread.MIN_PRIORITY);
        drudgeThread.setDaemon(true);
    }
    
}
