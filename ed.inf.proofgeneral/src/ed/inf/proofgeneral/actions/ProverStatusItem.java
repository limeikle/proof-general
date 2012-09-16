/*
 *  $RCSfile: ProverStatusItem.java,v $
 *
 *  Created on 29 Nov 2006 by grape
 *  part of Proof General for Eclipse
 */

package ed.inf.proofgeneral.actions;

import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.texteditor.StatusLineContributionItem;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.sessionmanager.ProverState;
import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.proofgeneral.ui.theme.PGColors.PGColor;

/**
 * This should report the current status of the prover at all times.
 * TODO complete this (add as an observer, and respond to changes)
 * FIXME fix background colour problems; at present this colours the background of its parent;
 * instead it should colour the background of its own control.
 */
public class ProverStatusItem extends StatusLineContributionItem implements Observer {

	public static final String dead = "Theorem Prover appears to be dead.";
	public static final String alive = "The Theorem Prover is alive.";

	private static final Color processed = PGColor.PROCESSED.get();
	private static final Color locked = PGColor.BUSY.get();

	public static final String ID = "ed.inf.proofgeneral.proverstatus";

	/** Creates a new ProverStatusItem. */
	public ProverStatusItem() {
		super(ID);
		setText("initialising...");
	}

	/**
	 * Update status according to change in ProverState.
	 */
	public void update(Observable o,  Object ignored) {
		assert o instanceof ProverState : "Wrong Observable type";
		ProverState ps = (ProverState) o;
		String tip = "";
		if (ps.isAlive()) {
			if (ps.getState() == ProverState.State.PROOF_LEVEL) {
				setText("proof ("+ps.getProofDepth()+")");
				tip = "  It is in level "+ps.getProofDepth()+" of a theory.";
			} else {
				setText(ps.getState().toString());
			}
			tip = alive+tip;
			setToolTipText(tip);
		} else {
			setText("offline");
			tip = dead;
		}
	}

	/**
	 * TODO: Doesn't quite work as expected...!
	 * @see org.eclipse.ui.texteditor.StatusLineContributionItem#fill(Composite)
	 */
	@Override
    public void fill(Composite parent) {
		if (!ProofGeneralPlugin.debug(this)) {
			return;	// naughty HACK to hide this unfinished code.
		}
		// System.out.println("Setting background"); // DEBUG
		Color c = parent.getBackground();
		Device d = c.getDevice();
		ProofScriptDocument doc = null;
		// TODO: should notice change of SM/editor here
		SessionManager sm = ProofGeneralPlugin.getSomeSessionManager();
		if (sm != null) {
			doc = sm.getActiveScript();
			if (doc != null) {
				if (doc.isActiveForScripting()) {
					c = processed;
				} else if (doc.isLocked()) {
					c = locked;
				}
			} else {
				c = new Color(d, new RGB(220, 220, 220));
			}
		}
		parent.setBackground(c);
		super.fill(parent);
	}

	/**
	 * Sets a prover state listener up for this action/label.
	 * @see ed.inf.proofgeneral.editor.actions.PGAction#setActiveEditor(org.eclipse.ui.IEditorPart)
	 */
// da: note: this now needs to use Observable methods.  
//	public void setActiveEditor(IEditorPart targetEditor) {
//		super.setActiveEditor(targetEditor);
//		if (targetEditor instanceof ProofScriptEditor) {
//			ProverState nps = ((ProofScriptEditor) targetEditor).getSessionManager().proverState;
//			if (ps==nps) return;
//			if (ps != null) ps.removeListener(this);
//			ps = nps;
//			ps.addListener(this);
//			// update those things that look out for prover state
//			pgipEvent(new ProverState.ProverStateChangeEvent(ps));
//		} else {
//			if (ps != null) ps.removeListener(this);
//			ps = null;
//		}
//	}



}
