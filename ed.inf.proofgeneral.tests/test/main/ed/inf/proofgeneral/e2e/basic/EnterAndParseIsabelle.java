package ed.inf.proofgeneral.e2e.basic;

import java.io.ByteArrayInputStream;

import junit.framework.TestCase;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.ProofGeneralProverRegistry.Prover;
import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.document.ProofScriptDocumentProvider;
import ed.inf.proofgeneral.editor.actions.retargeted.SendAllAction;
import ed.inf.proofgeneral.prover.isabelle.wizards.NewIsabelleProofScriptWizard;
import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.proofgeneral.wizards.NewProofProjectWizard;
import ed.inf.utils.eclipse.SimpleProgressMonitor;

public class EnterAndParseIsabelle extends TestCase {

	private IProject project = null;
	private ProofScriptDocument doc;
	private SessionManager sm;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		if (ProofGeneralPlugin.getDefault()==null)
			throw new IllegalStateException("ProofGeneral plugin not active.");
		project = newProofProject("test-project", ProofGeneralPlugin.getRegistry().getDefaultProver());
	}
	
	public static IProject newProofProject(String name, Prover prover) throws CoreException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject proj = root.getProject(name);
		if (proj.exists())
			throw new UnsupportedOperationException("Project named '"+name+"' already exists");
		SimpleProgressMonitor monitor = new SimpleProgressMonitor();
		proj.create(monitor);
		monitor.blockBrieflyAndLog();
		proj.open(monitor);
		monitor.blockBrieflyAndLog();
		NewProofProjectWizard.setupNewProject(proj, prover);
		return proj;
	}
	
	@Override
	protected void tearDown() throws Exception {
		if (project!=null) {
			//delete project after use
			SimpleProgressMonitor monitor = new SimpleProgressMonitor();
			project.delete(true, monitor);
			monitor.blockBrieflyAndLog();
		}
		
		super.tearDown();
	}
	
	protected void createIsabelleFileWithInnerContents(String innerContents) throws CoreException {
		final IFile file = project.getFile(new Path("Foo.thy"));
		SimpleProgressMonitor monitor = new SimpleProgressMonitor();
		file.create(new ByteArrayInputStream(
				NewIsabelleProofScriptWizard.getDefaultContentsForTheoryName("Foo").getBytes()), 
				true, monitor);
		monitor.blockBrieflyAndLog();
		//TODO PARSE
		System.out.println("TODO");
		//foo
		
		doc = new ProofScriptDocumentProvider().createDocument(file);
		sm = ProofGeneralPlugin.getSessionManagerForFile(doc.getTitle());
		
		String contents = doc.get();
		
		int end = contents.lastIndexOf("\n"+"end");
		if (end==-1) fail("'end' not found in document");
		
		contents = contents.substring(0, end);
		contents += "\n" +
			innerContents + "\n" +
			"end";
		
	}

	public void testEmpty() throws CoreException {
		createIsabelleFileWithInnerContents("");
		
		new SendAllAction(doc, sm).run();
	}

	public void testLemma() throws CoreException {
		createIsabelleFileWithInnerContents(
				"lemma foo: \"x = x\"\n" +
				"apply simp"+"\n"+
				"done");
		
		new SendAllAction(doc, sm).run();
	}
	
	public void testCommentAndLemma() throws CoreException {
		createIsabelleFileWithInnerContents(
				"(* this is a test comment *)" + "\n" +
				"lemma foo: \"x = x\"\n" +
				"apply simp"+"\n"+
				"done");
		
		new SendAllAction(doc, sm).run();
	}
	
}
