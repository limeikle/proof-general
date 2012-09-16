/*
 *  $RCSfile: LaunchBrowserAction.java,v $
 *
 *  Created on 9 Jan 2007 by gdutton
 *  part of Proof General for Eclipse
 */

package ed.inf.proofgeneral.actions;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.PlatformUI;

import ed.inf.utils.eclipse.ErrorUI;

/**
 * Action which launches the user's browser at a certain page.
 */
public abstract class LaunchBrowserAction extends Action {
	protected final String text;
	protected final URL url;

	/**
	 * Constructs an action targetting a browser at the given URL.
	 * @param text name of the action - must not be null.
	 * @param tip the tooltip text
	 * @param desc the action description
	 * @param url the target url of the action
	 */
	public LaunchBrowserAction(String text, String tip, String desc, URL url) {
		super();
		assert text != null;
		this.setText(text);
		if (tip != null) {
			this.setToolTipText(tip);
		}
		if (desc != null) {
			this.setDescription(desc);
		}
		this.text = text;
		this.url = url;
	}

	/**
	 * Concise constructor for a simple URL launcher.
	 * @param text name of the action.
	 * @param url the target url of the action
	 * @throws MalformedURLException if the string does not represent a well-formed URL.
	 */
	public LaunchBrowserAction(String text, String url) throws MalformedURLException {
		this(text, null, null, new URL(url));
	}

	/**
	 * Launches the Workbench-approved URL browser.
	 */
	@Override
    public void run() {
		try {
		    PlatformUI.getWorkbench().getBrowserSupport().createBrowser(getBrowserName()).openURL(url);
		} catch (Exception e) {
			// MalformedURLException or PartInitException or other
			String emsg = "Error opening web page.\n" +
			        "The web-address for online help is invalid or the browser cannot be opened.\n" +
	        		"Please check Preferences->Proof General and Preferences -> General -> Web browser.";
	        ErrorUI.getDefault().signalError(new Exception(emsg));
	        return;
		}
	}

	/**
	 * The name of this Workbench-launched browser.
	 * @return the browser 'name'.
	 */
	public String getBrowserName() {
		return "url_"+text;
	}

	/**
	 * Calls {@link #run()}.
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
	    run();
	}

}
