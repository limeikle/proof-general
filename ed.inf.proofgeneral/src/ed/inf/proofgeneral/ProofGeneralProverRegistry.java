/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jun 22, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */

package ed.inf.proofgeneral;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import ed.inf.proofgeneral.symbols.SymbolTable;

/*
 * TODO:
 *  - allow to dynamically add/remove configurations with bundle listener
 */

/**
 * Registry of provers which are connected through the prover extension
 * point.
 * 
 * @author David Aspinall
 */
public class ProofGeneralProverRegistry {

	public static final String PROVER_EXTENSIONPOINT_ID = "ed.inf.proofgeneral.prover";
	public static final String PROVER_EXTENSION_NAME_ATTR = "name";
	public static final String PROVER_EXTENSION_ACTIVATOR_ATTR = "class";
	private static final String PROVER_EXTENSION_FILEEXTENSIONS_ATTR = "extensions";
	
	/**
	 * The prover registry record of a prover.   Includes a symbol table
	 * which is  
	 */
	// TODO: clean this up a bit, merge with proverInfo.  Idea: this should maintain
	// prover information which may be common across possibly more than one running
	// instance of a prover.  Thus, sharing symbol table is OK, but might want to 
	// keep identifier info, etc, separate.
	public static class Prover {
		/** Prover name from extension point, key in map */
		private final String name;
		/** File extensions which associate to this prover */
		private final ArrayList<String> fileExtensions = new ArrayList<String>();  
		/** The plugin */
		private final IProverPlugin plugin;
		/** Symbol handling for this prover */
		private final SymbolTable symbols;
		public String getName() {
			return name;
		}
		public Prover(String name, IProverPlugin plugin) {
			this.name = name; 
			this.plugin = plugin;
			// Make an empty symbol table.  It's too early to load it yet
			// since we haven't initialised preferences to find the table on disk.
			this.symbols = new SymbolTable();
		}
		
		AtomicBoolean initialised = new AtomicBoolean(false);
		private void init() {
			if (initialised.getAndSet(true)) return;
			//base PG plugin preferences may not have been loaded yet,
			//which means we haven't loaded the XML for defaults here yet
			ProofGeneralPlugin.getDefault().getPluginPreferences().defaultPropertyNames();
		}
		
        public String getPluginId() {
	        return plugin.getPluginId();
        }        
		public Preferences getProverPreferences() {
			init();
			return plugin.getPlugin().getPluginPreferences();
		}
		public String getStringPref(String prefName) {
			// pre-pend name to account for behaviour of prefix="<proverName> " convention.
			// TODO: make this be automatic in PrefsPageBackend too; remove prefix setting.
			return getProverPreferences().getString(name + " " + prefName);
		}
		
        public AbstractUIPlugin getProverPlugin() {
        	return plugin.getPlugin();
        }
		/**
         * @return the symbols
         */
        public SymbolTable getSymbols() {
        	return symbols;
        }
        /**
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "Prover-"+getName()+"@"+hashCode();
        }
	}
	
	private final HashMap<String,Prover> registry = new HashMap<String,Prover>();
	
	public static class ProverRegistryException extends Exception { 
		public ProverRegistryException(String msg) {
			super(msg);
		}
	};
	
	public void addKnownProver(Prover p) throws ProverRegistryException {
		if (registry.containsKey(p.name)) {
			throw new ProverRegistryException("Prover registry already contains prover " + p.name);
		}
		registry.put(p.name, p);
		System.out.println("Registered prover " + p.name); // TODO: log/debug, not console
	}
	
	public void updateConnectedProvers() {
		IExtensionRegistry reg = Platform.getExtensionRegistry();
		IConfigurationElement[] extensions = reg.getConfigurationElementsFor(PROVER_EXTENSIONPOINT_ID);
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement extension = extensions[i];
			String name = extension.getAttribute(PROVER_EXTENSION_NAME_ATTR).intern();
			try {
				IProverPlugin plugin = 
					(IProverPlugin) extension.createExecutableExtension(PROVER_EXTENSION_ACTIVATOR_ATTR);
				if (registry.get(name) != null) {
					// This support is for future if we allow plugins to be added/removed without restarting.
					// (provers are not removed by this method, yet).
					System.out.println("Update connected provers: " + name + " already known, replacing content");
					registry.remove(name);
				}
				Prover p = new Prover(name, plugin);
				String fileExtensionsList = extension.getAttribute(PROVER_EXTENSION_FILEEXTENSIONS_ATTR);
				StringTokenizer st = new StringTokenizer(fileExtensionsList,",");
				while (st.hasMoreElements()) {
					String extn = st.nextToken();
					p.fileExtensions.add(extn);
					System.out.println("Registered file extension: " + "." + extn);
				}
				try {
					addKnownProver(p);
				} catch (ProverRegistryException e) {
					// TODO: log error message
					System.out.println("Exception when registering prover: "+p.name);
					e.printStackTrace();
				}
			} catch (CoreException e) {
				System.out.println("Exception when activating a prover plugin: "+name);
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Return the prover registered with the given name.
     * @param proverName
     * @return the prover with the given name.
     * @throws ProverRegistryException if the given name has no registered prover
     */
    public Prover getProver(String proverName) throws ProverRegistryException {
		Prover p = registry.get(proverName);
		if (p == null) {
			throw new ProverRegistryException("Unknown prover: " + proverName);
		}
	    return p;
    }
	
    /**
     * Return a prover for the given file extension.
     * @param fileExtn
     * @return the first associated prover found
     * @throws ProverRegistryException if a prover for the given extension can't be found.
     */
    public Prover getProverForFileExtension(String fileExtn) throws ProverRegistryException {
    	for (Prover p : registry.values()) {
    		if (p.fileExtensions.contains(fileExtn)) {
    			return p;
    		}
    	}
    	throw new ProverRegistryException("Cannot find prover for file extension: " + fileExtn);
    }
    
    
    public Collection<Prover> getConnectedProvers() {
    	return registry.values();
    }
//    public static class ProverRegistryBundleListener extends BundleListener {
//        public void bundleChanged(BundleEvent event) {
//            if (event.getBundle() == getBundle()) {
//                if (event.getType() == BundleEvent.STARTED) {
//                    // We're getting notified that the bundle has been started.
//                    // Make sure it's still active.  It may have been shut down between
//                    // the time this event was queued and now.
//                    if (getBundle().getState() == Bundle.ACTIVE) {
//                        refreshPluginActions();
//                    }
//                    fc.removeBundleListener(this);
//                }
//            }
//        }
//    }

	/**
     * @return the default prover (first one registered)
     */
    public Prover getDefaultProver() throws ProverRegistryException {
	    // TODO Auto-generated method stub
	    if (registry.isEmpty()) {
	    	throw new ProverRegistryException("No provers have been registered -- please check that you have installed a prover plugin (ed.inf.proofgeneral.prover.*)");
	    }
    	return registry.values().iterator().next();
    }
}
