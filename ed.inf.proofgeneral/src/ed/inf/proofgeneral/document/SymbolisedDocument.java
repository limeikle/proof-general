/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Mar 17, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */

package ed.inf.proofgeneral.document;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.Position;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.preferences.PreferenceNames;
import ed.inf.proofgeneral.symbols.SymbolTable;

/**
 * SymbolisedDocument: provide mechanism for converting character sequences (usually plain ASCII)
 * into other character sequences (usually single Unicode characters) using the 
 * symbol table mechanism provided by @{link ed.inf.proofgeneral.symbols.HTMLSymbols}.
 * 
 * TODO: ideally this document should mediate between the symbolised version
 * and desymbolised version by maintaining both texts instead of converting
 * between them (this seems expensive but is probably reasonable in the grand scheme of
 * things).  We should maintain a mapping here of the offset changes to help do that.
 * Or we may be able to use some existing view infrastructure in Eclipse for this.
 * Either way, it is a big nuisance (especially if it is only wanted by Isabelle
 * and other provers may use Unicode directly).
 * 
 * The LastSaveReferenceProvider is also a nuisance here: it should track the symbolised
 * state of the master document.
 * 
 * @author David Aspinall
 */
public class SymbolisedDocument extends Document implements ISymbolisedDocument {


	/** Whether we are currently using symbols in this document or not. */
	private boolean usingSymbols = false;
	
	/** Temporary empty table used before init() is called */
	private static final SymbolTable emptySymbols = new SymbolTable();
	
	/** The symbols we're using */ 
	private SymbolTable symbols = emptySymbols;

    public final static String SYMBOL_REPLACEMENT_POSITION_CATEGORY = "SYMBOL_REPLACE";
    
	/**
     * 
     */
    public SymbolisedDocument() {
	    super();
    	addPositionCategory(SYMBOL_REPLACEMENT_POSITION_CATEGORY);
    }
    
    /**
     * Synchronize the symbol support flag with the current preference value.
     * This is used by @{link LastSaveReferenceProvider} to allow for changes
     * in the symbol preference flag.
     */
    public void syncSymbolSupport() {
	    usingSymbols = ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_SYMBOL_SUPPORT);
    }

	/**
	 * @return the usingSymbols
	 */
	public boolean isUsingSymbols() {
		return usingSymbols;
	}

	/**
	 * Switch symbol usage in this document on or off.
	 * @param newusingSymbols the usingSymbols to set
	 */
	public void setUsingSymbols(boolean newusingSymbols) {
		if (usingSymbols != newusingSymbols) {
			usingSymbols = newusingSymbols;

			// Set the global preference value to track the change in the latest changed
			// document.  This is perhaps not so desirable without changing *all*
			// documents to match, but it is (1) reasonably intuitive and (2)
			// causes the last save reference provider to pick up the correct
			// symbolisation when it reloads.  The fact that we don't clear
			// the dirty state actually suggests to the user that the symbolised
			// version is being saved but this should never happen.
			// An improvement would be to clear the dirty status (need to access
			// document provider for that) and also change the lastsavereference
			// somehow.  But all a bit fiddly so should be saved for low-priority
			// fine tuning!
			
			ProofGeneralPlugin.getStaticPreferenceStore().setValue(PreferenceNames.PREF_SYMBOL_SUPPORT, newusingSymbols);

			if (newusingSymbols) {
				symboliseAndMakeMap();
			} else {
				deSymboliseAndClearMap();
			}
		}
	}

	
	/**
	 * Set the document text, possibly converting to symbols
	 * from the raw ASCII argument.  
	 * @param text
	 */
	@Override
    public void set(String text) {
		String symbolised = symbolise(text);
		super.set(symbolised);
	}
	
	
	/**
	 * Change ASCII symbol sequences into Unicode if the document
	 * is using symbols.
	 * Does not substitute (or contract) typing shortcuts, because they can be ambiguous.
	 * @param text
	 * @return a string containing substituted Unicode symbols.
	 */
	public String symbolise(String text) {
		if (usingSymbols &&
				// shouldn't need to test for null, but e.g. delete-to-end-of-line
				// uses replace null in eclipse < 3.3, see:
				// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=123895
				text != null) {
			text = rawSymbolise(text);
		}
		return text;
	}

	/**
	 * @param text - non-null
	 * @return unicode converted form of input
	 */
	private String rawSymbolise(String text) {
		return symbols.useUnicodeForDocument(text);
	}

	/**
	 * Change our special Unicode symbols into ASCII.
	 * The inverse of symbolise()
	 *
	 * @param text
	 * @return a string containing substituted ASCII sequences in place of Unicode symbols.
	 */
	private String desymbolise(String text) {
		return symbols.useAscii(text);
	}

	/**
	 * Return the contents of the document.
	 * The text will be converted from Unicode symbols into
	 * ASCII if the document is using symbols.
	 *
	 * @see DocumentEvent
	 *  @see org.eclipse.jface.text.IDocument#get()
	 */
	public String getDesymbolised() {
		return ( usingSymbols ) ? desymbolise(super.get()) : super.get();
	}

	private Position[] symbolReplacementMap; 
	private int[] symbolReplacementMapLengths;

	private void symboliseAndMakeMap() {
		TextEdit edits = symbols.symboliseEdits(this);
		symbolReplacementMapLengths = new int[edits.getChildrenSize()];
		int i = 0;
		for (TextEdit symreplace : edits.getChildren()) {
			symbolReplacementMapLengths[i++] = ((ReplaceEdit)(symreplace)).getRegion().getLength();
		}
		try {
			edits.apply(this);
		} catch (BadLocationException ex) {
			// do nothing for now
		}
		try {
			removePositionCategory(SymbolisedDocument.SYMBOL_REPLACEMENT_POSITION_CATEGORY);
		} catch (BadPositionCategoryException ex) {
			// shouldn't happen
		}
		addPositionCategory(SymbolisedDocument.SYMBOL_REPLACEMENT_POSITION_CATEGORY);
		symbolReplacementMap = new Position[edits.getChildrenSize()];
		i = 0;
		for (TextEdit symreplace : edits.getChildren()) {
			Position symreplacepos = new Position(symreplace.getOffset(),symreplace.getLength());
			try {
				addPosition(SymbolisedDocument.SYMBOL_REPLACEMENT_POSITION_CATEGORY, symreplacepos);
				symbolReplacementMap[i++] = symreplacepos;
			} catch (BadLocationException ex) {
				// ignore
			} catch (BadPositionCategoryException ex1) {
				// impossible
			}
		}
	}
	
	private void deSymboliseAndClearMap() {
		TextEdit edits = symbols.desymboliseEdits(this);		
		try {
			edits.apply(this);
		} catch (BadLocationException ex) {
			// do nothing for now
		}
		try {
			removePositionCategory(SymbolisedDocument.SYMBOL_REPLACEMENT_POSITION_CATEGORY);
		} catch (BadPositionCategoryException ex) {
			// shouldn't happen
		}
		symbolReplacementMap = null;
	}
	
	public int getTransformedOffset(int fileOffset) {
		if (!usingSymbols || symbolReplacementMap == null) {
			return fileOffset; // no difference or no mapping available
		}
		int displacement = 0;
		// Linear search through map to find transformed offset.
		for (int i = 0; i<symbolReplacementMap.length; i++) {
			Position sympos = symbolReplacementMap[i];
			if (sympos.getOffset() > fileOffset + displacement) {
				return fileOffset + displacement;
			}
			displacement += sympos.getLength() - symbolReplacementMapLengths[i];
		}
		return fileOffset + displacement;
	}

	/**
     * @return the symbols
     */
    public SymbolTable getSymbols() {
    	return symbols;
    }

	/**
	 * Initialise the document: set the symbol table being used, synchronize
	 * symbol usage with the preference, symbolise if required.
     * @param symbols the symbols to set
     */
    public void init(SymbolTable symbols) {
    	this.symbols = symbols;
	    syncSymbolSupport();
	    if (usingSymbols) {
	    	symboliseAndMakeMap();
	    }
    }
	
}
