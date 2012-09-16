/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jul 23, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */

package ed.inf.proofgeneral.document;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;

/**
 * Positions used for script management.
 * 
 * @author David Aspinall
 */
public class ScriptManagementPosition extends Position implements ITypedRegion {

	private static final String SCRIPT_MANAGEMENT_POSITION_CATEGORY = "SCRIPT_MANAGEMENT_POSITION";

	private final String type;
	
	public ScriptManagementPosition(String type, int offset, int length) {
		super(offset,length);
		this.type = type;
	}

	/**
     * @return the type
     */
    public String getType() {
    	return type;
    }
    
    
    /**
     * Initialise the given document with position management
     */
    public static void init(ProofScriptDocument doc, ScriptManagementPosition[] positions) {
    	doc.addPositionCategory(SCRIPT_MANAGEMENT_POSITION_CATEGORY);
    	IPositionUpdater updater = new OpenStartPositionUpdater(SCRIPT_MANAGEMENT_POSITION_CATEGORY);
    	doc.addPositionUpdater(updater);
    	for (ScriptManagementPosition pos : positions) {
    		try {
    			doc.addPosition(SCRIPT_MANAGEMENT_POSITION_CATEGORY,pos);
    		} catch (BadPositionCategoryException e) {
    			// should really not happen, we've just added it a second ago!
    			System.err.println("Unexpected exception during document initialisation:");
    			e.printStackTrace();
    		} catch (BadLocationException e) {
    			// should not happen: the initial value of positions should be allowed
    			// in the document
    			System.err.println("Unexpected exception during document initialisation:");
    			e.printStackTrace();
    		}
    	}
    }
}
