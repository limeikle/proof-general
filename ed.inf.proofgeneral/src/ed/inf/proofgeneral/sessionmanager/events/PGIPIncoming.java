/*
 *  $RCSfile: PGIPIncoming.java,v $
 *
 *  Created on 13 May 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.sessionmanager.events;
import org.dom4j.Element;

import ed.inf.proofgeneral.pgip2html.Converter;
import ed.inf.proofgeneral.sessionmanager.SessionManager;

/**
 * Class for events corresponding to messages really sent from the theorem prover.
 * (NB: superclass currently includes MANY other events, confusingly).
 */
public class PGIPIncoming extends PGIPEvent {
	private final SessionManager sm;
	
	public PGIPIncoming(SessionManager sm, Element content) {
		super(content,null);
		this.sm = sm;
	}
	
	public Converter getConverter() {
		return sm.getConverter();
	}
}
