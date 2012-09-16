package ed.inf.proofgeneral.prover.coq;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import ed.inf.proofgeneral.IProverPlugin;

public class CoqProverPlugin implements IProverPlugin {

	// Non-null
	private final Activator plugin; 

	public CoqProverPlugin() throws Exception {
		Activator pplugin = Activator.getDefault(); 
		if (pplugin == null) {
			throw new Exception("Cannot instantiate prover class, plugin not started");
		}
		this.plugin = pplugin;
	}
	
	public String getPluginId() {
		return Activator.PLUGIN_ID;
	}

	public ImageDescriptor getImageDescriptor(String path) {
		return Activator.getImageDescriptor(path);
	}
	
	public AbstractUIPlugin getPlugin() {
		return plugin;
	}
}