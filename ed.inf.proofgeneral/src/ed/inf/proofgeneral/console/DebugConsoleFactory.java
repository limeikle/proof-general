/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jan 8, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *
 */

package ed.inf.proofgeneral.console;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleFactory;
import org.eclipse.ui.console.IConsoleManager;

/**
 * Factory for debug consoles.
 * @author David Aspinall
 */
// We could be much more elaborate here, e.g. optionally hiding PGIP packet details,
// adding hyperlinks to documents, an interrupt button, real input to the subprocess,
// etc, etc.  Not worth it just now, this is adequate for debugging/inspection.
public class DebugConsoleFactory implements IConsoleFactory {

	public void openConsole() {
		showConsole();
	}

	public static void showConsole() {
		DebugConsole console =DebugConsole.getCurrentConsole();
		if (console != null) {
			IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
			IConsole[] existing = manager.getConsoles();
			boolean exists = false;
			for (int i = 0; i < existing.length; i++) {
				if(console == existing[i]) {
					exists = true;
				}
			}
			if (!exists) {
				manager.addConsoles(new IConsole[] {console});
			}
			manager.showConsoleView(console);
		}
	}

// da: This isn't in the interface
//	public static void closeConsole() {
//		IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
//		manager.removeConsoles((IConsole[])ProofConsole.getAllConsoles().toArray());
//	}
}
