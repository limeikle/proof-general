/*
 *  $RCSfile: PGIPOutgoing.java,v $
 *
 *  Created on 13 May 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.sessionmanager.events;
import ed.inf.proofgeneral.document.CmdElement;

public class PGIPOutgoing extends PGIPEvent {
	/**
	 * An event indicating the given content is going to be sent to the prover.
	 */
	public PGIPOutgoing(CmdElement content, Object cause) {
		// da: current code takes this thing apart again.  I can't see the point in that.
		// FIXME: fix that.
		super(content, cause);
	}
}
