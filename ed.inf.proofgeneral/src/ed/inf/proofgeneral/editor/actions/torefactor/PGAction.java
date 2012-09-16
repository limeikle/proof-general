/*
 *  $RCSfile: PGAction.java,v $
 *
 *  Created on 21 Apr 2004
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.actions.torefactor;

import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.editor.ProofScriptEditor;
import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.proofgeneral.sessionmanager.events.IPGIPListener;
import ed.inf.proofgeneral.sessionmanager.events.PGIPEvent;
import ed.inf.utils.datastruct.ToStringComparator;
import ed.inf.utils.exception.KnownException;

/**
 * Has an active editor, a session manager (sm) and a document (doc).
 * reacts to changes in editor or active script.
 * <p/>
 * Many of the actions don't need the full EclipseAction class;
 * just an implementation of IWindow..Action..Delegate.
 * In particular the plugin-based actionset actions don't get
 * this as a handler -- they have a WWinPluginAction (so setEnabled doesn't work)!
 * <p/>
 * event handler model is to override handleOurPgipEvent to deal with
 * messages caused by this (or all msgs if handleOnlyOurEvents is false),
 * or override pgipEvent() for more sophisticated behaviour.
 */
public class PGAction extends EclipseActionNew implements IPGIPListener {

	@Override
    public void setEditor(ITextEditor editor) {
		super.setEditor(editor);
		if (editor instanceof ProofScriptEditor) {
			this.setEnabled();
		} else {
			this.setEnabled(false);
		}
	}
	
	
	
	/**
	 * Create a new PG action which is not associated with any editor.
	 *
	 */
	// TODO: calling editor methods on this should be an error!!  We
	// may make separate classes instead.
	public PGAction() {
		super(null);  
	}

	/**
	 * Create a new retargettable action associated with the given editor.
	 * @param editor
	 */
	public PGAction(ITextEditor editor) {
		super(editor); 
	}

	/**
	 * 
     * @return the current active editor, or null if not a proof script editor.
  	 */
	protected ProofScriptEditor getActiveEditor() {
		IEditorPart ed = super.getTextEditor();
		if (ed instanceof ProofScriptEditor) {
			return (ProofScriptEditor) ed;
		}
		return null;
	}

	private SessionManager sm = null;

	/**
	 * The current session manager follows that of the active associatedEditor, if any.
	 * @return the current session manager
	 */
	public SessionManager getSessionManager() {
		if (getActiveEditor()==null) {
			if (sm!=null) {
				return sm;
			}
			return ProofGeneralPlugin.getSomeSessionManager();
		}
		SessionManager nsm = (getActiveEditor()).getSessionManager();
		setSessionManager(nsm);
		return sm;
	}

	public void setSessionManager(SessionManager sm) {
		if (this.sm==sm) {
			return;
		}
		//	    if (this.sm != null) {
		//	        this.sm.removeListener(this);
		//	        if (this.sm.proverState!=null) this.sm.proverState.removeListener(this);
		//	    }
		this.sm = sm;
		//	    sm.addListener(this);  Listen through ActionBarContributor instead
		//        sm.proverState.addListener(this);
	}
	/**
	 * This should <b>not</b> be set if the action wants to stay up-to-date.
	 * Setters must clear (though for PGProverAction it is cleared and set automatically).
	 */
	ProofScriptDocument associatedDocument = null;

	/** whether the default pgipEvent event code ignores events with an empty or different cause field
	 *  (apart from prover state / action bar events which everyone listens to), true by default */  //-AH
	public boolean onlyHandleEventsCausedByThis = true;

	/**
	 * The ActionBarContributor forwards various events to the PGActions.
	 * React to active script change events.
	 @see ed.inf.proofgeneral.sessionmanager.events.IPGIPListener#pgipEvent(PGIPEvent)
	 */
	public void pgipEvent(PGIPEvent event) {
		boolean doneSomething = false;
		if (!onlyHandleEventsCausedByThis || event.cause==this) {
			doneSomething = handleOurPgipEvent(event);
		}
// da: event refactoring: removed PGIPShutdown, so this (defunct) class is broken now.		
//		if (event instanceof PGIPShutdown) {
			// da: changed this to invoke even if done something above,
			// we should always reenable all actions after restart
//			doneSomething = doneSomething | handleProverShutdown(event);
//		}
// URGENT FIXME: temporarily disable this to avoid use of getSource.
//		if (!doneSomething) {
//			//nearly all events should listen for state change events [this is expensive but mostly necessary]
//			// TODO handle enabling correctly
//			if (event instanceof InternalEvent.ProverClear ||
//					event.getSource() instanceof ProofScriptEditorActionContributor
//					|| event instanceof ActiveScriptChangedEvent
//					|| event instanceof ProverState.ProverStateChangeEvent) {
//				setEnabled();
//			}
//		}
	}

	//by default just updates the enabled flag
	public boolean handleProverShutdown(PGIPEvent event) {
		//  da: testing here: often we see "still enabled with status 1" after 
		// restart which is really frustrating and a serious problem (even if
		// it is due to bugs elsewhere).  Try force enabling, since we don't
		// disable at the moment...
		forceSetEnabled(true); // setEnabled(); 
		return true;
	}

	public boolean handleOurPgipEvent(PGIPEvent event) {
		return false;
	}


	/**
	 * Fetch the document associated with this action.  On the first call, we make a record of the active
	 * associatedEditor and document, if an instance of ProofScriptEditor on a ProofScriptDocument.
	 * On subsequent calls, we return the previously associated document, which will
	 * remain the same until it is cleared.
	 * Guarantees non-null result
	 */
	public ProofScriptDocument getAndAssociateDocument() throws KnownException {
		if (associatedDocument==null) {
			IEditorPart editor = getActiveEditor();
			if (editor instanceof ProofScriptEditor) {
				associatedEditor = (ProofScriptEditor)editor;
				associatedDocument = associatedEditor.getDocument();
			}
			if (associatedDocument==null) {
				throw new KnownException("Cannot find associated proof script for action!");
			}
		}
		return associatedDocument;
	}

	// da: added this to ensure we get the right editor in actions.  The active
	// editor may well change in the meantime.  The ActionBarContributor is supposed
	// to track that.  TODO: sort this out, move it out of plugin class.
	/** This is the editor associated with the document retrieved by getDocument(). */
	private ProofScriptEditor associatedEditor;

	/**
	 * Return the editor which was active on the document at the time this action
	 * was initiated, if any.
	 * @return the editor which was associated with the document retrieved by
	 * getDocument()
	 */
	public ProofScriptEditor getAssociatedEditor() {
		return associatedEditor;
	}

	//---- ENABLED / DISABLED -------------------------------------

	/** what level the prover has to be at for this command to be enabled */
	public int requiredProverStatusLevel = IGNORE_PROVER;

	/** wait for prover to be clear and all events discharged --
	 *  this isn't as useful as it seems because the check is usually done by an
	 *  event which is still firing... */
	public final static int PROVER_NO_EVENTS = 5;
	/** wait for prover to be clear and no owner set */
	public final static int PROVER_AVAILABLE = 4;
	/** wait for prover to be clear (but events are okay) */
	public final static int PROVER_NO_QUEUE = 3;
	/** wait for prover to be not busy (but queued commands or events are okay) */
	public final static int PROVER_NOT_BUSY = 2;
	/** prover must be alive, but may be busy, queued, or with events */
	public final static int PROVER_ALIVE = 1;
	/** command can run even if prover is dead */
	public final static int IGNORE_PROVER = 0;

	/** what state the script has to be at for this command to be enabled (active, inactive, or null) */
	public int requiredScriptStatusLevel = IGNORE_SCRIPT;

	/** command needs the current script to be active in the prover (and not locked) */
	public final static int CURRENT_SCRIPT_ACTIVE = 4;
	/** command needs the current script to be active or available in the prover (possibly locked) */
	public final static int CURRENT_SCRIPT_ACTIVE_MAYBE_LOCKED = 3;
	/** command needs a script active in the prover, but it doesn't have to be the currently visible one */
	public final static int ANY_SCRIPT_ACTIVE = 1;
	/** command can run even if there is no script */
	public final static int IGNORE_SCRIPT = 0;

	/** sets this action to be enabled based on the result of decideEnabled */
	public void setEnabled() {
		setEnabled(decideEnabled());
	}

	/** sets this enabled status if it is different to the current */
	@Override
    public void setEnabled(boolean b) {
		if (isEnabled() != b) {
			forceSetEnabled(b);
		}
	}

	/**
	 * Sets the status of this and all calling actions, regardless of current status.
	 */
	public void forceSetEnabled(boolean b) {
		super.setEnabled(b);
		//also set this in all actions who have called us
		// TODO this only gets set when actions call us... would be better at init time, but oh well
		if (callingActions != null) {
			for (IAction a : callingActions) {
				if (a != this) {
					a.setEnabled(b);
				}
				// System.out.println(getClass().getName()+" setting "+a.getId()+" "+b);
			}
		}
	}

	/**
	 * Tests whether this action should be enabled.
     * TODO: implement this in a more precise way.  Currently always returns true.
	 * @return true if it should be enabled (but might want some checking if overridden),
	 * 			false otherwise (this is definitive)
     */
	protected boolean decideEnabled() {
		return true;
		/* // FIXME: This seems to be unresponsive and slightly buggy, giving both false +ives and -ives - Dan
        try {
    //		System.out.println("decideEnabled "+getClass().getName());
    		sm = getSessionManager();
    		if (sm==null) return false;  //usually because we are shutting down
    		switch (requiredProverStatusLevel) {
    		  case PROVER_NO_EVENTS:
    		  	if (!sm.isEmptyQueueAndEvents() || sm.proverState.isBusy() || !sm.proverState.isAlive()) return false;
    		  	break;
    		  case PROVER_AVAILABLE:
    		  	if (sm.isOwned() || !sm.isEmptyQueue() || sm.proverState.isBusy() || !sm.proverState.isAlive()) return false;
    		  	break;
    		  case PROVER_NO_QUEUE:
    		  	if (!sm.isEmptyQueue() || sm.proverState.isBusy() || !sm.proverState.isAlive()) return false;
    		  	break;
    		  case PROVER_NOT_BUSY:
    		  	if (sm.proverState.isBusy() || !sm.proverState.isAlive()) return false;
    		  	break;
    		  case PROVER_ALIVE:
    		  	if (!sm.proverState.isAlive()) return false;
    		  	break;
    		  case IGNORE_PROVER:
    		  	break;
    		}
    		switch (requiredScriptStatusLevel) {
    		  case CURRENT_SCRIPT_ACTIVE:
    		  	if (getDocument()==null || //getActiveEditor()==null ||    //may want this check if document is explicitly set on the action ?
    		  			getDocument().isLocked() ||
    		  					((getDocument() != sm.getActiveScript()) && (!sm.canChangeScript()))) return false;
    		  	break;
    		  case CURRENT_SCRIPT_ACTIVE_MAYBE_LOCKED:
    		  	if (getDocument()==null || //getActiveEditor()==null ||    //may want this check if document is explicitly set on the action ?
    		  			((getDocument() != sm.getActiveScript()) && (!sm.canChangeScript()))) return false;
    		  	break;
    		  case ANY_SCRIPT_ACTIVE:
    		  	if (sm.getActiveScript()==null && getDocument()==null)   //if doc is non-null, active script will get set
    		  			return false;
    		  	break;
    		  case IGNORE_SCRIPT:
    		  	break;
    		}
        } catch (Exception x) {
            if (ProofGeneralPlugin.debug(this)) x.printStackTrace();
        }
		return true;
		*/
	}

	//---------------------------------------------------

	/**
	 * @param doc The doc to set. Warning: This will prob. prevent the action from updating!
	 */
	public void setDocument(ProofScriptDocument doc) {
		this.associatedDocument = doc;
	}

	/** HACK to get the IAction corresponding to this action, so we can set its enabled state */
	public Set<IAction> callingActions = new TreeSet<IAction>(new ToStringComparator<IAction>());

	/**
	 * Runs the action, and also records the proxy action which called it.
	 * @see ed.inf.proofgeneral.editor.actions.defunct.EclipseAction#run(org.eclipse.jface.action.IAction)
	 */
	@Override
    public void run(IAction action) {
		if (!callingActions.contains(action)) {
			callingActions.add(action);
			if (action != this) {
				action.setEnabled(decideEnabled());
			}
		}
		super.run(action);
	}

	//attempts to get the IAction before run() is invoked:

//System.err.println("init goto action ("+getId()+")");
////we can get the action sets, but not the action for this
////((WorkbenchPage)(((WorkbenchWindow)window).getPages()[0])).getActionSets()
////((WorkbenchPage)window.getActivePage()).getActivePerspective().showActionSet("");
//try {
//	ActionSetRegistry r = WorkbenchPlugin.getDefault().getActionSetRegistry();
//	IActionSetDescriptor as[] = null;  //r.getActionSetsFor("ed.inf.proofgeneral.action.contribution.set");
//	if (as==null || as.length==0) {
//		as = r.getActionSets();
//	}
//	int i=0;
//  IAction action[] = ((PluginActionSet)as[i]).getPluginActions();
//  System.out.println("got action: "+action[i].getId());
//  action[i].setEnabled(false);
//} catch (Exception e) {
//	System.err.println(e);
//}

//((WorkbenchWindow)window).getActionPresentation().getActionSets()  ... but getAP is protected
//Workbench.getInstance().getCommandSupport()  //has a private map to handler...

//WorkbenchPlugin.getDefault().get

//ActionBarContributor.getDefault().getActionBars().setGlobalActionHandler(
////	getViewSite().getActionBars().setGlobalActionHandler(
//		"ed.inf.proofgeneral.actions.goto", this);
//ActionBarContributor.getDefault().getActionBars().setGlobalActionHandler(
//    "ed.inf.proofgeneral.action.contribution.set.ed.inf.proofgeneral.actions.goto", this);
//ActionBarContributor.getDefault().getActionBars().updateActionBars();
//setEnabled();

}
