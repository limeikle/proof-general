/*
 *  $RCSfile: SymbolTable.java,v $
 *
 *  Created on 07 Aug 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.symbols;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dom4j.Element;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Color;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import ed.inf.proofgeneral.Constants;
import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.ProofGeneralProverRegistry.Prover;
import ed.inf.proofgeneral.editor.symbols.SymbolTableFiles;
import ed.inf.proofgeneral.preferences.PreferenceNames;
import ed.inf.proofgeneral.sessionmanager.CommandProcessor;
import ed.inf.proofgeneral.sessionmanager.InterfaceScriptSyntax;
import ed.inf.proofgeneral.sessionmanager.ScriptingException;
import ed.inf.proofgeneral.ui.theme.PGColors;
import ed.inf.utils.datastruct.StringManipulation;
import ed.inf.utils.file.FileUtils;

/**
 * Maintain a symbol table (i.e. association of tokens to Unicode characters,
 * keyboard shortcuts, etc) and provide support for symbol use by token replacement.
 * 
 * @author Daniel Winterstein
 * @author Alex Heneveld
 * @author David Aspinall
 */

// TODO da: 
//  1. this class needs a careful review of where symbols are quoted
//  in regexp replacements and where not.  It's not quite correct at the moment.
//  2. I've simplified events, but they're still not really great for editors:
//  what we could do instead is pass in the symbol table change itself, which
//  would be more useful/efficient.
//  3. Unicode support checks are disabled, they caused nasty low-level problems
//  (e.g. very slow startup).  Could try reinstigating if better way found,
//  but not important.

public class SymbolTable  {

    /**
     * Create a new symbol table.  It starts off empty.
     */
    public SymbolTable() {
        super();
    }

    /**
     * Table of symbol stacks used for providing symbol support
     */
    private ArrayList<Stack<Symbol>> symbols = new ArrayList<Stack<Symbol>>();

    /**
     * @return All symbols, including hidden and inactive ones.
     */
    List<Symbol> getAllSymbols() {
    	List<Symbol> list = new ArrayList<Symbol>();
    	for (Stack<Symbol> stack : symbols) {
    		for(Symbol sym : stack) {
        		list.add(sym);
        	}
        }
        return list;
    }

    /**
     * @return all active symbols whose ASCII sequence begins with the given prefix
     */
    public List<Symbol> getAllActiveSymbolsWithPrefix(String prefix) {
    	List<Symbol> list = new ArrayList<Symbol>();
    	for (Stack<Symbol> stack : symbols) {
            if (stack.size()>0 && stack.firstElement().ascii.startsWith(prefix)) {
            	list.add(stack.firstElement());
            }
    	}
        return list;
    }

    /**
     * Load a table from the given file, and compile the matchers. 
     * @param path the path from which to load
     */
    // Does *not* check for unicode support.
    // Users must call unicodeSupportCheck() at some point.
    public void init(IPath path) {
    	fireSymbolPreChangeNotification(null);
    	ArrayList<Stack<Symbol>> oldsymbols = symbols;
    	symbols = new ArrayList<Stack<Symbol>>();
        try {
        	CommandProcessor.getDefault().processFile(null,path.toOSString(),Constants.UTF8,this);
        } catch (Exception x) {
        	System.err.println("Error initialising symbols from "+path.toString());
        	System.err.println(x.getLocalizedMessage());
        	x.printStackTrace();
        	symbols = oldsymbols;
        }
    	setMatchers();
    	fireSymbolChangeNotification(null);
    }

    /*
     * Check which unicode characters are actually supported by this font.
     * Sets the symbol status as appropriate.
     * ASSUME: We only need to do this once for the whole application.
     * If this is not true, a certain amount of refactoring will be required.
     * This class, and the Symbol class, are not set up to be variably active.
     * @param font - the swt font to check against
     */
//   public void unicodeSupportCheck(Font font) {
//    	if (unicodeSupportCheckDone) return;
//    	for(Iterator i = symbols.iterator(); i.hasNext();) {
//    		Stack ss = ((Stack) i.next());
//    		Symbol sym = (Symbol) ss.peek();
//    		String unicode = sym.unicode;
//    		if (unicode != null && unicode.length()>0 &&
//    			EclipseMethods.unicodeStringTest(font, sym.unicode)) {
//    			sym.status = Symbol.ENABLED;
//    		} else {
//    			sym.status = Symbol.DISABLED;
//    		}
//    	}
//    	unicodeSupportCheckDone = true;
//    }
//    protected boolean unicodeSupportCheckDone = false;

    /**
     * Load the default symbol table (may be later overridden by project-specific one).
     * @param prover the prover we're initialising for (used to retrieve the preference for
     * the symbol table file; association is not kept).
     */
    // See: {@link #unicodeSupportCheck(Font)} should be called at some point.
    // Currently not enabled.
    public void init(Prover prover) {
        try {
        	IPath table = null;;
        	try {
        		table = SymbolTableFiles.getDefault().getDefaultTable(prover);
        	} catch (ScriptingException e) {
        		System.out.println("WARNING: "+e.getMessage());
        	}
        	if (table!=null)
        		init(table);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    /**
     * Take a string of text and return a copy where all the ascii has been replaced
     * with html/unicode symbols.
     * @param text
     * @param escaped - indicates that text already has its
     * 			XML/HTML characters escaped (i.e. true if we have &gt; instead of >, etc).
     * @return a version with HTML symbols.
     */
    public String useHtml(String text, boolean escaped) {
        Symbol sym = null;
        for(Iterator i = symbols.iterator(); i.hasNext();) {
            try {
                sym = (Symbol) ((Stack) i.next()).peek();
                String r = null;
                if (sym.rHtml==null) {
                	r = sym.unicode;
                }
                else {
                	r= sym.rHtml;
                }
                if (r==null || !sym.status.equals(Symbol.ENABLED)) {
                	continue;     // Skip this symbol
                }
                if (escaped) {
                	text = sym.pEscaped.matcher(text).replaceAll(r);
                }
                else {
                	text = sym.pAscii.matcher(text).replaceAll(r);
                }
                //if (sym.unicode!=null) text = text.replaceAll(sym.unicode,sym.rHtml);
            } catch (Exception x) {
                if (sym!=null) {
                	System.out.println("Error replacing "+sym.name);
                }
                else {
                	x.printStackTrace();
                }
            }
        }
        return text;
    }

    /**
     * Convert all Unicode and HMTL symbols into their ASCII equivalents
     * @param text
     * @return ASCII version of the Unicode string.
     */
    public String useAscii(String text) {
        for(Iterator i = symbols.iterator(); i.hasNext();) {
            Symbol sym = (Symbol) ((Stack) i.next()).peek();
            if (sym.pHtml != null) {
            	text = sym.pHtml.matcher(text).replaceAll(sym.rAscii);
            }
            if (sym.unicode!=null) {
            	text = text.replaceAll(Pattern.quote(sym.unicode),sym.rAscii);
            }
        }
        return text;
    }
    
    /**
     * Convert all Unicode and HMTL symbols into their ASCII shortcuts (if they exist) or full equivalents
     * @param text
     * @return ASCII shortcut or \&lt;foo&gt; version of the Unicode string (not xml-escaped)
     */
    public String useShortcutsAscii(String text) {
        for(Iterator i = symbols.iterator(); i.hasNext();) {
            Symbol sym = (Symbol) ((Stack) i.next()).peek();
            String rReplacement = sym.rAscii;
            if (sym.shortcut!=null && sym.rShortcut!=null) {
            	rReplacement = sym.rShortcut+" ";
            	if (sym.pAscii!=null)
            		text = sym.pAscii.matcher(text).replaceAll(rReplacement);
            }            
            if (sym.pHtml != null) {
            	text = sym.pHtml.matcher(text).replaceAll(rReplacement);
            }
            if (sym.unicode!=null) {
            	text = text.replaceAll(Pattern.quote(sym.unicode), rReplacement);
            }            
            	
        }
        return text;
    }

    /**
     * Convert all unicode and html symbols into their ASCII equivalents, escaped for xml transmission
     * @param text
     * @return XML-escaped, ASCII version of the Unicode string.
     */
    public String useEscapedAscii(String text) {
        for(Iterator i = symbols.iterator(); i.hasNext();) {
            Symbol sym = (Symbol) ((Stack) i.next()).peek();
            if (sym.pHtml != null) {
            	text = sym.pHtml.matcher(text).replaceAll(sym.rEscapedAscii);
            }
            if (sym.unicode!=null) {
            	text = text.replaceAll(Pattern.quote(sym.unicode),sym.rEscapedAscii);
            }
        }
        return text;
    }

    /**
     * converts unicode or html to xml-escaped shortcut text, if there is a shortcut,
     * else to xml-escaped ascii
     * @author AH
     */
    public String useEscapedShortcutsOrAscii(String text) {
      for(Iterator i = symbols.iterator(); i.hasNext();) {
          Symbol sym = (Symbol) ((Stack) i.next()).peek();
          if (sym.shortcut!=null && sym.shortcut.length()>0) {
          	if (sym.rEscapedShortcut==null || sym.rEscapedShortcut.length()==0) {
          		System.err.println("no escaped shortcut for "+sym.shortcut);
          	} else {
              if (sym.pHtml != null) {
            	  //could be smarter and search for pHtml+" " first (to avoid inserting extra spaces), but no need
            	  text = sym.pHtml.matcher(text).replaceAll(sym.rEscapedShortcut+" ");
              }
              if (sym.unicode!=null) {
            	  text = text.replaceAll(Pattern.quote(sym.unicode), sym.rEscapedShortcut+" ");
              }
              continue;
          	}
          }
          if (sym.pHtml != null) {
        	  text = sym.pHtml.matcher(text).replaceAll(sym.rEscapedAscii);
          }
          if (sym.unicode!=null) {
        	  text = text.replaceAll(Pattern.quote(sym.unicode), sym.rEscapedAscii);
          }
      }
      return text;
  }

    /**
     * Convert all ASCII symbols (character sequences, e.g. \<longrightarrow>) or typing short-cuts (e.g. -->)
     * into their Unicode equivalents.
     * When converting short-cuts, we only replace the entire text (i.e. "-->" will be converted, but "a-->b" will not be).
     * If Unicode characters are not available, convert ASCII symbol names into their
     * short-cuts if the longcuts parameter is set.  NB: this should <b>not</b> be used for
     * documents, whose contents should remain in ASCII by default (or unicode if the prover
     * supports it).
     *
     * @param text - argument text (non-null)
     * @param escaped - determines whether the text has already had its XML/HTML characters escaped (eg. > vs &gt;)
     * @param shortcuts - if true, replace short-cuts by Unicode characters
     * @param longcuts - if true, replace ASCII symbols by their typing shortcuts when Unicode characters are not available.
     * @return a Unicode string
     */
	// FIXME da: if we're to keep a map of positions changing between source text and
    // viewed text, this needs serious reworking.  We should make one pass over the document
    // (rather than one per symbol) and record the substitution positions in a map.
    // Then we can map marker position changes, etc.
    // This looks like so much effort, it would be easier to encourage people to convert
    // their input documents to Unicode, perhaps, and use an Isabelle plugin to convert
    // back to X-Symbols on reading.
    private String useUnicode(String text, boolean escaped, boolean shortcuts, boolean longcuts) {
    	if (text.length() > 0) { // short cut
    		for(Iterator i = symbols.iterator(); i.hasNext();) {
    			Symbol sym = (Symbol) ((Stack) i.next()).peek();
    			if (sym.unicode==null || sym.unicode.length()==0 || !sym.status.equals(Symbol.ENABLED)) {
    				if (!longcuts || sym.shortcut==null || sym.shortcut.length()==0) {
    					continue;
    				}
    				if (escaped) {
        				// convert to shortcut char  -AH  (do this because otherwise ASCII codes appear everywhere; they are ugly!)
    					// da: I agree they're ugly, but this causes really nasty parsing bugs if shortcuts are
    					// not acceptable inputs.  Also, it changes the source text, which is bad. So we
    					// shouldn't do it for documents.  But it is fine for output.
    					text = sym.pEscaped.matcher(text).replaceAll(sym.shortcut);
    				}
    				//it isn't aways escaped, so do this as well
    				text = sym.pAscii.matcher(text).replaceAll(sym.shortcut);
    			} else {
    				if (escaped) {
    					text = sym.pEscaped.matcher(text).replaceAll(Matcher.quoteReplacement(sym.unicode));
    				} else {
    					text = sym.pAscii.matcher(text).replaceAll(Matcher.quoteReplacement(sym.unicode));
    					if (shortcuts && sym.shortcut!=null) {
    						if (text.equals(sym.shortcut) && !StringManipulation.isWhitespace(sym.shortcut)) {
    							text = sym.unicode;
    						}
    					}
    					//text = sym.pShortcut.matcher(text).replaceAll(sym.unicode);
    				}
    			}
    			//text = sym.pHtml.matcher(text).replaceAll(sym.unicode);
    		}
    	}
        return text;
    }
    
    // FIXME ISABELLEWS: check that the empty compile matches NO string
    // FIXME again: we could remove this junk if we make it a contract that
    // init() must be called before using a symbol table, but need to sort
    // out editor initialisation order first.
    private Pattern anySymboliseMatcher = Pattern.compile("");
    private Pattern anyDesymboliseMatcher = Pattern.compile("");
    private HashMap<String,String> symboliseReplaceMap = new HashMap<String,String>();
    private HashMap<String,String> desymboliseReplaceMap = new HashMap<String,String>();

    private void setMatchers() {
    	String symboliseregexp = "";
    	String desymboliseregexp = "";
    	symboliseReplaceMap = new HashMap<String,String>();
    	desymboliseReplaceMap = new HashMap<String,String>();
    	for(Stack<Symbol> symstack : symbols) {
    		Symbol sym = symstack.peek();
    		if (sym.unicode==null || sym.unicode.length()==0 || 
    				!sym.status.equals(Symbol.ENABLED)) {
    			continue;
    		}
   			symboliseregexp += 
   				(symboliseregexp.equals("")? "" : "|") +	Pattern.quote(sym.ascii);
  			desymboliseregexp += 
  				(desymboliseregexp.equals("")? "" : "|") +	Pattern.quote(sym.unicode);
    		symboliseReplaceMap.put(sym.ascii, sym.unicode);
    		desymboliseReplaceMap.put(sym.unicode, sym.ascii);
    	}
    	anySymboliseMatcher = Pattern.compile(symboliseregexp);
    	anyDesymboliseMatcher = Pattern.compile(desymboliseregexp);
    }

    public TextEdit symboliseEdits(IDocument doc) {
    	return makeEdits(doc,anySymboliseMatcher,symboliseReplaceMap);
    }
    public TextEdit desymboliseEdits(IDocument doc) {
    	return makeEdits(doc,anyDesymboliseMatcher,desymboliseReplaceMap);
    }

    private TextEdit makeEdits(IDocument doc,Pattern pattern, HashMap<String,String> replaceMap) {
    	TextEdit edits = new MultiTextEdit();
    	String doctext = doc.get();
    	Matcher matcher = pattern.matcher(doctext);
    	while (matcher.find()) {
    		int start = matcher.start();
    		int end = matcher.end();
    		String replacement = replaceMap.get(doctext.substring(start, end));
    		if (replacement != null) {
    			edits.addChild(new ReplaceEdit(start,end-start,replacement));
    		}
    	}
    	return edits;
    }
    
    /**
     * Convert all ASCII sequences into the input into their Unicode equivalents, if they exist and are available.
     * @param text
     * @return a Unicode string
     */
    public String useUnicodeForDocument(String text) {
        return useUnicode(text,false,false,false);
    }

    /**
     * Convert ASCII sequences into Unicode.  If the input string is (exactly) a typing short cut,
     * it will also be replaced if the shortcuts parameter is true.
     * @param text
     * @return a Unicode string
     */
    public String useUnicodeForTyping(String text) {
        return useUnicode(text,false,true,false);
    }

    /**
     * Convert ASCII sequences into Unicode, or, when Unicode characters
     * are not available, the typing short-cuts.
     * @param text
     * @param escaped whether input has been xml escaped (e.g. using ampersand gt;)
     * @return a Unicode string
     */
    public String useUnicodeForOutput(String text, boolean escaped) {
        return useUnicode(text,escaped,false,true);
    }

    public int startsWithSymbol(String s, int i, Symbol ss) {
    	if (ss.ascii!=null && s.startsWith(ss.ascii, i)) {
    		return ss.ascii.length();
    	}
    	if (ss.unicode!=null && s.startsWith(ss.unicode, i)) {
    		return ss.unicode.length();
    	}
    	if (ss.html!=null && s.startsWith(ss.html, i)) {
    		return ss.html.length();
    	}
    	if (ss.shortcut!=null && s.startsWith(ss.shortcut, i)) {
    		return ss.shortcut.length();
    	}
    	return -1;
    }

    /**
     * Checks whether s1 and s2 start with (usually different) representations of
     * the same symbol.
     * @return the length of the symbol-specific string if it starts in both s1
     * 			and s2, null otherwise (for use in parsing)
     * @param e1 start index for s1
     * @param e2 start index for s2
     * @author AH
     */
    public int[] checkStringsStartSameSymbol(String s1, int e1, String s2, int e2) {
    	for(Iterator i = symbols.iterator(); i.hasNext();) {
    		Symbol sym = (Symbol) ((Stack) i.next()).peek();
    		int i1 = startsWithSymbol(s1, e1, sym);
    		if (i1>0) {
    			int i2 = startsWithSymbol(s2, e2, sym);
    			if (i2>0) {
    				return new int[] { i1, i2 };
    			}
    		}
      }
    	return null;
    }

    /**
     * Adjust the symbol table according to an addsymbol or removesymbol command
     * 
     * <addsymbol ascii="" unicode="" html="" shortcut="" name="" />
     * <removesymbol ascii="" name="" />
     * Such commands
     *
     * @param cmd
     */
    public void processSymbolTableCommand(Element cmd) throws ScriptingException {
        if (cmd.getName().equalsIgnoreCase(InterfaceScriptSyntax.ADD_SYMBOL_COMMAND)) {
            addSymbol(cmd);
        } else if (cmd.getName().equalsIgnoreCase(InterfaceScriptSyntax.REMOVE_SYMBOL_COMMAND)) {
            removeSymbol(cmd);
        } else {
        	throw new ScriptingException("Command "+cmd.getName()+" is not a recognised symbol table command");
        }
    }
    /**
     * @see InterfaceScriptSyntax
     *
     * @param cmd
     */
    void addSymbol(Element cmd) throws ScriptingException {
        try { // FIXME da: remove this try block
        String unicode = cmd.attributeValue(InterfaceScriptSyntax.UNICODE);
        String name = cmd.attributeValue(InterfaceScriptSyntax.NAME);
        String ascii = cmd.attributeValue(InterfaceScriptSyntax.ASCII);
        String family = cmd.attributeValue(InterfaceScriptSyntax.FAMILY);
        if (ascii==null) {
        	throw new ScriptingException("Tried to define a symbol ("
        			+ (name==null?"noname":name) // RCN: I don't think so, attributeValue can be null.
        			+" ) without an ascii version. This is not possible.");
        }
        String shortcut = cmd.attributeValue(InterfaceScriptSyntax.SHORTCUT);
        if (ascii.length()>Constants.MAX_SYMBOL_LENGTH) {
            throw new ScriptingException("Tried to define a symbol ("
                    + (name==null?"noname":name) // RCN: I don't think so, attributeValue can be null.
                    +" ) with too long an ascii version. Symbols *must* have an ascii version shorter than "
                    +Integer.toString(Constants.MAX_SYMBOL_LENGTH)+" characters.");
        }
        Symbol sym = new Symbol(name,ascii,unicode,
        		cmd.attributeValue(InterfaceScriptSyntax.HTML),shortcut,
        		family);
        Stack<Symbol> ss;
        for(Iterator<Stack<Symbol>> i = symbols.iterator(); i.hasNext();) {
            ss = i.next();
            Symbol s = ss.peek();
            if (s.overlap(sym)) {
                if (s.equivalent(sym)) {
                    if (shortcut!=null && !shortcut.equals("")) {
                        // replace the shortcut
                        s.shortcut = sym.shortcut;
                    }
                    return; // no need to do anything
                }
                System.out.println("WARNING: A symbol is being obscured: "
                		+sym.name+" hides "+s.name);
                fireSymbolPreChangeNotification(cmd);
                s.status = Symbol.HIDDEN;
                ss.push(sym);
                sortSymbols();
                fireSymbolChangeNotification(cmd);
                return;
            }
        }
        fireSymbolPreChangeNotification(cmd);
        ss = new Stack<Symbol>();
        ss.push(sym);
        symbols.add(ss);
        sortSymbols();
        fireSymbolChangeNotification(cmd);
    	} catch (ScriptingException e) {
    		throw e;
    	} catch (Exception e) {
    		e.printStackTrace();
     	}
    }

    /**
     * Used to enforce sorting.
     */
    private boolean fSorted;
    /**
     * Sort the symbol table according to length of (unicode) symbol; longest first.
     * This allows overlapping symbols of the form -&gt;, --&gt;
     * This *must* be called between Symbol PreChange and Change Events.
     *
     */
    void sortSymbols() {
        Collections.sort(symbols, new Comparator<Stack>() {
            public int compare(Stack o1, Stack o2) {
                Stack s1 = o1;
                Stack s2 = o2;
                Symbol sym1 = (Symbol) s1.peek();
                Symbol sym2 = (Symbol) s2.peek();
                if (sym2.unicode==null || sym1.unicode==null) {
                	if (sym2.unicode==null && sym1.unicode==null) {
                		return 0;
                	}
                	return (sym1.unicode==null ? 1 : -1);
                }
                if (sym1.unicode.length() > sym2.unicode.length()) {
                	return -1;
                }
                if (sym1.unicode.length() == sym2.unicode.length()) {
                	return 0;
                }
                return 1;
            }
        });
        fSorted = true;
    }

    // TODO: see TRAC 79, http://proofgeneral.inf.ed.ac.uk/trac/ticket/79
    void includeSymbolFile(Element include) throws ScriptingException {
    	String filename = include.attributeValue(InterfaceScriptSyntax.NAME);
		File includedfile = FileUtils.findFileExt(null,filename);
		SymbolTable nestedTable = new SymbolTable();
		try {
			// FIXME: this mapping back to a path is messy.  Also, the init()
			// method should be turned into a factory method that doesn't
			// mess with the default table.
			nestedTable.init(new Path(includedfile.getCanonicalPath()));
		} catch (Exception e) {
			throw new ScriptingException(e.toString());
		}
    	// TODO: merge/link nested table now.  Ideally the representation
		// should be improved to allow this to be done efficiently so we could cache several
		// tables at once in the runtime.  Easiest for now to just merge
		// in the declarations.
    }
    
    /**
     * Only visible symbols (ie. those at the top of a stack) can be removed.
     * At most 1 symbol will be removed.
     * @param cmd
     * @throws ScriptingException
     */
    void removeSymbol(Element cmd) throws ScriptingException {
        String name = cmd.attributeValue(InterfaceScriptSyntax.NAME);
        //FIXME changes to InterfaceScriptSyntax broke the following, 2005-06-16
        // String family = cmd.attributeValue(InterfaceScriptSyntax.FAMILY);
        //replace temporarily following above (seems all families are 'default') ??
        String family = "default";
        Symbol sym;
        try {
            sym = new Symbol(name,name,name,name,name,family);
        } catch (Exception x) {
            throw new ScriptingException("Could not remove symbol "+name+":\n"
                    +x.getLocalizedMessage());
        }
        if (name==null) {
        	name = cmd.attributeValue(InterfaceScriptSyntax.ASCII);
        }
        if (name==null) {
        	throw new ScriptingException("remove symbol commands must have a name");
        }
        for(Iterator i = symbols.iterator(); i.hasNext();) {
            Stack ss = ((Stack) i.next());
            Symbol s = (Symbol) ss.peek();
            if (s.overlap(sym)) {
                fireSymbolPreChangeNotification(cmd);
                ss.pop();
                if (ss.size()==0) {
                	symbols.remove(ss);
                }
                sortSymbols();
                fireSymbolChangeNotification(cmd);
                return;
            }
        }
    }

    public void removeAll() {
        fireSymbolPreChangeNotification(null);
        symbols = new ArrayList<Stack<Symbol>>(); // delete all the symbols
        sortSymbols(); // pointless, but keeps the error checking happy
        fireSymbolChangeNotification(null);
    }
    
    /**
     * Create an HTML table of the symbols
     * @return the symbols in HTML form.
     */
    public String displaySymbolTable() {
        String table ="<h1>Symbol Table</h1>";
        boolean docsyms = ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_SYMBOL_SUPPORT);
        boolean outsyms = ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_OUTPUT_SYMBOL_SUPPORT);
        table += "Symbol support is currently " +
        	(docsyms ? "enabled" : "disabled") + " for documents by default, " +
        	(outsyms ? "enabled" : "disabled") + " for output.<br/>" +
        	"Go to Preferences-&gt;Proof General to change.<br>";
        table += "<table><tr><th>Name</th><th>ascii</th><th>unicode</th><th>html</th><th>shortcut</th></tr>";
        for(Iterator i = symbols.iterator(); i.hasNext();) {
            Symbol s = (Symbol) ((Stack) i.next()).peek();
            table += "<tr><th>"+s.name+"</th><th>"+s.escapedAscii+"</th><th>"+s.unicode+
            "</th><th>"+s.html+"</th><th>"+s.shortcut+"</th></tr>";
        }
        table += "</table>";
        return table;
    }

    /**
     * Look up a color object, then convert it into a css color: rgb()
     * @param name
     * @return the CSS-formatted colour.
     */
   public static String color2css(String name) {
       try {
           Color col = PGColors.getColor(name); // FIXME da: names have changed, fix stylesheet usage
           if (col==null) {
        	   return "";
           }
           return "color: rgb("
           			+col.getRed()+ ","+col.getGreen()+","+col.getBlue()+"); ";
       } catch (Exception ex) {
           ex.printStackTrace();
           return "";
       }
   }

   /**
    * Listeners for symbol (pre)change events.
    * @author Daniel Winterstein
    * @author David Aspinall (simplified)
    */
   public interface SymbolChangeListener {
       public void symbolChangeNotification(Element content);
       public void symbolPreChangeNotification(Element content);
   }

   Collection<SymbolChangeListener> listeners = new ArrayList<SymbolChangeListener>();

   public void addSymbolChangeListener(SymbolChangeListener sl) {
       if (!listeners.contains(sl)) {
           listeners.add(sl);
       }
   	}
   
	public void removeSymbolChangeListener(SymbolChangeListener listener) {
	    listeners.remove(listener);
	}
	
   public void fireSymbolPreChangeNotification(Element e) {
	   fSorted = false;
	   for (SymbolChangeListener listener : listeners) {
		   listener.symbolPreChangeNotification(e);
	   }
   }
   
   public void fireSymbolChangeNotification(Element e) {
	   assert fSorted : "Symbol table changed but not sorted.";
   		for (SymbolChangeListener listener : listeners) {
   			listener.symbolChangeNotification(e);
   		}
   }
}

