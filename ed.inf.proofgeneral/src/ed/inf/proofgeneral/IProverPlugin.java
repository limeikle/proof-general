package ed.inf.proofgeneral;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;


/**
 * Interface for prover plugins, must be implemented by plugin activator class.
 */
public interface IProverPlugin {

	public ImageDescriptor getImageDescriptor(String path);
	
	public String getPluginId();
	
	public AbstractUIPlugin getPlugin();
}
