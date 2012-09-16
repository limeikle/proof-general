/*
 *  $RCSfile: GotoAction.java,v $
 *
 *  Created on 21 Apr 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.actions.defunct;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.viewers.ISelection;

import ed.inf.proofgeneral.document.CmdElement;
import ed.inf.proofgeneral.document.DocElement;
import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.editor.lazyparser.Parser;
import ed.inf.proofgeneral.sessionmanager.ProverOwner;
import ed.inf.proofgeneral.sessionmanager.ScriptingException;
import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.proofgeneral.sessionmanager.events.CommandCausedErrorEvent;
import ed.inf.proofgeneral.sessionmanager.events.CommandProcessed;
import ed.inf.utils.eclipse.PGMarkerMethods;
import ed.inf.utils.process.PooledRunnable;
import ed.inf.utils.process.RunnableWithParams;

/**
 * Send or undo until we reach or pass the cursor location.
 * @author Daniel Winterstein
 */
public class GotoAction extends PGProverAction {

	/**
	 * Default constructor.
	 * Use {@link #GotoAction(ProofScriptDocument)} to set up a
	 * non-top-level instance.
	 */	public GotoAction() {
		super();
		setStatusDoneTrigger(STATUS_RUNNING | STATUS_DONE_PROVER);
		this.setId("ed.inf.proofgeneral.actions.goto");
		this.setToolTipText("Send/undo commands to reach the current location");
		this.setText("Goto");
		this.setDescription("");
	}

	/**
	 * Sets up a non-toplevel (i.e. back-end) version of this action.
	 * @param doc the document upon which this action should be performed.
	 */
	public GotoAction(ProofScriptDocument doc) {
		this();
		setTopLevel(false);
		setDocument(doc);
	}

	/** Set this to use goto action not at the top-level */
	int targetOffset = -1;

	int backDoneThreads = 0;

	/**
	 * Call this if using goto action with topLevelFlag = false.
	 * @param targetOffset The targetOffset to set.
	 */
	public void setTargetOffset(int targetOffset) {
		this.targetOffset = targetOffset;
	}

	/**
	 * @see ed.inf.proofgeneral.editor.actions.defunct.PGProverAction#runSingly()
	 */
	@Override
    public void runSingly() {
		try {
			ProofScriptDocument doc = getAndAssociateDocument();

			if (targetOffset == -1) {  //get the target offset from the editor (ie not set manually)
				if (getActiveEditor() == null) {
					throw new ScriptingException("No active editor!");
				}
				if (doc != (getActiveEditor()).getDocument()) {
					throw new ScriptingException("Action specified for "+doc.getTitle()+" but no target offset given.");
				}
				ISelection select = (getActiveEditor()).getSelectionProvider().getSelection();
				targetOffset = ((ITextSelection) select).getOffset();
			}
			final int finalTargetOffset = targetOffset;

			// da: First, check that the document is unlocked.  If it is locked, we need to retract
			// it first.  We'll exit after retracting, though, because I think the goto action
			// (at first attempt) should be approximate but fast (NEVER any forward, only backward).
			// In Emacs PG, if the user wants the forwards goto as well, she simply hits the button
			// twice.  Not a real hardship...
			if (doc.isLocked()) {
				if (finalTargetOffset<doc.getLength()) {
					// We're trying to go into the document, must retract it.

					// don't warn if this is a subclass
					if (this.getClass() != GotoAction.class || UndoAction.reallyRetract()) {
						getSessionManager().retractfile(getAndAssociateDocument().getResource().getLocationURI());
						updateStatus(STATUS_DONE_PROVER);
						return;
					}
				} else {
					// We're going to/past end, but it's already fully processed, so nothing to do.
					// Button enablers will prevent this dialog once working.
					error(new ScriptingException("Nothing to do: document already fully processed!"));
					updateStatus(STATUS_IDLE);
					return;
				}
			} else {
				// We need to do some work.  We must have the current script active.
				// FIXME da: I want to use the code below to switch to active script,
				// but it seems a bit tricky.  At the moment the active script is
				// set when the command gets queued, which is too low-level.
				// getSessionManager().setActiveScript(getDocument());
				startGotoThread(finalTargetOffset);
			}
		} catch (Exception e) {
			updateStatus(STATUS_FAILED);
			e.printStackTrace();
			error(e);
		}
	}

	/**
	 * @param finalTargetOffset
	 */
	private void startGotoThread(final int finalTargetOffset) {
		new PooledRunnable() {
			public void run() {
				int loc = finalTargetOffset;
				try {
					if (loc>getAndAssociateDocument().getLockOffset())
						goFwd(loc);
					else if (loc<=getAndAssociateDocument().getLockOffset())
						goBack(loc);
					else ; //nothing to do
					updateStatus(STATUS_DONE_PROVER);
				} catch (ScriptingException e) {
					updateStatus(STATUS_FAILED);
					e.goToError();
					error(e, false);
				} catch (ProverOwner.ProverOwnedBySomeoneElseException e) {
					updateStatus(STATUS_FAILED);
					//common on interrupts
				} catch (Exception e) {
					updateStatus(STATUS_FAILED);
					if (!Thread.currentThread().isInterrupted()) {
						e.printStackTrace();
						error(e);
					}
				} 
			}
		}.start();
	}

	// FIXME da: At first sight this looks like an unholy mess!!!!!
	// What I think it *should* do is:
	//  1) call a method in the session manager to undo/do to a given position, nothing more here.
	//  2) the method in the session manager calculates the PGIP commands necessary,
	//     and adds them to a queue to be sent to the theorem prover.
	//     We process the queue in order, we only allow items to be added to the queue
	//     when it's empty.  More flexibility than this is an added bonus but should
	//     only be enabled when the basics are working cleanly and robustly.
	//     Queue processing can be in a separate thread, but at most one!!

	void goBack(int targetLocn) throws Exception {
		final ProofScriptDocument doc = getAndAssociateDocument();

		while (exception==null && doc.getLockOffset() >= targetLocn) {

			backDoneThreads = 0;
			synchronized (this) {
				//NEEDS TO BE IN DISPLAY THREAD AS WELL AS WAITABLE THREAD ... see BUG whether undoAction might mess up
				//(shouldn't anymore since this is display thread)
				final GotoAction gA = this;
				new RunnableWithParams(null) {
					public void run() {
						try {
// method removed:							
//							getSessionManager().undoLastScriptCommand(doc, gA);
						} catch (Exception e) {
							exception = e;
						} finally {
							//this used to depend on what undoLast returned; but now even qUndo's generate messages
							synchronized (gA) {
								backDoneThreads++;
								gA.notifyAll(); // da: made notifyAll, unnecessary?
							}
						}
					}
				}.callDefaultDisplayAsyncExec();  //run that in display thread
				// FIXME: Findbugs reports UW, PS
				// da: I've removed the call
				// this.wait();  //and wait on both display thread and pgipEvent
				if (backDoneThreads<2 && exception==null) {
					//System.out.println("goBack waiting for other");
					this.wait();
				}
				//System.out.println("goBack done");
			}
			if (exception!=null) {
				throw exception;
			}
		}
		//if (doc.getLockOffset() < targetLocn - 1) {
			// perhaps we have undone a whole a proof, and now want to go back into it partway
			// da: NO!!   This shouldn't be supported by default, it may take an arbitrary
			// amount of time.
			// goFwd(targetLocn);
		//}
		//System.out.println("DONE, gone to "+targetLocn+"; at "+doc.fLockOffset);
		if (exception!=null) {
			throw exception;
		}
	}

	void goFwd(int targetLocn) throws Exception {
		try {
			final ProofScriptDocument doc = getAndAssociateDocument();
			if (doc == null) {
				throw new Exception("No document to parse!");
			}
			SessionManager sm = getSessionManager();
			if (sm == null) {
				throw new Exception("No session manager available");
			}

			// first try locking the display
			// FIXME da: should lock this up to the end of the command after
			// the target region (or last parsed position before), once that is found
			// [find doc element at end of given position].
			// Also, this should be provided by the document model, not done here in UI code.
			if (doc.getLockOffset() < targetLocn-1) {
				// Clean any error markers in the queued region.
				PGMarkerMethods.cleanErrorMarkers(doc, doc.getLockOffset()+1, targetLocn-doc.getLockOffset()-1);
// [defunct] OLD METHOD REMOVED: 				
//				doc.setQueueForwards(targetLocn-1);
			}

			// da: next try block should be extracted to a document/position method
			int startGotoOffset = doc.getProcessedOffset()+1;  //for reference, also after any whitespace
			try {
			  while (Character.isWhitespace(doc.getChar(startGotoOffset))) {
			    startGotoOffset++;
			  }
			} catch (Exception e) {}

			Parser parser = sm.getParser();
			ScriptingException parseError = null;
			if (targetLocn>doc.getParseOffset()) {
				//parse up to the target location
				try {
					parser.setCause(this);
					// da: parse until end of document again, avoiding parse errors from parsing partway into
					// commands.  Delays should be avoided by parsing in idle time.
					parser.parseDoc(doc, doc.getParseOffset()+1);
					//parser.parseDoc(doc, startParse, targetLocn - startParse);  //used to parse from processed location to end of doc, this is much faster -AH
				} catch (ScriptingException e) {
					parseError = e;
					//keep it for later
				}
			}
            // da: Yikes.  Parsing should be done elsewhere, not here.
			// This should queue a batch of commands and not be a loop.
			while (exception==null && doc.getProcessedOffset()<targetLocn && !doc.isFullyProcessed()) {
				//this should run fast (except for last command, if it's not whitespace; will again try parsing the last one)
				if (false) { // !parser.hasNextCommandFast(getAndAssociateDocument(),
						     // (Constants.SEND_COMMAND_STEPS_THROUGH_COMMENTS ? PGIPSyntax.ANYITEM : PGIPSyntax.COMMAND))) {
					int afterProcessedOffsetAndWhitespace = doc.getProcessedOffset() + 1;
					try {
					  while (Character.isWhitespace(doc.getChar(afterProcessedOffsetAndWhitespace))) {
					    afterProcessedOffsetAndWhitespace++;
					  }
					} catch (Exception e) {}
					if (parseError!=null) {
						try {
							if ((doc.getLineOfOffset(afterProcessedOffsetAndWhitespace)==
								    doc.getLineOfOffset(targetOffset))   //we're at the line where we should be
									&& (doc.getLineOfOffset(startGotoOffset)!=doc.getLineOfOffset(targetOffset)))  //and we didn't start here
							{
								break;   //we've done a multi-line goto and there's an error at the current line, don't worry about it
							}
						} catch (Exception e) {
							System.err.println("error computing line offsets...");
							//this shouldn't happen:  it means everything after the processing is whitespace,
							//and yet we still got a parse error
						}
						//either the error is before our current line, or the error is on the current line AND we clicked 'goto' on the current line
						throw parseError;
					}
					//there was no parse error, and there's no command left 'fast'
					//this means we are done my friend (all that is left should be whitespace... but let's check)

					String extra = doc.get(doc.getProcessedOffset()+1, targetLocn-(doc.getProcessedOffset()+1));
					if (extra.trim().length()==0) {
					  break;
					}
					//in this freakish situation, we will try to find the next command even though it isn't fast
					//(don't think there's any way to get here though)
					System.err.println("GotoAction has no command but is not at end of region, "+
							doc.getProcessedOffset()+"/"+targetLocn+", text is: '"+extra+"'");
				}
				final DocElement docelt = null; //parser.findNextCommand(getAndAssociateDocument(),    //TODO is the error near the command, or far away?
						//(Constants.SEND_COMMAND_STEPS_THROUGH_COMMENTS ? PGIPSyntax.ANYITEM : PGIPSyntax.COMMAND));
				TypedPosition pos = docelt.getPosition();
				final CmdElement command = DocElement.makeCommand(docelt, pos);

				Position p = command.getPosition();
				// da: this stops going beyond the target location, but I think the behaviour
				// is nicer (and matches Emacs) if we click in a command and process up to that
				// command, not the one before.
				// if (p.offset+p.length>targetLocn) break;

				//System.out.println("-- GotoCommand queuing command '"+command+"'");
				//final IAction gA = this;
				synchronized (this) {
					sm.queueCommand(command, this);
//					currentCommandNum++;  //for interest's sake... don't actually need this info for going backwards
					// FIXME da: FindBugs reports unconditional wait here: what are we waiting on?
					this.wait();  //get notification from pgipEvent
//				if (error!=null) throw error;  //don't display on error
					//we no longer care about the display locking, it takes care of itself
//				new RunnableWithParams(null) {
//					public void run() {
//						//update display once command is sent
//						try {
//							getDocument().commandSent(command);
//							//want to wait on this? YES, wait on this  TODO could be nicer/faster not to wait on this
//						} catch (Exception e) {
//							error = e;
//						} finally {
//							synchronized (gA) {
//								gA.notify();
//							}
//						}
//					}
//			  }.callDefaultDisplayAsyncExec();
//			  this.wait();  //get notification from display update
				}
				if (exception!=null) {
					if (exception instanceof ScriptingException) {
						ScriptingException sexp = (ScriptingException) exception;
						sexp.errorDoc = doc;
						sexp.errorOffset = p.offset;
					}
					throw exception;
				}
				if (p.offset+p.length == targetLocn) {
					break;  //don't parse one more time if we're at the end (and the display thread hasn't caught up)
				}
			}
			if (exception!=null) {
				throw exception;
			}
		} catch (Parser.ParsingInterruptedException e) {
			updateStatus(STATUS_FAILED);
			//not a problem
		} catch (Exception e) {
			//on error, clear the lock field back to the amount processed
			//TODO really this should be the end of the last commnad we successfully processed above
			//but this shortcut will work *most* of the time
			throw e;
		} finally {
			//resset the lock offset, in case it went too far, or if there was an error
			if (associatedDocument!=null && !associatedDocument.isLocked()) {
				associatedDocument.removeQueueRegion();
			}
		}
	}

	@Override
    public void pgipEventCausedError(CommandCausedErrorEvent event) {
		exception = new ScriptingException(event.fError);
		//System.out.println("finished event (error)");
		//this.notify();   //shouldn't notify on error; it gets its notification from cmd processed
	}

	@Override
    public synchronized void pgipEventCommandProcessed(CommandProcessed event) {
		//System.out.println("finished event (processed)");
		backDoneThreads++;  //has no effect if we're going forward
		this.notifyAll(); // da: made notifyAll, unnecessary?
	}

	/**
	 * Use to programmatically goto just before an element
	 * (if the element is a command, then it should be unlocked,
	 * but the previous command will be locked).
	 * @param offset the offset to jump to
	 */
	public void gotoOffset(int offset) {
		System.err.println("GotoAction.gotoOffset -- command will probably not work yet");
		topLevelFlag = false;
		targetOffset = offset;
		run();
		//needs to clear targetOffset
	}

	//** have to reset the targetOffset when this finishes
	@Override
    public void onRunFinished(int finalStatus) {
		targetOffset = -1;  // clear this when it has finished
		super.onRunFinished(finalStatus);
	}
}
