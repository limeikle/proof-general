/*
 *  $RCSfile: CommandProcessor.java,v $
 *
 *  Created on 20 Sep 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.sessionmanager;
import java.io.File;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.eclipse.core.resources.IFile;

import ed.inf.proofgeneral.Constants;
import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.preferences.PrefsPageBackend;
import ed.inf.proofgeneral.symbols.SymbolTable;
import ed.inf.utils.eclipse.EclipseMethods;
import ed.inf.utils.eclipse.ErrorUI;
import ed.inf.utils.file.FileUtils;

/**
 * Processes PGCommandReceived events - ie, handles interface scripting.
 * This is a singleton class.
 * SessionManagers should call link2sessionManager on creation.
 *
 * Scripting is limited at present, but should be simple to extend.
 * If you wish to extend interface scripting with new commands, then the procedure is:
 *
 * 1) Define and document the command using string constants/javadoc in InterfaceScriptSyntax.
 *
 * 2) Add the command name to InterfaceScriptSyntax.knownCommands.
 *
 * 3) Extend processCommand to actually do something.
 *
 * processCommand should remain essentially a switch statement,
 * routing commands to other classes.
 *
 * @see InterfaceScriptSyntax
 * @author Daniel Winterstein
 */
public class CommandProcessor /*implements PGIPListener, PGEventMaker*/ {

    private static CommandProcessor deflt = null;

    /**
     * Creates a new command processor on first call.
     * @return the single default CommandProcessor instance.
     */
    public static CommandProcessor getDefault() {
        if (deflt==null) {
        	deflt = new CommandProcessor();
        }
        return deflt;
    }

    /**
     * There is only one command processor. Use getDefault() to access it.
     */
    private CommandProcessor() {
        super();
    }

    /**
     * Adds the command processor as a pgip listener
     * @param sm
     */
    static void link2sessionManager(SessionManager sm) {
//        sm.addListener(getDefault());
        registerCommands(sm.proverInfo.syntax);
    }
    private static SessionManager sessionManager = null;

    public void setSessionManager(SessionManager sm) {
    	if (sm==sessionManager) {
    		return; // no change
    	}
//    	if (sessionManager != null) {
//    		sm.removeListener(getDefault());
//    		// TODO: unregister commands (not really important yet, as we only deal with 1 set of commands)
//    	}
    	sessionManager = sm;
    	if (sm != null) {
    		link2sessionManager(sm);
    	}
    }

    /**
     * Add the internal commands to a syntax object as command types
     * @param syntax
     */
    public static void registerCommands(PGIPSyntax syntax) {
    	for(int i=0; i<InterfaceScriptSyntax.knownCommands.length; i++) {
    		syntax.addType(InterfaceScriptSyntax.knownCommands[i],PGIPSyntax.INTERNAL_COMMAND);
    	}
    }

//    /**
//       @see ed.inf.proofgeneral.sessionmanager.PGIPListener#pgipEvent(ed.inf.proofgeneral.sessionmanager.events.PGIPEvent)
//     */
//    public void pgipEvent(PGIPEvent event) {
//        if (!(event instanceof PGIPCommandReceived)) return;
//        Element cmd = event.parseTree;
//        try {
//        	processCommand(cmd);
//        } catch (ScriptingException ex) {
//            ex.printStackTrace();
//            sendMessage("<errorresponse>"+ex.getMessage()+"</errorresponse>");
//        } finally {
//        	sendReadyMessage();
//        }
//    }

    /**
     * Process an internal scripting command.
     * Extend this to implement more commands.
     * Please use string constants defined in InterfaceScriptSyntax.
     * Its ugly, but it makes for a better documented, easier to maintain system.
     * Note: converts command names into lower case.
     * @param cmd
     * @throws ScriptingException if the command is not recognised or fails.
     */
    public void processCommand(Element cmd, SymbolTable symbols) throws ScriptingException {
        String cName = cmd.getName().toLowerCase();
        if (cName.equals(InterfaceScriptSyntax.SET_PREF_COMMAND)) {
		    String prefName;
		    String prefValue;
		    prefName = cmd.attributeValue(InterfaceScriptSyntax.NAME);
		    prefValue = cmd.attributeValue(InterfaceScriptSyntax.VALUE);
            PrefsPageBackend.setPref(prefName,prefValue);
    	} else if (cName.equals(InterfaceScriptSyntax.ADD_SYMBOL_COMMAND)) {
    		symbols.processSymbolTableCommand(cmd);
    	} else if (cName.equals(InterfaceScriptSyntax.ADD_SYMBOL_COMMAND)) {
    	    symbols.processSymbolTableCommand(cmd);
    	} else if (cName.equals(InterfaceScriptSyntax.LOAD_COMMAND)) {
    	    // NB: fileName absolute or relative to main plugin only 
    	    String fileName = cmd.attributeValue(InterfaceScriptSyntax.VALUE);
    	    processFile(null,fileName,Charset.defaultCharset(),symbols);
    	} else if (cName.equals(InterfaceScriptSyntax.LEXICAL_DEFN_COMMAND)) {
    	    sessionManager.proverInfo.proverSyntax.setLexicalDefns(cmd);
    	}
        // da: NB: FILE_LOADED and FILE_RETRACTED are real messages that come
        // from the prover, not just "interface script syntax".
        // Isabelle only gets this completely right in CVS>=22.11.06
    	else if (cName.equals(InterfaceScriptSyntax.FILE_LOADED)
    			|| (cName.equals(InterfaceScriptSyntax.FILE_RETRACTED))) {
    		try {
    			String urlattr = cmd.attributeValue("url");
    			String completeattr = cmd.attributeValue("complete"); // complete flag (optional)
    			// da: not in PGIP.. yet.
    			//String progressattr = cmd.attributeValue("progress"); // percentage complete (optional)
    			boolean complete = completeattr==null || completeattr.equals("true");
    			URI uri = new URI(urlattr);
    			if (cName.equals(InterfaceScriptSyntax.FILE_LOADED)) {
    					sessionManager.lock(uri,complete);
    			} else {
    					sessionManager.unlock(uri,complete);
    			}
    		} catch (Exception e) {
    			//fail silently unless debug mode on (FIXME: should do better)
    			if (ProofGeneralPlugin.getDefault()!=null  //no error if in standalone mode, TODO more robust way of checking standlone
    					&&  ProofGeneralPlugin.getDefault().isDebugging()) {
    				System.err.println("DEBUG: "+(e.getMessage()!=null ? e.getMessage() : ""+e));
    				e.printStackTrace();
    			}
    		}

    	} else {
    		throw new InternalScriptingException("Command Processor Error: "+cName+" is an unrecognised command!");
    	}
    }

// da: don't think this stuff is needed (unlex by Alex)
//    public void sendReadyMessage() {
//    	sendMessage("<ready/>");
//    }
//    /**
//     * Send this message to the session manager as if it were TP output.
//     * Adds some pgip wrapping (but not sequence numbers).
//     * @param msg
//     */
//    public void sendMessage(String msg) {
//    	sessionManager.fakeOutput("<pgip>"+msg+"</pgip>");
//    }

/**
 * Open a file and process all the internal scripting commands in it.
 * The file must be xml-parseable. Hence this method is not identical to opening a file and executing it
 * step by step in the editor.
 *
 * @param fileName should be a full path name to the file or a plugin-relative location
 * @throws InternalScriptingException
 */
// Idea: if we extend this to work for proof scripts, we can get our own "batch mode"
// processing method.
    public void processFile(String proverName, String fileName,Charset charset,SymbolTable symbols) throws InternalScriptingException {
		try {
			File file = FileUtils.findFileExt(proverName,fileName);
			if (file != null && file.canRead()) {
				// the xml parser requires a root element, so we wrap the text to make sure
				String text = "<dummy>"+FileUtils.file2String(file,charset) +"</dummy>";
				processText(text,symbols);
			} else {
				throw new InternalScriptingException("Problems opening file " + fileName + "for processing.");
			}
		} catch (Exception e) {
			throw new InternalScriptingException(e.getClass().toString()+": "+e.getMessage());
		}
	}

    /**
     * Open a file and process all the internal scripting commands in it.
     * The file must be xml-parseable. Hence this method is not identical to opening a file and executing it
     * step by step in the editor.
     *
     * @param file the file to process.
     * @throws InternalScriptingException
     */
    public void processFile(String proverName,IFile file,SymbolTable symbols) throws InternalScriptingException {
    	processFile(proverName,EclipseMethods.getOSPath(file),Constants.UTF8,symbols);
	}


    /**
     * Does the work for the processFile methods.
     * @param text
     * @throws InternalScriptingException
     */
    void processText(String text, SymbolTable symbols) throws InternalScriptingException {
    	List list;
    	List known = Arrays.asList(InterfaceScriptSyntax.knownCommands);
		try {
			Document document = getReader().read(new StringReader(text));
			list = document.selectNodes("//*");
			for (Iterator iter = list.iterator(); iter.hasNext(); ) {
				Element element = (Element) iter.next();
				if (known.contains(element.getName())) {
				    try {
				        processCommand(element,symbols);
				    } catch (Exception x) {
				        // NB: could offer a choice to continue or not
				        String msg = "Exception while processing\n" 
				        // da: this is likely to mean absolutely nothing to the user:
				        //   Interface Scripting Exception!\n"
				            +"The command "+element.asXML()+" generated the error:\n"
				            +x.getClass().toString()+" "+x.getMessage();
				        Exception ex = new InternalScriptingException(msg);
				        ErrorUI.getDefault().signalWarning(ex);
				    }
				}
			}
		} catch (Exception e) {
			throw new InternalScriptingException(e.getClass().toString()+": "+e.getMessage());
		}
	}


    private SAXReader reader = null;

    /**
     * @return a SAXReader for parsing xml documents.
     */
    SAXReader getReader() {
    	if (reader==null) {
    		reader = new SAXReader();
    	}
    	return reader;
    }



    /**
     * An interface script command has gone wrong.
       @author Daniel Winterstein
     */
    public static class InternalScriptingException extends ScriptingException {
    	public InternalScriptingException(String msg) {
    		super(msg);
    	}
    }
}
