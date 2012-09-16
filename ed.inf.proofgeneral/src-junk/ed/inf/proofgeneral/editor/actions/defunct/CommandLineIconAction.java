/*
 *  $RCSfile: CommandLineIconAction.java,v $
 *
 *  Created on 30 Nov 2006 by grape
 *  part of Proof General for Eclipse
 */

package ed.inf.proofgeneral.editor.actions.defunct;

import java.util.Observable;

import ed.inf.proofgeneral.document.CmdElement;
import ed.inf.proofgeneral.sessionmanager.ProverState;
import ed.inf.proofgeneral.sessionmanager.IProverStateObserver;

/**
 * Shows prover status in the status bar.
 */
// * @deprecated replaced by a StatusLineContributionItem:
// * 				Use {@link ProverStatusItem}
// */@Deprecated
public class CommandLineIconAction extends PGAction implements IProverStateObserver {

// 
//	 /**
//	  * @deprecated - use {@link ProverStatusItem}
//	  */@Deprecated
	  public CommandLineIconAction() {
		  super();
		  this.setEnabled(false);
		  this.setToolTipText("Shows the (suspected) state of the theorem prover");
		  this.setText("unknown");
	  }

	/**
	 * Sets a prover state listener up for this action/label.
	 * @see ed.inf.proofgeneral.editor.actions.PGAction#setActiveEditor(org.eclipse.ui.IEditorPart)
	 */
	//        public void setActiveEditor(IEditorPart targetEditor) {
	//            super.setActiveEditor(targetEditor);
	//            if (targetEditor instanceof ProofScriptEditor) {
	//                ProverState nps = ((ProofScriptEditor) targetEditor).getSessionManager().proverState;
	//                if (ps==nps) return;
	//                if (ps != null) ps.removeListener(this);
	//                ps = nps;
	//                ps.addListener(this);
	//                // update those things that look out for prover state
	//                pgipEvent(new ProverState.ProverStateChangeEvent(ps));
	//            } else {
	//                if (ps != null) ps.removeListener(this);
	//                ps = null;
	//            }
	//        }

	/**
	 * Sends some query commands when clicked.
	 * FIXME this doesn't appear to send valid commands.
	 */
	@Override
    public void run() {
		// TODO implement
		try {
			getSessionManager().queueCommand(new CmdElement("showproofstate"),this);
			getSessionManager().queueCommand(new CmdElement("showctxt"),this);
		} catch (Exception x) {
			error(x);
		}
		// error(new ScriptingException("Nothing implemented yet - not sure what should be"));
	}

	/**
	 * Updates the display to match the prover's internal state (if the prover has sent a proverstatechange event).
	 */
    public void update(Observable o, Object param) {
       	assert o instanceof ProverState : "Wrong Observer type";
    	ProverState ps = (ProverState)o;
		if (ps.isAlive()) {
			if (ps.getState() == ProverState.State.PROOF_LEVEL) {
				setText("proof ("+ps.getProofDepth()+")");
			} else {
				setText(ps.getState().toString());
			}
			setToolTipText("The Theorem Prover is alive.");
		} else {
			setText("offline");
			setToolTipText("Theorem Prover appears to be dead.");
		}
	}

}
