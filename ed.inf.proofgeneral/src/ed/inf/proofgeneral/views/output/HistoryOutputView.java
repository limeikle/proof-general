/*
 *  $RCSfile: HistoryOutputView.java,v $
 *
 *  Created on 24 Nov 2006 by gdutton
 *  part of Proof General for Eclipse
 */

package ed.inf.proofgeneral.views.output;

// TODO: add button to clear history

import java.util.Stack;

import org.eclipse.jface.action.Action;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.utils.datastruct.LimitedStack;

/**
 * An output view with History functionality.
 */
public abstract class HistoryOutputView extends OutputView {

	/**
	 * History of previous output
	 */
	protected Stack<String> history;

	/** Current position in the view */
	private int index;

	/** Size of history to keep */
	public static final int HISTORY_LENGTH = 30;

	public HistoryOutputView() {
		super();
		history = new LimitedStack<String>(HISTORY_LENGTH);
		index = 0;
	}

	public void clearHistory() {
		history.clear();
		setIndex(0);
	}

	/** The Back button */
	protected BackFwdAction backAction;

	/** The forward button */
	protected BackFwdAction forwardAction;

	/**
	 * @see ed.inf.proofgeneral.views.output.OutputView#makeActions()
	 */
	@Override
    protected abstract void makeActions();

	/**
	 * The name of this output view
	 * @return a friendly name for this view.
	 */
	public abstract String getName();


	/**
	 * @param index The index to set.
	 */
	// FIXME da: there is a race condition in start-up here (or maybe simply
	// problem with unrestored view?).   It can happen that setIndex is called
	// when there are no actions available.  Test case: switch to unopened output view
	// while prover is producing a bunch of output/working hard (Try TraceSimp.thy for
	// a sure crash).
	public void setIndex(int index) {
		this.index = index;
		if (forwardAction != null) {
			forwardAction.setEnabled(index != 0);
		}
		if (backAction != null) {
			backAction.setEnabled(history.size()>1 && index<history.size()-1);
		}
	}

	/**
	 * The current index within the view
	 * (output which has no index can return 0)
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Changes the viewer text, and queues a method to update the viewer once only.
	 * The text is pushed onto the history.
	 */
	@Override
    public void setText(final String msg) {
		super.setText(msg);
		historyStore(msg);
		setIndex(0);
	}

	/**
	 * Changes the viewer text, and queues a method to update the viewer once only.
	 * The text is not pushed onto the history (useful for interim output).
	 */
	// da:
	// This does result in a glitch: if go back and then forward we retrieve
	// the hidden output blasted by the setText.
	// The best history behaviour would be to keep a flag which indicates
	// when the next message comes along whether the previous one should be
	// kept in the history or not.
	 //
	public void setTextNohistory(final String msg) {
		super.setText(msg);
		setIndex(0);
	}

	/**
	 * Store a message in the history, unless the top of the history
	 * is empty, in which case we replace it with the given item, or
	 * the history already contains the current message at the top.
	 * @param msg the item to store
	 */
	public void historyStore(String msg) {
		if (!history.empty() && !history.firstElement().equals("")) {
			history.setElementAt(msg, 0);
		} else if (history.empty() || !history.firstElement().equals(msg)) {
			history.push(msg);
		}
	}

	private void superSetText(final String msg) {
		super.setText(msg);
	}

	/**
	 * A button action that moves the viewer back to previous state
	 * @author Daniel Winterstein
	 */
	public class BackFwdAction extends Action {
		protected HistoryOutputView view;
		protected boolean back;

		/**
		 * Creates Back/Forward buttons for an output view.
		 * @param view the view to control
		 * @param back true if this is a back action, false if it is forward.
		 */
		public BackFwdAction(HistoryOutputView view, boolean back) {//, ForwardAction fwd) {
			this.view = view;
			this.back = back;
			super.setText(back?"Back":"Forward");
			setToolTipText("See the "+(back?"previous":"next")+" "+getName());
			String img = getText().toLowerCase()+".gif";
			setImageDescriptor(ProofGeneralPlugin.getImageDescriptor("icons/e_"+img));
			setDisabledImageDescriptor(ProofGeneralPlugin.getImageDescriptor("icons/"+img));
			setEnabled(false);
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
			view.superSetText(replacement);
			//System.out.println("At history position " + Integer.toString(index)); // da: TEMP
		}

		/**
		 * Returns true if this action is at its limit
		 * (i.e. can not be performed in this direction)
		 */
		public boolean atLimit() {
			return back ? (getIndex() >= history.size()-1) : (getIndex() < 1);
		}
	}

}
