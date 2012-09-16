/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on 1 Oct 2007 by alex
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */

package ed.inf.proofgeneral.sessionmanager;

import ed.inf.utils.exception.KnownException;

/**
 *
 */
public class ProverDeadException extends KnownException {

	/**
     * @param command
     * @param message
     */
    public ProverDeadException() {
	    super("The prover is not accessible");
    }

}
