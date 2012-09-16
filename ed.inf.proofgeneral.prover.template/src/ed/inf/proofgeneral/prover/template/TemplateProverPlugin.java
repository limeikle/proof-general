package ed.inf.proofgeneral.prover.template;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import ed.inf.proofgeneral.IProverPlugin;

public class TemplateProverPlugin implements IProverPlugin {

	// Non-null
	private Activator plugin; 

	public TemplateProverPlugin() throws Exception {
		Activator pplugin = Activator.getDefault(); 
		if (pplugin == null) {
			throw new Exception("Cannot instantiate TemplateProver class, plugin not started");
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