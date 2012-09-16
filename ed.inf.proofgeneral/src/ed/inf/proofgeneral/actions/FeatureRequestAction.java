/*
 *  $RCSfile: FeatureRequestAction.java,v $
 *
 *  Created on 21 Apr 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.actions;

import ed.inf.proofgeneral.ProofGeneralPlugin;

/**
 * Send the user to the feature request page.
 * @author Daniel Winterstein
 */
public class FeatureRequestAction extends LaunchBrowserAction {

	public FeatureRequestAction() {
		super("Feature requests", "Edit feature requests","Add or edit a request for a new Proof General Feature.",
				ProofGeneralPlugin.getDefault().getHelpURL());
	}
	
}
