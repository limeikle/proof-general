/*
 *  $RCSfile: PGContentOutlinePage.java,v $
 *
 *  Created on 29 May 2004
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.document.outline;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dom4j.Element;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import ed.inf.proofgeneral.Constants;
import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.ContainerElement;
import ed.inf.proofgeneral.document.DocElement;
import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.document.ScriptManagementDocumentEvent;
import ed.inf.proofgeneral.editor.ProofScriptEditor;
import ed.inf.proofgeneral.editor.actions.retargeted.GotoAction;
import ed.inf.proofgeneral.preferences.PreferenceNames;
import ed.inf.proofgeneral.sessionmanager.PGIPSyntax;
import ed.inf.utils.datastruct.StringManipulation;


// da: TODO here:
// - add automatic tree opening/closing according to processed position
// - add popup menu for goto position, cut/copy corresponding to document parts
// - indication of queue direction

/**
 * A content outline page which always represents the content of the
 * connected editor in 10 segments.
 */
public class PGContentOutlinePage extends ContentOutlinePage {

    protected DrillDownAdapter drillDownAdapter;

    static boolean SHOW_COMMENTS = false; // TODO: make this into a preference
    
    final IAction parseDocAction;
    final GotoAction gotoAction;
    
	/**
	 * Creates a content outline page using the given provider and the given editor.
	 */
	public PGContentOutlinePage(IDocumentProvider provider, ProofScriptEditor editor) {
		super();
		fDocumentProvider= provider;
		fTextEditor= editor;
        parseDocAction = editor.getAction(Constants.PARSE_ACTIONID);
        gotoAction = (GotoAction)(editor.getAction(Constants.GOTO_ACTIONID));
	}

	@Override
    public void createControl(Composite parent) {
		super.createControl(parent);
		TreeViewer viewer= getTreeViewer();
		drillDownAdapter = new DrillDownAdapter(viewer);
		TreeContentProvider tcp = new TreeContentProvider();
		viewer.setContentProvider(tcp);
		//tcp.setOutlinePage(this);
		viewer.setLabelProvider(new PGOutlineLabelProvider(fTextEditor.getSessionManager().proverInfo.syntax,
		        										   fTextEditor.getDocument()));
		viewer.addSelectionChangedListener(this);
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				ISelection selection= event.getSelection();
				if (!selection.isEmpty() &&
						((IStructuredSelection) selection).getFirstElement() instanceof IPGOutlineElement) {
				    try {
				        IPGOutlineElement segment= (IPGOutlineElement) ((IStructuredSelection) selection).getFirstElement();
				        int start= segment.getPosition().getOffset();
				        gotoAction.setTargetOffset(start);
				        gotoAction.run();
				    } catch (ArrayIndexOutOfBoundsException e) {
				    	
				    }
				}
			}
		});
		
		if (fInput != null) {
			viewer.setInput(fInput);
		}
	}


    /**
     @see org.eclipse.ui.part.IPage#setActionBars(org.eclipse.ui.IActionBars)
     */
    @Override
    public void setActionBars(IActionBars actionBars) {
        actionBars.getToolBarManager().add(parseDocAction);
    }
    
	protected class TreeContentProvider implements
	IStructuredContentProvider, ITreeContentProvider,IDocumentListener  {

		protected IPGOutlineElement fContent = null;
		protected ProofScriptDocument fDoc = null;
	    Runnable updater;

	    TreeContentProvider() {
	        super();
	        updater = new Runnable() {
	            public void run() {
	                update();
	    		}
	        };
	    }

		void setDoc(ProofScriptDocument doc) {
		    if (doc == fDoc) {
				return;
			}
		    if (fDoc != null) {
				fDoc.removeDocumentListener(this);
			}
		    fDoc = doc;
		    if (doc==null) {
		        fContent = null;
		        return;
		    }
		    doc.addDocumentListener(this);
		    fContent = doc.getRootElement();
		}

		/**
		 * Sets the document to the changed input.
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			if (newInput != null) {
				ProofScriptDocument document= (ProofScriptDocument) fDocumentProvider.getDocument(newInput);
				setDoc(document);
			} else {
			    setDoc(null);
			}
		}

		/**
		 * Sets the stored content to null. 
		 * @see IContentProvider#dispose
		 */
		public void dispose() {
			if (fContent != null) {
				//fContent.clear();
				fContent= null;
			}
		}

		/**
		 * Always returns false.
		 * @see IContentProvider#isDeleted(Object)
		 */
		public boolean isDeleted(Object element) {
			return false;
		}

	    /**
	     * @see org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(org.eclipse.jface.text.DocumentEvent)
	     */
	    public void documentAboutToBeChanged(DocumentEvent event) {
	    }

	    /**
	     * @see org.eclipse.jface.text.IDocumentListener#documentChanged(org.eclipse.jface.text.DocumentEvent)
	     */
	    public void documentChanged(DocumentEvent event) {
	        if (!(event instanceof ScriptManagementDocumentEvent)) {
				return;
			}
	        if (!(event instanceof ScriptManagementDocumentEvent.ParseChangeEvent)) {
	            // command sent or undone
	            org.eclipse.swt.widgets.Display.getDefault().asyncExec(updater);
	            return;
	        }
	        if (event.getDocument() instanceof ProofScriptDocument) {
	            DocElement newContent = ((ProofScriptDocument)event.getDocument()).getRootElement();
	            if (fContent != newContent) {
	                fContent = newContent;
	            }
	        }
	        org.eclipse.swt.widgets.Display.getDefault().asyncExec(updater);
	    }

		/**
		 * Filter a list of elements for those that should be displayed in the outline.
		 * May be overridden.
		 * @param allElements the list to filter
		 * @return a smaller list
		 */
		public List<IPGOutlineElement> filterList(List allElements) {
		    List<IPGOutlineElement> elements = new ArrayList<IPGOutlineElement>();
		    for(Object obj : allElements) {
		    	IPGOutlineElement e = (IPGOutlineElement)obj; 
		        String name = e.getName();
		        PGIPSyntax syntax = fDoc.getSyntax();
		        // Ignore 100% parsed message
				if (e instanceof ParseInfo &&  fDoc.isFullyParsed()) {
						continue;
				}
		        // Ignore whitespace
		        if (syntax.subType(name,PGIPSyntax.WHITESPACE)) {
		        	continue;
		        }
		        // Ignore block structure markup
		        if (syntax.subType(name,PGIPSyntax.OPENBLOCK)) {
		        	continue;
		        }
		        if (syntax.subType(name,PGIPSyntax.CLOSEBLOCK)) {
		        	continue;
		        }
	        	// Skip goal statements: their containers are labelled instead
		        if (syntax.subType(name, PGIPSyntax.OPENGOAL)) {
		        	continue;
		        }
	        	// Skip close goal statements in case they are the first in
		        // a proof (this is somewhat Isabelle specific: but we take an
		        // immediate closegoal to mean a trivial proof).
		        else if (syntax.subType(name, PGIPSyntax.CLOSEGOAL) &&
		        		// FIXME: can change List<Element> to List<DocElement> every where, prob.
		        		 (e instanceof DocElement)) {
	        		// Little bit tricky as parent of CLOSEGOAL is actually 
	        		// *containing* element, not the proof container (FIXME: why? was this for undo?)
		        	// Otherwise we could just check parent for equality like so:
//		        	if (e.getParent() instanceof ContainerElement) {
//		        		ContainerElement parent = (ContainerElement) e.getParent();
//		        		if (parent.getFirstElement() == e) {
//		        			continue;
//		        		}
//		        	}
		        	DocElement closegoal = (DocElement) e;
		        	int closegoalOffset = closegoal.getPosition().getOffset();
		        	// FIXME: doesn't work, this gets the surrounding element again, darn.
		        	DocElement prev = fDoc.findPrevious(PGIPSyntax.OPENGOAL, closegoalOffset);
		        	int prevEnd = prev.getPosition().getOffset() + prev.getPosition().getLength();
		        	try {
		        		if ((prevEnd <= closegoalOffset) // sanity check
		        				&& (StringManipulation.isWhitespace(fDoc.get(prevEnd+1,closegoalOffset)))) {
		        			continue;
		        		}
		        	} catch (BadLocationException ex) {
		        		// shouldn't occur (will add element)
		        	}
		        }
  		        // Skip all comments, maybe, or definitely single-line comments appearing after some text
		        else if (syntax.subType(name,PGIPSyntax.COMMENT) ||
		        		 syntax.subType(name, PGIPSyntax.DOCCOMMENT)) {
		        	if (!SHOW_COMMENTS) {
		        		continue;
		        	}
		        	// comment is line comment not at start of line, but after some text
		        	if (e instanceof DocElement) {
		        		int estart = ((DocElement) e).getStartOffset();
		        		int eend = ((DocElement) e).getEndOffset();
		        		try {
		        			IRegion line = fDoc.getLineInformationOfOffset(estart);
		        			int linestart = line.getOffset();
		        			if ((linestart + line.getLength() > eend) &&
		        				!StringManipulation.isWhitespace(fDoc.get(linestart, estart-linestart))) {
		        				continue;
		        			}
		        		} catch (BadLocationException x) {
		        			// do nothing
		        		}
		        	}
		        	if (StringManipulation.isWhitespace(((DocElement)e).getText())) {
		        		continue;
		        	}
		        }
		        	
		        // Isabelle specific: hide proofsteps of just 'proof'
		        else if (syntax.subType(name,PGIPSyntax.PROOFSTEP)
		                && StringManipulation.trim(((DocElement) e).getText()).startsWith("proof")) {
		        	continue;
		        }
		        	
		        // Isabelle specific: hide duplicated section/subsection statement, label is in container 
		        else if ((syntax.subType(name, PGIPSyntax.DOCCOMMENT)) &&
		        		 (((DocElement) e).getStringValue().startsWith("section") ||
				 	      ((DocElement) e).getStringValue().startsWith("subsection"))) {
		        	continue;
		        }
		        
		        // If no exceptions, add this element to the outline view.
		        elements.add(e);
		    }
		    return elements;
		}

		/**
		 * @see IStructuredContentProvider#getElements(Object)
		 */
        public Object[] getElements(Object element) {
		    if (fContent==null) {
				return null;
			}
		    if (ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_DEBUG_SHALLOW_OUTLINE)) {
		    	return debugElements();
		    }
		    List<Object> rawelements = fContent.elements(); // NB: unchecked conv
		    List<IPGOutlineElement> elements = filterList(rawelements);
		    ParseInfo parseInfo = new ParseInfo(fDoc);
		    elements.add(parseInfo);
		    return elements.toArray();
		}

		/**
         * @return contents for outline page when debug is enabled
         */
        private Object[] debugElements() {
	        String[] cats = fDoc.getPositionCategories();
	        final List<Object> posnList = new ArrayList<Object>();
	        try {
	        	posnList.add("** Document Tree **");
	        	posnList.add(fDoc.getRootElement());
	        	for (String c : cats) {
	        		posnList.add("** " + c + " **");
	        		Position[] posns = fDoc.getPositions(c);
	        		posnList.addAll(Arrays.asList(posns));
	        	}
	        	return posnList.toArray();
	        } catch (Exception ex) {
	        	ex.printStackTrace();
	        	return null;
	        }
        }

		/*
		 * @see ITreeContentProvider#hasChildren(Object)
		 */
		@SuppressWarnings("unchecked")
        public boolean hasChildren(Object element) {
//		    if (element instanceof IPGOutlineElement) {
//			    List<IPGOutlineElement> kids = filterList(((IPGOutlineElement)element).elements());
//			    return kids != null && kids.size() > 0;
//		    }
//		    return false;
			// da: faster but more approximate test
			return element instanceof ContainerElement;
		}

		/*
		 * @see ITreeContentProvider#getParent(Object)
		 */
		public Object getParent(Object element) {
			if (element instanceof Element) {
				return ((Element) element).getParent();
			}
			return null;
		}

		/*
		 * @see ITreeContentProvider#getChildren(Object)
		 */
		@SuppressWarnings("unchecked")
        public Object[] getChildren(Object element) {
		    if (element instanceof ContainerElement) {
		        return filterList( ((Element) element).elements() ).toArray();
		    }
			return new Object[0];
		}
	}

	protected Object fInput;
	protected IDocumentProvider fDocumentProvider;
	protected ProofScriptEditor fTextEditor;
    

	@Override
    public void selectionChanged(SelectionChangedEvent event) {
		super.selectionChanged(event);
		ISelection selection= event.getSelection();
		if (selection.isEmpty()) {
			fTextEditor.resetHighlightRange();
			return;
		}
		if (((IStructuredSelection) selection).getFirstElement() instanceof IPGOutlineElement) {
		    try {
		        IPGOutlineElement segment= (IPGOutlineElement) ((IStructuredSelection) selection).getFirstElement();
		        int start= segment.getPosition().getOffset();
		        int length= segment.getPosition().getLength();
				fTextEditor.setHighlightRange(start, length, true);
				if (ProofGeneralPlugin.debug(this)) {
					// in debug mode, set the editor's selection as well as highlight ruler
				    ISelection sel = new TextSelection(segment.getProofScript(),start,length+1);
				    fTextEditor.getSelectionProvider().setSelection(sel);
				}
				return;
			} catch (Exception x) {
			    x.printStackTrace();
			}
		}
		fTextEditor.resetHighlightRange();
	}

	/**
	 * Sets the input of the outline page
	 */
	public void setInput(Object input) {
		fInput= input;
		update();
	}

	/**
	 * Updates the outline page.
	 */
	public void update() {
		TreeViewer viewer= getTreeViewer();
		if (viewer != null) {
			Control control= viewer.getControl();
			if (control != null && !control.isDisposed()) {
				if (viewer.getInput()!=fInput) {
					control.setRedraw(false);
					viewer.setInput(fInput);
					control.setRedraw(true);
				} else {
					//this seems to run much faster than the above, and reliably too -AH
					// FIXME da: on large files I get a lot of time in here...
					viewer.refresh();
				}
				//dirty = true;
				//viewer.expandAll();
				//control.setRedraw(true);
			}
		}
	}

	/**
	 * Provides feedback on how the parse is going (i.e. "?% parsed")
	 */
	public static class ParseInfo implements IPGOutlineElement {
	    private final ProofScriptDocument doc;
	    private final Position pos;
	    public ParseInfo(ProofScriptDocument doc) {
	    	this.doc = doc;
	    	this.pos = new Position(doc.getLength()>0 ? doc.getLength()-1 : 0,0);
	    }
	    public ProofScriptDocument getProofScript() {
	    	return doc;
	    }
	    public Position getPosition() {
	    	// simpler than position updater: make sure correct when fetched.
	    	pos.setOffset(doc.getLength() > 0 ? doc.getLength()-1 : 0);
	    	return pos;
	    }
	    public String getName() {
	    	return "parseinfo";
	    }
        @Override
        public String toString() {
		    if (doc==null) {
				return "??% parsed";
			}
		    String s;
		    if (doc.isFullyParsed()) {
		    	return "100% parsed";
		    }
		    int offset = doc.getParseOffset()+1;
		    int length = doc.getLength();
		    s = Integer.toString(100*offset/length);
		    s += "% parsed";
		    return s;
	    }
        public List<Object> elements() {
        	return new AbstractList<Object>() {
        		// An empty list
                @Override
                public Object get(int index) {
	                throw new IndexOutOfBoundsException("ParseInfo: Empty list");
                }
                @Override
                public int size() {
	                return 0;
                } };
        }
	}
	

}
