/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jul 20, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */

package ed.inf.proofgeneral.sessionmanager;

import java.util.Observer;

/**
 * A tagging interface for observers for ProverState.
 * Observers for ProverState are guaranteed to have their
 * update method called with a ProverState object as their Observable
 * parameter.
 */
public interface IProverStateObserver extends Observer {

}
