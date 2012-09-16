/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Mar 22, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */

package ed.inf.proofgeneral.document;

import org.eclipse.jface.text.DefaultPositionUpdater;

/**
 * This class implements a variant of {@link DefaultPositionUpdater} which
 * does not push regions forward when edits occur right at their start, 
 * and does not extend regions when inserts occur right at their end.
 * i.e., the interval is (...] instead of the default [...).
 * Special treatment is given to zero length positions: these are never
 * extended and only moved forward for edits strictly before their start.
 */
public class OpenStartPositionUpdater extends DefaultPositionUpdater {

	/**
     * @param category
     */
    public OpenStartPositionUpdater(String category) {
	    super(category);
    }
    
	/**
	 * Adapts the currently investigated position to an insertion.
	 */
	@Override
    protected void adaptToInsert() {

		int myStart= fPosition.offset;
		int myEnd=   fPosition.offset + fPosition.length - 1;
		myEnd= Math.max(myStart, myEnd);

		int yoursStart= fOffset;
		// int yoursEnd=   fOffset + fReplaceLength -1;
		// yoursEnd = Math.max(yoursStart, yoursEnd);

		if (myEnd < yoursStart) {
			return;
		}

		if (fPosition.length == 0) {
			if (yoursStart < myStart) {
				fPosition.offset += fReplaceLength;
				return;
			} 
			return; 
		}
		if (fLength <= 0) {

			if (myStart <= yoursStart)
				fPosition.length += fReplaceLength;
			else 
				fPosition.offset += fReplaceLength;

		} else {

			if (myStart < yoursStart && fOriginalPosition.offset < yoursStart)
				fPosition.length += fReplaceLength;
			else if (myStart > yoursStart) {
				fPosition.offset += fReplaceLength;
			}
		}
	}
	
}
