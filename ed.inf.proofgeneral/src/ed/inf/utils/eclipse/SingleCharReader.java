/*
 *  $RCSfile: SingleCharReader.java,v $
 *
 *  Created on 30 Apr 2005
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.eclipse;

import java.io.IOException;
import java.io.Reader;

/**
 * @author Alex Heneveld
 *
 */
abstract class SingleCharReader extends Reader {
	
	/**
	 * @see Reader#read()
	 */
	@Override
    public abstract int read() throws IOException;

	/**
	 * @see Reader#read(char[],int,int)
	 */
	@Override
    public int read(char cbuf[], int off, int len) throws IOException {
		int end= off + len;
		for (int i= off; i < end; i++) {
			int ch= read();
			if (ch == -1) {
				return i == off ? -1 : i - off;
			}
			cbuf[i]= (char)ch;
		}
		return len;
	}		
	
	/**
	 * @see Reader#ready()
	 */		
    @SuppressWarnings("unused")
    @Override
    public boolean ready() throws IOException {
		return true;
	}
	
	/**
	 * Gets the content as a String
	 */
	public String getString() throws IOException {
		StringBuffer buf= new StringBuffer();
		int ch;
		while ((ch= read()) != -1) {
			buf.append((char)ch);
		}
		return buf.toString();
	}
}
