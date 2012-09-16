package ed.inf.proofgeneral.prover.pml.wizards;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.ProofGeneralProverRegistry.Prover;
import ed.inf.proofgeneral.ProofGeneralProverRegistry.ProverRegistryException;
import ed.inf.proofgeneral.wizards.NewProofProjectWizard;

public class NewPMLProjectWizard extends NewProofProjectWizard {
	@Override
	public Prover getProver() throws ProverRegistryException {
		return ProofGeneralPlugin.getRegistry().getProver("PML");
	}
}
