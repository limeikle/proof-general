/*
/*
 *  $RCSfile: HTML2TextReader.java,v $
 *
 *  Created on 30 Apr 2006
 *  Taken from Eclipse internal code
 */
package ed.inf.utils.eclipse;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;

import org.eclipse.jface.text.TextPresentation;


/**
 * Reads the text contents from a reader of HTML contents and translates
 * the tags or cut them out.
 */
class HTML2TextReader extends SubstitutionTextReader {

	private static final String EMPTY_STRING= "";
	private static final Map<String,String> fgEntityLookup;
	private static final Set<String> fgTags;

	static {

		fgTags = new HashSet<String>();
		fgTags.add("b");
		fgTags.add("br");
		fgTags.add("h5");
		fgTags.add("p");
		fgTags.add("dl");
		fgTags.add("dt");
		fgTags.add("dd");
		fgTags.add("li");
		fgTags.add("ul");
		fgTags.add("pre");

		fgEntityLookup= new HashMap<String,String>(7);
		fgEntityLookup.put("lt", "<");
		fgEntityLookup.put("gt", ">");
		fgEntityLookup.put("nbsp", " ");
		fgEntityLookup.put("amp", "&");
		fgEntityLookup.put("circ", "^");
		fgEntityLookup.put("tilde", "~");
		fgEntityLookup.put("quot", "\"");
	}

	private int fCounter= 0;
	private final TextPresentation fTextPresentation;
	private int fBold= 0;
	private int fStartOffset= -1;
	private boolean fInParagraph= false;
	private boolean fIsPreformattedText= false;

	/**
	 * Transforms the HTML text from the reader to formatted text.
	 *
	 * @param reader the reader
	 * @param presentation If not <code>null</code>, formattings will be applied to
	 * the presentation.
	*/
	public HTML2TextReader(Reader reader, TextPresentation presentation) {
		super(new PushbackReader(reader));
		fTextPresentation= presentation;
	}

	@Override
    public int read() throws IOException {
		int c= super.read();
		if (c != -1) {
			++ fCounter;
		}
		return c;
	}

	protected void startBold() {
		if (fBold == 0) {
			fStartOffset= fCounter;
		}
		++ fBold;
	}

	protected void startPreformattedText() {
		fIsPreformattedText= true;
		setSkipWhitespace(false);
	}

	protected void stopPreformattedText() {
		fIsPreformattedText= false;
		setSkipWhitespace(true);
	}

	protected void stopBold() {
		-- fBold;
		if (fBold == 0) {
			if (fTextPresentation != null) {
				fTextPresentation.addStyleRange(new StyleRange(fStartOffset, fCounter - fStartOffset, null, null, SWT.BOLD));
			}
			fStartOffset= -1;
		}
	}

	/*
	 * @see org.eclipse.jdt.internal.ui.text.SubstitutionTextReader#computeSubstitution(int)
	 */
	@Override
    protected String computeSubstitution(int c) throws IOException {

		if (c == '<') {
			return  processHTMLTag();
		} else if (c == '&') {
			return processEntity();
		} else if (fIsPreformattedText) {
			return processPreformattedText(c);
		}
		return null;
	}

	private String html2Text(String html) {

		if (html == null || html.length() == 0) {
			return EMPTY_STRING;
		}
		String tag= html;
		if ('/' == tag.charAt(0)) {
			tag= tag.substring(1);
		}
		if (!fgTags.contains(tag)) {
			return EMPTY_STRING;
		}
		if ("pre".equals(html)) {
			startPreformattedText();
			return EMPTY_STRING;
		}

		if ("/pre".equals(html)) {
			stopPreformattedText();
			return EMPTY_STRING;
		}

		if (fIsPreformattedText) {
			return EMPTY_STRING;
		}
		if ("b".equals(html)) {
			startBold();
			return EMPTY_STRING;
		}
		if ("h5".equals(html) || "dt".equals(html)) {
			startBold();
			return EMPTY_STRING;
		}

		if ("dl".equals(html)) {
			return LINE_DELIM;
		}
		if ("dd".equals(html)) {
			return "\t";
		}
		if ("li".equals(html)) {
			return LINE_DELIM + " - ";  //TODO is this the right char?  (made it up) -AH
			  //ContentAssistMessages.getString("HTML2TextReader.listItemPrefix");
		}
		if ("/b".equals(html)) {
			stopBold();
			return EMPTY_STRING;
		}

		if ("p".equals(html))  {
			fInParagraph= true;
			return LINE_DELIM;
		}

		if ("br".equals(html)) {
			return LINE_DELIM;
		}
		if ("/p".equals(html))  {
			boolean inParagraph= fInParagraph;
			fInParagraph= false;
			return inParagraph ? EMPTY_STRING : LINE_DELIM;
		}

		if ("/h5".equals(html) || "/dt".equals(html)) {
			stopBold();
			return LINE_DELIM;
		}

		if ("/dd".equals(html)) {
			return LINE_DELIM;
		}
		return EMPTY_STRING;
	}

	/*
	 * A '<' has been read. Process a HTML tag
	 */
	private String processHTMLTag() throws IOException {

		StringBuffer buf= new StringBuffer();
		int ch;
		do {

			ch= nextChar();

			while (ch != -1 && ch != '>') {
				buf.append(Character.toLowerCase((char) ch));
				ch= nextChar();
				if (ch == '"'){
					buf.append(Character.toLowerCase((char) ch));
					ch= nextChar();
					while (ch != -1 && ch != '"'){
						buf.append(Character.toLowerCase((char) ch));
						ch= nextChar();
					}
				}
				if (ch == '<'){
					unread(ch);
					return '<' + buf.toString();
				}
			}

			if (ch == -1) {
				return null;
			}
			int tagLen= buf.length();
			// needs special treatment for comments
			if ((tagLen >= 3 && "!--".equals(buf.substring(0, 3)))
				&& !(tagLen >= 5 && "--!".equals(buf.substring(tagLen - 3)))) {
				// unfinished comment
				buf.append(ch);
			} else {
				break;
			}
		} while (true);

		return html2Text(buf.toString());
	}

	private String processPreformattedText(int c) {
		if  (c == '\r' || c == '\n') {
			fCounter++;
		}
		return null;
	}


	private void unread(int ch) throws IOException {
		((PushbackReader) getReader()).unread(ch);
	}

	protected String entity2Text(String symbol) {
		if (symbol.length() > 1 && symbol.charAt(0) == '#') {
			int ch;
			try {
				if (symbol.charAt(1) == 'x') {
					ch= Integer.parseInt(symbol.substring(2), 16);
				} else {
					ch= Integer.parseInt(symbol.substring(1), 10);
				}
				return EMPTY_STRING + (char)ch;
			} catch (NumberFormatException e) {
			}
		} else {
			String str= fgEntityLookup.get(symbol);
			if (str != null) {
				return str;
			}
		}
		return "&" + symbol; // not found
	}

	/*
	 * A '&' has been read. Process a entity
	 */
	private String processEntity() throws IOException {
		StringBuffer buf= new StringBuffer();
		int ch= nextChar();
		while (Character.isLetterOrDigit((char)ch) || ch == '#') {
			buf.append((char) ch);
			ch= nextChar();
		}

		if (ch == ';') {
			return entity2Text(buf.toString());
		}
		buf.insert(0, '&');
		if (ch != -1) {
			buf.append((char) ch);
		}
		return buf.toString();
	}
}
