/*
 *  $RCSfile: MixedParser.java,v $
 *
 *  Created on 16 Sep 2004
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.lazyparser;

import java.util.ListIterator;

import org.dom4j.Element;
import org.eclipse.jface.text.ITypedRegion;

import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.sessionmanager.PGIPSyntax;
import ed.inf.proofgeneral.sessionmanager.ScriptingException;
import ed.inf.proofgeneral.sessionmanager.SessionManager;

/**
 * Combines External and Internal parsers.
 * The external does most stuff.
 * The internal looks for interface script commands inside comment blocks.
 *
 */
public class MixedParser extends Parser {

	@Override
    public boolean isSlow() {
		return true;
	}

	Parser external;
	Parser internal;

	public MixedParser(SessionManager sm) {
		super(sm.proverInfo.syntax);
	    external = new ExternalLazyParser(sm);
	    internal = new InternalCommandsParser(syntax);
	}

	@Override
    public Element dumbParseText(String text) throws ScriptingException, ParsingInterruptedException {
		try {
			Element parse = external.dumbParseText(text);
			parse = secondParse(parse);
			return parse;
		} catch (ScriptingException ex) {
			return internal.dumbParseText(text);
		}
	}
	/* (non-Javadoc)
	 * @see ed.inf.proofgeneral.editor.lazyparser.Parser#parseText(java.lang.String, ed.inf.proofgeneral.editor.ProofScriptDocument, int)
	 */
	@Override
    public Element parseText(String text, ProofScriptDocument doc, int offset)
			throws ScriptingException, ParsingInterruptedException {
	    if (doc==null) {
	    	return dumbParseText(text);
	    }
		try {
			ITypedRegion partition = doc.getPartition(offset);
			String pText; // FIXME surely we must continue until all of text is parsed?
			try {
			    pText = text.substring(0,partition.getLength()); // removed a -1 from length 'cos i could not understand why it was there, and it seems to give errors
			} catch (StringIndexOutOfBoundsException ex) {
			    pText = doc.get(offset,partition.getLength());
			    assert pText.indexOf(text) == 0 : "Error in partition-based parsing: partition text did not match with expected text";
			}
			if (syntax.subType(partition.getType(),PGIPSyntax.PGTAG.name)) {
			    return internal.parseText(pText,doc,offset);
			} else if (syntax.subType(partition.getType(),PGIPSyntax.COMMENT)) {
			    return internal.parseText(pText,doc,offset);
			} else {
				Element parse = external.parseText(pText,doc,offset); // TODO something that works
				parse = secondParse(parse);
				return parse;
			}
		} catch (Exception ex) {
		    throw new ScriptingException(ex.getMessage());
		}
	}

	/**
	 * Run comment partitions through the internal parser, looking for pgtags
	 * @param parse the element to parse for pgtags
	 * @return the parsed element
	 */
	@SuppressWarnings("unchecked")
	public Element secondParse(Element parse) {
		for(ListIterator<Element> i = parse.elements().listIterator(); i.hasNext();) {
			Element e = i.next();
			String eName = e.getName();
			if (syntax.subType(eName,PGIPSyntax.COMMENT)) {
				try {
					Element p2 = internal.dumbParseText("<"+eName+">"+e.getText()+"</"+eName+">");//asXML());
					assert p2 != null && p2.elements().size()==1 : "Something wrong in secondParse";
					Element comment = (Element) p2.elements().get(0);
					if (comment.elements().size()==0) {
						continue;
					}
					comment.setAttributes(e.attributes());
					i.remove();			// replace the old comment with this new one
					i.add(comment);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		return parse;
	}

}
