/*
 *  $RCSfile: CommandFailedException.java,v $
 *
 *  Created on 04 May 2005 by Alex Heneveld
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.sessionmanager;

import ed.inf.utils.exception.KnownException;


public class CommandFailedException extends KnownException {
	protected String command;
	protected String exceptionMessage;
	
	public CommandFailedException(String command, Exception e) {
		super(e);
		this.command = command;
		exceptionMessage = e.getMessage();
	}
	
	public CommandFailedException(String command, String message) {
		// da: this extra information isn't really helpful now we have markers
		// to point exactly to the command that caused the error.  It's just
		// clutter.
		// super("Could not send command:\n"+command+"\nError message:\n"+message);
		super(message);
		this.command = command;
		this.exceptionMessage = message;
	}
	
	/**
	 * Returns the command which caused the problem.
	 */
	public String getCommand() {
		return command;
	}
	
	/**
	 * Gets the error message from the exception.
	 * Differs from {@link #getMessage()} because the message could be different if it was
	 * not generated from an existing exception.
	 * @see java.lang.Throwable#getMessage()
	 */
	public String getExceptionMessage() {
		return exceptionMessage;
	}
}