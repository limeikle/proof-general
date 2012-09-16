/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jan 13, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *
 */

package ed.inf.proofgeneral.document;

/**
 * Indicates an error in manipulating the document, for example, an attempt
 * to lock for processing beyond the parse limit, or lock for undoing
 * beyond the processed limit.
 * This exception should be prevented by the Session Manager and UI code,
 * but caught there just in case, to report as an internal error.
 */
public class ProofScriptDocumentException extends Exception {

	/**
	 * @param string
	 */
	public ProofScriptDocumentException(String string) {
		super(string);
	}
}
