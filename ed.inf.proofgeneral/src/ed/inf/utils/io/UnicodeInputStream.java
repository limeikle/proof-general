package ed.inf.utils.io;

/*
 *  This file is part of Proof General Eclipse
 *
 * From http://koti.mbnet.fi/akini/java/unicodereader/
 * Original pseudocode   : Thomas Weidenfeller
 * Implementation tweaked: Aki Nieminen, Graham Dutton
 * 
 * Copyright original authors.
 */

/**
 * version: 1.1a / 2007-08-15
 * - cleaned implementation for PGEclipse
 * 
 * version: 1.1 / 2007-01-25
 * - changed BOM recognition ordering (longer BOMs first)
 * 
 * http://www.unicode.org/unicode/faq/utf_bom.html
 * BOMs in byte length ordering:
 *  00 00 FE FF    = UTF-32, big-endian
 *  FF FE 00 00    = UTF-32, little-endian
 *  EF BB BF       = UTF-8,
 *  FE FF          = UTF-16, big-endian
 *  FF FE          = UTF-16, little-endian (Win2k notepad)
**/

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

/**
 * This inputstream will recognize unicode BOM marks
 * and will skip bytes if getEncoding() method is called
 * before any of the read(...) methods.
 * 
 * Designed to work around 'wontfix' Java bug:
 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4508058
 *
 * Usage pattern:
     String enc = "ISO-8859-1"; // or NULL to use systemdefault
     FileInputStream fis = new FileInputStream(file);
     UnicodeInputStream uin = new UnicodeInputStream(fis, enc);
     enc = uin.getEncoding(); // check and skip possible BOM bytes
     InputStreamReader in;
     if (enc == null) in = new InputStreamReader(uin);
     else in = new InputStreamReader(uin, enc);
 */
public class UnicodeInputStream extends InputStream {
	PushbackInputStream	internalIn;
	boolean            isInited = false;
	BOMs              	defaultEnc;
	BOMs				encoding;

	/**
	 * Possible byte-order markers.
	 */
	enum BOMs {
		UTF_32BE("UTF-32BE", 4),
		UTF_32LE("UTF-32LE", 4),
		UTF_16BE("UTF-16BE", 2),
		UTF_16LE("UTF-16LE", 2),
		UTF_8("UTF-8", 3),
		NO_BOM("Default", 0);
		
		private String n;
		private int s;
		BOMs(String name, int size) { n = name; s = size; }
		@Override
		public String toString() { return n; }
		public int getSize() { return s; }
	}
	
	/** Largest possible BOM size */
	private static final int BOM_SIZE = 4;

	public UnicodeInputStream(InputStream in) {
		this(in, BOMs.NO_BOM);
	}
	
	public UnicodeInputStream(InputStream in, BOMs defaultEnc) {
		internalIn = new PushbackInputStream(in, BOM_SIZE);
		this.defaultEnc = defaultEnc;
	}

	/**
	 * Read-ahead four bytes and check for BOM marks. Extra bytes are
	 * unread back to the stream, only BOM bytes are skipped.
	 */
	protected void init() throws IOException {
		if (isInited) return;

		byte bom[] = new byte[BOM_SIZE];
		int n, unread;
		n = internalIn.read(bom, 0, bom.length);

		if ( (bom[0] == (byte)0x00) && (bom[1] == (byte)0x00) &&
				(bom[2] == (byte)0xFE) && (bom[3] == (byte)0xFF) ) {
			encoding = BOMs.UTF_32BE;
		} else if ( (bom[0] == (byte)0xFF) && (bom[1] == (byte)0xFE) &&
				(bom[2] == (byte)0x00) && (bom[3] == (byte)0x00) ) {
			encoding = BOMs.UTF_32LE;
		} else if (  (bom[0] == (byte)0xEF) && (bom[1] == (byte)0xBB) &&
				(bom[2] == (byte)0xBF) ) {
			encoding = BOMs.UTF_8;
		} else if ( (bom[0] == (byte)0xFE) && (bom[1] == (byte)0xFF) ) {
			encoding = BOMs.UTF_16BE;
		} else if ( (bom[0] == (byte)0xFF) && (bom[1] == (byte)0xFE) ) {
			encoding = BOMs.UTF_16LE;
		} else {
			// Unicode BOM mark not found, unread all bytes
			encoding = defaultEnc;
		}      

		// System.out.println("read=" + n + ", unread=" + unread);
		unread = n - encoding.getSize();
		if (unread > 0) internalIn.unread(bom, (n - unread), unread);
		isInited = true;
	}

	@Override
	public void close() throws IOException {
		isInited = true;
		internalIn.close();
	}
	
	/**
	 * For now returns false, for safety.
	 * TODO: Return internal stream's status.
	 * @see java.io.InputStream#markSupported()
	 */
	@Override
	public boolean markSupported() {
		return false;
	    //return internalIn.markSupported();
	}
	
	@Override
	public int read() throws IOException {
		init();
		return internalIn.read();
	}
}
