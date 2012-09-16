/*
 *  $RCSfile: ToggleSymbolsAction.java,v $
 *
 *  Created on 21 Apr 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.actions.retargeted;


import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.texteditor.ITextEditor;

import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.editor.ProofScriptEditor;

/**
 * Toggle symbol use on or off.
 */
public class ToggleSymbolsAction extends PGRetargetableAction {

	public ToggleSymbolsAction(ProofScriptEditor editor) {
		super(editor);
		this.setToolTipText("Toggle symbol use for this document.");
		this.setText("Use symbols");
		this.setDescription("Toggle symbol use for this document.");
	}

	@Override
    public void setEditor(ITextEditor editor) {
		super.setEditor(editor);
		ProofScriptEditor pse = super.getEditor();
		if (pse != null) {
			ProofScriptDocument doc = pse.getDocument();
			if (doc != null) {
				this.setEnabled(true);
				this.setChecked(doc.isUsingSymbols());
				return;
			}
		}
		this.setEnabled(false);
	}

	@Override
    public void run() {
		setBusy();
		try {
			final ProofScriptDocument doc = getDocumentForRunningAction();
			final ProofScriptEditor ed = getEditorForRunningAction();
			if (ed == null || doc == null) {
				return;  // shouldn't happen ordinarily
			}
			final boolean oldUsingSymbols = doc.isUsingSymbols();
			final boolean newUsingSymbols = !oldUsingSymbols;
			IWorkspaceRunnable r = new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) {
					ed.setUsingSymbols(newUsingSymbols);
				}
			};
			
			try {
				IResource res = doc.getResource();
				res.getWorkspace().run(r, res, IWorkspace.AVOID_UPDATE, null);
				
				if (newUsingSymbols) {
					setChecked(true);
					setToolTipText("Show the document in ASCII.");      // for next time
					// setText("Use ASCII");  Label change on toggle in menu is wrong
				} else {
					setChecked(false);
					setToolTipText("Show the document with symbols.");  // for next time
					// setText("Use Symbols");
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
