/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jul 6, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */

package ed.inf.proofgeneral.document;

import org.eclipse.jface.text.IDocument;

import ed.inf.proofgeneral.symbols.SymbolTable;

/**
 * A symbol document provides a source of symbols
 */
public interface ISymbolisedDocument extends IDocument {
	public SymbolTable getSymbols();
}
