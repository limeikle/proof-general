/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Mar 2, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */
package ed.inf.proofgeneral.document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;

import org.dom4j.Element;
import org.dom4j.VisitorSupport;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.preferences.PreferenceNames;
import ed.inf.proofgeneral.sessionmanager.PGIPSyntax;
/**
 * Manage the folding structure on Proof Scripts.  The main responsibility
 * of this class is to decide where to put folding annotationsAdded.  The
 * annotationsAdded themselves are managed in {@link ed.inf.proofgeneral.document.DocElement}
 * and the folding annotation model is held in the 
 */
// TODO: improve efficiency of recalculation (don't remove all and re-do!)
public class Folding {

    /** The document we're connected to. */
    private final ProofScriptDocument proofScriptDocument;
    
    /** The minimum number of lines in a comment before it is folded. */
    public static final int FOLDING_THRESHOLD_COMMENT = 2;
    
    /** The minimum number of lines in a theory item before it is folded. */
    public static final int FOLDING_THRESHOLD_THEORY_ITEM = 5;
    
	/** The projection model for folding, or null if folding is disabled. */
    private ProjectionAnnotationModel projectionAnnotationModel;

    
	/**
	 * Construct a folding structure associated with the given document.
	 * To activate, the folding structure must be initialised by calling
	 * 
     * @param proofScriptDocument
     */
    public Folding(ProofScriptDocument proofScriptDocument) {
	    this.proofScriptDocument = proofScriptDocument;
	    //proofScriptDocument.addPositionCategory(DocElement.FOLDING_POSITION_CATEGORY);
    }

	// Support recalculating from a given point in the document onwards
    // See TRAC #90 for other improvements here.
    /**
     * Recalculate the folding structure for the whole document, updating the annotationsAdded.
     * Main method of this class.
     */
    public void recalculateFoldingStructure() {
    	if (projectionAnnotationModel == null) { 
    		return; // Folding is off
    	}
    	MakeAnnotations maker = new MakeAnnotations();
    	removeAllFoldingAnnotations();
    	proofScriptDocument.getRootElement().accept(maker);
    	projectionAnnotationModel.modifyAnnotations(
    			maker.annotationsDeleted.toArray(
    					new ProjectionAnnotation[maker.annotationsDeleted.size()]),
    			maker.annotationsAdded, 
    			maker.annotationsModified.toArray(
    					new ProjectionAnnotation[maker.annotationsModified.size()]));
    }

	/**
     * A document visitor which makes folding annotations for each element in
     * the document, as appropriate.
     */
    private final class MakeAnnotations extends VisitorSupport {

	    @Override
	    public void visit(Element elt) {
	    	if (elt instanceof DocElement) {
	    		DocElement e = (DocElement) elt;
	    		ProjectionAnnotation currentAnnot = e.getFoldingAnnotation();
	    		String type = e.getName();
	    		boolean hasAnn = makeAnnotation(e, type);
	    		if (!hasAnn && currentAnnot!=null) {
	    			annotationsDeleted.add(currentAnnot);
	    			e.removeFoldingAnnotation();
	    		}
	    	}
	    }

    	/** The blocks which are open so far during parsing */
    	final Stack<Integer> openBlocks = new Stack<Integer>();
    	/** The number of (non-whitespace) elements in the open block */
    	final Stack<Integer> eltsInBlock = new Stack<Integer>();

    	final HashMap<ProjectionAnnotation,Position> annotationsAdded = new HashMap<ProjectionAnnotation,Position>(); 
    	final ArrayList<ProjectionAnnotation> annotationsModified = new ArrayList<ProjectionAnnotation>(); 
    	final ArrayList<ProjectionAnnotation> annotationsDeleted = new ArrayList<ProjectionAnnotation>(); 

    	//int lineOfLastElement = 0;
    	
	    /**
	     * Make an annotation associated with the given element, adding annotation for multiple
	     * line elements and nested elements.  Update {@link #annotationsAdded} and
	     * {@link #annotationsModified} appropriately.
	     * @param e
	     * @param type
	     * @returns true if this element now has an annotation
	     */
	    @SuppressWarnings("boxing")
        private boolean makeAnnotation(DocElement e, String type) {
    		if (proofScriptDocument.syntax.subType(type, PGIPSyntax.WHITESPACE)) {
    			return false;
    		}
    		boolean hasAnn = false;
	    	try {
	    		if (proofScriptDocument.syntax.subType(type, PGIPSyntax.COMMENT) ||
	    				proofScriptDocument.syntax.subType(type, PGIPSyntax.DOCCOMMENT)) {
	    			hasAnn = multilineAnnotation(e,FOLDING_THRESHOLD_COMMENT,
	    					ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_FOLD_COMMENTS));
	    		} 
	    		// Note: annotations for open..close regions are associated with the closing
	    		// element, actually.  This is slightly counterintuitive: probably better to eagerly
	    		// make annotations as we see the open elements, and just fix up their locations
	    		// more accurately later.  (This is how Java folding behaves).
	    		else if (proofScriptDocument.syntax.subType(type,PGIPSyntax.OPENBLOCK)) {
	    			openBlocks.push(e.getPosition().getOffset());
	    			eltsInBlock.push(0);
	    		}
	    		else if (proofScriptDocument.syntax.subType(type, PGIPSyntax.CLOSEBLOCK)) {
	    			if (!openBlocks.isEmpty()) {
	    				int openOffset = openBlocks.pop();
	    				int elts = eltsInBlock.pop();
	    				// prevent trivial structure, esp for outdenting elts
	    				if (elts > 1) { 
	    					int closeOffset = e.getPosition().getOffset()-1;
	    					if (linesInRange(openOffset,closeOffset)>1) {
	    						addAnnotation(e,openOffset,closeOffset,
	    									ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_FOLD_PROOFS));
	    						hasAnn = true;
	    					}
	    				}
	    			}
	    		} else if (proofScriptDocument.syntax.subType(type, PGIPSyntax.THEORYITEM)) {
	    			// Add folding for particularly long definitions, etc.
	    			hasAnn = multilineAnnotation(e,FOLDING_THRESHOLD_THEORY_ITEM,
	    						ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_FOLD_ITEMS));
	    		}
	    	} catch (BadLocationException ex) {
	    		// TODO: log this error.  
	    		if (ProofGeneralPlugin.debug(this)) {
	    			System.err.println("Bad location during annotation calculation");
	    			ex.printStackTrace();
	    		}
	    	}
	    	if (!eltsInBlock.isEmpty()) {
	    		int i = eltsInBlock.peek();
	    		eltsInBlock.set(eltsInBlock.size()-1, i+1);
	        }
	    	return hasAnn;
	    }

	    	
	    /**
	     * Add a projection annotation for the given document element, if it
	     * occupies more than one line.
	     * @param e
	     * @param minlines
	     * @return true if an annotation is added
	     * @throws BadLocationException 
	     */
	    private boolean multilineAnnotation(DocElement e, int minlines,boolean initiallyFolded) 
	    	throws BadLocationException {
	    	Position pos = e.getPosition();
	    	int offset = pos.getOffset();
	    	int end = offset + pos.getLength()-1;
	    	if (linesInRange(offset,end) >= minlines) {
	    		addAnnotation(e,offset, end, initiallyFolded);
	    		return true;
	    	}
	    	return false;
	    }
	    
	    private void addAnnotation(DocElement e, int start, int end, boolean initiallyFolded) {
	    	int lastoffsetskipped = proofScriptDocument.offsetSkipSpacesNextLine(end);
	    	Position pos = new Position(start,lastoffsetskipped - start);
	    	boolean isnew = e.addFoldingAnnotation(pos, initiallyFolded);
	    	if (isnew) {
	    		annotationsAdded.put(e.getFoldingAnnotation(), e.getFoldingPosition());
	    	} else {
	    		annotationsModified.add(e.getFoldingAnnotation());
	    	}
	    }
	    
    }
    

    private int linesInRange(int start, int end) throws BadLocationException {
		int first = proofScriptDocument.getLineOfOffset(start);
		int last = proofScriptDocument.getLineOfOffset(end);
		return last - first;
    }
    
    /**
     * If the projection model exists, remove all its annotationsAdded
     */
    public void removeAllFoldingAnnotations() {
    	if (projectionAnnotationModel != null) {
    		projectionAnnotationModel.removeAllAnnotations();
    	}
    }
    /**
     * If the projection model exists, add the given annotation.
     * @param ann
     * @param pos
     */
    public void addFoldingAnnotationToModel(ProjectionAnnotation ann, Position pos) {
    	if (projectionAnnotationModel != null) {
    		projectionAnnotationModel.addAnnotation(ann, pos);
    	}
    }

    /**
     * If the projection model exists, delete the given annotation.
     * @param ann
     */
    // NB: this ends up being called by addAnnotation above, via DocElement#addFoldingAnnotation
    public void removeFoldingAnnotationFromModel(ProjectionAnnotation ann) {
    	if (projectionAnnotationModel != null) {
    		projectionAnnotationModel.removeAnnotation(ann);
    	}
    }    
    
	/**
     * @param projectionAnnotationModel
     */
    public void setProjectionAnnotationModel(ProjectionAnnotationModel projectionAnnotationModel) {
    	this.projectionAnnotationModel = projectionAnnotationModel;
    	projectionAnnotationModel.connect(proofScriptDocument);
    }
    
    /**
     * @param offset
     * @return the position of the first unfolded offset after the given parameter
     */
    public int getUnfoldedOffsetAfter(int offset) {
       	if (projectionAnnotationModel == null) {
    		return offset;
    	}
    	int offsetafter = offset;
    	Iterator annIterator = projectionAnnotationModel.getAnnotationIterator();
    	while (annIterator.hasNext()) {
    		ProjectionAnnotation ann = (ProjectionAnnotation) annIterator.next();
    		if (ann.isCollapsed() && !ann.isMarkedDeleted()) {
    			Position pos = projectionAnnotationModel.getPosition(ann);
    			int annoffset = pos.getOffset();
    			int annlength = pos.getLength();
    			if (annoffset <= offset && annoffset + annlength > offset && 
    					annoffset + annlength > offsetafter) {
    				offsetafter = annoffset + annlength;
    			}
    		}
    	}
    	return offsetafter;
    }
    
    
    /**
     * @param offset
     * @return the position of the first unfolded offset before the given parameter
     */
    public int getUnfoldedOffsetBefore(int offset) {
       	if (projectionAnnotationModel == null) {
    		return offset;
    	}
    	int offsetbefore = offset;
    	Iterator annIterator = projectionAnnotationModel.getAnnotationIterator();
    	while (annIterator.hasNext()) {
    		ProjectionAnnotation ann = (ProjectionAnnotation) annIterator.next();
    		if (ann.isCollapsed() && !ann.isMarkedDeleted()) {
    			Position pos = projectionAnnotationModel.getPosition(ann);
    			int annoffset = pos.getOffset();
    			int annlength = pos.getLength();
    			if (annoffset <= offset && annoffset + annlength > offset && 
    					annoffset > offsetbefore) {
    				offsetbefore = annoffset - 1;
    			}
    		}
    	}
    	return offsetbefore;
    }
    
}
