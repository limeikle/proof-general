/*
 *  $RCSfile: DocElement.java,v $
 *
 *  Created on 10 May 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.document;

import java.io.StringReader;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.util.UserDataElement;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;

import ed.inf.proofgeneral.NotNull;
import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.outline.IPGOutlineElement;
import ed.inf.proofgeneral.preferences.PreferenceNames;
import ed.inf.proofgeneral.sessionmanager.PGIPSyntax;
import ed.inf.utils.datastruct.StringManipulation;
import ed.inf.utils.datastruct.TreeWalker.Tree;

/**
 * A document element models a command in a proof script.  
 * See @{# {@link ProofScriptDocumentTree}.
 *
 * @author Daniel Winterstein
 * @author David Aspinall
 */
// TODO: at the moment we're reusing dom4j trees here, which is messy (confusion with
// XML or not) and contains more than we want.
// We ought to use our own tree model.
@SuppressWarnings("unchecked")
public class DocElement extends UserDataElement implements Tree, IPGOutlineElement {

	/** The proof script document to which this element belongs. */
	@NotNull
	private ProofScriptDocument fDocument;

	/** Set if raw text for this element is known. Used in parsing. */
	private String rawText = null;

	/** Tooltip for this element. Not set by default. */
	String tooltip = null;
	
	/**
	 * Create a new DocElement.
	 * @param name
	 * @param doc - null if this element is being created just to send as a command not
     *    associated with any document.
	 */
	public DocElement(String name,ProofScriptDocument doc) {
		super(name);
		assert doc != null : "Cannot create a document element without a document!";
		fDocument = doc;
		// FIXME: depgraph hook here?: addToGraph(name);
	}

	/**
	 * Convert a document element into a command for sending to the theorem prover.
	 * This will be a single <dostep> or <doitem> command, with no attributes.
	 * The pgiplevel determines dostep or doitem.  No attributes are set on the command.
	 * @return command version of this document element.
	 */
	// TODO: when we have a proper type for commands this should return that type
	// NB: we could set the pgiplevel in here, but for the bloomin' syntax parameter.
	public static CmdElement makeCommand(DocElement docelt,TypedPosition pos) {
		/* Argh! the code in ProverState likes to track the changes on the way out.
		 * So we can't munge the element name here as we'd like. */
//		 if (pgiplevel == DummyDocElement.PROOF_LEVEL) {
//			name = "dostep";
//		} else if (pgiplevel == DummyDocElement.THEORY_LEVEL) {
//			name = "doitem";
//		} else {
//			name = e.getName();
//		}
		CmdElement cmd = new CmdElement(docelt.getName(), docelt.getProofScript(), pos);
		cmd.setText(docelt.getStringValue());
		return cmd;
	}

	/**
	 * Gets the document to which this element belongs (if any recorded)
	 * @return the containing ProofScriptDocument (not the XML document)
	 */
	public ProofScriptDocument getProofScript() {
		return fDocument;
	}

	/**
	 * Get the typed position attached to this element.
	 * @return position, or null if none set.
	 */
	public TypedPosition getPosition() {
		return (TypedPosition) getData();
	}
	/**
	 * Set a typed position to attach to this element.
	 * @param posn
	 */
	public void setPosition(TypedPosition posn) {
		setData(posn);
		try {
			// Add position to the document so it moves with edits
			fDocument.addPosition(posn);
		} catch (BadLocationException ex) {
			// FIXME: error msg & log
		}
	}
	
	/**
	 * Delete the position associated with this element, also removing it from the document.
	 * @return the length of the deleted position, or 0 if no position was associated.
	 */
	public int deletePosition() {
	    Position posn = getPosition();
	    if (posn == null) {
	    	return 0;
	    }
    	int deletedLength = posn.getLength();
    	fDocument.removePosition(posn);
    	posn.delete();
    	return deletedLength;
	}

	
	/**
	 * @return the type of this element,
	 * obtained from it's position if possible, otherwise from its name
	 */
	public String getType() {
	    TypedPosition posn = getPosition();
	    if (posn!=null && posn.getType()!=null) {
	    	return posn.getType();
	    }
	    return getName();
	}

	/**
	 * Return <b>all</b> the text for this element.
	   Uses
	   @see org.dom4j.Node#getStringValue() instead of
	   @see org.dom4j.Node#getText()
	 */
	@Override
    public String getText() {
	    return getStringValue();
	}

	/**
	 * Overwritten to use rawText if set.
	 * This helps with mixed syntax files where document elements sometimes contain
	 * their xml tags and sometimes don't.
	   @see org.dom4j.Node#getStringValue()
	 */
    @Override
    public String getStringValue() {
        if (rawText != null) {
        	return rawText;
        }
        return super.getStringValue();
    }
    
	/**
	 * @return the offset of the start of this element
	 */
	public int getStartOffset() {
		return this.getPosition().getOffset();
	}

	/**
	 * @return the offset of the last character in this element
	 */
	public int getEndOffset() {
		return this.getPosition().getOffset() + this.getPosition().getLength() - 1;
	}

	//public String asDisplayHTML() {
	//    return converter.getDisplayText(this,false,false);
	//}

	@Override
    public String toString() {
	    List atts = attributes();
	    String as="";
	    for(Iterator i = atts.iterator(); i.hasNext();) {
	        Attribute att = (Attribute) i.next();
	        // name = att.getName();
	        //if (name.equals("name") || name.equals("label")) {
	            as += att.getText();
	        //}
	    }
	    return getType() + as;
	    //" ("+ Integer.toString(getPosition().offset)+","+
	    //Integer.toString(getPosition().offset +getPosition().length)+"): " + getText();
	}


	/**
	 * Remove this doc element from its parent, if there is one,
	 * and deletes its position from the document.
	 */
	// TODO: more efficient version of this which deletes a list of elements
	// from a given tree, using a sort on their positions.
	// See also ProofScriptDocumentTree 
	public void delete() {
	    int deletedLength = deletePosition();
// Fixme: do want to remove this from model, but not not until reconcilliation
// is working
//	    if (foldingAnnotation != null) {
//	    		fDocument.removeFoldingAnnotationFromModel(foldingAnnotation);
//	    		foldingAnnotation.markDeleted(true);
//	    }
	    Element e = getParent();
	    // remove from document tree
	    if (e != null) {
	    	e.remove(this);
	    }
	    // adjust container element lengths
	    // (skip for empty nodes, except where it's empty because something was deleted! 
	    if (deletedLength >0 || getText().length()>0) {
	    	while (e!=null) {
	    		if (e instanceof ContainerElement) {
	    			ContainerElement ce = (ContainerElement) e;
	    			Position cePos = ce.getPosition();
	    			if (cePos != null) {
	    				List<DocElement> children = ce.getChildren();
	    				if (children.size() > 0) {
	    					DocElement lastChild = children.get(children.size()-1);
	    					int newLength = lastChild.getEndOffset() - cePos.getOffset() + 1;
	    					cePos.setLength(Math.max(0, newLength));
	    				} else {
	    					cePos.setLength(0);
	    				}
	    			}
	    		}
	    		e = e.getParent(); // repeat for parents up the tree.
	    	}
	    }
	}
	

    /**
     * @see ed.inf.utils.datastruct.TreeWalker.Tree#getChildren()
     */
    public List getChildren() {
        return elements();
    }
    public String getTooltip() {
        return tooltip;
    }
    public void setTooltip(String tooltip) {
        this.tooltip = StringManipulation.trim(tooltip);
    }

  	/**
  	 * Convenience method for creating dom4j elements from xml.
  	 * @param xml the xml data to objectify
  	 * @return a dom4j element containing the XML data
  	 * @throws Exception
  	 */
  	public static Element xml2element(String xml) throws Exception {
  		if (saxReader==null) {
  			saxReader = new SAXReader();
  		}
		org.dom4j.Document document = saxReader.read( new StringReader(xml));
		List list = document.content();
		assert list.size()==1 : "xml2element is confused - too many elements received at once.";
		Element e = (Element) list.get(0);
		return e;
  	}
  	// used by xml2element
  	private static SAXReader saxReader = null;
  	
  	
  	// =============================================================================================
  	
  	// Folding annotationsAdded
  	

	/**
	 * A folding annotation for this element.  May be null
	 * if no annotation associated.  
	 */ 
	private ProjectionAnnotation foldingAnnotation;
	/**
	 * The position for this element's folding annotation.
	 * Non-null if {@link #foldingAnnotation} is non-null.
	 */
	private Position foldingPosition;

    /**
     * @return the foldingAnnotation
     */
    public ProjectionAnnotation getFoldingAnnotation() {
    	return foldingAnnotation;
    }

    /**
     * @return the foldingAnnotation
     */
    public Position getFoldingPosition() {
    	return foldingPosition;
    }

    /**
     * @return non-nil if there is a folding annotation associated with this element
     */
    public boolean hasFoldingAnnotation() {
    	return foldingAnnotation != null;
    }

    
	/**
	 * Add a folding annotation for this document element, making a position with the given start
	 * and end,  and putting the position under the control of the document's
	 * position updaters. The annotation is only added if it spans more than one
	 * line.  At most one annotation is associated with any document element.
	 * The annotation is added to the annotation model of the document, if one
	 * is active.
	 * The element must be owned by a document for this to work.
     * @param pos the new position
     * @param initiallyFolded
     * @return true if a new annotation has been created which will need to be added
     * to the annotation model; false if we are reusing an existing annotation for this element
     */
    public boolean addFoldingAnnotation(Position pos,boolean initiallyFolded) {
    	// First, set the position.  This should be the position used in
    	// the annotation model: when the model is connected, it will update with document edits.
    	if (foldingPosition == null) {
    		foldingPosition = pos;
    	} else if (!foldingPosition.equals(pos)) {
    		removeFoldingPosition();
    		foldingPosition = pos;
    	}
    	// Now set the annotation
    	if (foldingAnnotation == null) {
    		foldingAnnotation = new ProjectionAnnotation(initiallyFolded);
    		return true;  // Indicate new annotation, will need to be added to model
    	} else if (initiallyFolded) {
    		foldingAnnotation.markCollapsed();
    	} else {
    		foldingAnnotation.markExpanded();
    	}
    	return false;   // Indicate no new annotation created, will be modification
    }

    /**
     * Remove the folding annotation from this element, if any, along
     * with its associated position.
     */
    public void removeFoldingAnnotation() {
    	if (foldingAnnotation != null) {
    		foldingAnnotation = null;
    		removeFoldingPosition();
    	}
    }
    
    /**
     * Remove the folding position from this element, removing it from
     * the document and marking it as deleted.
     */
    private void removeFoldingPosition() {
		foldingPosition.delete();
		foldingPosition = null;
    }

	@Override
    public Object clone() {
 	   DocElement clone = (DocElement) super.clone();
 	   clone.fDocument = this.fDocument;
 	   return clone;
    }


	/**
     * Create a persistent resource marker for certain doc elements.
     * Theories, theorems, lemmas, definitions and mistakes.
     *
     */
	/*
     * TODO: Minor Improvement. Currently this creates 2 markers: a bookmark and a pgmarker.
     * In principle, it would be better to sub-class bookmark. However there appear to be
     * problems with the bookmark viewer when this approach is taken.
     *
     * da: Oct 06: add option for controlling automatic generation of bookmarks, and
     * default to off.  TODO: I think this option should be removed in future, bookmarks
     * are supposed to be a user-level device, not automatically generated.  Let's
     * experiment for a while, though, and try to get some user feedback.
     * da: Oct 06: add priority levels to task markers, and adjust marker subclassing.
     * NB: bookmarks are used to search for theorem definitions, etc.
     * We should use other kinds of markers for those.
     */
    @SuppressWarnings("boxing")
    public void createMarker(PGIPSyntax syntax) {
    	String type = getType();
    	boolean addbookmarks =
    		ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_MAKE_BOOKMARKS);
    	if (syntax.subType(type,PGIPSyntax.OPENTHEORY)) {
    		String name = attributeValue(PGIPSyntax.THEORY_NAME);
    		if (name==null) {
    			return;
    		}
    		if (addbookmarks) {
    			addMarker(ProofScriptMarkers.THEORY_PREFIX+name,ProofScriptMarkers.PGTHEORY_MARKER);
    		}
    		addMarker(name,ProofScriptMarkers.PGTHEORY_MARKER);
    	} else if (syntax.subType(type,PGIPSyntax.OPENGOAL)) {
    		String name = attributeValue(PGIPSyntax.THEOREM_NAME);
    		if (name==null) {
    			return;
    		}
    		if (addbookmarks) {
    			addMarker(ProofScriptMarkers.THEOREM_PREFIX+name,ProofScriptMarkers.PGTHEOREM_MARKER);
    		}
    		addMarker(name,ProofScriptMarkers.PGTHEOREM_MARKER);
    		// Don't make task markers for discarded goals: they're discarded for a reason.			
    		//		} else if (syntax.subType(type,PGIPSyntax.GIVEUPGOAL)) {
    		//			PGMarkerMethods.addMarker(e,PGMarkerMethods.UNSOLVED_MSG+" (discarded)",
    		//					PGMarkerMethods.PGTASK_MARKER,IMarker.PRIORITY, IMarker.PRIORITY_LOW);
    	} else if (syntax.subType(type,PGIPSyntax.POSTPONEGOAL)) {
    		addMarker(ProofScriptMarkers.UNSOLVED_MSG+" (obligation)",
    				ProofScriptMarkers.PGTASK_MARKER,IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
    	}
    }


	/**
     * Creates a new marker for the given document (or returns an existing
     * equivalent marker if one exists).
     * 
     * @param message
     * @param markerType
     */
    private void addMarker(String message, String markerType) {
    	ProofScriptMarkers.addMarker(getProofScript(), 
    				getPosition(), -1,// FIXME line from position for document?   														// document
    	        message, markerType,getTooltip(), null, 0);
    }


	/**
     * Creates a new marker for the given document element (or returns an
     * existing equivalent marker if one exists).
     * 
     * @param message
     * @param markerType
     * @param level
     * @param levelval
     */
    private void addMarker(String message, String markerType, String level,
            int levelval) {
    	ProofScriptMarkers.addMarker(getProofScript(), getPosition(), -1, // FIXME: line from position?  														// position?
    	        message, markerType, getTooltip(), level, levelval);
    }

	/**
     * @param rawText the rawText to set
     */
    public void setRawText(String rawText) {
    	this.rawText = rawText;
    }
}
