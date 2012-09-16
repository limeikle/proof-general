/*
 *  $RCSfile: HTMLTextLabel.java,v $
 *
 *  Created on 15 Jul 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.views.output;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;

/**
 * @author Daniel Winterstein
 */
public class HTMLTextLabel {
    String text = null;
    Browser browser;
    
    public HTMLTextLabel(Composite parent) {
        browser = new Browser(parent,SWT.NONE);//??SWT.DEFAULT);
    }
    
    /**
     * This method expects html fragments; it will add some html wrapping
     * @param htmlFragment the fragment to wrap and display in the browser
     */
    public void setText(String htmlFragment) {
    	try {    	
    		text = htmlFragment;
    		browser.setText("<html><body>"+htmlFragment+"</body></html>");
    	} catch (Exception e ) { e.printStackTrace(); }
    }
    
    public String getText() {
        return text;
    }
    
}
