/*
 *  $RCSfile: IPGIPListener.java,v $
 *
 *  Created on 07 Feb 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.sessionmanager.events;

import java.util.EventListener;

/**
 * Interface for PGIP listeners.
 * @author Daniel Winterstein
 */
public interface IPGIPListener extends EventListener {

	/**
	 * Override this method to do something with the incoming event
	 * @param event
	 */
	public void pgipEvent(PGIPEvent event);
}
