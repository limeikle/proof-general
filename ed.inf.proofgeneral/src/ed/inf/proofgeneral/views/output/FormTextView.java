/*
 *  $RCSfile: FormTextView.java,v $
 *
 *  Created on 17 February 2007 by David Aspinall
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.views.output;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.part.ViewPart;

import ed.inf.proofgeneral.ui.theme.PGColors;

/**
 * @author Daniel Winterstein
 */
public class FormTextView extends ViewPart {

	private FormToolkit toolkit;
	private ScrolledForm form;
	private FormText formText;
	String text;

	/**
	 * The constructor.
	 */
	public FormTextView() {
	}

	/**
	 * Create a simple form with no title and a formText body
	 * initialize it.
	 */
	@Override
    public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		form = toolkit.createScrolledForm(parent);
		TableWrapLayout layout = new TableWrapLayout();
		form.getBody().setLayout(layout);
		formText = toolkit.createFormText(form.getBody(), true);
		formText.setWhitespaceNormalized(false);
		//FIXME alex no checkin
//		// we can set title like this: form.setText(title);
//		formText.setColor("header", toolkit.getColors().
//			     getColor(IFormColors.TITLE));
		formText.setColor("blue", PGColors.getColor("COLOR_BLUE"));
		formText.setColor("green", PGColors.getColor("COLOR_GREEN"));
		formText.setColor("red", PGColors.getColor("COLOR_RED"));
		formText.setFont("header", JFaceResources.getHeaderFont());
		formText.setFont("code", JFaceResources.getTextFont());
		formText.setFont(JFaceResources.getTextFont());
		if (text != null) {
			setText(text);
		}
	}

	/**
	 * Passes the focus request to the form.
	 */
	@Override
    public void setFocus() {
		form.setFocus();
	}

	/**
	 * Disposes the toolkit
	 */
	@Override
    public void dispose() {
		toolkit.dispose();
		super.dispose();
	}


    /**
     * This method expects formText XML fragments; it will add <form> ... </form> wrapping
     * @param xmltext the fragment to wrap and display in the browser
     */
    public void setText(String xmltext) {
    	text = "<form>" + xmltext + "</form>";
    	if (formText != null) {
    		//
    		formText.setText("<form>" + // text
    				"<p>proof (prove) yup: step 4" +
    				"using this:\n" +
    				  "<span color=\"blue\">A</span> ∧ <span color=\"blue\">B</span>\n" +
    				"goal (1 subgoal):\n" +
    				"1. <span color=\"blue\">B</span> ∧ <span color=\"blue\">A</span></p>"
    				//"<p>hello <span color=\"header\">test </span><b>me</b>" + xmltext.length() + "</p>"
    				//"<p>hello</p>"
    							+ "</form>", true, false);
    		formText.update();
    		formText.redraw();
    	}
    }

    public String getText() {
        return text;
    }

}
