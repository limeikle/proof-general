/*
 *  $RCSfile: KnownException.java,v $
 *
 *  Created on 03 May 2006
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.exception;

/**
 * A class for marking a exception as a known one.
 * For example, we might just print an error message for a known exception,
 * but a stack trace for any other.
 * In particular, this exception is caught in EclipseMethods.errorDialog() to
 * display an error dialog without a stack trace.
 */
//da: we want this to be a *checked* exception, so extends Exception not RuntimeException!
public class KnownException extends Exception { 
	public KnownException() {
		super();
	}

	public KnownException(Exception e) {
		super(e);
	}
	
	public KnownException(String msg) {
		super(msg);
	}
}