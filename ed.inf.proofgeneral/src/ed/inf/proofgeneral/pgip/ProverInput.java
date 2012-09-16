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
 * Representation of PGIP messages which we construct and send to the theorem prover.
 */
public class ProverInput {

	// TODO: see pgip-isabelle-*input*
	// Mostly to be taken from PGIPSyntax.java.  We should have methods
	// for constructing well-formed messages of each kind, but the
	// underlying element kind can be an Enum -- although note that
	// we shouldn't need to iterate/switch over these things ourselves,
	// we make them then send on.
}
