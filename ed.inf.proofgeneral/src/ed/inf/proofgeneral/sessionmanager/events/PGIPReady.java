/*
 *  $RCSfile: PGIPReady.java,v $
 *
 *  Created on 07 Aug 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.sessionmanager.events;
import org.dom4j.Element;

import ed.inf.proofgeneral.sessionmanager.SessionManager;

/**
 @author Daniel Winterstein
 */
public class PGIPReady extends PGIPIncoming {

    public PGIPReady(SessionManager sm, Element content) {
        super(sm,content);
    }

}
