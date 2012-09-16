package ed.inf.proofgeneral.explorer;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.navigator.ILinkHelper;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Link resources in Proof Explorer to active editor.  
 */
public class ResourceLinkHelper implements ILinkHelper {

	// This code is copied from org.eclipse.ui.internal.navigator.resources.workbench.ResourceLinkHelper,
	// in Eclipse 3.3.
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.ILinkHelper#findSelection(org.eclipse.ui.IEditorInput)
	 */
	public IStructuredSelection findSelection(IEditorInput anInput) {
		IFile file = ResourceUtil.getFile(anInput);
		if (file != null) {
			return new StructuredSelection(file);
		}
		return StructuredSelection.EMPTY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.navigator.ILinkHelper#activateEditor(org.eclipse.ui.IWorkbenchPage, org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void activateEditor(IWorkbenchPage aPage,
			IStructuredSelection aSelection) {
		if (aSelection == null || aSelection.isEmpty())
			return;
		if (aSelection.getFirstElement() instanceof IFile) {
			IEditorInput fileInput = new FileEditorInput((IFile) aSelection.getFirstElement());
			IEditorPart editor = null;
			if ((editor = aPage.findEditor(fileInput)) != null)
				aPage.bringToTop(editor);
		}

	}

}
