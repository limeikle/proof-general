/*
 *  $RCSfile: BugReportAction.java,v $
 *
 *  Created on 21 Apr 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.actions;

import ed.inf.proofgeneral.ProofGeneralPlugin;

/**
 * A menu item to open the bug report web page
 */
public class BugReportAction extends LaunchBrowserAction {
	
	public BugReportAction() {
		super("Bug Report", "It's not a bug it's a feature","Report a new bug",
				ProofGeneralPlugin.getDefault().getBugReportURL());
	}

}
