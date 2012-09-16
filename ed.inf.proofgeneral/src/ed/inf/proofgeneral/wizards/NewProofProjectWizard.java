package ed.inf.proofgeneral.wizards;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.ProofGeneralProverRegistry.Prover;
import ed.inf.proofgeneral.ProofGeneralProverRegistry.ProverRegistryException;
import ed.inf.proofgeneral.builder.ProofNature;
import ed.inf.proofgeneral.editor.symbols.SymbolTableFiles;
import ed.inf.proofgeneral.preferences.ProverPreferenceNames;

public class NewProofProjectWizard extends BasicNewProjectResourceWizard {

	// We're not supposed to subclass BasicNewProjectResourceWizard, but it's a lot
	// less trouble than making an instance here and extending NewWizard.
	//protected BasicNewProjectResourceWizard wizard;

	public NewProofProjectWizard() {
	}
	
	/**
	 * Return a prover for this project.  Should be overridden by prover-specific subclasses.
	 * @return the prover for this project
	 * @throws ProverRegistryException
	 */
	public Prover getProver() throws ProverRegistryException {
		return ProofGeneralPlugin.getRegistry().getDefaultProver();
	}

	@Override
	public boolean performFinish() {
		boolean res= super.performFinish();
		// da: add proof nature to created project. Unless this should be automatic?
		if (res) {
			try {
				setupNewProject(getNewProject(), getProver());				
			} catch (CoreException e) {
				e.printStackTrace();
			} catch (ProverRegistryException e) {
				e.printStackTrace();
			}
		}
		return res;
	}

	/** invokes the various methods needed to setup a new proof project, with the given prover;
	 * 
	 * @param project project to setup as Proof project, must exist and be open
	 * @param prover optional prover to use (no prover if null)
	 * @throws CoreException
	 */
	public static void setupNewProject(final IProject project, Prover prover)
            throws CoreException {
		addProofProjectNature(project);
		if (prover!=null) {
			addProverToProofNature(project, prover);						
			// add new symbol table.
			SymbolTableFiles.getDefault().createDefaultTable(prover, project, false);
			runProofProjectSetupCommand(project, prover.getStringPref(ProverPreferenceNames.PREF_PROVER_PROJECT_SETUP_COMMAND));
		}
    }

	/** Run setup command, if one given, after completion (does nothing if arg is null) */
	public static void runProofProjectSetupCommand(final IProject project, final String projectSetupCommand)
            throws CoreException {
	    if (projectSetupCommand != null && !projectSetupCommand.equals("")) {
	    	IWorkspaceRunnable r = new IWorkspaceRunnable() {
	    		public void run(IProgressMonitor monitor) {
	    			try {
	    				File dir = project.getLocation().toFile();
	    				String cmd = projectSetupCommand.replaceAll(Pattern.quote("%PROJECTDIR%"), dir.getAbsolutePath());
	    				cmd = cmd.replaceAll(Pattern.quote("%PROJECTNAME%"), project.getName());
	                    Process process = Runtime.getRuntime().exec(cmd,null,dir);
	                    BufferedReader reader = new BufferedReader(new InputStreamReader(
	    						process.getInputStream()));
	                    String str;
	                    while ((str = reader.readLine()) != null) {
	                    	System.out.println(projectSetupCommand + ": " + str);
	                    }
	                    reader.close();
	                    if (process.waitFor() != 0) {
	                    	// TODO: either no longer throw an exception, or catch further up with
	                    	//		 an option to alter/re-run the command or explicitly ignore the failure?
	                    	throw new IOException("Project intialisation command " + projectSetupCommand + 
	                    	" exited abnormally.");
	                    }
	                    project.refreshLocal(IResource.DEPTH_INFINITE, null);
	    			} catch (IOException e) {
	    				e.printStackTrace();
	    			} catch (CoreException e) {
	    				e.printStackTrace();
	    			} catch (InterruptedException e) {
	    				e.printStackTrace();
	    			}
	    		}
	    	};
	    	project.getWorkspace().run(r, null, IWorkspace.AVOID_UPDATE, null);
	    }
    }

	/** adds the indicated prover to the proof nature of the given project */
	public static void addProverToProofNature(final IProject project, Prover prover) throws CoreException {
	    // Set the prover in the project nature
	    ProofNature proofNature = (ProofNature) project.getNature(ProofNature.NATURE_ID);
	    proofNature.setProver(prover);
    }

	/** adds the ProofNature to the project */
	public static void addProofProjectNature(final IProject project) throws CoreException {
	    IProjectDescription description = project.getDescription();
	    String[] natures = description.getNatureIds();
	    String[] newNatures = new String[natures.length + 1];
	    System.arraycopy(natures, 0, newNatures, 0, natures.length);
	    newNatures[natures.length] = ProofNature.NATURE_ID;
	    description.setNatureIds(newNatures);
	    
	    project.setDescription(description, null);
    }
	
	
}
