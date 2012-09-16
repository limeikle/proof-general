/*
 *  $RCSfile: HelpfulController.java,v $
 *
 *  Created on 30 Oct 2006
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.eclipse.customswt;

import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.IWorkbenchHelpSystem;

/**
 * Utility functions for the HelpfulControl interface which allows updating of tooltip and
 * context help text for all instances of HelpfulControl.
 *
 * Implementing classes can provide the following:
 * <pre>
 * class x extends HelpfulControl {
 * ...
 * public void setToolTipText(String text) {
 * 	HelpfulController.setToolTipText(text)
 * }
 * public void setHelp(String helpContextId) {
 * 	HelpfulController.setHelp(helpContextId);
 * }
 * </pre>
 *
 * @see HelpfulControl
 * @author Graham Dutton
 * @author Daniel Winterstein
 */
public class HelpfulController {

	/** Sets the tooltip text for all constituent controls */
    public static void setToolTipText(HelpfulControl hc, String text) {
        for(Control c : hc.getControls()) {
            if (c != null) {
            	c.setToolTipText(text);
            }
        }
    }

	/** Sets the context help for all constituent controls */
    public static void setHelp(HelpfulControl hc, String helpContextId) {
		IWorkbenchHelpSystem helpSystem = PlatformUI.getWorkbench().getHelpSystem();
        for(Control c : hc.getControls()) {
        	try {
        		if (c != null) {
        			helpSystem.setHelp(c, helpContextId);
        		}
        	} catch (Exception ex) {
        		ex.printStackTrace();
        	}
        }

    }
}