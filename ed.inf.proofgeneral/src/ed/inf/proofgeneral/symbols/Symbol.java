/*
 *  $RCSfile: Symbol.java,v $
 *
 *  Created on 30 Sep 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.symbols;

import java.util.regex.Pattern;

import org.dom4j.Element;
import org.dom4j.tree.DefaultElement;

import ed.inf.proofgeneral.sessionmanager.InterfaceScriptSyntax;
import ed.inf.proofgeneral.sessionmanager.ScriptingException;
import ed.inf.utils.datastruct.StringManipulation;

/**
 * A data-structure storing the ASCII proof script version of a symbol,
 * its Unicode and HTML display versions, and a typing shortcut.
 * The regular expression created for the typing shortcut will only work
 * on whole strings (i.e. separated by white space).
 *
 * @author Daniel Winterstein
 */
// da TODO: would be excellent to offer completion/selection of symbols.
// Are regexps here really a saving over plain strings?
// FIXME da: bugginess here/elsewhere is that desymbolising substitution is applied
// to whole of XML command!  Really should only be applied to text/content
// portions, not element/attribute names.
public class Symbol {

    public String family;
    public String name;
    public String ascii;
    public String html;
    public String shortcut;
    public String unicode;

    // Constants for status
    public static final String ENABLED = "on"; // Use this symbol!
    public static final String HIDDEN = "hidden"; // Indicates that another symbol is over-riding this one
    public static final String DISABLED = "off";

    /**
     * The status of this symbol: enabled (in use), hidden, or disabled.
     * Use the constant strings when setting.
     * Note that status is not saved: it is only a runtime property of a symbol.
     */
    String status = ENABLED;

    /**
     * XML escaped version of ascii
     */
    final String escapedAscii;

    /** XML escaped version of shortcut */ //-AH
    String escapedShortcut = null;

    /**
     * Regex escaped versions
     */
    /**  */
    final String rAscii;
    /**  */
    final String rShortcut;
    /**  */
    final String rEscapedAscii;
    /**  */
    final String rEscapedShortcut;
    /**  */
    final String rHtml;
    /**  */
    final Pattern pAscii;
    /**  */
    final Pattern pEscaped;
    // da: maybe not needed CLEANUP
    //final Pattern pShortcut;
    /**  */
    final Pattern pHtml;

    /**
     * Normalise the string argument by
     * @param arg
     * @return normalised version of arg: whitespace is trimmed, and if empty we return null.
     */
    private static String nullforempty(String arg) {
    	if (arg == null) {
    		return null;
    	}
		String trimmed = arg.trim();
        if (trimmed.equals("")) {
        	return null;
        }
		return trimmed;
    }

    /**
     * Compiles all the strings into regular expressions for a slight speed boost.
     * These patterns are adjusted to *only* match at word boundaries.
     * @param name
     * @param ascii
     * @param unicode (can be the actual unicode character, or the hex sequence)
     * @param html
     * @param shortcut
     *
     * @throws ScriptingException if the parameters are not acceptable (e.g., no ASCII)
     */
    public Symbol(String name, String ascii, String unicode, String html, String shortcut, String family)
    throws ScriptingException {
        name = nullforempty(name);
        family = nullforempty(family);
        unicode = nullforempty(unicode);
        html = nullforempty(html);
        shortcut = nullforempty(shortcut);
        this.family = family;
        this.name = name==null? ascii : name;
    	if (unicode != null && unicode.length()>3) {
    		String[] unicodes = unicode.split(",");
    		unicode="";
    		for(int i=0; i<unicodes.length; i++) {
    			if (unicodes[i].length()==1) {
    				unicode += unicodes[i];
    			} else {
    				if (unicodes[i].startsWith("\\u")) {
    					unicodes[i] = unicodes[i].substring(2);
    				}
    				if (unicodes[i].startsWith("#")) {
    					unicodes[i] = unicodes[i].substring(1);
    				}
    				if (unicodes[i].length()==4) {
    					char hexCode = (char) Integer.decode("#"+unicodes[i]).intValue();
    					unicode += Character.toString(hexCode);
    				} else {
    					throw new ScriptingException("Could not identify the hex code for the unicode character "+name);
    				}
    			}
    		}
    	}
        this.unicode = unicode;
        this.html = html;
        this.ascii = ascii;
        if (ascii==null) {
			throw new ScriptingException("Tried to create a symbol without specifying an ascii equivalent.");
		}
        this.shortcut = shortcut;
        escapedAscii = xmlEscape(ascii);
        escapedShortcut = xmlEscape(shortcut);

        // Adjust patterns to *only* match at word boundaries.
        rAscii = StringManipulation.regexEsc(ascii);
        rShortcut = StringManipulation.regexEsc(shortcut);
        rEscapedAscii = StringManipulation.regexEsc(escapedAscii);
        rEscapedShortcut = StringManipulation.regexEsc(escapedShortcut);

        String rAscii2 = rAscii;
        String rEscaped2 = rEscapedAscii;

        // Protect against patterns inside words
        if (Pattern.compile("^\\w").matcher(ascii).find()) {
        	rAscii2 = "\\b" + rAscii2;
        	rEscaped2 = "\\b" + rEscaped2;
        }
        if (Pattern.compile("\\w$").matcher(ascii).find()) {
        	rAscii2 += "\\b";
        	rEscaped2 += "\\b";
        }

        pAscii = Pattern.compile(rAscii2);
        pEscaped = Pattern.compile(rEscaped2);
// da: this field not used.  Do we really need all these versions?  CLEANUP
//        if (this.shortcut!=null) {
//        	// shortcuts can only match a whole string
//            pShortcut = Pattern.compile("^"+StringManipulation.regexEsc(this.shortcut)+"$");
//        } else {
//        	pShortcut = null;
//        }

        if (this.html!=null) {
            rHtml = StringManipulation.regexEsc(html);
            pHtml = Pattern.compile(rHtml);
        } else {
            rHtml = null; pHtml = null;
        }
    }

		private String xmlEscape(String text) {
			if (text==null) {
				return null;
			}
			Element e = new DefaultElement("dummy");
			e.setText(text);
			String escapedAsciiTemp = e.asXML();
			return escapedAsciiTemp.substring(7,escapedAsciiTemp.length()-8);
		}

    /**
     * Redefine toString so that it spits out the PGIP command for this symbol.
     */
    @Override
    public String toString() {
        Element cmd = new DefaultElement(InterfaceScriptSyntax.ADD_SYMBOL_COMMAND);
        if (family!=null) {
			cmd.addAttribute(InterfaceScriptSyntax.FAMILY,family);
		}
        cmd.addAttribute(InterfaceScriptSyntax.NAME,name);
        cmd.addAttribute(InterfaceScriptSyntax.ASCII,ascii);
        if (shortcut != null) {
			cmd.addAttribute(InterfaceScriptSyntax.SHORTCUT,shortcut);
		}
        if (unicode != null) {
        	String uc="";
        	for(int i=0; i<unicode.length(); i++) {
        		String ucc = Integer.toHexString(unicode.charAt(i));
		        // leading zeros are lost in this, but needed for reconversion
		        // so we add them back in:
		        if (ucc.length()<4) {
		            ucc = "0000".substring(ucc.length()) + ucc;
		        }
		        uc += ucc;
		        if (i < unicode.length()-1) {
					uc += ",";
				}
        	}
        	cmd.addAttribute(InterfaceScriptSyntax.UNICODE,uc);
        }
        if (html != null) {
			cmd.addAttribute(InterfaceScriptSyntax.HTML,html);
		}
        return cmd.asXML();
    }
    /**
     * Do these 2 symbols overlap in any of their attributes?
     * Ignores shortcut.
     * Note that for ascii, an overlap is defined as one
     * symbol being a substring of the other. For other attributes, equality is used.
     * Note that this means ambiguous symbols *can* be created via overlapping unicode strings.
     * @param sym2
     * @return false if no overlap, true otherwise.
     */
    public boolean overlap(Symbol sym2) {
        if (sym2.name.equals(name)) {
        	return true;
        }
        if (sym2.ascii.indexOf(ascii)!=-1 || ascii.indexOf(sym2.ascii)!=-1) {
			return true;
		}
        if (sym2.html != null && sym2.html.equals(html)) {
			return true;
		}
        if (sym2.unicode != null && sym2.unicode.equals(unicode)) {
			return true;
		}
//                sym2.unicode != ""    // This guards against partial unicode matching
//        	&& unicode != null && unicode != ""
//        	&& (sym2.unicode.indexOf(unicode)!=-1 || unicode.indexOf(sym2.unicode)!=-1)
//        	) return true;
        return false;
    }
    /**
     * Ignores shortcut.
     */
    // da: renamed this equivalent instead of equals to avoid hashCode warnings/problems
     public boolean equivalent(Object arg0) {
        if (arg0==this) {
        	return true;
        }
        if (arg0 instanceof Symbol) {
            try {
            	Symbol s2 = (Symbol) arg0;
            	if (s2.name.equals(name) && s2.ascii.equals(ascii) &&
            	    s2.html.equals(html) && s2.unicode.equals(unicode)) {
            		return true;
            	}
            	return false;
            } catch(NullPointerException x) {
            	return false;
            }
        }
        return false;
     }
}