/**
 * 
 */
package ed.inf.proofgeneral.prover.isabelle.wizards;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.ProofGeneralProverRegistry.Prover;
import ed.inf.proofgeneral.ProofGeneralProverRegistry.ProverRegistryException;
import ed.inf.proofgeneral.prover.isabelle.Activator;
import ed.inf.proofgeneral.wizards.NewProofProjectWizard;

public class NewIsabelleProjectWizard extends NewProofProjectWizard {
	@Override
	public Prover getProver() throws ProverRegistryException {
		return ProofGeneralPlugin.getRegistry().getProver(Activator.PROVER_NAME);
	}
}
