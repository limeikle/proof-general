/*
 *  $RCSfile: PGTextHover.java,v $
 *
 *  Created on 12 Oct 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.ui.texteditor.MarkerUtilities;

import ed.inf.proofgeneral.Constants;
import ed.inf.proofgeneral.NotNull;
import ed.inf.proofgeneral.document.DocElement;
import ed.inf.proofgeneral.document.ProofScriptMarkers;
import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.sessionmanager.KeyWord;
import ed.inf.proofgeneral.sessionmanager.PGIPSyntax;
import ed.inf.proofgeneral.sessionmanager.ProverInfo;
import ed.inf.proofgeneral.sessionmanager.ProverKnowledge;
import ed.inf.proofgeneral.sessionmanager.ProverSyntax;
import ed.inf.proofgeneral.sessionmanager.ProverKnowledge.KnowledgeItem;
import ed.inf.proofgeneral.sessionmanager.ProverKnowledge.LazyTheoryItem;
import ed.inf.utils.datastruct.StringManipulation;
import ed.inf.utils.file.FileUtils;

// da TODO: we should find out what the Java way of doing this is.
// Making markers seems a bit heavyweight.
// See DefaultTextHover.  Would be good if we could remove
// the association with the session manager/info/PK here.

/**
 * Provides helpful text hover tooltips.
 * These are based on pgmarkers created when parsing.
 * @author Daniel Winterstein
 */
public class PGTextHover implements ITextHover {

	@NotNull
	private final ProverInfo proverInfo;
	
	@NotNull
	private final ProverKnowledge proverKnowledge;

	/**
	 * Needs access to prover info in order to find the prover tooltips.
	 */
	public PGTextHover(ProverInfo proverInfo,ProverKnowledge proverKnowledge) {
		assert proverInfo != null : "ProverInfo must be non-null";
		assert proverKnowledge != null : "ProverKnowledgemust be non-null";
		this.proverInfo = proverInfo;
		this.proverKnowledge = proverKnowledge;
		try {
			loadTooltips();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	ProverInfo getProverInfo() {
		return proverInfo;
	}

	ProverKnowledge getProverKnowledge() {
		return proverKnowledge;
	}

	/**
	 * Loads tooltips stored as &lt;tooltip key="" value="" /&gt;
	 * @throws IOException if the relevant file cannot be found.
	 */
	@SuppressWarnings("unchecked")
	void loadTooltips() throws IOException {
		if (getProverInfo()==null) {
			return;
		}
		String proverName = getProverInfo().name;
		String path = "config/"+proverName+Constants.TOOLTIPS_FILE_NAME;
		try {
			File tt = FileUtils.findProverFile(proverName,path);
			if (tt == null) {
				throw new IOException("Could not find tooltips file: "+path);
			}
			SAXReader reader = new SAXReader();
			Document document = reader.read(tt);
			List<Element> list = document.selectNodes( "//tooltip" );
			proverTooltips = list.toArray(new Element[list.size()]);
		} catch (DocumentException ex) {
			throw new IOException(ex.getMessage());
		}
	}
	private static Element[] proverTooltips = null;

	/** A unique region indicating hover on the next command to process. */
	private static IRegion nextCommandHoverRegion = new Region(-2,0);

	/** A unique region indicating the next command to process. */
	private static IRegion currentStateHoverRegion = new Region(-1,0);
	
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		ProofScriptDocument doc = (ProofScriptDocument) textViewer.getDocument();
		if (isOnProcessedwhitespace(doc, offset)) {
			return currentStateHoverRegion;
		}
		if (isOnNextStepToProcess(doc, offset)) {
			return nextCommandHoverRegion;
		}
		return getWordRegion(doc,offset);
	}

	/**
	 * @param textViewer
	 * @param hoverRegion
	 * @return the word under the given hover region, or null if no word can/should be found
	 */
	public String getHoverWord(ITextViewer textViewer, IRegion hoverRegion) {
		// FIXME da: is this test necessary?
		if (hoverRegion == null) {
			return null;
		}
		try {
			// Only get words longer than 2 letters
			if (hoverRegion.getLength() > 2) { 
				ProofScriptDocument doc = (ProofScriptDocument) textViewer
				.getDocument();
				String word = doc.get(hoverRegion.getOffset(), hoverRegion
						.getLength());
				return word;
				//return getHelpString(word,doc);
			}
		} catch (Exception x) {
			x.printStackTrace();
		}
		return null;
	}

	/** 
	 * Return a string to be used PGSourceViewerConfiguration
	 * to populate DefaultInformationControl on hover.
	 * We construct a string, but we could make a class  
	 * implementing {@link ITextHoverExtension}
	 */
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		// TODO da: is hoverRegion allowed to be null here?  Are we allowed to return null?
		// Might be our choice: so let's do it to indicate no hover.
		if (hoverRegion == null) {
			return null;
		}
		if (hoverRegion == nextCommandHoverRegion) {
			return NEXTSTEPSTATE_HOVERINFO;
		}
		if (hoverRegion == currentStateHoverRegion) {
			// Indicates to display prover state in a popup
			return CURRENTSTATE_HOVERINFO;
		}
		String word = getHoverWord(textViewer, hoverRegion);
		return "SEARCH::"+word;
	}

	/** Hover info indicating to display the current proof state */
	public static final String CURRENTSTATE_HOVERINFO = "CURRENTSTATE";

	/** Hover info indicating to display the next proof state */
	public static final String NEXTSTEPSTATE_HOVERINFO = "NEXTSTEPSTATE";
	
	public static String hoverInfoSearchWord() {
		// TODO: if matches prefix above, return word suffix.
		return null;
	}

	private transient KnowledgeItem newKnowledge;
	private transient String newKnowledgeKeyword;
	
	/**
	 * Search for a help item relating to this keyword.
	 * First tries the markers, then the prover-tooltips, then the ProverSyntax.
	 * @param keyword
	 * @param doc (can be null)
	 * @param callback if we can't get the help string immediately,
	 *   if this is non-null, we will run this when we have been able to
	 */
	public String getHelpString(String keyword, ProofScriptDocument doc,
			Runnable callback) {
		//first try from markers
		IMarker m = null;
		IResource r =null;
		if (doc!=null && doc.getResource()!=null) {
				r = doc.getResource();
		}
		m = findMatchingMarker(r,keyword);
		if (m!=null) {
			return m.getAttribute(ProofScriptMarkers.TOOLTIP, "");
		}

		// try the prover tool-tips
		if (proverTooltips!=null) {
			for(int i=0; i<proverTooltips.length; i++) {
				//TODO maybe slow-- these should be organised better
				String k = proverTooltips[i].attributeValue("key");
				if (k!=null && k.equals(keyword)) {
					return proverTooltips[i].attributeValue("value");
				}
			}
		}

		// da: this gives type information for words which are learned from askids
		// in the Prover Object view.  But it isn't that useful...
		String kwtype = getKeywordType(keyword,
				(doc!=null ? doc.getProverSyntax() : proverInfo.proverSyntax));

		KnowledgeItem kni = getProverKnowledge().getItem(keyword);

		// da: New: try to make a lazy item which will look up a fully qualified keyword.
		// Now we never return null.  FIXME: Problem here: multiple entry will make multiple instances!
		if (kni==null) {
			if (keyword.equals(newKnowledgeKeyword) && newKnowledge != null) {
				kni = newKnowledge;
			} else {
				if (kwtype == null) {
					kwtype = "";
				}
				kni = getProverKnowledge().new LazyTheoryItem(keyword,kwtype,"Lookup "+keyword,null);
				newKnowledge = kni;
				newKnowledgeKeyword = keyword;
			}
		}

		String messageBody = kni.getStatementHtml();
		if (kni instanceof LazyTheoryItem) {
			LazyTheoryItem lt = (LazyTheoryItem) kni;
			if (!lt.isLoaded()) {
				if (lt.loadFullyBg(1,callback)==-1) {
					messageBody = kni.getStatementHtml();
				} else if (callback!=null) {
					messageBody = "<i>loading, please wait...</i>";
				} else {
					messageBody = "Statement could not be loaded.";
				}
			}
		}
		if (messageBody==null) {
			messageBody = "Statement not available.";
		}
		return kni.type+" <b>"+kni.id+"</b><p></p>"+messageBody;
	}

	/**
	 * @param keyword
	 * @param syntax
	 * @return The tooltip help string to display for this keyword.
	 *
	 * TODO: query the prover if necc.
	 */
	public static String getKeywordInfo(String keyword,ProverSyntax syntax) {
		KeyWord kw = syntax.getKeyWord(keyword);
		if (kw != null) {
			String type= kw.type;
			//String defined=kw.nameSpace.name;
			 // da: I don't understand what the /?'s are about here?
			//String skws = "type: "+type+"/? defined: "+defined+"/?";
			String skws = "Keyword type: "+type; // da: always global? +", defined: "+defined+".";
			return skws;
		}
		return null;
	}

	public static String getKeywordType(String keyword,ProverSyntax syntax) {
		KeyWord kw = syntax.getKeyWord(keyword);
		if (kw != null) {
			if (kw.type.equals("core")) {
				return "keyword";
			}
			return kw.type;
		}
		return null;
	}

	/**
	 * ASSUMPTION: PG markers are of the form Type: Name
	 * @param markers the markers to search within
	 * @param word the word to search for, within the markers
	 * @return the marker matching the search string
	 */
	public static IMarker findMatchingMarker(IMarker[] markers,String word) {
		for (int i=0; i<markers.length; i++) {
			String msg = MarkerUtilities.getMessage(markers[i]);
			if (msg==null) {
				continue;
			}
			if (msg.indexOf(':') != -1) {
				msg = msg.substring(msg.indexOf(':')+2);
				if (msg.equals(word)) {
					return markers[i];
				}
			}
		}
		return null;
	}
	/**
	 * Starting from this resource and going up the directory structure,
	 * look for a marker to match word.
	 * @param r the resource from which to begin the search, or null for the whole workspace
	 * @param word the word to match
	 * @return the marker matching the specified word
	 */
	public static IMarker findMatchingMarker(IResource r, String word) {
		IMarker[] ms;
		IMarker m=null;
		if (r == null) {
			r = ResourcesPlugin.getWorkspace().getRoot();
		}
		try {
			do {
				// da: added a new supertype here to restrict search for speed.
				ms = r.findMarkers(ProofScriptMarkers.LABEL_MARKER,true,IResource.DEPTH_INFINITE);
				m = findMatchingMarker(ms, word);
				r = r.getParent();
			} while(m==null && r!=null);
		} catch (Exception x) {x.printStackTrace();}
		return m;
	}
	
	public static boolean isOnProcessedwhitespace(ProofScriptDocument doc, int offset) {
		try {
			String s = doc.get(offset,1);
			if (StringManipulation.isWhitespace(s) && offset<=doc.getLockOffset()) {
				return true;
			}
		} catch (BadLocationException e) {
			// do nothing
		}
		return false;
	}
	
	public static boolean isOnNextStepToProcess(ProofScriptDocument doc, int offset) {
		if (doc.getProcessedOffset() < offset && 
				// FIXME: check that there is no queue in document instead of next test
				doc.getLockOffset() < offset) {
			DocElement elt = doc.findNext(PGIPSyntax.COMMAND, doc.getProcessedOffset());
			if (elt != null) {
				Position pos = elt.getPosition();
				if (pos.getOffset() + pos.getLength() > offset) {
					// We're hovering over the next command to process
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Find the word at/underneath position.
	 * Code lifted from DefaultTextDoubleClickStrategy.
	 * @param document the document to search
	 * @param position the character offset to search in the specified document
	 * @return the word requested
	 */
	public static IRegion getWordRegion(IDocument document, int position) {
		try {
			IRegion line= document.getLineInformationOfOffset(position);
			//exit if we're at the end of the line  ... why?  not sure, but maybe because that includes being to the right
			if (position == line.getOffset() + line.getLength()) {
				return null;
			}
//			fDocIter.setDocument(document, line);
//
//			BreakIterator breakIter= BreakIterator.getWordInstance();
//			breakIter.setText(fDocIter);
//
//			int start= breakIter.preceding(position);
//			if (start == BreakIterator.DONE)
//				start= line.getOffset();
//
//			int end= breakIter.following(position);
//			if (end == BreakIterator.DONE)
//				end= line.getOffset() + line.getLength();
//
//			if (breakIter.isBoundary(position)) {
//				if (end - position > position- start)
//					start= position;
//				else
//					end= position;
//			}

			//don't use the BreakIterator, it's too much work (usually, i think)
			//and it ignores numbers / underscores / etc
			//this one looks for valid word chars (including numbers and underscores, but not quotes or parens)
			String s = document.get(line.getOffset(), line.getLength());
			int i = position - line.getOffset();
			int start = i;
			int end = i;
			while (start>0 && (isWordChar(s.charAt(start-1)) || s.charAt(start-1)=='.')) {
				start--;
			}
			while (end<line.getLength() && isWordChar(s.charAt(end))) {
				end++;
			}
			if (start != end) {
				// TODO: da: would be good to find smallest enclosing document element too, and use the tooltip of
				// that as a fall back message.
				return new Region(start+line.getOffset(),end-start);
			}
		} catch (BadLocationException x) {
			x.printStackTrace();
		}
		return null;
	}

	public static boolean isWordChar(char c) {
		// TODO: use prover's given lexical syntax, which we can compile as regexp
		return (Character.isJavaIdentifierPart(c));   // approximate
	}

	public static String getWord(IDocument doc, int offset)
	throws BadLocationException{
		IRegion r= getWordRegion(doc,offset);
		return doc.get(r.getOffset(),r.getLength());
	}


}
