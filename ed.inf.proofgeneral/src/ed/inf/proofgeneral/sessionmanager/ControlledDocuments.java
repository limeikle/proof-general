/*
 *  $RCSfile: ControlledDocuments.java,v $
 *
 *  Created on Nov 24, 2006 by da
 *  part of Proof General for Eclipse
 */

package ed.inf.proofgeneral.sessionmanager;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.document.ProofScriptDocumentProvider;
import ed.inf.proofgeneral.document.ProofScriptDocument.ScriptingQueueState;

/**
 * Look after the collection of documents controlled by a session manager.
 * This class is responsible for unlocking/locking documents.
 */
// TODO: replace lockedScripts by a map from normalised URIs to ObjState,
// and remove busyScripts.
// Then we can have scripts queued, outdated, unparseable, etc.

public class ControlledDocuments {

//  -----------------------------------------------------------------------------
//  State
//  -----------------------------------------------------------------------------

	 /* TODO: consider whether to search for known documents instead of trying to
	 * track them here.
	 * To avoid confusion, a document should be controlled by at most one
	 * session manager (some argument for adding a reference to the sm in the document
	 * itself after all: or at least an "in use" reference flag).
     */

	/**
	 * Documents controlled by this session manager
	 * These may or may not be visible in editors or other views.
	 */
	private List<ProofScriptDocument> controlledDocuments;

	/**
	 * A list of URIs which are considered to be processed by the prover.
	 * Note that these may be proof scripts or other files.  They may
	 * or may not be known to the session manager in controlledDocuments.
	 * URIs are kept in this list in the form given by uri.normalize()
	 * INVARIANT: no duplicate URIs.
	 * @see #controlledDocuments
	 */
	private List<URI> lockedScripts;

	/**
	 * A list of URIs which are considered to be busy in the prover
	 * (not the interface).  These should be locked by the interface
	 * for safety.
	 * <p>
	 * <b>INVARIANT</b>: no duplicate URIs; every element of busyScripts 
	 * is also in lockedScripts.
	 */
	private List<URI> busyScripts;


//  -----------------------------------------------------------------------------
//  Constructor
//  -----------------------------------------------------------------------------

	public ControlledDocuments() {
		reset();
	}

	/**
	 * Clear state of controlled documents.
	 * Note: this may be a useful short cut when closing many documents at once,
	 * but if documents are still open their state must be
	 * reset individually, without losing track of them.  See unlockAll().
	 */
	private void reset() {
        this.lockedScripts = new ArrayList<URI>();
        this.busyScripts = new ArrayList<URI>();
        this.controlledDocuments = new ArrayList<ProofScriptDocument>();
	}

//  -----------------------------------------------------------------------------
//  Methods
//  -----------------------------------------------------------------------------

	/**
	 * Add a document to the list of those controlled by this Session Manager.
	 * Called when documents are associated with a session manager (currently 
	 * automatically on creation in {@link ProofScriptDocumentProvider} or when
	 * a document is made active).  This can result in doc being automatically
	 * locked, if the session manager believes it should be.
	 * <p>
	 * The list of controlled documents is used by {@link #unlockAll()} to 
	 * clear session state.
	 *
	 * @param doc
	 */
	public void controlDocument(ProofScriptDocument doc) {
		if (!controlledDocuments.contains(doc)) {
			controlledDocuments.add(doc);
		}
		if ( !doc.isLocked() &&	(doc.getResource() != null) ) {
			URI uri = doc.getResource().getLocationURI();
			if (this.isLocked(uri)) {
			      doc.lock(!this.isBusy(uri),ProofScriptDocument.ScriptingQueueState.FORWARDS);
			}
		}
	}

	/**
	 * Whether this URI is recorded as being busy in the prover
	 * @param uri the URI of the script to query 
	 * @return true if the URI is recorded as 'busy'; otherwise false.
	 */
	public boolean isBusy(URI uri) {
		return busyScripts.contains(uri);
	}

	/**
	 * Remove a document from the list of those controlled by the Session Manager.
	 * Does nothing if this document is not being tracked or if this document
	 * is flagged as being active.  This is safe to do when a document is freed,
	 * for example, when there is no view left on it.
	 * @param doc the document to remove from locked lists.
	 */
	public void freeDocument(ProofScriptDocument doc) {
		if (controlledDocuments.contains(doc) & !doc.isActiveForScripting()) {
			controlledDocuments.remove(doc);
		}
	}


	/**
	 * Add a script to the list of scripts locked in this session.
	 * Lock any open instances of the document.
	 * @param givenuri the URI to record
	 * @param complete whether or not the processing is complete
	 */
	public void lock(URI givenuri,boolean complete) {

		URI uri = givenuri.normalize();

		if (lockedScripts.contains(uri)) {
			if (ProofGeneralPlugin.debug(this)) {
				// da: note that it is normal to see this message for Isabelle.
				System.out.println("SM/CD: IGNORED harmless attempt to lock already locked URI: "+uri.toString());
			}
			return;
		}
		if (ProofGeneralPlugin.debug(this)) {
			System.out.println("SM/CD: LOCKING URI: "+uri.toString());
		}

		if (!lockedScripts.contains(uri)) {
			lockedScripts.add(uri);
		}
		if (complete) {
			busyScripts.remove(uri);
		} else {
			if (!busyScripts.contains(uri)) {
				busyScripts.add(uri);
			}
		}

		// FIXME da: it looks as if controlled documents should really be *all* documents
		// opened in any editors.   OR: we must check for a locked document when it's
		// even switched to in an editor (not just opened)?
        for(Iterator<ProofScriptDocument> i = controlledDocuments.listIterator(); i.hasNext();) {
            ProofScriptDocument doc = i.next();
            URI f = doc.getResource().getLocationURI().normalize();
            if (f.equals(uri)) {
            	doc.lock(complete,ScriptingQueueState.READY);
                //  no early exit here, in case multiple docs
            }
        }
	}

	/**
	 * Lock a document, also recording its underlying URI as being
	 * locked in this session.  This is used when the interface
	 * completes processing a script.
	 *
	 */
	public void lock(ProofScriptDocument doc) {
		URI uri = doc.getResource().getLocationURI();
		if (uri != null) {
				lock(uri,true);
		}
		// Ought to not be necessary, because we should have found the document
		// in our list of controlled documents.  An alternative backup would
		// be to add it to the controlled documents first.
		if (!doc.isLocked()) {
			doc.lock(true,ScriptingQueueState.READY);
		}
	}
	    // Old comments from ProofScriptDocument.lock(SessionManager sm):
		// GD: resource can be null when file is remote.
		// perhaps we shouldn't even bother trying to lock if we determine this.
		// DA: no, we should lock the file (it could be a file from a library outside
		// the workspace).  What was wrong was that we were locking with paths instead of
		// URIs, which is in fact what PGIP specifies to use and closely corresponds
		// to Eclipse resources.  However, if the file content is altered at a later
		// point (fetching from remote source, whatever) then we ought to retain
		// the locking state which might mean adjusting these locked positions.
		// It would be simpler if atomically locked documents are treated
		// as separate cases without using the two watermarks in this case.

	/**
	 * Is this script on the list of locked scripts?
	 * @param givenuri
	 * @return true if lockedscripts contains the normalised version of the given uri.
	 */
	public boolean isLocked(URI givenuri) {
		return lockedScripts.contains(givenuri.normalize());
	}


	/**
	 * Indicate an unlocking: if it is complete,
	 * removes the URI from the locked list and unlocks matching controlled documents.
	 * If the unlocking action is in progress, we indicate the script as being busy
	 * and locked.
	 * The next time this file is opened, it will be unlocked.
	 * Looks for corresponding controlled documents, and changes them appropriately if found.
	 * @param script
	 * @param complete if false, the unlock actually locks the document as being busy
	 */
	public void unlock(URI script, boolean complete) {
		if (ProofGeneralPlugin.debug(this)) {
			System.out.println("SM/CD: UNLOCKING URI: "+script.toString()+
						(lockedScripts.contains(script) ? "" : " (not known to be locked)" +
						(complete ? "" : "[IN PROGRESS]")));
		}
		if (!complete) {
			// This is actually identical to a lock action which indicates the
			// script is busy; we don't track whether the URI is being done or undone for now.
			this.lock(script,false);
			return;
		}
		if (lockedScripts.contains(script)) {
			lockedScripts.remove(script);
			busyScripts.remove(script);
			for(Iterator<ProofScriptDocument> i = controlledDocuments.listIterator(); i.hasNext();) {
				ProofScriptDocument doc = i.next();
	            URI f = doc.getResource().getLocationURI().normalize();
				if (f.equals(script)) {
					doc.unlock(); // da: NB no exit early in case of multiple docs
				}
			}
		}
	}

	/**
	 * Ensure that all of the known documents are unlocked; moreover, clear the record of locked URIs.
	 * This is the appropriate action when the session is shut down and restarted.
     * The record of controlled documents remains the same so that they are are
     * are updated properly in subsequent sessions.
	 */
	void unlockAll() {
		for(ProofScriptDocument fdoc : controlledDocuments) {
			fdoc.unlock();
			fdoc.clearActiveForScripting();
		}
	    this.lockedScripts = new ArrayList<URI>();
	}

	/**
     * Find a Proof Script document on the given resource.  Returns the
     * first such document found, which should be unique.
	 * @param res the resource for which to get the ProofScriptDocument.
	 * @return first document found for a resource.
	 */
	public ProofScriptDocument findDocumentOn(IResource res) {
		for (ProofScriptDocument doc : controlledDocuments) {
			if (res.equals(doc.getResource())) {
				return doc;
			}
		}
		return null;
	}

}
