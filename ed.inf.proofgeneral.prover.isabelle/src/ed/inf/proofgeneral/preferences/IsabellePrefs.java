package ed.inf.proofgeneral.preferences;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.prover.isabelle.Activator;

public class IsabellePrefs extends PrefsPageBackend {
	public IsabellePrefs() { super(Activator.PROVER_NAME); }
	
	/**
	 * Sets the launch command directly if the preference changes, making sure any setting
	 * configured with the Choose Logic action (which sets the special launch command)
	 * is removed. 
	 */
	@Override
    public void propertyChange(PropertyChangeEvent event) {
		try {
			FieldEditor fe = (FieldEditor) event.getSource();
			String pName = fe.getPreferenceName().intern();
			if (pName.indexOf(ProverPreferenceNames.PREF_PROVER_START_COMMAND)>=0) {
				// user changed start command; remove any special start command set
				if (!modifiedPrefs.contains(pName)) {
					ProofGeneralPlugin.getSessionManagerForProver("Isabelle").proverInfo
						.setSpecialLaunchCommand(null);
				}
			}
		} catch (Exception e) {
			System.err.println("error recording change of 'Start Command':");
			if (ProofGeneralPlugin.debug(this)) {
				e.printStackTrace();
			}
		}
		super.propertyChange(event);
	}
}
