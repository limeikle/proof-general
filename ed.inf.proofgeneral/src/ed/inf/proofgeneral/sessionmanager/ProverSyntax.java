/*
 *  $RCSfile: ProverSyntax.java,v $
 *
 *  Created on 03 Aug 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.sessionmanager;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.dom4j.Element;

import ed.inf.proofgeneral.Constants;
import ed.inf.proofgeneral.NotNull;
import ed.inf.proofgeneral.preferences.ProverPreferenceNames;
import ed.inf.proofgeneral.symbols.SymbolTable;
import ed.inf.utils.datastruct.TreeWalker;
import ed.inf.utils.datastruct.TreeWalker.Fn;
import ed.inf.utils.datastruct.TreeWalker.Tree;
import ed.inf.utils.eclipse.ErrorUI;
import ed.inf.utils.file.FileUtils;

/**
 * Contains syntax information specific to a given theorem prover.
 * @author Daniel Winterstein
 * @author David Aspinall
 */
public class ProverSyntax {

	/** Tags that can create comments. */
	@NotNull
	public Tag[] commentTags = { };

    /** String start/end characters. */
	@NotNull
    public char[] stringDelimiters = { };
    
    /** Universal escape characters. */
    public char escapeCharacter = '\\';
    
    /** A description of the prover this syntax applies to. */
    public ProverInfo proverInfo = null;

    /** Namespace for this syntax. */
    @NotNull
    NameSpace global = new NameSpace();

	public String[] triggerWords = { };

    /**
     * See if the given character is a string delimiter in this syntax
     * @param c character to test
     * @return true if c is a string delimiter
     */
    public boolean isStringDelimiter(char c) {
    	for (char d : stringDelimiters) {
    		if (c==d) {
    			return true;
    		}
    	}
    	return false;
    }
    /**
	 * Create a new syntax object for a prover.
	 * NOTE: Will *not* load the basic lexical syntax.
	 * This should be done by the enclosing session manager, by calling loadSyntax().
	 */
	public ProverSyntax(ProverInfo proverInfo) {
		super();
		this.proverInfo = proverInfo;
		// load keywords
		String keywordsfile = proverInfo.getString(ProverPreferenceNames.PREF_KEYWORDS_FILE);
		if (!keywordsfile.equals("")) {
		    loadKeywords(keywordsfile);
		}
	}

	/**
	 * Set the lexical syntax using the pgip definitions from a file.
	 *  - typically IsabelleLexicalSyntax.xml.
	 *  Should be called shortly after the proverSyntax object is created.
	 *  However it should facilitate adapting to when the broker sets this dynamically.
	 *  The use of CommandProcessor creates an unnecessary dependence on SessionManager
	 * @param sm the sessionmanager to process the file taken from PREF_LEXICAL_SYNTAX_FILE
	 */
	void loadSyntax(SessionManager sm,SymbolTable symbols) throws CommandProcessor.InternalScriptingException {
    	String fileName = proverInfo.getString(ProverPreferenceNames.PREF_LEXICAL_SYNTAX_FILE);
    	if (!fileName.equals("")) {
    		try {
    			CommandProcessor.getDefault().setSessionManager(sm);
    			CommandProcessor.getDefault().processFile(proverInfo.name,fileName,Constants.UTF8,symbols);
    		} catch (CommandProcessor.InternalScriptingException x) {
    			System.err.println("Error initialising syntax from "+fileName);
    			throw x;
    		}
    	}
	}

	/**
	 * Respond to a PGIP lexicalstructure command by setting the lexical definitions.
	 * @param cmd
	 */
	public void setLexicalDefns(Element cmd) {
		boolean changed = false;
        changed |= setCommentSyntax(cmd);
		changed |= setStringSyntax(cmd);
		changed |= setEscapeSyntax(cmd);
		// TODO: set whitespace, chars, keywords.
		if (changed) {
			fireSyntaxChangeEvent();
		}
	}

	/**
     * @param cmd
     */
    private boolean setEscapeSyntax(Element cmd) {
	    List nodes = cmd.selectNodes("//"+PGIPSyntax.LEXICAL_ESCAPECHAR);
		if (nodes.size()>1) {
			Exception e = new Exception("Problem loading lexical syntax definitions.\n" +
					"Too many scape character definitions found - should be only one.");
	        ErrorUI.getDefault().signalWarning(e);
		}
		if (nodes.size()>=1) {
			Element subcmd = (Element) nodes.get(0);
			String sd = subcmd.getText();
			assert sd.length()==1; // There should only be one character
			if (sd.charAt(0) != escapeCharacter) {
				escapeCharacter = sd.charAt(0);
				return true;
			}
		}
        return false;
    }

	/**
	 * Set string syntax from lexical syntax nodes.
     * @param cmd
     */
    @SuppressWarnings({ "boxing", "unchecked" })
    private boolean setStringSyntax(Element cmd) {
	    List<Element> nodes = cmd.selectNodes("//"+PGIPSyntax.LEXICAL_STRINGDELIMITER);
	    List<Character> chars = new ArrayList<Character>();
	    for (Element subcmd : nodes) {
	    	String del = subcmd.getText();
			if (del.length() != 1) {
				ErrorUI.getDefault().signalWarning(new Exception("Lexical string delimiter element ignored, empty or more than one character long"));
			} else {
				chars.add(subcmd.getText().charAt(0));
			}
	    }
	    char delims[] = new char[chars.size()];
	    int i = 0;
	    for (char c : chars) {
	    	delims[i++] = c;
	    }
	    if (!Arrays.equals(delims,stringDelimiters)) {
	    	stringDelimiters = delims;
	    	return true;
	    }
	    return false;
    }

	/**
	 * Set the comment tags.
     * @param cmd
     */
    @SuppressWarnings("unchecked")
    private boolean setCommentSyntax(Element cmd) {
		List<Element> nodes = cmd.selectNodes("//"+PGIPSyntax.LEXICAL_COMMENTDELIMITER);
		List<Tag> tags = new ArrayList<Tag>();
		for(Element subcmd : nodes) {
			String start = subcmd.attributeValue("start");
			String end = subcmd.attributeValue("end");
			if (start == null || start.equals("")) {
				ErrorUI.getDefault().signalWarning(new Exception("Lexical comment element ignored, missing/empty start attribute"));
			}
			if (end == null || end.equals("")) {
				tags.add(new Tag(PGIPSyntax.COMMENT,start));
			} else {
				tags.add(new Tag(PGIPSyntax.COMMENT,start,end));
			}
		}
		Tag[] newTags = new Tag[tags.size()];
		tags.toArray(newTags);
		if (!Arrays.equals(newTags,commentTags)) {
			commentTags = newTags;
			return true;
		}
		return false;
    }

	/**
	 * Set keywords to be all the words in a given file (or none on error).
	 * @param fileName
	 */
	// FIXME da: this should happen from PGIP message.
	final void loadKeywords(String fileName) {
		File file = FileUtils.findProverFile(proverInfo.name,fileName);
		if (file != null) {
			try {
				String text = FileUtils.file2String(file,Charset.defaultCharset());
				String[] keywords = text.split("\\s+");
				for(String keyword : keywords) {
					global.setKeyword(TYPE_CORE, keyword);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


    /**
     * @param type - will use type.intern()
     * @param fullNames - if true, will return fully qualified names
     * @return An array of all known keywords of all types, sorted.
     * May contain duplicates.
     * Warning: Keywords do not include namespace details (ie. they are all local names),
     * unless fullNames.
     */
    public String[] getKeywords(String type, boolean fullNames) {
    	return global.getKeywordNames(type, fullNames);
    }

    public String[] getKeywords(String type, boolean fullNames, int depth) {
    	return global.getKeywordNames(type, fullNames, depth);
    }

    /**
     * Search for a keyword. Returns the *first* match.
     * @param word the word to find in the namespace
     * @return first matchine keyword found
     */
    public KeyWord getKeyWord(String word) { // Nm
        final String fword = word.intern();
        return (KeyWord) TreeWalker.treeWalk(global,new Fn(){ // SIC
            public Object apply(Tree node) {
                NameSpace ns = (NameSpace) node; // BC
                return ns.getKeyword(fword);
            }
        });
    }

    /**
     * Get the name space for the given theory name, which may be qualified.
     * An empty theory name space will be
     * generated by side effect if it doesn't exist.
     * @param theoryName - if empty or NameSpace.GLOBAL, the global (top level) name space is returned
     * @return name space for the given theory.
     */
    public NameSpace getTheory(String theoryName) {
    	assert theoryName != null : "Null theory name not allowed";
    	if (theoryName.equals(NameSpace.GLOBAL)) {
    		return global;
    	}
		return global.getSubspace(theoryName);
    }

    /**
     * @param theoryName
     * @return true if the given theory name already exists in the syntax.  Always true for
     * empty theory or Proofsyntax.GLOBAL.
     */
    public boolean hasTheory(String theoryName) {
    	if (theoryName.equals(NameSpace.GLOBAL)) {
    		return true;
    	}
		return global.hasSubspace(theoryName);
    }

    /**
     * Wrapper for getKeywords(String,boolean), returns all.
     * @return getKeywords(null, false)
     */
    public String[] getAllKeywords() {
        return getKeywords(null,false);
    }

    /**
     * The basic keywords of a prover.
     */
    public static final String TYPE_CORE = "core";
    /**
     * The theory object type is always defined in PGIP.
     */
    public static final String TYPE_THEORY = "theory";

    /**
     * The theorem object type is always defined in PGIP.
     */
    public static final String TYPE_THEOREM = "theorem";

    /**
     * Tags for a region: consists of a start and end delimiter.
     */
    public static class Tag {
    	public final String start;
    	public final String end;
    	public final String name;

    	/**
    	 * All parameters must be given and non-null.
    	 * @param name - Please use token constants from PGIPSyntax
    	 * @param start - tag delimiter, eg. &lt;a Be aware that using &lt;a&gt; would *not* cover tags with attributes.
    	 * @param end - tag delimiter, eg. &lt;/a&gt;
    	 * @throws NullPointerException if any parameter is null
    	 */
    	public Tag(String name, String start, String end) {
    		assert !(name==null || start==null || end==null) :
    			"Error: Cannot create a tag without a name, and start and end delimiters.";
    		this.name = name.intern();
    		this.start = start.intern();
    		this.end = end.intern();
    	}
    	
    	/**
    	 * A region which is begun with name and ended with the end of the line.
    	 * @param name
    	 * @param start
    	 */
    	public Tag(String name, String start) {
    		this.name = name.intern();
    		this.start = start.intern();
    		this.end = null;
    	}
    	
    	@Override
        public boolean equals(Object other) {
    		if (!(other instanceof Tag)) {
    			return false;
    		}
    		Tag otherTag = (Tag) other;
    		return this.name==otherTag.name && 
    			this.start==otherTag.start && this.end == otherTag.end;
    	}
    }

// ---------------------------------------------------
    

    public interface ISyntaxChangeListener {
    	public void syntaxChangeEvent();
    }
    
    /** Objects who listen to our events. */
    Collection<ISyntaxChangeListener> listeners = new ArrayList<ISyntaxChangeListener>();

    public void addSyntaxChangeListener(ISyntaxChangeListener listener) {
        if (listeners.contains(listener)) {
			return;
		}
        listeners.add(listener);
    }

    public void removeSyntaxChangeListener(ISyntaxChangeListener listener) {
        listeners.remove(listener);
    }

    public void fireSyntaxChangeEvent() {
        for(ISyntaxChangeListener listener : listeners) {
        	try {
        		listener.syntaxChangeEvent();
            } catch (Exception x) {
                //ignore
            }
        }
    }

}
