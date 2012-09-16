/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Mar 17, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */

package ed.inf.proofgeneral.editor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension4;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 *
 */
public class PGCompletionProposal 
        implements ICompletionProposal, ICompletionProposalExtension4 {

	/**
	 * No public constructor: use factory methods instead
	 */
	private PGCompletionProposal() {
	}
	
	private CompletionProposal comp;
	
	public static PGCompletionProposal symbolProposal(String replacement, int offset, int replacementLength, 
				String displayText) {
		PGCompletionProposal pgcomp = new PGCompletionProposal();
		pgcomp.comp = new CompletionProposal(replacement,offset,replacementLength,replacement.length(),null,displayText,null,null);
		return pgcomp;
	}

	/**
	 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension4#isAutoInsertable()
	 */
	public boolean isAutoInsertable() {
		return true;
	}

	/**
     * @see org.eclipse.jface.text.contentassist.ICompletionProposal#apply(org.eclipse.jface.text.IDocument)
     */
    public void apply(IDocument document) {
	    comp.apply(document);	    
    }

	/**
     * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
     */
    public String getAdditionalProposalInfo() {
	    return comp.getAdditionalProposalInfo();
    }

	/**
     * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getContextInformation()
     */
    public IContextInformation getContextInformation() {
	    return comp.getContextInformation();
    }

	/**
     * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getDisplayString()
     */
    public String getDisplayString() {
	    return comp.getDisplayString();
    }

	/**
     * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getImage()
     */
    public Image getImage() {
	    return comp.getImage();
    }

	/**
     * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getSelection(org.eclipse.jface.text.IDocument)
     */
    public Point getSelection(IDocument document) {
	    return comp.getSelection(document);
    }

}
