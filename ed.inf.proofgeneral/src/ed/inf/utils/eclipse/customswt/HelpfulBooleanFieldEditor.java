/*
 *  $RCSfile: HelpfulBooleanFieldEditor.java,v $
 *
 *  Created on 27 May 2004
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.eclipse.customswt;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * @author Daniel Winterstein
 */
public class HelpfulBooleanFieldEditor extends BooleanFieldEditor implements HelpfulControl {

    /**
     * 
     */
    public HelpfulBooleanFieldEditor() {
        super();
    }

    /**
     * @param name
     * @param labelText
     * @param style
     * @param parent
     */
    public HelpfulBooleanFieldEditor(String name, String labelText, int style,
            Composite parent) {
        super(name, labelText, style, parent);
        this.parent = parent;
    }

    /**
     * @param name
     * @param label
     * @param parent
     */
    public HelpfulBooleanFieldEditor(String name, String label, Composite parent) {
        super(name, label, parent);
        this.parent = parent;
    }

    Composite parent;

    public Control[] getControls() {
        return new Control[] {getLabelControl(),getChangeControl(parent)};
    }

    public void setToolTipText(String text) {
    	HelpfulController.setToolTipText(this, text);
    }

    public void setHelp(String helpContextId) {
    	HelpfulController.setHelp(this, helpContextId);
    }
}
