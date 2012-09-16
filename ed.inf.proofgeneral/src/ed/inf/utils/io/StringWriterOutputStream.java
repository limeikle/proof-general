/*
 *  $RCSfile: StringWriterOutputStream.java,v $
 *
 *  Created on 01 Nov 2006 by Graham Dutton
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.io;

import java.io.*;

/**
 * Wraps a StringBuilder for use in an StringWriter chained write(*).
 */
public class StringWriterOutputStream extends OutputStream {

	protected final StringWriter w;

	/**
	 * Constructs a new output stream with a default, empty StringWriter.
	 */
	public StringWriterOutputStream() {
		 w = new StringWriter();
	}
	
	/**
	 * Constructs a new output stream with a specified initial size
	 * @param initialsize the initial size of the embedded StringWriter 
	 */
	public StringWriterOutputStream(int initialsize) {
		w = new StringWriter(initialsize);
	}

	/**
	 * Writes to the StringWriter.
	 * @throws IOException if an IOException occurs in StringWriter.
	 */
	public void write(char[] c) throws IOException {
		w.write(c);
	}

	/**
	 * Writes to the StringWriter.
	 */
	public void write(String s) {
		w.write(s);
	}
	
	/**
	 * Writes to the StringWriter.
	 */	@Override
	public void write(int b) {
		w.write(b);
	}

	/**
	 * Gets the read string.
	 */
	public String toString() {
		return w.toString();
	}
	
}
