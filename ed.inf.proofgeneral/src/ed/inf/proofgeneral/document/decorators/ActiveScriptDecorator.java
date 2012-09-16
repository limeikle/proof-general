/*
 *  $RCSfile: ActiveScriptDecorator.java,v $
 *
 *  Created 30 Jan 2007 by Graham Dutton
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.document.decorators;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.sessionmanager.SessionManager;

/**
 * Decorates the corner of scripts depending on one of three states:
 * <ol>
 * <li>Script is currently active in the prover -- a pink square</li>
 * <li>Script is <b>not</b> active, but has been processed already -- a blue square</li>
 * <li>Script is not active and has not been processed (or has been unloaded already) -- no decoration</li>
 * </ol>
 */
public class ActiveScriptDecorator extends RootDecorator {

	/** Active script file */
	private static ImageDescriptor PINK = ProofGeneralPlugin.getImageDescriptor("icons/dec_pink.png");
	/** Locked (fully processed) files. */
	private static ImageDescriptor BLUE = ProofGeneralPlugin.getImageDescriptor("icons/dec_blue.png");
	/** Last-known active session manager */
	private SessionManager sm = null;
	
	/**
	 * Constructs with current quadrant.
	 */
	public ActiveScriptDecorator() {
		super((ImageDescriptor) null, IDecoration.BOTTOM_LEFT);
	}

	/**
	 * Tests if a resource is the active script.
	 * @param r the resource to check (must not be null).
	 * @return true if this is the active script; false otherwise
	 */
	private boolean isActive(IResource r) {
		try {
			ProofScriptDocument psd = null;
			if (sm != null) {
				psd = sm.getActiveScript();
			}
			if (psd != null) {
				return psd.getResource().equals(r);
			}
		} catch (Exception e) {
			if (ProofGeneralPlugin.debug(this)) e.printStackTrace();
		}
		return false;

	}

	/**
	 * Tests if a resource has been processed and is 'loaded' by the prover.
	 * Note that 'fully processed' implies 'locked'.
	 * @param r the resource to check
	 * @return true if this has been processed, false otherwise.
	 * // TODO - doesn't yet work
	 */
	private boolean isProcessed(IResource r) {
		return ( sm != null ) ? sm.isLocked(r.getLocationURI()) : false;
	}
	
	/**
	 * Performs decoration based on the status of the file.
	 * Plugin now guarantees that only ProofScriptDocuments will be passed here.
	 * @see org.eclipse.jface.viewers.ILightweightLabelDecorator#decorate(java.lang.Object, org.eclipse.jface.viewers.IDecoration)
	 */
	@Override
    public void decorate(Object element, IDecoration decoration) {
		if (! (element instanceof IFile )) return;
		
		sm = ProofGeneralPlugin.getSomeSessionManager();
		IFile f = (IFile) element;
		
		if (isActive(f)) {
			super.descriptor = PINK;
			super.decorate(element, decoration);
		}

		if (isProcessed(f)) {
			super.descriptor = BLUE;
			super.decorate(element, decoration);
		}

	}

	/**
	 * Returns true if we are interested in an element property, i.e. if a change in property will change the label.
	 * @return <code>true</code> if the label would be affected,
     *    		<code>false</code> otherwise
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
	 * TODO remove most of this code, as we probably don't need to use properties.
	 */
	@Override
    public boolean isLabelProperty(Object element, String property) {
		System.err.println("isLabelProperty("+element+","+property+")...");
		return property.equals(ProofScriptDocument.IsActive.getLocalName());
	}

}
