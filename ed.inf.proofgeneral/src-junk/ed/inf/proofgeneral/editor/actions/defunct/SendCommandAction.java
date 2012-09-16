/*
 *  $RCSfile: SendCommandAction.java,v $
 *
 *  Created on 21 Apr 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.actions.defunct;

import org.dom4j.Element;
import org.eclipse.jface.text.TypedPosition;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.CmdElement;
import ed.inf.proofgeneral.document.DocElement;
import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.editor.lazyparser.Parser;
import ed.inf.proofgeneral.sessionmanager.ScriptingException;
import ed.inf.proofgeneral.sessionmanager.events.CommandCausedErrorEvent;
import ed.inf.proofgeneral.sessionmanager.events.CommandProcessed;
import ed.inf.utils.exception.KnownException;
import ed.inf.utils.process.PooledRunnable;

// TODO: rename as NextCommandAction.
// Standard button names: Next, Undo, RetractAll, ProcessAll

/**
 * Send the next command in the script out to the prover.
 * @author Daniel Winterstein
 */
// da: This action is a bit different from the goto action at the moment,
// because it will parse the next step singly and quickly if possible.
// We might unify them.
public class SendCommandAction extends PGProverAction {

	public static final String noCommandFoundException = "No more commands to send.";
	// public static String hitGotoPointException = "Stopped: ran into the goto point limit.";
	// public DocElement lastCommandSent = null;

	/** Stores success of last command */
	private Boolean succeeded = null;

	/**
	 * Create a send command action. You may need to subsequently call/set:
	 * <ul>
	 * 	<li>setActiveEditor</li>
	 *  <li>setBlocking</li>
	 *  <li>topLevelFlag</li>
	 * </ul>
	 */
	public SendCommandAction() {
		setStatusDoneTrigger(STATUS_RUNNING | STATUS_DONE_DISPLAY | STATUS_DONE_PROVER);
		this.setToolTipText("Send the next command to the theorem prover");
		this.setText("Send Command");
		this.setDescription("Send Command Description");
	}

	/**
	 * GotoAction can set this to block SendCommand from going too far.
	 * In non-top-level mode, SendCommand will not send any command that would lock the gotoPoint.
	 */
	// public int gotoPoint = -1;

	@Override
    public void runSingly() {
		succeeded = null;
		try {
			ProofScriptDocument doc = getAndAssociateDocument();
			if (doc != null) {
				// Must be in the active script: button enabler should prevent this failing
				// FIXME da: I want to use the code below to switch to active script,
				// but it seems a bit tricky.  At the moment the active script is
				// set when the command gets queued, which is too low-level.
				// getSessionManager().setActiveScript(getDocument());

				// da: this next thing *should* work, but the trouble is that we need to block.
				// getSessionManager().setActiveScript(getDocument());
				Parser parser = getSessionManager().getParser();
// da: commented this out so we can clean up Parser				
//				if (!blocking && !parser.hasNextCommandFast(doc,
//						(Constants.SEND_COMMAND_STEPS_THROUGH_COMMENTS ? PGIPSyntax.ANYITEM : PGIPSyntax.COMMAND))) {
//					BgParse bgParse = new BgParse(parser,this,Thread.currentThread());
//					//@todo sometimes hangs here on "GOTO"  ?? -AH  (maybe fixed now?)
//					//System.err.println("starting background parser thread for send-command action");
//					bgParse.start();
//				} else {
//					DocElement docelt = null;
//					assert docelt !=null :  "parser said it had a command fast but it didn't";
//					TypedPosition pos = docelt.getPosition();
//					CmdElement command = DocElement.makeCommand(docelt, pos);
//					sendCommand(command);
//					getAssociatedEditor().scrollToQueuedOffset();
//				}
			} else {
				throw new KnownException("No active editor!");
			}
//		} catch (ScriptingException ex) {
//			updateStatus(STATUS_FAILED);
//			error(ex, true);  // show dialog
//			// da: this isn't a prover error, but one of ours. Best not to dump it
//			// in the "Prover Output" window, then.
//			// ((LatestOutputView)(LatestOutputView.getDefault())).updateWithError(ex.getMessage());
		} catch (Exception ex) {
			updateStatus(STATUS_FAILED);
			System.err.print("SendCommandAction got an error:  ");
			ex.printStackTrace();
			error(ex);
		}
	}

	/**
	 * Sends a command to the prover queue.
	 * @param command the command to send.
	 * @throws ScriptingException if the command is unll
	 */
	void sendCommand(CmdElement command) throws ScriptingException,KnownException {
		if (command == null) {
			throw new KnownException(noCommandFoundException);
		}

		getSessionManager().queueCommand(command,	this);
		getAndAssociateDocument().commandSent(command);
		updateStatus(STATUS_DONE_DISPLAY);
	}

	/**
	 * Raises an error message about the command which caused an error.
	 * @see ed.inf.proofgeneral.editor.actions.defunct.PGProverAction#pgipEventCausedError(ed.inf.proofgeneral.sessionmanager.events.CommandCausedErrorEvent)
	 */
	@Override
    public void pgipEventCausedError(CommandCausedErrorEvent event) {
		super.pgipEventCausedError(event);
		succeeded = Boolean.FALSE;
		Object data = event.parseTree;
		if (event.userNotified) {
			return;
		}
		if (data !=null) {
//			String emsg = Methods.lineEnd("Could not execute command");
//			emsg += Methods.lineEnd("   "+lastCommandSent.getText());
//			emsg += Methods.lineEnd("Error Message: "+event.fError);
			error(new CommandFailedException(((Element)data).getStringValue(), event.fError), false);
			//already updated in views
			event.userNotified = true;
			//EclipseMethods.errorDialog(new Exception(emsg));
		} else {
			System.err.println("SendCommand had error with no parse tree set: "+event);
		}
	}

	/**
	 * Flags that the command was completed sucessfully.
	 * @see ed.inf.proofgeneral.editor.actions.defunct.PGProverAction#pgipEventCommandProcessed(ed.inf.proofgeneral.sessionmanager.events.CommandProcessed)
	 */
	@Override
    public void pgipEventCommandProcessed(CommandProcessed event) {
		if (succeeded==null) {
			succeeded = Boolean.TRUE;
		}
		//System.err.println(General.makeDateString()+"  finished command on "+SendCommandAction.this);
		super.pgipEventCommandProcessed(event);
	}

	/**
	 * Checks if the last command completed successfully.
	 * @return true if the last command completed successfully.  false if it did not, or if no command has been sent.
	 */
	public boolean wasSuccessful() {
		return (succeeded!=null && succeeded.booleanValue());
	}


	class BgParse extends PooledRunnable {
		Parser p;
		SendCommandAction a;
		Thread t;
		BgParse(Parser p,SendCommandAction a, Thread t) {
			super();
			this.p=p;
			this.a=a;
			this.t=t;
		}
		public void run() {
			try {
				DocElement docelt = null; //p.findNextCommand(getAndAssociateDocument(),
					//	(Constants.SEND_COMMAND_STEPS_THROUGH_COMMENTS ? PGIPSyntax.ANYITEM : PGIPSyntax.COMMAND));
				TypedPosition pos = docelt.getPosition();
				CmdElement command = DocElement.makeCommand(docelt, pos);
				a.sendCommand(command);
//			} catch (ParsingInterruptedException e) { // not a problem
//				updateStatus(STATUS_FAILED);
			} catch (ScriptingException ex) { // a minor problem
				updateStatus(STATUS_FAILED);
				error(ex, false);   //don't show dialog
			} catch (KnownException ex) { // a "routine" error (but not desirable?)
				updateStatus(STATUS_FAILED);
				System.err.print("SendCommandAction failed:  "+ex.getMessage());
				if (ProofGeneralPlugin.debug(this)) {
					ex.printStackTrace();
					error(ex, true);
				} else {
					error(ex, false);
				}
			} catch (Exception ex) { // an unexpected (probably programming) error
				updateStatus(STATUS_FAILED);
				System.err.print("Unexpected error during SendCommandAction:  ");
				ex.printStackTrace();
				error(ex);
			}
		}
	}
}
