/*
 *  $RCSfile: RootDecorator.java,v $
 *
 *  Created on 30 Jan 2007 by Graham Dutton
 *  part of Proof General for Eclipse
 */
/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package ed.inf.proofgeneral.document.decorators;

import java.net.URL;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.utils.file.FileUtils;

/**
 * An decorator from which all ProofGeneral decorators can inherit features.
 * @see ILightweightLabelDecorator
 */
public abstract class RootDecorator implements ILightweightLabelDecorator {

	/** The integer value representing the placement options */
	protected int quadrant;

	/** Cache for the image URL, taken from iconPath. */
	protected URL cachedUrl;

	/** The image description used in <code>addOverlay(ImageDescriptor, int)</code> */
	protected ImageDescriptor descriptor;

	/**
	 * Initialises default values for decoration.
	 * @param descriptor the ImageDescriptor for the icon.
	 * @param quadrant in which the decoration should appear, from {@link IDecoration} constants
	 */
	RootDecorator(ImageDescriptor descriptor, int quadrant) {
		this.descriptor = descriptor;
		this.quadrant = quadrant;
	}

	/**
	 * Initialises default values for decoration.
	 * @param iconUrl the fully-qualified URL of the icon
	 * @param quadrant in which the decoration should appear, from {@link IDecoration} constants
	 */
	RootDecorator(URL iconUrl, int quadrant) {
		this(ImageDescriptor.createFromURL(iconUrl), quadrant);
	}
	
	/**
	 * Initialises default values for decoration.
	 * @param iconPath the path (relative to the plugin directory) of the icon, e.e. <code>"icons/blah.png"</code>
	 * @param quadrant in which the decoration should appear, from {@link IDecoration} constants
	 */
	RootDecorator(String iconPath, int quadrant) {
		this(FileUtils.findURL(ProofGeneralPlugin.getDefault().getBundle(),iconPath), quadrant);
	}
	
	/**
	 * Performs decoration according to the parameters given in the constructor.
	 * This can be used by subclasses as follows:
	 * <pre>
	 * public void decorate(Object element, IDecoration decoration) {
	 * 		if (condition) {
	 * 			super.decorate(element, decoration);
	 * 		}
	 * }
	 * </pre>
	 * @see org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang.Object, org.eclipse.jface.viewers.IDecoration)
	 */
	public void decorate(Object element, IDecoration decoration) {
		// DEBUG
		/*
		System.out.println("decorating element "+element.toString()+" for "+this.toString());
		for ( String s : decoration.getDecorationContext().getProperties() ) {
			System.out.println("decoration property ["+s+"="+decoration.getDecorationContext().getProperty(s)+"]");
		} */
		if (descriptor != null) {
			decoration.addOverlay(descriptor,quadrant);
		} else {
			// DEBUG
			// System.err.println("no image descriptor could be found or created.");
		}
	}

	/**
	 * Does nothing.
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	public void addListener(ILabelProviderListener listener) {
	}

	/**
	 * Does nothing.
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	public void dispose() {
	}

	/**
	 * Always returns false.
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
	 */
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	/**
	 * Does nothing.
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	public void removeListener(ILabelProviderListener listener) {
	}
}