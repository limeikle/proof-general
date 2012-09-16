/*
 *  $RCSfile: CurrentStateView.java,v $
 *
 *  Created on 13 May 2004
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.views.output;

import java.util.Observable;

import org.eclipse.jface.action.IToolBarManager;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.editor.lazyparser.PGIPParseResult;
import ed.inf.proofgeneral.sessionmanager.PGIPSyntax;
import ed.inf.proofgeneral.sessionmanager.ProverState;
import ed.inf.proofgeneral.sessionmanager.IProverStateObserver;
import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.proofgeneral.sessionmanager.events.PGIPError;
import ed.inf.proofgeneral.sessionmanager.events.PGIPEvent;
import ed.inf.proofgeneral.sessionmanager.events.PGIPIncoming;
import ed.inf.utils.process.RunnableWithParams;

/**
 * Displays the last message from the prover. Keeps a history which can be viewed.
 * TODO Create a parent class for LimitedBackForwardViews or something.  There be duplication here.
 */
public final class CurrentStateView extends HistoryOutputView implements IProverStateObserver {

	static volatile CurrentStateView fCurrentStateView = null;

	public CurrentStateView() {
		super();
		SessionManager[] sms = ProofGeneralPlugin.getSessionManagers();
		for(int i=0; i<sms.length; i++) {
			SessionManager sm = sms[i];
			if (sm != null) {
				sm.getProverState().addObserver(this);
			}
		}
		fCurrentStateView = this;
	}

	/**
	 * Decide whether or not to display this event
	 * Subclass to create specialised output viewer (eg. errors only)
	 * @param event
	 * @return true if the event is to be displayed.
	 */
	@Override
    public boolean eventFilter(PGIPEvent event) {
//		if (event instanceof PGIPError) {
//			return true;
//		}
		// ah: do we want the above?
		
		if (!(event instanceof PGIPIncoming)) {
			return false;
		}
		if (event instanceof PGIPParseResult) {
			return false;
		}
		if (event.type==null) {
			return false;
		}
		if (event.type.equals(PGIPSyntax.CLEARDISPLAY)) {
			return true;
		}
		
		if (event.type.equals("proofstate")) {
			return true;
		}
		// ah: TODO 'proofstate' response seems no longer to come back;
		// have added PGIPError above which i think is what we want
		
		// da: only proof state, but TODO: may want cleanup action on
		// some internal pgip messages (e.g. clear window when prover dies)
		// [that case is now handled by separate listener]
		return false;
	}

	boolean saidDead = true;
	
	/**
	 * Respond to changes in the ProverState.
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	public void update(Observable o, Object param) {
	   	assert o instanceof ProverState : "Wrong Observer type";
		ProverState ps = (ProverState)o;
		if (!ps.isAlive()) {
			String msg = "<font color='darkred'>Either the prover is dead or the connection has failed.</font>";
			clearHistory();
			setText(msg);
			saidDead = true;
			return;
		} else if (saidDead) {
			String msg = "<font color='green'>Prover is on-line.</font>";
			clearHistory();
			setText(msg);
			saidDead = false;
			return;			
		}
	}
	
	/**
	 * Given an incoming PGIP message event, set it as the new message
	 * @see ed.inf.proofgeneral.views.output.OutputView#pgipEvent(ed.inf.proofgeneral.sessionmanager.events.PGIPEvent)
	 */
	@Override
    public void pgipEvent(PGIPEvent event) {
		if (eventFilter(event)) {
			if (event.type.equals(PGIPSyntax.CLEARDISPLAY)) {
				// FIXME da: ought to check which area is being cleared
				// da: display thread spawn happens also in superclass, remove this.
				setText("");
			} else if (event instanceof PGIPError) {
				PGIPError in = (PGIPError) event;
				String text = in.getConverter().getDisplayText(event.parseTree);
				setText(text);
			} else if (event instanceof PGIPIncoming) {
				PGIPIncoming in = (PGIPIncoming) event;
				String text = in.getConverter().getDisplayText(event.parseTree);
				setText(text);
			}
		}
	}

	public static CurrentStateView getDefault() {
		return fCurrentStateView;
	}

	@Override
    protected void makeActions() {
		clearAction = new ClearAction(getDefault());
		forwardAction = new BackFwdBgAction(getDefault(), false);
		backAction = new BackFwdBgAction(getDefault(), true);
	}

	@Override
    protected void fillLocalToolBar(IToolBarManager manager) {
		//super.fillLocalToolBar(manager); block adding the clear action
		manager.add(backAction);
		manager.add(forwardAction);
	}

	/**
	 * Changes to the next / previous history item in the background.
	 */
	class BackFwdBgAction extends BackFwdAction {

		/**
		 * @see #BackFwdBgAction(HistoryOutputView, boolean)
		 */
		public BackFwdBgAction(HistoryOutputView view, boolean back) {
			super(view, back);
		}

		@Override
        public void run() {
			if (atLimit()) {
				return;
			}
			if (back) {
				setIndex(getIndex()+1); //we are one further forward
			}
			else {
				setIndex(getIndex()-1); // we are one further back
			}
			String replacement = history.get(history.size()-getIndex()-1);
			new RunnableWithParams(new Object[] { replacement }, "CurrentStateView call to setText") {
				public void run() {
					view.setText((String)p[0]);
				}
			}.callDefaultDisplayAsyncExec();
		}
	}

	/**
	 * @see ed.inf.proofgeneral.views.output.HistoryOutputView#getName()
	 */
	@Override
    public String getName() {
		return "Proof State";
	}


}
