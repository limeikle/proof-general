/*
 *  $RCSfile: ResourcePropertyManagement.java,v $
 *
 *  Created on 24 Nov 2006 by gdutton
 *  part of Proof General for Eclipse
 */

package ed.inf.utils.eclipse;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

import ed.inf.proofgeneral.ProofGeneralPlugin;

/**
 * Gets and sets boolean IResource properties.
 */
public class ResourcePropertyManagement {

	/** The string "true" */
	public static final String True = "true";
	/** The string "false" */
	public static final String False = "false";

	public static QualifiedName makeProperty(Object caller, String name) {
		return new QualifiedName(caller.getClass().getCanonicalName(), "active");
	}

	/**
	 * Stores a persistent property against a file's Resource.
	 * @param name the 'key'
	 * @param value the value
	 * @return true if successful
	 */
	public static boolean setProp(IResource resource, QualifiedName name, boolean value) {
		try {
			resource.setPersistentProperty (name, (value?True:False));
			return true;
		} catch (CoreException e) {
			System.err.println("unable to set resource property for decorator");
			if (ProofGeneralPlugin.debug(null)) {
				e.printStackTrace();
			}
			return false;
		}
	}

	/**
	 * Stores a persistent property against a file's Resource.
	 * @param name the 'key'
	 * @return the value stored, or false if it is not found or unsuccessful
	 */
	public static boolean getProp(IResource resource, QualifiedName name) {
		try {
			String output = resource.getPersistentProperty (name);
			return (output != null && output.equals(True));
		} catch (CoreException e) {
			System.err.println("unable to get resource property for decorator");
			if (ProofGeneralPlugin.debug(null)) {
				e.printStackTrace();
			}
			return false;
		}
	}

}
