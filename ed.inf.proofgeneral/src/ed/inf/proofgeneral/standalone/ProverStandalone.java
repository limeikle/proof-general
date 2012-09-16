/*
 *  $RCSfile: ProverStandalone.java,v $
 *
 *  Created on 03 May 2005 by Alex Heneveld
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.standalone;

import java.util.ArrayList;
import java.util.Iterator;

import org.dom4j.Element;
import org.eclipse.core.runtime.Preferences;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.ProofGeneralProverRegistry.Prover;
import ed.inf.proofgeneral.ProofGeneralProverRegistry.ProverRegistryException;
import ed.inf.proofgeneral.document.CmdElement;
import ed.inf.proofgeneral.editor.lazyparser.ExternalLazyParser;
import ed.inf.proofgeneral.editor.lazyparser.Parser;
import ed.inf.proofgeneral.sessionmanager.CommandQueuedWithValues;
import ed.inf.proofgeneral.sessionmanager.PGIPSyntax;
import ed.inf.proofgeneral.sessionmanager.ProverKnowledge;
import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.proofgeneral.sessionmanager.ProverKnowledge.KnowledgeItem;
import ed.inf.proofgeneral.sessionmanager.events.CommandProcessed;
import ed.inf.proofgeneral.sessionmanager.events.IPGIPListener;
import ed.inf.proofgeneral.sessionmanager.events.PGIPError;
import ed.inf.proofgeneral.sessionmanager.events.PGIPEvent;
import ed.inf.proofgeneral.sessionmanager.events.PGIPIncoming;
import ed.inf.proofgeneral.sessionmanager.events.PGIPReady;
import ed.inf.utils.datastruct.MutableObject;
import ed.inf.utils.datastruct.NumericStringUtils;
import ed.inf.utils.eclipse.XmlPreferenceStore;
import ed.inf.utils.exception.KnownException;
import ed.inf.utils.process.PooledRunnable;

/**
 * ProverStandalone -- a class for using a theorem prover (eg Isabelle) through the Java PG classes,
 * but without loading the Eclipse interface
 */
public class ProverStandalone extends ProverInterface {

	private static ProverStandalone singleton = null;

	/** private constructor, as users should use static create to make a singleton instance 
	 * @throws InterruptedException */
	private ProverStandalone(Prover prover,Preferences prefStore) throws InterruptedException {
		ProofGeneralPlugin.setStaticPreferenceStore(prefStore);		
		synchronized (proverOwner) {
			SessionManager localSM = new SessionManager(prover);
//			proverOwner.set("starting");
//			localSM.init(proverName);
			init(localSM);
//			proverOwner.wait(); // FIXME: UW
		}		
	}

	/** creates a theorem prover session for the specified prover using the specified preference store
	 *  (eg DEFAULT_PREFERENCES, which caller could modify directly or clone then modify);
	 *  then initialises (starts) the prover session.
	 * <p/>
	 * this class provides routines which can then send commands to the theorem prover.
	 * <p/>
	 * currently only one Standalone session can be created at a time.
	 * @throws ProverRegistryException 
	 */
	public synchronized static ProverStandalone create(String proverName, Preferences prefStore) throws ProverRegistryException {
		Prover prover = ProofGeneralPlugin.getRegistry().getProver(proverName);
//		String s = prefStore.getString("Port");
//		s = prefStore.getString("Isabelle Start Command");
//		s = prefStore.getString("Start Command");
 		if (singleton!=null) {
			throw new RuntimeException("ProverStandalone already exists ("+singleton+"); you can only have one instance.");
		}
	  try {
		  singleton = new ProverStandalone(prover, prefStore);
		  singleton.initManager(prover);
	  } catch (InterruptedException e) {
	  	singleton.dispose();
	  	throw new RuntimeException("ProverStandalone couldn't be created; thread was interrupted.");
	  }
	  return singleton;
	}

	/** creates a theorem prover session using the specified prover (Isabelle is the only one available)
	 *  and DEFAULT_PREFERENCES;
	 *  then initialises (starts) the prover session.
	 * <p/>
	 * this class provides routines which can then send commands to the theorem prover.
	 * <p/>
	 * currently only one Standalone session can be created at a time.
	 * @throws ProverRegistryException 
	 */
	public static ProverStandalone create(String proverName) throws ProverRegistryException {
		return create(proverName, DEFAULT_PREFERENCES);
	}

	/** creates a theorem prover session using the specified prover (Isabelle is the only one available)
	 *  and DEFAULT_PREFERENCES;
	 *  then initialises (starts) the prover session.
	 * <p/>
	 * this class provides routines which can then send commands to the theorem prover.
	 * <p/>
	 * currently only one Standalone session can be created at a time.
	 * @throws ProverRegistryException 
	 */
	public static ProverStandalone createIsabelleWithLogic(String logicName) throws ProverRegistryException {
		return createIsabelleWithLogic(logicName, null);
	}

	/** creates a theorem prover session using the specified prover (Isabelle is the only one available)
	 *  and preference store (or the DEFAULT_PREFERENCES, which may be out of date, if store is null);
	 *  then initialises (starts) the prover session.
	 * <p/>
	 * this class provides routines which can then send commands to the theorem prover.
	 * <p/>
	 * currently only one Standalone session can be created at a time.
	 * @throws ProverRegistryException 
	 */
	public static ProverStandalone createIsabelleWithLogic(String logicName, Preferences prefs) throws ProverRegistryException {
		if (prefs==null) {
			prefs = (Preferences)(DEFAULT_PREFERENCES.clone());
		}
		String tp = "Isabelle";
		prefs.setValue("Isabelle Start Command", prefs.getString("Isabelle Start Command")+" "+logicName);
		return create(tp, prefs);
	}

	/** a (currently-functioning) set of default preferences, in case IsabellePrefs.xml isn't around;
	 * they might change without notice so this is not guaranteed */
	// da: Why not just use the Eclipse file format for preferences here?
	// ah: i think i was having trouble getting them to load when running as plain old java (non-OSGi)
  public static final XmlPreferenceStore DEFAULT_PREFERENCES = new XmlPreferenceStore();

  public static final String DEFAULT_IsabelleLexicalSyntaxFile = "IsabelleLexicalSyntax.xml";


  static {
  	DEFAULT_PREFERENCES.setFromXmlString(
			"<preferences prefix=\"Isabelle \">" +
				"<pref name=\"Start Command\" label=\"Start Command\" class=\"String\" default=\"isabelle -I -X\" />" +
				"<pref name=\"Use Sockets\" label=\"Use remote version and sockets?\" class=\"Boolean\" default=\"false\" />" +
				"<pref name=\"Host Address\" label=\"Host Address\" class=\"String\" default=\"localhost\" />" +
				"<pref name=\"Port\" label=\"Port\" class=\"Integer\" default=\"5678\" />" +
				"<pref name=\"Startup Script\"" +
				"  label=\"Proof script to be run at session start\"" +
				"  class=\"Text\" default=\"\" />" +
				"<pref name=\"Isabelle Stop Command\" label=\"Stop Command\" class=\"String\"" +
				"  default=\"\"/>" +
				"<pref name=\"use PGIP interrupts\" label=\"Send PGIP interrupts?\" "+
				"  tooltip=\"Send (as opposed to tty ones)\""+
				"  class=\"Boolean\" default=\"false\" />"+
				"<pref name=\"Isabelle ready messages\" label=\"Prover sends PGIP ready messages?\" class=\"Boolean\"" +
				"  default=\"true\" />" +
				"<pref name=\"Isabelle Keywords File\" label=\"Keywords File\"" +
				"  class=\"File\" default=\"IsabelleKeywords.txt\"" +
				"  tooltip=\"A file containing a list of key-words\" id=\"IsabelleKeywordsFile\" >" +
				"  <description>A file containing a list of key-words; used for syntax highlighting.</description>" +
				"</pref>" +
				"<pref name=\"Isabelle LINEEND\" label=\"New line character(s)\" class=\"String\" default=\"\\n\" />" +
				"<pref name=\"StyleSheet File\" label=\"Style-sheet File\""+
				"  class=\"File\" default=\"pgmlStyleFile.xsl\""+
				"  tooltip=\"An xsl file used to pretty-format prover output\" id=\"StyleSheetFile\"/>"+
				"<pref name=\"Lexical Syntax File\""+
				"  label=\"Lexical Syntax File\""+
				"  class=\"File\" default=\""+DEFAULT_IsabelleLexicalSyntaxFile+"\""+
				"  tooltip=\"A file containing pgip defintions for the basic alphabet of the prover\""+
				"  id=\"IsabelleLexicalSyntaxFile\">"+
				"  <description>A file containing pgip definitions for the basic lexical constants -"+
				"  e.g. string delimiters, word characters, etc. Used for syntax highlighting.</description>"+
				"</pref>"+
    	"</preferences>");
  }

	void initManager(Prover prover) throws InterruptedException {
		synchronized (proverOwner) {
			proverOwner.set("starting");
		  sm = new SessionManager(prover);
		  proverOwner.wait(); // FIXME: UW
		}
	}

	@Override
    public synchronized void dispose() {
		super.dispose();
		singleton = null; // ST: ignore it
	}

	//--------------------------------------------------------------------------------------

//begin OLDCODE
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
			//TODO may need to listen to other events
				}
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

//	public static class ProverCommandResponse {
//		public String commandString = null;
//		public DocElement commandElement = null;
//
//		/** if using this constructor, you will probably also need to set the element */
//		public ProverCommandResponse(String commandString) {
//			this.commandString = commandString;
//		}
//		public ProverCommandResponse(DocElement commandElement) {
//			this.commandElement = commandElement;
//			this.commandString = this.commandElement.getStringValue();
//		}
//
//		/** list of all PGIPEvents associated with this command */
//		public ArrayList events = null;
//		public PGIPEvent firstIncoming = null;
//		public PGIPEvent lastIncoming = null;
//		public PGIPEvent errorEvent = null;
//		public PGIPEvent fatalErrorEvent = null;
//
//		public Exception errorException = null;
//
//		/** returns whether this thread had an error */
//		public boolean isError() {
//			return ((errorException!=null) || (errorEvent!=null));
//		}
//
//		/** returns whether this command has parsed to make an element */
//		public boolean isParsed() {
//			return commandElement!=null;
//		}
//
//		/** returns whether this command has been sent to the prover */
//		public boolean isSent() {
//			return (events!=null);
//		}
//
//		boolean finished = false;
//		/** returns whether this command has finished (interrupted doesn't count, error does) */
//		public boolean isFinished() {
//			return finished || isError();
//		}
//
//		boolean interrupted = false;
//		/** returns whether this command has been interrupted */
//		public boolean isInterrupted() {
//			return interrupted;
//		}
//
//		/** returns immediately if the command has finished, otherwise waits until it finishes;
//		 *  optional parameter waits for a certain amount of time;
//		 * (assumes the command is queued already or in a different thread)
//		 * @throws InterruptedException if the thread is interrupted
//		 */
//		public synchronized void waitUntilDone() throws InterruptedException {
////			System.out.println("waiting "+this);
//			while (!isFinished() && !isInterrupted()) {
//			  this.wait(500);  //should get notification, but just in case...
////			  SessionManager smm = SessionManager.getDefault();
////			  smm.commandHistory.getClass();
//			}
////		  System.out.println("done waiting "+this);
//		}
//		/** returns immediately if the command has finished, otherwise waits until it finishes;
//		 *  optional parameter waits for a certain amount of time;
//		 * (assumes the command is queued already or in a different thread)
//		 * @throws InterruptedException if the thread is interrupted
//		 */
//		public synchronized void waitUntilDone(long ms) throws InterruptedException {
//			if (!isFinished() && !isInterrupted())
//			  this.wait(ms);
//		}
//	}

	/** sends the command to the theorem prover (technically, it queues the command for parsing and sending)
	 *  @return a wrapper for the result; use result.waitUntilDone() to wait for the command to finish
	 *          (fields will be null otherwise) */
	@Override
    public ProverCommandResponse queueCommand(String command) {
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
					queueCommand(cmd);
//					System.out.println(""+this+" done");
//				}
//				catch (InterruptedException e) {
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
    @Override
    public ProverCommandResponse queueCommand(CmdElement command)
	{
		final ProverCommandResponse cmd = new ProverCommandResponse(command);
		new PooledRunnable() {
			public void run() {
				try {
					queueCommand(cmd);
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
	@Override
    public void queueCommand(final ProverCommandResponse cmd) {
//		System.out.println("queueing "+cmd);
		sm.queueCommand(new CommandQueuedWithValues(sm, cmd.commandElement, cmd,
				null,
				new MutableObject(myProverListener)) {
			@Override
            public void onSend() {
//				System.out.println("SENDING: "+this);
				cmd.events = new ArrayList<PGIPEvent>();
			}
		});
	}

	/** parses a command, then sends it, and waits until the response is complete */
    public ProverCommandResponse sendCommand(String command) throws InterruptedException {
		ProverCommandResponse cmd = queueCommand(command);
		cmd.waitUntilDone();
		return cmd;
	}

	/** queues a command, then sends it, and waits until the response is complete */
    public ProverCommandResponse sendCommand(CmdElement command) throws InterruptedException {
		ProverCommandResponse cmd = queueCommand(command);
		cmd.waitUntilDone();
		return cmd;
	}


	//TODO could provide a way to undo these commands

	//TODO could have a postCommand with a callback

	//----- command string parsing ----------------------------------

	Parser preferredParser = null;
	@Override
    public Parser getPreferredParser() {
		synchronized (this) {
			if (preferredParser==null) {
				setPreferredParser(new ExternalLazyParser(sm));
				//sm.removeListener(elp);  //maybe it shouldn't listen to most things ... but could need shutdown info to clear waiting
			}
		}
		return preferredParser;
	}

	@Override
    public void setPreferredParser(Parser newPreferredParser) {
		synchronized (this) {
			if (preferredParser!=null) {
				preferredParser.dispose();
			}
			preferredParser = newPreferredParser;
		}
	}

	/** calls to the theorem prover to parse the command */
	@Override
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
			Element parseBody = null;
			if (ce.elements().size()>1) {
				StringBuffer cs = new StringBuffer();
				Iterator ci = ce.elementIterator();
				Element current = null;
				while (ci.hasNext()) {
					cs.append("   ITEM: ");
					current = (Element) ci.next();
					//cs.append(sm.getCommandString((Element)ci.next()));
					cs.append(current.asXML());
					if (ci.hasNext()) {
						cs.append("\n");
					}
					if (!current.getName().startsWith("error"))
						if (parseBody==null) {
							parseBody=current;
//TODO fix Isabelle bug where it generates errorresponses on certain things, like 'declare' and 'datatype'
						}
				}
				if (LOG_PARSE_GENERATES_MULTIPLE) {
				  System.out.println("PARSE generated multiple responses, parse of "+command+"\n"+cs.toString());
				  //System.err.println("(throwing away all but the first)");
				}
			}
			if (parseBody==null) {
				parseBody=(Element) ce.elements().get(0);
			}
			return parseBody;
		} catch (Exception e) {
			System.err.println("couldn't parse "+command+": "+e);
			e.printStackTrace();
			return null;
		}
	}

  //TODO we ignore non-first elements if the parse ever generates multiple, it might be a problem, set true to explore
	public static final boolean LOG_PARSE_GENERATES_MULTIPLE = false;

	/** blocks until the prover is available and ownership released
	 * @param eventsToo whether to wait for events to be processed as well
	 * (if false just waits for prover command queue to empty, not for events to be handled)
	 */
	public void waitForAvailable(boolean eventsToo) throws InterruptedException {
		waitForAvailable(eventsToo, -1);
	}

	/** blocks until the prover is available and ownership released
	 * @param eventsToo whether to wait for events to be processed as well
	 * (if false just waits for prover command queue to empty, not for events to be handled)
	 * @param timeoutMillis how long to wait; forever if negative; doesn't wait just checks if 0)
	 * @throws InterruptedException
	 * @return true iff prover is available; false on timeout
	 */
	public boolean waitForAvailable(boolean eventsToo, long timeoutMillis) throws InterruptedException {
		long endTime = System.currentTimeMillis() + timeoutMillis;
		if (!sm.commandQueueWait(timeoutMillis, endTime)) {
			return false;
		}
		if (eventsToo) {
			return sm.waitForProver(timeoutMillis);
		}
		return true;
	}



//end OLDCODE
	//------ a simple example for testing and demoing how it works

	@SuppressWarnings("null")
    public static void main(String args[]) {
		ProverStandalone prover = null;
		final Object ownProverLock = new Object();
		try {
//			ProofGeneralPlugin.LOG_EVENTS_RUNNING = true;
//			ProofGeneralPlugin.LOG_EVENTS_FIRING = true;
//			ProofGeneralPlugin.LOG_PROVER_IO = true;

			log("STARTING PROVER");
			prover = ProverStandalone.createIsabelleWithLogic("HOL");
			log("Prover started.");
			System.out.println();

			log("Opening theory Test:");
		  ProverCommandResponse cmd = prover.sendCommand("theory Test = Main:", true);
		  log(cmd.firstIncoming.parseTree.getStringValue());
		  System.out.println();

			boolean doCommandsPublicly = true;  //whether our commands are communicated to other listeners (eg proverState) -- must be true to undo
			//it's a good idea to "own" the prover if you're doing a batch of commands
			//this informs anyone else that they shouldn't send commands
//			if (!prover.sm.proverOwner.tryGetProverOwnership(ownProverLock)) {
//				log("COULDN'T GET PROVER OWNERSHIP (continuing anyway)");
//				System.out.println();
//			}

			//do a complete theorem
			sendWithDetails("lemma a_b_sym: \"a = b ==> b = a\"", prover, doCommandsPublicly);
			sendWithDetails("apply (rule sym)", prover, doCommandsPublicly);
			sendWithDetails("apply assumption", prover, doCommandsPublicly);
			//and a way of peeking on the prover state (only available for public commands)
			//TODO we should provide a way to get this fresh/definitively, ideally in predicate form
			log("FOR REFERENCE, PROVER STATE IS:  "+
					(prover.sm.getProverState().getLastResponse()!=null ?
							prover.sm.getProverState().getLastResponse().asXML() : "null"));
			sendWithDetails("done", prover, doCommandsPublicly);
			System.out.println();

			//show what the knowledge is immediately (it gets set from the response)
			KnowledgeItem ki = prover.sm.getProverState().getProverKnowledge().getItem("a_b_sym");
			if (ki!=null) {
				log("KNOWLEDGE (poor man's): "+ki.getId()+", "+ki.getStatementHtml());
			} else {
				log("NO KNOWLEDGE ABOUT \"a_b_sym\"");
			}

			//now release prover ownership
			log("Releasing prover ownership, to load knowledge fully.  We are done anyway.");
//			try {
//				prover.sm.proverOwner.releaseOwnership(ownProverLock);
//			} catch (ProverOwner.ProverOwnedBySomeoneElseException e) {
//				log("SOMEONE ELSE OWNS THE PROVER! WHO CALLED "+e.owner+" ?  (continuing anyway)");
//			}

			//let ProverKnowledge load full knowledge in the background
			if (ki instanceof ProverKnowledge.LazyTheoryItem) {
				//ProverKnowledge will load a_b_sym definitively & prettified by querying the prover.
				// this is advanced code to show how the callback approach works--
				// it might be nice to have that functionality here,
				// eg on ProverStandalone.queueCommand to get called when the command is done.
				// note how ProverKnowledge (and GetCommandResponseAction)
				// also respond to thread and command interruptions.
				synchronized (ownProverLock) {
					//this command is queued by
					if (((ProverKnowledge.LazyTheoryItem)ki).loadFullyBg(3, new Runnable() {
						public void run() {
							synchronized (ownProverLock) {
								log("Knowledge is loaded.");
								ownProverLock.notifyAll();
							}
						}
					})!=-1) {
						//wait on it, unless prover knowledge is already loaded
						  ownProverLock.wait(2000); // FIXME Wa?
					}
				}
			}
			ki = prover.sm.getProverState().getProverKnowledge().getItem("a_b_sym");
			if (ki!=null) {
				log("KNOWLEDGE (pretty): "+ki.getId()+", "+ki.getStatementHtml());
			} else {
				log("NO KNOWLEDGE ABOUT \"a_b_sym\"");
			}
			System.out.println();

			//undo that theorem -- should NOT own prover when running this
			log("UNDO");
			
			// FIXME da: this now needs to call sopmething else CLEANUP
			// prover.sm.undoLastScriptCommand(null, ProverStandalone.class);
			
			prover.waitForAvailable(true);  //have to wait for 'undo' to complete
			//verify it was undone, by showing no knowledge
			ki = prover.sm.getProverState().getProverKnowledge().getItem("a_b_sym");
			if (ki!=null) {
				log("KNOWLEDGE (undone, SHOULDN'T BE AVAILABLE!!): "+ki.getId()+", "+ki.getStatementHtml());
			} else {
				log("NO KNOWLEDGE ABOUT \"a_b_sym\" (as desired)");
			}
			log("Just to be sure it was undone... (should get an error)");
			//we don't own the prover here; not very nice, but it doesn't cause problems
			sendWithDetails("ML {* thm \"a_b_sym\" *}", prover, doCommandsPublicly);
			System.out.println();

//		} catch (ScriptingException e) {
//			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ProverRegistryException e) {
			e.printStackTrace();
		} finally {
			log("WAITING FOR ALL CLEAR.");
			try {
			  prover.waitForAvailable(true);   //let anything pending complete -- must not own prover here either
			} catch (InterruptedException e) {}
			//and shutdown the prover
			log("SHUTTING DOWN.");
			prover.dispose();
			System.out.println();
			log("ALL DONE. Hope you enjoyed the show.");
		}
		//no need to do System.exit(), because we clean up all threads!
	}

	/** prints the message with current time to stdout */
	private static void log(String message) {
		System.out.println(NumericStringUtils.makeDateString()+"  "+message);
	}

	private static String sendWithDetails(String command, ProverStandalone prover, boolean log) {
		log("SENDING: "+command);
		try {
			ProverCommandResponse cmd = prover.sendCommand(command, log);
			if (cmd.errorEvent==null) {
				if (cmd.firstIncoming==null) {
					log("    GOT:    (command processed without response)");
					return "";
				}
				log("    GOT:    "+cmd.firstIncoming.parseTree.getStringValue());
				return cmd.firstIncoming.parseTree.getStringValue();
			}
			log("  ERROR:    "+cmd.errorEvent.parseTree.getStringValue());
		} catch (InterruptedException e) {
			log("  ERROR:    (interrupted)");
//		} catch (ScriptingException e) {
//			log("  ERROR:    "+e);
		}
		return null;
	}


}
