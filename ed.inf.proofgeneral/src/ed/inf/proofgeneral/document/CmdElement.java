/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Mar 24, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */

package ed.inf.proofgeneral.document;

import org.dom4j.util.UserDataElement;
import org.eclipse.jface.text.TypedPosition;

/**
 * Commands for sending to the prover, represented as XML elements.
 * Possibly associated with a document and position for taking actions
 * on command completion.
 * 
 * This used to be part of DocElement, we are refactoring to remove
 * confusion between outgoing elements and incoming ones, and ones
 * that are part of the document model. 
 */
public class CmdElement extends UserDataElement { // FIXME: will become extends UserDataElement just

	private final String cmdtype;
	
	private final ProofScriptDocument doc;
	
	private final TypedPosition pos;

	
	/**
	 * Create a command for sending to the prover, recording the given document and
	 * typed position.  The string argument is the command type, which will be returned
	 * by getCmddType()  
	 */
	public CmdElement(String cmdtype, ProofScriptDocument doc, TypedPosition pos) {
		super(cmdtype);
		this.cmdtype = cmdtype;
		this.doc = doc;
		this.pos = pos;
	}
		
	/**
	 * Create a command for sending to the prover, not associated with any document
	 * or position.
	 */
	public CmdElement(String cmdtype) {
		super(cmdtype);
		this.cmdtype = cmdtype;
		this.doc = null;
		this.pos = null;
	}

	/**
     * @return the doc
     */
    public ProofScriptDocument getProofScript() {
    	return doc;
    }

	/**
	 * Return the position associated with this command.
	 * This will be non-null if the command is associated with some script management change
	 * for a queue region in the document (e.g. a command sent from the document or an undo command for the document).
     * @return the position
     */
    public TypedPosition getPosition() {
    	return pos;
    }
    
    public String getCmdType() {
    	return cmdtype;
    }
    
	/**
	 * @return the offset of the last character in this element
	 */
	public int getEndOffset() {
		return this.getPosition().getOffset() + this.getPosition().getLength() - 1;
	}

}
