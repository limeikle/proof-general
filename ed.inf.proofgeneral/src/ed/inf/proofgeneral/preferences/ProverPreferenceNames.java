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
 * Preference names for prover-specific preferences.
 * These are the strings used in the associated XML preference pages.
 */
public class ProverPreferenceNames {

	/** Command to start main PGIP-enabled prover process  */
	public static final String PREF_PROVER_START_COMMAND = "Start Command";
	/** Whether to use sockets in connection */
	public static final String PREF_USE_SOCKETS = "Use Sockets";
	/** Host address for a socket connection */
	public static final String PREF_HOST_ADDRESS = "Host Address";
	/** Port for a socket connection */
	public static final String PREF_PORT = "Port";
    
	/** Default lexical syntax file (<lexicalsyntax> messages overrides) */
	public static final String PREF_LEXICAL_SYNTAX_FILE = "Lexical Syntax File";
	/** Default keywords for highlighting (<lexicalsyntax> overrides) */
	public static final String PREF_KEYWORDS_FILE = "Keywords File";
	/** Prover-specific style sheet file for rendering PGML to HTML */
	public static final String PREF_STYLESHEET_FILE = "Stylesheet File";
    /** Default symbol table for prover, overriden by project-specific one. */
    public static final String PREF_DEFAULT_SYMBOL_TABLE = "Default Symbol Table";
	
	/** Shell command to setup a project directory: %PROJECTNAME% and %PROJECTDIR$ substituted. */
	public static final String PREF_PROVER_PROJECT_SETUP_COMMAND = "Project Setup Command";
	/** Batch build command for building a single file */
	public static final String PREF_PROVER_FILE_BATCH_BUILD_COMMAND = "Batch Build Command";
	/** Batch build shell command for building a whole project. */
	public static final String PREF_PROVER_PROJECT_BATCH_BUILD_COMMAND = "Project Batch Build Command";
	
	/** String that must appear in preference names (and only those names) for internal prover preferences (set with <setpref>).
	 *  The preference must have the form "<ProverName> Config <PreferenceName>" */
    public static final String PROVER_INTERNAL_PREFERENCE_NAME_TAG = " Config ";

    /** Preference for interrupt command */
	public static final String PREF_INTERRUPT_COMMAND = "Interrupt Command";
}
