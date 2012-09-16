/*
 *  $RCSfile: ShowView.java,v $
 *
 *  Created on 11 Dec 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.actions;

import org.eclipse.jface.action.Action;
import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.utils.eclipse.ErrorUI;

/**
 * Open a named Eclipse view.
 * @author Daniel Winterstein
 */
public class ShowView extends Action {
    String id;

    public ShowView(String viewId) {
        super();
        id = viewId;
    }

    @Override
    public void run() {
        try {
	        ProofGeneralPlugin.getDefault().getWorkbench()
	        	.getActiveWorkbenchWindow()
	        	.getActivePage().showView(id);
        } catch (Exception x) {
            if (ProofGeneralPlugin.debug(this)) {
            	x.printStackTrace();
            }
            Exception x2 = new Exception("Error opening/displaying "+id
                    +"\n"+x.getMessage()+"\nNote: View ids must be fully qualified Eclipse ids. See ed.inf.proofgeneral/plugin.xml for the basic PG view ids.");
    		ErrorUI.getDefault().signalError(x2, true);
        }
    }
}
