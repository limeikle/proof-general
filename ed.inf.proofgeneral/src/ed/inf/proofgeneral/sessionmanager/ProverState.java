/*
 *  $RCSfile: ProverState.java,v $
 *
 *  Created on 04 Aug 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.sessionmanager;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;

import org.dom4j.Element;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.CmdElement;
import ed.inf.proofgeneral.document.DocElement;
import ed.inf.proofgeneral.document.ProofScriptDocumentException;
import ed.inf.proofgeneral.pgip.PGIPMessages;
import ed.inf.proofgeneral.pgip2html.Converter;
import ed.inf.proofgeneral.sessionmanager.events.CommandCausedErrorEvent;
import ed.inf.proofgeneral.sessionmanager.events.CommandSucceeded;
import ed.inf.proofgeneral.sessionmanager.events.PGIPEvent;
import ed.inf.proofgeneral.sessionmanager.events.PGIPIncoming;
import ed.inf.proofgeneral.sessionmanager.events.PGIPOutgoing;
import ed.inf.proofgeneral.sessionmanager.events.PGIPReady;
import ed.inf.proofgeneral.sessionmanager.events.UndoSent;

// TODO: this needs refinement, and it doesn't match the PGIP model properly
// (which has four states).  Unless the correct commands are sent to Isabelle
// on file-open and file-close commands, the multiple file scripting will
// be buggy.
// da: I'm doing some work on this at the moment.  The PGIP/prover state is
// partly managed in the SessionManager at the moment but may be moved
// back here soon.

/**
 * <b>API</b>
 *
 The ProverState object tracks the current state of the prover we're talking to.
 This class contains modelEvent methods (called by the SessionManager) which
 attempt to model the theorem prover.  Other objects can listen to a prover
 state by registering as an Observer: the Observer.update method will be called
 (with null parameter) for any state change.

 This follows the abstract prover model defined by PGIP with several state-levels
 (ie. top level, file open, theory level, proof level).

 
 @author Daniel Winterstein
 @author David Aspinall
 */

/*
 * To understand the motivations of the PGIP state model you should read the
 * PGIP commentary and the UITP papers by Aspinall/Luth. Also please read the
 * notes on the wiki and help improve them:
 *
 * http://proofgeneral.inf.ed.ac.uk/wiki/Main/PGIPBasicProverModel
 *
 * To clarify for anyone confused by the PGIP design: the state model is based on
 * the current behaviour of the existing Proof General which is a common
 * abstraction of several provers.  Then we made some simplifications. It's
 * possible that some further simplifications could be made by forcing uniform
 * behaviour across the different levels (and overloading the same element
 * names), but the history recording mechanisms (i.e. discarding history for
 * completed proofs) are quite fundamental and implemented in many systems in
 * the same way.
 *
 */

// TODO da: we can implement ProverGuise in PGIP package as a type to use here.
// TODO da: I like the idea of queuing changes waiting to be confirmed/rolledback,
//  but I wonder if we really need to be that complicated.  Couldn't we just
//  commit the changes as and when they come from the prover?   Maybe not,
//  this mechanism does allow proverknowledge to switch context before the context
//  has actually changed in the prover, which is nifty.  Then it could use
//  cached info from a previous run (e.g. on opentheory which causes big
//  delay).

public class ProverState extends Observable {

    /**
     * The state of the prover: TOP, FILE, THEORY or PROOF.
     * These levels are specified by the PGIP model.
     */
	public enum State {
		TOP_LEVEL ("top"),
		FILE_LEVEL ("file"),
		THEORY_LEVEL ("theory"),
		PROOF_LEVEL ("proof");

		/** friendly name for the state */
		private String name;
		/** internal use only */
		State(String name) {
			this.name = name;
		}
		/** returns the friendly name for this state */
		@Override
        public String toString() {
			return name;
		}
	}

	/*
	 * TODO GD --	would it make sense to keep a mirror of the prover's active
	 * 				file alongside the other model information?
	 * I'm thinking something along the lines of:
	 * IResource activeFile					// file currently within the jaws of the prover
	 * Set&lt;IResource&gt; processedFiles	// loaded files (this is in ControlledDocuments now)
	 *
	 * DA: yes!  You're absolutely right: it also makes sense to record the name of the open
	 * theory when we're inside a theory, and the name of a theorem inside a proof.
	 * This is already in PGIP, it's called the "Prover Guise".  See the schema pgip.rnc.
	 * We can also query the prover to determine the guise to see if we're on track.
	 * 
	 * See currentfile below.
	 */

	/** Stores current state: @see {@link State} */
	private State state = State.TOP_LEVEL;

	/** The depth within a proof. */
	protected int proofDepth = 0;

	/** Name of file open. */
	// TODO: manage this on open/close/abort file.
	protected URI currentfile;

	/** Is the prover busy? */
	protected boolean busy = false;

	/** Is the prover process actually running? */
	boolean alive = false;

	/** TODO replace this with a {@link PGIPMessages}. (Or stuff in pgip package) */
	PGIPSyntax syntax;

	public ProverState(Converter conv, PGIPSyntax syntax) {
		super();
		this.syntax = syntax;
		this.proverKnowledge = new ProverKnowledge(conv);
	}

	/**
	 * Reset the state, appropriately for a (re)started prover.
	 */
	public void reset() {
		busy = false;
		alive = false;
		super.setChanged();
		setState(ProverState.State.TOP_LEVEL);  // will notify observers
	}

	/** Is the prover process actually running? */
	public boolean isAlive() {
		return alive;
	}

	/**
	 * Gets proof depth
	 * @return the depth within the proof.
	 */
	public int getProofDepth() {
		return state == ProverState.State.PROOF_LEVEL ? proofDepth : 0;
	}

	/**
	 * Sets the alive status, and notifies listeners if it's changed.
	 * @param alive the new alive status.
	 */
	public void setAlive(boolean alive) {
		if (this.alive == alive) {
			return; // no change
		}
		this.alive = alive;
		super.setChanged();
		super.notifyObservers();
	}
	
	/**
	 * Is this prover busy processing something, or ready for fresh input?
	 */
	public boolean isBusy() {
		return busy;
	}

	public void setBusy(boolean value) {
		if (busy==value) {
			return;
		}
		busy = value;
		super.setChanged();
		super.notifyObservers();
	}

	/**
	 * Is this prover available?  (true if it is alive and not busy)
	 */
	public boolean isAvailable() {
		return alive && !busy;
	}

	ProverKnowledge proverKnowledge = null;
	public ProverKnowledge getProverKnowledge() {
		return proverKnowledge;
	}

	/**
     * Adjust the prover state model in response to a goal being openend.
     * Switches to proof mode, and increments proof depth.
	 */
	public void openGoal() {
		setState(ProverState.State.PROOF_LEVEL);
		System.out.println("opening goal");
		proofDepth++;
		// da: below is too much output on large proofs
        // if (ProofGeneralPlugin.debug(this)) System.out.println("Proof depth: "+String.valueOf(proofDepth));
	}
	
	void closeGoal() throws ModellingException {
		if (state!=ProverState.State.PROOF_LEVEL) {
			throw new ModellingException("Cannot close a goal - the prover state model indicates that no goal is open");
		}
		System.out.println("closing goal");
		proofDepth--;
		// da: below is too much output on large proofs
        // if (ProofGeneralPlugin.debug(this)) System.out.println("Proof depth: "+String.valueOf(proofDepth));
		assert proofDepth > -1 : "We are in negative proof?!";
		if (proofDepth==0) {
			setState(ProverState.State.THEORY_LEVEL);
		}
	}

	/**
     * Sets the current state of the prover.
     */
	public void setState(State newState) {
		if (state != ProverState.State.PROOF_LEVEL) {
			proofDepth = 0;
		}
		State oldState = state;
		state = newState;
		if (oldState != newState) {
			super.setChanged();
			super.notifyObservers();
		}
	}

	/**
	 * Returns the current state of the prover
	 */
	public State getState() {
		return state;
	}

	Element lastResponse = null;
	/** returns the last significant response from the prover, usu an indication of the state of the theorem the prover is working on;
	 *  null if we have undone something or can't tell; set on CommandSucceeded, CmdCausedErrorEvent, and undo */
	public Element getLastResponse() {
		return lastResponse;
	}
	void setLastResponse() {
		PGIPEvent incoming = getTopmostCurrentModelChangesIncoming();
		if (incoming==null) {
			lastResponse = null;
		} else {
			lastResponse = incoming.parseTree;
		}
	}

	//public boolean handlingError = false;   //not needed anymore, with new CommandSucceeded event

	/**
	 * Called by session manager on prover events it listens to.
	 * This implementation pushes important such events (pushModelChange) until it gets
	 * a CmdProc or CmdCancel event, then it calls applyModelChanges or cancelModelChanges.
	 * @throws ModellingException if the state is different to expected (shouldn't happen)
	 */
	public void modelEvent(PGIPEvent e, SessionManager sm) throws ModellingException {
		//now events aren't modelled until we get a CmdProc (or cleared on CmdCancel event) -AH
		//another advantage of this is that prover output is available in currentModelChanges

		//all events that the SessionManager processes are sent here

		//it might seem nice to invalidate a command before sending,
		//but actually we should believe the prover as the authority
		//and not be too fancy here
		if (e instanceof PGIPReady) {
			return; //ignore ready messages (might speed it up slightly)
		}
		if (e instanceof CommandCausedErrorEvent) {
			setLastResponse();
			cancelModelChanges(e);
			CmdElement command = ((CommandCausedErrorEvent) e).getCommand();
			if (command != null && command.getPosition() != null) {
				// Script command failed; must remove queue region in document
				command.getProofScript().commandFailed(command);
			}
		} else if (e instanceof CommandSucceeded) {
			setLastResponse();
			try {
				applyModelChanges(e);
				CmdElement command = ((CommandSucceeded) e).getCommand();
				if (command != null && command.getPosition() != null) {
					// Commands that have a position imply some change in the document.
					command.getProofScript().commandSucceeded(command);
					if (command.getCmdType().equals(PGIPSyntax.ABORTFILE)) {					
						sm.doClearActiveScript(); // da: temporarily added sm as a parameter for this call,
						// what we should probably do is move active script management into this class.
					}
				}
			} catch (ModellingException me) {
				//shouldn't happen... if invalid, prover should throw error, and our cached changes cleared
				System.err.println("unexpected modelling errors when prover is valid (clearing; internal prover model is likely ruined)");
				synchronized (currentModelChanges) {
					currentModelChanges.clear();
				}
				if (ProofGeneralPlugin.debug(this)) {
					me.printStackTrace();
				}
				throw me;
			} catch (ProofScriptDocumentException de) {
				System.err.println(de.getMessage() + "\nInconsistency with document (clearing; internal prover model is likely ruined)");
				synchronized (currentModelChanges) {
					currentModelChanges.clear();
				}
				if (ProofGeneralPlugin.debug(this)) {
					de.printStackTrace();
				}
				throw new ModellingException(de.getMessage() + "\nInconsistency with document");
			}
		} else if (e instanceof UndoSent) {
			lastResponse = null;
			//model undo events immediately  -- TODO maybe we should model undos by watching the outgoing
			modelUndoEvent((UndoSent) e);
		} else {
			//we push Outgoing and mode Incoming, as well as Error, for inspection
			pushModelChange(e);
		}
	}

	/**
	 * a queue of unprocessed model changes;
	 * this is picked off from the front,
	 * so handlers can investigate down the line,
	 * eg to look at the response from a command
	 */
	List<PGIPEvent> currentModelChanges = new ArrayList<PGIPEvent>();

	/** adds an event as currently being processed */
	public void pushModelChange(PGIPEvent e) {
		synchronized (currentModelChanges) {
			currentModelChanges.add(e);
		}
	}

	/** returns the first 'PGIPIncoming' event still in the 'currentModelChanges' list;
	 *  used eg when processing PGIPOutgoing.opentheory
	 *  to get the response to the opentheory command; or null if none.
	 * <p/>
	 * ASSUMES:  that only one outgoing is processed at a time
	 * (which is reasonable, currently; but it wouldn't be too hard
	 * to file events by seq id; or do a check for the right refseq)
	 * TODO: probably better to check this by seq id / refseq
	 */
	public PGIPEvent getTopmostCurrentModelChangesIncoming() {
		Iterator mi = currentModelChanges.iterator();
		while (mi.hasNext()) {
			PGIPEvent e = (PGIPEvent)mi.next();
			if (e instanceof PGIPIncoming) {
				return e;
			}
		}
		return null;
	}

	/** Instructs the internal model to update with the success of all pushed PGIP events.
	 * Note that this happens <b>before</b> the command has been processed in the prover,
	 * so if some command fails we need to undo the change */
	public void applyModelChanges(PGIPEvent cmdProcEvent) throws ModellingException {
		if (currentModelChanges.size()>0) {
			synchronized (currentModelChanges) {
				while (currentModelChanges.size()>0) {
					//process each of the modeled changes (usually just one)
					PGIPEvent e = currentModelChanges.remove(0);
					if (e instanceof PGIPOutgoing) {
						modelOutgoingEvent((PGIPOutgoing) e);
					}
				}
			}
		}
	}
	/** instructs the internal model to ignore and drop all pushed PGIP events;
	 *  usually because they caused an error */
	public void cancelModelChanges(PGIPEvent e) {
		//we used to call modelErrorEvent on this,
		//but now the model isn't changed until after a successful application, so it's not necessary
		if (currentModelChanges.size()>0) {
			synchronized (currentModelChanges) {
				currentModelChanges.clear();
			}
		}
	}

	/** update the internal model with this event's success chez prover */
	void modelOutgoingEvent(PGIPOutgoing e) throws ModellingException {
		//System.out.println("modelling outgoing event "+e);
		if (e==null || e.parseTree==null) {
			return;
		}
		modelOutgoingElement(e.parseTree, e);
	}
	void modelOutgoingElement(Element xe, PGIPEvent e) throws ModellingException {
		String type = xe.getName();   //or cmd.getType() ??
		if (type==null) {
			return;
		}
        if (syntax.subType(type, PGIPSyntax.OPENFILE)) {
			doneOpenFile(xe, e);
		} else if (syntax.subType(type, PGIPSyntax.CLOSEFILE)) {
		    doneCloseFile(xe, e);
		} else if (syntax.subType(type, PGIPSyntax.RETRACTFILE)) {
   	    	retractedFile(xe, e);
		} else if (syntax.subType(type, PGIPSyntax.ABORTFILE)) {
 	    	abortedFile(xe, e);
		} else if (syntax.subType(type, PGIPSyntax.RETRACTTHEORY)) {
 	    	retractedTheory(xe, e);
		} else if (syntax.subType(type, PGIPSyntax.ABORTTHEORY)) {
		// da: NB: new case added during undo overhaul.  Changes
		// actions needed by modelling of undo theory open
 	    // So deal with it next step, try to make this step clean refactoring.
 	    // abortedTheory(xe, e);
 	    } else if (syntax.subType(type, PGIPSyntax.OPENTHEORY)) {
			doneOpenTheory(xe, e);
		} else if (syntax.subType(type,PGIPSyntax.CLOSETHEORY)) {
			doneCloseTheory(xe, e);
		} else if (syntax.subType(type,PGIPSyntax.OPENGOAL)) {
			doneOpenTheorem(xe, e);
		} else if (syntax.subType(type,PGIPSyntax.ABORTGOAL)) {
		// da: NB: new cases added during undo overhaul.  Changes
		// actions needed by modelling of undo theorem open
		// todo: deal with this next time.
		//	doneAbortGoal(xe, e);
		} else if (syntax.subType(type,PGIPSyntax.CLOSEGOAL)) {
			doneCloseTheorem(xe, e);
		} else if (syntax.subType(type, PGIPSyntax.THEORYITEM)) {
			doneTheoryItem(xe, e);
		} else if (syntax.subType(type, PGIPSyntax.PROOFSTEP)) {
			//do nothing
		} else if (syntax.subType(type, PGIPSyntax.WHITESPACE)) {
			//do nothing
		} else if (syntax.subType(type, PGIPSyntax.COMMENT)) {
			//do nothing
		} else if (syntax.subType(type, PGIPSyntax.DOCCOMMENT)) {
			//do nothing
		} else if (syntax.subType(type, PGIPSyntax.SPURIOUSCOMMAND)) {
			//do nothing
		} else if (syntax.subType(type, PGIPSyntax.UNDOSTEP)) {
			  //do nothing
		} else if (syntax.subType(type, PGIPSyntax.UNDOITEM)) {
			  //do nothing
		} else if (syntax.subType(type, PGIPSyntax.PROVEREXIT)) {
			//do nothing
		} else if (syntax.subType(type, InterfaceScriptSyntax.SET_PREF_COMMAND)) {
			// do nothing
		} else if (syntax.subType(type, PGIPSyntax.CHANGECWD)) {
			// TODO respond here rather than in SessionManager
//			setProverCwd(xe.);
		} else if (syntax.subType(type, PGIPSyntax.PROVERCONTROL)) {
			// do nothing
		} else if (syntax.subType(type, PGIPSyntax.PROVERQUERY)) {
			// do nothing
		} else {
			System.err.println("ProverState.modelOutgoingEvent, unknown: "+e);
		}
	}


  /**
	 * Model the effect of issuing an undo command.
	 * @param e
	 * @throws ModellingException
	 */
	// da: I'm a bit confused by rationale here.  We model outgoing undo events twice
	// now, once when they're outgoing and another time here.  (And of course, again,
	// when they're actually successfully processed: which is when the state changes
	// should really be committed IMO).
	// Aha: other comments suggest getting rid of this.  Good.  Will do next step.
	// TODO: UndoSent events don't seem to be firing; and if we back out over openTheorem we aren't decrementing the depth
	void modelUndoEvent(UndoSent e) throws ModellingException {
		DocElement cmd = (DocElement) e.parseTree;
		String type = cmd.getType();
		if (syntax.subType(type, PGIPSyntax.OPENTHEORY)) {
			//called if we back up over the start of a theory
			//is this called also if we back up over a closed theory ?
			undoneOpenTheory(cmd, e);
		} else if (syntax.subType(type,PGIPSyntax.OPENGOAL)) {
			//called if we back up over the start of a proof (or over a closed proof)
			undoneOpenTheorem(cmd, e);
			// da: TODO: model outgoing abortgoal/undoitem
		} else if (syntax.subType(type,PGIPSyntax.CLOSEGOAL)) {
			//don't think we ever come here
			// da: no, it looks like it would throw NPE anyway? 
			// and is wrong in the PGIP model, except for nested proofs.
			if (ProofGeneralPlugin.debug(this)) {
				System.err.println("Modelling an undoCloseGoal: this should not happen");
			}
			undoneCloseTheorem(cmd, e);
		} else if (syntax.subType(type,PGIPSyntax.CLOSETHEORY)) {
			undoneCloseTheory(cmd, e);
		} else if (syntax.subType(type, PGIPSyntax.THEORYITEM)) {
			undoneTheoryItem(cmd, e);
		} else if (syntax.subType(type, PGIPSyntax.PROOFSTEP)) {
			//do nothing
		} else if (syntax.subType(type, PGIPSyntax.WHITESPACE)) {
			//do nothing
		} else if (syntax.subType(type, PGIPSyntax.COMMENT)) {
			//do nothing
		} else if (syntax.subType(type, PGIPSyntax.DOCCOMMENT)) {
			//do nothing
		} else if (syntax.subType(type, PGIPSyntax.SPURIOUSCOMMAND)) {
			//do nothing
		} else {
			System.err.println("ProverState.modelUndoEvent, unknown: "+e);
		}
	}

	//not used-- errors don't need to be modelled now, they just cancel proposed changes
	//    void modelErrorEvent(CommandCausedErrorEvent e) throws ModellingException {
	//        if (e.parseTree instanceof DocElement)
	//            modelUndoEvent(new InternalEvent.UndoSent(e.getSource(),
	//                    								  (DocElement)e.parseTree));
	//        else throw new ModellingException("Command "+e.getText()+" caused an error"
	//                +" which could not be identified, since it did not seem to come from a proof script.");
	//    }

	/** models the successful application of an event other than outgoing and sent;
	 *  by default does nothing  */
	void modelOtherEvent(PGIPEvent e) //throws ModellingException
	{
		//don't need to do anything here right now (maybe subclasses want to)
	}



	//----- now actually update the model in response to a particular action

	//theorems (and theory items)

	void doneOpenTheorem(Element xe, PGIPEvent e) {
		openGoal();
		if (proofDepth == 1) {
			// new outermost theorem
			getProverKnowledge().onNewTheoremStart(xe);
		}
	}
	void doneCloseTheorem(Element xe, PGIPEvent e) throws ModellingException {
		closeGoal();
		if (proofDepth == 0) {
			// We're at the top level
			if (syntax.subType(xe.getName(), PGIPSyntax.POSTPONEGOAL)) {
				getProverKnowledge().onNewTheoremCancel(xe, arrayList(currentModelChanges));
			} else {
				getProverKnowledge().onNewTheoremFinish(xe, arrayList(currentModelChanges));
			}
		}
	}
	void doneAbortGoal(Element xe, PGIPEvent e) throws ModellingException {
		closeGoal();
		getProverKnowledge().onNewTheoremCancel(xe, arrayList(currentModelChanges));
	}

// TODO: remove this
// FIXME: why? seems this should get invoked if we undo immediatly after processing "lemma ..."; otherwise proof depth is wrong
	void undoneOpenTheorem(Element xe, UndoSent e) throws ModellingException {
		if (state==ProverState.State.PROOF_LEVEL) {
			closeGoal();
		}
		else {
			//no state action necessary if called from theory level.
			//a minor hack, we call this even when we undo a close theorem command (instead of undoneCloseTheorem)
			//this is what we were doing before, not sure why, but it was working.
			//and it means we are able to get the theorem name
		}
		getProverKnowledge().onUndoneTheorem(xe, xe, arrayList(currentModelChanges));
	}

	 // da: this is the undo operation that is *not* possible within the current PGIP model,
	 // so attempting this seems wrong, unless it's to unroll a failed transaction.
	 // (except: watch nested proofs)
	 // TODO: remove this
	 void undoneCloseTheorem(Element xe, UndoSent e) {
        if (state!=ProverState.State.PROOF_LEVEL) {
            System.err.println("Possible error in undo: attempting to undo whilst in "+state);
            System.err.println("   undone element: " + xe.asXML() );
        }
        openGoal(); //Changing from "no state action necessary - AH", 'cos I was getting errors with Isar nested proofs - Dan
		//note if we call this, we have to provide the theorem name (eg from the open theorem element) to the knowledge base
		getProverKnowledge().onUndoneTheorem(xe, null, arrayList(currentModelChanges));
	}

	void doneTheoryItem(Element xe, PGIPEvent e) {
		//no state action necessary
		getProverKnowledge().onNewTheoryItem(xe, arrayList(currentModelChanges));
	}

	void undoneTheoryItem(Element xe, UndoSent e) {
		//no state action necessary
		getProverKnowledge().onUndoneTheoryItem(xe, arrayList(currentModelChanges));
	}

	/**
	 * Returns a new instance of the given list, bypassing
	 * ArrayList's insistence on creating 110% sized arrays.
	 * @param input the List to start from
	 * @return a new ArrayList with the same elements.
	 */
	private static <T> ArrayList<T> arrayList(Collection<T> input) {
		ArrayList<T> copy = new ArrayList<T>(input.size());
		copy.addAll(input);
		return copy;
	}


	//theory open/close retract

	// FIXME: transitions for file level incomplete

	// ======= Top-level =========

	void doneOpenFile(Element xe, PGIPEvent e) {
		setState(ProverState.State.FILE_LEVEL);
		// TODO da: we should keep state of the file open here.
		// TODO da: we could consider making the script active here.
		// It should be, but that's enforced in SM at the mo.
	}

	void doneCloseFile(Element xe, PGIPEvent e) {
		setState(ProverState.State.TOP_LEVEL);
	}

	void retractedFile(Element xe, PGIPEvent e) {
		// State is still top level
		// FIXME assert here (and elsewhere) expected current states.
		setState(ProverState.State.TOP_LEVEL);
	}

	// ======= File-level ============

	void abortedFile(Element xs, PGIPEvent e) {
		// State is now top level, no file open
		setState(ProverState.State.TOP_LEVEL);
	}

	void doneOpenTheory(Element xe, PGIPEvent e) {
		setState(ProverState.State.THEORY_LEVEL);
		getProverKnowledge().onNewTheory(xe, arrayList(currentModelChanges));
	}

	void doneCloseTheory(Element xe, PGIPEvent e) {
		setState(ProverState.State.FILE_LEVEL);
		getProverKnowledge().onCloseTheory(xe);
	}

	void undoneCloseTheory(Element xe, UndoSent e) {
		// da: 30.12.06 change in Isabelle to allow undo of end: revert to theory open
		// TODO: Coq maybe has different behaviour here, we will need a parameter.
		getProverKnowledge().onUndoneCloseTheory();
		setState(ProverState.State.THEORY_LEVEL);
	}


	// ======= Theory level ============

	void undoneOpenTheory(Element xe, UndoSent e) {
		getProverKnowledge().onUndoneOpenTheory();
		setState(ProverState.State.FILE_LEVEL);
	}

	void abortedTheory(Element xs, PGIPEvent e) {
		// State is now top level, no file open.
		// This is same as undoneOpenTheory (TODO: which can be removed next step).
		getProverKnowledge().onUndoneOpenTheory();
		setState(ProverState.State.FILE_LEVEL);
	}

	void retractedTheory(Element xe, PGIPEvent e) {
		// undoneCloseTheory(xe, null);   // think this is the same as undoneCloseTheory
		                               // FIXME da: no, it isn't really: it happens when we never
		                               // completed the close theory.
		getProverKnowledge().onUndoneCloseTheory();
		// should already be file level
		setState(ProverState.State.FILE_LEVEL);
	}

	// --------------------------------- Exception ---------------------------

	public static class ModellingException extends ScriptingException {
		public ModellingException(String msg) {
			super(msg);
		}
	}

	public void dispose() {
		super.deleteObservers();
	}
}
