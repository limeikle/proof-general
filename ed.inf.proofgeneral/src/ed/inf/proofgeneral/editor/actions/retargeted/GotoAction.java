/*
 *  $RCSfile: GotoAction.java,v $
 *
 *  Created on 21 Apr 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.actions.retargeted;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.swt.widgets.Display;

import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.editor.ProofScriptEditor;
import ed.inf.proofgeneral.editor.lazyparser.Parser.ParsingInterruptedException;
import ed.inf.proofgeneral.sessionmanager.ScriptingException;
import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.utils.eclipse.EclipseMethods;

/**
 * Send commands from the document or undo commands until we reach a given
 * target location.  
 * 
 * @author Daniel Winterstein
 * @author David Aspinall
 */
public class GotoAction extends PGRetargetableAction implements IJobChangeListener {

	/** Set this to use a derived goto action */
	private IDocumentTarget target;
	
	/** To allow completion information to be passed back */
	private ProofScriptEditor lastEditor;
	
	@Override
    public void clearBusy() {
		super.clearBusy();
		this.targetOffset = ProofScriptEditor.NOTARGET;
	}

	public interface IDocumentTarget {
		/**
		 * @param doc
		 * @return the target offset
		 */
		abstract int getTargetOffset(ProofScriptDocument doc);
	}
	
	/**
	 * Constructor for action to go to a particular given target.
	 * @param editor the editor to configure action for, initially
	 * @param target the target setting method for this goto action
	 */	
	public GotoAction(ProofScriptEditor editor, IDocumentTarget target) {
		super(editor);
		this.target = target;
	}

	/**
	 * Constructor for action to go to editor's caret position as target.
	 * 	@param editor the editor to configure action for, initially
	 */
	public GotoAction(ProofScriptEditor editor) {
		super(editor);
	}

	/**
	 * Constructor for non-UI/ad-hoc on-off actions
	 * @param doc the target document
	 * @param target the target-setting method for this action
	 */
	public GotoAction(ProofScriptDocument doc, SessionManager sm, IDocumentTarget target) {
		super(doc);
		setSpecifiedSessionManager(sm);
		this.target = target;
	}
	
	/**
	 * Destination offset.  This is the target position for the value of getProcessedOffset()
	 * from the document.
	 * {@link ProofScriptEditor#NOTARGET} indicates the offset is taken from the caret position of the active editor. 
	 */
	private int targetOffset = ProofScriptEditor.NOTARGET;
	
	/**
	 * Call this to set a target offset for this action.
	 * @param targetOffset The targetOffset to set.
	 */
	public void setTargetOffset(int targetOffset) {
		this.targetOffset = targetOffset;
	}

	@Override
    public void run() {
		if (super.isBusy()) {
			return;
		}
		super.setBusy();
		try {
			ProofScriptDocument doc = getDocumentForRunningAction();
			lastEditor = getEditorForRunningAction(); // HACK: but it's OK that this could change in race-condition
			SessionManager sm = getSessionManagerForRunningAction();

			if (target != null) {
				targetOffset = target.getTargetOffset(doc);
			} else if (targetOffset == ProofScriptEditor.NOTARGET && lastEditor != null) {
				int selectionOffset = getEditorForRunningAction().getSelectionOffset();
				if (selectionOffset != ProofScriptEditor.NOTARGET) {
					targetOffset = selectionOffset;
				}
			}
			
			if (targetOffset == ProofScriptEditor.NOTARGET) {
				throw new ScriptingException("GotoAction: no editor (or unset target)");
			}
			final int finalTargetOffset = targetOffset;

			// da: First, check that the document is unlocked.  If it is locked, we need to retract
			// it first.  We'll exit after retracting, though, because I think the goto action
			// (at first attempt) should be approximate but fast (NEVER any forward, only backward).
			// In Emacs PG, if the user wants the forwards goto as well, she simply hits the button
			// twice.  Not a real hardship...
			if (doc.isLocked()) {
				if (finalTargetOffset<doc.getLength()) {
					if (this.getClass() != UndoAllAction.class || reallyRetract()) {
						sm.retractfile(doc.getResource().getLocationURI());
					}
				} else {
					// We're going to/past end, but it's already fully processed, so nothing to do.
					// Button enablers will prevent this dialog once working.
					error(new ScriptingException("Nothing to do: document already fully processed!"));
				}
				clearBusy();
				return;
			}

			if (finalTargetOffset == doc.getLockOffset()) {
				clearBusy(); // we're there already, do nothing.
				return;
			}

			startGotoThread(finalTargetOffset);  // thread must do clearBusy()

		} catch (Exception e) {
			clearBusy();
			e.printStackTrace();
			error(e);
		}
	}

	/**
	 * Starts a Goto thread which performs whatever actions are required to move the offset to loc.
	 * @param loc the target goto offset
	 */
	private void startGotoThread(final int loc) {
		final ProofScriptDocument doc = getDocumentForRunningAction();
		final SessionManager sm = getSessionManagerForRunningAction();

		Job gotoJob = new Job("Goto Thread") {

			/**
			 * Runs the Thread.
			 * @return always OK; the final cursor offset is returned as the event message.
			 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
			 */@Override
			protected IStatus run(IProgressMonitor monitor) {
				 boolean forward = true;
				// We need to do some work.  We must have the current script active.
				try {
					if (!sm.isActiveScript(doc)) {
						sm.setActiveScript(doc);
					}
					if (loc>doc.getLockOffset()) {
						goFwd(doc,loc);
					}
					else if (loc<doc.getLockOffset()) {
						goBack(doc,loc);
						forward = false;
					}
				} catch (ScriptingException e) {
					e.goToError();
					error(e, e.isShowInDialog());
				} catch (Exception e) {
					if (!Thread.currentThread().isInterrupted()) {
						e.printStackTrace();
						error(e);
					}
				} finally {
					clearBusy();
				}
				
				int offset = ( forward?doc.getLockOffset():doc.getProcessedOffset() );
				//TODO we should use a subclass of Status that records the new offset
				return new Status(Status.OK, Status.OK_STATUS.getPlugin(), String.valueOf(offset+1));
			 }
		};
		gotoJob.addJobChangeListener(this);
		gotoJob.schedule();
	}
	
	
	
	/**
	 * Move forward in the document, first parsing if necessary, then queueing up
	 * commands to be processed.  
	 * @param doc
	 * @param loc the position to send to, exclusive
	 *   (i.e. the char at this location will not be sent if it is the first non-whitespace char in a command)
	 * @throws ScriptingException
	 * @throws BadLocationException
	 * @throws ParsingInterruptedException
	 */
	private void goFwd(ProofScriptDocument doc, int loc) throws ScriptingException, BadLocationException, ParsingInterruptedException {
		final SessionManager sm = getSessionManagerForRunningAction();
		int parse = doc.getParseOffset();
		//back up one char, since the loc char is _included_ in what is sent
		//we don't want to send a command if curpos is before it
		
		if (parse < loc && doc.skipSpacesForward(parse+1)-1<doc.getLength()) {
			// parse at least as far as given location
			sm.getParser().parseDoc(doc, parse+1, loc-parse);
		}
		int lock = doc.getLockOffset();
		if (lock < loc && doc.skipSpacesForward(lock+1)<doc.getLength()) {
			sm.queueCommandsToProcess(doc, loc); 
		}
	}
	
	/**
	 * Move backward in the document, queueing up undo commands.
	 * The char at the indicated location will NOT be processed when this is finished. 
	 * @param doc
	 * @param loc
	 * @throws ScriptingException
	 */
	private void goBack(ProofScriptDocument doc, int loc) throws ScriptingException {
		final SessionManager sm = getSessionManagerForRunningAction();
		sm.queueCommandsToUndo(doc, loc);
	}

	/** For use in {@link #reallyRetract()} */
	private static final String[] opts = new String[] {"Retract", "Cancel"};
	

	/**
	 * Prompts the user to confirm that they really want to retract the whole script
	 * (for use when the 'retract' option has not explicitly been called).
	 * @return true if the user agrees; false otherwise.
	 */
	private static boolean reallyRetract() {
		String answer = EclipseMethods.messageDialog("Really retract?", "Because this script has been completed," +
				" this operation will retract the whole file (and any files which depend upon it).\n\nAre " +
				"you sure you wish to retract this script?", opts);
		        // FIXME da: add toggle for this dialog (seems to create problems):
				// true,MessageDialog.QUESTION);
		return (answer.equals(opts[0]));
	}

	protected IStatus lastJobResult = null;
	
	/** used if command is externally called */
	public IStatus getLastJobResult() {
		return lastJobResult;
	}
	
	/**
	 * Updates the Editor once the goto thread has completed.
	 * @param event the 'done' event; this is expected to contain the target offset in the event message.
     * @see org.eclipse.core.runtime.jobs.IJobChangeListener#done(org.eclipse.core.runtime.jobs.IJobChangeEvent)
     */
    public void done(IJobChangeEvent event) {
    	final int offset = Integer.parseInt(event.getResult().getMessage());
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
        		ProofScriptEditor pse = lastEditor;
        		if (pse != null) {
        			pse.setSelectionOffset( offset );
        		}
            }
        });
    }

    /** Unused Goto Thread listener */
    public void aboutToRun(IJobChangeEvent event) {}
    /** Unused Goto Thread listener */
    public void awake(IJobChangeEvent event) {}
    /** Unused Goto Thread listener */
    public void running(IJobChangeEvent event) {}
    /** Unused Goto Thread listener */
    public void scheduled(IJobChangeEvent event) {}
    /** Unused Goto Thread listener */
    public void sleeping(IJobChangeEvent event) {}

}
