/*
 *  $RCSfile: ScriptManagementDocumentEvent.java,v $
 *
 *  Created on 18 May 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.document;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;

/**
  * Mark script management events which only change the state of elements of
  * the document, not the document text itself.
  *
  * @author Daniel Winterstein
  */

// da: NOTE: I have separated this out.  The main purpose seems to be to update the outline
// view, where we look for exactly these events.  Do any other views make use of the fact
// we subclass DocumentEvent?  I don't think so.  We might be better with simply our own
// events here: document events which don't change the document cause confusion elsewhere.

public class ScriptManagementDocumentEvent extends DocumentEvent {
	public ScriptManagementDocumentEvent(IDocument doc, int offset, int length, String text) {
		super(doc, offset, length, text);
	}
	public ScriptManagementDocumentEvent() {
		super();
	}
	
	public static class ParseChangeEvent extends ScriptManagementDocumentEvent {
		public ParseChangeEvent(IDocument doc, int offset, int length, String text) {
			super(doc, offset, length, text);
		}
		public ParseChangeEvent() {
			super();
		}		
	}
}
