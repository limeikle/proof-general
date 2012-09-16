/*
 *  $RCSfile: GetCommandResponseAction.java,v $
 *
 *  Created on 21 Apr 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.actions.torefactor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Element;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.CmdElement;
import ed.inf.proofgeneral.sessionmanager.PGIPSyntax;
import ed.inf.proofgeneral.sessionmanager.ProverDeadException;
import ed.inf.proofgeneral.sessionmanager.ScriptingException;
import ed.inf.proofgeneral.sessionmanager.events.PGIPEvent;
import ed.inf.proofgeneral.sessionmanager.events.PGIPIncoming;
import ed.inf.utils.datastruct.MutableObject;

/**
 * send selected text out to the prover as a command
 * 
 * 
 * @author Daniel Winterstein
 */

// TODO da: this needs to be simplified and, ideally wrapped into the CommandQueue.
// Private listener idea is maybe OK, although would be nicer if other listeners would
// merely ignore specialised output from another command --- which seems to be the
// main purpose of this (an escape mechanism to talk to the prover outside PGIP, essentially).
public class GetCommandResponseAction extends PGProverAction {

	/** Creates an instance of a command sender
	 */
	public GetCommandResponseAction() {
		setStatusDoneTrigger(STATUS_RUNNING | STATUS_DONE_PROVER);
		requiredProverStatusLevel = PROVER_ALIVE;
		requiredScriptStatusLevel = IGNORE_SCRIPT;
		if (!ProofGeneralPlugin.isEclipseMode())  {//needs to have a shutdown hook, outside of eclipse
			getSessionManager().addListener(this);
		}
	}

	private static class GetCommandResponseActionHolder {
		static GetCommandResponseAction singleton = new GetCommandResponseAction();
	}

	private static GetCommandResponseAction getSingleton() {
		return GetCommandResponseActionHolder.singleton;
	}

	private final Session defaultSession = new Session("default");

	public Session getDefaultSession() {
		// da: this secondary initialisation seems to be utterly superfluous
		// if (defaultSession!=null) {
		//	return defaultSession; // DC
		//}
		//synchronized (this) {
		//	if (defaultSession!=null) {
		//    return defaultSession;
		//	}
		// defaultSession = new Session("default");
		//}
		return defaultSession;
	}

	public static Session getDefault() {
		return getSingleton().getDefaultSession();
	}

	/** lets a caller get a persistent command session;
	 *  this takes ownership of the prover and keeps it until it is disposed!
	 *  so make sure to dispose of it.
	 * returns null if a session could not be made (prover is in use). */
	public static Session getSession(String name) {
		return getSingleton().getSpecialSession(name);
	}

	/** lets a caller get a persistent command session;
	 *  this takes ownership of the prover and keeps it until it is disposed!
	 *  so make sure to dispose of it.
	 * keeps trying to make the session, until it is interrupted.
	 *  the cycleTime indicates how aggressively to try to make the session
	 *  (implemented as:  it waits for that many ms to go by with no prover commands);
	 * if cycletime is 0, it waits in a cycle of 20ms if it couldn't get the prover
	 * @throws InterruptedException if the prover shuts down (the thread will have the interrupted flag set) */
	public static Session getSessionWaiting(String name, int cycleTime) throws InterruptedException {
		try {
		while (true) {
			long waitTime = cycleTime;
			if (System.currentTimeMillis() - getSingleton().getSessionManager().lastProverAction >= cycleTime) {
			  Session s = getSingleton().getSpecialSession(name);
			  if (s!=null) {
				  return s;
			  }
			} else {
				waitTime = cycleTime - (System.currentTimeMillis() - getSingleton().getSessionManager().lastProverAction);
			}
			ArrayList<Thread> waiters = getSingleton().myWaitingThreads;
			synchronized (waiters) {
			  waiters.add(Thread.currentThread());
			  waiters.wait(waitTime>0 ? waitTime : 20);
			  waiters.remove(Thread.currentThread());
			}
		}
		} catch (NullPointerException e) {
			if (getSingleton()==null || getSingleton().getSessionManager()==null) {
				throw new InterruptedException("shutting down");
			}
			throw e;
		}
	}

	public Session getSpecialSession(String name) {
		synchronized (this) {
			if (activeSpecialSession!=null) {
				return null;
			}
		  Session result = new Session(name);
//		  if (!getSessionManager().proverOwner.tryGetProverOwnership(result)) {
//		  	return null;
//		  }
		  activeSpecialSession = result;
		  return result;
		}
	}

	static MutableObject singleEntry = new MutableObject(null);

	static Session activeSpecialSession = null;

	@Override
    public Object getProverOwnershipObject() {
		if (activeSpecialSession!=null) {
			return activeSpecialSession;
		}
		return getDefaultSession();
	}

	public class Session {

		public String name;
		Session(String name) {
			this.name = name;
		}

		public void dispose() {
			activeSpecialSession = null; // FindBugs ST, FIXME: is it OK?
//			try {
//				getSessionManager().proverOwner.releaseOwnership(this);
//			} catch (ProverOwnedBySomeoneElseException e) {
//				e.printStackTrace();
//			}
		}

		@Override
        protected void finalize() {
			if (activeSpecialSession!=null) {
				System.err.println("Someone forgot to dispose of GetCommandResponseAction.Session["+toString()+"]");
				if (activeSpecialSession==this) {
					dispose();
				}
			}
		}

		@Override
        public String toString() {
			return "GetCommandResponseAction-"+name+"-"+hashCode();
		}

		/** Runs a command, returning the list of events returned by this command (possibly empty);
		 *  or null if couldn't get ownership; can throw InterruptedException or ProverDeadException */
		public List doCommandEvents(CmdElement command) throws InterruptedException, ProverDeadException {
			if (singleEntry.get()!=null) {
				return null;  //someone is running
			}
			synchronized (singleEntry) {
				if (singleEntry.get()!=null) {
					return null;  //someone is running
				}
				singleEntry.set(Thread.currentThread());
			}
			try {
				//we have control, according to 'singleEntry'; messy to have both this and trySetStatus, but something like that seems needed
				//is a special session active ?
				if (!getSessionManager().getProverState().isAlive()) {
					throw new ProverDeadException();
				}
				if (activeSpecialSession!=null && activeSpecialSession!=this) {
					return null;  //a 'special' session is running
				}
				try {
					synchronized (myWaitingThreads) {
						GetCommandResponseAction.this.command = command;
						result = null;
						run();
						if (result!=null && getStatus()!=STATUS_FAILED) {
							//result is initialised to new Array if we got through 'run' successfully
							myWaitingThreads.add(Thread.currentThread());
							int count=-1;
							while (getStatus()>=0 && count < 10) { // FIXME: 10 second timeout hardwired
								if (count++>0 && count%30==0) {
									System.err.println(this+" still waiting for response after "+count+"s; command was "+command);
								}
								myWaitingThreads.wait(1000);
							}
							myWaitingThreads.remove(Thread.currentThread());
						}
						return result;
					}
				} catch (InterruptedException e) {
					throw e;
					//return null;
				} finally {
					result = null;
				}
			} finally {
				singleEntry.set(null);
			}
		}

		public Element doCommand(String command,boolean waiting) throws InterruptedException, ProverDeadException {
			CmdElement cmdelt = new CmdElement(PGIPSyntax.SYSTEMCMD);
			cmdelt.setText(command);
			return doCommand(cmdelt,waiting);
		}

		/** runs a command, returning the element of the first PGIPOutgoing if there is one,
		 * null if there were no PGIPOutgoing responses
		 * <p/>
		 * this will block if the prover is unavailable,
		 * and is interrupted only on shutdown;
		 * if the caller wants to interrupt on other actions,
		 * the he should maintain a handle to that thread
		 * TODO maybe the interrupt button should interrupt this (and other things)
		 * @throws InterruptedException
		 */
		public Element doCommandWaiting(String command) throws InterruptedException, ProverDeadException {
			//System.out.println("SENDING "+command);
			return doCommand(command, true);
		}

		public Element doCommandWaiting(CmdElement command) throws InterruptedException, ProverDeadException {
			//System.out.println("SENDING "+command);
			return doCommand(command, true);
		}

		/** runs a command, returning the element of the first PGIPOutgoing if there is one;
		 * null means either there was no PGIPOutgoing responses, or we couldn't get the prover */
		public Element doCommand(String command) throws InterruptedException, ProverDeadException {
			try {
			  return doCommand(command, false);
			} catch (InterruptedException e) {
				//if we're interrupted, usu means prover shut down
				throw e;
//				System.err.println("doCommand no wait shouldn't throw InterruptedException...");
//				e.printStackTrace();
//				return null;
			}
		}

		/** runs a command, returning the element of the first PGIPOutgoing if there is one;
		 * null means either there was no PGIPOutgoing responses, or we couldn't get the prover */
		public Element doCommand(CmdElement command) throws InterruptedException, ProverDeadException {
			try {
			  return doCommand(command, false);
			} catch (InterruptedException e) {
				//if we're interrupted, usu means prover shut down
				throw e;
//				System.err.println("doCommand no wait shouldn't throw InterruptedException...");
//				e.printStackTrace();
//				return null;
			}
		}

		Element doCommand(CmdElement command, boolean waitForAvailability) throws InterruptedException, ProverDeadException {
			List r = null;
			//System.out.println("GCRA send ["+status+"]: "+command);   //may be a race condition here -AH
			//once observed status=2 ... but should always go to 1 then |2= 3
			r = doCommandEvents(command);
			while (r==null && waitForAvailability) {
				//System.out.println("GCRA send retry ["+status+"]: "+command);
				myWaitingThreads.add(Thread.currentThread());
				try {
					Thread.sleep(200);  //wait 200ms if prover is not available
				} finally {
				  myWaitingThreads.remove(Thread.currentThread());
				}
				r = doCommandEvents(command);
			}
			if (r==null) {
				return null;
			}
			Iterator re = r.iterator();
			while (re.hasNext()) {
				PGIPEvent e = (PGIPEvent)re.next();
				if (e instanceof PGIPIncoming && (e.parseTree!=null)) {
					//System.out.println("GCRA receive ["+status+"]: "+e.parseTree.asXML());
					return e.parseTree;
				}
			}
			//System.out.println("GCRA receive ["+status+"] null.");
			return null;
		}
	}

	CmdElement command;
	List<PGIPEvent> result = null;
	ArrayList<Thread> myWaitingThreads = new ArrayList<Thread>();

	@Override
    public void runSingly() {
		if (command==null) {
			updateStatus(STATUS_FAILED);
			result = null;
			return;
		}
		try {
			//DocElement command = new DocElement(PGIPSyntax.SPURIOUSCOMMAND,null);
//			SAXReader reader = new SAXReader();
//			Document document;
//			document = reader.read( new StringReader("<spuriouscmd>"+command+"</spuriouscmd>"));
//			Element cmd1 = (Element) document.content().get(0);
			//SessionManager sm = getSessionManager();
			//Element pr = (Element) sm.getParser().dumbParseText(s).elements().get(0);
			//DocElement cmd2 = new DocElement("spuriouscmd",null);
			//cmd2.setText(command);     //TODO xml escape this (if necessary -- ?)
			//cmd2.setAttributes(cmd1.attributes());
			result = new ArrayList<PGIPEvent>();
			//System.out.println("running command "+command+"\n as xml: "+cmd2.asXML());
			getSessionManager().queueCommand(command, this,
					Boolean.FALSE, new MutableObject(this)); //maybe wants this as a private listener?   //TODO still affecting queue
		} catch (Exception e) {
			if (!(e instanceof ScriptingException)) {
				e.printStackTrace();
			}
			//error(e);
			updateStatus(STATUS_FAILED);
			result = null;
		}
	}

	@Override
    public void onProblemsStartingRun(String message) {
		//do nothing, the call will just return null (and possibly keep waiting)
	}

	/**
	 * @see ed.inf.proofgeneral.editor.actions.defunct.PGAction#handleOurPgipEvent(ed.inf.proofgeneral.sessionmanager.events.PGIPEvent)
	 */
	@Override
    public boolean handleOurPgipEvent(PGIPEvent event) {
		//System.out.println("handling "+event+"; result is "+result);
		if (result!=null) {
			result.add(event);
		} else {
			System.err.println("GetCommandResultAction has a null result list");
		}
		return super.handleOurPgipEvent(event);
	}


	/** this needs to interrupt any running threads on prover shutdown
	 * @see ed.inf.proofgeneral.editor.actions.defunct.PGAction#handleProverShutdown(ed.inf.proofgeneral.sessionmanager.events.PGIPEvent)
	 */
	@Override
    public boolean handleProverShutdown(PGIPEvent event) {
		super.handleProverShutdown(event);
		interruptAllWaitingThreads();
		return true;
	}
	/**
	 *
	 */
	private void interruptAllWaitingThreads() {
		synchronized (myWaitingThreads) {
			while (myWaitingThreads.size()>0) {
				Thread t = myWaitingThreads.remove(0);
				t.interrupt();
			}
		}
	}

	/**
	 * @see ed.inf.proofgeneral.editor.actions.defunct.PGProverAction#onRunFinished(int)
	 */
	@Override
    public void onRunFinished(int finalStatus) {
		//System.out.println("onRunfinished("+finalStatus+"), result is "+result);
		super.onRunFinished(finalStatus);
		synchronized (myWaitingThreads) {
			myWaitingThreads.notifyAll();
		}
	}
}
