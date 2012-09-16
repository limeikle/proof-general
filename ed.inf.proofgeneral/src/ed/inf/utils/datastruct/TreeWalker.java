/*
 *  $RCSfile: TreeWalker.java,v $
 *
 *  Created on 01 Nov 2006
 *  part of Proof General for Eclipse
 *  Code created from earlier work by Daniel Winterstein
 */
package ed.inf.utils.datastruct;

import java.util.List;
import java.util.ListIterator;

/**
 * Allows the implementer to apply a function to every node in a rooted tree -
 * or until the function returns an object.
 * @author Daniel Winterstein
 */
public class TreeWalker {

	/**
	 * Apply a function to every node in a rooted tree - or until the
	 * function returns an object.  Apply the function from
	 * leaves->root, with leaves in reverse order.
	 */
	@SuppressWarnings("unchecked")
    public static final Object reverseTreeWalk(Tree root, Fn fn) {
		Object result = null;
		List<Tree> kids = root.getChildren();
		for (ListIterator<Tree> iter = kids.listIterator(kids.size()); iter.hasPrevious();) {
			Tree kid = iter.previous();
			result = reverseTreeWalk(kid, fn);
			if (result != null) {
				return result;
			}
		}
		return fn.apply(root);
	}
	
	/**
	 * Apply a function to every node in a rooted tree - or until the function
	 * returns an object.
	 */
	@SuppressWarnings("unchecked")
    public static Object treeWalk(Tree root, Fn fn) {
		Object result = null;
		result = fn.apply(root);
		if (result != null) {
			return result;
		}
		List<Tree> kids = root.getChildren();
		for (Tree kid : kids) {
			result = treeWalk(kid, fn);
			if (result != null) {
				return result;
			}
		}
		return null;
	}
		

	/**
	 * A rooted tree node
	 */
	public static interface Tree {
		/**
		 * @return the child nodes of this node.
		 */
		public List getChildren();
	}

  /**
	 * An interface for passing function objects to Methods.treewalk.
	 */
 public static abstract interface Fn {
	   /**
		 * @param node
		 * @return the return value of the function. return null to continue the
		 *         tree walk, anything else breaks out early.
		 */
	   public abstract Object apply(Tree node);
 }

}
