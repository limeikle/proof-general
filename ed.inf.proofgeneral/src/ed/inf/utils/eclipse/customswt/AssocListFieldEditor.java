/*
 *  $RCSfile: AssocListFieldEditor.java,v $
 *
 *  Created on 23 Jun 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.eclipse.customswt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import ed.inf.utils.datastruct.StringManipulation;


/**
 * A field editor for key=value lists (aka association lists)
 * @author Daniel Winterstein
 */
public class AssocListFieldEditor extends ListEditor implements
	HelpfulControl {

    public static final String LIST_SEPARATOR = ",";
    public static final String KEY_VALUE_SEPARATOR = "=";
    public AssocListFieldEditor() {
        super();
    }

    public AssocListFieldEditor(String name, String labelText, Composite parent) {
        super(name, labelText, parent);
        this.parent = parent;
    }


    @Override
    protected String createList(String[] items) {
        StringBuffer path = new StringBuffer("");
    	for (int i = 0; i < items.length; i++) {
    		path.append(items[i]);
    		path.append(LIST_SEPARATOR);
    	}
    	return path.toString();
    }

    /**
     * TODO make this less crude --DW
     * @see org.eclipse.jface.preference.ListEditor#getNewInputObject()
     */
    @Override
    protected String getNewInputObject() {
        InputDialog dialog = new InputDialog(getShell(),"New","Enter new key"+KEY_VALUE_SEPARATOR+"value pair.",
                null,null);
        dialog.open();
        String s = dialog.getValue();
        return s;
    }


    @Override
    protected String[] parseString(String stringList) {
        return splitString(stringList);
    }

    /**
     * Splits a string into an array.
     * @param stringList a string, delimited by the the string LIST_SEPARATOR\n\r.
     * @return an array formed by splitting the string.
     * @see #LIST_SEPARATOR
     */
    public static String[] splitString(String stringList) {
        StringTokenizer st = new StringTokenizer(stringList,
                LIST_SEPARATOR + "\n\r");
    	ArrayList<String> v = new ArrayList<String>();
    	while (st.hasMoreElements()) {
    		v.add(st.nextToken());
    	}
    	return v.toArray(new String[v.size()]);
    }

    /**
     * Convert the stored string into a key-value map
     * @param stringList a string delimited into key-value pairs by KEY_VALUE_SEPARATOR
     * @return the mapping
     * @see #KEY_VALUE_SEPARATOR
     */
    public static Map<String,String> makeMap(String stringList) {
        String[] assocs = splitString(stringList);
        Map<String,String> map = new HashMap<String,String>();
        for(int i=0; i<assocs.length; i++) {
            String[] kv = assocs[i].split(KEY_VALUE_SEPARATOR);
            // only enter key-value pairs once, so that if there are several, the 1st one 'wins'
            // da: format of this table seems to get broken, so protect here by checking kv.length
            if (kv.length == 2 && !map.containsKey(kv[0])) {
            	map.put(kv[0].intern(),kv[1].intern());
            }
        }
        return map;
    }
    /**
     * Create a key suitable for storing from an object.
     * Will remove line breaks and separators if they occur in the key.
     * @param x
     * @return key
     */
    public static String makeKey(Object x) {
        String s = x.toString();
        s = s.replaceAll("[\\r\\n]"," "); // get rid of line breaks
        s = s.replaceAll(StringManipulation.regexEsc(KEY_VALUE_SEPARATOR)," ");
        s = s.replaceAll(StringManipulation.regexEsc(LIST_SEPARATOR)," ");
        return s;
    }
    /**
     * Convert a key-value map into a string suitable for being stored.
     * Sort-of a public version of createList. May shuffle entry order.
     * @param map
     */
    public static String makeString(Map<String,String> map) {
        StringBuffer path = new StringBuffer("");
    	for (Map.Entry kv : map.entrySet()) {
    		path.append(kv.getKey()+KEY_VALUE_SEPARATOR+kv.getValue());
    		path.append(LIST_SEPARATOR);
    	}
    	return path.toString();
    }

    Composite parent;
    public Control[] getControls() {
        return new Control[] {getLabelControl(),getListControl(parent),getButtonBoxControl(parent)};
    }

    public void setToolTipText(String text) {
    	HelpfulController.setToolTipText(this, text);
    }

    public void setHelp(String helpContextId) {
    	HelpfulController.setHelp(this, helpContextId);
    }

}
