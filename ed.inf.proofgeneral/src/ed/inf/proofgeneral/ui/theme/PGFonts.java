/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Feb 16, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *
 */

package ed.inf.proofgeneral.ui.theme;

import org.eclipse.swt.graphics.Font;
import org.eclipse.ui.PlatformUI;

/**
 * Proof General fonts defined within theme elements,
 * declared in the org.eclipse.ui.themes extension.
 * The workbench theme system manages the life cycle of these.
 * @author David Aspinall
 */
public class PGFonts {

	public enum PGFont {
		// NB: see plugin.xml for the fontDefinition elements which
		// define these identifiers and their default fonts.  Preferences settings
		// then appear under Preferences -> Appearance -> fonts and Fonts.
		CONSOLE ("ed.inf.proofgeneral.CONSOLE");

		private final String plugin_fontdefinition_id;

		private PGFont(String id) {
			plugin_fontdefinition_id = id;
		}

		public Font get() {
			return PlatformUI.getWorkbench().getThemeManager().
			getCurrentTheme().getFontRegistry().get(plugin_fontdefinition_id);
		}
	}

	public static Font getPlatformFont(String str) {
		return PlatformUI.getWorkbench().getThemeManager().
			getCurrentTheme().getFontRegistry().get(str);
	}
}


