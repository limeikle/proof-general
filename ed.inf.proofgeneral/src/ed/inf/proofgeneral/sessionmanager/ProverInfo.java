/*
 *  $RCSfile: ProverInfo.java,v $
 *
 *  Created on 18 May 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.sessionmanager;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.eclipse.core.runtime.Preferences;

import ed.inf.proofgeneral.ProofGeneralProverRegistry.Prover;
import ed.inf.proofgeneral.preferences.PrefsPageBackend;
import ed.inf.proofgeneral.preferences.ProverPreferenceNames;
import ed.inf.utils.file.FileUtils;

/**
 *  A front end for getting at settings regarding a theorem prover
 *  Uses PreferenceStore to actually hold everything
 *  Prover settings should be named: "PROVER_NAME PROPERTY_NAME" for proverInfo to find them
 *  @author Daniel Winterstein
 */

// TODO da: refactor this to combine with Prover class 
public class ProverInfo {  //implements IPropertyChangeListener {

    public String name;

	Preferences prefStore;
	/**
	 * General purpose (PGIP and Eclipse) syntax.
	 */
	public PGIPSyntax syntax;
	/**
	 * Syntax specific to a given prover.
	 */
	public ProverSyntax proverSyntax;
	//public Map info = new HashMap();

	/**
	 * The exposed preferences of the prover, changeable via Eclipse Prefs
	 */
	List<String> configPreferences = null;
	/**
	 * The names of the exposed preferences of the prover, changeable via Eclipse Prefs
	 * ASSUMPTION: Assumes all these prefs are stored in a file ${proverName}ConfigPrefs.xml and use "${proverName} Config " as their prefix
	 */
	// FIXME da: this is supposed to be generated by the PGIP message <askprefs/>,
	// with response <hasprefs/>.  The XML file here is not necessary/wanted.
	// We should have separate preferences for different provers
	// in prover-specific preference stores: the configuration
	// for each prover (i.e. hasPrefs response) can be stored in a canonical
	// preference value (which is *not* intended to be user-adjustable).
	// We store the information in memory to generate/restore the preference page from.
	// Tricky part is that this is partly dynamic and the extension point method
	// for preference pages maybe doesn't allow them to change once created.
	// But a first approximation would be to allow the user to restart
	// Eclipse to see the preference pages.
	//
	public List<String> getConfigPrefs() throws Exception {
		if (configPreferences!=null) {
			return configPreferences;
		}
		configPreferences = new ArrayList<String>();
		SAXReader reader = new SAXReader();
		String path = "config/"+name+"ConfigPrefs.xml";
		File file = FileUtils.findProverFile(name,path);
		if (file != null) {
			Document document = reader.read(file);
			List list = document.selectNodes( "//pref" );
			String prefix = name+ProverPreferenceNames.PROVER_INTERNAL_PREFERENCE_NAME_TAG;
			for (Iterator iter = list.iterator(); iter.hasNext(); ) {
				Element element = (Element) iter.next();
				String pName = PrefsPageBackend.getName(element,prefix);
				configPreferences.add(pName);
			}
		}
		return configPreferences;
	}

	/**
	 * Constructs a new ProverInfo object using the given prover's name and
	 * preference store.
	 */
	public ProverInfo(Prover prover) {
		this(prover.getName(), prover.getProverPreferences());
	}
	
	/**
	 * Constructs a new ProverInfo object, specifying name and
	 * (a prover's) preference store.
	 */
	public ProverInfo(String name, Preferences prefStore) {
		this.name = name.intern();
		this.prefStore = prefStore;
		syntax = new PGIPSyntax(this);
		proverSyntax = new ProverSyntax(this);
	}
	/*	prefStore.addPropertyChangeListener(this);
		List keys = new ArrayList();
	  try {
		File file = Methods.findFile(name+"Prefs.xml");
		SAXReader reader = new SAXReader();
		Document document = reader.read(file);
		List list = document.selectNodes( "//pref" );
		String keyName;
		for (Iterator iter = list.iterator(); iter.hasNext(); ) {
			Element element = (Element) iter.next();
			keyName = element.attributeValue("name");
			if (keyName!=null && !keyName.equals("")) keys.add(
			        keyName.substring(name.length()+1) );
		}
	  } catch (Exception e) { e.printStackTrace(); }
		for(int i=0; i<keys.size(); i++) {
		    setProperty((String) keys.get(i));
		}
	}

	public void propertyChange(PropertyChangeEvent event) {
	    if (event.getProperty().startsWith(name)) {
		// TODO react to changes in the pref page
	    }
	}

	public void setProperty(String key) {
	    if (prefStore.contains(name+" "+key)) {
	        info.put(key, prefStore.getString(name+" "+key));
	    }
	}
	*/
	public boolean getBoolean(String key) {
	    return prefStore.getBoolean(name+" "+key);
	}
	public String getString(String key) {
	    String answer = prefStore.getString(name+" "+key);
	    return answer;
	}
	public int getInt(String key) {
	    return prefStore.getInt(name+" "+key);
	}


	/** returns a user-specified launch command, if a special one is set,
	 *  otherwise takes it from proverInfo preference settings */
	public String getLaunchCommand() {
		if (specialLaunchCommand!=null) {
			return specialLaunchCommand;
		}
		return getString(ProverPreferenceNames.PREF_PROVER_START_COMMAND);
	}
	
	protected String specialLaunchCommand = null;

	/** sets a temporary user-specified launch command, without updating the pref store */
	public void setSpecialLaunchCommand(String s) {
		specialLaunchCommand = s;
	}
}
