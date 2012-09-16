/*
 *  $RCSfile: ProofScriptDocument.java,v $
 *
 *  Created on 26 Apr 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */    


// REFACTORING 21.3.07:
//  I'm changing offsets for the queue/lock region into positions.
//  The advantage is that positions will move with document edits.
//  This should make things robust for parsing, folding, etc.
//  It should also allow a simple annotation model to be used for
//  colouring the regions directly (perhaps w. markers).
//  The first step of the refactoring is to use setters/getters
//  instead of the integer assignments.  This can then be optimised
//  later.


/*
 * TODO:
 *  - can now refactor to change value of getProcessedOffset to 0 instead of -1; needs inspection
 *  of all call sites.  
 */

package ed.inf.proofgeneral.document;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.DocumentPartitioningChangedEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.texteditor.MarkerUtilities;

import ed.inf.proofgeneral.NotNull;
import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.ProofGeneralProverRegistry.Prover;
import ed.inf.proofgeneral.preferences.PreferenceNames;
import ed.inf.proofgeneral.sessionmanager.PGIPSyntax;
import ed.inf.proofgeneral.sessionmanager.ProverSyntax;
import ed.inf.utils.ReflectionHelper;
import ed.inf.utils.datastruct.MutableObject;
import ed.inf.utils.eclipse.DisplayCallable;
/**
 * An important class that acts as the model for proof scripts.
 * This class provides an interface to a script, including low-level parsing support and locking and unlocking functions.
 * @author Daniel Winterstein
 * @author David Aspinall
 */
// TODO:
//  - With the preference setting for editing processed text,
//    watermarks should be markers, not offsets.  This will be managed
//    automatically by the StateMarkers branch, so do not do this yet
//    but be aware of bug shown in test case 2006-10/BadEfficiencyMassOutput/Sleep.thy.

// CONCURRENCY HERE da:
// If we're going to inspect parts of document in different threads (e.g. async
// parsing) we need to be careful to propagate changes across threads, don't we?
// Or can we rely on Eclipse job management for loose sync?
// I see some really strange things happen with sending parse requests with the
// same data after the document has been changed.
// Taking a lock on the whole document object is a disaster, though:
// it's accessed all over the place.

public class ProofScriptDocument extends SymbolisedDocument {
	
	/** Document title */
	private String title;

	/** PGIP related syntax. */
	// FIXME da: this can probably be removed, but we need to be a bit careful,
	// think about it first.  If we keep it, it actually may make better sense
	// to have it here than every where else it gets duplicated.
	public PGIPSyntax syntax;

	/**
	 * A lightweight Eclipse description of the file corresponding to this document.
	 * This may be null if the document has no associated resource (e.g. it
	 * is a generated intermediate document).
	 */
	private IResource resource;
	/* da: Ideally I the document shouldn't need to know it's underlying resource.
     * E.g. when we edit a file outside the workspace, there is no underlying resource.
     * We'd like to create intermediate documents without resources, etc.
     * We need to protect the code against a null resource here, or, better, remove
     * the resource reference altogether and start from the edited document when we
     * want to get it in the UI.  At the moment we use the resource extensively
     * for managing markers on a document and also handling the notion of 
     * locked files (i.e. SessionManager/ControlledDocuments).
     */

	// =============================================================================================
	//
	// Parse tree
	//
	
	// TODO 7.8.08: moving out script management from here, in progress.
	//  Ignore javadoc warnings for now as these methods will be removed!
	// - ProofScriptDocumentTree now has document tree and SM methods
	// - Too much indirection here, should make parser a peer/field in document tree,
	//    then we can remove delegation below
	// - Eventually should be able to thin document references, e.g. remove them from
	//   every doc element in tree.
	
	@NotNull
	private final ProofScriptDocumentTree documentTree;
	
	/**
     * @return the root of the parse tree for this document 
     */
    public ContainerElement getRootElement() {
	    // TODO Auto-generated method stub
	    return documentTree.getRootElement();
    }
    
    /**
     * @see ProofScriptDocumentTree#isFullyParsed()
     */
    public boolean isFullyParsed() {
    	return documentTree.isFullyParsed();
    }
	
	/**
	 * delegate for {@link ProofScriptDocumentTree#closeOpenElement()}
	 */
	public void closeOpenElement() {
		documentTree.closeOpenElement();
	}
	
	public DocElement findNext(String type, int offset) {
	    return documentTree.findNext(type,offset);
	}
	
    public DocElement findPrevious(String type, int offset) {
	    return documentTree.findPrevious(type, offset);
    }

    public void fireParseTreeChangedEvent(int offset, int length) throws BadLocationException {
    	documentTree.fireParseTreeChangedEvent(offset, length);
    }

   public ContainerElement getOpenElement() {
	    return documentTree.getOpenElement();
    }

    public int getParseOffset() {
	    return documentTree.getParseOffset();
    }

    public List<CmdElement> lockAndGetCommandsUpto(int loc) throws ProofScriptDocumentException {
	    return documentTree.lockAndGetCommandsUpto(loc);
    }

    public void setParseRegionForEditAtOffset(int offset) {
    	documentTree.setParseRegionForEditAtOffset(offset);
    }

    public void setOpenElement(ContainerElement ce) {
    	documentTree.setOpenElement(ce);
	    
    }

    public void setParseOffset(int ppos) {
	    documentTree.setParseOffset(ppos);
	}

   public List<CmdElement> unlockAndGetCommandsBackTo(int loc) throws ProofScriptDocumentException {
	    return documentTree.unlockAndGetCommandsBackTo(loc);
    }
	
	
	// =============================================================================================
	//
	// Folding
	//	
	/**
	 * The folding structure associated with this document
	 */
	private final Folding folding = new Folding(this);
	
    /**
     * Delegate for {@link Folding#recalculateFoldingStructure()}
     */
    public void recalculateFoldingStructure() {
    	folding.recalculateFoldingStructure();
    }
    
	/**
	 * Delegate for {@link Folding#setProjectionAnnotationModel(ProjectionAnnotationModel)}
     */
    public void setProjectionAnnotationModel(ProjectionAnnotationModel projectionAnnotationModel) {
    	folding.setProjectionAnnotationModel(projectionAnnotationModel);
	    
    }
    
    /**
     * Delegate for {@link Folding#getUnfoldedOffsetAfter(int)}
     */
    public int getUnfoldedOffsetAfter(int offset) {
    	return folding.getUnfoldedOffsetAfter(offset);
    }

    /**
     * Delegate for {@link Folding#getUnfoldedOffsetBefore(int)}
     */
    public int getUnfoldedOffsetBefore(int offset) {
    	return folding.getUnfoldedOffsetBefore(offset);
    }


// End folding    
// =================================================================================
    
	/**
	 * If this document is currently active for scripting.
	 * NOTE: in the future we want to allow possibly more than one active document
	 * (e.g. more than one session manager (which the existing code is geared
	 * to allow), or in more advanced cases, even more than one active document,
	 * which is allowed with the Broker.
	 */
	protected boolean activeForScripting;

	/**
	 * A type representing the state of the queue.
	 */
	public enum ScriptingQueueState {
		/** Active queue moving forwards in document */
		FORWARDS,
		/** Active queue undoing in document */
		BACKWARDS,
		/** No active queue in document */
		READY;
	}

	private volatile ScriptingQueueState scriptingState;

	/**
	 * If true, the whole document is locked and should not be editable.
	 *
	 * INVARIANT: if the whole document is locked, both getProcessedOffset() and
	 * getLockOffset() should point to end of file.  This could be better named
	 * "finalised" rather than locked.  A completed file should not be editable
	 * at all, and should not be active for scripting (nothing can be done).
	 */
	// da: this could be perhaps implemented/reflected in a resource property.
	// However, it is relevant wrt a session manager instance, not global,
	// so it's probably too transient/specific.
	protected boolean fLocked = false;

	/**
	 * The document is split into three non-overlapping areas: the processed region (blue),
	 * the busy region (pink), and editable text (uncoloured), which appear in this sequence
	 * determined by two watermarks:
	 *
	 *   | processed (blue) |  queued (pink)  | editable   |
	 *   |      locked (read only)            |  unlocked  |
	 *                      ^                 ^
	 *                 getProcessedOffset()  getLockOffset()
	 *
	 *  INVARIANT: -1 <= getProcessedOffset() <= getLockOffset() <= last=getLength()-1
	 *
	 *  The watermarks are set at the last offset within the region
	 *  concerned, thus range from -1 (no region) to getLength()-1 (whole doc).
	 *  (By convention, document offsets range from 0 to getLength()-1)
	 *
	 */	
	// da: NB: this scheme doesn't allow us to tell whether queue is forwards or backwards, 
	// if we kept queue as offset from processed position it would.
	
	/** The document region which has been processed.  Only the length is relevant; the position
	 * always begins at the first offset. */
	private final ScriptManagementPosition fProcessedRegion = new ScriptManagementPosition("ProcessedRegion",0,0);
	
	/** The document region which is locked, the difference between this and
	 * {@link #fProcessedRegion} is used to calculate the queue region.  Only the length is used currently. */	
	private final ScriptManagementPosition fQueueRegion = new ScriptManagementPosition("QueueRegion",0,0);

	/** A marker for the current processed region. */
	protected IMarker processedMarker = null;
	
	/** A marker for the current queued region. */
	protected IMarker queuedMarker = null;
	
	/** Syntax specific to a prover; final after set in init() */
	protected ProverSyntax proverSyntax = null;

	/** The prover; final after it is set in init() */
	protected Prover prover = null;

	// -------------------------------------------------------------------------------------
	//
	// Simple accessors
	//
	
	/**
	 * @returns the title of this document.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @returns the PGIP related syntax.
	 */
	public PGIPSyntax getSyntax() {
		return syntax;
	}

	/**
	 * Gets the prover-specific syntax object
	 * @return the prover-specific syntax
	 */
	public ProverSyntax getProverSyntax() {
		return proverSyntax;
	}

	/**
	 * Returns the Eclipse description of the file for this document.
	 * @see #resource
	 */
	public IResource getResource() {
		return resource;
	}

	/**
     * @return the prover
     */
    public Prover getProver() {
    	return prover;
    }
    


	// -------------------------------------------------------------------------------------
	//
	// Methods for adjusting the processed/queued positions
	//
	
	/**
	 * Set the queue for moving forward in the document, moving the locked
	 * position to the given offset.
	 * @param newOffset The new value for the lock offset.
	 */
	private synchronized void setQueueForwards(int newOffset) {
		assert scriptingState == ScriptingQueueState.READY ||
		       scriptingState == ScriptingQueueState.FORWARDS :
		    "Document must not have a queue in the reverse direction";
		assert newOffset > getLockOffset() :
			"New offset " + newOffset + "should be beyond current locked position " + getLockOffset();
		scriptingState = ScriptingQueueState.FORWARDS;
		int oldOffset = getLockOffset();
		setLockOffset(newOffset);
		updateMarkerPositions();
		partitionChangeBroadcast(oldOffset+1, newOffset-oldOffset);
	}
	
	/**
	 * Move queue boundary, checking consistency for forwards movement in document.
	 * @param newOffset
	 */
	private synchronized void setQueueForwardsAdvance(int newOffset) {
		System.out.println("moving forwards, from "+getProcessedOffset()+" to "+newOffset+"; " + "lock is at "+getLockOffset());
		
		assert scriptingState == ScriptingQueueState.FORWARDS :
			 "Document inconsistency: should be moving forwards";
		assert newOffset > getProcessedOffset() : "Should be moving processed point forwards";
		int oldOffset = getProcessedOffset();
		
		//back up over whitespace (so locked pos excludes whitespace);
		//seems to be necessary iff command includes symbols
		try {
	        while (newOffset>=0 && (newOffset >= getLength() || Character.isWhitespace(getChar(newOffset))))
	        	newOffset--;
        } catch (BadLocationException e) {
	        //shouldn't happen -- doc range checked above
	        e.printStackTrace();
        }
		
		setProcessedOffset(newOffset);
		if (getLockOffset()<getProcessedOffset()) {
			// This shouldn't happen.
			System.err.println("Process forces setting lock offset to "
					+getProcessedOffset()+ " (from "+getLockOffset()+"), shouldn't happen if queued properly! (normal for old code)");
			setLockOffset(getProcessedOffset());
		}
		if (getLockOffset() == getProcessedOffset()) {
			scriptingState = ScriptingQueueState.READY;
		}
		updateMarkerPositions();
		partitionChangeBroadcast(oldOffset+1, newOffset-oldOffset);
	}
	

	/**
	 * Move queue boundary, checking consistency for backwards movement in document (undo).
	 * @param newOffset
	 */
	private synchronized void setQueueBackwardsRetract(int newOffset) {
		assert scriptingState == ScriptingQueueState.BACKWARDS :
			 "Document inconsistency: should be moving backwards";
		assert newOffset < getLockOffset() : "Should be moving processed position backwards";
		int oldOffset = getLockOffset();
		
		//back up over whitespace, too
		int whitespace = 0;
		try {
	        while (newOffset>=0 && Character.isWhitespace(getChar(newOffset))) {
	        	newOffset--;
	        	whitespace++;
	        }
        } catch (BadLocationException e) {
	        //shouldn't happen
	        e.printStackTrace();
        }
        
		setLockOffset(newOffset);
		if (getLockOffset()<getProcessedOffset()) {
			//shouldn't be off by more than the amount of whitespace
			if (getLockOffset()+whitespace < getProcessedOffset()) {
				System.err.println("Undo forces setting processed offset to "
						+getProcessedOffset()+ ", shouldn't happen if queued properly!");
			}
			//back up whitespace too
			setProcessedOffset(getLockOffset());
		}
		if (getLockOffset() == getProcessedOffset()) {
			scriptingState = ScriptingQueueState.READY;
		}
		updateMarkerPositions();
		partitionChangeBroadcast(newOffset+1, oldOffset-newOffset-1); // FIME: -1 or not?
	}
	
	/**
	 * Clear the queue region and indicate that it is ready for processing again.
	 */
	private synchronized void setQueueEmptyReady() {
		checkState();
		int oldOffset = getLockOffset();
		int newOffset;
		if (oldOffset != getProcessedOffset()) {
			// A forwards or backwards queue is on the document.  Remove it accordingly.
			if (scriptingState == ScriptingQueueState.FORWARDS) {
				// A command failed: remove forwards queue region
				newOffset = getProcessedOffset();
				setLockOffset(newOffset);
			} else if (scriptingState == ScriptingQueueState.BACKWARDS) {
				// An undo command failed (*shouldn't* happen in prover): remove backwards queue 
				newOffset = getLockOffset();
				setProcessedOffset(newOffset);
			} else {
				System.err.println("setQueueEmptyReady document inconsistency: expected to find a queue region");
				newOffset = getProcessedOffset();
				setLockOffset(newOffset);
			}
			updateMarkerPositions();
			if (newOffset < oldOffset) {
				partitionChangeBroadcast(newOffset, oldOffset-newOffset);
			} else {
				partitionChangeBroadcast(oldOffset, newOffset-oldOffset);
			}
		}
		scriptingState = ScriptingQueueState.READY;
	}

	/**
	 * A timer for running view updates after a while
	 */
	private final Timer markerUpdateTimer = new Timer("Marker update timer");
	private TimerTask markerUpdateTask = null;

	private class ScriptMarkerUpdateTask extends TimerTask {
		@Override
        public void run() {
			updateMarkerPositionsRealAction();
		}
	}

	void updateMarkerPositions() {
		// TODO: can optimise this into individual method calls, should
		// know whether markers may be deleted/exist or not.
		// TODO: batch up marker movements by signalling update
		// is pending, then do later.
		if (!ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_DEBUG_USE_MARKER_ANNOTATIONS)) {
			return;
		}
		if (markerUpdateTask != null) {
			markerUpdateTask.cancel();
		}
		markerUpdateTask = new ScriptMarkerUpdateTask();
		markerUpdateTimer.schedule(markerUpdateTask, 100);
	}

	// NB: scheduling this as a runnable results in unacceptable sluggishness
	// in update to annotation in editor.
	// NB 2: moreover, it doesn't fix the update position bug!

	private void updateMarkerPositionsRealAction() {
		IWorkspaceRunnable r = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
		//try {
				synchronized (ProofScriptDocument.this) {
					if (resource == null) {
						return; // can happen during startup [???]
					}
					if (getProcessedOffset() >= 0) {
						// Unfortunately, moving marker start/end positions doesn't seem to work
						// FIXME: is it a case of sending a correct update?  
						// For now, we delete and recreate the marker immediately, may make
						// some flickering/inefficiency
						// NB: moving end position forwards seems OK, but moving it backwards
						// seems tricky, doesn't always update properly.  Seems to be because
						// AnnotationPainter#catchUpWithModel is not clever enough to remove
						// pieces of annotation when annotationsAdded shrink, so they must be
						// deleted and re-added.
						// BEGIN PATCH
						if (processedMarker != null) {
							processedMarker.delete();
							processedMarker = null;
						}
						// END PATCH
						if (processedMarker == null) {
							processedMarker = resource.createMarker(ProofScriptMarkers.PROCESSED);
						}
						MarkerUtilities.setCharStart(processedMarker, 0);
						MarkerUtilities.setCharEnd(processedMarker, getProcessedOffset());
					} else if (processedMarker != null) {
						processedMarker.delete();
						processedMarker = null;
					}
					if (getLockOffset() > getProcessedOffset()) {
						// BEGIN PATCH
						if (queuedMarker != null) {
							queuedMarker.delete();
							queuedMarker = null;
						}
						// END PATCH
						if (queuedMarker == null) {
							queuedMarker = resource.createMarker(ProofScriptMarkers.QUEUED);
						}
						MarkerUtilities.setCharStart(queuedMarker, getProcessedOffset()+1);
						MarkerUtilities.setCharEnd(queuedMarker, getLockOffset()+1);
					} else if (queuedMarker != null) {
						queuedMarker.delete();
						queuedMarker = null;
					}
					//
					//ActiveScriptEditor.getActiveScriptEditor().refresh();
				}
			}
		};
		try {
			IWorkspace workspace = resource.getWorkspace();
			if (workspace != null) { // during shutdown 
				workspace.run(r, null,IWorkspace.AVOID_UPDATE, null);		
			}
		} catch (CoreException e) {
			// FIXME: log it
		}
// Ideas instead: modify *position* of marker with position updater: see
// annotation model & map.  Changing marker itself seems a bit heavy, mebbe,
// with resource change implication and job queueing in workspace.		
//		IWorkspace workspace = resource.getWorkspace();
//		workspace.
//		MarkerManager manager = workspace.getMarkerManager();
//		try {
//			workspace.prepareOperation(null, null);
//			workspace.beginOperation(true);
//
//		resource.
//		Display.
	}


	/**
	 * Remove the queue (pink) region from the document, if any is
	 * present.
	 */
	public synchronized void removeQueueRegion() {
		checkState();
		int oldstart = getLockOffset();
		int oldend = getProcessedOffset();
		if (scriptingState == ScriptingQueueState.FORWARDS) {
			setLockOffset(getProcessedOffset());
			scriptingState = ScriptingQueueState.READY;
		} else if (scriptingState == ScriptingQueueState.BACKWARDS) {
			setProcessedOffset(getLockOffset());
			scriptingState = ScriptingQueueState.READY;
		}
		updateMarkerPositions();
		partitionChangeBroadcast(oldstart+1,oldend-oldstart);
	}

	/**
	 * Sets how much of the document has been processed, and broadcasts the change.
	 * @param newOffset the new offset value.
	 */
	// TODO: this is in process of being refactored 
    synchronized void setProcessedOffset(int newOffset) {
    	int oldOffset = fProcessedRegion.getLength()-1;
    	fProcessedRegion.setLength(newOffset+1);
		processOffset(oldOffset, newOffset,true);
	}



	/**
	 * Return the position of the last locked character in the document, or
	 * -1 if there is no locked region.
	 * @return the offset of the last locked character
	 */
	public int getLockOffset() {
		return fQueueRegion.getLength() - 1;
	}

	/**
	 * Set the position of the last queued & locked character in the document, or
	 * -1 if there should be no queue region.
	 */
	// TODO da: refactoring this, this is an intermediate step
	synchronized void setLockOffset(int newOffset) {
		fQueueRegion.setLength(newOffset+1);
	}

	/**
	 * Gets the offset of the last processed character in the document, or
	 * -1 if there is no processed region.
	 * 
	 * @return the processing offset
	 */
	public int getProcessedOffset() {
		return fProcessedRegion.getLength() -1;
	}


	public String getText(Position p) throws BadLocationException {
			return get(p.offset,p.length);
	}
	/**
	 * Unlock the entire document by clearing the locked flag
	 * and moving offsets.  Works on completed scripts (shouldn't be active) and
	 * on partially processed scripts.
	 */
	public void unlock() {
		if (fLocked && ProofGeneralPlugin.debug(this)) {
			System.out.println("PSD: removing global lock (completed flag) on document " + getTitle());
		}
		resetDocument();
	}
	/**
	 * Reset the scripting state for this document and remove all processed/queued regions.
	 */
	private synchronized void resetDocument() {
		resetPositions();
		fLocked = false;
		scriptingState = ScriptingQueueState.READY;
		updateMarkerPositions();
		partitionChangeBroadcast(0, getLength()-1);
		updateProperties();
	}
	
	private void resetPositions() {
		// [ NB: setting offsets to zero shouldn't be necessary, but at the
		//   moment the position updaters can sometimes affect them. [FIXME] ]
		fProcessedRegion.setOffset(0);
		fProcessedRegion.setLength(0);
		documentTree.reset();
		fQueueRegion.setOffset(0);
		fQueueRegion.setLength(0);
	}


	/** Lock or queue the entire document atomically, marking it as completely processed or
	 *  completely queued and with the queue in the indicated state.  If the
	 *  indicated state is READY, the document will be locked atomically.
	 *  Otherwise only the queue region is locked, as usual.
	 *  NB: session managers contain a list of locked documents,
	 *  clients should usually use sessionmanager.lock instead of this method.
	 *  @param complete if true, mark as completely processed, otherwise, completely queued
     */
	public synchronized void lock(boolean complete,ScriptingQueueState state) {
		if (fLocked) {
			// TODO eventually replace debug with assert: this should not be an "active" document.
			// Then we can maintain invariant that locked => !active, active ==> !locked
			if (ProofGeneralPlugin.debug(this)) {
				System.out.println("PSD: IGNORED attempt to lock already locked document:" + getTitle());
			}
		} else {
			if (ProofGeneralPlugin.debug(this)) {
				System.out.println("PSD: locking document " + getTitle());
			}
			fLocked = (scriptingState == ScriptingQueueState.READY ? true : false);
			scriptingState = state;
			if (complete) {
				if (getProcessedOffset() < getLength()-1) {
					// 	Indicate rest of text locked
					int oldend = getProcessedOffset() + 1;
					setLockOffset(getLength()-1);
					setProcessedOffset(getLength()-1);
					updateMarkerPositions();
					partitionChangeBroadcast(oldend, getLength()-1-oldend);
				} else {
					if (getLockOffset() < getLength()-1) {
						// Indicate whole text queued
						int oldstart = getProcessedOffset() + 1;
						setProcessedOffset(-1);
						setLockOffset(getLength()-1);
						updateMarkerPositions();
						partitionChangeBroadcast(oldstart,getLength()-1-oldstart);
					}
				}
			}
			updateProperties();
		}
	}

	/**
	 * @deprecated - use ed.inf.proofgeneral.document.ProofScriptDocument#lock(boolean,ScriptingState) instead
	 */
	@Deprecated
	public void lock(boolean completed) {
		lock(completed,ScriptingQueueState.FORWARDS);
	}


	/**
	 * Check that the document state (class invariant) is valid.  This should always succeed:
	 * this method will cause an assertion failure in case the state is invalid.
	 */
	public synchronized void checkState() {
		// Check watermarks appear in order
		assert -1 <= getProcessedOffset() :
			"Processed offset is beyond start of document limit";
		assert getProcessedOffset() <= getLockOffset() :
			"Processed offset must appear before locked offset";
		assert getLockOffset() <= getLength()-1 :
			"Lock offset is beyond end of document limit";
		if (scriptingState == ScriptingQueueState.READY) {
			assert getProcessedOffset() == getLockOffset()
				: "If ready, there should be no queue region";
		}
		if (scriptingState == ScriptingQueueState.FORWARDS) {
			assert getProcessedOffset() < getLockOffset() 
				: "If going forwards, there should be a queue region after processed point";
		}
		if (scriptingState == ScriptingQueueState.BACKWARDS) {
			assert getProcessedOffset() > getLockOffset()
				: "If going backwards, there should be a queue region before processed point";
		}
	}

	/**
	 * Broadcasts a change in offset (given old and new offset values).
	 * Should be called by any set___Offset() methods
	 * @param offset the new offset
	 * @param old the previous offset
	 * @deprecated
	 */
	@Deprecated void processOffset(int offset, int old, boolean updatemarkers) {
		// da: actually it can happen that the document has got shorter in the meantime.
		// TODO: enable these assertions to find out where/why.
		// TODO: junk this method, instead.
		//assert offset >= -1 && offset <= getLength()-1 :
		//	"Offset out of range: [-1..]" + (getLength()-1) + "]:" + offset;
		//assert old >= -1 && offset <= getLength()-1 :
		//	"Old offset out of range: [-1..]" + (getLength()-1) + "]:" + old;
		int low = Math.min(old, offset)+1;
		int high = Math.max(old, offset)+1;
		int length = Math.min(getLength()-1,high)-low;
		// This was a bug in previous version, which assumed that higher value
		// might exceed end of document but not the lower one!
		// Really this means that the processOffset should be discarded because
		// the positions are now wrong, but let's send a notification for the
		if (low <= getLength()-1 && length>0) {
			if (updatemarkers) {
				updateMarkerPositions();
			}
			partitionChangeBroadcast(low, length);
		}
	}

	MutableObject lastPartitionChangeBroadcast = new MutableObject(null);

	/**
	 * Used for events that change the locked/processed nature of regions
	 * but otherwise do not change the parse tree;
	 * now this can be run in fg, launches bg as necessary (-AH)
	 * @param ooffset the offset of the change
	 * @param olength the length of the change
	 */
	protected void partitionChangeBroadcast(int ooffset, int olength) {
		if (!ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_DEBUG_USE_MARKER_ANNOTATIONS)) {
			oldpartitionChangeBroadcast(ooffset,olength);
			return;
		}
		// New method: we still need to send an event to update in the outline view.
		// Could we make this a custom event/listener instead somehow?
		try {
			//DocumentEvent e1 = new ScriptManagementDocumentEvent(ProofScriptDocument.this, 
			//		ooffset, olength, get(ooffset,olength));
			DocumentEvent e1 = new ScriptManagementDocumentEvent(ProofScriptDocument.this, 
					0, this.getLength(), get(0,this.getLength()));
			
			doFireDocumentChanged(e1);
		} catch (BadLocationException e) {
			// do nothing
		}
	}
	
	protected void oldpartitionChangeBroadcast(int ooffset, int olength) {
		if (olength == 0) {
			return;
		}
		synchronized (lastPartitionChangeBroadcast) {
			//use mutable object so broadcast is always most recent info, and subseqent are dropped
			Position p = (Position) lastPartitionChangeBroadcast.get();
			if (p==null) {
				if (ooffset < 0 || olength <= 0) {
					if (ProofGeneralPlugin.debug(this)) { // DEBUG
						ReflectionHelper.printCaller("Invalid partition change (@"+ooffset+"+"+olength+").", 4);
					}
					return;
				}
				lastPartitionChangeBroadcast.set(new Position(ooffset, olength));
			} else {
				if (p.offset+p.length < ooffset+olength) {
					p.length = ooffset+olength - p.offset;
				}
				if (ooffset<p.offset) {
					p.length = p.offset + p.length - ooffset;
					p.offset = ooffset;
				}
			}
		}
		new DisplayCallable("ProofScriptDoc.partitionChangeBroadcast") {
			@Override
            public Object run() {
				final int offset; final int length;
				synchronized (lastPartitionChangeBroadcast) {
					Position p = (Position) lastPartitionChangeBroadcast.get();
					if (p==null) {
						return null;
					}
					offset = p.getOffset();
					length = p.getLength();
					lastPartitionChangeBroadcast.set(null);
				}
				String text = "";
				try {
					// da: THE TEXT MIGHT HAVE CHANGED BEFORE WE GET HERE?!
					// SHOULD CACHE ABOVE
					text = ProofScriptDocument.super.get(offset, length);
				} catch (BadLocationException e) {
					e.printStackTrace();
				}
				// FIXME: this doesn't set an event in the master document fOriginalEvent field
				// which gives NPE when partitioning (erroneously, anyway) changes
				DocumentEvent e1 = new ScriptManagementDocumentEvent(ProofScriptDocument.this, offset, length, text);
				//fireDocumentAboutToBeChanged(e1);
				// da: this gives NPEs now in projection document:
//				at org.eclipse.jface.text.projection.ProjectionDocument.normalize(ProjectionDocument.java:661)
//				at org.eclipse.jface.text.projection.ProjectionDocument.masterDocumentAboutToBeChanged(ProjectionDocument.java:711)
//  reason seems to be that a field should be initialised in the projection document (fOriginalEvent) but hasn't
//  maybe projection document should be a listener on us?  Needs further investigation
// Problem may go away if we can remove/clean the fake partition changing
				try {
					// DA: this dummy event is really needed when the partitioning changes
					fireDocumentAboutToBeChanged(e1);
				} catch (Exception e) {
					// Print the stack trace message if in debug mode
					if (ProofGeneralPlugin.debug(this)) {
						e.printStackTrace();
					}
				}

				// TODO this should probably also be a dummy to save on recomputing the parse tree
				DocumentPartitioningChangedEvent e2;
				//fDocumentPartitioningChangedEvent= new DocumentPartitioningChangedEvent(this);
				e2 = new DocumentPartitioningChangedEvent(ProofScriptDocument.this);
				String[] ps = getPartitionings();
//				int partitioningPos = 0;
				for (String partitioning : ps) {
					e2.setPartitionChange(partitioning, offset, length);
					// da: shouldn't the partition change be exactly the region of the existing
					// partitonings????  This is my guess at better code:
					// ah: re-instated the line above, instead of the line below;
					// as the string partionining is e.g. "__light_pg_partitioning",
					// using the length of that string makes little sense
					// (it doesn't seem to make a difference either way, so i'm guessing offset+length are ignored...)
//					if (offset >= partitioningPos) {
//						e2.setPartitionChange(partitioning, partitioningPos, partitioning.length());
//					}
//					partitioningPos += partitioning.length();
				}
				//System.err.println(General.makeDateString()+"  partitionChangeBroadcast, firing aboutToChange");  //one of these moves the cursor to the end of the region if it is inside it  -AH

				fireDocumentPartitioningChanged(e2);
				//System.err.println(General.makeDateString()+"  partitionChangeBroadcast, firing doChanged");  //not sure they're necessary
				doFireDocumentChanged(e1);

				return null;
			}
		}.runDisplay();
	}



	/**
	 * Lock the document upto the end of the next command.
	 * Technically, this should be called command<i>Queued</i>.
	 */
	public synchronized void commandSent(CmdElement command) {
		Position posn;
		if (command == null) {
			//posn = findNextCommand().getPosition();
			return; //TODO something smarter?
		}
		posn = command.getPosition();
		try {
			int newoffset = posn.offset+posn.length - 1;
			if (getLockOffset()<newoffset) {
				//only do if it's not already locked this far	[da: how could it be already locked?]
				// Clean any error markers in the queued region.
				ProofScriptMarkers.cleanErrorMarkers(this, getLockOffset()+1, newoffset-(getLockOffset()+1));
				setQueueForwards(newoffset);
			}
		} catch (Exception e) { e.printStackTrace();}
	}

// Old undo code: CLEANUP
//	/**
//	 * Set the processed offset upto the end of the given command
//	 * @param command
//	 */
//	public synchronized void commandProcessed(DocElement command) {
//		Position posn = command.getPosition();
//		//perhaps someone else has updated it [da: HOW? WHY?]
//		if (posn.offset+posn.length-1>getProcessedOffset()) {
//			setQueueForwardsAdvance(posn.offset + posn.length-1);
//		} else {
//			// check this to be safe (shouldn't be necessary)
//			if (getLockOffset()<getProcessedOffset()) {
//				//should update the lock any time we updated the processed region, if necessary
//				setProcessedOffset(getProcessedOffset());
//				//System.out.println("process forces setting lock offset "+getProcessedOffset());
//			}
//		}
//
//	}

	/**
	 * Consider the command given as having been processed: either done or
	 * undone.  Change queue region appropriately.
	 * @param command
	 * @throws ProofScriptDocumentException
	 */
	public synchronized void commandSucceeded(CmdElement command) throws ProofScriptDocumentException {
		checkState();
		assert command.getPosition()!=null : "Position should always be set, it was missing for: " + command.getText();
		if (scriptingState == ScriptingQueueState.FORWARDS) {
			setQueueForwardsAdvance(command.getEndOffset());
		} else if (scriptingState == ScriptingQueueState.BACKWARDS) {
			setQueueBackwardsRetract(command.getPosition().getOffset()-1);
		} else {
			//setProcessedOffset(command.getPosition().getOffset()-1); // assume undo, but this is buggy
			// FIXME: exception here leads to loop, why??
			throw new ProofScriptDocumentException("Command to process without queue region in document.");
		}
		checkState();
	}
	
	public synchronized void commandFailed(CmdElement command) {
		checkState();
		assert command.getPosition()!=null : "Position should always be set, it was missing for: " + command.getText();
		setQueueEmptyReady();
	}

// da: old code CLEANUP	
//	/**
//	 * Respond to a command being undone.
//	 * Unlock the last command & upto the end of the one before.
//	 */
//	public synchronized void commandUndone(DocElement command) {
//		TypedPosition posn = command.getPosition();
//		if (posn == null) {
//			assert false : "Position should always be set, it was missing for: " + command.getText();
//			return;
//		}
//		int offset = posn.getOffset();
//		if (offset > getLockOffset()) {
//			return; // this command is already undone  (FIXME: shouldn't be...)
//		}
//// CLEANUP		
////		if (!ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_DEBUG_USE_NEW_UNDO)) {
////			// Old method skips here to match what the SM does.
////			// New method is more exact and should call us for every kind of
////			// command, so no need to do this.
////			DocElement pc = null;
////
////			pc = findPrevious((Constants.SEND_COMMAND_STEPS_THROUGH_COMMENTS ? PGIPSyntax.ANYITEM : PGIPSyntax.COMMAND),
////					offset);
////			if (pc==null) {
////				offset=0; // no more commands, so unlock all.
////			} else {
////				Position pposn = pc.getPosition();
////				offset = Math.min(pposn.getOffset()+pposn.getLength(),offset);
////			}
////		}
//		setQueueBackwardsRetract(offset-1);
//		if (command.getName().equals(PGIPSyntax.ABORTFILE)) {
//			clearActiveForScripting(); // will need to mirror in SM
//		}
//	}


	/**
	 * Initialise fields in a ProofScriptDocument.
	 */
	protected void init(String title, PGIPSyntax syntax,
			ProverSyntax proverSyntax, Prover prover, IResource resource) {
		this.title = title;
		this.syntax = syntax;
		this.proverSyntax = proverSyntax;
		this.resource = resource;
		this.activeForScripting = false;
		this.prover = prover;
		super.init(prover.getSymbols());
	}

	/**
	 * Nullary constructor required for factory creation.
	 */
	public ProofScriptDocument() {
		super();
		documentTree = new ProofScriptDocumentTree(this);
		ScriptManagementPosition[] positions = { documentTree.getParsedRegion(), 
					fProcessedRegion, fQueueRegion };
		ScriptManagementPosition.init(this, positions);
	}


	/**
	 * Return true if the given edit should be allowed, according to the document
	 * watermarks and the preference setting for editing of processed regions.
	 * @param pos offset of the start of the edit
	 * @param length length of the text being replaced
	 * @return true if the edit can be allowed
	 */
	public synchronized boolean isEditAllowed(int pos, int length, String newText) {
		if (ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_ALLOW_EDITING_PROCESSED)) {
		   // If we allow editing of processed regions, the edit must not intersect
		   // the queue region, if any.
			// TODO test edits at end of processed region; do they go into processed region or queued region?
			// (does it matter?)
		   return fLocked || pos > getLockOffset() || pos+length <= getProcessedOffset();
		}
		// If we don't allow editing of processed regions, the edit must be in
		// an unlocked region.
//		return !fLocked && pos > getLockOffset();
		// furthermore they cannot be immediately after the locked region unless they start with whitespace
		// (otherwise introduces parse ambiguity)
		try {
	        return !fLocked && (pos > getLockOffset()+1 ||
	        		(getLockOffset()<0) ||
	        		(pos > getLockOffset() && 
	        				((newText.length()>0 && Character.isWhitespace(newText.charAt(0))) ||
	        				 (newText.length()==0 && (getLength()==pos+length ||
	        						 Character.isWhitespace(getChar(pos+length)))))
	        		));
        } catch (BadLocationException e) {
	        //shouldn't happen
	        e.printStackTrace();
	        return false;
        }
	}

	/**
	 * Substitutes the given text for the specified document range.
	 * Sends a <code>DocumentEvent</code> to all registered <code>IDocumentListener</code>.
	 * IMPLEMENTS: Blocks any change that involves locked regions.
	 * IMPLEMENTS: Performs symbol substitutions (but not typing shortcuts), if symbol support is set.
	 * IMPLEMENTS: Detects shortcuts when preceded and followed by a space.
	 *
	 * @param pos the document offset
	 * @param length the length of the specified range
	 * @param text the substitution text
	 * @exception BadLocationException if the offset is invalid in this document
	 *
	 * @see DocumentEvent
	 * @see IDocumentListener
	 */
	@Override
    public void replace(int pos, int length, String text) throws BadLocationException {
		// block this change silently (as suggested by Eclipse), if not allowed
		// ah: a verify listener is now set by ProofScriptEditor, so calls should not even come here if edit isn't allowed;
		// seems a cleaner way to do it, also means that the cursor won't get moved on blocked edits
		if (isEditAllowed(pos, length, text)) {
			text = symbolise(text);
			// TODO: we could be a bit more lax here: e.g. when editing inside comment/whitespace
			documentTree.setParseRegionForEditAtOffset(pos); // may have to re-compute the parse tree
			//moved to do the replacement after clearing the doc tree
			super.replace(pos,length,text);
		}
	}

	
	/**
	 * Set to true in order to bypass the controls that prevent editing of locked regions.
	 * It also switches off the normal symbol replacement performed in replace.
	 * Used by the symbol support.
	 * The default is false.
	 * If set to true, it should be set back to false as soon as possible.
	 */
	// da: doesn't seem like this should be a field.
	// boolean overrideLock = false;

	/**
	 * From IDocument#set
	 * Replaces the content of the document with the given text.
	 * Sends a <code>DocumentEvent</code> to all registered <code>IDocumentListener</code>.
	 * If the document is even partly locked, this call will block the replacement.
	 * The text will have symbol conversion applied if the document is using symbols.
	 * The scripting state of the document is reset.  WARNING: this risks loss
	 * of synchronisation if the document is being processed.
	 *
	 * @param text the new content of the document
	 *
	 * @see DocumentEvent
	 * @see IDocumentListener
	 */
	// TODO: watch sync more carefully: assertion or error message here.
	@Override
    public void set(String text) {
		set(text, false);
		resetDocument();
	}


	/**
	 * Change whether or not the document is using symbols.
	 * This is a low-level method: it will destroy the watermark positions
	 * and so should not be called on an active document which has watermarks.
	 * This is considered client obligation, so we check with an assertion.
	 */
	 @Override
    public void setUsingSymbols(boolean newusingSymbols) {
	    assert !activeForScripting : "Document should not have symbol change while being processed!";
		super.setUsingSymbols(newusingSymbols);
		//try {
			// Try
			partitionChangeBroadcast(0, getLength()-1);
		//} catch (BadLocationException ex) {
			// shouldn't happen.
		//}
	}

	/*
	 * Return a subrange of the contents of the document.
	 * The text will be converted from Unicode symbols into
	 * ASCII if the document is using symbols.
	 *
	 * @see org.eclipse.jface.text.IDocument#get(int, int)
	 */
	/*
	public String get(int pos, int length) throws BadLocationException {
		// FIXME: map positions
		String presentationtext = super.get(pos,length);
		if (usingSymbols) {
			return desymbolise(presentationtext);
		} else {
			return presentationtext;
		}
	}*/



	/**
	 * Set the document text, possibly converting to symbols
	 * from the raw ASCII argument.  Will only replace if 
	 * document is completely unlocked or overrideLock is true.
	 * @param text
	 * @param overrideLock
	 */
	private void set(String text, boolean overrideLock) {
		if (overrideLock || isEditAllowed(0, getLength(), text)) {
			super.set(text);
			documentTree.setParseRegionForEditAtOffset(0); // will have to re-compute the parse tree
		}
	}


	// ---------------------- process position and finalised locking --------------------------------
	/**
	 * Checks if the document has been locked as being finalised and fully processed.
	 * FIXME da: we should be careful: isLocked should mean that no more changes should be allowed
	 * anywhere (even at end of document);
	 * @return true if this document is currently locked
	 */
	public boolean isLocked() {
		return fLocked;
	}

	/**
	 * Checks if the document has been completely processed, according to the offsets.
	 * (If the document has been fully processed, it might be locked automatically,
	 *  providing it has reached the file-level state).
	 * @return true if the offset is at the document's end, or if all that is left
	 * is whitespace; false otherwise.
	 */
	public synchronized boolean isFullyProcessed() {
		final int l = getLength() - 1;
		if (getProcessedOffset() >= l) {
			return true;
		}
		try {
			return super.get(getProcessedOffset()+1, l-getProcessedOffset()).matches("\\s*");
		} catch (BadLocationException e) {
			if (ProofGeneralPlugin.debug(this)) {
				e.printStackTrace();
			}
			return false;
		}
	}

	/**
	 * Check to see whether any commands from the document have been processed.
	 * (Documents which are fully unprocessed are in a good state for deactivating).
	 * @return true if the document is fully unprocessed.
	 */
	public synchronized boolean isFullyUnprocessed() {
		return (getLockOffset() < 0);
	}


	// ------------------------ active scripting flag -----------------------------------

	/**
	 * Checks if this document is active for scripting.
	 */
	public boolean isActiveForScripting() {
		return activeForScripting;
	}

	/**
	 * Sets this document to be active for scripting.  No effect if
	 * it is locked (i.e. marked fully processed) or already active.
	 */
	public void setActiveForScripting() {
		if (!fLocked && !activeForScripting) {
			if (ProofGeneralPlugin.debug(this)) {
				System.out.println("PSD: setting document " + getTitle() + " active for scripting");
			}
			activeForScripting = true;
			updateProperties();
		} else {
			if (fLocked && ProofGeneralPlugin.debug(this)) {
				System.err.println("PSD: ignored attempt to set locked document " + getTitle() + " active for scripting");
			}
		}
	}

	/**
	 * Clear this document's active for scripting flag.  No effect if
	 * it is partially processed or already inactive.
	 * This maintains the invariant that
	 *   !activeForScripting ==> isFullyProcessed || isFullyUnprocessed
	 */
	public void clearActiveForScripting() {
		if (activeForScripting && (isFullyProcessed() || isFullyUnprocessed())) {
			if (ProofGeneralPlugin.debug(this)) {
				System.out.println("PSD: clearing document's " + getTitle() + " active for scripting status");
			}
			activeForScripting = false;
			// TODO: clean up markers mebbe
		} else
			if (activeForScripting && ProofGeneralPlugin.debug(this)) {
				System.out.println("PSD: ignoring attempt to clear scripting flag for document " +getTitle());
			}
	}

	private static final String cn = ProofScriptDocument.class.getCanonicalName();
	/** The key to allow retrieval of the resource which underpins this document. */
	public static final QualifiedName IsActive = new QualifiedName(cn, "isActive");
	public static final QualifiedName IsLocked = new QualifiedName(cn, "isLocked");

	/**
	 * Updates the decorator for a change in resource status.
	 * Should be called on any change which might not be reflected in resources.
	 * TODO: check this is called enough
	 */
	private void updateProperties() {
		if (resource == null) {
			return;
		}
		/*
		try {
			//System.out.println("updating resource properupdateProperties to "+isActiveForScripting()+", "+isLocked());
			// Fda: I think these properties need to be managed by the ScriptManager, in fact:
			// it is the one that maintains the list of fully locked documents in the controlledDocuments
			// class, which is where these resources should be set/removed.  Ideally the
			// document model shouldn't need to know it's underlying resource.
			// The state of a processed document depends on the session.
			resource.setSessionProperty(IsActive, isActiveForScripting());
			resource.setSessionProperty(IsLocked, isLocked());
		} catch (CoreException c) {
			System.err.println("Couldn't set properties on the resource.");
		} */

		// TODO: find a more modern way to force decoration or resource updates here
		//		...sending an empty ResourceChange causes an exception...
		// FIXME: probably we should have resource change events and not block them in
		//    DummyDocumentEvents.

		if (!ProofGeneralPlugin.isShuttingDown()) {
			// da: don't do this during shutdown, leads to SWT disposed widget exceptions.
			WorkbenchPlugin.getDefault().getDecoratorManager().updateForEnablementChange();
		}

		//ActiveScriptDecorator.forceUpdate();

		/*
		try {
			if (marker != null) marker.delete();
			marker = resource.createMarker(IMarker.MARKER);
			marker.setAttribute(IMarker.MESSAGE, "Document Updated.");
		} catch (CoreException e) {
			e.printStackTrace();
		} */
	}

	// Handy if toString returns resource (while we have it...).
	@Override
    public String toString() {
		return "Document on " + resource.getName() + "[" + super.toString() + "]";
	}


	public int skipWhiteSpaceBackwards(int offset) {
		if (offset == 0) {
			return offset;
		}
		try {
			boolean isspace;
			do {
				char c = getChar(--offset);
				isspace = c==' ' || c=='\n' || c=='\r' || c=='\t';
			} while (isspace && offset>0);
			return offset;
		} catch (BadLocationException e) {
			return offset;
		}
	}

	
	/**
	 * @return true if we can queue commands
	 */
	public boolean canQueueForwards() {
		return scriptingState == ScriptingQueueState.READY ||
		       scriptingState == ScriptingQueueState.FORWARDS;
	}

	/**
	 * @return true if we can queue undo commands
	 */
	public boolean canQueueBackwards() {
		return scriptingState == ScriptingQueueState.READY ||
		       scriptingState == ScriptingQueueState.BACKWARDS;
	}

//	/**
//	 * Skip space backwards to the start of the line (i.e. newline/carriage return).
//     * @param offset
//     * @return first non-whitespace position, if any, on same line before given offset
//     */
//    private int skipSpaceBackwards(int offset) {
//	    char c;
//	    try {
//	    	while (--offset >= 0 && 
//	    		   (c = get(offset,1).charAt(0))<=32 && c!=13 && c!=10) { 
//	    		//
//	    	}
//	    	offset++;
//	    } catch (BadLocationException ex) {
//	    	//
//	    }
//	    return offset;
//    }
	
    /**
     * Skip whitespace characters (codes less than or equal to 32)
     * forwards until we hit a non whitespace or end of file.
     * @param offset
     * @return the offset of the first non-whitespace character after the parameter offset,
     * or the parameter if it points to a non-whitespace character, or the offset after
     * the offset *after* the last position in the file.
     */
    public int skipSpacesForward(int offset) {
    	try {
    		while (get(offset,1).charAt(0)<=32) {
    			offset++;
    		}
    	} catch (BadLocationException ex) {
    		offset=getLength();
    	}
    	return offset;
    }

    /**
     * Skip whitespace characters (codes less than or equal to 32)
     * forwards until we hit a non whitespace or end of file.
     * @param offset
     * @return the offset of the first non-whitespace character before the parameter offset,
     * or the parameter if it points to a non-whitespace character, or 0.
     */
    public int skipSpacesBackward(int offset) {
    	try {
    		while (get(offset,1).charAt(0)<=32 && --offset >= 0) {
    			//
    		}
    	} catch (BadLocationException ex) {
    		offset=0;
    	}
    	return offset;
    }
    
    /**
     * Skip whitespace characters forwards until we hit a non whitespace or end of file.
     * @param offset
     * @return the offset of the start of the next line if only whitespace appears after the given
     * offset on the same line, or the parameter itself if it points to a non-whitespace character.
     */
    public int offsetSkipSpacesNextLine(int offset) {
    	try {
    		while (++offset < getLength() && 
    				get(offset,1).charAt(0)==32) { 
    			//
    		}
    		if (get(offset,1).charAt(0)=='\n') { // FIXME LINEEND
    			return Math.min(offset+1,getLength()-1); // next line
    		}
    		offset--;
    	} catch (BadLocationException ex) {
    		//
    	}
    	return offset;
    }

	/**
     * @return the documentTree
     */
    public ProofScriptDocumentTree getDocumentTree() {
    	return documentTree;
    }

	/**
     * @param scriptingState the scriptingState to set
     */
    public void setScriptingState(ScriptingQueueState scriptingState) {
    	this.scriptingState = scriptingState;
    }
    
    
    /**
     * Temporary hook used in ProofScriptDocumentTree.  We need to refactor
     * the events used for parse tree changes to use our own, ideally,
     * unless any standard views would be changed by DocumentEvents 
     * that don't really correspond to edits of document...
     * @param e
     */
    void ourDoFireDocumentChanged(final DocumentEvent e) {
    	/* should be in display thread, as it updates the text history of the doc */ 
    	new DisplayCallable() {
    		@Override
            public Object run() throws Exception {
    			doFireDocumentChanged(e);
    			return null;
    		}
    	}.runDisplay();    	
    }

}
