/*
 *  $RCSfile: PGIPMessages.java,v $
 *
 *  Created on Nov 23, 2006 by da
 *  part of Proof General for Eclipse
 */

package ed.inf.proofgeneral.pgip;

// da NB: previous start at PGIPInput/PGIPOutput.  I think they should be separated as
// per README in this package.


import org.dom4j.Element;
import org.eclipse.jface.text.IDocument;

import ed.inf.proofgeneral.document.CmdElement;

/**
 * This class constructs PGIP messages to send to the theorem prover.
 * It will *replace* the string names in PGIPSyntax for PGIP commands.
 *
 * The Enum name should always match the PGIP name, (use <code>{@link #PGIPMessages()}</code>).
 * If Java syntax prevents this, use the constructor {@link #PGIPMessages(String)}.
 */
// TODO:

//  The replacement should be a combination of static final objects and static methods based on those objects.
//  When we're creating a message to send to the prover, we can associate it to a proof script document
//  at another point if necessary (e.g. when it's put into the queue).
//
// By now this has been implemented cleanly in Ruby, Haskell, Emacs Lisp, SML, it's
// embarrassing that the Java code is so poor here!!
//
// something like this:
//
// public static final XmlDocument pgmlsymbolson = new XmlElement("pgmlsymbolson");
//
// and in SessionManager.java:  command = PGIPMessages.pgmlsymbolson
//
// instead of
//
//    public static final String PGMLSYMBOLSON = "pgmlsymbolson";
//
//    ... command = new DocElement(PGIPSyntax.PGMLSYMBOLSON,null);
//


// FIXME da to GD: looking good so far, thanks.  But the messages here should only really include
// ones which are sent to the prover, i.e. just the "improper" commands.
// Not document markup messages (opengoal, etc).  None of this stuff with capitals.
// Both of those belong elsewhere.
// And attributes should be in individual messages.

// If you look at the code you see these PGIPSyntax syntax fields being assigned
// and passed around everywhere.  They're not needed!  We can get rid of them
// once we sort this out.  (I have a partial patch that does it but got a bit
// tangled).

// Actually I really don't see much benefit in an enum here, individual methods for each document
// would actually be the most uniform.  Then I can simply write:

//  sm.queuecommand(PGIPMessages.pgmlsymbolson)
//	and
//	 sm.queuecommand(PGIPMessages.changecwd(uri))
//
//
// based on definitions here:
//
//  public static final Element pgmlsymbolson = new DefaultElement("pgmlsymbolson");
//
//  public static Element changecwd(URI uri) {
//	  Element elt = new DefaultElement("changecwd");
// 	  elt.addAttribute("url", uri.toString());
//	  return elt;
//  }


// Then there's no need to create these documents on the fly in other code and
// we use static instances where possible.

// Really, the few moments to extend the enum instead of writing a new method are completely
// pointless given the time to spend messing with code that generates names
// from enums, etc, etc.  Much better to stick to a simple, clear solution, or we'll
// get back to code as bad as before.

// The useful part of this code is bits like above where you pass in the
// type of the data put into sub elements or attributes.

// PS This class doesn't have anything to do with ProofScriptDocument

public enum PGIPMessages {

    // --------------- Begin listing ---------------------------------------------------------

	/** to be able to get comments parsed individually if we wish */
	ANYITEM,
	/** a PCIP command */
	COMMAND,
	/** The PGIP command for undoing the last step when inside a proof */
	undostep, // UNDO
	/** Our currently active file */
	openfile,
	/** Completing the current file indicated by CLOSEFILE */
	closefile,
	/** Processing a whole file in batch mode by the prover */
	loadfile,
	/** Removing a whole file */
	retractfile,
	abortfile,
	opentheory,
	theoryitem,
	/** Individual THEORYSTEPs or PROOFs can be undone with UNDOITEM */
	undoitem,
	closetheory,
	/** Incomplete theories are undone in one step with ABORTTHEORY */
	aborttheory,
	/** Named theories can be removed out-of-sequence with RETRACTTHEORY */
	retracttheory,
	/** A PROOF consists of OPENGOAL PROOFSTEP* CLOSEGOAL */
	opengoal,
	/** A PROOF consists of OPENGOAL PROOFSTEP* CLOSEGOAL */
	proofstep,
	/** A PROOF consists of OPENGOAL PROOFSTEP* CLOSEGOAL */
	closegoal,
	postponegoal,
	giveupgoal,
	/** Incomplete proofs are undone in one step with ABORTGOAL */
	abortgoal,
	/** THEORY name */
	thyname,
	/** THEOREM name */
	thmname,
	/** an improper command that has no side-effects, ignored by undo. */
	spuriouscmd,
	/** get a prover/broker to parse some text */
	parsescript,
	/** response to a parsescript */
	parseresult,
	/** representation of whitespace - ignored by prover and interface. */
	whitespace,
	/** sub-element of lexicalstructure. Defines comment syntax. */
	commentdelimiter,
	/** sub-element of lexicalstructure. Defines string syntax. */
	stringdelimiter,
	/** sub-element of lexicalstructure. Defines the escape character (usually \). */
	escapecharacter,
	/** CONTAINER type element contains the proof for an opengoal */
	CONTAINER,
	SECTION_CONTAINER,
	PROOF_CONTAINER,
	/** now we use 'container' instead */
	proof,
	/** TODO Category for generating hyperlinks; not used yet */
	LINK,
	comment,
	/** Developer comments, not for publishing */
	COMMENT_INFORMAL("COMMENT.INFORMAL"),
	/** Comments that would be included in a published version */
	doccomment,
	INTERNAL_COMMAND,
	STRING,
	KEYWORD,
	/** A trigger word is linked to some special PG functionality. */
	TRIGGERWORD,
	DEFAULT(IDocument.DEFAULT_CONTENT_TYPE),
	LOCKED,
	QUEUED,
	PROCESSED,
	/** Normal background - used in calls to getColor; this is NOT a partition type */
	BACKGROUND,
	/** shutdown command. Provers receiving this should close (or end client session). */
	proverexit, // PROVEREXIT
	/** The PGIP interrupt command. */
	interruptprover, // INTERRUPTPROVER
	/** The PGIP error tag. */
	errorresponse, // ERROR
	/** fatality attribute setting indicating that the error can be ignored. */
	nonfatal, // ERROR_NON_FATAL
	unparseable,
	/** clear the display. */
	cleardisplay,
	/** A TP response relating meta-info. Probably for internal consumption, not for the user. */
	metainforesponse,
	askpgip, // ASKPGIP
	usespgip, // USESPGIP
	pgmlsymbolson, // PGMLSYMBOLSON
	pgmlsymbolsoff, // PGMLSYMBOLSOFF
    /** Change the current working directory - issued when a script becomes active. */
    changecwd,
    /** Attribute for CHANGECWD */
    dir,

    ; // --------------- End listing ---------------------------------------------------------

	/** Name, in case Java clashes with the command name. */
	private String name;
	private PGIPMessages parent;

	/**
	 * Default constructor.  Message is equivalent to the enum name.
	 */
	private PGIPMessages() {
		this(null, null);
	}

	/**
	 * Constructs a PGIPMessage with a special name.
	 * @see #toString()
	 * @param name the name to assign.
	 */
	private PGIPMessages(String name) {
		this(name, null);
	}

	/**
	 * Constructs a PGIPMessage with a predefined parent item.
	 * @see #toString()
	 * @param parent the parent of this message.
	 */
	/*private PGIPMessages(PGIPMessages parent) {
		this(null, parent);
	}*/

	/**
	 * Constructs a PGIPMessage with a special name and defines a parent.
	 * @see #toString()
	 * @param name the name to assign.
	 */
	private PGIPMessages(String name, PGIPMessages parent) {
		this.name = name;
		this.parent = parent;
	}

	/**
	 * The command name.
	 * Use this for communication purposes.
	 * @see java.lang.Enum#toString()
	 */
	@Override
    public String toString() {
		return name != null ? name : super.toString();
	}

	/**
	 * Gets the parent message of this one.
	 */
	public PGIPMessages getParent() {
		return parent;
	}

	/**
	 * The command in XML DocElement form.
	 * @return an XML version of this command.
	 * TODO make this work correctly!
	 */
	public Element toDocElement() {
		return new CmdElement(toString());
	}

	
	/**
	 * Checks if this message has an exceptional name, i.e. one which
	 * does not correspond to its enum name
	 */
	private boolean hasName() {
		return (name != null);
	}

	/**
	 * Gets the message whose name is the given string..
	 * @return the message for the given string, or null if not found.
	 */
	public PGIPMessages getMessage(String input) {
		PGIPMessages m = null;
		try {
			m = valueOf(input);
		} catch (IllegalArgumentException e) {
			System.err.println("unrecognised message "+input);
		}
		// now check exceptions.  this is not efficient, but only a *very* few have names.
		if (m == null) {
			for (PGIPMessages n : values()) {
				if (n.hasName() && n.toString().equals(input)) {
					m = n;
					break;
				}
			}
		}
		return m;
	}

	/**
	 * Isolates a message from an XML DocElement
	 * @return the matching message, or null if not found.
	 */
	public PGIPMessages getMessage(Element e) {
		if (e.getNodeType() != org.dom4j.Node.ELEMENT_NODE) {
			return null;
		}
		return getMessage(e.getName());
	}

}
