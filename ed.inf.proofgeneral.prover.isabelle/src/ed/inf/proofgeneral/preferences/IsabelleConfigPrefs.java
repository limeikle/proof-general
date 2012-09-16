/*
 *  $RCSfile: IsabelleConfigPrefs.java,v $
 *
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.preferences;

import ed.inf.proofgeneral.prover.isabelle.Activator;

public class IsabelleConfigPrefs extends PrefsPageBackend { 
	public IsabelleConfigPrefs() {
		super(Activator.PROVER_NAME);
	}
}
