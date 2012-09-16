/*
 *  $RCSfile: LatestOutputView.java,v $
 *
 *  Created on 01 Dec 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.views.output;
import java.util.ArrayList;
import java.util.Observable;

import org.dom4j.Element;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.widgets.Composite;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.editor.lazyparser.PGIPParseResult;
import ed.inf.proofgeneral.pgip2html.Converter;
import ed.inf.proofgeneral.sessionmanager.PGIPSyntax;
import ed.inf.proofgeneral.sessionmanager.ProverState;
import ed.inf.proofgeneral.sessionmanager.IProverStateObserver;
import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.proofgeneral.sessionmanager.events.PGIPError;
import ed.inf.proofgeneral.sessionmanager.events.PGIPEvent;
import ed.inf.proofgeneral.sessionmanager.events.PGIPIncoming;
import ed.inf.proofgeneral.sessionmanager.events.PGIPReady;

/**
 * A less fussy version of current proof state view.
 * @author Daniel Winterstein
 */
public class LatestOutputView extends HistoryOutputView implements IProverStateObserver {

	private static volatile LatestOutputView view;

	/** Whether we think the (default) prover is active. */
	boolean thinkDead = false;

	/** Output text accumulated */
	private final ArrayList<Element> outputs = new ArrayList<Element>();

	/**
	 * Constructs a new view and sets the latest instance to this object.
	 * Adds this object as listener to all current session managers.
	 * Should this throw PartInitException ?
	 * @see OutputView#OutputView()
	 */
	// NB da: Doesn't it make more sense to associate the view
	// with a particular session manager rather than all of them?
	public LatestOutputView() {
		super();
		SessionManager[] sms = ProofGeneralPlugin.getSessionManagers();
		for(int i=0; i<sms.length; i++) {
			SessionManager sm = sms[i];
			if (sm != null) {
				sm.getProverState().addObserver(this);
			}
		}
		view = this;
	}

	
	/**
	 * Respond to changes in the ProverState.
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	public void update(Observable o, Object param) {
	   	assert o instanceof ProverState : "Wrong Observer type";
		ProverState ps = (ProverState)o;
		if (ps.isAlive()) {
			if (thinkDead) {
				thinkDead = false;
				//immediately replaced by Welcome message, typically, but that's okay;
				//still do this, in case it isn't
				String msg = "<span color='green'>The prover is now running.</span>";
				setText(msg);
			}
		} else {
			thinkDead = true;
			String msg = "<span color='darkred'>Either the prover is dead or the connection has failed.</span>";
			setText(msg);
		}
		return;
	}
	
	/**
	 * @see ed.inf.proofgeneral.views.output.OutputView#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
    public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		boolean thinkDead = true;
		SessionManager sm = ProofGeneralPlugin.getSomeSessionManager();
		if (sm != null) {
			thinkDead = !sm.getProverState().isAlive();
		}
		String name = sm!=null ? sm.proverInfo.name : "default";
		label.setText("<p><b>Welcome to Proof General Eclipse!</b></p>"+
				"<p>"+(thinkDead ? "<font color='darkred'>The "+name+" proof assistant is not running yet.</span>" :
					"<font color='green'>The " + name + " proof assistant is running.</span>")+"</p>");
	}

	/**
	 * Decide whether or not to display this event
	 * Subclass to create specialised output viewer (eg. errors only)
	 * @param event
	 * @return true if this event is to be displayed
	 */
	@Override
    public boolean eventFilter(PGIPEvent event) {
		return true;  // the figuring is done in pgipEvent
	}


	/**
	 * Gather the outputs so far, formatting them.
	 * @return a string of the whole output
	 */
	// TODO da: this could be pretty inefficient in case of a lot of output.
	// Minor improvement may be to make a big document and transform it in
	// one go.
	// What we do in Emacs PG is to have a "spill threshold" after which we
	// start showing accumulated output directly.  But we also need a method
	// to add a line to the browser view, rather than set the whole text each time
	private String accumulateOutputs(Converter converter) {
		String output = "";
		plainOutput = "";
		synchronized (outputs) {
			for (Element msgcontent : outputs) {
				output += converter.getDisplayText(msgcontent);
				plainOutput += converter.getPlaintext(msgcontent.getStringValue());
			}
		}
		return output;
	}
	
	/** Used for hover display of prover output. */
	String plainOutput;

	public String getPlainOutput() {
		return plainOutput;
	}
	
	
	/**
	 * given an incoming pgip event, set it as the new message
	 */
	// da: Here for each command I see *two* (or 4, 5, 6) prover state change events
	// (why? are they parse change events?), then
	// a PGIP outgoing, then incoming, then ready.
	// Also, I get command processed events for commands which don't have a document
	@Override
    public void pgipEvent(PGIPEvent event) {
		if (event instanceof PGIPIncoming) {
			if (event instanceof PGIPParseResult) {
				return ;
			}
			if (event instanceof PGIPReady) {
				// Display the accumulated output since last <ready>
				// ah: only if there is output, otherwise we overwrite the nice startup message
				String msg = accumulateOutputs(((PGIPIncoming)event).getConverter());
				if (msg.length()>0) {
					setText(msg);
				}
				outputs.clear();
				return;
			}
			
			// TODO: would be better to filter positively below
			if (PGIPSyntax.SETIDS.equals(event.type)) {
				return;
			}
			// TODO: maybe add case for urgent output here,
			// which is supposed to be displayed immediately,so might
			// raise the view.

			// If we've got a proof state output, consider skipping it.
			// FIXME: PGIP 2.1 changes
//			if (event.type.equals("proofstate")) {
//				boolean proofStateViewActive =
//					(CurrentStateView.getDefault() != null) ?
//							CurrentStateView.getDefault().getViewSite()!=null : false;
//				if (proofStateViewActive) {
//					return ;
//				}
//			}
			
			if (event.parseTree.elements().size() > 0) {
				Element content = (Element)event.parseTree.elements().get(0);
				outputs.add(content);
			} else {
				setText(""); // formerly CLEARDISPLAY in PGIP 2.0
			}
		} else if (event instanceof PGIPError) {
			//String error = Converter.stringToXml(event.parseTree.getStringValue());
			// da: message is in PGML, try this instead:

			Element error = null;
			if (event.parseTree!=null && event.parseTree.elements().size() > 0) {
				error = (Element)event.parseTree.elements().get(0);
			}
			// FIXME da: make element for these "fake" or "dummy" messages.
            // Although I'd rather they were not generated in the first place.
            /*
			if (error==null || error.length()==0) {
				error =	Converter.stringToXml(event.getText());  //should always have text on errors
			    error = "<font color='darkred'>"+error+"</font>";
			}
			*/
			if (error != null) {
				outputs.add(error);
			}
			super.setTextNohistory(accumulateOutputs(((PGIPError)event).getConverter())); // display text so far, including error.
		}
	}

//	/**
//	 * Show the given error message in the view.  The message should
//	 * be formatted as HTML.
//	 * @param error
//	 */
	// da: the previous version of this pre-pended the message to the previous
	// output.  This seemed quite confusing because most of the time it had nothing
	// to do with the previous output.  Now we replace the previous message.
	// If the user wants to see the previous output, she can either use the history or
	// (for proofs), the proof state view.
	/* da: it's rude to use this method to blast away the true output from the prover
	 * with a message from the interface.   Could do with something more transient/
	 * which still doesn't get in users way.  Disabling for now while working on message
	 * cleanup.
	public synchronized void updateWithError(String error) {
		if (error==null) error="error (no message available)";
		String text = "<font color='darkred'>"+error+"</font>";
		setText(text);

		//old code from AH in this section, possibly useful if we need to shorten/clean-up the errors?
		//(delete when we discover it's not at all useful)
//		String old_text = textBeforeError;
//		if (old_text==null) old_text = getText();
//
//		error = error.trim();
//		int lf = error.indexOf('\n');
//		//old AH code; not sure whether this is desired
////		error = General.sReplace(error, "***", "").trim();
////		//seems to be better if we show entire message -AH ?
////		String short_error = error;
////		if (lf>=0) short_error = error.substring(0, lf).trim();  //only display the first line here
////		if (short_error.endsWith(",")) error = error.substring(0, error.length()-1).trim();  //get rid of , at end
//		if (lf>=0) error = error.substring(0, lf).trim();  //only display the first line here
//		if (error.endsWith(",")) error = error.substring(0, error.length()-1).trim();  //get rid of , at end
//
////		if (old_text.indexOf(short_error)>=0) return;  //we're already displaying this
//		if (old_text.indexOf(error)>=0) return;  //we're already displaying this
//
//		String text = "<p><font color='darkred'>"+error+"</font></p>"+old_text;
//		setText(text);
//		textBeforeError = old_text;
	}
    */

	public static LatestOutputView getDefault() {
		if (view == null) {
			try {
				ProofGeneralPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().showView("ed.inf.proofgeneral.views.LatestOutput");
			} catch (Exception e) {
				System.err.println("ERROR opening LatestOutputView:");
				if (ProofGeneralPlugin.debug(LatestOutputView.class)) {
					e.printStackTrace();
				}
			}
		}
		return view;
	}

	public static boolean isCreated() {
		return (view != null);
	}

	@Override
    protected void makeActions() {
		forwardAction = new BackFwdAction(getDefault(), false);
		backAction = new BackFwdAction(getDefault(), true);
	}

	@Override
    protected void fillLocalToolBar(IToolBarManager manager) {
		manager.add(backAction);
		manager.add(forwardAction);
	}

	/**
	 * Name of this view
	 */
	@Override
    public String getName() {
		return "Prover Output";
	}
}
