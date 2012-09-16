/*
 *  $RCSfile: ToggleSymbolsAction.java,v $
 *
 *  Created on 21 Apr 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.actions.defunct;


import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.editor.ProofScriptEditor;
import ed.inf.proofgeneral.editor.actions.defunct.PGAction;

/**
 * Toggle symbol use on or off.
 */
public class ToggleSymbolsAction extends PGAction {

	public ToggleSymbolsAction() {
		this.setToolTipText("Toggle symbol use for this document.");
		this.setText("Use symbols");
		this.setDescription("Toggle symbol use for this document.");
		activeDocumentChangeListener();
	}

	// TODO: hook this up to action bar!
	private void activeDocumentChangeListener() {
		ProofScriptEditor pse = getActiveEditor();
		if (pse != null) {
			ProofScriptDocument doc = pse.getDocument();
			if (doc != null) {
				this.setEnabled(true);
				this.setChecked(doc.isUsingSymbols());
				return;
			}
		}
		// don't do this until we have listener working
		//this.setEnabled(false);
	}

	@Override
    public void run() {
		try {
			final ProofScriptDocument doc = getAndAssociateDocument();
			if (doc == null) {
				return;  // shouldn't happen ordinarily
			}
			final ProofScriptEditor ed = getAssociatedEditor();
			if (ed == null) {
				return;  // shouldn't happen ordinarily
			}
			final boolean oldUsingSymbols = doc.isUsingSymbols();
			IWorkspaceRunnable r = new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) {
					ed.setUsingSymbols(!oldUsingSymbols);
				}
			};
			
			try {
				IResource res = doc.getResource();
				res.getWorkspace().run(r, res, IWorkspace.AVOID_UPDATE, null);
				
				if (doc.isUsingSymbols()) {
					setToolTipText("Show the document with symbols.");  // for next time
					setText("Use Symbols");
					setChecked(false);
				} else {
					setToolTipText("Show the document in ASCII.");      // for next time
					setText("Use ASCII");
					setChecked(false);
				}
			} catch (CoreException e) {
				e.printStackTrace(); // FIXME workbench log it
			}
		} catch (Exception e2) {
			e2.printStackTrace();
			error(e2);
		}
	}
}
