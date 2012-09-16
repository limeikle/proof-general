/*
 *  $RCSfile: ParseDocAction.java,v $
 *
 *  Created on 21 Apr 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.actions.retargeted;

import org.eclipse.jface.resource.ImageDescriptor;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.editor.ProofScriptEditor;
import ed.inf.proofgeneral.editor.lazyparser.Parser;
import ed.inf.proofgeneral.sessionmanager.ScriptingException;
import ed.inf.proofgeneral.sessionmanager.SessionManager;

/**
 * Parse the entire current doc
 */
public class ParseDocAction extends PGRetargetableAction {

	public ParseDocAction(ProofScriptEditor editor) {
		super(editor);
		ImageDescriptor imd = ProofGeneralPlugin.getImageDescriptor("icons/tree.gif");
		this.setImageDescriptor(imd);
		this.setToolTipText("Parse script");
		this.setDescription("Parse the script");
	}

	@Override
    public void run() {
		if (isBusy()) {
			return;
		}
		setBusy();
		try {
			ProofScriptDocument doc = getDocumentForRunningAction();
			SessionManager sm = getSessionManagerForRunningAction();
			Parser parser = sm.getParser();
			BgParse bgParse = new BgParse(parser,doc);
			bgParse.start();
		} catch (Exception e) {
			e.printStackTrace();
			error(e,true);
		} finally {
			clearBusy();
		}
	}

	class BgParse extends Thread {

		Parser parser;

		ProofScriptDocument doc;
		
		BgParse(Parser parser,ProofScriptDocument doc) {
			super();
			this.parser = parser;
			this.doc = doc;
		}
		
		@Override
        public void run() {
			try {
				parser.setCause(ParseDocAction.this);
				// FIXME: partial parse is faulty, we must solve problem of orphaned empty elements
				//parser.parseDoc(doc,doc.getParseOffset()+1);
				parser.parseDoc(doc,0);
				
			} catch (ScriptingException e) {
				// Let's not bug the user: errors will be seen in the editor
				//e.goToError();
				//error(e);
			} catch (Exception e) {
				e.printStackTrace();
				error(e);
			} finally {
				parser.setCause(null);
		  }
		}
	}


}
