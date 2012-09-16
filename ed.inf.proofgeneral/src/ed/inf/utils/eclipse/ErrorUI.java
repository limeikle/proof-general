/*
 *  $RCSfile: ErrorUI.java,v $
 *
 *  Created on 02 Aug 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.eclipse;
import java.awt.Toolkit;

import org.eclipse.jface.dialogs.MessageDialog;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.sessionmanager.UserCancelException;

/**
 * Handles Error UI stuff for session Manager
 * ('cos I want to isolate sessionManager from Eclipse as much as possible).
 * Created by ProofGeneralPlugin, which also hooks it up to session manager.
 * TODO let the user set some of this behaviour?
 * @author Daniel Winterstein
 */
// da: CLEANUP candidate for removal/joining with another class (e.g. EclipseMethods)
public class ErrorUI {

    private static ErrorUI def = null;

    public static ErrorUI getDefault() {
        if (def==null) {
        	def = new ErrorUI();
        }
        return def;
    }

    /**
     * Default constructor. Does nothing special.
     */
    public ErrorUI() {
        super();
    }

    /**
     * Inform the user about an error.
     * Ignores UserCancelException, since these result from the user's own intervention.
     * @param e the exception
     * @param displayDialog whether to display a dialog; if false, just shows the log/latest view
     */
    public void signalError(Exception e, final boolean displayDialog) {
        if (e instanceof UserCancelException) {
        	return;
        }
        if (displayDialog) {
        	Toolkit.getDefaultToolkit().beep();
        	EclipseMethods.errorDialog(e); // dialog to tell the user
        }
    }

    /**
     * Inform the user about a warning (ie. an ignorable error).
     * @param e
     */
    public void signalWarning(Exception e) {
    	if (!ProofGeneralPlugin.isEclipseMode()) {
    		System.err.println("WARNING: "+e);
    		if (ProofGeneralPlugin.debug(this)) {
    			e.printStackTrace();
    		}
    		return;
    	}

        Toolkit.getDefaultToolkit().beep();
        final String[] ok = {"OK"};
        final String fmsg = e.getMessage();
        org.eclipse.swt.widgets.Display.getDefault().asyncExec(
    			new Runnable() {
					public void run() {
						EclipseMethods.messageDialog("Warning!",
						        fmsg,ok,true,MessageDialog.WARNING);
					}
    			});
    }

	/**
	 * @param e the exception to signal.
	 */
	public void signalError(Exception e) {
		signalError(e, true);
	}

}
