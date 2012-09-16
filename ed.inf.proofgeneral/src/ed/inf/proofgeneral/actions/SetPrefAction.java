/*
 *  $RCSfile: SetPrefAction.java,v $
 *
 *  Created on 21 Sep 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.actions;

import org.eclipse.jface.action.Action;

import ed.inf.proofgeneral.document.CmdElement;
import ed.inf.proofgeneral.preferences.ProverPreferenceNames;
import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.utils.eclipse.ErrorUI;

/**
 * Set an external prover preference.
 */
public class SetPrefAction extends Action {

	CmdElement command;
	SessionManager smgr;

	/**
	 * Make an action to set a preference in the prover.  
	 * Strips off the prover name and configuration prefix if found
	 * (see {@value ProverPreferenceNames#PROVER_INTERNAL_PREFERENCE_NAME_TAG}). 
	 * @param sm
	 * @param name
	 * @param value
	 */
	public SetPrefAction(SessionManager sm,String name,Object value) {
		this.smgr = sm;
		name = stripPrefix(name);
		command = new CmdElement("setpref");
		//command.add(new DefaultAttribute("name", name));
        command.setAttributeValue("name",name);
        command.setText(value.toString());
	}

	@Override
    public void run() {
		try {
			smgr.queueCommand(command);
		} catch (Exception e) {
			e.printStackTrace();
			ErrorUI.getDefault().signalError(e, true);
		}
	}

	/**
	 * Strip out the standard preference prefix for prover internal preferences, if there is one
	 * @param prefName
	 * @return a preference string without the prefix
	 */
	public String stripPrefix(String prefName) {
		String prefix = smgr.proverInfo.name+ProverPreferenceNames.PROVER_INTERNAL_PREFERENCE_NAME_TAG;
		if (prefName.startsWith(prefix)) {
			return prefName.substring(prefix.length());
		}
		return prefName;
	}

}
