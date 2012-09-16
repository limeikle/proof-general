/*
 *  $RCSfile: PGIPSyntax.java,v $
 *
 *  Created on 22 Apr 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse.
 *
 */
package ed.inf.proofgeneral.sessionmanager;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.Element;
import org.eclipse.jface.text.IDocument;


/**
 * Stores syntax info (mainly PGIP element and attribute names)
 * @see ProverSyntax for prover-specific syntax.
 * @see InterfaceScriptSyntax for PGIP commands that only go to the interface.
 *
 * To understand the structure of PGIP better, please refer to the PGIP
 * commentary at http://proofgeneral.inf.ed.ac.uk/Kit/docs/commentary.pdf
 *
 * @author Daniel Winterstein
 */


/**
 * FIXME replace with PGIP abstraction.  Bit tricky because DocElement is
 * overloaded so much.
 * TODO da: these strings-as-types seem to be a real mess: also I wonder if there
 * is a neater way of providing the colouring than duplicating all the tokens?
 */
public class PGIPSyntax {



	/**
	 * system for sub-typing text regions:
	 * 		a type tree of child->parent entries
	 * Note: Only single-parents are allowed at present
	 */
	private static Map<String,String> typeTree = new HashMap<String,String>();

	/**
	 * Add a type to the type tree (the type must have a parent)
	 * Does nothing if the type is already known.
	 * @param type
	 * @param parent
	 */
	public void addType(String type, String parent) {
		assert type != null && parent != null : "addType called with a null argument";
		if (typeTree.containsKey(type)) {
			return;
		}
		typeTree.put(type.intern(),parent.intern());
	}

	/** Classifies all elements of PGIP markup which correspond to some text
	 * in the document (proper commands), but NOT improper ones or additional
	 *  document markup (metainfo, openblocks, etc). */
	public static final String ANYITEM = "ANYITEM"; // classifer

	public static final String COMMAND = "COMMAND"; // classifier
	{ typeTree.put(COMMAND,ANYITEM); }

	/**
	 * A CONTAINER does not hold text, only other elements
	 */
	// public static final String CONTAINER = "CONTAINER";

	/**
	 * For container types (e.g. proof), this details what their bits
	 * should be construed as
	 */
	protected Map containerDetails = new HashMap();

	/** Dummy undo commands, not sent to the prover but put into the queue to give the right effect
	 * at the right move. */
	public static final String DUMMYUNDO = "DUMMYUNDO";  // TODO da: to become instance of Command type

	/** Type for undo commands (improper) */
	public static final String UNDOTYPE = "UNDOTYPE";
	/** Type for abort commands (improper) */
	public static final String ABORTTYPE = "ABORTTYPE";
	/** Type for retract commands (improper).  These are undo with named targets. */
	public static final String RETRACTTYPE = "RETRACTTYPE";

	/** Type for open commands (proper).  They move into next PGIP level  */
	public static final String OPENTYPE = "OPENTYPE"; // classifier
	{ typeTree.put(OPENTYPE,COMMAND); }
	/**  Type for close commands (proper). They complete the PGIP level and move out. */
	public static final String CLOSETYPE = "CLOSETYPE"; // classifier
	{ typeTree.put(CLOSETYPE,COMMAND); }

	/**
	 * The PGIP command for undoing the last step when inside a proof
	 */
	public static final String UNDOSTEP = "undostep";
	{ typeTree.put(UNDOSTEP,UNDOTYPE); }

	/**
	 * File manipulation commands are all improper (do not appear in scripts)
	 * and are only allowed at the top level.  For convenience inside the document,
	 * we store the root element as an <openfile> element.
	 *
	 * These commands are used to synchronise file loaded state with provers that
	 * may load files behind the scenes, and keep their own store of loaded files.
	 *
	 *  - Our currently active file is indicated to the prover by OPENFILE
	 *  - Completing the current file is indicated by CLOSEFILE
	 *  - Removing a whole file is achieved by RETRACTFILE
	 *  - Processing a whole file in batch mode by the prover is achieved by LOADFILE
	 *
	 * Some provers may not support these operations (or may do so in a trivial way).
	 * Others will respond to LOADFILE and RETRACTFILE by sending <informfileloaded>
	 * or <informfileretracted> messages which the interface should obey.
	 */

	public static final String OPENFILE = "openfile";
	{ typeTree.put(OPENFILE,OPENTYPE); }
	public static final String CLOSEFILE = "closefile";
	{ typeTree.put(CLOSEFILE,CLOSETYPE); }
	public static final String RETRACTFILE = "retractfile";
	{ typeTree.put(RETRACTFILE,RETRACTTYPE); }
	public static final String ABORTFILE = "abortfile";
	{ typeTree.put(ABORTFILE,ABORTTYPE); }


// da: REFACTORING, gradually: replace messy/unreliable calls to subtype with
// predicate calls below and simple equality tests.  Only use predicates for 
// classification cases (not, e.g. for opengoal: use equality there).
// Since the document model for PGIP is fixed, we don't need all this
// subtype spurious generality.	

	/**
	 * Is this element name a PGIP file command?
	 * @param pgipType
	 * @return true if it is openfile, closefile, retractfile or abortfile.
	 */
	// da: this is needed as an interim while moving to document based undo model.
	// It's used in SM to remove file commands from history used to calculate
	// undos in using old mechanism.  Parents changed above so no FILETYPE parent
	// now.  Probably this isn't really needed, but it helps in making refactoring
	// invariant for old behaviour.
	public boolean isFileType(String pgipType) {
		return subType(pgipType,OPENFILE) || subType(pgipType,CLOSEFILE)
		    || subType(pgipType,RETRACTFILE) || subType(pgipType,ABORTFILE);

	}
	
	/**
	 * @param pgipType
	 * @return true if the command type corresponds to some non-empty text in the input proof script 
	 */
	public boolean isScriptContent(String pgipType) {
		// Don't count white space or embedded errors
		if (subType(pgipType,WHITESPACE) || subType(pgipType,ERRORRESPONSE)) {
			return false;
		}
		return subType(pgipType,ANYITEM) ||
			(!subType(pgipType,OPENBLOCK) && 
					!subType(pgipType,CLOSEBLOCK) &&
					!subType(pgipType,METAINFO));
	}

	/** Command which causes the prover to batch load a file. */
	public static final String LOADFILE = "loadfile";

	/**
	 * A theory constists of OPENTHEORY (THEORYSTEP | PROOF)* CLOSETHEORY
	 *  - Individual THEORYSTEPs or PROOFs can be undone with UNDOITEM
	 *  - Incomplete theories are undone in one step with ABORTTHEORY
	 *  - Named theories can be removed out-of-sequence with RETRACTTHEORY
	 */
	public static final String OPENTHEORY = "opentheory";
	{ typeTree.put(OPENTHEORY,OPENTYPE); }
	public static final String THEORYITEM = "theoryitem";
	{ typeTree.put(THEORYITEM,COMMAND); }
	public static final String UNDOITEM = "undoitem";
	{ typeTree.put(UNDOITEM,UNDOTYPE); }
	public static final String CLOSETHEORY = "closetheory";
	{ typeTree.put(CLOSETHEORY,CLOSETYPE); }
	public static final String ABORTTHEORY = "aborttheory";
	{ typeTree.put(ABORTTHEORY,ABORTTYPE); }
	public static final String RETRACTTHEORY = "retracttheory";
	{ typeTree.put(RETRACTTHEORY,RETRACTTYPE); }

	/**
	 * A PROOF consists of OPENGOAL PROOFSTEP* CLOSEGOAL
	 * Individual PROOFSTEPS can be undo with UNDOSTEP
	 * Incomplete proofs are undone in one step with ABORTGOAL
	 */
	public static final String OPENGOAL = "opengoal";
	{ typeTree.put(OPENGOAL,OPENTYPE); }
	public static final String PROOFSTEP = "proofstep";
	{ typeTree.put(PROOFSTEP,COMMAND); }
	public static final String CLOSEGOAL = "closegoal";
	{ typeTree.put(CLOSEGOAL,CLOSETYPE); }
	public static final String POSTPONEGOAL = "postponegoal"; // Isabelle: sorry
	{ typeTree.put(POSTPONEGOAL,CLOSEGOAL); }
	public static final String GIVEUPGOAL = "giveupgoal";     // Isabelle: oops
	{ typeTree.put(GIVEUPGOAL,CLOSEGOAL); }
	public static final String ABORTGOAL = "abortgoal";
	{ typeTree.put(ABORTGOAL,ABORTTYPE); } // da: was UNDOSTEP


	/**
	 * Open and close block elements give hints for visual layout of the document.
	 * They may also be named and used to communicate document positions for
	 * actions back to the prover.
	 */
	// da: don't classify these for now: in particular, openblock isn't given OPENTYPE
	// because it doesn't correspond to state change.
	public static final String OPENBLOCK = "openblock";
	public static final String CLOSEBLOCK = "closeblock";


	/*
	 * Because we sometimes need to combine attributes (e.g. to locate a theorem within a theory),
	 * each object type has its own slightly different name attribute.
	 */
	public static final String THEORY_NAME = "thyname";
	public static final String THEOREM_NAME = "thmname";


	/** This classifies commands which are ignored in some way for do or undo. */
	public static final String SPURIOUSTYPE = "SPURIOUS"; // classifier

	/** Type of fake commands put onto command queue just to move processed position forwards. */
	public static final String SPURIOUSDO = "SPURIOUSDO";  // da: to become element of Command
	{ typeTree.put(SPURIOUSDO,SPURIOUSTYPE); }

	/** Type of fake commands put onto command queue just to move processed position backwards. */
	public static final String SPURIOUSUNDO = "SPURIOUSUNDO"; // da: to become element of Command
	{ typeTree.put(SPURIOUSUNDO,SPURIOUSTYPE); }

	/**
	 * A proper spurious command is one which appears in a document but
	 * has no state-changing side-effects in the prover when executed.
	 * Examples are commands that inspect the state.
	 * It is ignored by undo (nothing to undo), hence "spurious" for proof.
	 */
	public static final String SPURIOUSCOMMAND = "spuriouscmd";
	// da: because it is *ignored* for undo, let's give it same parent as comment.
	{ typeTree.put(SPURIOUSCOMMAND,ANYITEM); }      //da: this is a change!  FIXME: Check old code is safe.

	/**
	 * The command to send to get a prover/broker to parse some text
	 */
	public static final String PARSESCRIPT = "parsescript";
	/**
	 * The node that the prover sends back responding to a parsescript
	 */
	public static final String PARSERESULT = "parseresult";

	/**
	 * A pgip element marking whitespace - ignored by prover and interface.
	 */
	public static final String WHITESPACE = "whitespace";

	/**
	 * A pgip sub-element of lexicalstructure. Defines comment syntax.
	 */
	public static final String LEXICAL_COMMENTDELIMITER = "commentdelimiter";
	/**
	 * A pgip sub-element of lexicalstructure. Defines string syntax.
	 */
	public static final String LEXICAL_STRINGDELIMITER = "stringdelimiter";
	/**
	 * A pgip sub-element of lexicalstructure. Defines the escape character (usually \).
	 */
	public static final String LEXICAL_ESCAPECHAR = "escapecharacter";



	/**
	 * CONTAINER elements add tree structure to parsed documents, based on
	 * their PGIP state effect and other aspects.
	 */
	// TODO: this could be an enum
	public static final String CONTAINER = "CONTAINER";
	public static final String ROOT_CONTAINER = "ROOT_CONTAINER";
	public static final String THEORY_CONTAINER = "THEORY_CONTAINER";
	public static final String SECTION_CONTAINER = "SECTION_CONTAINER";
	public static final String PROOF_CONTAINER = "PROOF_CONTAINER";
	public static final String BLOCK_CONTAINER = "BLOCK_CONTAINER";


	{
		typeTree.put(ROOT_CONTAINER,CONTAINER);     // PGIP file blocks (document root)
		typeTree.put(THEORY_CONTAINER,CONTAINER);   // PGIP theory blocks
		typeTree.put(PROOF_CONTAINER,CONTAINER);    // PGIP proof blocks
		typeTree.put(BLOCK_CONTAINER,CONTAINER);    // PGIP non-state (indenting,etc) structure
		typeTree.put(SECTION_CONTAINER,CONTAINER);  // outlining only (Isabelle specific)
	}

	public static final String COMMENT = "comment";
	{ typeTree.put(COMMENT,ANYITEM); }

	/**
	 * Developer comments, not for publishing
	 */
	public static final String INFORMALCOMMENT = "COMMENT.INFORMAL";
	{ typeTree.put(INFORMALCOMMENT,COMMENT); }

	/**
	 * Document comments, processed by the prover
	 */
	public static final String DOCCOMMENT = "doccomment";
	{ typeTree.put(DOCCOMMENT,COMMAND); }
	 // da: this is for Isabelle-specific undo behaviour
	 // Presumably they are counted for undo because unlike
	 // regular comments, they aren't stripped by the lexer
	 // and so generate transitions.
	 // Perhaps we should make PGIP match this (probably easier than change the other way...)

	/**
	 * Category for inserting special regions into pg eclipse
	 * TODO e.g. <pg contains="html">some <b>stuff</b></pg>??
	 *
	 * These are used by the light parser's partition scanner
	 */
	public static final ProverSyntax.Tag PGTAG;
	static {
		PGTAG = new ProverSyntax.Tag("proofgeneral","<proofgeneral", "</proofgeneral>");
	}
	// Note: the start tag is not <proofgeneral> to allow for the possibility of attributes

	public static final String INTERNAL_COMMAND = "INTERNAL_COMMAND";
	{ typeTree.put(INTERNAL_COMMAND,COMMAND); }

	public static final String STRING = "STRING";
	public static final String KEYWORD = "KEYWORD";
	/**
	 * A trigger word is linked to some special PG functionality,
	 * probably via a right-click context menu.
	 */
	public static final String TRIGGERWORD = "TRIGGERWORD";
	//public static final String TYPE = "TYPE";
	//public static final String CONSTANT = "CONSTANT";

	public static final String DEFAULT = IDocument.DEFAULT_CONTENT_TYPE;// "DEFAULT";

	// The three "locked" types whose commands cannot be edited.
	public static final String LOCKED = "LOCKED";
	public static final String QUEUED = "QUEUED";
	{ typeTree.put(QUEUED,LOCKED); }
	public static final String PROCESSED = "PROCESSED";
	{ typeTree.put(PROCESSED,LOCKED); }
	// da: NB: this state currently not used.  Not sure what it was used
	// for.  Could become OUTDATED area
	// (redoable, but NOT really locked).
	// public static final String NEXTSTEP = "NEXTSTEP";
	// { typeTree.put(NEXTSTEP,LOCKED); }

	// bgTypes collects the locked types
	private static String[] bgTypes = {QUEUED,PROCESSED};

	/**
	 * Normal background - used in calls to getColor; this is NOT a partition type
	 */
	public static final String BACKGROUND = "BACKGROUND";

	/**
	 * PGIP shutdown command. Provers receiving this should close
	 * (or end the session, if they are a server).
	 */
	public static final String PROVEREXIT = "proverexit";
	/**
	 * The PGIP interrupt command.
	 */
	public static final String INTERRUPTPROVER = "interruptprover";

	/**
	 *  The PGIP element for error and system messages.
	 *  Not all error messages indicate fatal errors.
	 *  Error messages may be associated with locations.
	 */
	public static final String ERRORRESPONSE = "errorresponse";

	/**
	 *  The PGIP element for normal messages, including
	 *  ordinary output during proof, and user-level tracing
	 *  or debugging messages.
	 */
	public static final String NORMALRESPONSE = "normalresponse";

	/**
	 *  The PGIP element indicating the prover is ready for more
	 *  input.
	 */
	public static final String READY = "ready";

	/**  The PGIP tag for an unparseable bit of text, appears in the
	 * &lt;parseresult&gt; message.
	 * */
	public static final String UNPARSEABLE = "unparseable";
	{ typeTree.put(UNPARSEABLE,ERRORRESPONSE); } // NB: this is crucial for parser code linkParseAux()

	/**
	 * PGIP command to clear the display.
	 */
	public static final String CLEARDISPLAY = "cleardisplay";

	/**
	 * A TP response relating meta-info.
	 * Probably for internal consumption, and not to be shown to the user.
	 */
	public static final String METAINFORESPONSE = "metainforesponse";
	public static final String METAINFO = "metainfo";

	//public List types = new ArrayList();

	private static volatile String[] types = null; // da: added volatile to patch lazy init. VO: OK
	/**
	 * Return all the different fg types this syntax recognises
	 *  - Excluding the LOCKED versions.
	 * For Eclipse functions, you may want to use PartitionScanner.LIGHT_PARSE_TYPES
	 * instead, which only returns a subset of these types.
	 * @see ed.inf.proofgeneral.editor.lightparser.PartitionScanner
	 * @return a list of types recognised by this syntax.
	 */
	private String[] getTypes() {
	    if (types==null) {
			Set<String>tkeys = new HashSet<String>();
			tkeys.addAll(typeTree.keySet());

			// check this is not a bug -- perhaps replace with (*)? --GD
			// da: I *think* you're right although we should understand
			// the partitioning code to be sure.  (Although that may get
			// replaced by marker stuff eventually).
			// Actually it's not clear if the values
			// are even right to add since they include ANYITEM which
			// doesn't have a syntax?  Let's test with this.
			//tkeys.addAll(typeTree.entrySet());
			tkeys.addAll(typeTree.values()); // (*)

			tkeys.removeAll(Arrays.asList(bgTypes));
			tkeys.add(DEFAULT);
			// tkeys.add(COMMENT); // TODO find another way of doing this

			types = tkeys.toArray(new String[tkeys.size()]);
	    }
		return types;
	}

	/**
	 * @return All the different types this syntax recognises
	 *  - INCLUDING the LOCKED versions
	 */
	public String[] getAllTypes() {
		List<String> allTypes = new ArrayList<String>();
		List<String> fgTypes = Arrays.asList(getTypes());
		allTypes.addAll(fgTypes);
		for(Iterator i = fgTypes.iterator(); i.hasNext();) {
		    String fgType = (String) i.next();
		    for(int j=0; j<bgTypes.length; j++) {
		        allTypes.add( setBgType(fgType, bgTypes[j]) );
		    }
		}
		String[] result = new String[allTypes.size()];
		result = allTypes.toArray(result);
		return result;
	}

	// da: these methods don't seem to be used
	/**
	 * @param type document partition type
	 * @return the "locked" version of type, coloured as PROCESSED text
	 */
//	public static String lock(String type) {
//	    return setBgType(type,PROCESSED);
//	}
	/**
	 * @param type  document partition type
	 * @return the processed version of type
	 */
//	public static String process(String type) {
//		return setBgType(type,PROCESSED);
//	}
	/**
	 * @param type  document partition type
	 * @return the processed version of type
	 */
//	public static String nextstep(String type) {
		//return setBgType(type,NEXTSTEP);
//}
	/**
	 *
	 * @param pType foreground type (ie. an unlocked content type)
	 * @param bgType background type
	 * @return the combined type
	 */
	private static String setBgType(String pType, String bgType) {
	    return bgType+"."+pType;
	}

	/**
	 * @param type document partition type (possibly locked)
	 * @return the 'clean' version of type
	 * - definitely un-locked and un-processed and not next-step
	 */
	public static String unlock(String type) {
	    if (type.startsWith(PROCESSED)) {
	    	return unlock(type.substring(PROCESSED.length()+1));
	    }
		if (type.startsWith(QUEUED)) {
			return unlock(type.substring(QUEUED.length()+1));
		}
		//if (type.startsWith(NEXTSTEP)) return unlock(type.substring(NEXTSTEP.length()+1));
		return type;
	}

	/**
	 * Return true if partition typeA is a subtype of typeB
	 * Ignores locked/unlocked distinction in typeA
	 * @param typeA  
	 * @param typeB
	 * @return true if typeA is a subtype of typeB; always false if typeA is null 
	 */
	public boolean subType(String typeA, String typeB) {
		if (typeA == null) {
			return false;
		}
		//if (typeA.startsWith(typeB)) return true;
		typeA = unlock(typeA);
		if (typeA.equals(typeB)) {
			return true;
		}
		//if (typeA.startsWith(typeB)) return true;
		if (typeTree.containsKey(typeA)) {
			return subType(typeTree.get(typeA),typeB);
		}
		return false;
	}

	/**
	 * Return true if typeA consists of elements of typeB
	 * That is, text elements in typeA should be assumed to be of typeB
	 * e.g. proof type consists of proofsteps
	 * @param typeA
	 * @param typeB
	 * @return
	 */
	/*public boolean superType(String typeA, String typeB) {
	    // currently only one case for this:
	    return subType(typeA,CONTAINER)
	    		&& subType((String) containerDetails.get(typeA),typeB);
	}*/

	/**
	 * @param cType
	 * @return the type which text elements of this node should be interpreted as
	 * @deprecated - not actaully used anywhere.
	 */@Deprecated
	public String getContainedType(String cType) {
	    return (String) containerDetails.get(cType);
	}

	/**
	 * If typeA consists of elements of typeB, then return typeB
	 * That is, text elements in typeA should be assumed to be of typeB
	 * e.g. proof type consists of proofsteps
	 * otherwise return null
	 * @param typeA
	 * @param typeB
	 * @return
	 */
	//public String superType(String typeA) {
	    // currently only one case for this:
	  //  return subType(typeA,SUPERCOMMAND)? COMMAND : null;
	//}

	/**
	 * Create a new syntax object for a prover.
	 */
	 public PGIPSyntax(ProverInfo info) {
		 super();
	 }

	public static final String PROVERCONTROL = "PROVERCONTROL"; // classifier
	public static final String PROVERQUERY= "PROVERQUERY"; // classifier

	public static final String ASKPGIP = "askpgip";
	{ typeTree.put(ASKPGIP,PROVERQUERY); } // not really control, but..
	public static final String USESPGIP = "usespgip";

	public static final String PGMLSYMBOLSON = "pgmlsymbolson";
	{ typeTree.put(PGMLSYMBOLSON,PROVERCONTROL); }
    public static final String PGMLSYMBOLSOFF = "pgmlsymbolsoff";
	{ typeTree.put(PGMLSYMBOLSOFF,PROVERCONTROL); }
    //TODO should have a type category for these, so prover state can be smarter about skipping them.

    public static final String SETPROVERFLAG = "setproverflag";
	{ typeTree.put(SETPROVERFLAG,PROVERCONTROL); }
    public static final String PROVERFLAG_QUIET = "quiet";
    public static final String PROVERFLAG_SYMBOLS = "pgmlsymbols";
    public static final String PROVERFLAG_THMDEPS = "metainfo:thmdeps";
	//public Parser parser = null;

    /**
     * Change the current working directory - issued when a script becomes active.
     */
    public static final String CHANGECWD = "changecwd";


    /** Command sent to the prover to ask it about known identifiers */
	public static final String ASKIDS = "askids";
	{ typeTree.put(ASKIDS,PROVERQUERY); }
	/** Response received from prover after <askids> */
	public static final String SETIDS = "setids";

	/** Command sent to the prover to ask it to display the value of some identifier */
	public static final String SHOWID = "showid";
	{ typeTree.put(SHOWID,PROVERQUERY); }

	/** Command to send raw text to the prover */
	public static final String SYSTEMCMD = "systemcmd";

	/**
	 * Add given URI as a URL attribute to the passed PGIP command.
	 * Uses java.net.URI.toURL to do conversion.
	 * @param uri
	 * @param command
	 * @throws ScriptingException
	 */
	public static void addURLattribute(URI uri, Element command) throws ScriptingException {
		try {
			String uris = uri.toURL().toString();
			if (uris.startsWith("file:/") && !uris.startsWith("file://"))
				//jva file.toURI().toURL() returns file:/path/to/x,
				//which is not technically valid; clean it up so ML accepts it.
				uris = "file://"+uris.substring(5);
	        addUrlAttribute(uris, command, "url");
        } catch (MalformedURLException e) {
        	throw new ScriptingException("addURLattribute: error in URI->URL conversion");
        }
	}

	public static void addUrlStringAttribute(String url, Element command) throws ScriptingException {
		addUrlAttribute(url, command, "url");
	}

	private static void addUrlAttribute(String uri, Element command, String attributeName) throws ScriptingException {
		command.addAttribute(attributeName, uri);
     }

}
