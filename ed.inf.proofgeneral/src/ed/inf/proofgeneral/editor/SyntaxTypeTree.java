/*
 *  $RCSfile: SyntaxTypeTree.java,v $
 *
 *  Created on 24 Nov 2006 by gdutton
 *  part of Proof General for Eclipse
 */

package ed.inf.proofgeneral.editor;

import java.util.HashMap;
import java.util.Map;

import ed.inf.proofgeneral.pgip.PGIPMessages;

/**
 * Stores syntactic type information for highlighting.
 * TODO use this to replace the typetree in PGIPSyntax
 * TODO complete this.
 */
public class SyntaxTypeTree {

	private final Map<Enum,Enum> tree;
	
	/**
	 * Creates a new type tree, loading in mandatory syntactical
	 * elements, and prepares to accept new ones. 
	 */
	private SyntaxTypeTree() {
		tree = new HashMap<Enum,Enum>();
		loadDefaults();
	}
	
	/**
	 * Sets up default mappings.
	 */
	private void loadDefaults() {
		setParent(PGIPMessages.ANYITEM, PGIPMessages.values());
	}
	
	/**
	 * Gets the parent of a given message
	 * @param child the prospective child message 
	 * @return the parent message, or the child itself if it has no parents.
	 */
	public Enum getParent(Enum child) {
		Enum out = tree.get(child);
		return (out == null ? child : out);
	}
	
	/**
	 * Gets the final, top-level parent of this message.
	 * @param child the child message
	 * @return the top-level parent message, or the child itself if it has no parents.
	 */
	public Enum getAncestor(Enum child) {
		return tree.containsKey(child) ? getAncestor(child) : child;
	}
	
	/**
	 * Sets the parent of one or more messages.
	 * @param parent the parent item
	 * @param children the child items to set.
	 */
	private void setParent(Enum parent, Enum ... children) {
		for (Enum child : children) {
			tree.put(child, parent);
		}
	}
	
}
