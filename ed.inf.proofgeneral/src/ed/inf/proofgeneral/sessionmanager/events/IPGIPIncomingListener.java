/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jul 7, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */

package ed.inf.proofgeneral.sessionmanager.events;

// TODO: refactoring event listening to partition between different kinds of events.
// We can make an event listening factory which matches against a particular incoming type of message, even.

/**
 * A listener for incoming PGIP events from the prover.
 *
 * @author David Aspinall
 */
public interface IPGIPIncomingListener {
	public void pgipIncoming(PGIPIncoming event);
}
