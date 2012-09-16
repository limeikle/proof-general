/*
 *  $RCSfile: PGIPParseResult.java,v $
 *
 *  Created on 06 Sep 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.lazyparser;

import org.dom4j.Element;

import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.proofgeneral.sessionmanager.events.PGIPIncoming;

/**
 @author Daniel Winterstein
 */
public class PGIPParseResult extends PGIPIncoming {

    public PGIPParseResult(SessionManager sm, Element content) {
        super(sm, content);
    }

}
