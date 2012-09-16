/*
 *  $RCSfile: ProofScriptMarkers.java,v $
 *
 *  Created on 24 Map 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.document;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.Position;
import org.eclipse.ui.texteditor.MarkerUtilities;

import ed.inf.proofgeneral.pgip.Location;
import ed.inf.proofgeneral.pgip2html.Converter;
import ed.inf.proofgeneral.sessionmanager.events.PGIPError;
import ed.inf.utils.datastruct.StringManipulation;

/**
 * Methods for creating, testing and removing markers in proof script documents.
 * 
 * @author Daniel Winterstein
 * @author David Aspinall
 */

// TODO da: I have separated markers from bookmarks, keeping user and
// programmatic settings separate.
// However, markers for theorems and theories still seem heavyweight? I'm not
// sure. Would be good to see whether JDT does it like that, once a file has
// been parsed.

// TODO: almost every method here uses a proof script document.  Might be cleaner
// to associate with document (e.g. inner class)

public class ProofScriptMarkers {

	/** Root marker type. All user-level marker types all inherit from this. */
	public static final String PGMARKER = "ed.inf.proofgeneral.pgmarker";
	/** Problem marker type. */
	public static final String PGPROBLEM_MARKER = "ed.inf.proofgeneral.pgproblem";
	/** Task marker type. */
	public static final String PGTASK_MARKER = "ed.inf.proofgeneral.pgtask";

	/** Label supertype for recording locations of declarations, used, e.g. for tooltips. */
	public static final String LABEL_MARKER = "ed.inf.proofgeneral.label";
	/** Marker type for theory declarations. */
	public static final String PGTHEORY_MARKER = "ed.inf.proofgeneral.theory";
	/** Prefix in marker messages for theory declarations. */
	public static final String THEORY_PREFIX = "Theory: ";
	/** Marker type for theorem declarations. */
	public static final String PGTHEOREM_MARKER = "ed.inf.proofgeneral.theorem";
	/** Prefix in marker messages for theorem declarations. */
	public static final String THEOREM_PREFIX = "Theorem: ";
	/** Marker type for definitions. */
	public static final String PGDEFN_MARKER = "ed.inf.proofgeneral.defn";
	/** Prefix in marker messages for definitions. */
	public static final String DEFN_PREFIX = "Defn: ";
	/** Message for markers for unfinished proofs. */
	public static final String UNSOLVED_MSG = "Unfinished proof"; 																	// counts

	/** Marker type for jumping to positions, used in scrolling */
	public static final String PG_GOTO_MARKER = "ed.inf.proofgeneral.gototempmarker"; 																						// scrolling

	/** Attribute used to store the tooltip associated with markers. */
	public static final String TOOLTIP = "tooltip";

	/** Scripting state markers all inherit from SCRIPT_REGION. */
	public static final String SCRIPT_REGION = "ed.inf.proofgeneral.scriptregion";
	/** Marker type for unparsed regions. */
	public static final String UNPARSED = "ed.inf.proofgeneral.unparsed";
	/** Marker type for parsed regions. */
	public static final String PARSED = "ed.inf.proofgeneral.parsed";
	/** Marker type for queued regions. Coloured by Queued Proof annotation. */
	public static final String QUEUED = "ed.inf.proofgeneral.queued";
	/** Marker type for processed regions. Coloured by Processed Proof annotation. */
	public static final String PROCESSED = "ed.inf.proofgeneral.processed";
	/** Marker type for outdated regions. Coloured by Outdated Proof annotation. */
	public static final String OUTDATED = "ed.inf.proofgeneral.outdated";
	
	/**
	 * Creates a new marker for the given resource (or returns an existing
	 * equivalent marker if one exists).
	 * 
	 * @param resource
	 *            resource on which to create marker
	 * @param posn
	 *            position of the marker or null for a marker on the whole
	 *            document
	 * @param message
	 *            short message for the marker
	 * @param markerType
	 *            type of the marker
	 * @param tooltip
	 *            detailed message associated with the marker (ignored at
	 *            present)
	 */
	@SuppressWarnings("boxing")
    public static void addMarker(final IResource resource, Position posn, int line, String message,
	        String markerType, String tooltip, String level, int levelval) {
		if (resource == null) {
			return;
		}
		if (markerType == null) {
			markerType = PGMARKER;
		}
		final Map<String, Object> attributes = new HashMap<String, Object>(11);
		if (posn != null) {
			int start = posn.offset;
			int length = posn.length;
			MarkerUtilities.setCharStart(attributes, start);
			MarkerUtilities.setCharEnd(attributes, start + length);
		}
		MarkerUtilities.setMessage(attributes, message);
		if (posn == null && line != -1) {
			MarkerUtilities.setLineNumber(attributes, line);
			// we could maybe pass in location description (could be from PGIP
			// attr descr),
			// except platform suggests it should be *short*, and creates the
			// text
			// below automatically from the line number.
			// attributes.put(IMarker.LOCATION, "line " +
			// Integer.toString(line));
		}
		attributes.put(TOOLTIP, tooltip);
		if (level != null) {
			attributes.put(level, levelval);
		}
		try {
			// synchronized(newMarker) {
			// da: I think really we should be removing markers when they're
			// dead and re-making
			// them --- so no need to search on every add.
			// IMarker e = exists(resource,markerType,attributes);
			// if (e!=null) return e;
			final String fmt = markerType;
			IWorkspaceRunnable r = new IWorkspaceRunnable() {
				// code taken from
				// MarkerUtilities.createMarker(doc.resource,attributes,markerType);
				public void run(IProgressMonitor monitor) {
					try {
						IMarker marker = resource.createMarker(fmt);
						marker.setAttributes(attributes);
						// PGMarkerMethods.newMarker = marker;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			resource.getWorkspace().run(r, null, IWorkspace.AVOID_UPDATE, null);
			// }
			// return newMarker;
			// }
		} catch (CoreException x) {
			// x.printStackTrace(); da: may happen resource was removed, don't
			// fret
			// ErrorUI.getDefault().signalError(x);
			return;
		}
	}

	/**
	 * Creates a new marker for the given document (or returns an existing
	 * equivalent marker if one exists).
	 */
	public static void addMarker(ProofScriptDocument doc, Position posn, int line, String message,
	        String markerType, String tooltip, String level, int levelval) {
		addMarker(doc.getResource(), posn, line, message, markerType, tooltip, level, levelval);
	}

	/**
	 * Add a problem marker for the given document, with the given severity
	 * level. The marker will appear in the Problems view. It has a short
	 * message and a long tooltip.
	 * 
	 * @param doc
	 * @param position
	 * @param msg
	 * @param tooltip
	 * @param severity
	 *            a value for IMarker.SEVERITY marker attribute
	 */
	public static void addProblemMarker(ProofScriptDocument doc, Position position, int line,
	        String msg, String tooltip, int severity) {
		addMarker(doc, position, line, msg, ProofScriptMarkers.PGPROBLEM_MARKER, tooltip,
		        IMarker.SEVERITY, severity);
	}

	/**
	 * Add a marker for a PGIP error message. The marker will appear in the
	 * Problems View and have appropriate severity according to the PGIP
	 * fatality attribute. The resource and location of the marker are
	 * determined either by the PGIP locations attribute in the error, or, if
	 * those are absent, by the given document command element.
	 * 
	 * @param err
	 *            the error: its location field and fatality field control the
	 *            marker
	 * @param cmd
	 *            the document element that generated this error, or null.
	 */
	public static void addErrorMarker(PGIPError err, CmdElement cmd, Converter converter) {
		Location loc = err.location;
		ProofScriptDocument doc = cmd == null ? null : cmd.getProofScript();
		// Markers are only added for commands from documents or errors
		// with explicit locations.
		// FIXME: we want to explicitly *prevent* markers for commands
		// which came from certain commands (e.g., menu, prover knowledge).
		if (cmd != null && (loc.isUseful() || doc != null)) {

			IResource res = loc.getResource();
			Position pos = loc.getPosition();
			int line = loc.getLine();

			// Make the message
			String msg = err.getText();

			// Find a resource: first, any given one; then, document or
			// workspace.
			if (res == null) {
				if (loc.getUri() != null) {
					// We were given a URI, but couldn't map it to a workspace
					// resource.
					msg = msg + "\nError reported for URL: " + loc.getUri();
				}
				pos = cmd.getPosition();
				// TODO:
				// pos.offset = doc.skipSpacesAfter(pos.offset);
				line = -1;
				if (doc != null && doc.getResource() != null) {
					res = doc.getResource();
				} else {
					res = ResourcesPlugin.getWorkspace().getRoot();
				}
			}
			// Symbolise and convert from PGML structured markup
			msg = converter.getPlaintext(msg);
			String shortmsg = StringManipulation.ellipsisTrim(msg,100);

			/*
			 * if (line == -1 && doc != null && pos.offset != -1) { try { line =
			 * doc.getLineOfOffset(pos.offset); } catch
			 * (org.eclipse.jface.text.BadLocationException ex) { // da: don't
			 * fret, it can happen: document may have changed meanwhile //
			 * ex.printStackTrace(); } }
			 */

			// FIXME NB: addMarker in PGMarkerMethods already uses a runnable,
			// but its scope might be wider. It also performs a potentially
			// costly
			// search for duplicate markers which might be avoidable, perhaps.
			// We want to be quick here, get those events out there.
			// FIXME 2: addMarker replaces marker with same location, but if
			// location is on a file/workspace, this is wrong.
			ProofScriptMarkers.addMarker(res, pos, line, msg, ProofScriptMarkers.PGPROBLEM_MARKER,
			        shortmsg, IMarker.SEVERITY, err.fatality.markerSeverity());
		}
	}

	/**
	 * Test whether an equivalent marker already exists for this resource.
	 * 
	 * @param r
	 *            the resource to test
	 * @param mType
	 *            the type of marker to search for (or null for all types)
	 * @return an equivalent marker, if one exists, or null otherwise
	 */
	public static IMarker exists(IResource r, String mType, Map attr) {
		IMarker[] ms;
		Object key;
		Object val;
		if (r == null) {
			System.err.println("PGMarkerMethods.exists: no resource given");
			return null;
		}
		Map iattr;
		try {
			ms = r.findMarkers(mType, false, IResource.DEPTH_ZERO);
			boolean eqFlag;
			for (int i = 0; i < ms.length; i++) {
				iattr = ms[i].getAttributes();
				eqFlag = true;
				for (Iterator j = attr.keySet().iterator(); j.hasNext();) {
					key = j.next();
					val = attr.get(key); // FIXME: WMI efficiency
											// improvements
					if (val == null) {
						if (iattr.containsKey(key) && iattr.get(key) != null) {
							eqFlag = false;
						}
					} else {
						if (!iattr.containsKey(key) || !val.equals(iattr.get(key))) {
							eqFlag = false;
						}
					}
				}
				if (eqFlag) {
					return ms[i];
				}
			}
		} catch (Exception x) {
			x.printStackTrace();
		}
		return null;
	}

	/**
	 * Delete any PG markers that begin in the given range in the given
	 * resource.
	 */
	private static void cleanMarkersSuper(IResource res, int offset, int length, String markersuper) {
		try {
			IMarker[] markers = res.findMarkers(markersuper, true, IResource.DEPTH_INFINITE);
			for (IMarker m : markers) {
				// da: FIXME: this won't work with markers that have unset
				// start/end
				// da: FIXME: it's probably enough to remove markers that
				// *start* in a given range, no?
				// In fact I think yes, because otherwise if document gets
				// shorter this test
				// will fail to delete out of date markers that are no longer
				// fully contained.
				int mstart = MarkerUtilities.getCharStart(m);
				if (offset == -1 || mstart == -1 || (mstart >= offset && mstart < offset + length)) {
					try {
						m.delete();
					} catch (CoreException ex) { // do nothing (should maybe
													// be workbench log)
						// ex.printStackTrace();
					}
				}
			}
		} catch (CoreException e) { // do nothing (should maybe be workbench
									// log)
			// e.printStackTrace();
		}
	}

	private static void cleanMarkersSuper(ProofScriptDocument doc, int offset, int length,
	        String markersuper) {
		if (doc.getResource() != null) {
			cleanMarkersSuper(doc.getResource(), offset, length, markersuper);
		}
	}

	public static void cleanMarkers(ProofScriptDocument doc, int offset, int length) {
		cleanMarkersSuper(doc, offset, length, PGMARKER);
		// cleanMarkersSuper(doc,offset,length,SCRIPT_REGION);
	}

	public static void cleanErrorMarkers(ProofScriptDocument doc, int offset, int length) {
		cleanMarkersSuper(doc, offset, length, PGPROBLEM_MARKER);
		// cleanMarkersSuper(doc,offset,length,SCRIPT_REGION);
	}

	public static void cleanAllMarkers() {
		cleanMarkersSuper(ResourcesPlugin.getWorkspace().getRoot(), -1, -1, PGMARKER);
	}
	
// CLEANUP
// seems unused
//	/**
//	 * Make this marker transient (ie. it will not be saved when Eclipse
//	 * closes).
//	 * 
//	 * @param m
//	 */
//	public static void setTransient(IMarker m) {
//		try {
//			m.setAttribute(IMarker.TRANSIENT, true);
//		} catch (Exception x) {
//			x.printStackTrace();
//		}
//	}
//

}
