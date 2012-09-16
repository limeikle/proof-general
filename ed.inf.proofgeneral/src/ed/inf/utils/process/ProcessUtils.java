/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jul 9, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */

package ed.inf.utils.process;

/**
 * Utilities for helping with processes.
 */
public class ProcessUtils {
	
	/**
	 * Wait for a process to terminate.  If it doesn't within the given
	 * number of seconds, destroy it.  Check it twice per second for termination.
	 * @param process
	 * @param secondsTimeOut number of seconds to wait
	 * @returns the exit code of the process, or -1 if it was destroyed. 
	 */
	public static int waitFor(Process process, int secondsTimeOut) {
		for (int time = 0; time < secondsTimeOut; time++) {
			try {
				process.exitValue();
			} catch (IllegalStateException e) {
				// not terminated yet
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				break;
			}
			time++;
		}
		try {
			return process.exitValue();
		} catch (IllegalStateException e) {
			process.destroy();
			return -1;
		}
	}
}
