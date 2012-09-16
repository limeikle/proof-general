/*
 * Browser example snippet: render HTML from memory
 *
 * For a list of all SWT example snippets see
 * http://www.eclipse.org/swt/snippets/
 *
 * @since 3.0
 */
package ed.inf.proofgeneral.views.output;
import org.eclipse.swt.*;
import org.eclipse.swt.browser.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

// da: added this to experiment with embedded browser to test support
// for fonts, etc.

public class Snippet136 {
	public static void main(String [] args) {
		String html = "<HTML><HEAD><TITLE>HTML Test</TITLE></HEAD><BODY>";
		//html += "<div style=\"font: fixed;\"";
		html += "<div style=\"font-family:Mono\">";
		for (int i = 0; i < 100; i++) html += "This is line "+i+"<br/>";
		//html += "</div>";
		html += "</BODY></HTML>";

		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		Browser browser = new Browser(shell, SWT.NONE);
		browser.setText(html);
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}
}