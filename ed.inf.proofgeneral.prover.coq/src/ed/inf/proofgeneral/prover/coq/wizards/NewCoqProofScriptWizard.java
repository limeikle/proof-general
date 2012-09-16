/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jun 12, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */

package ed.inf.proofgeneral.prover.coq.wizards;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import ed.inf.proofgeneral.wizards.NewProofScriptWizard;
import ed.inf.proofgeneral.wizards.NewProofScriptWizardPage;

/**
 *
 */
public class NewCoqProofScriptWizard extends NewProofScriptWizard {
	/**
	 * Adding the page to the wizard.
	 */
	@Override
    public void addPages() {
		page = new NewProofScriptWizardPage(selection, "v", "NewModule", "Module");
		addPage(page);
	}
	
	@Override
    public InputStream openContentStream(String basename) {
		String contents =
			"Module "+basename+". \n\n"+
			"End "+basename+".";
		return new ByteArrayInputStream(contents.getBytes());
	}
}
