/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jan 8, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *
 */

package ed.inf.proofgeneral.console;

import java.util.Collection;
import java.util.HashMap;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.sessionmanager.SessionManager;

/**
 * A debug message console for displaying system-level debug messages.
 * @author David Aspinall
 */
public class DebugConsole extends PGMessageConsole {

	public DebugConsole(SessionManager sm) {
		super(sm.proverInfo.name + " Debug Messages");
		sm.connectDebugConsole(this);
		outstream.setActivateOnWrite(true);
	}

	// Might be better to have an a list here, easier to look up SM from Console.
	private static HashMap<SessionManager,DebugConsole> consoleMap = new HashMap<SessionManager,DebugConsole>();

	/**
	 * Get a console for the default session manager, making one if necessary
	 * @return a DebugConsole or null if there is no active session manager.
	 */
	static DebugConsole getCurrentConsole() {
		SessionManager sm = ProofGeneralPlugin.getSomeSessionManager();
		if (sm != null) {
			if (consoleMap.containsKey(sm)) {
				return consoleMap.get(sm);
			}
			DebugConsole pc = new DebugConsole(sm);
            consoleMap.put(sm, pc);
            return pc;
		}
		return null;
	}

	static Collection<DebugConsole> getAllConsoles() {
		return consoleMap.values();
	}

	void deleteConsole(DebugConsole pc) {
		// TODO.  Invoke deletion when SM dies.
		// Where does console deletion happen in Eclipse?
	}
}
