/*
 *  $RCSfile: ProofScriptEditorActionContributor.java,v $
 *
 *  Created on 09 Apr 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.actions;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.editors.text.TextEditorActionContributor;

import ed.inf.proofgeneral.Constants;
import ed.inf.proofgeneral.editor.ProofScriptEditor;

/**
 * Manages retarget of actions for the Proof Script editor.
 * 
 * @author Daniel Winterstein
 * @author David Aspinall
 */
public class ProofScriptEditorActionContributor extends TextEditorActionContributor { 

	private void doSetActiveEditor(IEditorPart part) {

		ProofScriptEditor pse= null;
		if (part instanceof ProofScriptEditor) {
			pse= (ProofScriptEditor) part;
		}

		IActionBars actionBars= getActionBars();

		for (String actionId : Constants.getRetargetActionIds()) {
			actionBars.setGlobalActionHandler(actionId, getAction(pse,actionId));
		}
	}
	
	@Override
    public void setActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);
		doSetActiveEditor(part);
	}
	
}

