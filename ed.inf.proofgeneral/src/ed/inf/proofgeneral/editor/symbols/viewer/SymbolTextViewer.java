/*
 *  $RCSfile: SymbolTextViewer.java,v $
 *
 *  Created on Nov 19, 2006 by da
 *  part of Proof General for Eclipse
 */

package ed.inf.proofgeneral.editor.symbols.viewer;

import org.eclipse.jface.text.IDocumentAdapter;
import org.eclipse.jface.text.TextViewer;


/**
 *
 */
public class SymbolTextViewer extends TextViewer {

	/**
	 * @see org.eclipse.jface.text.TextViewer#createDocumentAdapter()
	 */
	@Override
	protected IDocumentAdapter createDocumentAdapter() {
		return new SymbolDocumentAdapter();
	}

}
