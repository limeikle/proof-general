/*
 *  $RCSfile: OpenBookmarkAction.java,v $
 *
 *  Created by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.actions;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.ide.IGotoMarker;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.utils.eclipse.ErrorUI;

/**
 * Given a marker, open an editor on it.
 * TODO: test, use
 * @author Daniel Winterstein
 */
public class OpenBookmarkAction extends Action {

    public OpenBookmarkAction(IMarker marker) {
        this.marker = marker;
    }

    IMarker marker;
    
    /**
     * Run action to open a bookmark
     */
    @Override
    public void run() {
    	IEditorPart editor;
        try {
	        // IProofScriptEditor e = (IProofScriptEditor) ProofGeneralPlugin.getActiveEditor();
	        IWorkbenchPage page = ProofGeneralPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage();
	        // IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
	        editor = org.eclipse.ui.ide.IDE.openEditor(page,marker,false);
	    	IGotoMarker gotoMarker = (IGotoMarker) editor.getAdapter(IGotoMarker.class);
	    	gotoMarker.gotoMarker(marker);
        } catch (Exception x) {
            x.printStackTrace();
            ErrorUI.getDefault().signalError(x, true);
        }
    }

}
