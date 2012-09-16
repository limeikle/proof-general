/*
 *  $RCSfile: ProofNature.java,v $
 *
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.builder;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

import ed.inf.proofgeneral.ProofGeneralProverRegistry.Prover;

// TODO: this should perhaps become ProofProject class and moved to own package.
// Hold off for now until multiple branches resolved and we can design this
// carefully.

public class ProofNature implements IProjectNature {

	/**
	 * ID of this project nature
	 */
	public static final String NATURE_ID = "ed.inf.proofgeneral.proofNature";

	private IProject project = null;

	private Prover prover;  // Configured prover for this project (TODO: not yet used)
	
	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.resources.IProjectNature#configure()
	 */
	public void configure() throws CoreException {
		IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();

		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(ProofBuilder.BUILDER_ID)) {
				return;
			}
		}

		ICommand[] newCommands = new ICommand[commands.length + 1];
		System.arraycopy(commands, 0, newCommands, 0, commands.length);
		ICommand command = desc.newCommand();
		command.setBuilderName(ProofBuilder.BUILDER_ID);
		newCommands[newCommands.length - 1] = command;
		desc.setBuildSpec(newCommands);
		project.setDescription(desc, null);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.resources.IProjectNature#deconfigure()
	 */
	public void deconfigure() throws CoreException {
		IProjectDescription description = getProject().getDescription();
		ICommand[] commands = description.getBuildSpec();
		for (int i = 0; i < commands.length; ++i) {
			if (commands[i].getBuilderName().equals(ProofBuilder.BUILDER_ID)) {
				ICommand[] newCommands = new ICommand[commands.length - 1];
				System.arraycopy(commands, 0, newCommands, 0, i);
				System.arraycopy(commands, i + 1, newCommands, i,
						commands.length - i - 1);
				description.setBuildSpec(newCommands);
				return;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.resources.IProjectNature#getProject()
	 */
	public IProject getProject() {
		return project;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.resources.IProjectNature#setProject(org.eclipse.core.resources.IProject)
	 */
	public void setProject(IProject project) {
		this.project = project;
	}

	/**
     * @return the prover
     */
    public Prover getProver() {
    	return prover;
    }

	/**
     * @param prover the prover to set
     */
    public void setProver(Prover prover) {
    	this.prover = prover;
    }

}
