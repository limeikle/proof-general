/*
 *  $RCSfile: ProofScriptDocumentProvider.java,v $
 *
 *  Created on 29 Mar 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.document;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.FileDocumentProvider;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.editor.lightparser.PartitionScanner;
import ed.inf.proofgeneral.sessionmanager.ProverInfo;
import ed.inf.proofgeneral.sessionmanager.SessionManager;

/**
 * Document provider for proof scripts.
 */
public class ProofScriptDocumentProvider extends FileDocumentProvider
{

	/**
	 *
	 * @see org.eclipse.ui.editors.text.StorageDocumentProvider#createEmptyDocument()
	 */
	@Override
    protected ProofScriptDocument createEmptyDocument() {
		return new ProofScriptDocument();
	}

	/**
	 * Makes a document, standardly.
	 * Adds it to the relevant session manager.
	 * Converts symbols, if symbol support is set.
	 */
	@Override
    public ProofScriptDocument createDocument(Object element) throws CoreException {
		IResource res = null;
		String name = "";
		
		IDocument doc = super.createDocument(element);
		if (doc!=null) {
			if (element instanceof IEditorInput) {
				IEditorInput inp = (IEditorInput) element;
				name = inp.getName();
				res = getResource(inp); // may return null				
			}
		}
		if (doc==null && element instanceof IFile) {
			doc = createEmptyDocument();
			setDocumentContent(doc, ((IFile) element).getContents(), ((IFile) element).getCharset());
			res = (IResource) element;
			name = res.getName();
		}
		
		if (res==null) {
			if (doc!=null) {
				throw new CoreException(new Status( IStatus.ERROR,
						ProofGeneralPlugin.getDefault().getBundle().getSymbolicName(),
						IStatus.OK,
						"Error opening document for scripting: can't get resource on element\n(Note: opening files outside workspace is not supported, please use File -> Import instead to add the file to a project).",
						null));
			}
			return null;
		}
		
		setupDocument((ProofScriptDocument) doc, name, res);
		
		return (ProofScriptDocument) doc;
	}
	
//	/**
//	 * Return a new document on the given resource, or null if one cannot be created.
//	 * @param res
//	 * @return th new document
//	 */
//	public IDocument createDocument(IResource res) {
//		try {
//			ProofScriptDocument doc = this.setupDocument(res,res.getName(),res);
//			// FIXME: does this set the contents?  Do we need to get an input somehow?
//			return doc;
//		} catch (CoreException e) {
//			return null;
//		}
//	}

	private void setupDocument(ProofScriptDocument document, String name, IResource res)
		throws CoreException {
		SessionManager sm = ProofGeneralPlugin.getSessionManagerForFile(name);
			
		document.init(name, sm.proverInfo.syntax, sm.proverInfo.proverSyntax, 
				sm.getProver(), res);

		IDocumentPartitioner partitioner = createProofScriptPartitioner(sm.proverInfo);
		document.setDocumentPartitioner(PartitionScanner.LIGHT_PG_PARTITIONING,partitioner);
		sm.controlDocument(document);
		sm.autoParseDoc(document);
		partitioner.connect(document);
	}	


	/**
	 * Returns the resource underlying a given input, where possible.
	 * Used to create a document or place a marker.
	 * Returns <code>null</code> if there is no applicable resource. This
	 * queries the editor's input using <code>getAdapter(IResource.class)</code>.
	 *
	 * @return the resource underlying the given editor input
	 */
	protected static IResource getResource(IEditorInput input) {
		return (IResource) input.getAdapter(IResource.class);
	}


	/**
	 * Return a new partitioner for proof script files.
	 */
	public IDocumentPartitioner createProofScriptPartitioner(ProverInfo pi) {
	    return new FastPartitioner(PartitionScanner.getDefault(pi), PartitionScanner.PARTITION_TYPES);
	}

	/**
	 * Register this element as not dirty.  Used when symbol support
	 * is turned on or off but no real change to the document occurs.
	 * @param element
	 */
	public void setClean(Object element) {
		ElementInfo info = getElementInfo(element);
		info.fCanBeSaved = false;
		addUnchangedElementListeners(element, info);
		fireElementDirtyStateChanged(element, info.fCanBeSaved);
	}

	/**
	 * Indicate element is newly dirty.  Used when symbol support
	 * is turned on/off and the document is already dirty;
	 * should trigger a resync of the quick diff reference.
	 * @param element
	 */
	public void setDirtyAgain(Object element) {
		ElementInfo info = getElementInfo(element);
		info.fCanBeSaved = true;
		fireElementDirtyStateChanged(element, info.fCanBeSaved);
	}

	/**
	 * Creates a new element info object for the given element.
	 * <p>
	 * This method is called from <code>connect</code> when an element info needs
	 * to be created. The <code>AbstractDocumentProvider</code> implementation
	 * of this method returns a new element info object whose document and
	 * annotation model are the values of <code>createDocument(element)</code>
	 * and  <code>createAnnotationModel(element)</code>, respectively. Subclasses
	 * may override.</p>
	 *
	 * @param element the element
	 * @return a new element info object
	 * @exception CoreException if the document or annotation model could not be created
	 */
	@Override
    protected ElementInfo createElementInfo(Object element) throws CoreException {
		if (element instanceof IFileEditorInput) {

			IFileEditorInput input= (IFileEditorInput) element;

			try {
				refreshFile(input.getFile());
			} catch (CoreException x) {
				handleCoreException(x,"ProofScriptDocumentProvider.createElementInfo"); //$NON-NLS-1$
			}

			IDocument d= null;
			IStatus s= null;

			try {
				d= createDocument(element);
			} catch (CoreException x) {
				s= x.getStatus();
				d= createEmptyDocument();
			}

			IAnnotationModel m= createAnnotationModel(element);
			FileSynchronizer f= new FileSynchronizer(input);
			f.install();

			FileInfo info= new PGElementInfo(d, m, f);
			info.fModificationStamp= computeModificationStamp(input.getFile());
			info.fStatus= s;
			info.fEncoding= getPersistedEncoding(input);

			return info;
		}
		return super.createElementInfo(element);
	}


	protected class PGElementInfo extends FileInfo {
		public PGElementInfo(IDocument document, IAnnotationModel model, FileSynchronizer fileSynchronizer) {
			super(document,model,fileSynchronizer);
		}
		/**
		 * Blocks dirty-state changes caused by dummy (lock/unlock) events
		 * @see IDocumentListener#documentChanged(DocumentEvent)
		 */
		@Override
        public void documentChanged(DocumentEvent event) {
			if (event instanceof ScriptManagementDocumentEvent) {
				return;
			}
			super.documentChanged(event);
		}

	}

}
