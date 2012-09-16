/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jan 6, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */

package ed.inf.proofgeneral.pgip;

/**
 * Representation of PGIP messages sent to us from the theorem prover.
 */
public class ProverOutput {

	// TODO: see pgip-isabelle-*output* for stuff we may receive here.
	// NB: messages from the broker are different (display messages).
	// Here we should parse input XML into our representation, validating
	// along the way.  The underlying type (even PGIPInput itself) can be an Enum,
	// which is useful because we'll need to switch on it.
	
}
