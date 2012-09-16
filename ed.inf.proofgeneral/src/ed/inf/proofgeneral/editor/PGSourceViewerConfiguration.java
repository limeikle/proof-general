/*
 *  $RCSfile: PGSourceViewerConfiguration.java,v $
 *
 *  Created on 18 Apr 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.DefaultTextDoubleClickStrategy;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.DocElement;
import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.editor.lightparser.PGCodeScanner;
import ed.inf.proofgeneral.editor.lightparser.PartitionScanner;
import ed.inf.proofgeneral.sessionmanager.PGIPSyntax;
import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.proofgeneral.views.output.LatestOutputView;
import ed.inf.utils.datastruct.MutableObject;
import ed.inf.utils.datastruct.StringManipulation;
import ed.inf.utils.eclipse.HTMLTextPresenter;
import ed.inf.utils.process.RunnableWithParams;

/**
 * @author Daniel Winterstein
 */
public class PGSourceViewerConfiguration extends SourceViewerConfiguration {

    private PGIPSyntax syntax;
    //private ProverInfo proverInfo;
    private final ProofScriptDocument doc;
    SessionManager sm;

    public PGSourceViewerConfiguration(SessionManager sm, ProofScriptDocument doc) {
    	this.sm = sm;
    	//this.proverInfo = sm.proverInfo;
    	if (sm != null) {
    		this.syntax = sm.proverInfo.syntax;
    	} else {
    		// FIXME da: not sure if this actually happens, maybe on startup or if
    		// we get called for non-proof scripts (which we shouldn't)
    		System.err.println("PGSourceViewerConfiguration called with null Session Manager!  Things may go wrong...");
    	}
    	this.doc = doc;
    }


    @Override
    public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
    	return PartitionScanner.getPartitionTypes(sm.proverInfo);
	}
    
	@Override
    public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		return PartitionScanner.LIGHT_PG_PARTITIONING;
	}

	//static class MyInfoControl extends Composite implements IInformationControl
	@Override
    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		ContentAssistant assistant = new ContentAssistant();

		//set black and white, instead of the usual ugly black on yellow
		//  da: if this is a standard Eclipse UI thing (and configured under
		//  general preferences) we ought to leave it as it is.  But it
		//  maybe isn't so standard?
		assistant.setProposalSelectorBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		assistant.setProposalSelectorForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));

		String[] types = PartitionScanner.LIGHT_PARSE_TYPES;
		//TODO be smarter about types
		for(int i=0; i<types.length; i++) {
			assistant.setContentAssistProcessor(new PGCompletionProcessor(sm), types[i]);
		}
		assistant.enableAutoActivation(true);
		IInformationControlCreator icc = new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				//DefaultInformationControl.IInformationPresenter presenter;
				//return new AnnotationExpansionControl(parent, 0, null);   //not sure what this is
				UpdateableHTMLTextPresenter presenter = new ProverHTMLTextPresenter(sm);
				DefaultInformationControl control = new DefaultInformationControl(parent, presenter);
				presenter.setControl(control);
				return control;
			}
		};
		assistant.setInformationControlCreator(icc);
		assistant.setAutoActivationDelay(10);  //500ms, but we want info to appear very quickly
		//TODO is there any way to let F2 make popup help (or this help) persistent, then user could drag it and keep typing
		assistant.setProposalPopupOrientation(
				ContentAssistant.PROPOSAL_STACKED
				//ContentAssistant.CONTEXT_INFO_BELOW  //not one of the choices, -AH 2005-12-01
				);
		assistant.setContextInformationPopupOrientation(ContentAssistant.CONTEXT_INFO_BELOW);
		//assistant.setContextInformationPopupBackground(
		//		ProofGeneralPlugin.getDefault().getColorRegistry().get("contextInfoBgColor"));

		return assistant;
	}

	@Override
    public ITextDoubleClickStrategy getDoubleClickStrategy(
			ISourceViewer sourceViewer, String contentType) {
		return new DefaultTextDoubleClickStrategy();
	}



	/* (non-Javadoc)
	 * Method declared on SourceViewerConfiguration
	 */
	@Override
    public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {

		PresentationReconciler reconciler= new PresentationReconciler();
		reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		// colour commands in just 1 colour (or could create a new code scanner for them)
		String[] types = PartitionScanner.LIGHT_PARSE_TYPES;
		for(int i=0; i<types.length; i++) {
			oneColourSyntaxHighlight(reconciler,types[i],doc);
		}

		return reconciler;
	}

	/**
	 * Q: Do we need a set of these per document, or could a singleton set be shared?
	 * Creates syntax highlighter for locked, processed, nextstep and normal versions of the token
	 * @param reconciler
	 * @param tokenName
	 */
	public void oneColourSyntaxHighlight(PresentationReconciler reconciler, String tokenName, ProofScriptDocument doc) {
		assert doc != null : "Must have a document";
		DefaultDamagerRepairer dr;
		ITokenScanner scanner;
		if (syntax.subType(tokenName,PGIPSyntax.PGTAG.name)) {
		    scanner = new SpecialStyleScanner(doc, tokenName);
		} else if (syntax.subType(tokenName,PGIPSyntax.COMMENT)) {
		    scanner = new SingleTokenScanner(doc, tokenName);
		} else {
		    scanner = new PGCodeScanner(doc);
		}
		dr = new DefaultDamagerRepairer(scanner);
		reconciler.setDamager(dr, tokenName);
		reconciler.setRepairer(dr, tokenName);
	}

	/**
	 * Single token scanner.
	 * Returns only the given token for a partition??
	 */
	static class SingleTokenScanner extends PGCodeScanner {
		public SingleTokenScanner(ProofScriptDocument doc, String tokenName) {
		    super(doc);
		    Token one = makeToken(tokenName);
			setDefaultReturnToken(one);
			setRules(new IRule[0]);
		}
	}

	/**
	 * scanner to draw italic text (e.g. for comments)
	 */
	static class SpecialStyleScanner extends PGCodeScanner {
		public SpecialStyleScanner(ProofScriptDocument doc, String tokenName) {
		    super(doc);
		    Token one = makeToken(tokenName,SWT.ITALIC);
			setDefaultReturnToken(one);
			setRules(new IRule[0]);
		}
	}

	@Override
    public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
		return sm.hoverHelp;
	}

	@Override
    public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		return new PGAnnotationHover();
	}


	//not needed (funny diff classes IInfoPres and DefInfoControl.IInfoPres
//	/**
//	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getInformationPresenter(org.eclipse.jface.text.source.ISourceViewer)
//	 */
//	public IInformationPresenter getInformationPresenter(
//			ISourceViewer sourceViewer) {
//		return new HTMLTextPresenter();
//		// TODO fill in method (autogenerated)
//		return super.getInformationPresenter(sourceViewer);
//	}

	/**
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getInformationControlCreator(org.eclipse.jface.text.source.ISourceViewer)
	 */
	@Override
    public IInformationControlCreator getInformationControlCreator(
			ISourceViewer sourceViewer) {
		//loads the same as the default but with HTML support
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				//DefaultInformationControl.IInformationPresenter presenter;
				//return new AnnotationExpansionControl(parent, 0, null);
				ProverHTMLTextPresenter presenter = new ProverHTMLTextPresenter(sm);
				DefaultInformationControl control = new DefaultInformationControl(parent, presenter);
				presenter.setControl(control);
				return control;
			}
		};
	}

	/** an HTMLTextPresenter with a changeText method -- you must set the control on this before use */
	public static class UpdateableHTMLTextPresenter extends HTMLTextPresenter {

		/** the control we are connected to, if any */
		protected DefaultInformationControl control = null;

		public void setControl(DefaultInformationControl control) {
			this.control = control;
		}

		String currentId = "";

		/** changes the text to newMessage if the current id of the presenter is 'id' */
		public void changeText(final String id, final String newMessage) {
			if (currentId.equals(id)) {
				new RunnableWithParams(null, "delayed-hover") {
					public void run() {
						if (currentId.equals(id)) {
							try {
								control.setInformation(newMessage);
								Point p = control.computeSizeHint();
								Rectangle r = control.getBounds();
								if ((r.width<p.x || r.height<p.y) && (p.x<600 && p.y < 600)) {
									//need to resize
									control.setSize(p.x, p.y);
								}
							} catch (Exception e) {}
						}
					}
				}.callDefaultDisplayAsyncExec();
			}
		}

		/**
		 * @see org.eclipse.jface.text.DefaultInformationControl.IInformationPresenter#updatePresentation(org.eclipse.swt.widgets.Display, java.lang.String, org.eclipse.jface.text.TextPresentation, int, int)
		 */
		// FIXME: this is deprecated in super class
		@Override
        public String updatePresentation(final Display display, final String hoverInfo,
				final TextPresentation presentation, final int maxWidth, final int maxHeight) {
			currentId = StringManipulation.makeRandomId(6);
			return super.updatePresentation(display, hoverInfo, presentation,
					maxWidth, maxHeight);
		}
	}

	/** a presenter which will attempt to look up anything that starts with "SEARCH::" */
	public class ProverHTMLTextPresenter extends UpdateableHTMLTextPresenter {

		// FIXME: this field might be removable in favour of field from super,
		// now this class has been made static to get at the document.
		// Maybe sm will vary over time (& field in super should be updated: TODO: check)
		// But doc should be fixed.
		/**
		 * @param sm the session manager
		 */
		public ProverHTMLTextPresenter(SessionManager sm) {
			this.sm = sm;
		}
		SessionManager sm;

		@Override
        public String updatePresentation(final Display display, final String hoverItem,
				final TextPresentation presentation, final int maxWidth, final int maxHeight) {
			if (hoverItem==null) {
				return "";
			}
			if (hoverItem.equals(PGTextHover.CURRENTSTATE_HOVERINFO)) {
						String proverState = "";
						//final MutableObject thisId = new MutableObject(null);
						if (LatestOutputView.isCreated()) {
							proverState = LatestOutputView.getDefault().getPlainOutput();
							//	String result = super.updatePresentation(display, proverState, presentation, maxWidth, maxHeight);
							//	thisId.set(currentId);
							return proverState;
						}
			}
			if (hoverItem.equals(PGTextHover.NEXTSTEPSTATE_HOVERINFO)) {
				if (ProofGeneralPlugin.debug(this)) {
					// debug mode only
					System.err.println("TEST: NEXT STATE REQUESTED!");
					// TODO: replace this code with a method from the SM/document
					// TODO: check that the next command is still the one that the 
					// hover was over, otherwise back off
					DocElement command = doc.findNext(PGIPSyntax.COMMAND, doc.getProcessedOffset());
					//try {
					if (command != null) {
						//sm.queueCommand(command, this);
						//doc.commandSent(command);
						return "Next state will be here, to send: " + command.getStringValue();
						// TODO: wait for response in workbench, then return state.
						// 	Could do get command response, but that doesn't sync with document.
						// TODO: indicate in document this is a temporary step
						// TODO: find out how to undo when mouse leaves hover
						// OR: we always stay one step ahead of the user, but colour the
						// document as if we haven't, and cache the next proof state!!
						// [Notable] Disadvantage: next step may be expensive, hogging prover
						// so preventing exploration of definitions, etc.  What we could
						// do with is a prover-supplied heuristic for the cost of the next
						// state.
					//} catch (ScriptingException ex) {
						// Prover is probably unavailable.  Don't display the hover.
					}
						return "";
				}
				return ""; // not yet working, don't do it.
			}
			if (hoverItem.startsWith("SEARCH::")) {
				final String item = hoverItem.substring(8);
				final MutableObject thisId = new MutableObject(null);
				String helpString = sm.hoverHelp.getHelpString(item, null,
						new Runnable() {
					public void run() {
						//callback to this if getHelpString needs to load
						changeText((String)thisId.get(), sm.hoverHelp.getHelpString(item, null, null));
					}
				});
				String result = super.updatePresentation(display, helpString, presentation, maxWidth, maxHeight);
				thisId.set(currentId);
				return result;
			}
			return super.updatePresentation(display, hoverItem, presentation, maxWidth, maxHeight);
		}
	}
	}
