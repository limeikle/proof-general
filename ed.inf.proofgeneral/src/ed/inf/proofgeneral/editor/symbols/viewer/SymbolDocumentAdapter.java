/*
 *  $RCSfile: SymbolDocumentAdapter.java,v $
 *
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.symbols.viewer;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentAdapter;
import org.eclipse.jface.text.IDocumentAdapterExtension;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.IRepairableDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TextChangeListener;
import org.eclipse.swt.custom.TextChangedEvent;
import org.eclipse.swt.custom.TextChangingEvent;

import ed.inf.proofgeneral.document.ISymbolisedDocument;
import ed.inf.proofgeneral.symbols.SymbolTable;


/**
 * Default implementation of {@link org.eclipse.jface.text.IDocumentAdapter}.
 */
class SymbolDocumentAdapter implements IDocumentAdapter, IDocumentListener, IDocumentAdapterExtension {

	/** The adapted document. */
	private final ISymbolisedDocument fDocument = null;
	/** The document clone for the non-forwarding case. */
	private IDocument fDocumentClone;
	/** The original content */
	private String fOriginalContent;
	/** The original line delimiters */
	private String[] fOriginalLineDelimiters;
	/** The registered text change listeners */
	private final List<TextChangeListener> fTextChangeListeners= new ArrayList<TextChangeListener>(1);
	/**
	 * The remembered document event
	 * @since 2.0
	 */
	private DocumentEvent fEvent = null;
	/** The line delimiter */
	private String fLineDelimiter= null;
	/**
	 * Indicates whether this adapter is forwarding document changes
	 * @since 2.0
	 */
	private boolean fIsForwarding= true;
	/**
	 * Length of document at receipt of <code>documentAboutToBeChanged</code>
	 * @since 2.1
	 */
	private int fRememberedLengthOfDocument;
	/**
	 * Length of first document line at receipt of <code>documentAboutToBeChanged</code>
	 * @since 2.1
	 */
	private int fRememberedLengthOfFirstLine;
	/**
	 * The data of the event at receipt of <code>documentAboutToBeChanged</code>
	 * @since 2.1
	 */
	private final  DocumentEvent fOriginalEvent= new DocumentEvent();

	private SymbolTable symbols;
	
	/**
	 * Creates a new document adapter which is initially not connected to
	 * any document.
	 */
	public SymbolDocumentAdapter() {
	}

	/**
	 * Sets the given document as the document to be adapted.
	 *
	 * @param document the document to be adapted or <code>null</code> if there is no document
	 */
	public void setDocument(IDocument document) {

		if (fDocument != null) {
			fDocument.removePrenotifiedDocumentListener(this);
		}

		fLineDelimiter= null;

		// FIXME: this might not be set yet?  Then we never get symbols here!
		symbols = symbolsFrom(document); 

		if (!fIsForwarding) {
			fDocumentClone= null;
			if (fDocument != null) {
				fOriginalContent= fDocument.get();
				fOriginalLineDelimiters= fDocument.getLegalLineDelimiters();
			} else {
				fOriginalContent= null;
				fOriginalLineDelimiters= null;
			}
		}

		if (fDocument != null) {
			fDocument.addPrenotifiedDocumentListener(this);
		}
	}

	// ISABELLEWS: FIXME: why is the symbol info duplicated here and in S*D?
	/** Temporary empty table during initialisation */
	private static SymbolTable emptySymbols = new SymbolTable();

	private SymbolTable symbolsFrom(IDocument document) {
		if (document instanceof ISymbolisedDocument) {
			return ((ISymbolisedDocument)document).getSymbols();
		}
		return emptySymbols;
	}
	
	/*
	 * @see StyledTextContent#addTextChangeListener(TextChangeListener)
	 */
	public void addTextChangeListener(TextChangeListener listener) {
		Assert.isNotNull(listener);
		if (! fTextChangeListeners.contains(listener)) {
			fTextChangeListeners.add(listener);
		}
	}

	/*
	 * @see StyledTextContent#removeTextChangeListener(TextChangeListener)
	 */
	public void removeTextChangeListener(TextChangeListener listener) {
		Assert.isNotNull(listener);
		fTextChangeListeners.remove(listener);
	}

	/**
	 * Tries to repair the line information.
	 *
	 * @param document the document
	 * @see IRepairableDocument#repairLineInformation()
	 * @since 3.0
	 */
	private void repairLineInformation(IDocument document) {
		if (document instanceof IRepairableDocument) {
			IRepairableDocument repairable= (IRepairableDocument) document;
			repairable.repairLineInformation();
		}
	}

	/**
	 * Returns the line for the given line number.
	 *
	 * @param document the document
	 * @param line the line number
	 * @return the content of the line of the given number in the given document
	 * @throws BadLocationException if the line number is invalid for the adapted document
	 * @since 3.0
	 */
	private String doGetLine(IDocument document, int line) throws BadLocationException {
		IRegion r= document.getLineInformation(line);
		return document.get(r.getOffset(), r.getLength());
	}

	private IDocument getDocumentForRead() {
		if (!fIsForwarding) {
			if (fDocumentClone == null) {
				String content= fOriginalContent == null ? "" : fOriginalContent; //$NON-NLS-1$
				String[] delims= fOriginalLineDelimiters == null ? DefaultLineTracker.DELIMITERS : fOriginalLineDelimiters;
				fDocumentClone= new SymbolDocumentClone(content, delims);
			}
			return fDocumentClone;
		}

		return fDocument;
	}

	/*
	 * @see StyledTextContent#getLine(int)
	 */
	public String getLine(int line) {

		IDocument document= getDocumentForRead();
		try {
			return doGetLine(document, line);
		} catch (BadLocationException x) {
			repairLineInformation(document);
			try {
				return doGetLine(document, line);
			} catch (BadLocationException x2) {
			}
		}

		SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		return null;
	}

	/*
	 * @see StyledTextContent#getLineAtOffset(int)
	 */
	public int getLineAtOffset(int offset) {
		IDocument document= getDocumentForRead();
		try {
			return document.getLineOfOffset(offset);
		} catch (BadLocationException x) {
			repairLineInformation(document);
			try {
				return document.getLineOfOffset(offset);
			} catch (BadLocationException x2) {
			}
		}

		SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		return -1;
	}

	/*
	 * @see StyledTextContent#getLineCount()
	 */
	public int getLineCount() {
		return getDocumentForRead().getNumberOfLines();
	}

	/*
	 * @see StyledTextContent#getOffsetAtLine(int)
	 */
	public int getOffsetAtLine(int line) {
		IDocument document= getDocumentForRead();
		try {
			return document.getLineOffset(line);
		} catch (BadLocationException x) {
			repairLineInformation(document);
			try {
				return document.getLineOffset(line);
			} catch (BadLocationException x2) {
			}
		}

		SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		return -1;
	}

	/*
	 * @see StyledTextContent#getTextRange(int, int)
	 */
	public String getTextRange(int offset, int length) {
		try {
			return getDocumentForRead().get(offset, length);
		} catch (BadLocationException x) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
			return null;
		}
	}

	/*
	 * @see StyledTextContent#replaceTextRange(int, int, String)
	 */
	public void replaceTextRange(int pos, int length, String text) {
		try {
			String symbolised = symbols.useUnicodeForDocument(text);
			fDocument.replace(pos, length, symbolised);
		} catch (BadLocationException x) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
	}

	/*
	 * @see StyledTextContent#setText(String)
	 */
	public void setText(String text) {
		if (symbols != null) {
			String symbolised = symbols.useUnicodeForDocument(text);
			fDocument.set(symbolised);
		} else {
			fDocument.set(text);
		}
	}

	/*
	 * @see StyledTextContent#getCharCount()
	 */
	public int getCharCount() {
		return getDocumentForRead().getLength();
	}

	/*
	 * @see StyledTextContent#getLineDelimiter()
	 */
	public String getLineDelimiter() {
		if (fLineDelimiter == null) {
			fLineDelimiter= TextUtilities.getDefaultLineDelimiter(fDocument);
		}
		return fLineDelimiter;
	}

	/*
	 * @see IDocumentListener#documentChanged(DocumentEvent)
	 */
	public void documentChanged(DocumentEvent event) {
		// check whether the given event is the one which was remembered
		if (fEvent == null || event != fEvent) {
			return;
		}

		if (isPatchedEvent(event) || (event.getOffset() == 0 && event.getLength() == fRememberedLengthOfDocument)) {
			fLineDelimiter= null;
			fireTextSet();
		} else {
			if (event.getOffset() < fRememberedLengthOfFirstLine) {
				fLineDelimiter= null;
			}
			fireTextChanged();
		}
	}

	/*
	 * @see IDocumentListener#documentAboutToBeChanged(DocumentEvent)
	 */
	public void documentAboutToBeChanged(DocumentEvent event) {

		fRememberedLengthOfDocument= fDocument.getLength();
		try {
			fRememberedLengthOfFirstLine= fDocument.getLineLength(0);
		} catch (BadLocationException e) {
			fRememberedLengthOfFirstLine= -1;
		}

		fEvent= event;
		rememberEventData(fEvent);
		fireTextChanging();
	}

	/**
	 * Checks whether this event has been changed between <code>documentAboutToBeChanged</code> and
	 * <code>documentChanged</code>.
	 *
	 * @param event the event to be checked
	 * @return <code>true</code> if the event has been changed, <code>false</code> otherwise
	 */
	private boolean isPatchedEvent(DocumentEvent event) {
		return fOriginalEvent.fOffset != event.fOffset
			|| fOriginalEvent.fLength != event.fLength
			|| fOriginalEvent.fText != event.fText; // ES, OK
	}

	/**
	 * Makes a copy of the given event and remembers it.
	 *
	 * @param event the event to be copied
	 */
	private void rememberEventData(DocumentEvent event) {
		fOriginalEvent.fOffset= event.fOffset;
		fOriginalEvent.fLength= event.fLength;
		fOriginalEvent.fText= event.fText;
	}

	/**
	 * Sends a text changed event to all registered listeners.
	 */
	private void fireTextChanged() {

		if (!fIsForwarding) {
			return;
		}

		TextChangedEvent event= new TextChangedEvent(this);

		if (fTextChangeListeners != null) {
			for (TextChangeListener t : fTextChangeListeners) {
				t.textChanged(event);
			}
		}
	}

	/**
	 * Sends a text set event to all registered listeners.
	 */
	private void fireTextSet() {

		if (!fIsForwarding) {
			return;
		}

		TextChangedEvent event = new TextChangedEvent(this);

		if (fTextChangeListeners != null) {
			for (TextChangeListener t : fTextChangeListeners) {
				t.textSet(event);
			}
		}
	}

	/**
	 * Sends the text changing event to all registered listeners.
	 */
	private void fireTextChanging() {

		if (!fIsForwarding) {
			return;
		}

		try {
		    IDocument document= fEvent.getDocument();
		    if (document == null) {
				return;
			}

			TextChangingEvent event= new TextChangingEvent(this);
			event.start= fEvent.fOffset;
			event.replaceCharCount= fEvent.fLength;
			event.replaceLineCount= document.getNumberOfLines(fEvent.fOffset, fEvent.fLength) - 1;
			event.newText= fEvent.fText;
			event.newCharCount= (fEvent.fText == null ? 0 : fEvent.fText.length());
			event.newLineCount= (fEvent.fText == null ? 0 : document.computeNumberOfLines(fEvent.fText));

			if (fTextChangeListeners != null) {
				for (TextChangeListener t : fTextChangeListeners) {
					t.textChanging(event);
				}
			}
		} catch (BadLocationException e) {
		}
	}

	/*
	 * @see IDocumentAdapterExtension#resumeForwardingDocumentChanges()
	 * @since 2.0
	 */
	public void resumeForwardingDocumentChanges() {
		fIsForwarding= true;
		fDocumentClone= null;
		fOriginalContent= null;
		fOriginalLineDelimiters= null;
		fireTextSet();
	}

	/*
	 * @see IDocumentAdapterExtension#stopForwardingDocumentChanges()
	 * @since 2.0
	 */
	public void stopForwardingDocumentChanges() {
		fDocumentClone= null;
		fOriginalContent= fDocument.get();
		fOriginalLineDelimiters= fDocument.getLegalLineDelimiters();
		fIsForwarding= false;
	}
}
