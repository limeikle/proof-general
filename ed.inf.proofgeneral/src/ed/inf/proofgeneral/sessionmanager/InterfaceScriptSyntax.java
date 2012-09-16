/*
 *  $RCSfile: InterfaceScriptSyntax.java,v $
 *
 *  Created on 29 Oct 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.sessionmanager;
import org.dom4j.Element;


/**
 * This class defines the interface scripting commands and display commands as string constants.
 * It should, eventually, cover all pgip that can be sent from the prover to Eclipse, but not vice versa.
 * (Those are part of PGIP: they should be distinguished from the "Interface script syntax" which
 *  is purely an invention for Proof General Eclipse).
 *
 * These constants should always be used in preference to local strings, in
 * order to improve maintainability.
 * It *should* be possible to change the interface script syntax
 * by editing this file alone (although in practice there will probably be bugs).
 *
 * Note that CommandProcessor expects lower case command names
 *
 * @see CommandProcessor
 * @see ed.inf.proofgeneral.sessionmanager.PGIPSyntax
 *
 * @author Daniel Winterstein
 */
public class InterfaceScriptSyntax {

    /**
     * &lt;removesymbol name="" /&gt;, removes a symbol from the symbol table.
     */
    public static final String REMOVE_SYMBOL_COMMAND = "removesymbol";

    /**
     * &lt;addsymbol name="" ascii="" unicode="" html="" shortcut="" /&gt;, adds a symbol to the symbol table.
     *
     * Unicode should be a hex code, eg. unicode="20d2".
     * This command has the following effects:
     * <ol>
     * <li>This symbol should now display as a unicode character or html fragment
     *  depending on the widget.</li>
     * <li>If the symbol table already contains any overlapping entries (i.e. entries
     *  with the same name, ascii, unicode or html), then this new symbol will hide them.
     *  Any uses of these hidden symbols will turn back into ascii.</li>
     * </ol>
     */
    public static final String ADD_SYMBOL_COMMAND = "addsymbol";
    /**
     * Attributes used by addsymbol
     */
    public static final String NAME = "name";
    public static final String FAMILY = "family"; // think namespace
    public static final String ASCII = "ascii";
    public static final String HTML = "html";
    public static final String UNICODE = "unicode";
    public static final String SHORTCUT = "shortcut";

    /**
     * E.g. &lt;load file="interface.pg" /&gt;
     * Currently, this will only load and process <i>interface</i> scripts.
     * i.e it will *not* load proof scripts.
     */
    public static final String LOAD_COMMAND = "load";
    public static final String FILE_ATTRIBUTE = "file";

    /**
     * e.g. &gt;setpref name="" value="" /&lt; - set a preference.
     * TODO Not sure whether or not this communicates the change to the prover.
     * Probably not. @see PrefsPageBackend
     */
    public static final String SET_PREF_COMMAND = "setpref";
    public static final String VALUE = "value";

    /**
     * PGIP command to define the basic alphabet of the prover.
     * TODO: Although this is currently generated from a static file
     */
    public static final String LEXICAL_DEFN_COMMAND = "lexicalstructure";

    /**
     * PGIP message indicating that a theory has been loaded.
     * Interpreted as a command to lock the file at the given URI.
     */
    public static final String FILE_LOADED = "informfileloaded";

    /**
     * PGIP message indicating that a theory has been loaded.
     * Interpreted as a command to lock the file at the given URI.
     */
    public static final String FILE_OUTDATED = "informfileoutdated";

    /**
     * PGIP message indicating that a theory has been retracted.
     * Interpreted as a command to unlock the theory file.
     */
    public static final String FILE_RETRACTED = "informfileretracted";


    /** Element in symbols DTD for included file */
    public static final String INCLUDE_SYMBOL_FILE = "includesymbolfile";
    
    /**
     * List of commands that will be recognised as interface scripting commands.
     */
    static String[] knownCommands = {
    		SET_PREF_COMMAND,
            ADD_SYMBOL_COMMAND,
            REMOVE_SYMBOL_COMMAND,
            INCLUDE_SYMBOL_FILE,
            LOAD_COMMAND,
            LEXICAL_DEFN_COMMAND,
            FILE_LOADED, FILE_RETRACTED };

    public static boolean isInterfaceCommand(Element cmd) {
        if (cmd == null) {
        	return false;
        }
    	String name = cmd.getName();
    	for(int i=0; i<knownCommands.length; i++) {
    		if (knownCommands[i].equals(name)) {
    			return true;
    		}
    	}
    	return false;
    }

    /**
     * This class is not meant to be instantiated.
     */
    private InterfaceScriptSyntax() {
        super();
    }

}
