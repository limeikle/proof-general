/*
 *  $RCSfile$
 *
 *  Created on 03 Jun 2005 by Alex Heneveld
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.standalone;

import java.util.ArrayList;

import org.dom4j.Element;

import ed.inf.proofgeneral.document.CmdElement;
import ed.inf.proofgeneral.editor.lazyparser.ExternalLazyParser;
import ed.inf.proofgeneral.editor.lazyparser.Parser;
import ed.inf.proofgeneral.sessionmanager.CommandQueuedWithValues;
import ed.inf.proofgeneral.sessionmanager.PGIPSyntax;
import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.proofgeneral.sessionmanager.events.CommandProcessed;
import ed.inf.proofgeneral.sessionmanager.events.IPGIPListener;
import ed.inf.proofgeneral.sessionmanager.events.PGIPError;
import ed.inf.proofgeneral.sessionmanager.events.PGIPEvent;
import ed.inf.proofgeneral.sessionmanager.events.PGIPIncoming;
import ed.inf.proofgeneral.sessionmanager.events.PGIPReady;
import ed.inf.utils.datastruct.MutableObject;
import ed.inf.utils.exception.KnownException;
import ed.inf.utils.process.PooledRunnable;

/**
 * ProverInterface -- a wrapper for the SM so classes can use the prover nicely
 */
public class ProverInterface {

	/** null constructor; user must call init(sm) */
	public ProverInterface() {}

	/** usual constructor, calls init(sm) */
	public ProverInterface(SessionManager sm) {
		init(sm);
	}

	/** initialises this for use with the given SessionManager;
	 *  can be called multiple times to change sm (though not tested) */
	public synchronized void init(SessionManager sm) {
		this.sm = sm;
		if (myProverListener!=null) {
			sm.removeListener(myProverListener);
		}
		sm.addListener(myProverListener = new MyProverListener());
	}

	public IPGIPListener myProverListener = null;  //TODO shouldn't be public
	public SessionManager sm = null;  //TODO shouldn't be public

	public SessionManager getSessionManager() {
		return sm;
	}

  /** set to "starting" when it's starting up */
  MutableObject proverOwner = new MutableObject(null);

	//---------------------------------------------------------------------


	public class MyProverListener implements IPGIPListener {

		public void pgipEvent(PGIPEvent event) {
			if (event.cause==null || !(event.cause instanceof ProverCommandResponse)) {
				//not ours, probably ignore it
				if ("starting".equals(proverOwner.get())) {
					if (event instanceof PGIPReady) {
						synchronized (proverOwner) {
							proverOwner.set(null);
							proverOwner.notifyAll();
						}
					}
				}
				//TODO maybe respond to interrupts/shutdown ?
				return ;
			}
//			System.out.println("StandaloneProver: got event "+event);
			ProverCommandResponse r = (ProverCommandResponse)(event.cause);
			if (r.events==null) {
				System.err.println("null events list on cause "+r);
				return;
			}
			r.events.add(event);
			if (event instanceof PGIPIncoming) {
				if (r.firstIncoming==null) {
					r.firstIncoming = event;
				}
				r.lastIncoming = event;
			} else if (event instanceof PGIPError) {
				if (r.errorEvent==null) {
					r.errorEvent = event;
				}
				if (!((PGIPError)event).nonFatal()) {
					r.fatalErrorEvent = event;
				}
			//TODO may need to listen to other events
			} else if (event instanceof CommandProcessed) {
				synchronized (r) {
					r.finished = true;
//					System.out.println("notifying "+r);
					r.notifyAll();
				}
			}
		}

	}

	//---------------------------------------------------------------------


	/** shuts down the theorem prover; blocks until it's actually done */
	public synchronized void dispose() {
		if (sm==null) {
			System.err.println("ProverInterface.dispose called after disposal (or when not initialised).");
			return;
		}
		sm.stopSession(this,true);
		myProverListener = null;
		sm = null;
	}

	/** sends the command to the theorem prover (technically, it queues the command for parsing and sending)
	 *  @return a wrapper for the result; use result.waitUntilDone() to wait for the command to finish
	 *          (fields will be null otherwise) */
	public ProverCommandResponse queueCommand(String command)
	{
		final ProverCommandResponse cmd = new ProverCommandResponse(command);
		new PooledRunnable("ProverStandalone.QueueCommand") {
			public void run() {
				try {
//					System.out.println(""+this+" parsing");
					Element elt = parseCommandWaiting(cmd.commandString);
					if (elt==null) {
						cmd.commandElement = null;
						throw new KnownException("couldn't parse '"+cmd.commandString+"'; got null");
					}

					// da: refactoring to remove DummyDocElement changed return type of
					// parseCommandWaiting; below is approximate attempt to recover.
					cmd.commandElement = new CmdElement(elt.getStringValue()); 

					//					System.out.println(""+this+" queueing");
					ProverInterface.this.queueCommand(cmd.commandElement);
//					System.out.println(""+this+" done");
//				} catch (InterruptedException e) {
//					cmd.interrupted = true;
//					synchronized (cmd) {
////						System.out.println("notifying on interrupt "+cmd);
//						cmd.notifyAll();
//					}
				} catch (Exception e) {
					e.printStackTrace();
					cmd.errorException = e;
					synchronized (cmd) {
//						System.out.println("notifying on error "+cmd);
						cmd.notifyAll();
					}
				}
			}
		}.start();
	  return cmd;
	}

	/** sends the command (as PGIP XML, without the PGIP tag)
	 *  to the theorem prover (or queues the command for sending)
	 *  @return a wrapper for the result; use result.waitUntilDone() to wait for the command to finish
	 *          (fields will be null otherwise) */
	public ProverCommandResponse queueCommand(CmdElement command)
	{
		final ProverCommandResponse cmd = new ProverCommandResponse(command);
		new PooledRunnable() {
			public void run() {
				try {
					queueCommand(cmd.commandString);
				} catch (Exception e) {
					//e.printStackTrace();
					synchronized (cmd) {
						cmd.errorException = e;
						cmd.notifyAll();
					}
				}
			}
		}.start();
	  return cmd;
	}
	public void queueCommand(final ProverCommandResponse cmd) {
//		System.out.println("queueing "+cmd);
		sm.queueCommand(new CommandQueuedWithValues(sm, cmd.commandElement, cmd,
				null, 
				new MutableObject( myProverListener )) {
			@Override
            public void onSend() {
//				System.out.println("SENDING: "+this);
				cmd.events = new ArrayList<PGIPEvent>();
			}
		});
	}

	/** parses a command, then sends it, and waits until the response is complete */
	public ProverCommandResponse sendCommand(String command, boolean log) throws InterruptedException {
		ProverCommandResponse cmd = queueCommand(command);
		cmd.waitUntilDone();
		return cmd;
	}
	/** queues a command, then sends it, and waits until the response is complete */
	public ProverCommandResponse sendCommand(CmdElement command, boolean log) throws InterruptedException {
		ProverCommandResponse cmd = queueCommand(command);
		cmd.waitUntilDone();
		return cmd;
	}

	//TODO could provide a way to undo these commands

	//TODO could have a postCommand with a callback

	//----- command string parsing ----------------------------------

	Parser preferredParser = null;
	public Parser getPreferredParser() {
		synchronized (this) {
			if (preferredParser==null) {
				setPreferredParser(new ExternalLazyParser(sm));
				//sm.removeListener(elp);  //maybe it shouldn't listen to most things ... but could need shutdown info to clear waiting
			}
		}
		return preferredParser;
	}
	public void setPreferredParser(Parser newPreferredParser) {
		synchronized (this) {
			if (preferredParser!=null) {
				preferredParser.dispose();
			}
			preferredParser = newPreferredParser;
		}
	}

	/** calls to the theorem prover to parse the command */
	public Element parseCommandWaiting(String command) {
		try {
			CmdElement parseCmdElt = new CmdElement(PGIPSyntax.PARSESCRIPT);
			parseCmdElt.setText(command);
//			System.out.println("PARSE waiting "+command);
			ProverCommandResponse parseCmd = sendCommand(parseCmdElt, false);
			if (parseCmd.isError()) {
				System.err.println("couldn't parse "+command+": "+parseCmd.errorEvent+" / "+parseCmd.errorException);
				return null;
			}
			if (parseCmd.isInterrupted()) {
				throw new InterruptedException();
			}
			if (LOG_PARSE_GENERATES_MULTIPLE && parseCmd.lastIncoming!=parseCmd.firstIncoming) {
				System.err.println("got multiple responses to parse; ignoring all but the first");
			}
			if (parseCmd.firstIncoming==null || parseCmd.firstIncoming.parseTree.elements().size()==0) {
				System.err.println("couldn't parse "+command+": no elements in response");
				return null;
			}

			Element ce = (Element)parseCmd.firstIncoming.parseTree.elements().get(0);

// da: parse shouldn't generate multiple.  It's good to report errors but that
// would be much better done by a validator higher up.
// CLEANUP.
//			StringBuffer cs = new StringBuffer(); // CD: dead store (reported because LOG_PARSE_GENERATES_MULTIPLE defaults to false)
//			if (LOG_PARSE_GENERATES_MULTIPLE && ce.elements().size()>1) {
//				Iterator ci = ce.elementIterator();
//				while (ci.hasNext()) {
//					cs.append("   ITEM: ");
//					//cs.append(sm.getCommandString((Element)ci.next()));
//					cs.append(((Element)ci.next()).asXML());
//					if (ci.hasNext()) {
//						cs.append("\n");
//					}
//				}
//				System.out.println("PARSE generated multiple responses, parse of "+command+"\n"+cs.toString());
//
//				System.err.println("(throwing away all but the first)");
//			}
			// da: refactored to remove DummyDocElement atrocity; 
			// return new DummyDocElement((Element)ce.elements().get(0));
			return (Element)ce.elements().get(0);
		} catch (Exception e) {
			System.err.println("couldn't parse "+command+": "+e);
			e.printStackTrace();
			return null;
		}
	}

  //TODO we ignore non-first elements if the parse ever generates multiple, it might be a problem, set true to explore
  // da: this won't happen if you're parsing *commands* in Isabelle.  It can happen for terms, but we don't see
  // results of term parsing.
	public static final boolean LOG_PARSE_GENERATES_MULTIPLE = false;

}
