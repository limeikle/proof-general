/*
 *  $RCSfile: CommandQueued.java,v $
 *
 *  Created on 03 May 2005 by Alex Heneveld
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.sessionmanager;

import ed.inf.proofgeneral.NotNull;
import ed.inf.proofgeneral.document.CmdElement;

public class CommandQueued {
	
	/** The command */
	@NotNull
	CmdElement command;
	
	/** An optional record of an object that caused this command to be sent */
	Object cause;
	
	/** Sequence number */
	public int seq;

	/** The session manager this command is queued for */
	// da: TODO: refactor this so that there is a command queue per SM, not commands queued with SM.
	protected final SessionManager sessionManager;

	public CommandQueued(SessionManager manager, CmdElement command, Object cause) {
		assert command != null : "Invalid command";
		assert manager != null : "Invalid session manager";
		this.command = command;
		this.sessionManager = manager;
		this.cause = cause;
	}

	/**
	 * Called before the PGIPOutgoing event is fired, just after the command has been sent.
	 * <b>Must execute quickly</b>
	 */
	public void preFire() { }

	/**
	 * Called just after the PGIPOutgoing event is fired 
	 * <b>Must execute quickly</b>
	 */
	public void postFire() { }
	
	/**
	 * @param currentSeqnNo
	 */
	public void setSeqNo(int currentSeqnNo) {
		seq = currentSeqnNo;
		sessionManager.setCommandQueued(this, currentSeqnNo);
	}

	/*private List<PGIPEvent> errors = null; */
	boolean sent = false;

//	/**
//	 * Records an error, given its event.
//	 * @param event the even to record as an error
//	 */
//	public synchronized void recordError(PGIPEvent event) {
//		if (errors==null) errors = new ArrayList<PGIPEvent>();
//		errors.add(event);
//	}

	/** A flag indicating whether this command is considered to have failed. */
	public boolean commandFailed;

//	/**
//	 * Checks if errors have been reported.
//	 * @return true if the error log is not empty or null
//	 */
//	public boolean hadErrors() {
//		return (errors!=null && !errors.isEmpty());
//	}

	/**
	 * Gets the string containing reported errors
	 * @return the errors in string form, or null if none.
	 */
//	public String getErrorString() {
//		if (!hadErrors()) return null;
//		if (errors.size()==1) return ((PGIPEvent)errors.get(0)).getText();
//		StringBuffer errMsg = new StringBuffer(""+errors.size()+" errors:");
//		Iterator ei = errors.iterator();
//		while (ei.hasNext()) {
//			PGIPEvent e = (PGIPEvent)ei.next();
//			errMsg.append("\n"+e.getText());
//		}
//		return errMsg.toString();
//	}

	/** called when the command is sent to the prover (for prover commands only); by default does nothing */
	public void onSend() {
	}
}