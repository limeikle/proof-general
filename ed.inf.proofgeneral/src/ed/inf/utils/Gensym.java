/*
 *  $RCSfile: Gensym.java,v $
 *
 *  Created on Nov 24, 2006 by da
 *  part of Proof General for Eclipse
 */

package ed.inf.utils;

/**
 * Class to generate names.  
 * 
 */
// da: this is based on a fragment from SessionManager which turned out
// not to be used at the moment.
public class Gensym {
	/**
	 * count of unnamed objects, used by makeName
	 */
	private int count;
	private String basename;
	
	public Gensym(String base) {
		count = 1;
		basename = base;
	}
	
	public void reset() {
		count = 1;
	}

	/**
	 * Supply a new unique name
	 * @return the generated name
	 */
	public String get() {
		String name = basename + Integer.toString(count);
		count++;
		return name;
	}
	
}
