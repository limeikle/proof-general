/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jan 10, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */

package ed.inf.proofgeneral.pgip.editor;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.ProofScriptDocument;

/**
 *
 */
public class PGIPEditorInput implements IEditorInput {

	/** The underlying document.   This is our descriptor of the editor input.  It is never null. */
	private ProofScriptDocument doc;
	
	public PGIPEditorInput(ProofScriptDocument doc) {
		assert doc != null : "Null argument for PGIP Editor input invalid";
		this.doc = doc;
	}
	/**
	 * @see org.eclipse.ui.IEditorInput#exists()
	 */
	public boolean exists() {
		return false; // most-recently used given by underlying file
	}

	/**
	 * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
	 */
	public ImageDescriptor getImageDescriptor() {
		return ProofGeneralPlugin.getImageDescriptor("icons/star16.gif");
	}

	/**
	 * @see org.eclipse.ui.IEditorInput#getName()
	 */
	public String getName() {
		return "PGIP:" + doc.getTitle();
	}

	/**
	 * @see org.eclipse.ui.IEditorInput#getPersistable()
	 */
	public IPersistableElement getPersistable() {
		return null;
	}

	/**
	 * @see org.eclipse.ui.IEditorInput#getToolTipText()
	 */
	public String getToolTipText() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @return the doc
	 */
	public ProofScriptDocument getDoc() {
		return doc;
	}
}
