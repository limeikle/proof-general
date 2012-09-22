/*
 *  $RCSfile: OpenDefinition.java,v $
 *
 *  Created on 11 Nov 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.actions.retargeted;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.part.FileEditorInput;

import ed.inf.proofgeneral.actions.OpenBookmarkAction;
import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.editor.PGTextHover;
import ed.inf.proofgeneral.editor.ProofScriptEditor;
import ed.inf.proofgeneral.sessionmanager.ProverKnowledge;
import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.utils.eclipse.EclipseMethods;
import ed.inf.utils.exception.KnownException;


/**
 * Try to find and open the definition for
 * the word under the cursor.
 * TODO: should also work from selection ranges as well as individual words.
 @author Daniel Winterstein
 */
 public class OpenDefinition extends PGRetargetableAction { 

	 // constructor seems needed for some automated creation..
	 public OpenDefinition() {
		 this(null);
	 }
	 
	public OpenDefinition(ProofScriptEditor editor) {
		super(editor);
		setText("Open Definition");
		setToolTipText("Try to find where a term was defined, and show that definition.");
	}

	/**
	 * The term to look for.
	 */
	String term = "";

	@Override
    public void run() {
		if (super.isBusy()) {
			return;
		}
		super.setBusy();
		try {
			ProofScriptEditor editor = getEditorForRunningAction();
			ProofScriptDocument doc = getDocumentForRunningAction();
			if (term.equals("")) {
				if (editor==null || doc==null) {
					throw new Exception("No script document or editor found.");
				}
				int offset = editor.getCaretOffset();
				IRegion r = PGTextHover.getWordRegion(doc,  offset);
				if (r==null) {
					throw new KnownException("Could not identify a word at the current location");
				}
				term = doc.get(r.getOffset(),r.getLength());
			}
			SessionManager sm = getSessionManagerForRunningAction(); 
			if (sm != null) {
				ProverKnowledge.KnowledgeItem pki =
					sm.getProverState().getProverKnowledge().getItem(term);
				IMarker m = PGTextHover.findMatchingMarker(doc.getResource(),term);
				if (pki!=null) {
					if (pki.marker!=null) {
						goToMarker(pki.marker);
					}
					else if (m!=null) {
						goToMarker(m);
					}
					else {
						goToKnowledgeItem(pki);
					}
				} else {
					throw new KnownException("No definition found for "+term);
				}
			}
		} catch (Exception x) {
			error(x);
			if (!(x instanceof KnownException)) {
				x.printStackTrace();
			}
		} finally {
			super.clearBusy();
		}
	}

	/**
	 * @param m
	 */
	private void goToMarker(IMarker m) {
		OpenBookmarkAction oba = new OpenBookmarkAction(m);
		oba.run();
	}

	/**
	 * @param pki
	 * @throws KnownException
	 * @throws PartInitException
	 */
	private void goToKnowledgeItem(ProverKnowledge.IKnowledgeItem pki) throws KnownException,
	PartInitException {
		String f = pki.getTheory().getFile();
		if (f==null) {
			//TODO prompt for source file
			Shell shell = EclipseMethods.getShell();
			FileDialog fd = new FileDialog(shell);
			fd.setText("Locate File for Theory "+pki.getTheory().getName());
			f = fd.open();
			if (f==null) {
				return; //user cancel
			}
		}
		IEditorPart editor = getEditorForRunningAction();
		if (editor==null) {
			throw new KnownException("OpenDefinition: no editor for "+f);
		}
		//code taken from OpenExternalFileAction
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		IPath location= new Path(f);
		IFile[] files= workspace.getRoot().findFilesForLocation(location);
		if (files==null || files.length==0) {
			throw new KnownException("OpenDefinition: file '"+f+"' for "+term+" not found");
		}
		if (files.length>1) {
			System.out.println("Multiple matches found for "+f+"; choosing "+files[0].getFullPath().toOSString());
		}
		IEditorInput input = new FileEditorInput(files[0]);
		IEditorRegistry editorRegistry = editor.getEditorSite().getWorkbenchWindow().getWorkbench().getEditorRegistry();
		IEditorDescriptor descriptor= editorRegistry.getDefaultEditor(files[0].getName());
		String editorId = EditorsUI.DEFAULT_TEXT_EDITOR_ID;
		if (descriptor != null) {
			editorId = descriptor.getId();
		}
		IWorkbenchPage page= editor.getEditorSite().getPage();
		IEditorPart kfep = page.openEditor(input, editorId);
		//then find document
		if (kfep instanceof ProofScriptEditor) {
			String text = ((ProofScriptEditor)kfep).getDocument().get();
			int loc = text.indexOf(term);
			if (loc==-1) {
				throw new KnownException("'"+term+"' was not found in this document.");
			}
			((ProofScriptEditor)kfep).scrollToViewPosition(loc);
//			IMarker m = PGMarkerMethods.addMarker(this, new Position(ofs2, 0), "Scroll to position", PGMarkerMethods.PG_GOTO_MARKER,
//			"Temporary placeholder for scrolling to line "+line);
//			//((ProofScriptEditor)ProofGeneralPlugin.getActiveEditor()).gotoMarker(m);  //deprecated
//			IGotoMarker gm = (IGotoMarker)((ProofScriptEditor)ProofGeneralPlugin.getActiveEditor()).getAdapter(IGotoMarker.class);
//			gm.gotoMarker(m);
//			m.delete();
		}
	}

	/**
	 * manually sets the term to look up (must be cleared with setTerm(null) to
	 * get caret item; but typically we have two instances,
	 * one for editor context menu (which uses this),
	 * and another for the drop down (which takes from editor current position)
	 */
	public void setTerm(String word) {
		term = word;
	}

}
