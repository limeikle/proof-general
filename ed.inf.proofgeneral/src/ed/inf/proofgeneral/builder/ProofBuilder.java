/*
 *  $RCSfile: ProofBuilder.java,v $
 *
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.builder;

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.CmdElement;
import ed.inf.proofgeneral.document.ProofScriptMarkers;
import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.preferences.PreferenceNames;
import ed.inf.proofgeneral.sessionmanager.PGIPSyntax;
import ed.inf.proofgeneral.sessionmanager.ScriptingException;
import ed.inf.proofgeneral.sessionmanager.SessionManager;

public class ProofBuilder extends IncrementalProjectBuilder {

	class ProofDeltaVisitor implements IResourceDeltaVisitor {
		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
		 */
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			SessionManager sm = smForResource(resource); // implies resource is IFile
			if (sm != null)  {
				switch (delta.getKind()) {
				case IResourceDelta.ADDED:
					// handle added resource: probably it's been added by
					// importing/renaming, so let's try proving it.
					proveProofScript(sm,(IFile) resource);
					break;
				case IResourceDelta.REMOVED:
					// handle removed resource
					// TODO: remove from controlled URIs, maybe?
					break;
				case IResourceDelta.CHANGED:
					// handle changed resource.  Probably it's been saved by
					// an editor, so let's re-parse/prove.
					parseProofScript(sm,(IFile) resource);
					proveProofScript(sm,(IFile) resource);
					break;
				default:
				    break;
				}
			}
			return true; // continue visiting children.
		}
	}

	class ProofResourceVisitor implements IResourceVisitor {
		public boolean visit(IResource resource) throws CoreException {
			SessionManager sm = smForResource(resource); // implies resource is IFile
			if (sm != null) {
				// TODO: would like to prove in order, and give up if have problems on dependent files
				parseProofScript(sm,(IFile) resource);
				proveProofScript(sm,(IFile) resource);
			}
			return true; // continue visiting children.
		}
	}

	public static final String BUILDER_ID = "ed.inf.proofgeneral.proofBuilder";

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
	 *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
    protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {
		if (kind == FULL_BUILD) {
			fullBuild(monitor);
		} else {
			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
				fullBuild(monitor);
			} else {
				incrementalBuild(delta, monitor);
			}
		}
		return null; // da: null return is OK.
	}

	void parseProofScript(SessionManager sm, IFile file) {
		if (ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_PARSE_AUTO)) {
			// Eventually: make parsescript command with file contents to cache parse
			// results.  Don't need a document; cache results of parse in meta data
			// shadow file (true model).  First and for now: if we have a
			// document, parse that, otherwise ignore.
			ProofScriptDocument doc = sm.findDocumentOn(file);
			if (doc != null) {
				sm.autoParseDoc(doc);
			}
		}
	}

	void proveProofScript(SessionManager sm, IFile file) {
		if (ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_PROVE_AUTO)) {
			deleteMarkers(file); // FIXME: probably removes too much [don't want to remove parse warnings, for ex]
			CmdElement loadFileCmd = new CmdElement(PGIPSyntax.LOADFILE);
			try {
				PGIPSyntax.addURLattribute(file.getRawLocationURI(), loadFileCmd);
				// TODO:
				//  -- dependency analysis (for provers that won't do that themselves, so we
				//       queue loads in right order without redundancy);
				//  -- check against "locked" state in SM: if locked, ignore load (assume
				//       has succeeded earlier) OR with timestamp, myaybe remove/reload
				sm.queueCommand(loadFileCmd);
			} catch (ScriptingException e) {
				// This happens if we can't convert the given URI into a URL.
				// Just ignore/TODO: log
			}
		}
	}


	private SessionManager smForResource(IResource resource) throws CoreException {
		if (resource instanceof IFile &&
			ProofGeneralPlugin.hasSessionManagerForFile(resource.getName())) {
				return ProofGeneralPlugin.getSessionManagerForFile(resource.getName());
		}
		return null;
	}


	private void deleteMarkers(IFile file) {
		try {
			file.deleteMarkers(ProofScriptMarkers.PGMARKER, true, IResource.DEPTH_ZERO);
		} catch (CoreException ce) {
		}
	}

	protected void fullBuild(final IProgressMonitor monitor) {
		try {
			// Here we can invoke 
			// ProverPreferenceNames.PREF_PROVER_PROJECT_BATCH_BUILD_COMMAND
			// NB: this is likely to be a (very) long running operation,
			// should use standard workbench process stuff for showing progress
			// in background (although no clear way to get progress reports)
			getProject().accept(new ProofResourceVisitor());
		} catch (CoreException e) {
		}
	}


	protected void incrementalBuild(IResourceDelta delta,
			IProgressMonitor monitor) throws CoreException {
		// the visitor does the work.
		delta.accept(new ProofDeltaVisitor());
	}
}
