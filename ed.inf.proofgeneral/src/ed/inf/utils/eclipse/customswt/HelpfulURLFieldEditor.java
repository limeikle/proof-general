/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jan 3, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */

package ed.inf.utils.eclipse.customswt;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.swt.widgets.Composite;


/**
 *
 */
public class HelpfulURLFieldEditor extends HelpfulStringFieldEditor  {

	/**
     * @param name
     * @param labelText
     * @param parent
     */
    public HelpfulURLFieldEditor(String name, String labelText, Composite parent) {
        super(name, labelText, parent);
        setEmptyStringAllowed(false);
        setErrorMessage("Value must be a valid URL");
    }
    
    /**
     * Checks whether the text input field contains a valid value or not.
     *
     * @return <code>true</code> if the field value is valid,
     *   and <code>false</code> if invalid
     */
    protected boolean doCheckState() {
    	try { 
    		 new URL( getTextControl().getText() ); 
    		 return true;
    	} catch (MalformedURLException e) {
    		return false;
    	}
    }
 

    
}
