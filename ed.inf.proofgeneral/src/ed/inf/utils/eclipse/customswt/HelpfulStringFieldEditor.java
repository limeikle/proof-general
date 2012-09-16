/*
 *  $RCSfile: HelpfulStringFieldEditor.java,v $
 *
 *  Created on 27 May 2004
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.eclipse.customswt;

import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;


/**
 @author Daniel Winterstein
 */
public class HelpfulStringFieldEditor extends StringFieldEditor implements HelpfulControl {

    /**
     * @param name
     * @param labelText
     * @param width
     * @param strategy
     * @param parent
     */
    public HelpfulStringFieldEditor(String name, String labelText, int width, int strategy, Composite parent) {
        super(name, labelText, width, strategy, parent);
    }

    /**
     * @param name
     * @param labelText
     * @param width
     * @param parent
     */
    public HelpfulStringFieldEditor(String name, String labelText, int width, Composite parent) {
        super(name, labelText, width, parent);
    }

    /**
     * @param name
     * @param labelText
     * @param parent
     */
    public HelpfulStringFieldEditor(String name, String labelText, Composite parent) {
        super(name, labelText, parent);
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
    
}
