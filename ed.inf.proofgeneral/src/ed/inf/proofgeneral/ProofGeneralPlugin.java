/*
 *  $RCSfile: ProofGeneralPlugin.java,v $
 *
 *  Created on 03 Feb 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral;

/**
 * Activator for Eclipse Proof General Plugin
 * Also stores some plugin-wide objects
 * @author Daniel Winterstein
 * @author Alex Heneveld
 * @author David Aspinall
 * 
 */

//* TODO: pref of parse files on opening
//* FIXME errors with mixed parser? is it even doing sensible things?
//* FIXME Isabelle emits unescaped PGML - this screws up xml parsing eg.
//<pgip class = "pg" origin = "Isabelle/Isar" id = "/s9902178/1101038050.362" refseq = "7" refid = "PG-Eclipse" seq = "13">
//<proofstate>
//<pgml><statedisplay>
//proof (prove): step 0
//fixed variables: Q
//goal (lemma (Wally), 1 subgoal):
//1. <atom kind = "free">Q</atom> \<longrightarrow> <atom kind = "free">Q</atom>
//</statedisplay></pgml>
//</proofstate></pgip>
//Should be \&lt;longrightarrow&gt;
//* FIXME Retract to activate does not seem to do anything!!
//* FIXME the proof state button (a) should update, and (b) should initiate a forced sync when pushed
//* FIXME define a command scope/context to prevent clashes (e.g. F3)
//* TODO make findFile check that there is only 1 match, output an error otherwise.
//* TODO Allow regular expressions in the symbol table editor. And possibly support recursive use of symbol expansion/compression.
//* TODO Support double-character unicode symbols that overlap - requires sorting the symbol table by size
//* TODO check that bg parses display errors properly, otherwise edit error
//* TODO undoing a locked-but-not-processed command should issue an interrupt, then unlock
//* TODO display errors for bugs in syntax
//* TODO sort out contentAssistant auto-complete feature
//* TODO make parsing happen in the background
//* FIXME There are multi-line commands that can be parsed as one line. The next-command parser should not be happy until it finds two commands or end-of-file.
//* TODO There seem to be a bug in PG(Prover?)Action:
//*   cannot run ed.inf.proofgeneral.editor.actions.GotoAction; shouldn't have been enabled
//*  This occurred when running without access to Isabelle (i.e. the prover was labelled dead)
//*  AH: this is because I can't figure out how to get a handle on any of those action buttons
//*  until the user clicks them.  After the first click we store a reference to it and so can
//*  setEnabled state correctly, but before the first click there seems no apparent way to set
//*  state programmatically.

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import ed.inf.proofgeneral.ProofGeneralProverRegistry.ProverRegistryException;
import ed.inf.proofgeneral.preferences.PreferenceNames;
import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.proofgeneral.sessionmanager.SessionManagerFactory;


public class ProofGeneralPlugin extends AbstractUIPlugin {

	/** Identifier */
	public static final String PLUGIN_ID = "ed.inf.proofgeneral";

	/** The shared instance. */
	private static ProofGeneralPlugin plugin;


	/**
	 * Returns the plugin's resource bundle,
	 */
	public static ResourceBundle getResourceBundle() throws MissingResourceException {
		return ResourceBundle.getBundle("ed.inf.proofgeneral.PGResourceBundle");
	}

	/**
	 * Returns the shared instance.
	 */
	public static ProofGeneralPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the workspace instance.
	 */
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}
	
	
	/////////////////////////////////////////////////////////////////////////////////////
	//
	// Lifecycle 
	//	
	

	/**
	 * The constructor, called when the plugin is activated.
	 */
	public ProofGeneralPlugin() {
		// plugin = this;  // ST: OK, platform treats us as singleton.


		// Set the ID string from the bundle name.
	    // Or maybe this could simply be the constant "ed.inf.proofgeneral".
		// PLUGIN_ID = Platform.getBundle("ed.inf.proofgeneral").getSymbolicName();
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
    public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;        // ST: OK by platform contract
		// ASSUMPTION: the extensions are all registered by now, so this update adds all provers we know.
		registry.updateConnectedProvers();  
	}
	
	/**
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
    public void stop(BundleContext context) throws Exception {
        // TODO this method is called too late for this flag to be useful!
		shuttingDown = true;  // ST: OK by platform contract
		for(SessionManager sm : sessionManagerFactory.getSessionManagers()) {
			sm.stopSessionBg(null, 500);
		}
		dispose();
		plugin = null;        // ST: OK by platform contract
		super.stop(context);
	}
	/**
	 * A flag, indicating whether or not the plugin is being closed down.
	 * (Useless for editors, as they shut first!)
	 */
	private static volatile boolean shuttingDown = false;

	/** tells whether eclipse is shutting down; cannot tell definitively, may return false negatives;
	 *  uses getActiveEditor and a cast from IWorkbench to Workbench
	 * @return true if we can tell that eclipse is shutting down
	 */
	public static boolean isShuttingDown() {
		if (shuttingDown) {
			return true;
		}
		return PlatformUI.getWorkbench().isClosing();
	}


	/**
	 * Dispose of images and other resources (eg. temp files).
	 */
	void dispose() {
		// clean up images
		for (Iterator i = images.values().iterator(); i.hasNext();) {
			Image image = (Image) i.next();
			image.dispose();
		}
	}

	/**
	 * Returns the string from the plugin's resource bundle,
	 * or null if not found or has another type.
	 */
	public static String getResourceString(String key) {
		ResourceBundle bundle =	ProofGeneralPlugin.getResourceBundle();
		try {
			return bundle.getString(key);
		} catch (MissingResourceException e) {
			return null;
		} catch (ClassCastException e1) {
			return null;
		}
	}

	
	/////////////////////////////////////////////////////////////////////////////////////
	//
	// Preferences 
	//	

	/** A static preference store for when we're running without platform. */
	static Preferences staticPreferenceStore = null;

	/**
	 * Caller can override the preference store, e.g. for compatibility these methods can be called in
	 *  a non-eclipse context
	 * @param ps the user-specified preference store
	 */
	public static void setStaticPreferenceStore(Preferences ps) {
		staticPreferenceStore = ps;
	}

	/**
	 * @return the (generic) preference store 
	 */
	public static Preferences getStaticPreferenceStore() {
		if (staticPreferenceStore!=null) {
			return staticPreferenceStore;
		}
		// FIXME: we can get this exception during shutdown (often)
		if (plugin==null) {
			System.out.println("Plugin is null"+(ProofGeneralPlugin.isShuttingDown()?" because of shutdown.":"."));
			throw new RuntimeException("cannot get preferences; none set in ProofGeneralPlugin.setStaticPreferenceStore() and Eclipse is not available");
		}
		return plugin.getPluginPreferences();
	}

	public static boolean getBooleanPref(String name) {
		return getStaticPreferenceStore().getBoolean(name);
	}
	public static int getIntegerPref(String name) {
		return getStaticPreferenceStore().getInt(name);
	}
	public static String getStringPref(String name) {
		return getStaticPreferenceStore().getString(name);
	}


	/////////////////////////////////////////////////////////////////////////////////////
	//
	// Images
	//

	
	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	/** Map image names to images */
	static Map<String,Image> images = new HashMap<String,Image>();

	/**
	 * Return the image object for this file.
	 * Using this method means PG handles the storage and disposal of images.
	 * @param fileName plug-in relative path
	 * @return the image of the file found.
	 */
	public static Image getImage(String fileName) {
		// da: use full paths in calls to this method.
		//if (!fileName.startsWith("icons/")) fileName = "icons/"+fileName;
		Image i = images.get(fileName);
		if (i!=null) {
			return i;
		}
		ImageDescriptor id = getImageDescriptor(fileName);
		i = id.createImage();
		images.put(fileName,i);
		return i;
	}


	/////////////////////////////////////////////////////////////////////////////////////
	//
	// Registry of provers and session managers
	//
	
	/** Registry of connected provers */
	private static final ProofGeneralProverRegistry registry = new ProofGeneralProverRegistry();
	
	/** Factory for making session managers */
	private static final SessionManagerFactory sessionManagerFactory = new SessionManagerFactory(registry);
	
	/**
     * @return the registry
     */
    public static ProofGeneralProverRegistry getRegistry() {
    	return registry;
    }
    
	/**
	 * Return some session manager.  TODO: make this return the last used session
	 * manager, updated somehow by user/automatically (e.g. last viewed editor)
	 * @return a default session manager, or null if no session managers have
	 * been initialised yet.
	 */
	public static SessionManager getSomeSessionManager() {
		if (sessionManagerFactory.numberOfSessionManagers() > 0) {
			SessionManager[] sms = sessionManagerFactory.getSessionManagers();
			return sms[0];
		}
		return null;
	}
	
	public static SessionManager[] getSessionManagers() {
		return sessionManagerFactory.getSessionManagers();
	}
	
	/**
	 * Return a session manager for the given prover name.
	 * See {@link SessionManagerFactory#getSessionManagerForProver(String)}.
	 * @param proverName
	 * @throws ProverRegistryException if no session manager associated with the name
	 */
	public static SessionManager getSessionManagerForProver(String proverName) throws ProverRegistryException {
		return sessionManagerFactory.getSessionManagerForProver(proverName);
	}

	/**
	 * Return a session manager for the given file name.
	 * See {@link SessionManagerFactory#getSessionManagerForFile(String)}.
     * @param fileName
     * @return session manger, always non-null 
     * @throws CoreException if a session manager cannot be found
     */
    public static SessionManager getSessionManagerForFile(String fileName) throws CoreException {
	    try{
	    	return sessionManagerFactory.getSessionManagerForFile(fileName);
	    } catch (ProverRegistryException e) {
	    	throw new CoreException(
					new Status( IStatus.ERROR,
							ProofGeneralPlugin.getDefault().getBundle().getSymbolicName(),
							IStatus.OK,
							"Error opening document for scripting: no session manager for prover for file " + fileName,
							null));
	    }		
    }    

    /**
     * See {@link SessionManagerFactory#hasSessionManagerForFile(String)}.
     */
    public static boolean hasSessionManagerForFile(String fileName) {
    	return sessionManagerFactory.hasSessionManagerForFile(fileName);
    }
    
	/**
	 * @return a class implementing {@link IProverPlugin}.
	 */
	public static AbstractUIPlugin getProverPlugin(String proverName) throws ProverRegistryException {
		return registry.getProver(proverName).getProverPlugin();
	}
	

/////////////////////////////////////////////////////////////////////////////////////////
//
// Utility
//

	/**
     * @return true if we're running on the platform rather than in standalone mode.  
     */
    public static boolean isEclipseMode() {
	    return plugin != null;
    }

	/**
	 * Used to control generation of useful-but-ugly debug info,
	 * inefficient reloading of files, etc.
	 * @param object - Callers should pass 'this', or .class if static.
     * In future, the debug() method may specify some classes to debug and some not to
     * (allowing more fine-grained control).
	 */
	public static boolean debug(Object object) {
		try {
			return Constants.DEBUG_DEBUG || getBooleanPref(PreferenceNames.PREF_DEBUG);
		} catch (Exception e) {
			//complains sometimes if prefs are cleared on shutdown
			return Constants.DEBUG_DEBUG;   //will only come here if false but easier to read
		}
	}

	/**
	 * Gets the URL of the ProofGeneral Wiki
	 * TODO make this less convoluted.
	 * @return the help URL, or null if the source is faulty.
	 */
	public URL getHelpURL() {
		try {
			return new URL(ProofGeneralPlugin.getStaticPreferenceStore().getString(PreferenceNames.PREF_HELP_URL));
		} catch (Exception e) {
			if (ProofGeneralPlugin.debug(this)) {
				System.err.println("Error setting up Help URL: "+e);
			}
			return null;
		}
	}
	
	/**
	 * Gets the URL of the ProofGeneral Wiki
	 * TODO make this less convoluted.
	 * @return the help URL, or null if the source is faulty.
	 */
	public URL getBugReportURL() {
		try {
			return new URL(ProofGeneralPlugin.getStaticPreferenceStore().getString(PreferenceNames.PREF_BUGREPORT_URL));
		} catch (Exception e) {
			if (ProofGeneralPlugin.debug(this)) {
				System.err.println("Error setting up Help URL: "+e);
			}
			return null;
		}
	}
}
