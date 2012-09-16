/*
 *  $RCSfile: ReportableException.java,v $
 *
 *  Created on 7 Jul 2006
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.exception;

/**
 * Indicates an exception which the user should be told about.
 * Use this for exceptions which are generated in the normal flow of a program, 
 * and which require notifying the user (e.g. user-generated errors).
 */
public class ReportableException extends Exception {

    public ReportableException() {
        super();
    }

    public ReportableException(String message) {
        super(message);
    }

    public ReportableException(Throwable cause) {
        super(cause);
    }

    public ReportableException(String message, Throwable cause) {
        super(message, cause);
    }

}
