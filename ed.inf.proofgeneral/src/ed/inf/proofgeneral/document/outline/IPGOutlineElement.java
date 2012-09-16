/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Mar 24, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */

package ed.inf.proofgeneral.document.outline;

import java.util.List;

import org.eclipse.jface.text.Position;

import ed.inf.proofgeneral.document.ProofScriptDocument;

/**
 * Interface for items in the outline tree.
 */
public interface IPGOutlineElement {
	List<Object> elements();
	ProofScriptDocument getProofScript();
	Position getPosition();
	String getName();
}
