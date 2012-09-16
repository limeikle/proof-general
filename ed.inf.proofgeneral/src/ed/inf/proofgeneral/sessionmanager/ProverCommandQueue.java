/*
 *  $RCSfile: ProverCommandQueue.java,v $
 *
 *  Created on Nov 23, 2006 by da
 *  part of Proof General for Eclipse
 */

package ed.inf.proofgeneral.sessionmanager;

/**
 * This class implements the command queue for delivering commands to a running prover.
 * There is a unique command queue for each running prover/session manager.
 * The queue may be active (non-empty, being processed) or empty and inactive.
 * Commands can only be added if it is free: this is a first approximation but gets
 * us going.  Commands are added all at once in a list.
 * Commands are fed to the prover one-by-one in a thread that is spawned to process 
 * the queue.  
 * Output from the prover can be matched against items sent from the queue.
 * We only allow one command to be sent to the prover at a time; we process output
 *  from the prover until the next <ready/>.
 * 
 */
public class ProverCommandQueue {

}
