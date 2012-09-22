/*
 *  $RCSfile: SessionManager.java,v $
 *
 *  Created on 16 Apr 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.sessionmanager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.Socket;
import java.net.URI;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.VisitorSupport;
import org.dom4j.io.SAXReader;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.PreferencesUtil;

import ed.inf.proofgeneral.Constants;
import ed.inf.proofgeneral.NotNull;
import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.ProofGeneralProverRegistry;
import ed.inf.proofgeneral.ProofGeneralProverRegistry.Prover;
import ed.inf.proofgeneral.actions.SetPrefAction;
import ed.inf.proofgeneral.actions.WaitReadyAction;
import ed.inf.proofgeneral.console.DebugConsole;
import ed.inf.proofgeneral.console.PGIPLogConsole;
import ed.inf.proofgeneral.console.TracingConsole;
import ed.inf.proofgeneral.document.CmdElement;
import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.document.ProofScriptDocumentException;
import ed.inf.proofgeneral.document.ProofScriptMarkers;
import ed.inf.proofgeneral.editor.PGTextHover;
import ed.inf.proofgeneral.editor.lazyparser.ExternalLazyParser;
import ed.inf.proofgeneral.editor.lazyparser.MixedParser;
import ed.inf.proofgeneral.editor.lazyparser.PGIPParseResult;
import ed.inf.proofgeneral.editor.lazyparser.Parser;
import ed.inf.proofgeneral.editor.lazyparser.Parser.UnparseableException;
import ed.inf.proofgeneral.pgip.Fatality;
import ed.inf.proofgeneral.pgip.Location;
import ed.inf.proofgeneral.pgip2html.Converter;
import ed.inf.proofgeneral.preferences.PreferenceNames;
import ed.inf.proofgeneral.preferences.ProverPreferenceNames;
import ed.inf.proofgeneral.sessionmanager.CommandProcessor.InternalScriptingException;
import ed.inf.proofgeneral.sessionmanager.PGIPEventQueue.FirePGIPEventThread;
import ed.inf.proofgeneral.sessionmanager.ProverOwner.ProverOwnedBySomeoneElseException;
import ed.inf.proofgeneral.sessionmanager.events.CommandCausedErrorEvent;
import ed.inf.proofgeneral.sessionmanager.events.CommandProcessed;
import ed.inf.proofgeneral.sessionmanager.events.CommandSucceeded;
import ed.inf.proofgeneral.sessionmanager.events.IPGIPListener;
import ed.inf.proofgeneral.sessionmanager.events.PGIPError;
import ed.inf.proofgeneral.sessionmanager.events.PGIPEvent;
import ed.inf.proofgeneral.sessionmanager.events.PGIPIncoming;
import ed.inf.proofgeneral.sessionmanager.events.PGIPReady;
import ed.inf.utils.datastruct.MutableObject;
import ed.inf.utils.datastruct.NumericStringUtils;
import ed.inf.utils.datastruct.StringManipulation;
import ed.inf.utils.eclipse.EclipseMethods;
import ed.inf.utils.eclipse.ErrorUI;
import ed.inf.utils.file.PathUtils;
import ed.inf.utils.process.PooledRunnable;
import ed.inf.utils.process.ProcessUtils;
import ed.inf.utils.process.ThreadPool;

// TODO da:
// We need to sort out some of the responsibilities here, and clean up the abstractions,
// removing unnecessary associations and references which can be static
// (e.g. PGIP syntax).
// Also we need to fix the PGIP model handling (file level was badly broken).
// To be more careful with concurrency we should take locks on state changes
// in the key data types (e.g. prover model, prover state, active script).
// da: I'm doing some cleanups on this currently, but won't manage everything.

// Plan:
//  1. Get things working
//  2. Enforce encapsulation here: script management should take place
//     through the SM, but no UI here.  If some operation fails, raise
//     ScriptingException and have that handled elsewhere.
/**
 * This class is responsible for managing a session with a theorem prover.  It will:
 * <ul>
 * <li>start, maintain & close process based sessions with a command line prover</li>
 * <li>pass commands to the prover</li>
 * <li>receive responses from the prover & fire events (to which listeners can be attached)</li>
 * <li>maintain a command history & message log</li>
 * <li>maintain lists of currently associated documents and locked files</li>
 * </ul>
 * Session managers must be given the details of the prover they will talk to.
 * @author Daniel Winterstein
 */
public class SessionManager implements IPGIPListener,IPropertyChangeListener {

//  -----------------------------------------------------------------------------
//  State
//  -----------------------------------------------------------------------------

	/** The prover we're connected to */
	@NotNull
	private final ProofGeneralProverRegistry.Prover prover;
	
	/** Information about the prover we're connected to (including syntax, etc) */
	@NotNull
	public final ProverInfo proverInfo;  // FIXME da: should be private

	/** A stylesheet output converter for this session manager  */
	@NotNull
	private final Converter converter;  

	/** Queue of commands being sent to prover */
	@NotNull
	private final CommandQueue commandQueue;

	/** Queue of events generated by this session manager */
	@NotNull
	private final PGIPEventQueue eventQueue;

	
	@NotNull
	public final PGTextHover hoverHelp;  // FIXME da: to be removed, no UI here, this is never
                                   // accessed here.  May need a place holder for session
	                               // dependent state, though (separate class).

	/** Documents whose scripting state we are tracking */
	@NotNull
	private final ControlledDocuments controlledDocuments;

	/** Lock taken by some actions */
	@NotNull
	private final ProverOwner proverOwner;  // FIXME da: to remove 
	
	/** The parser for this session */
	@NotNull
	private final Parser parser;


	/** The document that is currently active for script management, if any. */
	private ProofScriptDocument activeScript;
	

//  -----------------------------------------------------------------------------
//  Constructors/destructors
//  -----------------------------------------------------------------------------

	/**
	 * Initialises a session with the session manager.
	 */
	public SessionManager(Prover prover) {
		// TODO: desirable refactorings here:
		//  - remove dependencies on whole of sm in queue, etc, just use parts needed
		//  - combine proverInfo and prover
		//  - make sure symbols being used in document is set to be same as prover
		//  - qupdater: this is comm point between events/queues, ideally could be only one
		this.prover = prover;
		converter = new Converter(prover);
		proverInfo = new ProverInfo(prover);
		proverState = new ProverState(converter, proverInfo.syntax);
		eventQueue = new PGIPEventQueue(this);
		commandQueue = new CommandQueue(this,eventQueue,prover.getSymbols());
		proverOwner = new ProverOwner(this);
		controlledDocuments = new ControlledDocuments();
		if (ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_ENABLE_SCRIPTING)) {
			parser = new MixedParser(this);
		} else {
			parser = new ExternalLazyParser(this);
		}
		hoverHelp = new PGTextHover(proverInfo,proverState.getProverKnowledge());
		qUpdater = new QueueUpdater();

		// register for preference change events
		ProofGeneralPlugin.getStaticPreferenceStore().addPropertyChangeListener(this);
		// listen to its own events
		addListener(this);
		synchronized (allSessionManagerListeners) {
			for (IPGIPListener li : allSessionManagerListeners) {
				addListener(li);
			}
		}
		// set up command processing
		CommandProcessor.getDefault().setSessionManager(this);
		// Now we can load the syntax (this goes through the command processor)
		try {
			proverInfo.proverSyntax.loadSyntax(this,prover.getSymbols());
		} catch (InternalScriptingException e1) {
			if (ProofGeneralPlugin.isEclipseMode()) {
				e1.printStackTrace();
				//outwith eclipse don't fuss with stack trace
			}
		}
		try {
			startSession();
		} catch (Exception e) {
			//			e.printStackTrace();
			firePGIPEvent(new PGIPError(StringManipulation.convertLineBreak("Could not start a scripting session!")
					+ e.getLocalizedMessage() ));
		}
	}

//  -----------------------------------------------------------------------------
//  Visible interface to controlled documents
//  -----------------------------------------------------------------------------

	public void controlDocument(ProofScriptDocument doc) {
		controlledDocuments.controlDocument(doc);
	}

	public ProofScriptDocument findDocumentOn(IResource res) {
		return controlledDocuments.findDocumentOn(res);
	}
	public boolean isLocked(URI uri) {
		return controlledDocuments.isLocked(uri);
	}

	public void lock(ProofScriptDocument doc) {
		controlledDocuments.lock(doc);
	}

	public void lock(URI uri, boolean complete) {
		controlledDocuments.lock(uri,complete);
	}

	public void unlock(URI uri, boolean complete) {
		controlledDocuments.unlock(uri,complete);
	}

//  -----------------------------------------------------------------------------
//  Exported interface to command queue
//  -----------------------------------------------------------------------------

	/**
	 * @param command
	 */
	public boolean queueCommand(CmdElement command) 
	//throws ScriptingException 
	{
		return commandQueue.queueCommand(command);
	}

	/**
	 * @param command
	 * @param cause
	 */
	public boolean queueCommand(CmdElement command, Object cause) 
	//throws ScriptingException 
	{
		return commandQueue.queueCommand(command, cause);
	}

	/**
	 * Send the given command to the prover, adding it to the queue and sending it if
	 * the prover isn't busy.
	 * @param command
	 * @return true if the command has been sent, false if it is queued but not sent.
	 */
	public boolean queueCommand(CommandQueued command) {
		return commandQueue.queueCommand(command);
	}

	/**
	 * @param command
	 * @param cause
	 * @param backgroundThread
	 * @param privateListener
	 */
	public void queueCommand(CmdElement command, Object cause, Boolean backgroundThread, MutableObject privateListener)
//		throws ScriptingException 
		{
		commandQueue.queueCommand(command, cause, backgroundThread, privateListener);

	}

	/**
     * 
     */
    public void commandQueueNotify() {
    	// TODO Auto-generated method stub
	    
    }
    
	/**
	 * Wait on the command queue.  Not recommended to use.
	 * @param timeoutMillis
	 * @param endTime
	 * @return true if the prover is available or dead, otherwise we block and wait
	 */
    // TODO: da: this was moved from ProverInterface.  It needs review.
   	public boolean commandQueueWait(long timeoutMillis, long endTime) {
		synchronized (commandQueue) {
			try {
				while (proverState.isAlive() && (proverState.isBusy())) {
					if (timeoutMillis==0) {
						return false;
					}
					if (timeoutMillis<0) {
						// FIXME: MWN?
						commandQueue.wait(500);					//we don't get notify so loop instead
					} else {
						long ms = endTime - System.currentTimeMillis();
						if (ms<=0) {
							return false;
						}
						// FIXME: MWN?
						commandQueue.wait(Math.min(ms, 500));  //we don't get notify so loop instead
					}
				}
			} catch (InterruptedException e) {
				return false;
			}
		}
		return true;
	}
   	
   	public void setCommandQueued(CommandQueued cq, int currentSeqnNo) { 
   		commandQueue.activeCommandLookup.put(Integer.valueOf(currentSeqnNo), cq);
   	}
	
   	
	/**
	 * Send commands to process up to the given point
	 * (but not necessarily including the character at that offset)
	 * The given document must already be set
	 * active (not yet done here because may need UI interaction).  
	 * 
	 * @param doc
	 * @param offset
	 */
	public void queueCommandsToProcess(ProofScriptDocument doc, int offset) throws ScriptingException {
		// TODO: ought to take a lock on the document here
		synchronized (activeScript) {
			assert activeScript == doc : "Document must be made active first";
			synchronized (commandQueue) {
				if (activeScript.canQueueForwards()) {
					try {
						List<CmdElement> cmds = doc.lockAndGetCommandsUpto(offset);
//System.out.println("AH: getting commands up to "+offset+"; got "+cmds.size()+" commands");						
						for (CmdElement cmd: cmds) {
//System.out.println("  got "+cmd.getPosition().offset+"+="+cmd.getPosition().length+" as '"+cmd.getText()+"'");
							queueCommand(new CommandQueued(this, cmd, null));
						}
					} catch (ProofScriptDocumentException e) {
						throw new ScriptingException(e.getMessage());
					}
				} else {
					// This is the case where we could attempt to edit the queue, but let's not do that 
					// until we're completely robust in the simpler situation.
					throw new ScriptingException("Cannot queue commands: document is busy in other direction");
				}
			}
		}
	}
	

   	/**
   	 * Send commands to undo back to the given point.  The given document must already be set
   	 * active (not yet done here because may need UI interaction).
   	 * 
   	 * @param doc
   	 * @param offset position which should be unprocessed when undo has finished
   	 * (special case, -1 means to undo everything)
   	 */
   	public void queueCommandsToUndo(ProofScriptDocument doc, int offset) throws ScriptingException {
   		// TODO: ought to take a lock on the document here
		synchronized (activeScript) {
			assert activeScript == doc : "Document must be made active first";
				synchronized (commandQueue) {
   					if (activeScript.canQueueBackwards()) {
   						try {
   							List<CmdElement> cmds = doc.unlockAndGetCommandsBackTo(offset);
   	   						for (CmdElement cmd: cmds) {
   	   							queueCommand(new CommandQueued(this, cmd, null));
   	   						}
   						} catch (ProofScriptDocumentException e) {
   							throw new ScriptingException(e.getMessage());
   						}
   					} else {
   						// This is the case where we could attempt to edit the queue, but let's not do that 
   						// until we're completely robust in the simpler situation.
   						throw new ScriptingException("Cannot queue commands: document is busy in other direction");
   					}
				}
		}
   	}
   	
	
//  -----------------------------------------------------------------------------
//  Exported interface to prover ownership
//  -----------------------------------------------------------------------------

	/**
	 * @return true if the prover is owned
	 */
	public boolean isOwned() {
		return proverOwner.isOwned();
	}

//  -----------------------------------------------------------------------------
//  Exported interface to event queue
//  -----------------------------------------------------------------------------


	public boolean waitForProver(long ms) throws InterruptedException {
		return eventQueue.waitForProver(ms);
	}
	
	/**
	 * @return the object used to synchronize event firing
	 */
	public Object getQueueSyncObject() {
		return eventQueue.firingSequence;
	}
	
	/**
	 * Relay an event fire to the event queue.
	 */
	private void firePGIPEvent(PGIPEvent event) {
		eventQueue.firePGIPEvent(event);
	}
	
	public IPGIPListener getEventQueuePrivateListener() {
		return eventQueue.privateListener;
	}
	
	public void setEventQueuePrivateLister(IPGIPListener listener) {
		eventQueue.privateListener = listener;
	}


//  -----------------------------------------------------------------------------
//  Prover process
//  -----------------------------------------------------------------------------

	/** Process for prover communication.  Only use on setup/teardown. */
	private Process process = null;

	/** Socket for prover communication.  Only used on setup/teardown. */
	private Socket socket = null;

	/** Writes to the prover. */
	protected BufferedWriter writer = null;

	/** Receives output from the prover. */
	protected StreamGobbler tpOutputGobbler = null;

	/** Receives error output from the prover */
	protected StreamGobbler tpErrorGobbler = null;

	/** State of the prover.  */
	@NotNull
	private final ProverState proverState;

//  -----------------------------------------------------------------------------
//  Session management methods
//  -----------------------------------------------------------------------------

	/**
	 * Reset scripting state for this session manager.
	 */
	public void resetSessionState() {
        eventQueue.clear();
        proverOwner.clear();
        commandQueue.clear();
	}

	/**
	 * Initiate a session (opening sockets, launching commands, etc. as necc)
	 * @throws IOException
	 */
	public void startSession() throws IOException, ScriptingException {
		int to = ProofGeneralPlugin.getIntegerPref(PreferenceNames.PREF_TIME_OUT);
		if (to>0) {
			setTimeout(to,true);
		}
		//		backgroundThread = false;    //changed so this isn't set here... should be set by default, and if changed, changer should revert this back (need true for standalone) -AH
//		logging = true;

		resetSessionState();

		startUpBumpfFlag = true;   // ignore initial non-xml output
        proverState.setBusy(true); // prover busy on start up until we get a ready message back

		if (proverInfo.getBoolean(ProverPreferenceNames.PREF_USE_SOCKETS)) {
			String hostName = proverInfo.getString(ProverPreferenceNames.PREF_HOST_ADDRESS);
			int port = Integer.parseInt( proverInfo.getString(ProverPreferenceNames.PREF_PORT));
			startSocketSession(hostName,port);
		} else {
			startProcessSession();
		}

		// da: make sure the state is clean (probably not necessary, if this
		// is successfully done in stopSession and init).
		resetScriptingState();

		// da: only change CWD on activating scripting.  Actually, now
		// only by prover automatically on <openfile>.  So changecwd may not be needed.
		// setProverCWD(new Path(proverInfo.getProverStartDir().toString()));

		// da: when is right moment for this and who actually uses it?
		// TODO: rationalise events [NB: Ahsan has improved/simplified events in Uni-Bremen-Ahsan1]
		// da: do we really need this?  We could/should instead use <usespgip> response
		// from prover.
		// da: I can't see that this is used, should test without
		// firePGIPEvent(new PGIPEvent("Scripting session active.",null));

		try {
			CmdElement askpgip = new CmdElement(PGIPSyntax.ASKPGIP);
			commandQueue.queueCommand(askpgip);
			// FIXME: this is doubled because the first command seems to be lost
			// commandQueue.queueCommand(askpgip);  
	    } catch (Exception e) {
	    	throw new ScriptingException(e.getMessage());
	    }

	    sendStartupScript(); // CLEANUP: we can probably lose this

        setOutputSymbolSupport();
        
        // FIXME: just for testing sake
        setProverFlag(PGIPSyntax.PROVERFLAG_THMDEPS,true);

		try {
			setTPPrefs();
		} catch (ScriptingException x) {
			ErrorUI.getDefault().signalWarning(x);
		}
	}

	/**
	 * Set output symbol support in the prover according to the preference setting.
	 */
	private void setOutputSymbolSupport() 
	//throws ScriptingException 
	{
		// TODO: add a listener for the change of preferences and adjust in
		// the prover appropriately.
        boolean outputSymbols = 
        	ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_OUTPUT_SYMBOL_SUPPORT);
        setProverFlag(PGIPSyntax.PROVERFLAG_SYMBOLS,outputSymbols);
	}

	public void startQuiet() 
	//throws ScriptingException 
	{
		setProverFlag(PGIPSyntax.PROVERFLAG_QUIET, true);
	}
	
	public void stopQuiet() 
	//throws ScriptingException 
	{
		setProverFlag(PGIPSyntax.PROVERFLAG_QUIET, false);
	}
	
	private void setProverFlag(String flagname, boolean value) 
	//throws ScriptingException 
	{
        CmdElement setflag = new CmdElement(PGIPSyntax.SETPROVERFLAG);
		setflag.addAttribute("flagname", flagname);
		setflag.addAttribute("value", value ? "true" : "false");
		commandQueue.queueCommand(setflag);
	}
	/**
	 * Send startup script, if any.
	 */
	// da: FIXME: this could be a single command, using the PGIP load message
	// to batch-interpret.
	private void sendStartupScript() 
	//throws ScriptingException 
	{
		String STARTUP_SCRIPT  = proverInfo.getString("Startup Script");
		if (STARTUP_SCRIPT != null && !STARTUP_SCRIPT.equals("")) {
			String[] ss = STARTUP_SCRIPT.split(StringManipulation.LINEEND);
			for(int i=0; i<ss.length; i++) {
				CmdElement command = new CmdElement(PGIPSyntax.SPURIOUSCOMMAND);
				command.setText(ss[i]);
				commandQueue.queueCommand(command);
			}
		}
	}

	/**
	 * Clear the state maintained while scripting (not the process handling state).
	 * This is for initialisation or abrupt termination, not a tidy reset which
	 * co-ordinates with the prover.
	 */
	private void resetScriptingState() {
		if (ProofGeneralPlugin.isShuttingDown()) {
			// short-cut; avoids errors with getting preferences during shutdown
			return; 
		}
		incompleteOutput = new StringBuffer("");
		activeScript = null;             // rude clear, no prover interaction
        controlledDocuments.unlockAll(); // ditto
        proverState.reset();
        proverCWD = null;
//        clearHistory();
	}

	/**
	 * Send non-default config prefs out to the prover
	 */
	protected void setTPPrefs() throws ScriptingException {
		try {
			Preferences pStore = ProofGeneralPlugin.getProverPlugin(proverInfo.name).getPluginPreferences();
			for(Iterator i = proverInfo.getConfigPrefs().iterator(); i.hasNext();) {
				String pName = (String)i.next();
				Object value = pStore.getString(pName);
				if (value.equals(pStore.getDefaultString(pName))) {
					continue;
				}
				SetPrefAction setAction = new SetPrefAction(this,pName,value);
				setAction.run();
			}
		} catch (Exception e) {
			throw new ScriptingException(e.getMessage());
		}
	}

	public final static int PROVER_STARTUP_TIMEOUT = 15;  // seconds
	
	/**
	 * Launches the prover by executing the specified command externally.
	 * Captures its input and output(s) into the relevant {@link StreamGobbler} objects.
	 */
	// TODO da: for PGIP process class
	protected void startProcessSession () {
		try {
			final String err_no_response = "The prover failed to send a ready message after "+ 
			PROVER_STARTUP_TIMEOUT + " seconds. "+
			"The prover start command is probably wrong. "+
			"Try editing them in the Proof General preferences and restarting the prover. "+
			"(e.g., for isabelle, make sure to pass the -I and -X options). "+
			"(It is unlikely that any commands will work until you fix this problem.)";

			// da: TODO: remove this?  Maybe nobody really needs this event.
			//firePGIPEvent(new PGIPEvent(this,"Attempting to start a scripting session.",null));

			// URGENT removed the next, does it really send a message????
			//firePGIPEvent(new PGIPEvent("<pgip><askpgip/></pgip>",null));

			if (ProofGeneralPlugin.isEclipseMode()) {
				WaitReadyAction.getDefault().start("Prover Not Started", err_no_response, 
						PROVER_STARTUP_TIMEOUT*1000, this);
			}

			// check that the launch command is valid; avoid spurious exceptions if not.

			String proverCommand = proverInfo.getLaunchCommand().split(" ")[0];
			// DEBUG - TODO - a debug / log framework so that we can hide this sort of output
			String proverCommandLocation = PathUtils.which(proverCommand);
			System.out.println("Searching for prover ("+proverCommand+"): "+ 
					(proverCommandLocation != null ? proverCommandLocation:
						"command \"" + proverCommand + "\" was not found."));

			if ( proverCommand != null && !proverCommand.equals("") &&
					PathUtils.fileExists(proverCommand) ) {

				synchronized (this) {
					process = Runtime.getRuntime().exec(proverInfo.getLaunchCommand(), null, null);
				}
				BufferedReader reader = new BufferedReader(new InputStreamReader(
						process.getInputStream()));

				tpOutputGobbler = new StreamGobbler("OutputStream", reader, this, true);

				reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				tpErrorGobbler = new StreamGobbler("ErrorStream", reader, this, false);

				writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

			} else {
				final String e = "Cannot find (or access) prover.";
				System.err.println("Error: "+e);
				firePGIPEvent(new PGIPError("Could not start session: "+e));
				
//				// launch error feedback in a new thread because we may be invoked here 
//				// in a context which has a lock on the session manager
//				// (and deadlock with a display thread which then locks the session manager)
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						String pid = Constants.systemPreferencesPageIdFor(prover.getName());

						EclipseMethods.messageDialog("Error",
								e+"\n Please check settings in the preference dialog which follows.",
								new String[] { "OK" });

						PreferenceDialog pd = PreferencesUtil.createPreferenceDialogOn(
								null,pid,new String[]{pid},null);
						pd.setMessage(e); // TODO make this work!

						WaitReadyAction.getDefault().stop();
						pd.open();
						if (ProofGeneralPlugin.isEclipseMode()) {
							WaitReadyAction.getDefault().start("Prover Not Started", err_no_response, 20*1000, SessionManager.this);
						}
					}
				});
			}

		} catch (IOException e) {
			e.printStackTrace();
			firePGIPEvent(new PGIPError("Could not start session: "+e.getMessage() ));
		}
	}

	/**
	 * Create a socket to a remote (already running) prover process.
	 * Hooks the new socket up to the reader and writer.
	 * @param hostName
	 * @param port
	 * @throws IOException
	 */
	// TODO da: for PGIP process class
	protected void startSocketSession(String hostName,int port) throws IOException {
		// da: again another spurious event, probably something for logging rather than event
		//firePGIPEvent(new PGIPEvent("Attempting to connect to remote system: " +
		//		hostName+":"+port, null));
		socket = new Socket(hostName,port);
		writer = new BufferedWriter(new OutputStreamWriter(
				socket.getOutputStream() ));
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				socket.getInputStream()
		));
		tpOutputGobbler = new StreamGobbler("Socket-Output", reader, this, true);
		//error gobbler probably not needed here, as comes from socket, no separate error stream --AH
	}

	protected PGIPLogConsole consoleoutput;

	public void connectConsole(PGIPLogConsole console) {
		this.consoleoutput = console;
	}

	protected TracingConsole tracingoutput;

	public void connectTracingConsole(TracingConsole console) {
		this.tracingoutput = console;
	}

	protected DebugConsole debugoutput;

	public void connectDebugConsole(DebugConsole console) {
		this.debugoutput = console;
	}

	private void clearConsoles() {
		if (debugoutput != null) {
			debugoutput.clearConsole();
		}
		if (tracingoutput != null) {
			tracingoutput.clearConsole();
		}
		if (consoleoutput != null) {
			consoleoutput.clearConsole();
		}
	}

	public int SHUTDOWN_TIMEOUT = 5000;

	/**
	 * Exit the theorem prover if its running, and clean up.
	 * @param cause the action causing this; can be null
	 * @param systemexit - true if we're exiting the application.
	 */
	// TODO da: split into session/process responsibilities, also try to be careful that
    // this works with concurrent threads, and does shutdown in right order.
	public synchronized void stopSession (Object cause,boolean systemexit) {
		Process oldProc = process;
		
		// Consider the prover dead already
		proverState.setAlive(false);
		
		process = null; // to prevent stream closure from giving errors

		commandQueue.killQueue("prover stopped");
		if (oldProc!=null) {
			try {	// to exit the prover politely (might not bother on system exit)
				
				//TODO first check not busy, if it is we should interrupt it
				CmdElement stop = new CmdElement(PGIPSyntax.PROVEREXIT);
				synchronized (eventQueue.firingSequence) {
					eventQueue.privateListener = null;  //clear this
					commandQueue.sendCommand(new CommandQueued(this, stop, cause));
				}

				// Doesn't get a response, so wait for theorem prover -AH
				long time = System.currentTimeMillis();
				int exitCode = -256;  //marker that we have not exitted yet
				while (System.currentTimeMillis()-time<SHUTDOWN_TIMEOUT && exitCode==-256) {
					// Wait up to one second for prover to exit
					try {
						exitCode = oldProc.exitValue();
					} catch (Exception e) {
						Thread.sleep(100);
					}
				}
				//System.out.println("prover exited after "+(System.currentTimeMillis()-time)+"ms with exit code "+exitCode);
				if (exitCode!=0) {
					if (exitCode==-256) System.err.println("Prover did not exit after "+NumericStringUtils.makeTimeString(SHUTDOWN_TIMEOUT)+
					"; destroying process.");
					//else System.err.println("Prover exited with exit code "+exitCode);
				}
			} catch (Exception e) {e.printStackTrace();}
		} 
		
		try {if (writer != null) { writer.close(); writer=null;} } catch (IOException e) {/*e.printStackTrace();*/}
		try {if (tpOutputGobbler != null) { tpOutputGobbler.interrupt(); } } catch (Exception e) {/*e.printStackTrace();*/}  //changed stop to interrupt, since stop is deprecated
		try {if (tpErrorGobbler != null) tpErrorGobbler.interrupt(); } catch (Exception e) {/*e.printStackTrace();*/} finally { tpErrorGobbler = null; }  //--AH
		try {if (process != null) process.destroy(); process=null;} catch (Exception e) {/*e.printStackTrace();*/}
		try {if (oldProc != null) oldProc.destroy();} catch (Exception e) {/*e.printStackTrace();*/}
		try {if (socket != null) socket.close(); socket=null;} catch (Exception e) {/*e.printStackTrace();*/}
		if (!systemexit) {
			// Unlocking documents changes parses, etc.  Don't bother if we're exiting.
			try {controlledDocuments.unlockAll();} catch (Exception e) {/*e.printStackTrace();*/}
		}
		proverOwner.clear();
		ThreadPool.get().closeAll();  // da: conc'y problems seem likely here wrt state changes above
		clearConsoles();
		resetScriptingState();
	}

	final Object shutdownLock = new Object();
	/** stops the session, but in the background; used on shutdown so UI tasks return quickly (we delay the indicated time
	 * if it is >0, forever if it is <0, and none at all if =0) */
	public void stopSessionBg (final Object cause, long delay) {
		try {controlledDocuments.unlockAll();} catch (Exception e) {e.printStackTrace();}   //do this here to prevent display.async errors later
		synchronized (shutdownLock) {
			new PooledRunnable() {
				public void run() {
					stopSession(cause,true);
					synchronized (shutdownLock) {
						shutdownLock.notifyAll();
					}
				}
			}.start();
			try {
				if (delay>0) shutdownLock.wait(delay);  //wait delay  FIXME Wa?
				else if (delay<0) {
					shutdownLock.wait();
				}
			} catch (InterruptedException e) {}
		}
	}


	// this sends the next command when PGIPCmdProc is processed
	QueueUpdater qUpdater = null;

	/** extra listener to send the next item from the queue */
	class QueueUpdater implements IPGIPListener {
		public void pgipEvent(PGIPEvent event) {
			if (event instanceof CommandProcessed) {
				commandQueue.sendNextQueuedCommand();
			}
		}
	}

	/**
	 * Is it daft to listen to our own events?
	 This method does things like generating ancillary events

	 Implements an aspect of the undo behaviour:
	 ASSUMPTION: commands that cause errors (other than nonfatal errors) do not have any effects.
	 They therefore do not need to be undone.
	 */
	// FIXME da: events need rationalising
	public void pgipEvent(PGIPEvent e) {
		if (e instanceof PGIPIncoming) {
			boolean finished = handleLocalIncoming(e);
			if (finished) {
				return;
			}
		}
		try {
			proverState.modelEvent(e,this); // FIXME: temporarily pass in this ref til move active script out
		} catch (ScriptingException ex) {
			if (ProofGeneralPlugin.getDefault()!=null) {
				ErrorUI.getDefault().signalError(ex);
			}
			ex.printStackTrace();
		}
	}

	/**
	 * Handle "local" incoming PGIP events, i.e. ones which are really
     * dealt with by the interface.  
	 * @param e
	 * @returns true if we've dealt with this event, false otherwise.
	 */
	private boolean handleLocalIncoming(PGIPEvent e) {
		// handle PGIP-related handshake traffic that needn't go outside the session manager

		// TODO: next two cases should be handled in visitor loop below.
		// We're forgiving of more than one pgip element in a packet (and also
		// allow comments, etc).
		
		if (e.parseTree.selectSingleNode("//"+PGIPSyntax.ASKPGIP) != null) {
			CmdElement response = new CmdElement(PGIPSyntax.USESPGIP);
			try {
				response.addAttribute("version",Constants.PGIP_VERSION_SUPPORTED);
				commandQueue.queueCommand(response);
			} catch (Exception x) {
				x.printStackTrace();
			}
			return true;
		}
		if (e.parseTree.selectSingleNode("//"+"knownprover") != null) {
			// HACK: this is all rather ugly
			Element kp = (Element) e.parseTree.selectSingleNode("//"+"knownprover");
			CmdElement response = new CmdElement("launchprover");
			try {
				response.addAttribute("componentid",kp.attributeValue("componentid"));
				commandQueue.queueCommand(response);
			} catch (Exception x) {x.printStackTrace();}
			return true;
		}
		// da: new case, but I would rather this was handled more simply when command
		// is sent, not via this self-listener.  For that to work we need to remove
		// all the concurrency mess so things happen in right order.
		if (e.type != null && proverInfo.syntax.subType(e.type, PGIPSyntax.SPURIOUSTYPE)) {
			// Spurious commands are ones which are not sent to prover, but dealt with as
			// if they have been.  They are generated from a document.  (Typically: do on
			// whitespace/comments, and all undo commands).  Used in new doc-based undo.
			assert e.parseTree instanceof CmdElement : "Spurious command has wrong type";
			CmdElement cmd = (CmdElement)e.parseTree;
		    // da: Two more events: this has succeeded and has been processed.  You'd imagine
		    // a listener in proverState would respond to these but in fact proverState
	        // is called directly, so these events are  purpose of these events isn't unclear.
		    PGIPEvent cs = new CommandSucceeded(cmd, e.cause);
		    PGIPEvent cp = new CommandProcessed(cmd, e.cause);
// would be more efficient to process these immediately, maybe.    
		    eventQueue.firePGIPEvent(cs);
		    eventQueue.firePGIPEvent(cp);
			return true;
		}
		// CLEANUP: da: the next case should be impossible!
//		if (!e.parseTree.getName().equals("pgip")) {
//			System.err.println("Unusual XML message received, not in PGIP packet: "+e.parseTree.asXML());
//			return;
//		}
		// Look at embedded script commands
		e.parseTree.accept(new VisitorSupport() {
			@Override
            public void visit(Element innerE) {
				// was: Element innerE = (Element) e.parseTree.node(0);
				if (InterfaceScriptSyntax.isInterfaceCommand(innerE)) {
					// Handle requests to lock files, etc. Maybe should go in ProverKnowledge
					try {
						CommandProcessor.getDefault().processCommand(innerE,prover.getSymbols());
					} catch (Exception x) {
						x.printStackTrace();
						PGIPError error = new PGIPError(
								x.getClass().toString()+": "+x.getMessage()+"\n"+innerE.toString());
						firePGIPEvent(error);
					}
				}
			}
		});
		return false;
	}

	/**
	 * @return Returns the active script document, or null if there is none.
	 */
	public ProofScriptDocument getActiveScript() {
		return activeScript;
	}

	/**
	 * Indicate whether it is possible to change the active script or not.
	 * An active script change is only allowed if the currently active script
	 * (if any) is completely processed or completely unprocessed.
	 * @return true if change will be allowed
	 */
	public boolean activeScriptChangePossible() {
		return ((activeScript == null) ||
				activeScript.isFullyProcessed() ||
				activeScript.isFullyUnprocessed());
	}

	/**
	 * Set a new active script being edited by the given document.  Throws
	 * ScriptingException if this isn't allowed by activeScriptChangePossible().
	 * Note: this *can* send a command to the prover (<openfile>)
	 * and (TODO: should) blocks until it completes.
	 *
	 * @param newactiveScript
	 * @throws ScriptingException
	 */
	// da: cleaned this.  Updates document active flag.
	// also locking granularity should be fixed
	public void setActiveScript(ProofScriptDocument newactiveScript)
    throws ScriptingException {
	    synchronized (this) {
	    	if (newactiveScript == this.activeScript) {
				return;
			}
	    	if (activeScriptChangePossible() && !proverState.isBusy()) {
	    		// NB: we want that !proverState.isBusy() --> commandQueue empty.

	    		if (activeScript != null) {
	    			clearActiveScript();
	    		}

	    		// Move the prover to the right directory in case of included files
	    		// da: this is now correctly done by Isabelle on <openfile> messages.
	    		// We should mandate this for other provers too, to simplify the protocol.
	    		// setProverCWD(newactiveScript.getResource());

	    		// Now tell the prover we're editing this file (take the file transition in PGIP).
	    		CmdElement command = new CmdElement(PGIPSyntax.OPENFILE);
	    		URI uri = newactiveScript.getResource().getLocationURI();
	    		PGIPSyntax.addURLattribute(uri, command);
				if (ProofGeneralPlugin.debug(this)) {
					System.out.println("SM: sending <openfile> message for file "+uri.toString());
				}
	    		commandQueue.queueCommand(command);  
	    		// FIXME: should wait for success: need commandQueue.queueCommandAndBlock

	    		// Finally change state and let other people know
	    		activeScript = newactiveScript;
	    		// FIXME da: this next step can fail because the openfile causes an automatic unlock
	    		// action in Isabelle, but that doesn't complete in time since we haven't blocked
	    		// above.  What we should do?  Maybe take retract action explicitly ourselves
	    		// for locked files, before sending <openfile> message.  But we shouldn't assume
	    		// that the retract/open will succeed (perhaps the prover doesn't want to allow us
	    		// to run a file).
	    		activeScript.setActiveForScripting();
	    		// CLEANUP: see earlier note re ActiveScriptChangedEvent.
	    		//firePGIPEvent(new ActiveScriptChangedEvent());
	    	} else if (activeScript != null) {
	    		throw new ScriptingException("Attempt to work in "+
	    				newactiveScript.getTitle()+" when "+
	    				activeScript.getTitle()+" is active.\n\n"+
	    		"You must fully complete or fully undo the active script first.",
	    		true);
	    	} else {
	    		throw new ScriptingException("Failure to switch active script to "+
	    				newactiveScript.getTitle(), true);
	    	}
	    }
	}

	/**
	 * Checks that the prover is alive, but not currently processing something.
	 * @return true if the prover is alive but not busy.
	 */
	public boolean isProverAvailable() {
		return proverState.isAlive() && !proverState.isBusy();
	}

	/**
	 * Clear the currently active script, if there is one.  Throws
	 * ScriptingException if this isn't allowed by activeScriptChangePossible()
	 * or (TODO) if telling the prover about this state change fails.
	 * If a currently active script is completed at this point, add it to the list
	 * of completed documents.
	 * If no active script, just return.
	 * @throws ScriptingException
	 * @author da
	 */
	// da: this method instead of setActiveScript(null,null) in old code.
	// FIXME: if editing of processed text is allowed, we have a glitch here,
	// because more commands can be added and send after <closefile> has
	// been sent.  No real way to get around this: it should count as
	// extending the processed region.  (TODO: write test case for this)
	public void clearActiveScript() throws ScriptingException {
		synchronized (this) {
			if (activeScript != null) {
				if (activeScriptChangePossible() && isProverAvailable()) {
					CmdElement command;
					if (activeScript.isFullyProcessed()) {
						command = new CmdElement(PGIPSyntax.CLOSEFILE);
					} else {
						command = new CmdElement(PGIPSyntax.ABORTFILE);
					}
					if (ProofGeneralPlugin.debug(this)) {
						System.out.println("SM: sending "+command.asXML()+" message for document "+activeScript.getTitle());
					}
					commandQueue.queueCommand(command);	// FIXME: should wait for success
					doClearActiveScript();
				} else {
					throw new ScriptingException("Attempt to clear active script when current script still busy.");
				}
			}
		}
	}
	
	/**
	 * Clear the active script, unconditionally, recording the document as fully processed
	 * on the controlled documents list if appropriate.
	 */
	protected void doClearActiveScript() { 
		activeScript.clearActiveForScripting();
		if (activeScript.isFullyProcessed()) {
			// FIXME da: this has already been called, but other calls should be removed
			controlledDocuments.lock(activeScript);
		}
		activeScript = null;
//		commandHistory.clear(); // FIXME da: to be removed once undo fixed
		// CLEANUP: NB: activescript changed event was a subtype of internal event,
		// but I don't think anyone took notice of it specifically.
		//firePGIPEvent(new ActiveScriptChangedEvent());
	}


	/**
	 * The current working directory of the prover, as last set by us.
	 * Always a normalised URI or null.
	 */
	private URI proverCWD;

    /**
     * Set the working directory for the prover, sending a command
     * to the prover to change it if necessary.
     * @param path - Should be an directory IPath (no file fragment).
     */
	// TODO da: clear up responsibility between here and proverKnowledge
	// which was also keeping the cwd (but as a file, not IPath).
	// Maybe a ProverPath class which looks after CWD and PATH.
	// I'd prefer a flatter structure with that managed here
	// or maybe in proverState, not proverKnowledge (let's keep that
	// for more exotic stuff).
	// For now, I want to control the CWD with this method, and
	// have this method make changes in proverKnowledge.
    public void moveProverCWD(URI path) throws ScriptingException {
    	URI nuri = path.normalize();
    	if (!nuri.equals(proverCWD)) {
        	// If has really changed, tell the prover
    		if (ProofGeneralPlugin.debug(this)) {
    			System.out.println("Changing prover path to " + nuri.toString());
    		}
 			CmdElement cmd = new CmdElement(PGIPSyntax.CHANGECWD);
    		PGIPSyntax.addURLattribute(nuri, cmd);
    		commandQueue.queueCommand(cmd);
    		// update state - TODO: ought to do this only on success.
    		setProverCWD(nuri);
    	}
    }

    /**
     * Set proverCWD field.
     * @param nuri
     */
    private void setProverCWD(URI nuri) {
		proverCWD = nuri;
    }

    /**
     * Sets the current working directory to the URI of the file, if possible.
     * This just sets our record, no command is sent to the prover.
     * @param file the file from which to ascertain the new CWD.
     */
    /* da: new(ly dead) code; keep for a moment or two.
    private void setProverCWD(IResource file) throws ScriptingException {
    	if (file.getLocation() != null) {
     		setProverCWD(file.getParent().getLocationURI());
    	}
    } */

	/**
	 * Kills active commands, kills the queue, releases ownership,
	 * and sends an interrupt signal if busy.
	 */
	public void doInterrupt() {
		if (eventQueue.activeFiredEvents.size()>0) {
			//this only interrupts events that are running (ie have gotten their firingQueueSequence)
			//TODO maybe instead we should traverse the "fired" events queue in reverse order,
			//in such a way that they don't update the firingQueueSequence number
			FirePGIPEventThread activeFiredEvent = (eventQueue.activeFiredEvents.get(0));
			if (Constants.LOG_INTERRUPT_ACTIONS) {
				System.out.println("SM: interrupting "+activeFiredEvent);
			}
			if (activeFiredEvent.interruptTask()) {
				return;
			}
			synchronized (eventQueue.firingSequence) {
            	if (eventQueue.activeFiredEvents.remove(activeFiredEvent)) {
            		if (Constants.LOG_INTERRUPT_ACTIONS) {
            			System.out.println("SM: interrupt: "+activeFiredEvent+" wasn't actually active, reset it");
            		}
            		return;
            	}
				if (Constants.LOG_INTERRUPT_ACTIONS) {
                	System.out.println("SM: interrupt: "+activeFiredEvent+" vanished; continuing with interrupt");
                }
            }
		}
		synchronized (commandQueue) {
			//synchronise this to prevent others from modifying/processing the queue
			try {
				proverOwner.releaseOwnership(null);
			} catch (ProverOwnedBySomeoneElseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  //TODO should we force an ownership release ?? -- maybe only if pressed twice ? -- what about interrupting all threads?, or query 'Interruptible' interface on owner
			if (proverState.isBusy()) {
				if (Constants.LOG_INTERRUPT_ACTIONS) {
					System.out.println("SM: interrupt is killing prover queue (had "+
							commandQueue.size()+" elements) then interrupting prover (because it is busy)");
				}
				//if the prover is busy, send the interrupt signal
				// FIXME da: possible livelock here
				interruptedFlag = true;  //only necessary if busy
				commandQueue.killQueue("Interrupt");
				
				// FIXME da: possible race condition in removing queue here if
				// we have outstanding CommandProcessed events to deal with.
				if (activeScript != null) {
					activeScript.removeQueueRegion();
				}
				sendInterrupt();
			} else if (commandQueue.size()>0) {
				if (Constants.LOG_INTERRUPT_ACTIONS) {
					System.out.println("SM: interrupt is killing prover queue (had "+commandQueue.size()+" elements)");
				}
				commandQueue.killQueue("Interrupt");
			}
		}
	}

	/**
	 * Interrupt the prover. Uses the preference settings to determine how to do this.
	 * Does *not* adjust the queue.
	 */
	public void sendInterrupt() {
		//throw new RuntimeException("Not Implemented Yet.\n (sorry)");
		//i think what we need is  "kill -2 PID"   -AH
		if (!proverState.isBusy()) {
			return;   //only send if busy, otherwise we terminate
		}
		try {
			String interruptCommand = prover.getStringPref(ProverPreferenceNames.PREF_INTERRUPT_COMMAND);
			System.err.println("SM: sending interrupt to prover with command: " + interruptCommand);
			Runtime.getRuntime().exec(interruptCommand);
		} catch (Exception e) {
			e.printStackTrace();
		}

		//		try {
		//		    if (proverInfo.getBoolean(Constants.SETTING_KEY_PGIP_INTERRUPT)) {
		//		        DocElement bashta = new DocElement(PGIPSyntax.INTERRUPTPROVER,null);
		//		        sendCommand(bashta);
		//		    } else { // send a control-c interrupt
		//		        char i = 3; // FIXME test, make interrupt work
		//		        //proverState.setBusy(false);
		//		        writer.write(i); writer.write(lineEnd());
		//		        writer.flush();
		//		    }
		//			//proverResponded = false;
		//			//commandHistory.push(command);
		//			//firePGIPEvent(new PGIPOutgoing(this,commandString));
		//		} catch (Exception e) {
		//		    proverState.setBusy(false);
		//			//firePGIPEvent(new PGIPError(this,"Cannot send command: "+e.getMessage(),command));
		//		}
	}

	/**
	 * If sessionManager has not heard from the prover within msecs,
	 * throw a pgip error
	 * Note: We only support timing the last command sent
	 * if there is an existing time out, then calling setTimeout cancels it
	 * @param msecs - do nothing if < 1
	 * @param fatal - If true, a timeout error will trigger a prover process shutdown.
	 */

	public void setTimeout(int msecs, boolean fatal) {
		if (msecs<1) {
			return;
		}
		if (timeOuter != null) {
			timeOuter.cancel(); // stop old time out, if set
		}
		timeOuter = new Timer(true);
		TimerTask checkDone = new CheckDone(this,fatal);
		timeOuter.schedule(checkDone,msecs);
	}
	Timer timeOuter = new Timer(true);
	//boolean proverResponded = true;
	public static final String TIMEOUT_ERROR_TEXT = "Command Timed Out";


	/**
	 * Undo the last processed script command, interpreting the PGorg.eclipse.ui.ide.workbenchIP model for undo behaviour
	 * (indirectly via categorization in {@link PGIPSyntax}.
	 *
	 * If the last command was the end of a proof,
	 * we will have to undo the whole proof + the theorem/lemma
	 * This code assumes that the lemma command comes directly before the proof in the commandHistory
	 *
	 * @return true, if the command undoes instantly (ie. it didn't really need to be undone; note it will still generate events),
	 * false, if the undo has been sent off and it will be some time before the undo is complete.
	 */

// FIXME da: oh dear, what a right old mess!  The undo mechanism should work by being given
// a target position to undo to, and then calculating the undo command necessary to get there,
// and then issuing it in a couple of steps by queuing up the commands.
// We should never mix do/undo commands on the queue.

// CLEANUP: defunct, old undo
//	public boolean undoLastScriptCommand(ProofScriptDocument script, Object cause)
//	throws ScriptingException {
//		if (ProofGeneralPlugin.debug(this)) {
//	        System.out.println("SM: undoLastScriptCommand at state:" +
//	        		proverState.getState().toString() + ", proof depth: " +
//	        		proverState.proofDepth);
//        }
//
//		// da: note that it's the UI's responsibility for preventing this exception.
//		if (script != null && (script != activeScript || !script.isActiveForScripting())) {
//			throw new ScriptingException("Attempt to undo in a script which is not active.");
//		}
//
//		// da: this next block is never entered because prover owner stops it being called,
//		// at least by user actions.  Can it happen in standalone?
//		// First check the command queue, removing the last element which is from this script
//		if (!commandQueue.isEmpty()) {
//			// NB: this will probably ruin the event model, see 'cause' below (interrupt them instead?)
//			synchronized (commandQueue) {
//				if (!commandQueue.isEmpty()) {
//					for (int i=commandQueue.size()-1; i>-1; i--) {
//						CommandQueued qcq = commandQueue.get(i);
//						System.err.println("undoing item from the queue (may fail): "+qcq.command.asXML());
//						if (qcq.command.getProofScript() == script) {
//							commandQueue.remove(i);
//							try {
//								qcq.preFire();
//							  qUndo(qcq.command, cause);  //need to inform Send/Undo's in the queue by passing them their cause
//							} finally {
//								qcq.postFire();
//							}
//							return true;
//						}
//					}
//				}
//			}
//		}
//
//		DocElement oldCommand = null;
//
//		// FIXME da: shouldn't use history here.  Instead look in document to calculate
//		// undo commands.  If document is locked, we must retract.  (Could give error
//		// or warning first).
//
//		for (int i=commandHistory.size()-1; i>-1; i--) {
//			oldCommand = commandHistory.get(i);   // find a command to undo
//			if (oldCommand.getProofScript() == script) {
//				break;  // must be a proper command from the script
//			}
//			if (oldCommand instanceof CmdElement) {       // da: added this to skip any other non-script
//				System.err.println("CmdElement found in history, shouldn't happen!");
//				break;                                         // commands that were added with dummydoc's
//															   // (probably a mistake: this is used only in CommandQueue)
//			}
//			oldCommand = null;
//		}
//
//		if (oldCommand == null) {
//			// TODO: ensure that script is totally unlocked at this point.
//			throw new ScriptingException("Cannot undo: There are no script commands to undo.");
//		}
//
//		if (ProofGeneralPlugin.debug(this)) {
//	        System.out.println("SM: undoLastScriptCommand trying to remove old command: "+oldCommand.getText());
//        }
//
//		if (!proverInfo.syntax.subType(oldCommand.getType(),PGIPSyntax.COMMAND) ||
//			 proverInfo.syntax.subType(oldCommand.getType(),PGIPSyntax.SPURIOUSCOMMAND)) {
//			//in general, dummy undo spurious commands and anything not a command
//			commandHistory.remove(oldCommand);
//			qUndo(oldCommand, cause);
//			return true;
//		}
//		// da: I'm adding a case for file commands.  Undoing these should have no
//		// effect since they're improper.  Leaving event firing out of it for now,
//		// although it should be something TODO at least for PK
//		if (proverInfo.syntax.isFileType(oldCommand.getType())) {
//			commandHistory.remove(oldCommand);
//			return true;
//		}
//
//		// cannot undo into a proof as the history is discarded; this implements the undo model
//		if (proverInfo.syntax.subType(oldCommand.getType(), PGIPSyntax.CLOSEGOAL)
//                && proverState.getState() != ProverState.State.PROOF_LEVEL) {
//			//now on close theorem, we go back through the history looking for an open theorem command,
//			//instead of relying on the document parse (below) -AH
//			//TODO this might break nested things like isar scripts! I think it does! -Dan
//			// da: the document parse is surely more reliable than a mixed up partial history??
//			// da: it does break Isar scripts.  Yuk.  We should at least match open/closes, I'm
//			// doing that now to ignore nesting.  Moreover, what about checking the righ script???
//			DocElement thm;
//			int depth = 0;
//			do {
//				thm = commandHistory.pop();
//				if (thm != null) {
//					if (thm.getProofScript() != script) {
//						continue;
//					}
//					if (proverInfo.syntax.subType(thm.getType(), PGIPSyntax.OPENGOAL)) {
//						depth--;
//						if (depth == 0) {
//							break;
//						}
//					}
//					if (proverInfo.syntax.subType(thm.getType(), PGIPSyntax.CLOSEGOAL)) { // first time should be one we've found above
//						depth++;
//					}
//				}
//
//			} while (thm != null);
//			if (thm==null) {
//				throw new ScriptingException("Undo called on the end of a theorem, but there was no corresponding theorem command in the history! The prover probably has to be restarted.");
//			}
//			//this tells the prover to undo the OPEN theorem element, as opposed to the close theorem element; it works...
////			proverState.openGoal(); // pretend we are inside the proof - This was causing a bug; Sending <abortgoal/> which is inccorect from theorylevel
//			undo(thm, cause);
//		} else if (outerProof(oldCommand) != null && proverState.getState() != ProverState.State.PROOF_LEVEL) {
//			System.err.println("Undoing inside of a theorem's proof element but not undoing a close theorem element");
//			//shouldn't come here any more
//			//this uses the document parse (old code), and works, but not for standalone
//			DocElement proof = outerProof(oldCommand);
//			int start = proof.getPosition().offset;
//			DocElement thm = null;
//			while( (commandHistory.peek()).getPosition() == null ||
//					(commandHistory.peek()).getPosition().offset >= start )
//			{ // TODO what if a proof straddles the queue/history stacks? this probably screws up
//				//System.err.println("commandHistory.pop");
//				thm = commandHistory.pop();
//			}
//			//now no longer needed -- the outerproof *is* the opentheorem element
//			//System.err.println("commandHistory.pop");
////			DocElement thm = (DocElement) commandHistory.pop();
////			// check that this is a theorem
////			if (!proverInfo.syntax.subType(thm.getType(),proverInfo.syntax.OPENTHEOREM))
////				throw new ScriptingException("Undoing proof - expected to find a preceding theorem, but found "
////						+thm.getType()+" "+thm.getText());
//			//this tells the prover to undo the OPEN theorem element, as opposed to the close theorem element
//			undo(thm,cause);
//		} else //if (proverInfo.syntax.subType(oldCommand.getType(), PGIPSyntax.CLOSETHEORY)) {
//			// da: Undo of end is now like ordinary case in Isabelle (history retained).
//			// Supported like this in Isabelle CVS >= 1.1.07
//			/* DocElement openTheory = null;
//			do {
//				if (commandHistory.size()==0) break;
//				openTheory = (DocElement) commandHistory.pop();
//			} while (!proverInfo.syntax.subType(openTheory.getType(), PGIPSyntax.OPENTHEORY));
//			if (!proverInfo.syntax.subType(openTheory.getType(), PGIPSyntax.OPENTHEORY)) {
//				System.err.println("undoLastCommand: didn't find an open theory to retract");
//			}*/
//			//commandHistory.remove(oldCommand);
//			//undo(oldCommand, cause);
//		//} else
//			{ // normal case
//			//System.err.println("commandHistory.remove "+oldCommand.getStringValue());
//			commandHistory.remove(oldCommand);
//			undo(oldCommand, cause);
//		}
//		return false;
//	}
// CLEANUP
//	/**
//	 * Find the largest proof containing this DocElement, or null
//	 * @param e
//	 * @return the outer proof, or null if none found
//	 */
//	DocElement outerProof(DocElement e) {
//		if (e==null) {
//			return null;
//		}
//		DocElement parent = (DocElement) e.getParent();
//		// note: did say PROOF or PROOF_CONTAINER here.
//		if (proverInfo.syntax.subType(e.getType(), PGIPSyntax.PROOF_CONTAINER)) {
//			DocElement larger = outerProof(parent);
//			if (larger!=null) {
//				return larger;
//			}
//			return e;
//		}
//		//set this if we find nothing recursing upwards
//        return outerProof(parent);
//	}

// da: Old undo CLEANUP	
//	/**
//	 * Undo the given command, by mapping onto the appropriate PGIP command.
//	 * Called by undoLastScriptCommand.
//	 * Does NOT remove the command from the history.
//	 * Skips over misc. commands.
//	 * Updates the prover state model.
//	 *
//	 * @param command
//	 */
//	public void undo(DocElement command, Object cause) throws ScriptingException {
//		/* Map an undo action onto the relevant PGIP undo command, which depends on context. */
//		CmdElement undoCmd = null;
//
//		/* ---------- proof level -------------------------- */
//		if (proverState.getState() == ProverState.State.PROOF_LEVEL
//				&& proverState.proofDepth==1 // Warning: Be careful - this could be different depending on how we arrive here - by undoing the opengoal, or by undoing the closegoal
//				&& proverState.syntax.subType(command.getType(),PGIPSyntax.OPENGOAL)) {
//			undoCmd = new CmdElement(PGIPSyntax.ABORTGOAL);
//
//		/* ---------- theory level -------------------------- */
//        } else if (proverState.getState() == ProverState.State.THEORY_LEVEL) {
//			undoCmd = new CmdElement(PGIPSyntax.UNDOITEM);
//
//		/* ---------- file level -------------------------- */
//        } else if (proverState.getState() == ProverState.State.FILE_LEVEL
//				&& proverState.syntax.subType(command.getType(),PGIPSyntax.CLOSETHEORY)) {
//			// we are trying to undo an 'end' command
//			// this requires retracting a named theory - we must first work out the theory name
//			// This implements the undo model
//			// It is done here rather than higher up 'cos it is not the same as undoing an opentheory command
//        	/* da: this now works as ordinary step with ordinary undo command. */
//			undoCmd = new CmdElement(PGIPSyntax.UNDOITEM);
//			/*undoCmd = new DocElement(PGIPSyntax.RETRACTTHEORY,null);
//			if (command.getProofScript() == null) throw new ScriptingException("Cannot find the document to undo. This is a strange and unexpected error.");
//			// da: this seems to repeat a search higher up (which looked into the history, but
//			// threw away result of search: also fails unnecessarily if the history is lost).
//			DocElement openThy = (DocElement) TreeWalker.treeWalk(command.getProofScript().getRootElement(),
//					new Fn() {
//				public Object apply(Tree node) {
//					if (node instanceof DocElement) {
//						if (proverInfo.syntax.subType( ((DocElement)node).getType(), PGIPSyntax.OPENTHEORY))
//							return node;
//					}
//					return null;
//				}
//			});
//			if (openThy==null) throw new ScriptingException("Tried to retract a theory, but can't locate the opentheory command to retract.");
//			String name = openThy.attributeValue("thyname");   //shoudl be thid
//			undoCmd.addAttribute("thyname",name);
//			commandQueue.queueCommand(undoCmd,cause);
//			//TODO the prover model could get this info by monitoring outgoing UNDO events; then if prover complains, model will know
//			// da: yes, this seems like a bad design failure: it assumes undo commands always succeed, right?
//			proverState.modelEvent(new InternalEvent.UndoSent(this,command));
//			// need to undo the open theory command
//			if (command.getProofScript() != null) command.getProofScript().commandUndone(openThy);
//			return;
//	        */
//		/* ---------- other conditions: treat as undo in a proof (maybe wrong!) -------------------------- */
//		} else {
//			undoCmd = new CmdElement(PGIPSyntax.UNDOSTEP);
//		}
//
//		if (undoCmd!=null) {
//			commandQueue.queueCommand(undoCmd,cause,null, Boolean.FALSE, null);  //don't log
//		}
//		// da: NB: we could as well as do this when the PGIP command is outgoing from the queue.
//		proverState.modelEvent(new UndoSent(command));
//		// if this command came from a proof script, unlock it
//		// da: FIXME: this assumes that the command will succeed (of course, it should... but
//		// we really only should unlock and remove from history when it in fact does complete successfully).
//		if (command.getProofScript() != null) {
//			command.getProofScript().commandUndone(command);
//			if (command.getName().equals(PGIPSyntax.ABORTFILE)) {
//				this.clearActiveScript();
//			}
//		}
//	}

	// da: Old undo CLEANUP	
//	/**
//	 * Pretend undo for commands that were queued but never executed
//	 * and for spurious commands (which don't need to be undone).
//	 * @param command
//	 */
//	// da: this method is a BAD idea.  We should use call backs and process these
//	// events in the normal order of things via the queue.
//	public void qUndo(DocElement command, Object cause) {
//		// da: I'm removing next three junk events, hopefully they're quite unnecessary
//		//firePGIPEvent(new PGIPEvent("Dummy Undo: "+command.asXML(), cause));
//		//firePGIPEvent(new CommandProcessed("<dummyUndo/>", cause));
//		//firePGIPEvent(new PGIPReady(""));
//		// if this command came from a proof script, unlock it
//		if (command.getProofScript() != null) {
//			command.getProofScript().commandUndone(command);
//		}
//	}

	/** the UTC time of the last prover input or output */
	public long lastProverAction = -1;

	/**
	 * Stores partial theorem prover output
	 */
	protected StringBuffer incompleteOutput;

	/*
	//TODO not sure when is best time to go ahead and send the next element from the queue -AH
	public static final int WHEN_TO_SEND_NEXT_FROM_QUEUE = _ON_READY;
	public static final int _ON_READY_RECEIVED = 1;
	//advantage of ON_READY is that things happen faster when there's a queue
	//disadvantage is that if there is a fatal error it might not be realised in time to clear the queue (that is done by sm.pgipEvent handler)
	public static final int _ON_CMD_PROC_EVENT = 2;
	//disadvantage is that if someone else takes cmd proc event, the queue might hang !!
	//none of this is implemented, BTW
	*/

	/**
	 * Process output from the theorem prover, firing off events
	 * also removes the processed command from the queue.
	 * Calls parseTpOutput, which does the work of creating PGIP events.
	 *
	 * @param output a line of output from the theorem prover
	 */
	@SuppressWarnings({ "boxing", "boxing" })
    protected void processOutput(String output) {
		//System.err.println(NumericStringUtils.makeDateString()+"  got some output");
		lastProverAction = System.currentTimeMillis();

		if (ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_LOG_PROVER_IO)) {
			System.err.println("PROVER-"+getProver().getName()+" ->: "+output.trim());
		}
		// da: this conversion on every input seems overly expensive in case we will discard
		// the text.  It should be removed IMO.  Also, there is a difference between the
		// results of parsescript being encoded (don't want) and the output text being decoded.
		// FIXME: sort this out.  Turn off for now and see what happens.
		//if (ProofGeneralPlugin.getBooleanPref(Constants.PREF_SYMBOL_SUPPORT)) {
		//	output = prover.getSymbols().useUnicode(output,true); // switch early to using symbols
		//}

		// We ignore any bumpf from the theorem prover before the first PGIP message.
		if (startUpBumpfFlag) {
			if (output.indexOf("<pgip") == -1) { // safe since output is whole line
					System.err.println("Prover startup: "+output+" (ignored)");
					return;
			}
			startUpBumpfFlag = false;
            incompleteOutput = new StringBuffer("");
		}

		incompleteOutput.append(output);

		if (output.indexOf("</pgip")==-1) { // FIXME da: not safe for CDATA with </pgip> in it!!
			StringManipulation.lineEnd(incompleteOutput);  // add new line
			return;
		}

		// Perhaps we've got a complete message now (or possibly more than one)
		PGIPEvent event = parseTpOutput();

		if (event ==null) {
			//System.err.println(NumericStringUtils.makeDateString()+"  output was incomplete");
			StringManipulation.lineEnd(incompleteOutput);  // add new line
			return;
		}
		//System.err.println(NumericStringUtils.makeDateString()+"  output was complete, firing events");

		if (timeOuter != null) {
			timeOuter.cancel(); // stop setTimeOut, if set
			timeOuter = null;
		}

		//new model takes from active command lookup; synchronized on it with interruptions, new commands added
		//(doesn't actually need to be synchronized with new commands added but it's all fast, so no realy problem)
		synchronized (commandQueue) {
			CommandQueued cq = null;
			if (event.refseq>=0) {
				cq = commandQueue.activeCommandLookup.get(event.refseq);
			}
			if (!(event instanceof TimeOutError) && !proverState.isAlive()) {
				//if not alive, check that process is still okay, then try setting alive
				synchronized (this) { // This is a bit ugly. We should try to avoid touching process or socket except at startup/shutdown. - Dan
					if (process!=null || socket!=null) {
						proverState.setAlive(true);
					}
				}
			}
			synchronized (eventQueue.firingSequence) {
				//this is wrapped in a synchronized block so the CommandProcessed events are
				//fired before the initial events are handled -AH   (not quite sure what i meant by this... perhaps it enforces more than we need ??)

				// da: I'm not quite sure what you mean either, because synchronized just means that nobody else
				// can get in here at the same time as you, it doesn't make any guarantees on the order of
				// events being processed.  But they should be processed by the event queue in the order fired, right?
				// Unless it spawns them off into different threads.
				// Presumably this is happening somewhere because I see non-determinism in the interface at the moment
				// (sometimes interface outputs appear before prover, sometimes vise versa).
				// A way to avoid this particular oddity is not to have the interface displaying outputs in the same place
				// as the prover, which is anyway a bit confusing IMO.

				if (cq==null) {
					// Something not in active table... this can occur from initial startup messages, but also
					// over-enthusiastic theorem provers that want to tell us things from time to time.
					firePGIPEvent(event);
					if (event instanceof PGIPReady) {
						processReady(event);
					}
				} else {
					try {
						cq.preFire();
						event.cause = cq.cause;
						firePGIPEvent(event);
						if (event instanceof PGIPError) {
							// da: FIXME: this could be done at the same time as we make the markers,
							// which would mean we set this flag even if exceptions occur in firing event
							if (((PGIPError)event).fatality.commandFailed()) {
								cq.commandFailed = true;
							}
						} else if (event instanceof PGIPReady) {
							processReady(event, cq);
						}
					} finally {
						cq.postFire();
					}
				}
			}
		}
	}

	/**
	 * Process a PGIP ready event which has arrived without an explictly associated command
	 * being sent.  Maybe an interrupt, or maybe the prover just being voluntarily chatty.
	 * @param event
	 */
	private void processReady(PGIPEvent event) {
		synchronized (commandQueue) {
			proverState.setBusy(false);
			interruptedFlag = false;

			if (commandQueue.size()>0) {
				//if any other events are getting through here, they may need the processedCommand and queue size is not a good way to tell (also who would remove it)
				//TODO probably shouldn't be coming here, not sure these commands do what they should
				if (interruptedFlag) {
					System.err.println("interrupted, but still items in queue, likely more errors will follow; when generating event: "+event);
				}
			}
			CommandQueued cq = commandQueue.getActiveCommand();
			if (cq != null) {
				firePGIPEvent(new CommandProcessed(cq.command, cq.cause));
				commandQueue.clearActiveCommand();
				// TODO: we could send the next command here ourselves, but the 
				// qUpdater does this at the moment.  There are other places
				// where CommandProcessed events are sent, so that's OK for now.
				// This could be simplified, though.
			}
		}
	}

	/**
	 * Process a PGIP ready event which has resulted from a command we sent.
	 * @param event
	 * @param cq
	 */
	@SuppressWarnings("boxing")
    private void processReady(PGIPEvent event, CommandQueued cq) {
		// we used to do this only if there was something in the queue
		// now we don't say CommandProcessed if there was an error; maybe we should?
		// (normally a ready message will be sent as well, but if the boolean flag is set false we shouldn't wait on it)
		synchronized (commandQueue) {
			proverState.setBusy(false);
			interruptedFlag = false;

			//TODO could do this removal in qUpdater ?
			// da: yes, this should happen there, because then we can handle command completion here
			// for commands which weren't sent to the prover.
//			if (!commandQueue.remove(cq)) {
//				System.err.println("command found for event "+event+" was not in queue");
//				//could happen if we're not waiting on ready messages and we get multiple output from a command ?? no, shouldn't
//			}
			if (commandQueue.activeCommandLookup.remove(cq.seq)==null) {
				System.err.println("command found for event "+event+" was not in active table");
				//how could this get removed, we just got it above, and it's all synced in commandQueue
				//could happen if we're not waiting on ready messages and we get multiple output from a command ?? no, again, shouldn't
			}
		}
		if (cq.commandFailed) {
			// FIXME da: consider removing this.  I don't think a log of all our errors is very useful,
			// since they'll be repeated, corrected, etc.  Marker model is much better.  We can
			// keep problems from session around and clear them at session re-init.
// CLEANUP			
//			if (logging) {
////				DocElement lastCmd = (commandHistory.isEmpty() ? null : (DocElement) commandHistory.pop());
//				//if (lastCmd!=null) System.err.println("commandHistory.pop");
//				if (!cq.command.equals(lastCmd)) {
//					System.err.println("error in command "+cq.command.asXML()+", but top of history is "+
//							(lastCmd==null ? "empty" : lastCmd.asXML()));
//				}
//			}
			// da: now I've added marker generation at the same time as event generation, we don't
			// keep a log of errors along with the command. They could perhaps be reconstructed
			// by scanning markers if necessary (don't think so).
			firePGIPEvent(new CommandCausedErrorEvent(cq.command, event.getText() , cq.cause));

			commandQueue.killQueueBut("An earlier command caused an error.", cq);  //don't remove this command from the queue; the Ready message should do that -AH  (moved here, it will have already done it so could just kill all now, but in case we want to move it)
			//but don't kill first item on queue
		} else {
			firePGIPEvent(new CommandSucceeded(cq.command, cq.cause));
		}
		// da: why do we need this final step??   The only references I can find in the code
		// are ones which fire this event, nothing that looks for it specifically.
		firePGIPEvent(new CommandProcessed(cq.command, cq.cause));
	}

	/**	Flag for ignoring non-XML output during prover startup (e.g. with Isabelle) */
	public boolean startUpBumpfFlag = true;

	/** True when we interrupt the process or at start; supresses error messages */
	protected boolean interruptedFlag = false;

	/** Reader used in {@link #parseTpOutput() } */
	private final SAXReader saxReader = new SAXReader();

	/**
	 * Return an event for the next PGIP message in incompleteOutput.
	 * Returns null if there is a problem parsing the message.
	 * There must be a complete PGIP message in incompleteOutput.
	 *
	 * @return an event representing this output
	 */
	protected PGIPEvent parseTpOutput() {

		// da: this code needed reworking (at least because with new ML compilers/Isabelle,
		// multiple PGIP messages are received from the input at once). It still has problems.
		// Can we not use saxReader to read from the input stream directly?
		// Or break the input stream into chunks by filtering it rather than
		// breaking up output, scanning it, then reassembling?

		// Coming here the suggestion is that we have a complete message in
		// incompleteOutput.  But we need to keep the rest of it.
		// FIXME: next code is broken with </pgip> appearing in CDATA, for example
		int msgend = incompleteOutput.indexOf("</pgip>"); // sane XML producer case
		if (msgend == -1) {
			msgend = incompleteOutput.indexOf("</pgip");
			if (msgend == -1) {
				return null;  // called erroneously
			}
			 // insane XML producers: </pgip [whitespace] >
			int closeangle = msgend+6;
			while (incompleteOutput.length() > closeangle &&
					incompleteOutput.charAt(closeangle) == ' ') { // FIXME: add other whitespace chars
				closeangle++;
			}
			if (incompleteOutput.length() > closeangle &&
					incompleteOutput.charAt(closeangle) == '>') {
				incompleteOutput.delete(msgend+6, closeangle); // remove crummy ws
			} else {
				return null;  // confused, e.g. <pgipXXX/>  but no <pgip/>
			}
		}
		assert msgend >= 0;
		int end = msgend + 7;
		String output = incompleteOutput.substring(0, end);
		incompleteOutput.delete(0, end);

		Document document = null;
		try {
			//System.err.println(NumericStringUtils.makeDateString()+"  parsing output as xml");
			document = saxReader.read(new StringReader(output));
			//System.err.println(NumericStringUtils.makeDateString()+"  parsed output as xml, successfully");
		} catch (DocumentException e) {
			// System.err.println(NumericStringUtils.makeDateString()+"  had parse errors");
			// not valid XML.  Used to make a "bogus" event for it, but let's just drop it now.
			// (This breaks some nice error recovery for older Isabelle, wrong command invocations, etc,
			// but we need to keep outselves trim).
			
			//TODO ALEX added next two lines back in for testing
//			document = Converter.fixBadXml(output, saxReader);
//			if (document==null) {
				
			System.err.println("unknown non-XML prover message: "+output);
			return null;
			
//			}
		}
		List list = document.content();
		if (list.size()!=1) {
			System.err.println("Wrong number of responses received, should be exactly one PGIP payload per PGIP packet\n" +
					"Ignored PGIP packet:\n" + output);
			return null;
		}
		return makeEvent((Element)list.get(0));
	}

	/**
	 * Try to find the document which may have caused this event.  This is an interim way of
	 * finding a resource for markers (eventually should use location attributes when Isabelle
	 * makes them).
	 * @param event an event 'caused by' a DocElement.
	 * @return the document which caused the specified event.
	 */
	@SuppressWarnings("boxing")
    private  CmdElement commandOfEvent(PGIPEvent event) {
		CommandQueued cq = null;
		if (event.refseq>=0) {
			// NB: Code can get here but not find non-null cq.
			// FIXME: *should* that happen?  (maybe seen on interrupt)
			cq = commandQueue.activeCommandLookup.get(event.refseq);
		}
		if (cq != null) {
			return cq.command;
		}
		return null;

	}

// da: new code but sadly no good: every *character* appears as a separate
// text node in dom4j.  So desymbolise on display for now, after all.
// That may be less efficient (larger strings, symbolising more than once),
// but does avoid symbolising when it's not necessary.
//
//	static class SymboliseVisitor extends VisitorSupport {
//		HTMLSymbols syms = prover.getSymbols();
//		public void visit(CDATA node) {
//			node.setText(syms.useUnicode(node.getText(), true));
//		}
//		public void visit(Text node) {
//			node.setText(syms.useUnicode(node.getText(), true));
//		}
//	}
//
//	private static Element symboliseOutput(Element proveroutput) {
//	if (ProofGeneralPlugin.getBooleanPref(Constants.PREF_OUTPUT_SYMBOL_SUPPORT)) {
//		SymboliseVisitor visitor = new SymboliseVisitor();
//		proveroutput.accept(visitor);
//	}
//	return proveroutput;
//}

	/**
	 * Create an event from an element of prover output.
	 * @param e an element of prover output.
	 * @return an event, or null if one could not be created
	 */
	PGIPEvent makeEvent(Element e) {

		Iterator ei = e.elementIterator();
		if (!ei.hasNext()) {
			return null;
		}

		Element pgipPayload = (Element)ei.next();
		String pgipelt = pgipPayload.getName().intern();

		if (pgipelt == PGIPSyntax.PARSERESULT) {
			return new PGIPParseResult(this,e);

		} else if (pgipelt == PGIPSyntax.ERRORRESPONSE) {

			// FIXME da: this seems to be an UGLY HACK for an internal error condition,
			// would be neater to add it to the event queue without faking
			// it as theorem prover input.
			if (pgipPayload.getText().startsWith(TIMEOUT_ERROR_TEXT)) {
				return new TimeOutError(e.toString());
			}
			return makeErrorEvent(e);

		} else if (pgipelt == PGIPSyntax.READY) {
			// da: could set a flag right here to know that the command has succeeded.
			return new PGIPReady(this,e);

		} else if (pgipelt == PGIPSyntax.NORMALRESPONSE) {
			List elts = pgipPayload.elements();
			if (elts.size()>0) {
				// da: skim out tracing messages and deal with them quickly
				Element pgml = (Element)pgipPayload.elements().get(0);
				if (tracingoutput != null &&
							"tracing".equals(pgml.attributeValue("area"))) {
						// decoding PGML/symbolising is maybe a bit too costly? (better if
						// we could batch it up, do it lazily, but that might be tricky in
						// console view).
						//String str = pgipPayload.getStringValue();
						String str = converter.getPlaintext(pgml);
						tracingoutput.write(str);
						tracingoutput.write("\n");
					return null;
				}
			}
			return new PGIPIncoming(this,e);
		} else if (pgipelt == PGIPSyntax.METAINFORESPONSE) {
			// TODO: handle unexpected parsescript results somewhere!
			return new PGIPParseResult(this,e);
		} else {
			return new PGIPIncoming(this,e);
		}
	}

	/**
	 * Make an error event, and take early, platform-general action.  In particular,
	 * we generate markers, send debug messages to the console, and log messages to the log.
	 * @param e the erroneous element
	 * @return the newly-created error.
	 */
	private PGIPError makeErrorEvent(Element e) {
		Element pgipPayload = (Element)e.elementIterator().next();
		Fatality fatality = Fatality.fromString(pgipPayload.attributeValue("fatality"));

		// Let's filter out log and debug messages.
		if (fatality == Fatality.DEBUG && debugoutput != null) {
			debugoutput.write(pgipPayload.getStringValue()+"\n");
		}

		// Error messages are either system-oriented (log/debug messages)
		// or user-oriented.

		if (fatality.log()) {
			// TODO: log to workbench log or somewhere else.  Then
			// We discard these messages and don't transmit them further.
			return null;
		}
		// Make an event and add a marker
        final PGIPError err = new PGIPError(this,e);
        err.fatality = fatality;
        err.location = new Location(pgipPayload);
        final CmdElement cmd = commandOfEvent(err);

        // da: next method potentially costly, we could run it in a bg thread
        ProofScriptMarkers.addErrorMarker(err,cmd,this.converter);
        return err;
	}


	// TODO da: I don't really understand why we keep a history of commands sent.
	// The correct undo information can be calculated efficiently from the document.
	// Making processed commands read-only means that it can't be corrupted.
	// NB: however, this may not be true for Alex's code.
	// But one way to re-work in a PGIP-compliant way would be to use a temporary document
	// which isn't displayed to the user.
	// Working on several documents simultaneously really does require context switching, however.

//	// Message history stuff
//	protected int MAX_COMMAND_HISTORY = 5000;
//	// FIXME da: remove this next thing, it's not needed.  History is implicit in the document!!!
//	protected LimitedStack<DocElement> commandHistory = new LimitedStack<DocElement>(MAX_COMMAND_HISTORY);
//	protected int MAX_MESSAGE_HISTORY = 200;
//	/**
//	 * A stack of all events generated by the SessionManager
//	 *  - includes outgoing commands, incoming messages and internal messages
//	 */
//	public LimitedStack<PGIPEvent> messageHistory = new LimitedStack<PGIPEvent>(MAX_MESSAGE_HISTORY);
//	//public Stack openGoals = new Stack();
//
//	/**
//	 * replace all the history objects with fresh copies
//	 */
//	public void clearHistory() {
//		commandHistory = new LimitedStack<DocElement>(MAX_COMMAND_HISTORY);
//	}

	public void addListener(IPGIPListener listener) {
		assert listener != null : "Tried to add a null listener";
		if (! eventQueue.listeners.contains(listener)) {
			eventQueue.listeners.add(listener);
		}
	}

	public void removeListener(IPGIPListener listener) {
		eventQueue.listeners.remove(listener);
	}

	private static List<IPGIPListener> allSessionManagerListeners = new LinkedList<IPGIPListener>();

	/** this will add the given listener to all current session managers (actually to their proverState objects if not null),
	 *  and keep a record of it so that it is automatically added to all new created session managers as well
	 *  (useful mainly for Views which might be created before or after the session manager)
	 */
	public static void addAllSessionManagerListener(IPGIPListener l) {
		SessionManager[] sms;
		synchronized (allSessionManagerListeners) {
		  sms = ProofGeneralPlugin.getSessionManagers();
		  allSessionManagerListeners.add(l);
		}
		for(int i=0; i<sms.length; i++) {
			SessionManager sm = sms[i];
			if (sm != null) {
				sm.addListener(l);  //the views added to the proverState instead, odd.
//				if (sm.proverState!=null) {
//					sm.proverState.addListener(l);
//				} else {
//					//shouldn't happen...
//					System.err.println("proverState is null on "+sm+"; adding to SessionManager instead.");
//					sm.addListener(l);
//				}
			}
		}
	}
	/** this will stop automatically adding the listener to newly created session managers */
	public static void removeAllSessionManagerListener(IPGIPListener l, boolean alsoDeregisterEverywhere) {
		synchronized (allSessionManagerListeners) {
		  allSessionManagerListeners.remove(l);		  
		}
		for (SessionManager sm : ProofGeneralPlugin.getSessionManagers()) {
			sm.removeListener(l);
		}		
	}

	/**
	 * Checks if the queue is and events are all empty
	 * @returns true if the queue is empty and all events have been dispensed
	 */
	public boolean isEmptyQueueAndEvents() {
		return (commandQueue.isEmpty() && (eventQueue.firingSequence.get()==eventQueue.firingQueueSequence) && commandQueue.isEmpty());
	}

	/**
	 * Checks if the queue is empty
	 * @return true if the command queue is empty.
	 */
	public boolean isEmptyQueue() {
		return commandQueue.isEmpty();
	}

	/*
	 * Block until the prover is ready, for a maximum of MAX millisecs.
	 * Uses setTimeout, which will fire an error  if it doesn't get a response.
	 * MAX must be used, since this method might be blocking the display thread
	 *  - it will default to the preference time out or 20secs if passed 0 or -1.
	 * TODO: I don't think this works properly,a lthough I can't see why not.
	 */
	/*public void wait4Ready(int MAX) {
	 if (MAX<1) {
	 MAX = ProofGeneralPlugin.getIntegerPref(Constants.PREF_TIME_OUT);
	 if (MAX<1) MAX = 20000;
	 }
	 if (!proverState.isBusy()) return;
	 boolean bg = backgroundThread;
	 backgroundThread=true; // so that any messages can get through the block we're about to throw up
	 setTimeout(MAX,false);
	 while (proverState.isAlive() && proverState.isBusy() && timeOuter != null) {
	 // wait
	  }
	  backgroundThread = bg;
	  }*/


	/**
	 * Access to deep (lazy) parsing is provided via the session manager
	 */
	public Parser getParser() {
		return parser;
	}
	
	/**
	 * Set to true to stop output being sent via the display thread
	 * Note: this will cause a stream of errors from thos PGIPListeners that try to do things with the display,
	 * but these should not be a problem
	 */
	//TODO actually i think nothing should be done in the display thread
	boolean dontProcessOutputInDisplayThread = true;


// da: CLEANUP	
//	/**
//	 * Determines whether or not to keep messages.
//	 * Affects both messageHistory and CommandHistory.
//	 * Note: WITHOUT THIS SET TO TRUE, UNDO WILL NOT WORK.
//	 */
//	boolean logging = true;

	/**
	 * Called when a stream stops being functional
	 * -- usually signifies the prover has closed. */
	protected void noteStreamStopped() {
		if (socket!=null) {
			//TODO handle socket closures
			return;
		}
		if (process != null) {
			synchronized (this) {
				// Let's wait a wee while for it to exit
				int exitCode = ProcessUtils.waitFor(process, 5);
				if (exitCode >= 0) {
					System.out.println("prover exited unexpectedly with exit code "+exitCode);
				} else {
					System.out.println("prover closed I/O stream and was destroyed");
				}
				commandQueue.killQueue("prover died");
				proverState.setAlive(false);
				process = null;
			}
		}
	}

	private static class SwitchThread implements Runnable {
		SessionManager sm;
		String line;
		public SwitchThread(SessionManager sm, String line) {
			this.sm = sm;
			this.line = line;
		}
		public void run () {
			sm.processOutput(line);
		}
	}

	/**
	 * Used to do time outs. Sends an error message if actually run.
	 */
	class CheckDone extends TimerTask {
		SessionManager sm;
		boolean fatal;

		/**
		 * @param sm
		 * @param fatal - if true, a timeout error will be interpreted as a dead process.
		 */
		public CheckDone(SessionManager sm,boolean fatal) {
			this.sm = sm;
			this.fatal = fatal;
		}

		@Override
        public void run() {
			String line;
			timeOuter = null;
			if (fatal) {
				sm.stopSession(null,false);
				String msg = "The theorem prover timed out, and is now presumed dead."
					+"\nThis session has been closed.";
				ErrorUI.getDefault().signalError(new Exception(msg));
			}
			String msg;
			CommandQueued cq = commandQueue.getActiveCommand();
			String cmdtext = (cq != null ? cq.command.getText() : "missing command: inconsistency!"); 
			msg = TIMEOUT_ERROR_TEXT + ": "+ cmdtext;
			// da: yuk, why do we make a fake message here to input?  TODO: remove this and do better
			line = "<pgip><errorresponse>"+msg+"</errorresponse></pgip>";
			org.eclipse.swt.widgets.Display.getDefault().asyncExec(new SwitchThread(sm,line) );
		}
	}

	public static class TimeOutError extends PGIPError {
		public TimeOutError(String pgipMessage) {
			super(pgipMessage);
		}
	}


	/**
	 * Our listener for preference change events which may affect the
	 * state of the session manager.
	 * @see org.eclipse.core.runtime.Preferences.IPropertyChangeListener#propertyChange(org.eclipse.core.runtime.Preferences.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		String prop = event.getProperty();
		//if (prop.equals(PreferenceNames.PREF_ENABLE_SCRIPTING)) {
		//	parser = null; // reset the parser
		// } 
		if (prop.equals(ProverPreferenceNames.PREF_STYLESHEET_FILE)) {
			converter.init();
		}
	}

	/**
	 * Retract a completely processed (locked) file by issuing a PGIP retractfile command.
	 * @param uri the identifier of the resource to retract.
	 * TODO: not all provers support this command, we should check before sending it.
	 * After retraction we should remove
	 * the file from our list of locked files (this *may* happen automatically
	 * by informfileretracted if the prover sends that message; it does
	 * in the case of Isabelle).
	 */
	public void retractfile(URI uri) throws ScriptingException {
		// Make sure at top-level (necessary to register completed file).
		clearActiveScript();
        CmdElement command = new CmdElement(PGIPSyntax.RETRACTFILE);
        PGIPSyntax.addURLattribute(uri, command);
        commandQueue.queueCommand(command);
	}


	/**
	 * Test to see if some document is active for scripting.
	 * @return true if there is an active script
	 */
	 public boolean hasActiveScript() {
		return activeScript != null;
	}

	/**
	 * Test to see if the given document is active for scripting with this session manager.
	 * @param document
	 * @return true if the session manager's active script is the given document
	 */
	public boolean isActiveScript(ProofScriptDocument document) {
		return activeScript == document;
	}


	/**
	 * Try to parse the rest of the given document
	 * @param doc
	 */
	// FIXME: try to get this working asynchronously
	public void autoParseDoc(final ProofScriptDocument doc) {
        if (ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_PARSE_AUTO)) {
        	final SessionManager sm = this;
        	try {
        		if (doc.getParseOffset()+1 < doc.getLength()) {
        			IWorkspaceRunnable r = new IWorkspaceRunnable() {
        				public void run(IProgressMonitor monitor) {
        					// FIXME: would be better to wait for quiet queue than give up
        					if (sm.proverState.isAvailable()) {
        						try {
        							// FIXME: race condition here, check isAvailable, then someone
        							// enters something into the queue...
        							// FIXME 2: another problem: if this is run from
        							// another thread, we get a livelock/contention issue.
        							sm.getParser().parseDoc(doc, doc.getParseOffset()+1);
        						} catch (UnparseableException e) {
        							// Parse errors are ignored (will be flagged by markers)
        						} catch (Exception e) {
        							e.printStackTrace();
        						}
        					}
        				}
        			};
        			IResource resource = doc.getResource();
        			resource.getWorkspace().run(r, null,IWorkspace.AVOID_UPDATE, null);
        		}
        	} catch (Exception ex) {
        		// TODO: log
        		if (ProofGeneralPlugin.debug(Parser.class)) {
        			ex.printStackTrace();
        		}
        	}
        }
	}

	/**
     * @return the converter (non-null)
     */
    @NotNull
    public Converter getConverter() {
    	return converter;
    }

	/**
     * @return the prover (non-null)
     */
    @NotNull
    public ProofGeneralProverRegistry.Prover getProver() {
    	return prover;
    }

	/**
     * @return the proverState (non-null)
     */
    @NotNull
    public ProverState getProverState() {
    	return proverState;
    }


}
