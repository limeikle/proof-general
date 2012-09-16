/*
 *  $RCSfile: MiscUtils.java,v $
 *
 *  Created on 01 Nov 2006
 *  part of Proof General for Eclipse
 */
package ed.inf.utils;


/**
 * Miscellaneous utilities which do not yet fit into a category.
 * Feel free to move these when they become more relevant to a particular operation.
 * @author <a href="mailto:heneveld@alumni.princeton.edu">alex heneveld</a>
 * @author Daniel Winterstein
 */
public class MiscUtils {

	/**
	   * gets the value of a property 'name' by looking at:
	   *  - java system properties
	   *  - system environment variables
	   * @param name the property name
	   * @return the property value, or null if not found.
	   */
	  public static String getSystemVariable(String name) {
	    String value = null;
	    try {
	    	value = System.getProperty(name);
	    } catch (Throwable e) {
	    	System.err.println("Error getting system property '"+name+"', trying other means: "+e);
	    } try {
	    	value = System.getenv(name);
	    } catch (Throwable t) {
	    	System.err.println("Error reading from environment, looking for "+name+": " + t);
	    }
	    return value;
	  }

	/**
	   * Returns the smaller non-negative value of the two arguments;
	   * If there is none, returns the smaller absolute value (ie the larger value)
	   */
	  public static int minNonNeg(int a, int b) {
	    if (a>=0 && b>=0) {
	    	return Math.min(a, b);
	    }
	    return Math.max(a, b);
	  }

}
