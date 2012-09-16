/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jan 20, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *
 */

package ed.inf.proofgeneral.sessionmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Element;

import ed.inf.proofgeneral.NotNull;
import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.CmdElement;
import ed.inf.proofgeneral.preferences.PreferenceNames;
import ed.inf.proofgeneral.sessionmanager.events.CommandCausedErrorEvent;
import ed.inf.proofgeneral.sessionmanager.events.CommandProcessed;
import ed.inf.proofgeneral.sessionmanager.events.PGIPError;
import ed.inf.proofgeneral.sessionmanager.events.PGIPIncoming;
import ed.inf.proofgeneral.sessionmanager.events.PGIPOutgoing;
import ed.inf.proofgeneral.sessionmanager.events.CommandCausedErrorEvent.CommandCancelled;
import ed.inf.proofgeneral.symbols.SymbolTable;
import ed.inf.utils.datastruct.MutableObject;

/**
 *  Maintains the queue of commands being sent to the theorem prover,
 *  packages and delivers them.
 *  
 *  @author Daniel Winterstein
 *  @author David Aspinall
 */
public class CommandQueue {

	/**
	 * This is really a vector not a stack - we use a stack for convenience when
	 * manipulating queue+history in undo.
	 * TODO remove public access.  This involves implementing a wait mechanism for the queue.
	 * TODO type this object correctly -- at the moment it holds CommandQueued and DocElement objects!
	 */
	public List<CommandQueued> commandQueue = new ArrayList<CommandQueued>();

	/**
	 * A map between outstanding seq ids and commands.  Once a command
	 * has been dealt with, it should be removed from the map (FIXME da: todo)
	 */
	Map<Integer,CommandQueued> activeCommandLookup = new HashMap<Integer,CommandQueued>();

	private final SessionManager sm;  // da: can avoid?

	@NotNull
	private final SymbolTable symbols;      // Used for decoding symbols in strings before sending text.
	
	@NotNull
	private final PGIPEventQueue eventQueue;
	
	public CommandQueue (SessionManager sm,PGIPEventQueue eventQueue, SymbolTable symbols) {
		assert sm != null && eventQueue != null && symbols != null : "Invalid initialiser";
		this.sm = sm;
		this.eventQueue = eventQueue;
		this.symbols = symbols;
	}

	/**
	 * Clear this command queue and reset the PGIP packaging (sequence numbering).
	 * This is appropriate if the component restarts
	 */
	public void clear() {
		pgip_init();
		commandQueue.clear();
	}


	/**
	 * Add a command to the queue, and if the prover isn't currently busy, send it.
	 * Note that scripting commands which are already in the queue may be changing the state,
	 * so there is a risk that the given command may no longer be relevant.  Where
	 * this is a concern, the client should check for an empty queue before calling here.
	 * 
	 * @param cq
	 * @return true if the command has been sent immediately to the prover, false if it is waiting
	 */
	public boolean queueCommand(CommandQueued cq) {
		synchronized (commandQueue) {
			commandQueue.add(cq);
			if (commandQueue.size()==1 && !sm.getProverState().isBusy()) {
				return sendNextQueuedCommand();
			}
			return false;
		}
	}
	
	/**
	 * Queue the given command and wait for it to be processed.
	 * @param cq
	 * @return true if the command was processed successfully.
	 */
	public boolean queueCommandAndWaitForReady(CommandQueued cq) {
		boolean commandSent = queueCommand(cq);
		if (commandSent) {
			// wait until it is processed
		} 
		return true;
	}

	/**
	 * Send a command that doesn't have an associated cause
	 * @param cmd the command to queue
	 * @return true, if the command has been processed, false if we are waiting on the prover
	 */
	public boolean queueCommand(CmdElement cmd) 
//	throws ScriptingException 
	{
		return queueCommand(cmd,null);
	}

	/**
	 * Kill the command queue (removing all queued commands), also clearing any prover ownership.
	 * Generates a stream of @see CommandCausedErrorEvent with the message.
	 * <p/>
	 * note that if a command is in the middle of being processed it will give minor errors.
	 * use killQueueBut to kill everything but a particular command.
	 */
	// TODO da: PGIP/process
	protected void killQueue(String msg) {
		killQueueBut(msg, null);
//		try {
//			sm.proverOwner.releaseOwnership(null);
//		} catch (ProverOwnedBySomeoneElseException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	/**
	 * Add a command to the queue to be sent.
	 * Triggers sendCommand to send the command immediately if the queue is empty.
	 * If this is called with a command from a script, then the script must be the
	 * active one.
	 * @param command
	 * @param cause  the action (if known) that caused this command to be queued.
	 * @return true, if the command has been processed, false if we are waiting on the prover
	 */
	public boolean queueCommand(CmdElement command, Object cause)
//	throws ScriptingException 
	{
		//assertProofScriptIsActiveFor(command);
		return queueCommand(new CommandQueued(sm, command, cause));
	}

//	private void assertProofScriptIsActiveFor(CmdElement command) throws ScriptingException {
//		// da: I want the code to be this:
//		// assert (command.getProofScript() == null || command.getProofScript()==sm.getActiveScript());
//		// But for now things break unless have previous behaviour of this:
//		if (command.getProofScript() != null) {
//			sm.setActiveScript(command.getProofScript());
//		}
//		// Active script should be part of higher-level protocol, not here
//		// (assert even is only temporary).
//	}

	// FIXME  da: this needs cleaning up.
	/**
	 * Add a command to the queue to be sent.
	 * triggers sendCommand if the queue is empty.
	 * Checks whether the document for this command is active.
	 * Allows setting SessionManager values for the command
	 * @param command
	 * @param cause - the action (if known) that caused this command to be queued.
	 * @param backgroundThread  (set null to use previous)
	 * @param privateListener the PGIPListener wrapped; if null use previous,
	 * if wrapped null, explicitly set no private listeners for this command
	 * @return true, if the command has been processed, false if we are waiting on the prover
	 */
	public boolean queueCommand(CmdElement command, Object cause, Boolean backgroundThread,
       MutableObject privateListener) {
		//assertProofScriptIsActiveFor(command);
		return queueCommand(new CommandQueuedWithValues(sm, command, cause, backgroundThread, privateListener));
	}




	/**
	 * Kill the command queue (removing all queued commands), except for the indicated command
	 * Generates a stream of @see #{@link CommandCausedErrorEvent} with the message.
	 * @param msg 
	 * @param cq 
	 */
	// TODO da: PGIP/process
	@SuppressWarnings("boxing")
    protected void killQueueBut(String msg, CommandQueued cq) {
		//System.out.println("killing queue (size "+commandQueue.size()+"), "+msg);
		synchronized (commandQueue) {
			for (int i=0; i<commandQueue.size(); ) {
				CommandQueued cmd = commandQueue.get(i);
				if (cmd==cq) {
					i++;
				} else {
					commandQueue.remove(i);
					activeCommandLookup.remove(cmd.seq);
					try {
						cmd.preFire();
						eventQueue.firePGIPEvent(new CommandCancelled(cmd.command, msg, cmd.cause));
						eventQueue.firePGIPEvent(new CommandProcessed(cmd.command, cmd.cause));
					} finally {
						cmd.postFire();
					}
				}
			}
		}
	}


// Sending commands ============================================================


	/**
	 * Sends the next command from the queue, if not empty.  If it is empty, notify 
	 * all the threads waiting on the queue.
	 * @return true if the command has been sent, false if it is queued but not sent.
	 */
	boolean sendNextQueuedCommand() { 
		synchronized (commandQueue) {
			if (commandQueue.isEmpty())
				commandQueue.notifyAll();  //notify anyone listening on the queue that it has emptied
			else {
				CommandQueued cmd = commandQueue.get(0);
				if (!cmd.sent) { // da: flag should be unnecessary
					sendCommand(cmd);
				}
				commandQueue.remove(0);
				return true;
			}
		}
		return false;
	}

	private CommandQueued currentlyActiveCmd;
	
	/**
	 * @return the command currently being processed by the theorem prover, or null
	 * if none.
	 */
	public synchronized CommandQueued getActiveCommand() {
		return currentlyActiveCmd;
	}
	
	/**
	 * Record the currently active command as having been completely processed.
	 */
	public synchronized void clearActiveCommand() {
		currentlyActiveCmd = null;
	}

	/**
	 * Send a command out to the prover - but ignores the queue.
	 * Most uses should use commandQueue.queueCommand.
	 *
	 * Will convert into a PGIP message & add a linebreak to the end if necc.
	 *
	 * @param co the command to send
	 * @return true, if the command has been processed, false if we are waiting on the prover
	 */
	synchronized boolean sendCommand(CommandQueued co) {
		currentlyActiveCmd = co;
		CmdElement command = co.command;
		assert (command != null) : "Command queued with null command element";
		if (internalUseOnly(command)) {
			co.sent = true;
			fakeSendCommand(command);
			return true;
		}
		String commandString;
		commandString = getCommandString(command);
		try {
			synchronized (sm.getQueueSyncObject()) {  //make sure outgoing is fired before incoming -AH
				co.setSeqNo(currentSeqnNo++);
				commandString = packageCommand(commandString, co.seq);
				// FIXME da: visibility on prover busy flag broken. Should be maintained
				// here as part of script management/process handling state, not with
				// a public set method!
				sm.getProverState().setBusy(true);
				sm.setTimeout(ProofGeneralPlugin.getIntegerPref(PreferenceNames.PREF_TIME_OUT),false);
				co.sent = true;
				co.onSend();
				sm.lastProverAction = System.currentTimeMillis();
				
				if (ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_LOG_PROVER_IO)) {
					System.err.println("PROVER-"+sm.getProver().getName()+" <-: "+commandString.trim());
				}
				
				sm.writer.write(commandString);
				sm.writer.flush();
				if (sm.consoleoutput != null) {
					sm.consoleoutput.writeinput(commandString+"\n");
				}
				//System.err.println(NumericStringUtils.makeDateString()+"  command sent");
				try {
					co.preFire();
					eventQueue.firePGIPEvent(new PGIPOutgoing(command,co.cause));
				} finally {
					co.postFire();
				}
			}
		} catch (Exception e) {
			sm.getProverState().setBusy(false);
			PGIPError err = new PGIPError("Cannot send command "+command.asXML()+": "+e.getMessage());
			err.seq = co.seq;
			try {
				co.preFire();
				eventQueue.firePGIPEvent(err);
			} finally {
				co.postFire();
			}
		}
		return false;
	}

	/**
	 * Send a command to ourselves, as if it came from the theorem prover.
	 * No command is outstanding in the theorem prover, so we clear the record
	 * of command being processed.
	 * @param cmd
	 */
	void fakeSendCommand(CmdElement cmd) {
		eventQueue.firePGIPEvent(new PGIPIncoming(sm,cmd));
		clearActiveCommand();
	}

	/**
	 * Given a command element, construct a string to send out.
	 * The string is given by converting into XML and replacing
	 * Unicode symbols with tokens.    
	 * @param command the command to process
	 * @return the ready-to-send string.
	 */
	public String getCommandString(Element command) {
		String cs = command.asXML();
		// Ideally we don't want to have to do this at all: we should keep the source
		// text in ASCII and only display a view with the symbols
		return symbols.useEscapedAscii(cs);
	}


// Inspecting commands =====================================================================

	/**
	 * Check whether this command should be sent to the prover or not.
	 * If it inside a special PGTAG or SPURIOUSTYPE, then it should not be.
	 * Recursively search containers of the given command.
	 * @param e the command to search from
	 * @return true if the tag is found to be enclosing the given command
	 */
	boolean internalUseOnly(CmdElement e) {
		if (sm.proverInfo.syntax.subType(e.getCmdType(), PGIPSyntax.PGTAG.name)) {
			return true;
		}
		// DA: this case is new
		if (sm.proverInfo.syntax.subType(e.getName(), PGIPSyntax.SPURIOUSTYPE)) {
			return true;
		}
		return false; 
	}

//  PGIP delivery =============================================================

	protected String pgipClass = "pa"; // this is a proof assistant
	protected String pgipId= "PG-Eclipse"; //
    protected String pgipVersion = "2.0";
    protected int currentSeqnNo; // counts upwards in packageCommand

    private void pgip_init() {
    	currentSeqnNo = 1;
    }

	/**
	 * Wraps a command with appropriate PGIP Packaging, if required.
	 * TODO needs to be fast, called in sync block ?
	 * @param command the command to wrap
	 * @param seq - sequence number (i.e. currentSeqnNo), or -1 if this is not a sequenced command.
	 * @return the command, wrapped and terminated if required.
	 */
	String packageCommand(String command, int seq) {
		if (command.equals("")) {
			return command; // reject empty commands
		}
		command = pgipPackaging(command, seq);
		// ensure all commands end with a suitable line break
		String lineEnd = lineEnd();
		if (!command.endsWith(lineEnd)) {
			command += lineEnd;
		}
		return command;
	}

	/**
	 * @return the line end character for this theorem prover
	 */
	String lineEnd() {
		String lineEnd = sm.proverInfo.getString(PreferenceNames.SETTING_LINEEND);
		if (lineEnd.equals("")) {
			lineEnd = "\n"; // default to unix
		}
		if (lineEnd.equals("\\n")) {
			lineEnd = "\n";
		} else if (lineEnd.equals("\\r\\n")) {
			lineEnd = "\r\n";
		} else if (lineEnd.equals("\\r")) {
			lineEnd = "\r";
		}
		return lineEnd;
	}


	/**
	 * Add PGIP wrapping to a non-pgip command
	 * @param command
	 * @return the wrapped command.
	 */
	// TODO da: really we want to send out large texts by writing them straight from XML,
	// not converting into string.
	String pgipPackaging(String command, int seqNo) {
		// if not already packaged
		if (command.indexOf("<pgip ") == -1 && command.indexOf("<PGIP ") == -1) {
			command = "<pgip class=\""+pgipClass+"\" id=\""+pgipId+"\" seq=\""+seqNo +"\">"
			+command+"</pgip>";
		} else {
			// da: I don't want to package anywhere but here!
			System.err.println("Warning: pgipPackaging got an already packaged command, shouldn't happen:" + command);
		}
		return command;
	}



//  Accessors =============================================================================

	/**
	 * @return size of queue of commands waiting to be sent to the prover
	 */
	public int size() {
		return commandQueue.size();
	}


	/**
	 * @return true if the queue of commands waiting to be sent is empty
	 */
	public boolean isEmpty() {
		return commandQueue.isEmpty();
	}


// CLEANUP	
//	/**
//	 * @param i
//	 * @return
//	 * TODO: remove this
//	 */
//	public CommandQueued get(int i) {
//		return commandQueue.get(i);
//	}
//
//	public boolean remove(CommandQueued cq) {
//		return commandQueue.remove(cq);
//	}
}
