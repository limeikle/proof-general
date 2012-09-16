/*
 *  $RCSfile: ChooseLogicAction.java,v $
 *
 *  Created on 21 Apr 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */

// FIXME: this action is entirely ISABELLE SPECIFIC
package ed.inf.proofgeneral.prover.isabelle.editor.actions;

import java.io.IOException;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Shell;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.ProofGeneralProverRegistry.ProverRegistryException;
import ed.inf.proofgeneral.editor.actions.retargeted.RestartAction;
import ed.inf.proofgeneral.sessionmanager.ScriptingException;
import ed.inf.utils.eclipse.ErrorUI;
import ed.inf.utils.process.PooledRunnable;

public class ChooseLogicAction extends Action {

	public ChooseLogicAction() {
//		requiredProverStatusLevel = IGNORE_PROVER;
//		requiredScriptStatusLevel = IGNORE_SCRIPT;
//		resetOnShutdown = false;
//		setStatusDoneTrigger(STATUS_RUNNING | STATUS_DONE_PROVER);
		this.setToolTipText("Choose the logic to use with Isabelle (set in code)");
		this.setText("Choose Logic");
		this.setDescription("Choose the logic to use with Isabelle (description, in code)");
	}

//	@Override
//    public void pgipEvent(PGIPEvent event) {
//		if (isEnabled()) {
//			if (event instanceof PGIPShutdown) {
//				setEnabled();  //shutting down
//			}
//			return ;  //if we're enabled, we only care about shutdown events
//		}
//		//not enabled -- check on each event
//		setEnabled();
//	}
//
//	@Override
//    public boolean handleOurPgipEvent(PGIPEvent event) {
//		//not called
//		return false;
//	}
//
//
//	public boolean decideEnabled() {
//		boolean b = super.decideEnabled();
//		System.out.println("Choose logic is enabled: "+b);
//		return b;
//	}



	@Override
    public void run() {
		try {
			ChooseLogicDialog dialog = new ChooseLogicDialog(new Shell());
			dialog.setBlockOnOpen(true);

			String proverCmd = ProofGeneralPlugin.getSessionManagerForProver("Isabelle").proverInfo
		     .getLaunchCommand();
//			if (ProofGeneralPlugin.getDefault().getProverManager("Isabelle").proverInfo
//		        .getSpecialLaunchCommand()!=null) {
//				//TODO need to know if user has changed preferences
//			}
			//System.out.println("isabelle uses cmd line '"+proverCmd+"'");
			String cmdWithoutLogic = proverCmd.trim();
			String logic = null;
			int ls = cmdWithoutLogic.lastIndexOf(' ');
			if (ls>0) {
				//TODO HACK! this just assumes the logic is the last word on the line
				//if it is not the first word and it doesn't start with -
				String lastWord = cmdWithoutLogic.substring(ls+1);
				if (!lastWord.startsWith("-")) {
					//System.out.println("stripping logic '"+lastWord+"' from cmd line");
					cmdWithoutLogic = cmdWithoutLogic.substring(0, ls).trim();
					logic = lastWord.trim();
				}
			}
			if (logic==null || logic.length()==0) {
				logic="HOL";   //default
			}
			dialog.setDefaultLogic(logic);

			//dialog.setUserCwd(ProofGeneralPlugin.getProverManager("Isabelle").proverInfo
			//	   .getProverStartDir());

			if (dialog.open() == ChooseLogicDialog.OK) {
				// String userCwd = dialog.getUserCwd();

				String s = dialog.getSelectedLogic();
				if (s==null || s.equals("")) {
					return;
				}

				//System.out.println("user chose logic "+s);
				if (cmdWithoutLogic==null || cmdWithoutLogic.length()==0) {
					cmdWithoutLogic = "isabelle -I -X";  //shouldn't happen
				}
				String newCmd = cmdWithoutLogic+" "+s;

//				String cmd = ProofGeneralPlugin.getDefault().getPluginPreferences()
//				.getString("Isabelle Start Command");
//				if (cmd==null || cmd.length()==0) {
//					error(new Exception("no preference set for 'Isabelle Start Command', setting to 'isabelle -I'"));
//					cmd = "isabelle -I";
//				}
//				if (cmd.indexOf("-X")>=0) {
//					String cmd_left = cmd.substring(0, cmd.indexOf("-X"));
//					String cmd_right = "";
//					cmd = cmd.substring(cmd_left.length()+2).trim();
//					cmd_left = cmd_left.trim();
//					if (cmd.indexOf(' ')>=0) {
//						cmd_right = cmd.substring(cmd.indexOf(' ')).trim();
//					} else cmd_right = "";
//					cmd = (cmd_left+" -X "+s+" "+cmd_right).trim();
//				} else {
//					cmd = cmd.trim() + " -X "+s;
//				}

				System.out.println("ChooseLogicAction: user selected new, logic, command line is '"+newCmd+"'"+
						(dialog.getSaveLogicPreference() ? "; saving as preference" : ""));

				if (dialog.getSaveLogicPreference()) {
				  ProofGeneralPlugin.getDefault().getPluginPreferences()
				    .setValue("Isabelle Start Command", newCmd);
				} else {
					ProofGeneralPlugin.getSessionManagerForProver("Isabelle").proverInfo
				   .setSpecialLaunchCommand(newCmd);
				}
				dialog = null;
				new PooledRunnable() {
					public void run() {
						try {
							RestartAction.restartSession(ProofGeneralPlugin.getSessionManagerForProver("Isabelle"),
									ChooseLogicAction.this);
//							updateStatus(STATUS_DONE_PROVER);
						} catch (IOException e) {
//							updateStatus(STATUS_FAILED);
						} catch (ScriptingException e) {
//							updateStatus(STATUS_FAILED);
						} catch (ProverRegistryException e) {
							// shouldn't happen
						}
					}
				}.start();
			} else {
//				updateStatus(STATUS_FAILED);
			}
		} catch (Exception e) {
			e.printStackTrace();
			ErrorUI.getDefault().signalWarning(
					new Exception("Could not choose logic: "+e.getMessage(), e));
//			updateStatus(STATUS_FAILED);
		}
	}

}
