/*
 *  $RCSfile: PrefsPageBackend.java,v $
 *
 *  Created on 06 Feb 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.preferences;

/**
 * @author Daniel Winterstein
 *
 * Proof General preference pages that read what options they set from xml files.
 * The xml files should use the tags &lt;preferences&gt;, &lt;pref name="" class="" default=""&gt;, &lt;help&gt;
 *
 * To create a pref page that exposes the underlying prover's settings
 * use preferences that are named "$SystemName Config $PreferenceName"
 */

// da: Should look at Ahsan's code for improvements.  He got the PGIP
// preference mechanism working, I think.

//  da: I've made some cleanups here but more is required:
//  1. Remove the dynamically generated help files which link to
//  non-existent pages on wiki.  Sort of a nice idea but a bit overkill
//  and messy to implement.  Better to include proper help documentation
//  where necessary (extracting from preference .xml files possible for
//  that but again an overkill for only a few pages).
//  2. Make the mechanism work for prover plugins which connect to us
//  but have their own .xml files inside other plugins.
//  3. Implement a specialised subclass for prover-generated preferences.
//  The stuff in IsabelleConfigPrefs.xml doesn't seem to correspond
//  properly with the <hasprefs> advertisement.  What we may need to do
//  here is to have a way of regenerating the preference page on-the-fly.
//  More easy would be write out preference .xml files on prover startup,
//  except that we don't really want to write into the installation area.
//  5. Other cleanups desirable: fix to use Eclipse bundle
//  stuff to find .xml files instead of winterstein methods, so we don't
//  have to unpack plugin for runtime and don't try to write into the
//  installation area.  Searching various paths for .xml
//  files doesn't seem useful -- except for pt 2 (in other plugins)
//  6. Having a different preference format here from the PGIP preference
//  format seems unnecessary: they should be unified.
//  7. After all this effort messing with XML files, preference names
//  get defined again (but with slightly different names) in Constants.java,
//  which seems pretty messy.


import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FontFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.ProofGeneralProverRegistry.ProverRegistryException;
import ed.inf.proofgeneral.actions.SetPrefAction;
import ed.inf.proofgeneral.sessionmanager.CommandProcessor;
import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.utils.eclipse.ErrorUI;
import ed.inf.utils.eclipse.customswt.AssocListFieldEditor;
import ed.inf.utils.eclipse.customswt.HelpfulBooleanFieldEditor;
import ed.inf.utils.eclipse.customswt.HelpfulColorFieldEditor;
import ed.inf.utils.eclipse.customswt.HelpfulControl;
import ed.inf.utils.eclipse.customswt.HelpfulFileFieldEditor;
import ed.inf.utils.eclipse.customswt.HelpfulIntegerFieldEditor;
import ed.inf.utils.eclipse.customswt.HelpfulScaleFieldEditor;
import ed.inf.utils.eclipse.customswt.HelpfulStringFieldEditor;
import ed.inf.utils.eclipse.customswt.HelpfulTextFieldEditor;
import ed.inf.utils.eclipse.customswt.HelpfulURLFieldEditor;
import ed.inf.utils.eclipse.customswt.PrefPageLink;
import ed.inf.utils.eclipse.customswt.TextFieldEditor;
import ed.inf.utils.file.FileUtils;

/**
 * Use this class for preference pages that look in xml files for their fields
 * The xml files should use the tags <preferences prefix="" >, <pref name=""
 * id="" class="" default="" tooltip="" >, <description> Currently supports
 * classes: String, Integer, Boolean, Font, File, Link
 *
 * The procedure for creating a new page is:
 *
 * 1) Add to plugin.xml a preference page extension for org.eclipse.ui.preferencePages,
 * specifying a (possibly trivial) subclass of this class whose name matches the file name of the
 * xml file:  ed.inf.proofgeneral.<ClassName> (.xml will be added)
 *
 * 2) Create an XML file specifying the preferences in config/<FileName>.xml
 *
 *
 */

// da: plan:
// -- see how many times fieldeditor init is called (want to see
// if reading file every time or not, also whether we'll have a chance
// to adjust dynamically for prover-specified preferences)
// -- revert to using class name to determine xml file name, OR:
// fetch preference page class during init and find a static field
// which specifies the page name
// -- study extensions stuff


public abstract class PrefsPageBackend extends FieldEditorPreferencePage
implements IWorkbenchPreferencePage {


	private Element prefs;    // XML content.
	                          // TODO: this can probably be disposed after
	                          // createFieldEditors has been called.

	/** the name of the xml file (without path, should be in config/ directory) */
	private final String fileName; 
	{ 
		fileName = fileNameOfClassName(this.getClass().getName());
	}
	
	/**
	 * Here we load in defaults from the xml files (this is virtually repeated in createFieldEditors).
	 * This is done for the given namespace so we can use it in other plugins too.
	 *
	 * @param store
	 */
// FUTURE: maybe replace ournamespace with automatic lookup for IContributor once
// Eclipse APIs have settled there.
	public static void initializePGPrefs(Preferences store, String ournamespace, String proverName) {
		List<String> prefPageFiles = new ArrayList<String>();

		IExtensionPoint point = Platform.getExtensionRegistry().
			getExtensionPoint("org.eclipse.ui.preferencePages");
		if (point == null) {
			return;
		}
		IExtension pgPrefPages = null;
		for (IExtension extn : point.getExtensions()) {
			if (extn.getNamespaceIdentifier().equals(ournamespace)) {
				pgPrefPages = extn;
				break;
			}
		}
		if (pgPrefPages==null) {
			return;
		}
		for (IConfigurationElement ce : pgPrefPages.getConfigurationElements()) {
		    String pclass = ce.getAttribute("class").intern();
			String filename = fileNameOfClassName(pclass);
		    prefPageFiles.add(filename);
			prefPageClasses.put(pclass,filename);
		}
		makeHelpContextsFileAndInit(proverName,store, prefPageFiles);
	}

	/**
	 * Make context file for help and initialise preferences.
	 * @param proverName
	 * @param store
	 * @param prefPageFiles
	 */
	private static void makeHelpContextsFileAndInit(String proverName,Preferences store, List<String> prefPageFiles) {
		SAXReader reader = new SAXReader();
		for(String fName : prefPageFiles) {
		    initDefaultPrefsFromFile(proverName,store,fName,reader);
		}
	}

	/**
	 * List of all the pref pages, used by setPref to flush changes out to provers.
	 */
	static Map<String,String> prefPageClasses = new HashMap<String,String>();

	/**
	 * Initialise the preferences stored in a single file. 
	 * @param store
	 * @param fileName
	 * @param reader
	 */
	static void initDefaultPrefsFromFile(String proverName, Preferences store, String fileName, SAXReader reader) {
		try {
			String path = "config/" + fileName;
		    if (ProofGeneralPlugin.debug(PrefsPageBackend.class)) {
		    	System.out.println("Initialising preferences from \""+path+"\" " + 
		    			(proverName == null ? "(in generic plugin)" : "(in plugin for prover \"" + proverName + "\")" ));
		    }
		    File file = FileUtils.findProverFile(proverName,path);
		    if (file == null) {
		    	System.err.println("Warning (likely to cause bad problems): could not find preferences file: "+path);
		    	return;
		    }
			Document document = reader.read(file);
			Element proot = (Element) document.selectSingleNode("//preferences");
			String prefix = "";
			if (proot!=null) {
				prefix = proot.attributeValue("prefix");
				if (prefix==null) prefix="";
			}
			String ourOS = Platform.getOS();
			//System.err.println("We're on OS: " + ourOS);
			List prefNodes = document.selectNodes( "//pref" );
			List<String> seenPrefs = new ArrayList<String>();
			for (Object node : prefNodes) {
				if (node instanceof Element) {
					Element element = (Element) node;
					String name = getName(element,prefix);
					String defValue = element.attributeValue("default");
					String osValue = element.attributeValue("os");
					if (osValue != null && !ourOS.contains(osValue)) {
						// NB: this is a crude way of having OS-specific prefs:
						// give the os attribute, and for specific instances
						// first, then the default for others.  Preference
						// must be repeated, and defaults overridden by users
						// won't get OS-specific variants.  See
						// Default Symbol Table in ProofGeneralPrefs.xml
						continue;
					}
					if (seenPrefs.contains(name)) {
						continue; // set already
					}
					if (name != null && defValue!=null) {
						// NB: we always blast existing defaults in the preference
						// store, replacing them with settings from our XML files.
						store.setDefault(name,defValue);
						seenPrefs.add(name);
					}
				}
			}
		// prevent an error in one file killing all the pref files
		} catch (Exception e) { e.printStackTrace(); }
	}

	/**
	 * Add child to parent, removing any already existing matching children
	 * @param parent
	 * @param child
	 * @return number of children removed
	 */
	public static int uniqueAdd(Element parent, Element child) {
	    String query = "//"+child.getName()+"[@id='"+child.attributeValue("id")+"']";
	    List existing = parent.selectNodes(query);
	    for (int i=0; i<existing.size(); i++) {
	    	parent.remove((Node) existing.get(i));
	    }
	    parent.add(child);
	    return existing.size();
	}


	public static String fileNameOfClassName(String classname) {
		 classname = classname.substring(classname.lastIndexOf('.')+1);
		 classname += ".xml";
		 return classname;
	}

	/**
	 * Make a generic preference page, which uses the generic preference store and
	 * is initialised from the main plugin. 
	 */
	public PrefsPageBackend() {
		super(GRID);
		readPrefsFile(null);
	}
	
	/**
	 * Make a prover-specific preference page, which uses the preference store
	 * from the prover plugin with the given (prover) name
	 */
	public PrefsPageBackend(String proverName) {
		super(GRID);
		readPrefsFile(proverName);
		setDescription("Settings for running the " + proverName + " theorem prover");
	}
	
	private String proverName;
	
	/**
     * @return the proverName, or null if this is a generic preference page.
     */
    public String getProverName() {
    	return proverName;
    }

	void readPrefsFile(String proverName) {
		Bundle bundle;
		this.proverName = proverName;
		AbstractUIPlugin plugin;
		try {
			if (proverName == null) {
				plugin = ProofGeneralPlugin.getDefault();
			} else {
				plugin = ProofGeneralPlugin.getRegistry().getProver(proverName).getProverPlugin(); 
			}
			setPreferenceStore(plugin.getPreferenceStore());
			bundle = plugin.getBundle();
			URL config = FileUtils.findURL(bundle,"config/"+fileName);
			if (config != null) {
				SAXReader reader = new SAXReader();
				Document prefdoc = reader.read(config);
				prefs = (Element) prefdoc.selectSingleNode("//preferences");
				if (prefs.element("titledesc")!=null) {
					setDescription(prefs.element("titledesc").getText());
				}
			}
		} catch (DocumentException e) {
			// TODO: logging
			e.printStackTrace();
		} catch (ProverRegistryException e) {
			// TODO: logging
			e.printStackTrace();
		}
	}
	

	

	@Override
    protected void createFieldEditors() {
		FieldEditor field;
		String prefix = prefs.attributeValue("prefix");
		if (prefix==null) {
			prefix="";
		}
		List prefNodes = prefs.selectNodes( "//pref" );
		List<String> seenPrefs = new ArrayList<String>();
		String ourOS = Platform.getOS();
		Composite parent = getFieldEditorParent();
		for (Object prefNode : prefNodes) {
			Element element = (Element) prefNode;
			String visible = element.attributeValue("visible");
			if (visible != null && visible.equalsIgnoreCase("false")) {
				continue; // this preference isn't shown to user
			}
			String name = getName(element,prefix);
			if (seenPrefs.contains(name)) {
				continue; // this preference already shown
			}
			seenPrefs.add(name);
			String os = element.attributeValue("os");
			if (os != null && !ourOS.contains(os)) {
				continue; // this preference is for another OS
			}
			String eClass = element.attributeValue("class");
			if (eClass==null) {
				eClass = "string";
			} else {
				eClass = eClass.toLowerCase();
			}
			String label = element.attributeValue("label");
			if (label == null) {
				label=name.substring(prefix.length()); // don't include the prefixes
			}

			// different classes of pref
			if (eClass.equals("string")) {
				field = new HelpfulStringFieldEditor(name, label,
						StringFieldEditor.UNLIMITED, //Math.max(25,defValue.length()),
						getFieldEditorParent());
			} else if (eClass.equals("text")) {
				field = new HelpfulTextFieldEditor(name, label, TextFieldEditor.UNLIMITED,
						getFieldEditorParent());
			} else if (eClass.equals("file")) {
				field = new HelpfulFileFieldEditor(proverName, name, label, getFieldEditorParent());
			} else if (eClass.equals("url")) {
				field = new HelpfulURLFieldEditor(name, label, getFieldEditorParent());
			} else if (eClass.equals("scale")) {
				field = new HelpfulScaleFieldEditor(name, label,
						getFieldEditorParent());
			} else if (eClass.equals("integer")) {
				field = new HelpfulIntegerFieldEditor(name, label,
						getFieldEditorParent());
			} else if (eClass.equals("boolean")) {
				field = new HelpfulBooleanFieldEditor(name, label,getFieldEditorParent());
			} else if (eClass.equals("color")) {
				field = new HelpfulColorFieldEditor(name, label, parent);
			} else if (eClass.equals("assoclist")) {
				field = new AssocListFieldEditor(name,label,parent);
			} else if (eClass.equals("font")) {
				field = new FontFieldEditor(name,label,parent);
			} else if (eClass.equals("link")) {
				field = new PrefPageLink(name,label,parent);
			} else {
				field = new HelpfulStringFieldEditor(name,
						label, 25, getFieldEditorParent());
			} //end switch
			// control is ready
			addField(field);
			// add help
			if (!(field instanceof HelpfulControl)) {
				continue; // can't add help here
			}
			String id = element.attributeValue("id");
			if (id!=null) {
				id = makeFullId(id,fileName);
				((HelpfulControl) field).setHelp("ed.inf.proofgeneral."+id);
			}
			if (element.attributeValue("tooltip") != null) {
				((HelpfulControl) field).setToolTipText(element.attributeValue("tooltip"));
			}
		} //end for
		PlatformUI.getWorkbench().getHelpSystem().
		//WorkbenchHelp.
		setHelp(getFieldEditorParent(),"ed.inf.proofgeneral."+makeFullId("",fileName));
	}

	public static String getName(Element element,String prefix) {
		String name = element.attributeValue("name");
		if (name==null)
			return null;
		if (prefix==null)
			return name;
		if (!name.startsWith(prefix)) {
			name = prefix+name;
		}
		return name;
	}

	// TODO da: clean this up, store the class name and use that instead of add/removing
	// .xml extension.
	protected static String makeFullId(String id, String fileName) {
		fileName = fileName.replaceAll("[ _.]","");
		if (fileName.endsWith("xml")) {
			fileName = fileName.substring(0,fileName.length()-3);
		}
		return fileName + id;
	}

	/**
     * Keep track of modified preferences so that we can pass them on to the prover.
       @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {
        super.propertyChange(event);
        if (event.getNewValue().equals(event.getOldValue())) {
        	return;
        }
        FieldEditor fe = (FieldEditor) event.getSource();
        String pName = fe.getPreferenceName().intern();
        if (!modifiedPrefs.contains(pName)) {
        	modifiedPrefs.add(pName);
        }
    }

	/**
	 * Programmatically set a (generic) preference, e.g. because of an interface script command.
	 * @see CommandProcessor
	 * @param name
	 * @param value
	 */
	public static void setPref(String name,String value) {
		ProofGeneralPlugin.getDefault().getPluginPreferences()
			.setValue(name,value);
		PrefsPageBackend.updateExternalPrefs();
	}

    @Override
    public boolean performOk() {
        boolean answer = super.performOk();
       try {
            IEclipsePreferences node = new InstanceScope().getNode("ed.inf.proofgeneral");
            node.flush();	    // save prefs to disk
        } catch (Exception e) { // error saving prefs to disk
            e.printStackTrace();
        }
        PrefsPageBackend.updateExternalPrefs(); // send out external prefs, if necc.
        return answer;
    }

    public static final List<String> modifiedPrefs = new ArrayList<String>();

	/**
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	/**
     * Updates external preferences.
     * Assumes that external preferences are of the form <pre>&lt;SystemName&gt; Config &lt;PreferenceName&gt;</pre>
	 * (see {@value ProverPreferenceNames#PROVER_INTERNAL_PREFERENCE_NAME_TAG}). 
     * Stops on failure, and clears the list of modified preferences.
     */
    public static void updateExternalPrefs() {
        String proverName;
        SessionManager sm;
        try {
        	for (String pName : modifiedPrefs) {
        		int prefix = pName.indexOf(ProverPreferenceNames.PROVER_INTERNAL_PREFERENCE_NAME_TAG);
                if (prefix==-1) {
                	continue; // not an external preference
                }
                proverName = pName.substring(0,prefix);
                sm = ProofGeneralPlugin.getSessionManagerForProver(proverName);
                String value = ProofGeneralPlugin.getProverPlugin(proverName).getPluginPreferences().getString(pName);
                SetPrefAction setAction = new SetPrefAction(sm,pName,value);
    			setAction.run();
        	}
        } catch (Exception ex) {
            ex.printStackTrace();
            ErrorUI.getDefault().signalError(ex);
            return;
        } finally {
            modifiedPrefs.clear();
        }
    }


}
