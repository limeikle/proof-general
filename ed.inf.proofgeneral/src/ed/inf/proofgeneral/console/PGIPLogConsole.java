/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jan 8, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *
 */

package ed.inf.proofgeneral.console;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;

import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.proofgeneral.ui.theme.PGColors.PGColor;
import ed.inf.proofgeneral.ui.theme.PGFonts.PGFont;

/**
 * A PGIP log console which connects to a session manager, for displaying a log
 * of PGIP communications.
 * @author David Aspinall
 */
//We could be much more elaborate here, e.g. optionally hiding PGIP packet details,
//adding hyperlinks to documents, an interrupt button, real input to the subprocess,
//etc, etc.  Not worth it just now, this is adequate for debugging/inspection.
public class PGIPLogConsole extends IOConsole {

	/** Stream for messages sent from prover */
	private IOConsoleOutputStream output;
	/** Stream for messages sent to prover */
	private IOConsoleOutputStream input;

	public PGIPLogConsole(SessionManager sm) {
		super("Proof General: " + sm.proverInfo.name + " PGIP Log",
				"ed.inf.proofgeneral",
				ProofGeneralPlugin.getImageDescriptor("icons/star16.gif"));
		output = this.newOutputStream();
		input = this.newOutputStream();
        input.setColor(PGColor.PGIP_CONSOLE_INPUT.get());
        this.setFont(PGFont.CONSOLE.get());
		sm.connectConsole(this);
	}

	/**
	 * Write a string to the console, as output from the prover.
	 * @param str
	 */
	public void writeoutput(String str) {
		if (output != null) {
			try {
				output.write(str);
			} catch (IOException e) {
				output = null;
			}
		}
	}

	/**
	 * Write a string to the console, as input from the prover.
	 * @param str
	 */
	public void writeinput(String str) {

		if (input != null) {
			try {
				input.write(str);
			} catch (IOException e) {
				input = null;
			}
		}
	}

	// Might be better to have an alist here, easier to look up SM from Console.
	private static HashMap<SessionManager,PGIPLogConsole> consoleMap = new HashMap<SessionManager,PGIPLogConsole>();

	/**
	 * Get a console for the default session manager, making one if necessary
	 * @return a PGIPLogConsole or null if there is no active session manager.
	 */
	static PGIPLogConsole getCurrentConsole() {
		SessionManager sm = ProofGeneralPlugin.getSomeSessionManager();
		if (sm != null) {
			if (consoleMap.containsKey(sm)) {
				return consoleMap.get(sm);
			}
			PGIPLogConsole pc = new PGIPLogConsole(sm);
            consoleMap.put(sm, pc);
            return pc;
		}
		return null;
	}

	static Collection<PGIPLogConsole> getAllConsoles() {
		return consoleMap.values();
	}

	static void deleteConsole(PGIPLogConsole pc) {
		// TODO.  Invoke deletion when SM dies.
		// Where does console deletion happen in Eclipse?
	}
}
