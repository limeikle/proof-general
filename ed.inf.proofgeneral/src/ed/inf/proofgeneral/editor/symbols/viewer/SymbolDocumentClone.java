/*
 *  $RCSfile: SymbolDocumentClone.java,v $
 *
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.ConfigurableLineTracker;
import org.eclipse.jface.text.ITextStore;

/**
 * An {@link org.eclipse.jface.text.IDocument} that is a read-only clone of another document.
 *
 * @since 3.0
 */
class SymbolDocumentClone extends AbstractDocument {

	private static class StringTextStore implements ITextStore {

		private final String fContent;

		/**
		 * Creates a new string text store with the given content.
		 *
		 * @param content the content
		 */
		public StringTextStore(String content) {
			assert content != null : "Illegal argument!";
			fContent= content;
		}

		/*
		 * @see org.eclipse.jface.text.ITextStore#get(int)
		 */
		public char get(int offset) {
			return fContent.charAt(offset);
		}

		/*
		 * @see org.eclipse.jface.text.ITextStore#get(int, int)
		 */
		public String get(int offset, int length) {
			return fContent.substring(offset, offset + length);
		}

		/*
		 * @see org.eclipse.jface.text.ITextStore#getLength()
		 */
		public int getLength() {
			return fContent.length();
		}

		/*
		 * @see org.eclipse.jface.text.ITextStore#replace(int, int, java.lang.String)
		 */
		public void replace(int offset, int length, String text) {
		}

		/*
		 * @see org.eclipse.jface.text.ITextStore#set(java.lang.String)
		 */
		public void set(String text) {
		}

	}

	/**
	 * Creates a new document clone with the given content.
	 *
	 * @param content the content
	 * @param lineDelimiters the line delimiters
	 */
	public SymbolDocumentClone(String content, String[] lineDelimiters) {
		super();
		setTextStore(new StringTextStore(content));
		ConfigurableLineTracker tracker= new ConfigurableLineTracker(lineDelimiters);
		setLineTracker(tracker);
		getTracker().set(content);
		completeInitialization();
	}
}
