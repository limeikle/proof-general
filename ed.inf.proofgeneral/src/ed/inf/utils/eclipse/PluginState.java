/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Feb 5, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *
 */

package ed.inf.utils.eclipse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

import ed.inf.proofgeneral.ProofGeneralPlugin;

/**
 * Utility class to manage the persistency of our UI state of the plugin.
 * NB: persistency of views is handled in org.eclipse.ui.workbench
 * This isn't used at the moment.
 */
public class PluginState {

	public static IMemento getPluginStateMemento(String stateFilename) {
		File stateFile = ProofGeneralPlugin.getDefault().getStateLocation().append(stateFilename).toFile();
		FileReader reader;
		try {
			reader = new FileReader(stateFile);
			IMemento memento = XMLMemento.createReadRoot(reader);
			return memento;
		} catch (FileNotFoundException e) {
			// FIXME: log it
		} catch (WorkbenchException e) {
			// FIXME: log it
		}
		return null;
	}

	public static void setPluginStateMemento(String stateFilename, XMLMemento memento) {
		File stateFile = ProofGeneralPlugin.getDefault().getStateLocation().append(stateFilename).toFile();
		try {
			if (!stateFile.exists()) {
				stateFile.createNewFile();
			}
			Writer writer = new FileWriter(stateFile);
			memento.save(writer);
        } catch (IOException e) {
        	// FIXME: log it
        }
	}

}
