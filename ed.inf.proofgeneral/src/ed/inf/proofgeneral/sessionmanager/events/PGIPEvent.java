/*
 *  $RCSfile: PGIPEvent.java,v $
 *
 *  Created on 07 Feb 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.sessionmanager.events;
import java.io.StringReader;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultElement;
import org.dom4j.util.UserDataElement;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.sessionmanager.ScriptingException;
import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.utils.datastruct.StringManipulation;

/**
 * Event created by the session manager when an incoming PGIP message is received
 * @author Daniel Winterstein
 */
// da: this was extending java.util.EventObject but seems not necessary
public class PGIPEvent {

	public Element parseTree; // TODO: move to incoming
	public String type = null;
	public String id=null;    
	public int seq=-1;        // TODO: move to incoming/remove
	public int refseq=-1;     // TODO: ditto
	/** anything can set a 'cause' item on an event, for reference */
	public Object cause;
	
	public SessionManager sm;

	/**
	 * Construct an XML document representing the given message, by attempting to parse it.
	 * @param pgipMessage
	 * @param cause - The action responsible for causing this event, or null if unknown
	 */
	// da: TODO: remove dummy <pgip> stuff here
	public PGIPEvent(// SessionManager sm
				// Object source, da: this isn't really used and is ugly, have refactored it away
			    // as first part of refactoring event management
			String pgipMessage, Object cause) {
		// super(XXX); // NB: dummy as source
		this.cause = cause;
		if (StringManipulation.isWhitespace(pgipMessage)) {
		    parseTree = new UserDataElement("pgip");
		    return;
		}
		try {
			SAXReader reader = new SAXReader();
			Document document;
			document = reader.read(new StringReader(pgipMessage));
			parseTree = (Element) document.content().get(0);
		} catch (Exception e) {
			// da: we get here when we get startup junk from prover without
			// pgip markup as well as nonsense messages generated
			// internally.  Would be better to enter loop to junk it,
			// waiting on <pgip> appearance.  We shouldn't need this
			// case, and we shouldn't generate pgip messages ourselves
			// here.
			parseTree = new UserDataElement("pgip");
			parseTree.addText(pgipMessage);
		}
	}

	/**
	 *
	 * @param content
	 * @param cause - The action responsible for causing this event, or null if unknown
	 */
	public PGIPEvent(Element content, Object cause) {
		//super(null);
		this.cause = cause;
		parseTree = content;
		if (parseTree==null) {
		    if (ProofGeneralPlugin.debug(this)) {
		        System.err.println("Event created with a null parse-tree.");
		    }
		    parseTree = new DefaultElement("dummy");
		}
		try {
			String seq = parseTree.attributeValue("seq");
			if (seq != null && !seq.equals("")) {
				try {
					this.seq = Integer.parseInt(seq);
				} catch (NumberFormatException e) {}
			}
			String refseq = parseTree.attributeValue("refseq");
			if (refseq != null && !refseq.equals("")) {
				try {
					this.refseq = Integer.parseInt(refseq);
				} catch (NumberFormatException e) {}
			}
			id = parseTree.attributeValue("id");
		    switch (parseTree.elements().size()) {
		    	case 0: type = parseTree.getName(); break;
		    	case 1: type = ((Element) parseTree.elements().get(0)).getName(); break;
		    	default: {
		    		System.err.println("Event without valid Parse Tree: "+parseTree);
		    		throw new ScriptingException("Event created without a valid parse-tree or element.");
		    	}
		    }
		} catch (ScriptingException x) {
		    x.printStackTrace();
		}
	}


	public String getText() {
		return parseTree.getStringValue();
	}

	//public String getHTMLText() {
	//	return converter.getDisplayText(parseTree, false,true);
	//}

	@Override
    public String toString() {
	  return super.toString()+": id="+id+"; type="+type+"; seq="+seq+"; "+parseTree.asXML();
	}
}
