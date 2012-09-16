/*
 *  $RCSfile: PGOutlineLabelProvider.java,v $
 *
 *  Created on 30 May 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.document.outline;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.DocElement;
import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.sessionmanager.PGIPSyntax;
import ed.inf.proofgeneral.ui.theme.PGColors.PGColor;
import ed.inf.utils.datastruct.StringManipulation;

// da TODO: fixes needed here:
//  - make MAX_TEXT_LENGTH into a preference
//  - make SHOW_COMMENTS into a preference
//  - allow coalescing successive comments
//  - don't make lines for (comment only?) elements not at the start of a line.
//  - IDEA: use "anti-" font locking.  Since we can't get hold of names, 
//    which are the interesting part for the outline view, we could
//    instead de-emphasise keywords (even hide them).
/**
 * This class creates the labels displayed in the Outline View
 * @author Daniel Winterstein
 * @author David Aspinall
 */
public class PGOutlineLabelProvider extends LabelProvider implements IColorProvider {

	public PGOutlineLabelProvider(PGIPSyntax syntax,ProofScriptDocument doc) {
		super();
		this.syntax = syntax;
		this.doc=doc;
	}
	private final PGIPSyntax syntax;
	private final ProofScriptDocument doc;

	static int MAX_TEXT_LENGTH = 45; // TODO: could be a preference 

	public static final String TREE_ICON = "icons/tree.gif";
	public static final String PROOFSTEP_ICON = "icons/footstep16.gif";
	public static final String OPENGOAL_ICON = "icons/dooropen16.gif";
	public static final String CLOSEGOAL_ICON = "icons/qedsquare16.gif";
	public static final String COMMENT_ICON = "icons/comment.gif";
	public static final String DEFAULT_ICON = "icons/starellipsis.gif";

	private static final String SECTION_ICON = "icons/section.gif";
	private static final String PROOF_OKAY_ICON = "icons/proof.gif";
	private static final String PROOF_GIVEUP_ICON = "icons/proof-question.gif";
	private static final String PROOF_POSTPONE_ICON = "icons/proof-bang.gif";

	@Override
    public String getText(Object obj) {
		// Normal case
		if (obj instanceof DocElement) {
			return getBasicOutlineText((DocElement) obj);
		}
		// Cases for debug usage of outline
		if (obj instanceof ITypedRegion) {
			ITypedRegion posn = (ITypedRegion) obj;
			return regionString(posn.getType(),posn.getOffset(),posn.getLength(),((Object)posn).toString());
		}
		if (obj instanceof TypedPosition) {
			TypedPosition posn = (TypedPosition) obj;
			return regionString(posn.getType(),posn.getOffset(),posn.getLength(),((Object)posn).toString());
		}
		return obj.toString();
	}
	
	private static String regionString(String name, int offset, int length, String id) {
		return name + " [" + Integer.toString(offset) + ", " 
			        + Integer.toString(offset+length) + "] " + id;
	}


	/**
	 * Construct the outline text for a document element.  The text
	 * is desymbolised and no more than {@link PGOutlineLabelProvider#MAX_TEXT_LENGTH} long.
	 * @param e an element to return an outline of
	 * @return the outline text, trimmed and abbreviated
	 */
	private String getBasicOutlineText(DocElement e) {
		String type = e.getName();
		String text = e.getTextTrim();
		if (syntax.subType(type,PGIPSyntax.CONTAINER)) {
			return getContainerOutlineText(e); 
		} 
		// HACK Isabelle specific catch for header comments
		else if (syntax.subType(type,PGIPSyntax.SPURIOUSCOMMAND) && text.indexOf("header")!=-1) {
			text = text.substring(text.indexOf("{*")+2,text.indexOf("*}"));
		} 
		else if (syntax.subType(type, PGIPSyntax.COMMENT)) {
			// nothing; comments verbatim
		} else {
		    // nothing; other elements verbatim too
		}
		// approximate prune to avoid too much symbolising
		text = text.substring(0,Math.min(text.length(), MAX_TEXT_LENGTH*2));
		text = doc.symbolise(text);
		text = StringManipulation.ellipsisTrim(text, MAX_TEXT_LENGTH);
		return text;
	}

	/**
	 * Construct the outline text for a container element.
	 * See {@link #getBasicOutlineText(DocElement)}
     * @param e the element
     * @return the outline text, trimmed and abbreviated
     */
    private String getContainerOutlineText(DocElement e) {
		String type = e.getName();
	    if (e.elements().size()==0) {
	    	return "(empty container)";  //shouldn't happen
	    }
	    DocElement firstChild = (DocElement)e.elements().get(0);
	    String containerLabel = "";
	    if (syntax.subType(type, PGIPSyntax.THEORY_CONTAINER)) {
	    	if (syntax.subType(firstChild.getType(), PGIPSyntax.OPENTHEORY)) {
	    		if (firstChild.attribute(PGIPSyntax.THEORY_NAME) != null) {
	    			return firstChild.attributeValue(PGIPSyntax.THEORY_NAME);
	    		}
	    		// Otherwise leave blank  (contents will be shown inside on first element)
	    		return ""; 
	    	}
	    } else if (syntax.subType(type, PGIPSyntax.PROOF_CONTAINER)) {
	    	if (syntax.subType(firstChild.getType(), PGIPSyntax.OPENGOAL)) {
	    		if (firstChild.attribute(PGIPSyntax.THEOREM_NAME) != null) {
	    			// FIXME DEPGRAPH: addDocElement here
	    			//System.out.println("add: "+firstChild.attributeValue(PGIPSyntax.THEOREM_NAME));
	    			// Graph.addDocElement(firstChild);
	    			containerLabel = 
	    				firstChild.attributeValue(PGIPSyntax.THEOREM_NAME);
	    		}

	    		// add in abbreviated text of statement
	    		if (containerLabel.length()<MAX_TEXT_LENGTH-10) {
	    			// If name is short-ish/empty, take statement as well.
	    			// FIXME: could have a statement attribute here in PGIP
	    			// and so get the right text exactly. (Even strip quotes in Isabelle)
	    			String goalText = StringManipulation.firstLine(firstChild.getStringValue());
	    			String symGoalText = doc.symbolise(goalText);
	    			boolean paren = !containerLabel.equals("");;
	    			containerLabel +=
	    				(paren ? " (" : "") +
	    				StringManipulation.ellipsisTrim(symGoalText,MAX_TEXT_LENGTH-containerLabel.length()-(paren?3:0)) +
	    				(paren ? ")" : "");
	    		}
	    		return containerLabel;
	    	}
	    }
	    // Containers otherwise get the text of their first element
	    String text = getBasicOutlineText(firstChild);
	    //    HACK Isabelle specific catch for headers&subsections comments
	    if (text.indexOf("{*")>=0) {
	    	text = text.substring(text.indexOf("{*")+2);
	    }
	    if (text.endsWith("*}")) {
	    	text = text.substring(0, text.length()-2);
	    }
	    return text;
    }
	
	
	@Override
    public Image getImage(Object obj) {
		if (obj instanceof String) {
			return ProofGeneralPlugin.getImage(TREE_ICON); // debug usage
		}
		if (obj instanceof PGContentOutlinePage.ParseInfo) {
			return ProofGeneralPlugin.getImage(TREE_ICON);
		}
		if (obj instanceof DocElement) {
			DocElement de = (DocElement) obj;
			String type = de.getType();
			if (syntax.subType(type,PGIPSyntax.POSTPONEGOAL)
					|| syntax.subType(type,PGIPSyntax.GIVEUPGOAL)) {
				return ProofGeneralPlugin.getDefault().getWorkbench().getSharedImages()
				//return PlatformUI.getWorkbench().getSharedImages()
				.getImage(ISharedImages.IMG_OBJS_WARN_TSK);
			} else if (syntax.subType(type,PGIPSyntax.PROOFSTEP)) {
				return ProofGeneralPlugin.getImage(DEFAULT_ICON); // was the steps: PROOFSTEP_ICON);
			} else if (syntax.subType(type,PGIPSyntax.COMMENT)) {
				return ProofGeneralPlugin.getImage(COMMENT_ICON);
			} else if (syntax.subType(type,PGIPSyntax.OPENGOAL)) {
				// da: this case isn't used now, we put it into the container
				return ProofGeneralPlugin.getImage(OPENGOAL_ICON);
			} else if (syntax.subType(type,PGIPSyntax.CLOSEGOAL)) {
				return ProofGeneralPlugin.getImage(CLOSEGOAL_ICON);
			} else if (syntax.subType(type,PGIPSyntax.PROOF_CONTAINER)) {
				// flag the container by whether the proof contains sorrys or oopses
				if (elementContains(de, PGIPSyntax.POSTPONEGOAL)) {
					return ProofGeneralPlugin.getImage(PROOF_POSTPONE_ICON);
				}
				if (elementContains(de, PGIPSyntax.GIVEUPGOAL)) {
					return ProofGeneralPlugin.getImage(PROOF_GIVEUP_ICON);
				}
				if (elementContains(de, PGIPSyntax.CLOSEGOAL)) {
					return ProofGeneralPlugin.getImage(PROOF_OKAY_ICON);
				}
				return ProofGeneralPlugin.getImage(PROOF_POSTPONE_ICON);
			} else if (syntax.subType(type,PGIPSyntax.CONTAINER)) {
				return ProofGeneralPlugin.getImage(SECTION_ICON);
			}
		}
		return ProofGeneralPlugin.getImage(DEFAULT_ICON);
	}
	
	/**
	 * @param de
	 * @param type
	 * @return true if the docElement contains a descendant of type type
	 */
	public boolean elementContains(DocElement de, String type) {
		if (syntax.subType(de.getType(), type)) {
			return true;
		}
		List kids = de.elements();
		for(Iterator i = kids.iterator(); i.hasNext();) {
			try {
				DocElement kde = (DocElement) i.next();
				if (elementContains(kde, type)) {
					return true;
				}
			} catch (Exception x) {}
		}
		return false;
	}

	/**
	 * Does this element contain any abandon-goal type commands?
	 * @param de
	 * @return true means no sorrys (ie. this might be a complete proof)
	 */
	public boolean noSorrys(DocElement de) {
		if (syntax.subType(de.getType(),PGIPSyntax.POSTPONEGOAL)
				|| syntax.subType(de.getType(),PGIPSyntax.GIVEUPGOAL)) {
			return false;
		}
		List kids = de.elements();
		if (kids==null || kids.size()==0) {
			return true;
		}
		DocElement kde;
		for(Iterator i = kids.iterator(); i.hasNext();) {
			try {
				kde = (DocElement) i.next();
				if (!noSorrys(kde)) {
					return false;
				}
			} catch (Exception x) {}
		}
		return true;
	}


	// da: instead of all these separately configurable colours, this would be better designed
	// perhaps just to have an option to use the standard script management colours
	// (and similarly for colouring text in the proof script).
	
	/**
     * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
     */
    public Color getForeground(Object element) {
    	return chooseProcessedBusyNone(element, 
    			PGColor.OUTLINE_PROCESSED_FG.get(), PGColor.OUTLINE_BUSY_FG.get());
    }

	/**
     * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
     */
    public Color getBackground(Object element) {
    	return chooseProcessedBusyNone(element, 
    			PGColor.OUTLINE_PROCESSED_BG.get(), PGColor.OUTLINE_BUSY_BG.get());
    }
    
    private Color chooseProcessedBusyNone(Object element, Color processed, Color busy) {
    	if (!(element instanceof DocElement)) {
    		return null;
    	}
		DocElement e = (DocElement) element;
		Position posn = e.getPosition();
		Color col = null; 
		if (posn != null) {
			if (posn.getOffset()+posn.getLength()-1<=doc.getProcessedOffset()) {
				col = processed;
			} else if (posn.getOffset() <= doc.getLockOffset()) { 
				col = busy;
			}
		}
		return col;
    }

}
