/*
 *  $RCSfile: ContainerElement.java,v $
 *
 *  Created on 10 Sep 2005
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.document;
import java.util.List;

import ed.inf.proofgeneral.NotNull;
import ed.inf.proofgeneral.sessionmanager.PGIPSyntax;

/**
 * A container element is a DocElement which expects child elements (e.g., a proof).
 * Some container elements correspond to OPENTYPE type declarations in the document
 * (i.e., openfile, opentheory, opengoal).  These ones are aborted for undo and
 * contribute to the PGIP level of the container's elements.
 * See {@link ed.inf.proofgeneral.editor.lazyparser.Parser}.
 */
public class ContainerElement extends DocElement {

	/**
	 * Whether this element is 'open' for new sub-elements, or has been closed
	 * default is true
	 */
	private boolean open = true;

	/**
	 * The PGIP type for this element, if any.  If the container is created
	 * for a PGIP open-type element (openfile, opentheory, opengoal) then
	 * the name of it should be stored here.  This information is used for
	 * undo, especially to calculate the PGIP level of document elements.
	 */
	@NotNull
	private String pgiptype;

	/**
	 * The prover name for the declaration constructed inside this container,
	 * if any.  This information is used for named retraction, part of document
	 * based undo.
	 */
	private String itemname;

	/**
	 * See whether undo must abort this container or can re-open it. This may be
	 * prover-dependent, set by prover meta-model.  Currently suitable for
	 * Isabelle, and requires this container element to be properly in the 
	 * document tree (for counting parents).
	 * @return true if this container can be re-entered by undo. 
	 */
	public boolean isUndoRentering(PGIPSyntax syntax) {
		// FIXME: temporary test based on pgiptype and proof depth, HACK for Isabelle
		return PGIPSyntax.BLOCK_CONTAINER.equals(pgiptype)
			|| PGIPSyntax.THEORY_CONTAINER.equals(pgiptype)
			|| PGIPSyntax.SECTION_CONTAINER.equals(pgiptype)
			|| getPGIPLevel(syntax, this) >= 3;
	}

	public ContainerElement(String name, ProofScriptDocument doc,String pgiptype) {
		super(name, doc);
		this.pgiptype = pgiptype;
	}

	public ContainerElement(String name, ProofScriptDocument doc,String pgiptype, String itemname) {
		super(name, doc);
		this.pgiptype = pgiptype;
		this.itemname = itemname;
	}

	/**
	 * Get the PGIP type of this container, or null if it doesn't have one.
	 * @return the PGIP type
	 */
	public String getPgiptype() {
		return pgiptype;
	}


	private static class ContDepth {
		ContainerElement ce;
		int depth = 0;
	}

	private static ContDepth calculatePGIPstate(PGIPSyntax syntax, DocElement elt) {
		ContDepth cd = new ContDepth();
		DocElement parent = (DocElement) elt.getParent();
		while (parent != null) {
			assert parent instanceof ContainerElement : "Ill-formed document";
			ContainerElement ce = (ContainerElement) parent;
			if (ce.getType().equals(PGIPSyntax.ROOT_CONTAINER) ||
				(ce.getPgiptype() != null &&
				 syntax.subType(ce.getPgiptype(), PGIPSyntax.OPENTYPE)
				 
				 && (!(syntax.subType(ce.getPgiptype(), PGIPSyntax.OPENGOAL) && syntax.subType(elt.getType(), PGIPSyntax.CLOSEGOAL)))
				
				)) {

				//previously we did this
				//sm.getProverState().getProofDepth()>0 || elt != ce.getLastElement())
				 /* but depth comparison above won't work if we have nested layers;
				  * if elt is the last thing in ce, we need to know whether elt ends ce,
				  * e.g. if the depth we were at when we started elt is > sm.getProverState().getProofDepth() then skip _ce_.
				  * OR, better, just pass "undo" to the prover and let it figure out how much to undo, and it tells us.
				  * still, should fix the problem *we* are seeing in normal (non-isar) mode that if you edit something
				  * within a lemma just after the last process, then undo, the model views elt as the last
				  * and so (without the depth check) assumes the entire lemma needs undoing
				  */
				//however the above fixed a bug with original code which just did the following
				//(elt!=ce.getLastElement())
				
				// If the element ends properly inside the container, we've found
				// the PGIP container.  The closing element belongs to the parent,
				// however, unless we're at the root already.
				// [ Really we should fix this nesting in the parsing;
				//   the document structure is designed for the outline at the moment! ]

				cd.depth++;
				if (cd.ce == null) {
					cd.ce = ce;       // first enclosing container
				}
			}
			parent = (DocElement) parent.getParent();
		}
		return cd;
	}

	public static final int TOP_LEVEL = 0;
	public static final int FILE_LEVEL = 1;
	public static final int THEORY_LEVEL = 2;
	public static final int PROOF_LEVEL = 3;

	/**
	 * Return the PGIP level of this element.  This is the number
	 * of open elements which surround it inside a document.  For a properly
	 * formed document, this is 1 at the top level inside the file,
	 * 2 inside a theory and 3 or more inside a proof. Higher numbers than
	 * 3 count for nested proofs.
	 * @param sm 
	 * @param syntax is used for the check for which elements are open commands
	 * @return the PGIP level.  0 is returned for an element without a parent.
	 */
	public static int getPGIPLevel(PGIPSyntax syntax, DocElement elt) {
		ContDepth cd = calculatePGIPstate(syntax,elt);
		return cd.depth;
	}

	/**
     * @return the open
     */
	// FIXME da: it turns out that the open flag is never used.
	// All that seems important is current notion of open element,
	// when we close an element we just take the parent to be the
	// new open element (not the first parent that is currently open).
	// This maybe needs refinement as we add richer nesting to model?
    //private boolean isOpen() {
    //	return open;
    //}

	/**
     * @param open the open to set
     */
    public void setOpen(boolean open) {
    	this.open = open;
    }

	/**
	 * Get the PGIP parent of a document element according to the PGIP state model, or null if it doesn't have one.
	 * The document's ROOT element counts as a PGIP element.  By convention, the last (closing) element
	 * of a container belongs to the parent in the PGIP state model (because if it has been
	 * processed, the state is at the parent level).
	 * @param sm 
	 * @param elt
	 * @returns the container element for the given element
	 */
	public static ContainerElement getPGIPContainer(PGIPSyntax syntax, DocElement elt) {
		ContDepth cd = calculatePGIPstate( syntax,elt);
		return cd.ce;
	}



	/**
	 * Gets this PGIP name of the element declared in this container, if any.
	 * For example, for opentheory elements it will be the theory name.
	 * @return the itemname
	 */
	// NB: metainfo responses should be allowed to update this to reflect
	// names actually declared (if we get that flexible and really, really need
	// it for Isabelle).
	public String getItemname() {
		return itemname;
	}



   /**
    * Gets the last element.
    * @return The last element in this container, or null.
    */
   public DocElement getLastElement() {
       List es = elements();
       if (es.size()==0) {
    	   return null;
       }
       return (DocElement) es.get(es.size()-1);
   }

	/**
	 * Gets the first element.
	 * @return The first element in this container, or null.
	 */
	public DocElement getFirstElement() {
		List es = elements();
		if (es.size()==0) return null;
		return (DocElement) es.get(0);
	}
//
//	/**
//	 * Method for making container elements for an element of <parseresult>.
//	 * If the given element corresponds to a conceptual open of structure
//	 * (PGIP open types: opentheory, opengoal) or a visual one (openblock),
//	 * we make a container to impose tree structure on the document.
//	 *
//	 * @param e - the element being considered
//	 * @param doc - the document
//	 * @return an appropriate ContainerElement, or null if none is needed.
//	 */
//	public static ContainerElement containerForElement(Element e, ProofScriptDocument doc) {
//		ContainerElement ce = null;
//		String name = e.getName();
//		PGIPSyntax syntax = doc.syntax;
//    	// A container must be generated for every opentype item, at least.
//    	// (da: this is new, for document based undo).  The only opentypes
//    	// we expect in the parseresult are for theories and theorems, although
//		// for document model it is natural to include OPENFILE as the root element too.
//		if (syntax.subType(name, PGIPSyntax.OPENTHEORY)) {
//			String thyname = e.attributeValue(PGIPSyntax.THEORY_NAME);
//			if (thyname != null && !thyname.equals("")) {
//				ce = new ContainerElement(PGIPSyntax.THEORY_CONTAINER, doc, PGIPSyntax.OPENTHEORY, thyname);
//			} else {
//				ce = new ContainerElement(PGIPSyntax.THEORY_CONTAINER, doc, PGIPSyntax.OPENTHEORY);
//			}
//		} else if (syntax.subType(name,PGIPSyntax.OPENGOAL)) {
//			String thmname = e.attributeValue(PGIPSyntax.THEOREM_NAME);
//			if (thmname != null && thmname != "") {
//				ce = new ContainerElement(PGIPSyntax.PROOF_CONTAINER, doc, PGIPSyntax.OPENGOAL, thmname);
//			} else {
//				ce = new ContainerElement(PGIPSyntax.PROOF_CONTAINER, doc, PGIPSyntax.OPENGOAL);
//			}
//		} else if (syntax.subType(name, PGIPSyntax.OPENBLOCK)) {
//			// TODO: name info
//			ce = new ContainerElement(PGIPSyntax.BLOCK_CONTAINER, doc, null);
//		} else if ((syntax.subType(name, PGIPSyntax.DOCCOMMENT)) &&
//		// da: FIXME: this case is ISABELLE SPECIFIC. TODO: try to get openblock working nicely here instead
//				    (e.getStringValue().startsWith("subsection"))) {
//			ce = new ContainerElement(PGIPSyntax.SECTION_CONTAINER, doc, null);
//		}
//		return ce;
//	}

  @Override
public Object clone() {
	   ContainerElement clone = (ContainerElement) super.clone();
	   clone.open = this.open;
	   clone.pgiptype = this.pgiptype;
	   clone.itemname = this.itemname;
	   return clone;
   }
}
