/*
 *  $RCSfile: OutputView.java,v $
 *
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.views.output;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.pgip2html.Converter;
import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.proofgeneral.sessionmanager.events.IPGIPListener;
import ed.inf.proofgeneral.sessionmanager.events.PGIPEvent;
import ed.inf.proofgeneral.sessionmanager.events.PGIPIncoming;
import ed.inf.utils.datastruct.StringManipulation;
import ed.inf.utils.eclipse.DisplayCallable;
import ed.inf.utils.process.RunnableWithParams;


/**
 * This is the basic output view. It simply displays all PGIP events.
 *
 * Subclasses can implement more interesting behaviour by changing the methods:
 *
 * 		eventFilter
 *
 * 		pgipEvent
 */
public abstract class OutputView extends ViewPart implements IPGIPListener {

    /**
     * The widget that displays the text.
     */
	//protected FormTextView label;
	protected HTMLTextLabel label;
	/**
	 * Stick messages in here if label is null
	 * (ie. if the view has not been created yet).
	 */
	String store = "";

	//this field isn't actually used(!) -- we can assume it is connected if there is a SM open and we've addAllSMListeners this
	//public boolean connected = false;


	/**
	 * Decide whether or not to display this event
	 * Subclass to create specialised output viewer (eg. errors only)
	 * @param event
	 * @return true if the event is to be displayed
	 */
	public boolean eventFilter(PGIPEvent event) {
		return true;
	}

	/**
	 * Changes the label text, and queues a method to update the viewer once only.
	 * @param msg new label text, must be non-null
	 */
	public void setText(final String msg) {
		assert(msg!=null);
		// da: added this opt in case of duplicated output; this loses in case
		// the output is really fresh and ought to be brought to the attention
		// of the user again.
		if (msg.equals(store)) {
			return;
		}
		store = msg;
		// FIXME da: I'm suspecting that this strategy is
		// far too expensive.  It looks as if it
		// spawns a new thread for every output call!  But we may get
		// *thousands* of very rapid output messages from the prover,
		// so we can't afford to be spawning threads every time.
		// We probably shouldn't even be firing events for each one.
		// Instead we should batch the updates (they anyway should be collected
		// together between <ready/>'s, and only "urgent" messages shown immediately,
		// this is part of the message model).  Moreover, we should use
		// startquiet and stopquiet messages before processing/undoing long files.
		// This is something that has been solved in Emacs for some years...
		
		// ah: not convinced this is that expensive. i don't think it creates threads,
		// just Runnables which get executed, and they bail out fast if they are no longer relevant
		// (i.e. store!=msg).
		
		// however i agree an "urgent" mode would be very good.
		// (also agree eclipse's display model is primitive.)
		
		new DisplayCallable("OutputView.setText") {
			@Override
            public Object run() {
				//this logic doesn't work if we translate the text (ie html in setTextLabel), so could be more efficient -AH
				if (store!=msg) { // ES, OK
					//System.err.println(General.makeDateString()+"  fast cancelling text label set for "+this+", was "+(msg.length()>20 ? msg.substring(0, 20) : msg));
					return null;
				}
				//System.err.println(General.makeDateString()+"  yield in display thread");
				Thread.yield(); //to discourage unnecessary display updates
				if (store==msg) {
					//System.err.println(General.makeDateString()+"  setting text label for "+this+", to "+(msg.length()>20 ? msg.substring(0, 20) : msg));
					setTextLabel();
					//OutputView.this.setPartProperty(key, value);
					//System.err.println(General.makeDateString()+"  set text label for "+this);
				} // else {
					//System.err.println(General.makeDateString()+"  late cancelling text label set for "+this+", was "+(msg.length()>20 ? msg.substring(0, 20) : msg));
				// }
				return null;
				//else 				//it has been updated again
			}
		}.runDisplay();
	}

	void setTextLabel() {
		// formText: setTextLabel(false);
		setTextLabel(true);
	}

	/**
	 * Puts the HTML text to the label, optionally adding an HTML header
	 * <pre>
	 * &lt;html xmlns="http://www.w3.org/1999/xhtml"&gt;
	 * &lt;head&gt;
	 * &lt;meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/&gt;
	 * &lt;/head&gt;&lt;body&gt;
	 * [text]
	 * &lt;/body&gt;&lt;/html&gt;
	 * </pre>
	 * @param addHtmlPrefixIfNecessary
	 */
	void setTextLabel(boolean addHtmlPrefixIfNecessary) {
		String text = store;
		if (addHtmlPrefixIfNecessary) {
			//added by AH to have unicode (utf-8) set
			String t2 = text.trim();
			if (t2.startsWith("<")) {
				//might be html
				t2 = t2.substring(1).trim();
				if (t2.length()>6) {
					t2 = t2.substring(0, 6);
				}
				if (t2.toLowerCase().startsWith("html")) {
					t2 = null; //it is html!
				}
			}
			if (t2!=null) { //not html, wrap it in html tags so we get UTF-8 charset
				text = "<html xmlns=\"http://www.w3.org/1999/xhtml\">" +
						"<head>" +
						"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>" +
						"</head><body>\n"+
						text+
						"</body></html>";
			}
		}
		try {
			//long ms = System.currentTimeMillis();
			//System.out.println("setting OutputViewer state: "+text);
			store = text;
			if (label!=null && label.browser!=null && !label.browser.isDisposed()) {
				label.setText(text);
				//scroll it to the bottom  -- doesn't work, and is wrong thread (?? or is it ??)
//				if (parent!=null && parent.getVerticalBar()!=null) {
//					ScrollBar vb = parent.getVerticalBar();
//					if (vb.getMaximum()>vb.getThumb())
//					  vb.setSelection(vb.getMaximum()-vb.getThumb());
//				}
			}
			//ms = System.currentTimeMillis() - ms;
			//System.out.println(""+General.makeDateString()+" ["+(General.makeTimeString(ms))+"]:  "+this+"  "+text);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getText() {
		return store;
	}

	//private DrillDownAdapter drillDownAdapter;
	protected Action clearAction;
	//private Action doubleClickAction;

// CLEANUP	
//	/** The default View instance */
//	protected static final OutputView view;

	/**
	 * Creates a new OutputView.  Should probably only be called by getDefault();
	 */
	protected OutputView() {
		super();
		SessionManager.addAllSessionManagerListener(this);
		// CLEANUP
		// view = this;
		//    connected = true;
		
		//		//		 register as a PGIP event listener with *ALL* the session managers
		//		SessionManager[] sms = ProofGeneralPlugin.getDefault().getSessionManagers();
		//		for(int i=0; i<sms.length; i++) {
		//		    SessionManager sm = sms[i];
		//		    if (sm != null) {
		//		        sm.addListener(this); // this is mirrored in SessionManager(), to protect against odd starting orders
		//		        connected = true;
		//		    }
		//		}
	}

	/**
	 * Remove ourselves from the session manager listeners.
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	@Override
    public void dispose() {
		SessionManager.removeAllSessionManagerListener(this, true);
		super.dispose();
	}

	/**	Given an incoming pgip event, append it to the existing text */
	public void pgipEvent(PGIPEvent event) {
		if (eventFilter(event)) {
			if (label != null) {
				String text = StringManipulation.convertLineBreak(label.getText()+"<br/>");
				if (event instanceof PGIPIncoming) {
					Converter converter = ((PGIPIncoming) event).getConverter();
					text = text + converter.getDisplayText(event.parseTree);
				} else  {
					text = text + Converter.plainConverter.getPlaintext(event.parseTree.toString()); // FIXME: nonsense?
				}
				//setText(text);
				new RunnableWithParams(new Object[] { text }, "OutputViewer.pgipEvent call to setText") {
					public void run() {
						setText((String)p[0]);
					}
				}.callDefaultDisplayAsyncExec();
			}
		}
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	@Override
    public void createPartControl(Composite parent) {
		checkBrowserLibrary(parent.getShell(), true);
		label = new HTMLTextLabel(parent);
	    // FT: label = new FormTextView();
		label.setText(store);
		// FT: label.createPartControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(label.browser,"ed.inf.proofgeneral.generic");
		makeActions();
		contributeToActionBars();
		// this.parent = parent;  CLEANUP
		if (!initialised) {
			init();
		}
	}

	private static Boolean BROWSER_LIBRARY_FILES_FOUND = null;
	private static boolean BROWSER_LIBRARY_WARNING_GIVEN = false;
	/**
	 * checks that the system library file(s) needed for Mozilla browser are available;
	 * return whether the browser setup looks okay or not.
	 * <p>
	 * optionally can display a dialog warning to the user (once only)
	 */
	@SuppressWarnings("boxing")
    public static boolean checkBrowserLibrary(Shell shell, boolean warnUser) {
		if (BROWSER_LIBRARY_FILES_FOUND!=null) {
			return BROWSER_LIBRARY_FILES_FOUND.booleanValue();
		}
		Browser b = null;
		try {
			b = new Browser(shell, SWT.NONE);
			try {
				b.setText("<html><b>Test</b></html>");
			} finally {
				b.dispose();
			}
			//it's okay
			BROWSER_LIBRARY_FILES_FOUND = true;
			return true;
		} catch (Throwable e) {
			//not found, let's see why...
			System.err.println("WARNING: discovered browser configuration error, trying to investigate " +
					"("+e+")");
			e.printStackTrace();
			String msg = null;
			msg  = "It appears that Eclipse is unable to load the HTML browser for your system. " +
				"Some important functionality is likely to be unavailable until this is corrected " +
				"(and it may not work at all). " +
				"This is a well-known difficulty between Eclipse and Linux browsers, and it is " +
				"hoped this will be fixed in new versions of Eclipse and/or Linux.  For now, there are " +
				"some things you can try:";
			msg += "\n\n";
			if (e.toString().indexOf("MOZILLA_FIVE_HOME")>=0) {
				msg  += "Make sure the environment variable 'MOZILLA_FIVE_HOME' points to your Mozilla or xulrunner " +
						"directory (e.g. \"export MOZILLA_FIVE_HOME=/usr/lib/xulrunner\", or /usr/lib/mozilla, or comparable).";
			} else {
				msg  += "Try varying where the environmnet variable MOZILLA_FIVE_HOME points " +
						"(/usr/{lib,local}/{mozilla,xulrunner,mozilla-firefox}), " +
						"and note that often it is a directory which contains libxpcom.so. " +
						"You may also try upgrading to the latest versions of Mozilla, Firefox, and/or xulrunner. " +
						"(As a last resort, try adding that library directory to /etc/ld.so.conf and rerun ldconfig -- " +
						"the problem seems to be a version, path, or unsatisfied link error: " +
						e.toString()+".)";
			}
			msg += "\n\n";
			msg += "More information can be found in local Eclipse log files and possibly other error messages, " +
					"as well as on the SWT FAQ on the Eclipse web site:" +
					"http://www.eclipse.org/swt/faq.php#browserlinux";
			
			if (!BROWSER_LIBRARY_WARNING_GIVEN && warnUser) {
				MessageDialog d = new MessageDialog(shell, "Browser Error",
						null, msg, MessageDialog.ERROR, new String[] { "&OK" }, 0);
		           d.setBlockOnOpen(true);
		           //d.getShell().moveAbove(null);
		           d.open();

			}
			BROWSER_LIBRARY_FILES_FOUND = false;
			return false;
		}
	}

	// CLEANUP not used
	//Composite parent = null;

	boolean initialised = false;

	/**
	 * A view may need to be initialised. Sub-class this to do so.
	 * It will typically be called once, at the end of createPartControl.
	 */
	protected void init() {
	    initialised=true;
	}

	protected void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalToolBar(bars.getToolBarManager());
	}

	/**
	 * Change this to add more actions to the toolbar
	 * @param manager
	 */
	protected void fillLocalToolBar(IToolBarManager manager) {
		manager.add(clearAction);
		manager.add(new Separator());
	}

	abstract protected void makeActions();


	/**
	 * A button action that clears the viewer
	   @author Daniel Winterstein
	 */
	static class ClearAction extends Action {
		OutputView view;
	    public ClearAction(OutputView view) {
	        this.view = view;
	        super.setText("Clear");
			setToolTipText("Clear the viewer");
			ImageDescriptor imd = ProofGeneralPlugin.getImageDescriptor("icons/console_clear.gif");
			setImageDescriptor(imd);
	    }
		@Override
        public void run() {
			view.setText("");
		}
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
    public void setFocus() {
		//viewer.getControl().setFocus();
	}

}