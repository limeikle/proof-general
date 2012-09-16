/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jan 7, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */

package ed.inf.proofgeneral.pgip;

import ed.inf.proofgeneral.document.ProofScriptMarkers;

/**
 * The representation of states of objects in the interface.
 * At the moment this doesn't quite match pgip.rnc, the design is 
 * being tweaked.
 * @author da 
 */
public enum ObjState {
	UNPARSED         ("unparsed",false),       // unparsed object, not yet examined
	UNPARSEABLE      ("unparseable",false),    // unparseable object, erroneous
	PARSED           ("parsed",false),         // parsed object
	PROCESSED        ("processed",false),      // processed object
	OUTDATED         ("outdated",false),       // outdated object
	BEING_PARSED     ("being_parsed",true),    // object which is being parsed
	BEING_PROCESSED  ("being_processed",true), // object which is being processed
	BEING_UNDONE     ("being_undone",true);    // object which is being undone

	private final String pgipname;   // PGIP attribute name
	private final boolean busy;      // We're busy, heading towards the given state

	private ObjState (String name,boolean busyflag) {
		pgipname = name.intern();
		busy = busyflag;
	}

	public boolean isBusy() {
		return busy;
	}

	/**
	 * Convert the given PGIP objstate name into an ObjState value.
	 * @param str
	 * @return the ObjState value or null if the argument was null or an invalid PGIP objstate name.
	 */
	public static ObjState fromString (String str) {
		if (str != null) {
			String testf = str.intern();
			for (ObjState s : ObjState.values()) {
				if (testf == s.pgipname) {
					return s;
				}
			}
		}
		return null;
	}

	/**
	 * Return a marker type name for representing the given object state.
	 * Different marker types have different visual representations by their
	 * marker annotation specifications.  
	 * @return the name of a marker type from PGMarkerMethods, or null if none appropriate.  
	 */
	// NB: could be implemented per-enum with enum abstract method pattern, although
	// that makes Enum definition more cluttered IMO.
	public String markerType(){
		if (isBusy()) {
			return ProofScriptMarkers.QUEUED;
		}
		switch(this) {
        case UNPARSEABLE: return null; // should be decorated by problem marker 
        case PARSED:      return ProofScriptMarkers.PARSED;
        case PROCESSED:   return ProofScriptMarkers.PROCESSED;
        case OUTDATED:    return ProofScriptMarkers.OUTDATED;
        default:          assert false : "invalid Enum input" + this.toString();
                          return null;
        }
	}
}
