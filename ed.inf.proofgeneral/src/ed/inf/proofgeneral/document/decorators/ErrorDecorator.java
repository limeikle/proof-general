/*
 *  $RCSfile: ErrorDecorator.java,v $
 *
 *  Created on 30 Jan 2007 by Graham Dutton
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.document.decorators;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;

/**
 * Decorates files which contain errors and warnings.
 * @see ILightweightLabelDecorator
 */
public class ErrorDecorator extends RootDecorator {

	/**
	 * Establishes icon path and quadrant.
	 */
	public ErrorDecorator() {
		super("icons/sample_decorator.gif", IDecoration.TOP_LEFT);
	}
	
	/**
	 * TODO complete
	 * @see org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang.Object, org.eclipse.jface.viewers.IDecoration)
	 */
	@Override
    public void decorate(Object element, IDecoration decoration) {
		IResource resource = (IResource) element;
		ResourceAttributes attrs = resource.getResourceAttributes();
		if ( attrs != null && attrs.isReadOnly() ) { // TODO add condition
			super.decorate(element, decoration);
		}
	}

}