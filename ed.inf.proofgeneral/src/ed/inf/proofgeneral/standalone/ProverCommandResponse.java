/*
 *  $RCSfile$
 *
 *  Created on 03 Jun 2005 by Alex Heneveld
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.standalone;

import java.util.ArrayList;

import ed.inf.proofgeneral.document.CmdElement;
import ed.inf.proofgeneral.sessionmanager.events.PGIPEvent;

public class ProverCommandResponse {
		public String commandString = null;
		public CmdElement commandElement = null;

		/** if using this constructor, you will probably also need to set the element */
		public ProverCommandResponse(String commandString) {
			this.commandString = commandString;
		}
		public ProverCommandResponse(CmdElement commandElement) {
			this.commandElement = commandElement;
			this.commandString = this.commandElement.getStringValue();
		}

		/** list of all PGIPEvents associated with this command */
		public ArrayList<PGIPEvent> events = null;
		public PGIPEvent firstIncoming = null;
		public PGIPEvent lastIncoming = null;
		public PGIPEvent errorEvent = null;
		public PGIPEvent fatalErrorEvent = null;

		public Exception errorException = null;

		/** returns whether this thread had an error */
		public boolean isError() {
			return ((errorException!=null) || (errorEvent!=null));
		}

		/** returns whether this thread had a fatal error */
		public boolean isFatalError() {
			return ((errorException!=null) || (fatalErrorEvent!=null));
		}

		/** returns whether this command has parsed to make an element */
		public boolean isParsed() {
			return commandElement!=null;
		}

		/** returns whether this command has been sent to the prover */
		public boolean isSent() {
			return (events!=null);
		}

		boolean finished = false;
		/** returns whether this command has finished (interrupted doesn't count, error does) */
		public boolean isFinished() {
			return finished || isFatalError();
		}

		boolean interrupted = false;
		/** returns whether this command has been interrupted */
		public boolean isInterrupted() {
			return interrupted;
		}

		/** returns immediately if the command has finished, otherwise waits until it finishes;
		 *  optional parameter waits for a certain amount of time;
		 * (assumes the command is queued already or in a different thread)
		 * @throws InterruptedException if the thread is interrupted
		 */
		public synchronized void waitUntilDone() throws InterruptedException {
//			System.out.println("waiting "+this);
			while (!isFinished() && !isInterrupted()) {
			  this.wait(500);  //should get notification, but just in case...
//			  SessionManager smm = SessionManager.getDefault();
//			  smm.commandHistory.getClass();
			}
//		  System.out.println("done waiting "+this);
		}
		/** returns immediately if the command has finished, otherwise waits until it finishes;
		 *  optional parameter waits for a certain amount of time;
		 * (assumes the command is queued already or in a different thread)
		 * @throws InterruptedException if the thread is interrupted
		 */
		public synchronized void waitUntilDone(long ms) throws InterruptedException {
			if (!isFinished() && !isInterrupted()) {
			  this.wait(ms); // FIXME Wa?
			}
		}
	}