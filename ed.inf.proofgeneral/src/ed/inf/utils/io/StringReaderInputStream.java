/*
 *  $RCSfile: StringReaderInputStream.java,v $
 *	Created on 01 Nov 2006 by Graham Dutton
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.io;

import java.io.*;

/**
 * Creates a stream from a StringReader, compatible with InputStream rather
 * than Reader.
 */
public class StringReaderInputStream extends InputStream {

	protected final StringReader r;

	/**
	 * Create a new Stream-compatible StringReader wrapper.
	 * Creates a StringReader to deal with the input string.
	 * @param s the String from which to obtain data.
	 * @throws IOException if a null reader is passed.
	 */
	public StringReaderInputStream(String s) throws IOException {
		if (s == null) {
			throw new IOException("null String input!");
		}
		r = new StringReader(s);
	}

	/**
	 * Create a new Stream-compatible StringReader wrapper.
	 * @param r the StringReader from which to obtain data.
	 * @throws IOException if a null reader is passed.
	 */
	public StringReaderInputStream(StringReader r) throws IOException {
		if (r == null) {
			throw new IOException("null String reader input");
		}
		this.r = r;
	}

	/**
	 * @see java.io.InputStream#read()
	 */ @Override
	public int read() throws IOException {
		return r.read();
	}

}
