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
 * Console for tracing messages from the prover.  These are messages which may
 * be possibly voluminous (tens or hundreds/second) : they are filtered from ordinary
 * output and minimally marked up.
 * @author David Aspinall
 */
public class TracingConsole extends PGMessageConsole {

	public TracingConsole(SessionManager sm) {
		super(sm.proverInfo.name + " Tracing Messages");
		sm.connectTracingConsole(this);
	}

	// Might be better to have an alist here, easier to look up SM from Console.
	private static HashMap<SessionManager,TracingConsole> consoleMap = new HashMap<SessionManager,TracingConsole>();

	/**
	 * Get a console for the default session manager, making one if necessary
	 * @return a TracingConsole or null if there is no active session manager.
	 */
	static TracingConsole getCurrentConsole() {
		SessionManager sm = ProofGeneralPlugin.getSomeSessionManager();
		if (sm != null) {
			if (consoleMap.containsKey(sm)) {
				return consoleMap.get(sm);
			}
			TracingConsole pc = new TracingConsole(sm);
            consoleMap.put(sm, pc);
            return pc;
		}
		return null;
	}

	static Collection<TracingConsole> getAllConsoles() {
		return consoleMap.values();
	}

	static void deleteConsole(TracingConsole pc) {
		// TODO.  Invoke deletion when SM dies.
		// Where does console deletion happen in Eclipse?
	}
}
