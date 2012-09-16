/*
 *  $RCSfile: PreferenceInitializer.java,v $
 *
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.preferences;

import java.util.Collection;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.ProofGeneralProverRegistry.Prover;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	@Override
    public void initializeDefaultPreferences() {
		// Initialise the generic preferences. 
		// Prover-specific preferences are initialised when we load a prover plugin.
		Preferences store = ProofGeneralPlugin.getDefault().getPluginPreferences();
		PrefsPageBackend.initializePGPrefs(store,"ed.inf.proofgeneral",null);
		
		Collection<Prover> provers = ProofGeneralPlugin.getRegistry().getConnectedProvers();
		for (Prover p : provers) {
			Preferences pstore = p.getProverPlugin().getPluginPreferences();
			PrefsPageBackend.initializePGPrefs(pstore, p.getPluginId(), p.getName());
			// Initialise the default symbol table now we know where to find it.
			// TODO: the project symbol table ought to override this, we should
			// read that instead if it's set.  Needs some reworking
			p.getSymbols().init(p);
		}
	}
}

