/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jul 25, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */

package ed.inf.proofgeneral.editor.actions.retargeted;

/**
 * Actions implementing this interface provide a method for clearing
 * their busy state.
 * @author da 
 */
public interface IClearableAction {
	/**
	 * Clear this action's busy state.
	 */
	public void clearBusy();
}
