/*
 *  $RCSfile: StringManipulation.java,v $
 *
 *  Created on 01 Nov 2006
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.datastruct;

import java.security.MessageDigest;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ed.inf.utils.MiscUtils;

/**
 * Static methods to manipulate strings.
 * @author AH
 * @author Daniel Winterstein
 */
public class StringManipulation {

	/**
	 * The local line-end string. \n on unix, \r\n on windows, \r on mac.
	 */
	public static final String LINEEND = System.getProperty("line.separator");

	/** The pattern representing whitespace */
	private static final Pattern WHITESPACE = Pattern.compile("\\S");

	/**
	 * Remove leading <i>and</i> trailing whitespace from a string.
	 *
	 * @param s
	 * @return trimmed string or "" - <b>never</b> returns null, even if passed null.
	 */
	public static String trim(String s) {
		if (s == null) {
			return "";
		}
		s = s.trim();
		// da: do these next steps really do something more? 
		// Don't think \\S matches character code above 0u0020
		// FIXME: probably remove this method.
		Matcher m = StringManipulation.WHITESPACE.matcher(s);
		if (!m.find()) {
			return "";
		}
		return s.substring(m.start());
	}

	/**
	 * Ensures that all line breaks in a string use the correct line terminator
	 * @param text the string to convert
	 * @return the string, with all incorrect line-end characters replaced.
	 */
	public static String convertLineBreaks(String text) {
		return text.replaceAll("[(\r\n)(\r)(\n)]", LINEEND);
	}

	/**
	 * Ensure that a single line ends with the correct line terminator
	 * @param line the line to convert
	 * @return the string, terminated with the correct line-end character.
	 */
	// da: should check JLS carefully, but I think we only need to do this
	// for strings that are to be sent to streams, etc.  Console output
	// and GUI display is safe with just \n.
	public static final String convertLineBreak(String line) {
		// strip all existing line-terminators
		line = line.replaceAll("[(\r\n)(\r)(\n)]$", LINEEND);
		// add system-specific line-enc
		return line.replaceAll("([^" + LINEEND + "])$", "$1" + LINEEND);
	}

	/**
	 * A version of lineEnd for string buffers.
	 * @param line
	 */
	public static final void lineEnd(StringBuffer line) {
		if (line.length() == 0) {
			line.append(LINEEND);
			return;
		}
		// strip possibly inappropriate line-endings
		char last = line.charAt(line.length() - 1);
		if (last == '\n') {
			if (line.length() > 1 && line.charAt(line.length() - 2) == '\r') {
				// \r\n
				line.replace(line.length() - 2, line.length(), LINEEND);
				return;
			}
			line.replace(line.length() - 1, line.length(), LINEEND);
			return;
		}
		if (last == '\r') {
			line.replace(line.length() - 1, line.length(), LINEEND);
			return;
		}
		line.append(StringManipulation.LINEEND);
		return;
	}

	/**
	 * Returns true if the string is just whitespace, or empty, or null.
	 * @param s the string to test.
	 * @return true if the string contains nothing but whitespace, false if
	 *         there are any other characters contained within.
	 */
	public static final boolean isWhitespace(String s) {
		if (s == null) {
			return true;
		}
		return !StringManipulation.WHITESPACE.matcher(s).find();
	}

	/**
	 * @param s
	 * @return a version of s where the first letter is upeercase and all others are lowercase
	 */
	public static final String capitalise(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
	}

	/**
	 * Escape some of the characters that are meaningful to the regex engine.
	 * (i.e. create a string that will be treated by replace as if it were
	 * literally the input string).
	 *
	 * @param s the plain 'regular' regexp.
	 * @return an escaped string.
	 * @see Pattern
	 */
	public static final String regexEsc(String s) {
		if (s == null) {
			return null;
		}
		// da: was the below intended to produce something different to this?
		// CLEANUP
		// return Matcher.quoteReplacement(s);
		// da: apparently yes, otherwise symbol replacement goes pear-shaped.
		// TODO: investigate this.  Some uses of this method look like
		// they should be just Pattern.quote(s) .

		// these daft looking replacements should create the right level of
		// escaping for \ characters
		s = s.replaceAll("\\\\", "\\\\\\\\"); // replaces \ in s with \\, honest.
		s = s.replaceAll("\\$", "\\\\\\$"); // replaces $ in s with \$
		s = s.replaceAll("\\^", "\\\\\\^"); // replaces ^ in s with \^
		s = s.replaceAll("\\|", "\\\\\\|"); // replaces | in s with \|
		s = s.replaceAll("\\(", "\\\\\\("); // replaces ( in s with \(
		s = s.replaceAll("\\)", "\\\\\\)"); // replaces ) in s with \)
		s = s.replaceAll("\\[", "\\\\\\["); // replaces [ added by AH for lbrakk
											// symbol
		s = s.replaceAll("\\]", "\\\\\\]"); // replaces ]
		return s;
	}

	public static String glob2regexp(String s) {
		s = s.replaceAll(Pattern.quote("."), Matcher.quoteReplacement("\\."));
		s = s.replaceAll(Pattern.quote("*"), Matcher.quoteReplacement(".*"));
		s = s.replaceAll(Pattern.quote("?"), Matcher.quoteReplacement("."));
		s = s.replaceAll(Pattern.quote("^"), Matcher.quoteReplacement("\\^"));
		s = s.replaceAll(Pattern.quote("|"), Matcher.quoteReplacement("\\|"));
		s = s.replaceAll(Pattern.quote("("), Matcher.quoteReplacement("\\("));
		s = s.replaceAll(Pattern.quote(")"), Matcher.quoteReplacement("\\)"));
		s = s.replaceAll(Pattern.quote("$"), Matcher.quoteReplacement("\\$"));
		return s;
	}

	public static String regexp2glob(String s) {
		s = s.replaceAll(Pattern.quote("\\^"), Matcher.quoteReplacement("^"));
		s = s.replaceAll(Pattern.quote("\\|"), Matcher.quoteReplacement("|"));
		s = s.replaceAll(Pattern.quote("\\("), Matcher.quoteReplacement("("));
		s = s.replaceAll(Pattern.quote("\\)"), Matcher.quoteReplacement(")"));
		s = s.replaceAll(Pattern.quote("\\$"), Matcher.quoteReplacement("$"));
		s = s.replaceAll(Pattern.quote(".*"),  Matcher.quoteReplacement("*"));
		s = s.replaceAll("[^\\\\]\\.",         Matcher.quoteReplacement("?"));
		s = s.replaceAll(Pattern.quote("\\."), Matcher.quoteReplacement("."));
		return s;
	}


	/**
	 * replaces all occurrences of 'sub' in 'str' with 'rep'
	 * @deprecated - use {@link String#replaceAll(String, String)}
	 */ @Deprecated
	public static String replace(String str, String sub, String rep) {
		StringBuffer res = new StringBuffer();
		int li = 0;
		int i = str.indexOf(sub, li);
		while (i >= li) {
			if (i > li) {
				res.append(str.substring(li, i));
			}
			res.append(rep);
			li = i + sub.length();
			i = str.indexOf(sub, li);
		}
		res.append(str.substring(li));
		return res.toString();
	}

	/**
	 * replaces all occurrences of 'sub' in 'str' with 'rep', ignoring case.
	 * @deprecated - why not use {@link String#replaceAll(String, String)}?
	 */ @Deprecated
	public static String replaceIgnoreCase(String str, String sub, String rep) {
		String strl = str.toLowerCase();
		String subl = sub.toLowerCase();
		StringBuffer res = new StringBuffer();
		int li = 0;
		int i = strl.indexOf(subl, li);
		while (i >= li) {
			if (i > li) {
				res.append(str.substring(li, i));
			}
			res.append(rep);
			li = i + sub.length();
			i = strl.indexOf(subl, li);
		}
		res.append(str.substring(li));
		return res.toString();
	}

	/**
	 * Returns a random string made from the given characters
	 */
	public static String makeRandomString(int length, char[] characters) {
		StringBuilder sb = new StringBuilder();
		while ((length--) > 0) {
			sb.append(characters[(int) (characters.length * Math.random())]);
		}
		return sb.toString();
	}

	/** The digits 0-9 */
	static final char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };

	/** The hexadecimal digits 0-f (lowercase) */
	private static final char[] hex_digits = { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	/** The digits 0-9, and the (lowercase) roman alphabet. */
	private static final char[] digits_and_lowercase = { '0', '1', '2', '3',
			'4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g',
			'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
			'u', 'v', 'w', 'x', 'y', 'z' };

	public static String makeRandomString(int length) {
		return makeRandomString(length, digits_and_lowercase);
	}

	public static String makeRandomDigitsString(int length) {
		return makeRandomString(length, digits);
	}

	public static String makeRandomHexString(int length) {
		return makeRandomString(length, hex_digits);
	}

	/**
	 * Returns the first occurrence of either sub1 or sub2
	 */
	  public static int getFirstOccurrence(String s, int start, char sub1, char sub2) {
	    int i=s.indexOf(sub1, start);
	    int j=s.indexOf(sub2, start);
	    if (i==-1) {
			return j;
		}
	    if (j==-1 || i<j) {
			return i;
		}
	    return j;
	  }

	  /**
	   * Converts all 'special' characters (\r, \n, \\, and \") in Java
	   * to their escaped equivalent
	   */
	  public static String toJavaString(String s) {
	    StringBuffer sb = new StringBuffer();
	    int len=s.length();
	    for (int i=0; i<len; i++) {
	      char c=s.charAt(i);
	      switch (c) {
	        case '\n': sb.append("\\n"); break;
	        case '\r': sb.append("\\r"); break;
	        case '\\': sb.append("\\\\"); break;
	        case '\"': sb.append("\\\""); break;
	        case '\'': sb.append("\\\'"); break;
	        default: sb.append(c); break;
	      }
	    }
	    return sb.toString();
	  }

	  /**
	   * returns the number of times 'substr' occurs in 's'
	   * @return the number of occurrences, otherwise
	   * @throws IndexOutOfBoundsException if substr's length is 0
	   */
	  public static int countOccurrences(String s, String substr) {
	    int count=0;
	    int end = s.length() - substr.length();
	    int lenRest = substr.length()-1;
	    if (lenRest==-1) {
			throw new IndexOutOfBoundsException("cannot count occurrences of empty string in '"+s+"'");
		}
	    char startChar = substr.charAt(0);
	    for (int i=0; i<=end; i++) {
	      if (s.charAt(i)==startChar) {
	        int j=1;
	        for (; j<lenRest; j++) {
	          if (s.charAt(i+j)!=substr.charAt(j)) {
				break;
			}
	        }
	        if (j>=lenRest) {
				count++;
			}
	      }
	    }
	    return count;
	  }

	  /**
	   * Removes HTML-style tags from a string.
	   * @param s a String from which to remove tags
	   * @return a string with all instances of <.*> removed.
	   */
	  public static String removeTags(String s) {
	    StringBuffer sb = new StringBuffer();
	    boolean inTag = false;
	    for (int i=0; i<s.length(); i++) {
	      char c = s.charAt(i);
	      if (c=='<') {
			inTag=true;
		}
	      if (!inTag) {
			sb.append(c);
		}
	      if (c=='>') {
			inTag=false;
		}
	    }
	    return sb.toString();
	  }

	  //NOTE old parenthesesMatch routine was buggy; i've rewritten it  -AH
	  
	  /** determines whether parentheses match in the given string;
	   *  returns true if there are the same number of brackets (round, square, and curly) 
	   *  excluding those in single quotes or in double quotes or after backslashes;
	   *  requiring the opener to appear before the closer,
	   *  and being strict about regions not partially overlapping
	   *  (e.g. "(a [b) c]" is not allowed)
	   */
	  public static boolean parenthesesMatch(String s) {
		  return parenthesesMatch(s, true, true, true, false, false);
	  }
	  /** determines whether parentheses match in the given string
	   * 
	   * @param s string to check
	   * @param ignoreInSingleQuotes whether to ignore everything between paired single quotes;
	   * 	when ignoring items in quotes, this implementation looks for the first quote char (single and/or double, as specified),
	   *    then the first match, and ignores the contents between them;
	   *    the only exception is if backslash is used to escape quotes and backslashEscapes is set
	   * @param ignoreInDoubleQuotes as ignoreInSingleQuotes but for double quotes
	   * @param backslashEscapes ignores any bracket or quote or backslash immediately after an unescaped backslash
	   * @param requireOpenBeforeClose if false, )( will be accepted as valid
	   * @param requireStrictMatching if false, [(]) will be accepted as valid
	   * @return true if the string is well-formed
	   */
    public static boolean parenthesesMatch(String s, 
			  boolean ignoreInSingleQuotes,
			  boolean ignoreInDoubleQuotes,
			  boolean backslashEscapes,
			  boolean requireOpenBeforeClose, 
			  boolean requireStrictMatching) {
	    if (s==null) {
	    	return true;
	    }
	    Stack<Character> parenCharsFound = new Stack<Character>(); 
	    int countParentheses=0;
	    int countBrackets=0;
	    int countBraces=0;
	    char activeQuote=0;
	    for (int i=0; i<s.length(); i++) {
	      char c = s.charAt(i);
	      if (c=='\\' && backslashEscapes) {
	    	  i++;
	    	  continue;
	      }
	      if (activeQuote!=0) {
	    	  if (c==activeQuote) {
	    		  activeQuote=0;
	    	  }
	    	  continue;
	      }
	      if ((c=='\'' && ignoreInSingleQuotes) || (c=='\"' && ignoreInDoubleQuotes)) {
	    	  activeQuote=c;
	    	  continue;
	      }
	      switch (c) {
	      case '\'':
	    	  if (ignoreInSingleQuotes) activeQuote=c;
	    	  break;
	      case '\"':
	    	  if (ignoreInDoubleQuotes) activeQuote=c;
	    	  break;
	      case '[':
	    	  countBrackets++;
	    	  if (requireStrictMatching) parenCharsFound.push(c);
	    	  break;
	      case ']':
	    	  countBrackets--;
	    	  if (requireOpenBeforeClose && countBrackets<0) return false;
	    	  if (requireStrictMatching && (parenCharsFound.isEmpty() || '[' != parenCharsFound.pop().charValue())) return false;
	    	  break;
	      case '{':
	    	  countBraces++;
	    	  if (requireStrictMatching) parenCharsFound.push(c);
	    	  break;
	      case '}':
	    	  countBraces--;
	    	  if (requireOpenBeforeClose && countBrackets<0) return false;
	    	  if (requireStrictMatching && (parenCharsFound.isEmpty() || '{' != parenCharsFound.pop().charValue())) return false;
	    	  break;
	      case '(':
	    	  countParentheses++;
	    	  if (requireStrictMatching) parenCharsFound.push(c);
	    	  break;
	      case ')':
	    	  countParentheses--;
	    	  if (requireOpenBeforeClose && countBrackets<0) return false;
	    	  if (requireStrictMatching && (parenCharsFound.isEmpty() || '(' != parenCharsFound.pop().charValue())) return false;
	    	  break;
	      default:
	    	  break;
	      } // end of switch ()
	    }
	    //Message.message("parentheses match value " + s + ", " + stop + ", match " + match + ", " + countBrackets);
	    return (activeQuote==0 && countBrackets==0 && countBraces==0 && countParentheses==0 && parenCharsFound.isEmpty());
	  }

	/**
	 * Removes substring 'start' from the beginning of string 's', if it occurs there.
	 */
	  public static String cutFromStartOfString(String s, String start) {
		  //Message.message("cutFromStart testing '"+start+"' at the beginning of '"+s+"'");
		  if (start==null || s==null) {
			return s;
		}
		  if (s.startsWith(start)) {
			return s.substring(start.length());
		}
		  return s;
	  }

	/**
	 * Replaces all occurrences of s1 in s with s2
	 * @deprecated - why not use {@link String#replaceAll(String, String)}?
	 */@Deprecated
	 public static String sReplace(String s, String s1, String s2) {
		 if (s==null || s1==null || s2==null) {
			return null;
		}
		 int i=0;
		 while ( (i=s.indexOf(s1, i) )>=0) {
			 s = s.substring(0, i) + s2 + s.substring(i + s1.length());
			 i+=s2.length();
		 }
		 return s;
	 }

	/**
	 * Returns true if the two strings are equal, allowing one * at the end as a wildcard,
	 * and possibly more (?, ., etc)
	 */
	  public static boolean wildCardMatch(String s1, String s2) {
	    if ("*".equals(s1) || "*".equals(s2)) {
			return true;
		}
	    if (s1==null || s2==null) {
			return (s1==null && s2==null);
		}
	    int s1w = s1.indexOf("*");
	    int s2w = s2.indexOf("*");
	    if (s1w>0 || s2w>0) {
	      int sw = MiscUtils.minNonNeg(s1w, s2w);
	      //if (sw<=0 || (sw>s2w && s2w>0)) sw=s2w;  //pretty sure should have been <0 here
	      if (s1.length()<=sw || s2.length()<=sw) {
			return false;
		}
	      if (s1.substring(0, sw).equals(s2.substring(0, sw))) {
			return wildCardMatch(s1.substring(sw), s2.substring(sw));
		}
		return false;
	    }
	    return (s1.equals(s2));
	  }

	/**
	 * make a (almost) unique hash string given the input string
	 * @return the md5 digest of the input string.
	 */
	public static String makeHashId(String s) {
		try {
			//Message.message("hash for '"+s+"' is "+Password.md5crypt(s));
			MessageDigest md = MessageDigest.getInstance("md5");
			byte[] digested = md.digest(s.getBytes());

		    // convert byte -> hex to make a character
		    StringBuilder sb = new StringBuilder();
		    for (byte b : digested) sb.append(b);
		    return sb.toString();

		} catch (java.security.NoSuchAlgorithmException e) {
			System.err.println("makeHashId using md5crypt: "+e);
			// Message.logWarning("makeHashId using md5crypt: "+e);
			return "-";
		}
	}

	/**
	 * makes a random id string (letters and numbers) of the given length
	 * @param l the number of characters to generate
	 * @return a string of length 'l'
	 */
	  public static String makeRandomId(int l) {
	    char[] id = new char[l];
	    double d=0;
	    for (int i=0; i<l; i++) {
	      if (i%3==0) {
			d=Math.random();
		} else {
			d=d-Math.floor(d);
		}
	      // the following test ensures that the id doesn't start with a digit
	      if (i==0){
	        d=d*(26+26);
	      }
	      else{
	        d=d*(26+26+10);
	      }
	      int c = (int)(d);
	      //Message("random " + c + " " + d);
	      if (c<26) {
			id[i]=(char)('A'+c);
		} else if (c<52) {
			id[i]=(char)('a'+c-26);
		} else if (c<62) {
			id[i]=(char)('0'+c-52);
		} else {
			id[i]='!'; //shouldn't come here
		}
	    }
	    //Message.message("random id is " + id);
	    return new String(id);
	  }

	  /**
	   * TODO will need a decoder, plus this (i think) is done in several places ??
	   * @param s
	   */
	  public static String makeHexString(String s) {
	    char[] c = s.toCharArray();
	    StringBuffer result = new StringBuffer();
	    for (int i=0; i<c.length; i++) {
	      result.append(NumericStringUtils.makePaddedString(Integer.toString(c[i], 16), 2, '0'));
	    }
	    return result.toString();
	  }

		/**
     * Return the first line of the given string, using either \n or the system new line sequence.
	 * @param s
	 * @return the first line of the string s
	 */
	public static String firstLine(String s) {
		int lineend = s.indexOf(LINEEND);
		if (lineend != -1) {
			return s.substring(0,lineend);
		}
		int crend = s.indexOf("\n");
		if (crend != -1) {
			return s.substring(0,crend);
		}
		return s;
	}
	
	
	  /**
	 * Trim the given text to its first line, with whitespace removed,
	 * up to the given maximum length.  The 
	 * ellipsis string is "..." added to the end if the text was abbreviated.
	 * @param text
	 * @param maxlen
	 * @return the abbreviated string
	 */
	public static String ellipsisTrim(String text, int maxlen) {
		String text1 = firstLine(text).trim();
		if (text1.length()>maxlen-3) {
			return text1.substring(0,maxlen-3)+"...";
		} else if (!text1.equals(text) &&
					text.indexOf("\n")!=-1 ||  
					// to be sure wasn't space at front
					text.indexOf(StringManipulation.LINEEND) != -1) {
			return text1 + "...";
		}
		return text1;
	}

	

}
