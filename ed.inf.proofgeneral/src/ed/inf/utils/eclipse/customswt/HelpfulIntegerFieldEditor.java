/*
 *  $RCSfile: HelpfulIntegerFieldEditor.java,v $
 *
 *  Created on 27 May 2004
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.eclipse.customswt;

import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 @author Daniel Winterstein
 */
public class HelpfulIntegerFieldEditor extends IntegerFieldEditor implements
        HelpfulControl {

    /**
     * 
     */
    public HelpfulIntegerFieldEditor() {
        super();
    }

    /**
     * @param name
     * @param labelText
     * @param parent
     */
    public HelpfulIntegerFieldEditor(String name, String labelText,
            Composite parent) {
        super(name, labelText, parent, 10);
    }

    /**
     * @param name
     * @param labelText
     * @param parent
     * @param textLimit
     */
    public HelpfulIntegerFieldEditor(String name, String labelText,
            Composite parent, int textLimit) {
        super(name, labelText, parent, textLimit);
    }

    public Control[] getControls() {
        return new Control[] {getLabelControl(),getTextControl()};
    }

    public void setToolTipText(String text) {
    	HelpfulController.setToolTipText(this, text);
    }

    public void setHelp(String helpContextId) {
    	HelpfulController.setHelp(this, helpContextId);
    }
    

}
