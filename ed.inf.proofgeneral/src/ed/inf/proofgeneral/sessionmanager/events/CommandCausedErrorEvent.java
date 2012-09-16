/*
 *  $RCSfile: CommandCausedErrorEvent.java,v $
 *
 *  Created on 23 May 2004
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.sessionmanager.events;
import ed.inf.proofgeneral.document.CmdElement;

/**
 * Note that this is *not* a sub-class of PGIPError. 
 * It is used as an internal notification that an error occured, 
 * rather than as a representation of the actual error.
 */
public class CommandCausedErrorEvent extends InternalEvent {

    /**
     * The error message.
     */
    public String fError = null;    
    
    /**
     * Protect against bugging the user with multiple warning dialogs.
     */
    public boolean userNotified = false;
    
    @Override
    public String getText() {
        return super.getText()+": "+fError;
    }

    /**
     * an event that indicates a command failed
     * @param command the command that failed
     * @param error the reason the command failed
     * @param cause the cause of the command that failed
     */
    public CommandCausedErrorEvent(CmdElement command, String error,
            						Object cause) {
        super(command,cause);
        fError = error;
        // 
    }
    
	
	/**
	 * @return the command for this event, or null if it has the wrong type
	 */
	public CmdElement getCommand() {
	    return parseTree instanceof CmdElement ? (CmdElement) parseTree : null;
	}

	
    public static class CommandCancelled extends CommandCausedErrorEvent {
        /**
         * Use this for commands that fail, but were not themsleves the source of an error. 
         */
        public CommandCancelled(CmdElement command, String error, Object cause) {
            super(command,error,cause);
        }
    }

}
