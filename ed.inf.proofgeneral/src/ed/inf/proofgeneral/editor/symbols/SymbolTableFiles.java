/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on 20 Dec 2006 by grape
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *
 */

package ed.inf.proofgeneral.editor.symbols;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.FileEditorInput;

import ed.inf.proofgeneral.Constants;
import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.ProofGeneralProverRegistry.Prover;
import ed.inf.proofgeneral.ProofGeneralProverRegistry.ProverRegistryException;
import ed.inf.proofgeneral.preferences.ProverPreferenceNames;
import ed.inf.proofgeneral.sessionmanager.ScriptingException;
import ed.inf.proofgeneral.symbols.SymbolTableEditor;
import ed.inf.utils.file.FileUtils;

/**
 * Methods for management of the Symbol table files.
 * To get the current default symbol table, use the following:
 * SymbolTableFiles().getDefaultTable(IProject);
 * where IProject is the project of the currently-edited file.
 * TODO allow IProject == null.
 */
public class SymbolTableFiles implements IObjectActionDelegate {

	private static final IFileSystem fileSystem = EFS.getLocalFileSystem();
	private static final String prefF =	Constants.PROJECT_SYMBOL_TABLE_NAME;

	private IFileStore tempStore;
	private IFileStore fileStore;
	private ISelection selection;

	private static class SingletonHolder {
		private static SymbolTableFiles fDefault = new SymbolTableFiles();
	}
	
	/**
	 * 
	 */
	// FIXME: this should be private, don't want others to make objects
	// At the moment get a log exception if this is private
	public SymbolTableFiles() { 
			
	}

	/**
	 * Get singleton instance
	 */
	public static SymbolTableFiles getDefault() {
		return SingletonHolder.fDefault;
	}
	
	/**
	 * Gets the default symbol table file location from the preferences.
	 * @returns the path found
	 * @throws ScriptingException if the file cannot be found
	 */
	public IPath getDefaultTable(Prover p) throws ScriptingException {
		String prefT = p.getStringPref(ProverPreferenceNames.PREF_DEFAULT_SYMBOL_TABLE);
		if (prefT.equals("")) {
	    	throw new ScriptingException("No symbol table set for prover " + p.getName() + " (preference \"" + 
	    			ProverPreferenceNames.PREF_DEFAULT_SYMBOL_TABLE + "\" is not set)");
		}
		File defaultsymfile = FileUtils.findFileExt(p.getName(),prefT);
    	if (defaultsymfile != null) {
    		IPath pathT = Path.fromOSString(defaultsymfile.getAbsolutePath());
    		if (pathT != null) {
    			tempStore = fileSystem.getStore(pathT);
    			return pathT;
    		}
    	}
    	throw new ScriptingException("Symbol table file (\"" + prefT + "\") cannot be found.");
	}

	/**
	 * Gets the default file location from preferences, and returns the
	 * path for the default symbol table in the given project.
	 * @param project the project for which to find the default symbol table
	 * @return path to the default symbol table.
	 */
	public IPath getDefaultTablePath(IProject project) {
		if (project == null) {
			project = findDefaultProject();
		}

		IPath path = null;
		try {
			URI current = new URI(project.getLocationURI()+"/"+prefF);
			fileStore = fileSystem.getStore(current);
			path = new Path(current.toString());
			//System.out.println("default path: "+path.toString()); // DEBUG
		} catch (URISyntaxException e) {
			System.err.println("Invalid Symbol Table name.  Please check preferences.");
		}
		return path;
	}

	/**
	 * Gets the currently active project.
	 * FIXME: currently just finds first project which exists.
	 * @return the currently active project, or null if none exist.
	 */
	private static IProject findDefaultProject() {
		// TODO: find current project
		// currently just finds first project which exists.
		for (IProject pj : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			if (pj.exists()) {
				//System.out.println("symbol table for project: "+pj.getName());
				return pj;
			}
		}

		return null;
	}

	/**
	 * Copies the template table to the default table location.
	 * FIXME: currently will not always work with project == null, since {@link #findDefaultProject()} is broken.
	 * @param overwrite overwrite existing table?
	 * @return the created table, or the preexisting one.
	 */
	public IPath createDefaultTable(Prover p, IProject project, final boolean overwrite) {
		IPath def = null;
		// initialise file stores
		def = getDefaultTablePath(project);
		// if default file already exists...
		if (fileSystem.getStore(def).fetchInfo().exists() && !overwrite) {
			return def;
		}
		// no problem.  just recreate.
		// find source for copy.
		try {
			getDefaultTable(p); // initialise
		} catch (ScriptingException e) {
			System.err.println("error getting template:");
			e.printStackTrace();
			return def;
		} try { // perform copy.
			int opts = (overwrite?EFS.OVERWRITE:EFS.NONE);
			tempStore.copy(fileStore, opts, null);
			// TODO: notify change so navigator picks it up without refresh?
		} catch (CoreException c) {
			if (overwrite) {
				c.printStackTrace(); // ignore overwrite-failed notices
			}
		} catch (Exception e) {
			System.err.println("error performing copy: "+e.toString());
		}
		return getDefaultTablePath(project);
	}

	/**
	 * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction, org.eclipse.ui.IWorkbenchPart)
	 * does nothing.
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		/* do nothing. */
	}

	/**
	 * Opens the default symbol table, creating it if necessary.
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		// get current project from selection
		if (selection instanceof IStructuredSelection) {
			IProject project;

			for (Iterator it = ((IStructuredSelection) selection).iterator(); it
					.hasNext();) {
				Object element = it.next();
				project = null;
				if (element instanceof IProject) {
					project = (IProject) element;
				} else if (element instanceof IAdaptable) {
					project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
				}
				if (project != null) {
					// now create the symbol table...
					try {
						// FIXME: should choose the prover from the project metadata, if possible.
						Prover prover = ProofGeneralPlugin.getRegistry().getDefaultProver();
						createDefaultTable(prover, project, false);
					} catch (ProverRegistryException e) {
						// nothing for now
					}
					//IPath path = createDefaultTable(project, false);
					// openDefaultSymbolTableFor(path);
					break;
				}
			}
		}
	}

	/**
	 * Opens the symbol table editor on the given symbol table.
	 * @param table the symbol table to open.
	 * @return true if the operation was successful; false otherwise.
	 * FIXME currently does not work.
	 */
	public static boolean openDefaultSymbolTableFor(IPath table) {
		try {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IWorkbenchPage page = ProofGeneralPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage();

			IFile file = root.findFilesForLocation(table)[0];
			System.err.println("file: "+file);
			IEditorInput einput = new FileEditorInput(file);
			org.eclipse.ui.ide.IDE.openEditor(page,einput,SymbolTableEditor.EDITORID,true);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 * Sets the active selection.  Used by {@link #run(IAction)}.
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;  
	}

}
