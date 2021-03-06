/*
 *  $RCSfile: Parser.java,v $
 *
 *  Created on 09 Sep 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.lazyparser;
import java.util.Iterator;

import org.dom4j.CharacterData;
import org.dom4j.Element;
import org.dom4j.Node;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TypedPosition;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.ContainerElement;
import ed.inf.proofgeneral.document.DocElement;
import ed.inf.proofgeneral.document.ProofScriptMarkers;
import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.pgip.Fatality;
import ed.inf.proofgeneral.preferences.PreferenceNames;
import ed.inf.proofgeneral.sessionmanager.PGIPSyntax;
import ed.inf.proofgeneral.sessionmanager.ScriptingException;
import ed.inf.utils.datastruct.StringManipulation;

/**
 * The base class for script parsers. Extend this class (or subclasses)
 * to create a new parser - then edit SessionManager to get your new parser used.
 * This base class provides some generic functionality for linking a parseresult with a document.
 * This job is made harder by:
 *
 * 1) The parseresult may mangle whitespace and linebreaks.
 * 2) The parseresult may mix elements whose xml markup is generated by the parser
 * (ie. normal proof script elements),
 * and elements whose xml markup appears in the text (ie. interface script elements).
 *
 * [These were faults in Isabelle 2005 which have now been resolved]
 * @author Daniel Winterstein
 */
public abstract class Parser {

	/** The syntax object used in parsing */
	protected final PGIPSyntax syntax;

	public Parser(PGIPSyntax syntax) {
		this.syntax = syntax;
	}


	/**
	 * The action that initiated the current/latest parse.
	 * Used with external parsers to co-ordinate the response.
	 * Should be set to null when the parse is finished.
	 */
	// da: can we get rid of this field??  Used in ExternalLazyParser, I've reenabled debug message there.
	Object cause = null;

	/**
	 * @param cause The action that initiated this parse. Used with external parsers to co-ordinate the response.
	 */
	public void setCause(Object cause) {
		this.cause = cause;
	}
	
	/** Exception during parsing */
	public static class ParsingInterruptedException extends Exception {
		public ParsingInterruptedException(String text) {
			super(text);
		}
	}

	/**
	 * Determines whether this parser is slow (and should not be allowed to block) or fast
	 * @return whether or not this parser is slow (true=slow)
	 */
	public abstract boolean isSlow();

// CLEANUP	
//	/** @return whether this parser can give the next command really quickly
//	 *  (and thus doesn't need to be put in a separate thread, eg by SendCommandAction)
//	 */ //added by -AH to fix bug where go-to-line doesn't work
//	public boolean hasNextCommandFast(ProofScriptDocument doc, String type) {
//		if (!isSlow()) {
//			return true;
//		}
//		int so = Math.max(doc.getProcessedOffset() + 1,0);
//		DocElement e = doc.findNext(type,so);
//		return (e!=null);
//	}

//	/** @return whether this parser can give the next command of type COMMAND really quickly
//	 *  (and thus doesn't need to be put in a separate thread, eg by SendCommandAction)
//	 */ //added by -AH to fix bug where go-to-line doesn't work
//	public boolean hasNextCommandFast(ProofScriptDocument doc) {
//		return hasNextCommandFast(doc, PGIPSyntax.COMMAND);
//	}

	/**
	 * Return a list of DocElements
	 * @param text
	 * @return
	 * @throws ScriptingException
	 */
	//public abstract List parse(String text) throws ScriptingException;

	/**
	 * Parse document from offset for at least the given length, looking for
	 * a command for scripting.  
	 * This is the main parser entry point.  This method is synchronized so we don't
	 * attempt more than one parse at once.  We should also ensure that the document
	 * does not change during the parse (TODO).  Ideally, so that marker clearing is
	 * accurate, offset+length should be the end of a span.
	 * @param doc
	 * @param offset
	 */
    public synchronized void parseDoc(ProofScriptDocument doc, int offset, int length)
	throws BadLocationException,ScriptingException,ParsingInterruptedException {
		String text = doc.get(offset, length);
		findObject(PGIPSyntax.ANYITEM, text, doc, offset+length);
	}

// CLEANUP    
//	/**
//	 * Returns the next element of the specified type, for sending out to the prover.
//	 * @param doc
//	 * @return the next command, or null if one cannot be found
//	 * @throws ParsingInterruptedException
//	 */
//	public synchronized DocElement findNextCommand(ProofScriptDocument doc, String type)
//	throws ScriptingException, ParsingInterruptedException {
//		int so = Math.max(doc.getProcessedOffset()+1, 0);
//		if (ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_SKIP_FOLDED)) {
//			so = doc.getUnfoldedOffsetAfter(so);
//		}
//		DocElement e = doc.findNext(type, so);
//		if (e != null) {
//			return e;
//		}
//		return findObject(type, doc, so);
//	}
	
//	/** 
//	 * Find the next object in the script with given type after the currently parsed position
//	 * @param type
//	 * @param doc
//	 * @returns the next command, or null if none found */
//	public synchronized DocElement findNextCommandFast(ProofScriptDocument doc, String type) {
//		int so = Math.max(doc.getProcessedOffset()+1, 0);
//		return doc.findNext(type, so);
//	}
	
//	/** 
//	 * Find the next object in the script with given type after given position
//	 * @param type
//	 * @param doc
//	 * @param startOffset
//	 * @returns the next command, or null if none found */
//	private synchronized DocElement findObject(String type,ProofScriptDocument doc, int startOffset)
//	throws ScriptingException, ParsingInterruptedException {
//		return findObject(type,"",doc,startOffset);
//	}

	/** finds the next object, by following a programmed list of non-blank lines to jump;
	 *  use strategy to add 1 line, then 3, then 12, then the whole shebang
	 * @throws ParsingInterruptedException
	 * @returns the next command, or null if none found */
	private synchronized DocElement findObject(String type, String preLine,
			ProofScriptDocument doc, int startOffset)
	throws ScriptingException, ParsingInterruptedException
	{
		int[] intervals = { 0, 10, 20, 40, 108, 432, -1 };
		boolean useintervals = 
			ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_USE_GATHERING_PARSER);
		return findObject(type, preLine, doc, startOffset,
				useintervals ? intervals : new int[] { -1 } );
	}


	/**
	 * Parse ahead looking for objects
	 * Returns null if it hits the end of file without finding anything.
	 * @param type
	 * @param preLine
	 * @param doc
	 * @param startOffset
	 * @param linesToAdd an array of the number of lines to add at a time
	 * @return the discovered object matching specification
	 * @throws ScriptingException
	 * @throws ParsingInterruptedException
	 */
	// FIXME da: this needs a clean up.  Esp to unify return points.
	private synchronized DocElement findObject(String type, final String preLine,
			ProofScriptDocument doc, int startOffset, int[] linesToAdd)
	throws ScriptingException, ParsingInterruptedException
	{
		IRegion r;
		String line = "";
		int endOffset = -1;
		int lineNo = -1;
		int offset = startOffset;
		int linesAdded = 0;
		if (preLine.length() + line.length() + startOffset < doc.getLength()) {
			try {
				lineNo = doc.getLineOfOffset(offset);
				do {
					r = doc.getLineInformation(lineNo);
					endOffset = r.getOffset() + r.getLength();
					if (endOffset <= offset) {
						// startOffset is a line delimiter; move on to the next line
						lineNo++;
						continue;
					}
					line += doc.get(offset, endOffset-offset);   //TODO should use a stringbuffer instead...
					offset += endOffset-offset;
					if (!StringManipulation.isWhitespace(line)) {
						linesAdded++; // don't send empty lines
					}
					lineNo++;
				} while (linesToAdd[0]==-1 || linesAdded<linesToAdd[0]);
			} catch(BadLocationException ex) {
				//reached end of file
				if (linesAdded==0) {
					return null;
				}
			}
		}
		Element parseResult;
		try {
			parseResult = parseText(preLine+line, doc, startOffset);
			//System.out.println("result of parse from "+startOffset+" (of "+(preLine+line).length()+" chars) is "+parseResult.elements().size()+" elements");
			//if (parseResult.elements().size()>8 && startOffset>60) ...
		} catch (ScriptingException ex2) {
			//doesn't usually throw Unparseable
			if (ex2 instanceof UnparseableException && preLine.length() + line.length() < doc.getLength()) {
				return findObject(type,preLine+line,doc,endOffset,dropFirstFromArray(linesToAdd));
			}
			throw ex2;
		}
		
		// Try to make sure we really have found the end of a command (and not partway through)
		int results = parseResultContentSize(parseResult);
		try {
			if (results == 1 &&
				!doc.get(offset, doc.getLength()-offset).matches("\\s*") &&
				preLine.length() + line.length() + startOffset < doc.getLength()) {
				// This is the dodgy case: we may have only parsed a command prefix, so try for more
				return findObject(type,preLine+line,doc,endOffset,dropFirstFromArray(linesToAdd));
			}
		} catch (BadLocationException ex) {
			throw new ScriptingException("Exception when looking for a command: "+ex);
		}
              				
		
		try {
			int realStart = startOffset - preLine.length();
			String parsedText = preLine + line;
			ProofScriptMarkers.cleanMarkers(doc,realStart,parsedText.length()); // NB: removes all types of marker
			linkParse(parseResult,doc,realStart,parsedText); // NB: this is where parse gets connected to document!
			doc.recalculateFoldingStructure();
		} catch (UnparseableException x) {
			// perhaps the unparseable bit is below our element?
			DocElement e = doc.findNext(type,startOffset - preLine.length());
			if (e != null) {
				return e;
			}
			DocElement foundElt = null;
			//System.out.println(General.makeDateString()+"  expanding parse starting at "+(startOffset-preLine.length())+", size "+(linesToAdd[0]));
			foundElt = findObject(type,preLine+line,doc,endOffset, dropFirstFromArray(linesToAdd));
			//System.out.println(General.makeDateString()+"  expanded parse starting at "+(startOffset-preLine.length())+", size "+(linesToAdd[0])+"; result "+(foundElt==null ? "null" : "FOUND"));
			if (foundElt!=null) {
				return foundElt;
			}
			//if we got null, we are at end of doc, and unparseable
			throw x;
		}
		DocElement e = doc.findNext(type,startOffset - preLine.length());
		if (e != null) {
			return e;
		}
		try {
			//throwing exception is normal when at end of document
			int nextOffset = doc.getLineOffset(1+lineNo);
			return findObject(type,"",doc, nextOffset);
		} catch (BadLocationException ex) {
			throw new ScriptingException("Exception when looking for a command: "+ex);
		}
	}

	/**
	 * @param parseResult
	 * @return the number of elements in the parse result which correspond to 
	 * text commands in the proof script (i.e., ignoring meta info, blocks)
	 */
	private int parseResultContentSize(Element parseResult) {
		int i = 0;
		for (Object result : parseResult.elements()) {
			Element e = (Element) result;
			if (syntax.isScriptContent(e.getName())) {
				i++;
			}
		}
		return i;
	}
	
	/**
	 * Removes the first item from an array, if it contains more than one element.
	 * @param in the array to modify
	 * @return a new array, minus the first element of the given array
	 */
	// FIXME da: library function?  Do we need to make a new array here?
	private int[] dropFirstFromArray(int[] in) {
		int drop = (in.length > 1 ? 1 : 0);
		int[] out = new int[in.length - drop];

		for (int i = 0; i < out.length; i++) {
			out[i] = in[i+drop];
		}
		return out;
	}

	/**
	 * Parse the rest of the document from offset. Wrapper for {@link #parseDoc(ProofScriptDocument, int, int)}
	 * @param doc
	 * @param offset first position from which to start parsing
	 * @throws BadLocationException
	 * @throws ScriptingException
	 * @throws ParsingInterruptedException
	 */
	public void parseDoc(ProofScriptDocument doc, int offset)
	throws BadLocationException,ScriptingException, ParsingInterruptedException {
		if (doc.getLockOffset() > offset || doc.getProcessedOffset() > offset) {
			// This can happen if we allow edit of processed region.
			System.err.println("Warning:  Parser.parseDoc called to parse from "+offset+"; but buffer is locked to "+
					doc.getLockOffset()+ " and processed to " + doc.getProcessedOffset());
		}
		try {
			doc.setParseRegionForEditAtOffset(offset); // throw away old parse information
		} catch (Exception e) {
			System.err.println("Error setting offset in Parser.parseDoc: "+e);
			e.printStackTrace();
		}
		parseDoc(doc, offset, doc.getLength()-offset);
	}

	/**
	 * Parse the text, returning a <parseresult> element, containing the parse results (elements and/or errors).
	 * Use in preference to dumbParseText where doc is known, as it allows the parser to use context.
	 * The default implementation just calls dumbParseText
	 * @param text
	 * @return the parsed element
	 * @throws ScriptingException
	 */
	public Element parseText(String text,ProofScriptDocument doc,int offset) throws ScriptingException, ParsingInterruptedException {
		return dumbParseText(text);
	}
	/**
	 * Parse text, ignoring issues regarding document partitions.
	 * For this reason, you should use parseText in preference where possible.
	 * @param text
	 * @return parse result
	 * @throws ScriptingException
	 */
	public abstract Element dumbParseText(String text) throws ScriptingException, ParsingInterruptedException;

	/**
	 * Exception to signal that some text was unparseable
	 */
	public static class UnparseableException extends ScriptingException {

		UnparseableException(String msg) {
			super(msg);
		}
	}

	/**
	 * Convert the parse result into doc elements
	 * @param parseResult (containing a list of Elements)
	 * @param doc the document to notify of our events
	 * @param startOffset the starting offset
	 * @throws UnparseableException if the parse result was not good.
	 */
	private void linkParse(Element parseResult, ProofScriptDocument doc,
			int startOffset, String rawText)
	throws UnparseableException {
		try {
			linkParseAux(parseResult.elements().listIterator(),doc,startOffset,rawText);
		} catch (UnparseableException ex) {
			try {
				if (doc.getParseOffset() >= startOffset) {
					// we made some progress
					doc.fireParseTreeChangedEvent(startOffset,doc.getParseOffset()-startOffset+1);
				}
			} catch (Exception ex2) { ex2.printStackTrace(); }
			throw ex;
		}
		if (doc.getParseOffset()<startOffset) {
			//we've done nothing, probably at end of document
			return;
		}
		try {
			doc.fireParseTreeChangedEvent(startOffset,doc.getParseOffset()-startOffset+1);
		} catch (Exception ex) {
			//don't worry about this out of debug mode
			//(it seems pretty common when starting without causing any problems), eg
			/*
start-offset:0 offset:22 doc-length:54
org.eclipse.jface.text.Assert$AssertionFailedException: Assertion failed:
        at org.eclipse.jface.text.Assert.isTrue(Assert.java:189)
        at org.eclipse.jface.text.Assert.isTrue(Assert.java:174)
        at org.eclipse.ui.internal.texteditor.quickdiff.compare.equivalence.DocEquivalenceComparator.<init>(DocEquivalenceComparator.java:47)
        at org.eclipse.ui.internal.texteditor.quickdiff.DocumentLineDiffer.handleChanged(DocumentLineDiffer.java:916)
        at org.eclipse.ui.internal.texteditor.quickdiff.DocumentLineDiffer.documentChanged(DocumentLineDiffer.java:762)
        at org.eclipse.jface.text.AbstractDocument.doFireDocumentChanged2(AbstractDocument.java:729)
        at org.eclipse.jface.text.AbstractDocument.doFireDocumentChanged(AbstractDocument.java:692)
        at org.eclipse.jface.text.AbstractDocument.doFireDocumentChanged(AbstractDocument.java:677)
        at ed.inf.proofgeneral.editor.ProofScriptDocument.fireParseTreeChangedEvent(ProofScriptDocument.java:153)
        at ed.inf.proofgeneral.editor.lazyparser.Parser.linkParse(Parser.java:404)
        at ed.inf.proofgeneral.editor.lazyparser.Parser.findObject(Parser.java:235)
        at ed.inf.proofgeneral.editor.lazyparser.Parser.findObject(Parser.java:177)
        at ed.inf.proofgeneral.editor.lazyparser.Parser.findObject(Parser.java:166)
        at ed.inf.proofgeneral.editor.lazyparser.Parser.findNextCommand(Parser.java:127)
        at ed.inf.proofgeneral.editor.actions.SendCommandAction$BgParse.run(SendCommandAction.java:182)
        at ed.inf.heneveld.utils.process.ThreadPool$PooledThread.run(ThreadPool.java:172)
			 */
			if (ProofGeneralPlugin.debug(this)) {
				System.err.print("Exception firing ParseTreeChangedEvent ");
				System.err.println( "start-offset:"+ startOffset + " parse offset:"+doc.getParseOffset() +
						" length:" + (doc.getParseOffset()-startOffset+1) +  " doc-length:"+doc.getLength() );
				// ex.printStackTrace()
				/*
        		try {
        			doc.fireParseTreeChangedEvent(Math.max(0,startOffset),
        					Math.min(doc.getLength()-Math.max(0,startOffset),
        							(doc.getParseOffset()>startOffset ? doc.getParseOffset()-startOffset : 0)));
        		} catch (Exception x) {}
				 */
			}
		}
	}

	/**
	 * Creates problem markers for parsing errors.
	 * Recursive method that does the work for linkParse.
	 */
	private void linkParseAux(Iterator nodeIterator, ProofScriptDocument doc,
			int startOffset, String rawText) throws UnparseableException {
		int offset = startOffset;
		String eText;
		DocElement parent = doc.getOpenElement();
		UnparseableException parseError = null;
		Fatality parseErrorFatality = null;
		
		//cache fatal parsing errors until the end (since they usually only occur at the end, and we can take useful information before it)
		for (; nodeIterator.hasNext();) {
			Node node = (Node) nodeIterator.next();
			String name = node.getName();

			// handle parsing errors: these are different to normal error messages because they are embedded
			// in the parseresult message and by convention precede an <unparseable> sequence.
			// So the main event loop does not add markers for parse errors.  That's just
			// as well because it allows us to calculate the regions affected here, and the messages
			// (from Isabelle, as of Jan 07) do not include location information themselves.
			// We could consider flattening the structure when Isabelle gets better at reporting
			// locations, but this will need changes in the Broker too. - da.

			if (syntax.subType(name,PGIPSyntax.UNPARSEABLE)) {
				if (parseError == null) { // we've not seen an error response earlier, but we should have done
					parseError = new UnparseableException("Parse error, unparsable text: " + node.getStringValue());
					parseErrorFatality = Fatality.fromString("fatal");
				} else if (parseErrorFatality == null) { // shouldn't happen
					parseErrorFatality = Fatality.fromString("fatal");
				}
				int errorOffset = doc.skipSpacesForward(offset);
				// FIXME: use location if set
				Position errorpos = new Position(errorOffset,Math.min(doc.getLength()-errorOffset,node.getText().length()));
				String msg = parseError.getMessage();
				ProofScriptMarkers.addProblemMarker(doc,errorpos,
						-1, "Parse error: "+msg.replaceFirst("\n.*","..."),	msg,parseErrorFatality.markerSeverity());
				parseError.errorOffset = errorOffset;
				parseError.errorDoc = doc;
				throw parseError;  // Unparseable should be last node; we ignore rest
			}

			else if (syntax.subType(name, PGIPSyntax.ERRORRESPONSE)) {
				parseErrorFatality = Fatality.fromString(((Element) node).attributeValue("fatality"));
				if (!parseErrorFatality.commandFailed()) { 
					try {
						String msg = parseError != null ? parseError.getMessage() : ((Element) node).getStringValue();
						int errorOffset = doc.skipSpacesForward(offset);
						ProofScriptMarkers.addProblemMarker(doc,
								new Position(errorOffset,node.getText().length()), // FIXME: use location if set
								-1,
								"Parse problem: "+msg.replaceFirst("\n.*","..."),
								msg,
								parseErrorFatality.markerSeverity());
						continue;
					} catch (Exception x) {
						x.printStackTrace();
						throw new UnparseableException(node.getStringValue());
					}
					// A fatal error usually terminates the parse with the rest being unparseable.
				} else if (parseErrorFatality == Fatality.FATAL) {
					if (parseError==null) {
						// note we assume the result comes as:  <errorresponse .../> .... <unparseable .../> ....
						// and we keep loading nodes after errors, except for the 'unparseable' bit:
						// this allows us to set any valid parse info returned
						parseError = new UnparseableException(node.getStringValue());
					}
				}
			}
			
			else {
				Position p=null;
				if (node instanceof Element) {
					//  internal commands are wrapped in XML in the document.
					// FIXME da: I think we should remove this. Should check
					// first that this isn't used in startup.
					// FIXME da: it seems to loop sometimes anyway, bloomin nuisance.
					p = findMatch((Element)node,rawText);
					if (p!=null) {
						eText = rawText.substring(p.getOffset(),p.getOffset()+p.getLength());
						System.err.println("DEBUG NOTE: Found a match with XML in document!");
					} else {
						eText = node.getText();
					}
				} else {
					eText = node.getText();
				}

				// If we're a metainfo or we have an empty element in the parse result, let's add it
				// to the document immediately.  (e.g. <openblock/>,<closeblock/>)
				if (syntax.subType(name,PGIPSyntax.METAINFO) || eText.equals("")) {
					linkElement((Element) node,doc,offset,"",false,0);
					continue;
				}

				// TODO da: do not trip here, but rather match exactly.
				// We should skip spaces when we add markers instead.
				eText = StringManipulation.trim(eText); // some protection against \r\n = \n wierdness. Also stops markers being positioned on earlier (empty) lines.
				//TODO we should take the position as the next in the doc after fParseOffset, and this below should just be a check; but this is working
				int gap = rawText.indexOf(eText);
				// da: if the text was found, check that only whitespace was given
				// first.
				// debug:
				//if (gap > -1) {
				//	System.out.println("Found match for element "+node.getName()+" with gap " + gap);
				//	if (!rawText.substring(0, gap).matches("[ \\n]+")) { // FIXME regexp
				//		// Match was later in text, don't take it.
				//		gap = -1;
				//	}
				//}
				// attempt to correct for whitespace and linebreak issues.
				Position truePosn = null; // lots of potential for out-by-1-or-2 errors
				if (gap==-1) {
//					FIXME da: very buggy, only matches part of element text, allows spaces anywhere!
					truePosn = findMatch(eText,rawText);
					if (truePosn != null) {
						gap = truePosn.getOffset();
					} else {
						truePosn = findMatchModSymbols(eText,rawText,doc);
						if (truePosn != null) {
							gap = truePosn.getOffset();
						}
					}
				}

				if (gap!=-1) {
					offset += gap;
					rawText = rawText.substring(gap);
				} else {
					//if (ProofGeneralPlugin.debug())
					System.err.println("Possible Error: linkParse found a gap of -1 in element "+eText);
					// leave offset alone and hope for the best
				}
				if (node instanceof CharacterData) {
					parent.addText(node.getText());
				} else {
					// ignore empty elements
					if (StringManipulation.isWhitespace(eText)
							|| PGIPSyntax.WHITESPACE.equals(name)) {
						continue;  //if (!node.hasContent()) continue;
					}

					if (p!=null) {
						DocElement docE = linkElement((Element) node,doc,offset,rawText,false,p.getLength());
						docE.setRawText(eText); // avoid setting this where possible, because it will probably cause bugs
					} else {
						int length = truePosn==null? -1 : truePosn.getLength();
						DocElement docE = linkElement((Element) node,doc,offset,rawText,false,length);
						//eText = StringManipulation.trim(docE.getText());
						eText = docE.getText(); // da: see if removing trim fixes length error
					}
				}
				offset += truePosn!=null? truePosn.getLength() : eText.length();
				// da: we can get here without success and then try to take a substring on a shorter
				// string, throwing IndexOutOfBoundsException.  I've patched but the
				// parsing stuff here really needs a good makeover!
				if (truePosn != null) {
					assert truePosn.getLength() <= rawText.length() : "Parser identified/lost too much text";
					rawText = rawText.substring(truePosn.getLength()); // good case
				} else {
					if (eText.length()<rawText.length()) {
						rawText = rawText.substring(eText.length());
					} else {
						rawText = "";
					}
				}
			}
		}
		if (parseError!=null) {
			//we come here if there were no elements
			throw parseError;
		}
	}

	/**
	 * Create a DocElement (with typed position) from an element
	 * and add it to the current open container of the document.
	 *
	 * Attaches comments to elements, where possible, for use as tooltips/hover help.
	 *
	 * This implements part of the spec for how proof scripts are set out
	 *
	 * @param e the element to convert
	 * @param doc			the document to notify
	 * @param offset		the offset of the element
	 * @param rawText		the text of the element
	 * @param fireEvents	should be true, unless the calling function intends to fire
	 * 						a bulk event (eg. it is doing a big parse, and doesn't want to
	 * 						slow the system down with lots of little events)
	 * @param length		Set to -1 if the length is unknown
	 * @return the converted Element
	 */
	// da: Note here that we impose some structure based on the basic PGIP markup.
	// We're moving towards instead using openblock/closeblock elements to contain
	// structure hints in markup from the prover, and only use those.  They can
	// be mapped directly to container elements here.  This would allow a smooth
	// and generic handling of sections in Isabelle, for example.
	// At the moment there is a mix of two approaches.
	private DocElement linkElement(Element e, ProofScriptDocument doc, int offset,
			String rawText,boolean fireEvents, int length)
	throws UnparseableException {
		DocElement de;
		String name = e.getName();

		ContainerElement addPt = doc.getOpenElement();

		// Add container for special kinds of comment (Isabelle section headings)
		if (syntax.subType(name, PGIPSyntax.DOCCOMMENT)) { 
			// close the previous open element when we get here, if it is a section
			// This approximate and flattens sect/sub/subsub; we should use openblock/closeblock instead
			if (e.getStringValue().startsWith("subsection") &&
					addPt.getName() == PGIPSyntax.SECTION_CONTAINER) {
				doc.closeOpenElement();
				addPt = doc.getOpenElement();
			}
		}

		// Close theory closes off open sections first.
		// FIXME: Isabelle specific 
		if (syntax.subType(name,PGIPSyntax.CLOSETHEORY)) {
			while (addPt.getName() == PGIPSyntax.SECTION_CONTAINER) {
				doc.closeOpenElement();
				addPt = doc.getOpenElement();
			}
		}

		DocElement previous = addPt.getLastElement();

		TypedPosition posn = new TypedPosition(offset,0,name);
		if (e.elements().size()!=0) {
			// FIXME/TODO: this is in case we have nested elements already in the parse
			// result, which will happen with new PGML marked up input.  Probably we
			// *don't* want a container in this case!
			System.err.println("Parser found nested parse result; not handled properly yet: \n"+e.asXML());
			de = new ContainerElement(name,doc,null);
		} else {
			String containerType = null;
			if (syntax.subType(name,PGIPSyntax.OPENTHEORY)) {
				containerType = PGIPSyntax.THEORY_CONTAINER;
			} else if (syntax.subType(name,PGIPSyntax.OPENGOAL)) {
					containerType = PGIPSyntax.PROOF_CONTAINER;
			} else if ((syntax.subType(name, PGIPSyntax.DOCCOMMENT)) &&
					(e.getStringValue().startsWith("subsection"))) {
				containerType = PGIPSyntax.SECTION_CONTAINER;
			}
			if (containerType!=null) {
				ContainerElement ce =  new ContainerElement(containerType, doc,name);
				addPt.add(ce);
				doc.setOpenElement(ce);
				ce.setPosition(new TypedPosition(offset, 0, containerType));
				addPt = doc.getOpenElement();
			}
			de = new DocElement(name,doc);
		}

		if (de instanceof ContainerElement) {
			doc.setOpenElement((ContainerElement)de);
			linkParseAux(e.nodeIterator(), doc, offset, rawText); // recursive call
			doc.closeOpenElement();
		} else {
			de.setText(e.getText());
		}
		de.setPosition(posn);
		de.setAttributes(e.attributes());
		addPt.add(de);

		// give this element some length
		if (length==-1) {
			posn.setLength(de.getText().length());
		} else {
			posn.setLength(length);
		}
		int cEnd = offset + posn.getLength() - 1;
		ContainerElement ce = addPt;
		while (ce!=null) {
			//set container length on all nodes up the hierarchy
			// FIXME: this gives negative lengths often.  WHY?
			ce.getPosition().setLength(Math.max(0,cEnd - ce.getPosition().getOffset()+1));
			ce = (ContainerElement)ce.getParent();
		}

		if (syntax.subType(name,PGIPSyntax.CLOSEGOAL)) {
			// close the open element, once we have been added
			// FIXME: this assumes the surrounding open is the closegoal:
			// we might instead locate the surrounding thing with the right
			// type (i.e., opengoal).
			if (!syntax.subType(addPt.getType(),PGIPSyntax.PROOF_CONTAINER)) {
				throw new UnparseableException("A close theorem command appears without a corresponding open theorem");
			}
			if (addPt != doc.getRootElement()) {
				doc.closeOpenElement();
			} else { // tried to close the root element
				System.err.println("syntax error in script - too many closing elements"); // TODO signal the user
			}
		}

		// if the previous element was a comment, and this one is not a comment
		// then create a tooltip for our new element.
		if (previous !=null && syntax.subType(previous.getType(),PGIPSyntax.COMMENT)
				//&& !syntax.subType(previous.getType(),PGIPSyntax.FORMALCOMMENT)
				&& !syntax.subType(name,PGIPSyntax.COMMENT)) {
			de.setTooltip(previous.getText());
		}

		de.createMarker(doc.getSyntax()); // must be called after de.setTooltip()

		try {
			if (fireEvents) {
				doc.fireParseTreeChangedEvent(offset,cEnd-offset);
			}
			// Since we don't add elements for whitespace, move parse offset to end of whitespace after command
			int pEnd = doc.skipSpacesForward(cEnd+1);
			doc.setParseOffset(pEnd-1);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		if (ProofGeneralPlugin.debug(this)) { //DEBUG-TEST-CODE
			try {
				String deText = de.getText();
				String docText = doc.get(de.getPosition().getOffset(),de.getPosition().getLength());
				assert findMatch(deText,docText) != null : "Mismatch in parser: "+deText+" != "+docText;
			} catch (Exception x) {x.printStackTrace();}
		} //END-DEBUG
		return de;
	}

	/**
	 * Try to match the xml of an element against the beginning of the "raw" text from the editor.
	 * Ignores white-space and quote marks, since these can be changed by xml parsers.
	 * ASSUMPTION: This will <b>not</b> spot that &lt;dummy&gt;&lt;/dummy&gt; = &lt;dummy/&gt;;
	 *  I am assuming this kind of syntax switch won't occur.
	 * @return position indicating start and length relative to the raw text supplied, or null if the match failed.
	 */
	public static Position findMatch(Element e,String rawText) {
		// FIXME da: crummy code again.  Shouldn't use exceptions as matter of course.
		try {
			String name = e.getName();
			String asXML = e.asXML();
			int start = rawText.indexOf(name)-1;
			if (start<0) {
				return null;
			}
			int j=start;
			for(int i=0; i<asXML.length(); i++) {
				char xc = asXML.charAt(i);
				if (IGNORABLE_SPACE_CHARS.indexOf(xc) != -1) {
					continue;
				}
				while(true) {
					char rc = rawText.charAt(j);
					if (IGNORABLE_SPACE_CHARS.indexOf(rc)!= -1) {
						j++; continue;
					}
					if (xc==rc) {
						j++;
						break;
					}
					return null;
				}
			}
			return new Position(start,j-start);
		} catch (Exception ex) {
			return null;
		}
	}
	static final String IGNORABLE_SPACE_CHARS =" \r\n\t";  //don't think we should be ignoring ' and "  -AH


	/**
	 * Try to match text against the beginning of the raw text, ignoring whitespace and \r\n / \n differences.
	 * @return position indicating start and length relative to the raw text supplied, or null if the match failed.
	 */
	// da: FIXME: pretty sloppy stuff.  Use of exceptions isn't great,
	// (possibly odd return values when text prefix matches at end of string?)
	// Also, this matches "h e l l o" against "hello" which we don't want.
	public static Position findMatch(String eText,String rawText) {
		assert !(eText.equals("") || rawText.equals("")): "Empty input";
		try {
			int start=-1;
			int j=0;
			for(int i=0; i<eText.length(); i++) {
				char ec = eText.charAt(i);
				if (IGNORABLE_SPACE_CHARS.indexOf(ec) != -1) {
					continue;
				}
				while(true) {
					char rc = rawText.charAt(j);
					if (IGNORABLE_SPACE_CHARS.indexOf(rc)!= -1) {
						j++; continue;
					}
					if (ec==rc) {
						if (start==-1) {
							start = j;
						}
						j++;
						break;
					}
					return null;
				}
			}
			if (start==-1) {
				return null;
			}
			return new Position(start,j-start);
		} catch (Exception ex) {
			return null;
		}
	}

	private static Position findMatchModSymbols(String eText, String rawText, ProofScriptDocument doc) {
		try {
			int ei=0, ri=0;
			while (ei<eText.length() && Character.isWhitespace(eText.charAt(ei))) {
				ei++;
			}
			while (ri<rawText.length() && Character.isWhitespace(rawText.charAt(ri))) {
				ri++;
			}
			int start=ri;
			while (ei<eText.length() && ri<rawText.length()) {
				if (eText.charAt(ei)==rawText.charAt(ri)) {
					ei++;
					ri++;
				} else {
					int[] mis = doc.getProver().getSymbols().checkStringsStartSameSymbol(eText, ei, rawText, ri);
					if (mis!=null) {
						ei += mis[0];
						ri += mis[1];
					} else {
						System.err.println("Parser unable to reconcile "+eText.substring(ei)+"\nwith "+rawText.substring(ri));
						return null;
					}
				}
				while (ei<eText.length() && Character.isWhitespace(eText.charAt(ei))) {
					ei++;
				}
				while (ri<rawText.length() && Character.isWhitespace(rawText.charAt(ri))) {
					ri++;
				}
			}
			if (ei<eText.length()) {
				System.err.println("Parser unable to reconcile parse "+eText+"\nwith source "+rawText+"\nseems parse has extra characters!");
				return null;
			}
			return new Position(start, ri-start);
		} catch (Exception ex) {
			System.err.println("Parser unable to reconcile parse "+eText+"\nwith source "+rawText+"\n"+ex);
			return null;
		}
	}
	
	/** Free up resources. */
	public void dispose() {
		// Do nothing.  Subclasses may add behaviour.
	}
}
