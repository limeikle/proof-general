/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jun 12, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */

package ed.inf.proofgeneral.prover.isabelle.wizards;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import ed.inf.proofgeneral.wizards.NewProofScriptWizard;
import ed.inf.proofgeneral.wizards.NewProofScriptWizardPage;

/**
 *
 */
public class NewIsabelleProofScriptWizard extends NewProofScriptWizard {
	/**
	 * Adding the page to the wizard.
	 */
	@Override
    public void addPages() {
		page = new NewProofScriptWizardPage(selection, "thy", "NewTheory", "Theory");
		addPage(page);
	}

	@Override
    public InputStream openContentStream(String basename) {
		return new ByteArrayInputStream(getDefaultContentsForTheoryName(basename).getBytes());
	}
	
	public static String getDefaultContentsForTheoryName(String basename) {
		return "theory "+basename+" imports Main\n"+
			"begin\n\n" +
			"end\n";
	}
}
