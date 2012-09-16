package ed.inf.proofgeneral.document;

import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.search.core.text.TextSearchEngine;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.core.text.TextSearchScope;

/**
 * Searching in possibly symbolised documents.  
 * TODO: this should desymbolise the search pattern (a nuisance it's been converted
 * to a pattern now)
 */
public class SymbolisedSearch extends TextSearchEngine {

	public SymbolisedSearch() {
	}

	@Override
	public IStatus search(TextSearchScope scope, TextSearchRequestor requestor,
	        Pattern searchPattern, IProgressMonitor monitor) {
		return TextSearchEngine.create().search(scope,requestor,searchPattern,monitor);
	}

	@Override
	public IStatus search(IFile[] scope, TextSearchRequestor requestor, Pattern searchPattern,
	        IProgressMonitor monitor) {
		return TextSearchEngine.create().search(scope,requestor,searchPattern,monitor);
	}
}
