/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Feb 17, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *
 */

package ed.inf.proofgeneral.views.output;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

/**
 *
 */
public class FormTextTest {
	public static void main(String [] args) {
		String text = "<form><p>" +
		    "tester"
			+ "</p></form>";

		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		FormToolkit toolkit = new FormToolkit(display);
		ScrolledForm form = toolkit.createScrolledForm(shell);
		TableWrapLayout layout = new TableWrapLayout();
		form.getBody().setLayout(layout);
		FormText formText = toolkit.createFormText(form.getBody(), true);
		formText.setWhitespaceNormalized(false);
		formText.setText(text, true, false);

		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}
}
