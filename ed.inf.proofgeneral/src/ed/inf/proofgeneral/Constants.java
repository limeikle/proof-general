/*
 *  $RCSfile: Constants.java,v $
 *
 *  Created on 12 Sep 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral;

import java.nio.charset.Charset;

/**
 * General constants.
 * @author Daniel Winterstein
 */
public final class Constants {

	/** UTF-8: all configuration files must be stored in this format for cross-platform compatibility.	 */
	public static final Charset UTF8 = Charset.forName("UTF-8");
	
    /**
     * Used when detecting that a symbol or typing shortcuts has been typed.
     */
    public static final int MAX_SYMBOL_LENGTH = 30;
    
	/** The PGIP version we support */
	public static final String PGIP_VERSION_SUPPORTED = "2.1";

    /**
     * @param proverName
     * @return the preference page identifier for the given prover name.
     * This must match the identifier used in the extension 
     * org.eclipse.ui.preferencePages
     */
    public static String systemPreferencesPageIdFor(String proverName) {
    	return "ed.inf.proofgeneral.prefpages." + proverName + "Prefs";
    }

	/** The filename of automatically-generated symbol tables */
    public static final String PROJECT_SYMBOL_TABLE_NAME = ".projectsymbols.sym";

	public static final String ACTIVATE_ACTIONID = "ed.inf.proofgeneral.actions.retarget.ActivateAction";
	public static final String SEND_ACTIONID = "ed.inf.proofgeneral.actions.retarget.SendAction";
	public static final String RESTART_ACTIONID = "ed.inf.proofgeneral.actions.retarget.RestartAction";
	public static final String GOTO_ACTIONID = "ed.inf.proofgeneral.actions.retarget.GotoAction";
	public static final String SENDALL_ACTIONID = "ed.inf.proofgeneral.actions.retarget.SendAllAction";
	public static final String INTERRUPT_ACTIONID = "ed.inf.proofgeneral.actions.retarget.InterruptAction";
	public static final String UNDO_ACTIONID = "ed.inf.proofgeneral.actions.retarget.UndoAction";
	public static final String UNDOALL_ACTIONID = "ed.inf.proofgeneral.actions.retarget.UndoAllAction";
	public static final String SYMBOLS_ACTIONID = "ed.inf.proofgeneral.actions.retarget.ToggleSymbolsAction";
	public static final String PARSE_ACTIONID = "ed.inf.proofgeneral.actions.retarget.ParseDocAction";
	public static final String ENTERCOMMAND_ACTIONID = "ed.inf.proofgeneral.editor.actions.retargeted.EnterCommandAction";

	/** Return an array of the retarget action names, which are class names and action identifiers. */
	private static final String[] retargetActionIds = {
		ACTIVATE_ACTIONID, SEND_ACTIONID, RESTART_ACTIONID,
		GOTO_ACTIONID, SENDALL_ACTIONID, INTERRUPT_ACTIONID, UNDO_ACTIONID, UNDOALL_ACTIONID,
		SYMBOLS_ACTIONID, PARSE_ACTIONID, ENTERCOMMAND_ACTIONID };

	public static final String[] getRetargetActionIds() {
		return retargetActionIds.clone();
	}

	/** Whether ASCII should be converted to shortcut if it exists and Unicode is unavailable or empty
     * TODO really the symbol table should be more complicated in order to support this.
     * da: FIXME: this isn't really great.  It means we can't type in the real symbols and we
     * lose them if we edit them.  I'm making the default false rather than true for now.
     * Hopefully we get more nice Unicode fonts soon, at long last.  (stixfonts.com)
     */
    public static final boolean CONVERT_ALL_ASCII_TO_SHORTCUTS = false;

	/** code override for debug preference flag */
    public static final boolean DEBUG_DEBUG = false;

	/** debug flag to see what happens on 'interrupt' */
    public static final boolean LOG_INTERRUPT_ACTIONS = true;

	/** whether Next (and Undo) should step through individual comments
     *  (normal behaviour was false, every step does a command) */  //-AH
    public static final boolean SEND_COMMAND_STEPS_THROUGH_COMMENTS = true;

	/** Name of the prover-specific tooltips file, stored in config/ in prover plugin. */
    public static final String TOOLTIPS_FILE_NAME = "Tooltips.xml";
	
	
    /** This class is not intended to be instantiated. */
    private Constants() { }
    
}
