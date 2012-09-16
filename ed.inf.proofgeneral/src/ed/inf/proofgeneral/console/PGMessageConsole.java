/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Feb 16, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *
 */

package ed.inf.proofgeneral.console;

import java.io.IOException;

import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.ui.theme.PGFonts.PGFont;

/**
 * Message console with an output stream.
 */
public class PGMessageConsole extends MessageConsole {

	protected MessageConsoleStream outstream;

	public PGMessageConsole(String name) {
		super("Proof General: " + name,
				ProofGeneralPlugin.getImageDescriptor("icons/star16.gif"),
				true);
		outstream = this.newMessageStream();
		this.setFont(PGFont.CONSOLE.get());
	}

	/**
	 * Write a message to this console.
	 * @param str
	 */
	public void write(String str) {
		if (outstream != null) {
			try {
				outstream.write(str);
			} catch (IOException x) {
				outstream = null;
			}
		}
	}
}
