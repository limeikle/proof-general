/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jul 21, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */

package ed.inf.proofgeneral.sessionmanager.events;

import ed.inf.proofgeneral.document.DocElement;

public class UndoSent extends InternalEvent {
	public UndoSent(DocElement undone) {
		super(undone,null);
	}
}