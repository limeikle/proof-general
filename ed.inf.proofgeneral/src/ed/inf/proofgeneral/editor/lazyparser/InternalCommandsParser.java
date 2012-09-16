/*
 *  $RCSfile: InternalCommandsParser.java,v $
 *
 *  Created on 16 Sep 2004
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.lazyparser;

import java.io.StringReader;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.tree.DefaultElement;

import ed.inf.proofgeneral.sessionmanager.PGIPSyntax;
import ed.inf.proofgeneral.sessionmanager.ScriptingException;

/**
 * A parser that looks for xml tags in the text.
 * This is used to find <proofgeneral>... sections, which contain commands for
 * scripting the Proof General interface itself
 * (as opposed to commands for scripting the theorem prover).
 */

// da: this is non-standard so certainly these commands should only alter
// appearance in interface, never affecting provability.  In future
// this mechanism will be subsumed by a master document notion.
// We don't even know if this syntax is good, really (if it is stored
// in proof scripts, it had better be in comments).

public class InternalCommandsParser extends Parser {


	public InternalCommandsParser(PGIPSyntax syntax) {
		super(syntax);
	}

	public boolean isSlow() {
		return false;
	}

	static SAXReader reader = null;

	/* (non-Javadoc)
	 * @see ed.inf.proofgeneral.editor.lazyparser.Parser#parseText(java.lang.String)
	 */
	public Element dumbParseText(String text) throws ScriptingException {
	    if (reader==null) {
	    	reader = new SAXReader();
	    }
	    Document document;
		try {
			document = reader.read( new StringReader(text));
		} catch (DocumentException e) {
			throw new ScriptingException(text+" is not valid as an internal command section.");
		}
		Element result = new DefaultElement(PGIPSyntax.PARSERESULT);
		result.add(document.getRootElement());
		return result;
	}

}
