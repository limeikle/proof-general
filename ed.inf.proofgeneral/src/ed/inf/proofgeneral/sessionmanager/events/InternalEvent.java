/*
 *  $RCSfile: InternalEvent.java,v $
 *
 *  Created on 23 May 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.sessionmanager.events;
import org.dom4j.Element;


/**
 * @author Daniel Winterstein
 */
public class InternalEvent extends PGIPEvent {

	
    /**
     * @param pgipMessage
     */
    public InternalEvent(String pgipMessage,Object cause) {
        super(pgipMessage,cause);
    }

    /**
     * @param content
     * @param cause
     */
    public InternalEvent(Element content, Object cause) {
    	super(content,cause);
    }
    
    /*
        TODO i think we want a ProverMayBeAvailable item instead,
        which is then subclassed into 'clear' and 'owner-release' ; 
        we guarantee one of these will be sent when prover is available
        BUT it may be sent other times, and 
        we don't promise to notify on all sub-events  -AH

     */

    //not used... ProverClear is sent when ownership is released (if applicable)
//  	/**
//  	 * OwnershipReleased -- sent when someone releases ownership
//  	 */
//  	public class OwnershipReleased extends PGIPEvent {
//    	public OwnershipReleased(Object source, String msg) {
//    		super(source, msg, null);
//    	}
//  	}

}
