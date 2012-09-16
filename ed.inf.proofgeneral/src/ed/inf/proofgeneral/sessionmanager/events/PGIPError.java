/*
 *  $RCSfile: PGIPError.java,v $
 *
 *  Created on 29 Apr 2004
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.sessionmanager.events;
import org.dom4j.Element;

import ed.inf.proofgeneral.pgip.Fatality;
import ed.inf.proofgeneral.pgip.Location;
import ed.inf.proofgeneral.pgip2html.Converter;
import ed.inf.proofgeneral.sessionmanager.SessionManager;

/**
 * Event indicating an error.
 * Note that this is NOT a subclass of PGIPIncoming or PGIPOutgoing.
 */
public class PGIPError extends PGIPEvent {

	/** The fatality level for this error. */
	public Fatality fatality;

	/** The location of this error. */
	public Location location;

	/**
     * Does this error indicate a PGIP message has failed?
     * @return true if the error is not fatal and can be ignored.
     */
    public boolean nonFatal() {
        if (fatality != null && !fatality.commandFailed()) {
            return true;
        }
        return false; // da: eek, odd default.  This is for fake errors I guess.
    }
 
	private SessionManager sm; 

	/**
	 * This constructor is used for internal messages
	 * @param plainMessage
	 */
	// FIXME: refactor to separate from incoming error messages
	public PGIPError(String plainMessage) {
		super(plainMessage, null);
	}
	/**
	 * This constructor is used for error messages from the prover
	 * @param sm
	 * @param content
	 */
	public PGIPError(SessionManager sm, Element content) {
		super(content,null);
		this.sm = sm;
	}

	public Converter getConverter() {
	    // TODO Auto-generated method stub
	    return sm.getConverter();
    }

}
