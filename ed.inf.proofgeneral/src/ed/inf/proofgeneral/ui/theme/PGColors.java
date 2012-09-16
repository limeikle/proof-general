/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Feb 16, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *
 */

package ed.inf.proofgeneral.ui.theme;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.PlatformUI;

import ed.inf.proofgeneral.sessionmanager.PGIPSyntax;

/**
 * Proof General colours defined within theme elements,
 * declared in the org.eclipse.ui.themes extension.
 * The workbench theme system manages the life cycle of these
 * colours.
 * @author David Aspinall
 */
public class PGColors {

	public enum PGColor {
		// NB: see plugin.xml for the colorDefinition elements which
		// define these identifiers and their default colours.  Preferences settings
		// then appear under Preferences -> Appearance -> Colors and Fonts.
		// This renders old colour preferences page obsolete.
		PROCESSED ("PROCESSED"),
		BUSY      ("BUSY"),
		OUTLINE_PROCESSED_FG ("OUTLINE_PROCESSED_FG"),
		OUTLINE_PROCESSED_BG ("OUTLINE_PROCESSED_BG"),
		OUTLINE_BUSY_FG      ("OUTLINE_BUSY_FG"),
		OUTLINE_BUSY_BG      ("OUTLINE_BUSY_BG"),		
		OUTLINE_OUTDATED_FG  ("OUTLINE_OUTDATED_FG"),
		OUTLINE_OUTDATED_BG  ("OUTLINE_OUTDATED_BG"),		
		KEYWORD   ("KEYWORD"),
		STRING    ("STRING"),
		COMMENT   ("COMMENT"),
		HOLE      ("HOLE"),
		ERRORBG   ("ERRORBACKGROUND"),
		PGIP_CONSOLE_INPUT ("PGIP_CONSOLE_INPUT");

		private final String plugin_colordefinition_id;

		private PGColor(String id) {
			plugin_colordefinition_id = "ed.inf.proofgeneral." + id;
		}

		public Color get() {
			return PlatformUI.getWorkbench().getThemeManager().
			getCurrentTheme().getColorRegistry().get(plugin_colordefinition_id);
		}

		/**
		 * @param type
		 * @return a colour for the given type, SWT.COLOR_BLACK if nothing special available
		 */
		public static Color getColorForTokenType(String type) {
			if (type.equals(PGIPSyntax.KEYWORD)) {
				return KEYWORD.get();
			} else if (type.equals(PGIPSyntax.COMMENT)) {
				return COMMENT.get();
			} else if (type.equals(PGIPSyntax.STRING)) {
				return STRING.get();
			} else if (type.equals(PGIPSyntax.TRIGGERWORD)) {
				return HOLE.get();
			} else {
				return PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK);
			}
		}
	}

	/**
	 * Fetch a colour from the theme.
	 * @param name
	 * @return the named colour from the theme, or black if one cannot be found
	 */
	public static Color getColor(String name) {
		Color col = PlatformUI.getWorkbench().getThemeManager().
			getCurrentTheme().getColorRegistry().get(name);
		if (col == null) {
			col = PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_BLACK);
		}
		return col;
	}
}
