/*
 *  $RCSfile: XmlPreferenceStore.java,v $
 *
 *  Created on 29 Oct 2004
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.eclipse;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.tree.DefaultText;
import org.eclipse.core.runtime.Preferences;

/**
 * XmlPreferenceStore
 * @author Alex Heneveld
 */
public class XmlPreferenceStore extends Preferences implements Cloneable {

	public XmlPreferenceStore() {
		super();
	}

	public static XmlPreferenceStore fromFile(String filename) {
		XmlPreferenceStore prefStore = new XmlPreferenceStore();
		try {
			FileReader fr = new FileReader(filename);
			prefStore.setFromReader(fr);
			try { fr.close(); } catch (Exception e) {}
		} catch (Exception e) {
			System.err.println("error loading preferences: "+e);
			//e.printStackTrace();
		}
		return prefStore;
	}

	/**
	 * @param defaultPrefs
	 * @return the preferenceStore
	 */
	public static XmlPreferenceStore fromString(String defaultPrefs) {
		XmlPreferenceStore prefStore = new XmlPreferenceStore();
		//prefStore.setFromXmlString(defaultPrefs);
		prefStore.setFromReader(new StringReader(defaultPrefs));
		return prefStore;
	}

	public void setFromReader(Reader r) {
		try {
			StringBuffer sb = new StringBuffer();
			char[] buf = new char[4096];
			int i = r.read(buf);
			while (i>0) {
				sb.append(buf, 0, i);
				i = r.read(buf);
			}
			//System.out.println("PREFS FILE IS:\n"+sb.toString());
			setFromXmlString(sb.toString());
		} catch (IOException e) {
			throw new RuntimeException("cannot load preferences, file io error", e);
		}
	}

	public void setFromXmlString(String s) {
		try {
			Document d = DocumentHelper.parseText(s);
			Element root = d.getRootElement();
			setFromElement("", root);
		} catch (DocumentException e) {
			throw new RuntimeException("cannot load preferences, bad xml", e);
		}
	}

	public void setFromElement(String prefix, Element e) {
		if ("pref".equals(e.getName())) {
			//on a pref node
			Attribute name = e.attribute("name");
			//Attribute label = e.attribute("label");
			//Attribute clazz = e.attribute("class");
			Attribute defo = e.attribute("default");
			Attribute value = e.attribute("value");
			//ignore class type, tooltip, description children
			if (name!=null) {
				if (defo!=null) {
					setDefault(prefix+name.getValue(), defo.getValue());
					setDefault(name.getValue(), defo.getValue());
				}
				if (value!=null) {
					setValue(prefix+name.getValue(), value.getValue());
					setValue(name.getValue(), value.getValue());
				}
			}
		} else {
			Attribute pa = e.attribute("prefix");
			String prefix2 = prefix;
			if (pa!=null) {
				prefix2 = prefix + pa.getValue();
			}
			Iterator ci = e.content().iterator();
			while (ci.hasNext()) {
				Object o = ci.next();
				if (! (o instanceof DefaultText)) {
					setFromElement(prefix2, (Element)o);
				}
			}
		}
	}

	/**
	 * Creates a copy of this preference store, by cloning the values and defaults HashMaps.
	 * Note that this requires an unchecked conversion, but we can guarantee that a clone
	 * should have the same type as its originator.
	 * <pre>@SuppressWarnings("unchecked")</pre> has been applied.
	 */@SuppressWarnings("unchecked")
	public Object clone() {
		XmlPreferenceStore clone = null;
		try {
			clone = (XmlPreferenceStore)super.clone();
		} catch (CloneNotSupportedException e) {
			clone = new XmlPreferenceStore();
		}
		for (String prop : this.propertyNames()) {
			clone.setValue(prop, getString(prop));
		}
		for (String def : this.defaultPropertyNames()) {
			clone.setValue(def, getString(def));
		}
		return clone;
	}

}
