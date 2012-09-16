/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jun 23, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */

package ed.inf.proofgeneral.preferences;

/**
 * Preference names for general (prover independent) preferences.
 * These are the strings used in the associated XML preference pages.
 */
public class PreferenceNames {

	public static final String PREF_FILE_ASSOCIATIONS = "File Associations";
	public static final String PREF_TIME_OUT = "Prover response time-out";
	/** Root Help URL - preset to http://proofgeneral.inf.ed.ac.uk/wiki/ - now used only as-is */
    public static final String PREF_HELP_URL = "Help URL";
	public static final String PREF_BUGREPORT_URL = "Bug Report URL";
	public static final String PREF_BLOCKED_MESSAGES = "Blocked Messages";
	// da: the previous single setting was simpler and better UI, but lets try with this for debugging for now.
    public static final String PREF_SYMBOL_SUPPORT = "Use symbols in documents";
	public static final String PREF_OUTPUT_SYMBOL_SUPPORT = "Use symbols for prover output";
	public static final String PREF_SYMBOL_SHORTCUTS = "Use symbol shortcuts while typing";
	public static final String PREF_ENABLE_SCRIPTING = "Enable Scripting";
	public static final String SETTING_LINEEND = "LINEEND";
	public static final String PREF_GRAB_PROVER_KNOWLEDGE = "Gather prover knowledge";
	public static final String PREF_MAKE_BOOKMARKS = "Make bookmarks for theory elements";
	public static final String PREF_USE_GATHERING_PARSER =
    "Use gathering parser (speed workaround; only good for provers with command terminators";
	public static final String PREF_ALLOW_EDITING_PROCESSED =
    "Allow editing of processed text";
	public static final String PREF_USE_PGIP_INTERRUPTS = "use PGIP interrupts";
	
	// NB da: debug preferences have to have "DEBUG " as prefix to be recognised as such
	public static final String PREF_DEBUG = "DEBUG DEBUG";
    public static final String PREF_DEBUG_SHALLOW_OUTLINE =
    	"DEBUG Outline shows command positions";
	public static final String PREF_DEBUG_USE_NEW_UNDO =
		"DEBUG Use new command queueing code for do/undo (INCOMPLETE)";  // DEFUNCT, CLEANUP
	public static final String PREF_DEBUG_USE_MARKER_ANNOTATIONS =
		"DEBUG Use marker annotations for scripting state regions (BUGGY)";
	public static final String PREF_LOG_EVENTS_FIRING = 
		"DEBUG Log Event Firing";
	public static final String PREF_LOG_EVENT_RUNNING = 
		"DEBUG Log Event Running";
	public static final String PREF_LOG_PROVER_IO = 
		"DEBUG Log Prover I/O";
	
	public static final String PREF_USE_FOLDING = "Enable folding (experimental)";
	public static final String PREF_SKIP_FOLDED = "Skip over folded elements";
	public static final String PREF_FOLD_COMMENTS= "Initially fold comments";
	public static final String PREF_FOLD_PROOFS = "Initially fold proofs";
	public static final String PREF_FOLD_ITEMS = "Initially fold theory elements";
	/** Style sheet to convert PGML to plain text */
    public static final String PLAINTEXT_STYLESHEET_FILE = "config/pgml-plaintext.xsl";
	public static final String PREF_PARSE_AUTO = "Parse automatically (experimental)";
	public static final String PREF_PROVE_AUTO = "Prove automatically (experimental)";

    /**
     * This class is not intended to be instantiated.
     */
    private PreferenceNames() { }

}
