/*
 *  $RCSfile: HelpfulFileFieldEditor.java,v $
 *
 *  Created on 27 May 2004
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.eclipse.customswt;

import java.io.File;

import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import ed.inf.utils.file.FileUtils;

/**
 @author Daniel Winterstein
 */
public class HelpfulFileFieldEditor extends FileFieldEditor implements
        HelpfulControl {

	/** The prover name used to check files within the correct plugin. */
	private final String proverName;

    /**
     * See {@link FileFieldEditor#FileFieldEditor(String, String, Composite)}.
     */
    public HelpfulFileFieldEditor(String proverName, String name, String labelText,
            Composite parent) {
        super(name, labelText, parent);
        this.proverName = proverName;
    }


    public Control[] getControls() {
        return new Control[] {getLabelControl(), getTextControl()};
    }

    public void setToolTipText(String text) {
    	HelpfulController.setToolTipText(this, text);
    }

    public void setHelp(String helpContextId) {
    	HelpfulController.setHelp(this, helpContextId);
    }
    
    /** Checks whether the text input field specifies an existing file.
     * Overridden to check also for prover plugin-relative location for config files.
     */
    @Override
    protected boolean checkState() {

    	String msg = null;

    	String path = getTextControl().getText();
    	if (path != null) {
    		path = path.trim();
    	} else {
    		path = "";//$NON-NLS-1$
    	}
    	if (path.length() == 0) {
    		msg = getErrorMessage();
    	} else {
    		File file = FileUtils.findFileExt(proverName,path);
    		if (file == null) {
    			msg = getErrorMessage();
    		}
    	}

    	if (msg != null) { // error
    		showErrorMessage(msg);
    		return false;
    	}

    	// OK!
    	clearErrorMessage();
    	return true;
    }
    


}
