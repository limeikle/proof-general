/*
 *  $RCSfile: UserCancelException.java,v $
 *
 *  Created on 31 Oct 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.sessionmanager;


/**
 Exception created when something does not happen due to user intervention. 
 */
public class UserCancelException extends ScriptingException {
    public UserCancelException() {
        super();
    }
    public UserCancelException(String message) {
        super(message);
    }

    public UserCancelException(Throwable cause) {
        super(cause);
    }
    public UserCancelException(String message, Throwable cause) {
        super(message, cause);
    }

}
