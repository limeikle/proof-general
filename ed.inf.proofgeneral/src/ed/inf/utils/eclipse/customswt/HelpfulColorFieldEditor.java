/*
 *  $RCSfile: HelpfulColorFieldEditor.java,v $
 *
 *  Created on 27 May 2004
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.eclipse.customswt;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;


/**
 @author Daniel Winterstein
 */
public class HelpfulColorFieldEditor extends ColorFieldEditor implements
        HelpfulControl {

    /**
     * 
     */
    public HelpfulColorFieldEditor() {
        super();
    }

    /**
     * @param name
     * @param labelText
     * @param parent
     */
    public HelpfulColorFieldEditor(String name, String labelText,
            Composite parent) {
        super(name, labelText, parent);
        this.parent = parent;
    }

    Composite parent = null;
    
    public Control[] getControls() {
        return new Control[] {getLabelControl(), getChangeControl(parent)};
    }

    public void setToolTipText(String text) {
    	HelpfulController.setToolTipText(this, text);
    }

    public void setHelp(String helpContextId) {
    	HelpfulController.setHelp(this, helpContextId);
    }
    
}
