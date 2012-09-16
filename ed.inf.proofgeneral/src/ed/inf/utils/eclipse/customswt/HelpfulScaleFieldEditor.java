/*
 *  $RCSfile: HelpfulScaleFieldEditor.java,v $
 *
 *  Created on 27 May 2004
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.eclipse.customswt;

import org.eclipse.jface.preference.ScaleFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;


/**
 @author Daniel Winterstein
 */
public class HelpfulScaleFieldEditor extends ScaleFieldEditor implements
        HelpfulControl {

    /**
     * @param name
     * @param labelText
     * @param parent
     */
    public HelpfulScaleFieldEditor(String name, String labelText,
            Composite parent) {
        super(name, labelText, parent);
    }

    /**
     * @param name
     * @param labelText
     * @param parent
     * @param min
     * @param max
     * @param increment
     * @param pageIncrement
     */
    public HelpfulScaleFieldEditor(String name, String labelText,
            Composite parent, int min, int max, int increment, int pageIncrement) {
        super(name, labelText, parent, min, max, increment, pageIncrement);
    }

    public Control[] getControls() {
        return new Control[] {getLabelControl(), getScaleControl()};
    }

    public void setToolTipText(String text) {
    	HelpfulController.setToolTipText(this, text);
    }

    public void setHelp(String helpContextId) {
    	HelpfulController.setHelp(this, helpContextId);
    }
    
}
