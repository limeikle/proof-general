/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Mar 14, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */

package ed.inf.proofgeneral.document;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.projection.IProjectionPosition;

/**
 * A simple instance of projection position which calculates its foldable region
 * by collapsing all subsequent lines after the first, including spaces after
 * the end of the region itself.  Currently this is not yet used.
 */
public class FoldingPosition extends Position implements IProjectionPosition {

	/**
     * @param offset
     * @param length
     */
    public FoldingPosition(int offset, int length) {
    	super(offset,length);
    }

	/**
	 * @see org.eclipse.jface.text.source.projection.IProjectionPosition#computeCaptionOffset(org.eclipse.jface.text.IDocument)
	 */
	public int computeCaptionOffset(IDocument document) { // throws BadLocationException {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @see org.eclipse.jface.text.source.projection.IProjectionPosition#computeProjectionRegions(org.eclipse.jface.text.IDocument)
	 */
	public IRegion[] computeProjectionRegions(IDocument document) throws BadLocationException {
		ProofScriptDocument doc = (ProofScriptDocument) document;
		int startOffset = getOffset();
		int nextline = doc.getLineOfOffset(startOffset)+1;
		int startCollapse = doc.getLineOffset(nextline);
		int lastOffset = startOffset + getLength() - 1;
		int lastCollapse = doc.offsetSkipSpacesNextLine(lastOffset); // may be wrong
		IRegion collapsed = new Region(startCollapse,lastCollapse-startCollapse);
		return new IRegion[] {collapsed};
	}

}
