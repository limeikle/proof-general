/*
 *  $RCSfile: IdView.java,v $
 *
 *  Created on 11 Dec 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.views;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.dom4j.Element;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;

import ed.inf.proofgeneral.document.CmdElement;
import ed.inf.proofgeneral.editor.PGTextHover;
import ed.inf.proofgeneral.editor.ProofScriptEditor;
import ed.inf.proofgeneral.editor.actions.retargeted.PGRetargetableAction;
import ed.inf.proofgeneral.sessionmanager.NameSpace;
import ed.inf.proofgeneral.sessionmanager.PGIPSyntax;
import ed.inf.proofgeneral.sessionmanager.ProverSyntax;
import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.proofgeneral.sessionmanager.ProverSyntax.ISyntaxChangeListener;
import ed.inf.proofgeneral.sessionmanager.events.IPGIPListener;
import ed.inf.proofgeneral.sessionmanager.events.PGIPError;
import ed.inf.proofgeneral.sessionmanager.events.PGIPEvent;
import ed.inf.proofgeneral.sessionmanager.events.PGIPIncoming;
import ed.inf.proofgeneral.sessionmanager.events.PGIPReady;
import ed.inf.proofgeneral.ui.theme.PGColors.PGColor;
import ed.inf.proofgeneral.views.output.HTMLTextLabel;
import ed.inf.utils.datastruct.StringManipulation;
import ed.inf.utils.eclipse.DisplayCallable;

/**
 * A view to display identifiers and keywords.
 * This view updates the ProverSyntax lists of identifiers.
 * @author Daniel Winterstein
 * @author David Aspinall
 */
// See TRAC#12, TRAC#13
// TODO:  remove use of PGAction

public class IdView extends ViewPart implements ISyntaxChangeListener {


	protected static final String ANY_THEORY = "all theories";
	//protected static final String ANY_TYPE = "any type";  // TODO: we need to record the type from response

	public static final String OBJTYPE_THEORY = "theory";
	public static final String OBJTYPE_THEOREM = "theorem";
	public static final String OBJTYPE_KEYWORD = "keywords";  // Isabelle specific, belongs in subclass
	
	// Messages
	private static final String QUERY_MESSAGE = "Querying prover...";
	private static final String NO_PROVER_MESSAGE = "Prover not available...";
	private static final String NO_IDENTIFIERS_MESSAGE = "No identifiers found.";



    // TODO: pick up a list of types from the prover syntax (and update objTypes)
	static final String[] objTypes = {OBJTYPE_THEORY, OBJTYPE_THEOREM,OBJTYPE_KEYWORD};


	/*
	 * The model for this viewer.
	 *
	 * INVARIANT: theory != ANY_THEORY ==> theory + "." + theories[i] is an
	 *                                     absolute path to a nested theory,
	 *                                 and theory + "." + allitems[i] similarly
	 *            theory == ANY_THEORY ==> theories[i], allitems[i] are absolute paths
	 *
	 */
	// NB: following fields not private for speedy access in PGAction class
	// NB2: if we made elements for these we could gather into a list.
	/** The list of object identifiers actually displayed. Non-null. */
	protected String[] items = {};
	/** The theories available at the moment. */
	protected String[] theories =  { };

	/** The selected type.  Non-null. */
	protected String type = OBJTYPE_THEOREM;
	/** The selected theory.  Non-null. */
	protected String theory = ANY_THEORY;
	/** The name pattern to match or null for any (no pattern) */
	protected volatile Pattern pattern = null;
	/** The (first) selected keyword, or null if none.  Used only while looking up selection. */
	protected String keyword = null;
	/** The list of all items from which to filter.  Non-null. */
	protected String[] allitems = {};

	/** Lock to prevent concurrent access to this viewer's model */
	protected Object modelLock = new Object();

	/*
	 * The presentation widgets.  Initial state set in createPartControl
	 * should match initial state of model.
	 */
	protected org.eclipse.swt.widgets.List idList;
	//protected Button button;
	protected Combo typeCombo;
	protected Combo theoryCombo;
	protected SashForm sash;
	protected Text patternText;
	protected HTMLTextLabel desc;
	private final int[] sashWeights = new int[] {3,1};
	private boolean initialised = false;
	
	/**
	 * @return true if we're initialised and none of our widgets has been disposed
	 */
	private boolean isWidgetAvailable() {
		return !(!initialised ||
			    idList.isDisposed() || typeCombo.isDisposed() || 
			    theoryCombo.isDisposed() || sash.isDisposed() || patternText.isDisposed());
	}

	/** The session manager we last connected to. */
	protected volatile SessionManager sessionManager;
	
	/** The syntax whose lexicon we are investigating.  */
	protected volatile ProverSyntax syntax;

	
	/**
	 * A timer for running view updates after a while
	 */
	private final Timer idViewTimer = new Timer("IdView update timer");
	private TimerTask idViewTask = null;
	
	/** Flag indicating a new query has been issued so we should stop updating the view. */
	private volatile boolean taskCancelled;


	/**
	 * Initialise the dialog elements from the model
	 */
	private void setDialogFromModel() {
		if (!isWidgetAvailable()) {
			return;
		}
		synchronized (modelLock) {  // ensure a consistent view of model
			idList.setItems(items);
			typeCombo.setText(type);
			theoryCombo.setItems(theories);
			theoryCombo.add(ANY_THEORY);
			theoryCombo.setText(theory);
			patternText.setText(pattern == null ? "" : StringManipulation.regexp2glob(pattern.pattern()));
		}
	}
	
	/** The lookup action for this view */
	private LookUpAction lookUpAction;
	
	/**
	 * Create the view, setting dialog contents from model.
	 */
	@Override
    public void createPartControl(Composite parent) {
		
		ProofScriptEditor pse = ProofScriptEditor.getActiveProofScriptEditor();
		lookUpAction = new LookUpAction(pse);
		
		Layout layout = new GridLayout(1,false);
		parent.setLayout(layout);
		parent.setLayoutData (new GridData (GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));

		Composite grp = new Composite(parent,SWT.NORMAL);
		GridLayout grpLayout = new GridLayout ();
		grp.setLayout(grpLayout);
		grpLayout.numColumns = 3;
		grp.setLayoutData (new GridData (GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));

		Label tl = new Label(grp,SWT.NORMAL);
		tl.setText("Type:");
		typeCombo = new Combo(grp,SWT.DROP_DOWN);
		GridData gd = new GridData (GridData.GRAB_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		typeCombo.setLayoutData(gd);
		typeCombo.setItems(objTypes);
		typeCombo.setToolTipText("Type of objects to list");
		typeCombo.addSelectionListener(
				new SelectionListener() {
					public void widgetDefaultSelected(SelectionEvent e) {
					}
					public void widgetSelected(SelectionEvent e) {
						lookUpAction.run();
					}
				});

		Label l2 = new Label(grp,SWT.NORMAL);
		l2.setText("Theory:");
		theoryCombo = new Combo(grp,SWT.DROP_DOWN);
		GridData gd2 = new GridData (GridData.GRAB_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
		gd2.horizontalSpan = 2;
		theoryCombo.setLayoutData(gd2);
		// da: toolTips over text boxes are annoying
		//theoryCombo.setToolTipText("Find objects in this theory");
		theoryCombo.addSelectionListener(new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {
				}
				public void widgetSelected(SelectionEvent e) {
					if (theoryCombo.getText().equals(ANY_THEORY)) {
						// Clear the pattern matching when any theory is selected.
						pattern = null;
					}
					lookUpAction.run();
				}
			});

		Label l3 = new Label(grp,SWT.NORMAL);
		l3.setText("Matching:");
// alternative using new 3.3 SWT.SEARCH L&F.  On platforms without cancel
// button we'd need to add it manually (see Snippet258).
//		patternText = new Text(grp,SWT.LEFT | SWT.SEARCH | SWT.CANCEL);
		patternText = new Text(grp,SWT.LEFT | SWT.SINGLE);
		patternText.setLayoutData(new GridData (GridData.GRAB_HORIZONTAL |
				GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));
		// da: toolTips over text boxes are annoying
		//patternText.setToolTipText("Search regular expression");
//		patternText.addSelectionListener(new SelectionAdapter() {
//			@Override
//            public void widgetDefaultSelected(SelectionEvent e) {
//				if (idViewTask != null) {
//					idViewTask.cancel();
//				}
//				if (e.detail == SWT.CANCEL) {
//					System.out.println("Search cancelled");
//				} else {
//					updatePattern();
//					idViewTask = new IdViewUpdateTimerTask();
//					idViewTimer.schedule(idViewTask, 250);
//				}
//			}
//		});
		patternText.addModifyListener(
				new ModifyListener() {
					public void modifyText(ModifyEvent e) {
								updatePattern();
								if (idViewTask != null) {
									idViewTask.cancel();
								}
								idViewTask = new IdViewUpdateTimerTask();
								idViewTimer.schedule(idViewTask, 250);
					}
				});


		sash = new SashForm(parent, SWT.VERTICAL);
		sash.setLayoutData (new GridData (SWT.FILL, SWT.FILL, true, true));

		idList = new org.eclipse.swt.widgets.List(sash,SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
		idList.setLayoutData (new GridData (SWT.FILL, SWT.FILL, true, true));
		idList.addMouseListener(new MouseAdapter() {
			@Override
            public void mouseDoubleClick(MouseEvent e) {
				lookupNested();  // TODO: or better, go to source
			}
		});
		idList.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
			}
			public void keyReleased(KeyEvent e) {
				if (e.character=='\n' || e.character=='\r') {
					crudeCopyNPaste();
				}
			}
		});

		desc = new HTMLTextLabel(sash);

		sash.setWeights(sashWeights);

		idList.addSelectionListener(new SelectionAdapter() {
			@Override
            public void widgetSelected(SelectionEvent e) {
				String[] texts = idList.getSelection();
				if (texts.length==0) {
					desc.setText("");
					return;
				}
				String ourkeyword;
				synchronized (modelLock) {
					// Use the model invariant to construct a fully-qualified name to lookup
					if (theory.equals(ANY_THEORY)) {
						ourkeyword = texts[0];
					} else {
						ourkeyword = theory + syntax.getTheory(NameSpace.GLOBAL).delimiter + texts[0];
					}
					keyword = ourkeyword;
				}
				setDescriptionFor(ourkeyword);
			}
		});

		setDialogFromModel();

		initialised = true;
		
		// Update the view a wee while after startup, to not delay appearance of view
		idViewTask = new LookUpActionTimerTask();
		idViewTimer.schedule(idViewTask, 2000);
	}

	/**
	 * Initialise ourselves from a previously saved memento.  We set the model elements
	 * and view configuration parameters accordingly.  They then get copied into the view
	 * in {@link #createPartControl(Composite)}.
	 * 
	 * @see org.eclipse.ui.part.ViewPart#init(org.eclipse.ui.IViewSite, org.eclipse.ui.IMemento)
	 */
	@SuppressWarnings("boxing")
    @Override
    public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site,memento);
		if (memento != null) {
			Integer sash0 = memento.getInteger("sash0");
			Integer sash1 = memento.getInteger("sash1");
			if (sash0 != null) {
				sashWeights[0] = sash0;
				}
			if (sash1 != null) {
				sashWeights[1] = sash1;
			}
			queryMemento = memento;
		}
	}
	
	/** Temporary copy of view memento used during startup. */
	IMemento queryMemento;
	
	/**
	 * Restore a remembered query in the view.  We do this after the
	 * master query has run, so that the list of theories is set correctly.
	 * This is not ideal and gives display glitches.
	 */
	private void restoreQueryMemento() {
		if (queryMemento != null) {
			String mtheory = queryMemento.getString("theory");
			String mtype = queryMemento.getString("type");
			String mpattern = queryMemento.getString("pattern");
			synchronized (modelLock) {
				if (mtheory != null) {
					theory = mtheory;
				}
				if (mtype != null) {
					type = mtype;
				}
				if (mpattern != null) {
					try {
						pattern = Pattern.compile(mpattern);
					} catch (PatternSyntaxException e) {
						pattern = null;
					}
				}
				setDialogFromModel(); // triggers an update of view
			}
			queryMemento = null;  // we're done with it
		}
	}
		
		
	/**
	 * Save some state for this view from the model.  We save the elements
	 * used to lookup, rather than the results of the lookup.
	 * @see org.eclipse.ui.part.ViewPart#saveState(org.eclipse.ui.IMemento)
	 */
	@Override
    public void saveState(IMemento memento) {
		super.saveState(memento);
		memento.putString("theory",theory);
		memento.putString("type",type);
		if (pattern != null) {
			memento.putString("pattern",pattern.pattern());
		}
		memento.putInteger("sash0",sash.getWeights()[0]);
		memento.putInteger("sash1",sash.getWeights()[1]);
	}


	/**
	 * Set the description box for the given keyword (non-null)
	 * @param somekeyword
	 */
	private void setDescriptionFor(final String somekeyword) {
		String info = "";
		if (sessionManager != null && sessionManager.hoverHelp != null) {
			Runnable callback =
				new Runnable() {
				public void run() {
					boolean doit;
					synchronized (modelLock) {
						doit = (keyword == somekeyword); // ES: intended
					}
					if (doit) {
						Display.getDefault().asyncExec(new Runnable() {
						//new DisplayCallable("IdView.setDescriptionFor") {
							public void run() {
								String info = sessionManager.hoverHelp.getHelpString(somekeyword, null, null);
								desc.setText(info);
								//keyword = null; // indicate done
							}
						}); // .runDisplay();
					}
				};
			};
			info = sessionManager.hoverHelp.getHelpString(somekeyword, null, callback);
		}
		if (type.equals(OBJTYPE_KEYWORD) && info.equals("")) {
			// statically as fall back
			info = PGTextHover.getKeywordInfo(somekeyword,syntax);
		}
		desc.setText(info);
	}

	/**
	 * Paste the current value into the currently active editor.
	 */
	void crudeCopyNPaste() {
		Clipboard clipboard = null;
		try {
			// NB: other kinds of editor would be OK to paste into, but ProofScriptEditor
			// exposes getDisplay for us to do this.  Would be better to rerrange.
			ProofScriptEditor editor = ProofScriptEditor.getActiveProofScriptEditor();
			if (editor==null) {
				return;
			}
			String[] texts = idList.getSelection();
			String text="";
			for(int i=0; i<texts.length; i++) {
				if (i>0) {
					text += ", ";
				}
				text += texts[i];
			}
			TextTransfer transfer = TextTransfer.getInstance();
			clipboard = new Clipboard(editor.getDisplay());
			clipboard.setContents(new Object[]{text},new Transfer[]{transfer});
			IAction paste = editor.getAction(ITextEditorActionConstants.PASTE);
			paste.run();
		} catch (Exception x) {
			x.printStackTrace();
		} finally {
			if (clipboard != null) {
				clipboard.dispose();
			}
		}
	}

	/**
	 * Lookup a selected item within the currently selected theory.
	 * Action run from display thread.
	 */
	synchronized void lookupNested() {
		synchronized (modelLock) {
//			boolean oldIgnoreComboChanges = ignoreComboChanges;
//			ignoreComboChanges = true;
			String[] texts = idList.getSelection();
			if ("theory".equals(typeCombo.getText())) {
				if (texts.length > 0 && texts[0].length() > 0) {
					theory = texts[0];
					theoryCombo.setText(texts[0]); // not necessarily in theories
					lookUpAction.run();
				}
			}
//			ignoreComboChanges = oldIgnoreComboChanges;
		}
	}


	/**
	 * Does nothing.
	 * @see org.eclipse.ui.IWorkbenchPart#setFocus()
	 */
	@Override
    public void setFocus() {
	}

	/**
	 * Add ourselves as a listener to the given ProverSyntax object, removing ourselves
	 * from the previous syntax if one was set.
	 * @param syntax The syntax to set.
	 */
	// NB: if the syntax would update/extend itself as we query, we could use that directly.
	public void setSyntax(ProverSyntax syntax) {
		if (this.syntax == syntax) {
			return;
		}
		if (this.syntax != null) {
			this.syntax.removeSyntaxChangeListener(this);
		}
		this.syntax = syntax; // volatile: this is only write
		if (syntax!=null) {
			syntax.addSyntaxChangeListener(this);
		}
	}


	/**
	 * Update the model's list of available theories.
	 * Should be called with modelLock is held.
	 */
	private void updateTheories() {
		if (syntax != null) {
			boolean toplevel = theory.equals(ANY_THEORY) || !syntax.hasTheory(theory);
			NameSpace thy = toplevel ? syntax.getTheory(NameSpace.GLOBAL) : syntax.getTheory(theory);
			theories = thy.getKeywordNames(ProverSyntax.TYPE_THEORY,toplevel,1);
			Arrays.sort(theories);
			if (toplevel) {
				theory = ANY_THEORY;  // normalise in case of invalid value
			}
		} else {
			theories = new String[] { ANY_THEORY };
		}
	}

	/**
	 * Update the view in the display thread.  The view is updated with the model
	 * contents for the list of items displayed and the theories available in
	 * the drop-down theory selection.
	 */
	private void updateView() {
		new DisplayCallable("IdView.pgipEvent") {
			@Override
            public Object run() {
				synchronized (modelLock) {
					idList.setItems(items);
					theoryCombo.setItems(theories);
					theoryCombo.add(ANY_THEORY);
					theoryCombo.setText(theory);
				}
				theoryCombo.update();
				idList.update();
				return null;
			}
		}.runDisplay();
	}

	private class IdViewUpdateTimerTask extends TimerTask {
		@Override
        public void run() {
			if (!isWidgetAvailable()) {
				updateView();
			}
		}
	}

	/**
	 * Update the subselection of matching items using the current pattern.
	 * Model-only change.
	 */
	private void updateMatching () {
		final String[] allitemscopy;
		synchronized (modelLock) {
			if (pattern == null || pattern.toString().equals("")) {
				items = allitems;
				return;
			}
			// Allow items to be updated while we're matching previous version
			allitemscopy = allitems.clone();
			taskCancelled = false;
		}
		final ArrayList<String> matcheditems = new ArrayList<String>(allitems.length/5+10);
		if (Display.getCurrent() == null) {
			for (String name : allitemscopy) {
				Matcher m = pattern.matcher(name);
				if (m.matches()) {
					matcheditems.add(name);
				}
				if (taskCancelled) {
					break;
				}
			}
		} else {
			IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
			try {
				progressService.busyCursorWhile(new IRunnableWithProgress(){
					public void run(IProgressMonitor monitor) {
						for (String name : allitemscopy) {
							Matcher m = pattern.matcher(name);
							if (m.matches()) {
								matcheditems.add(name);
							}
							if (taskCancelled || monitor.isCanceled()) {
								break;
							}
						}
					}});
			} catch (InterruptedException e) {
				// Do nothing
			} catch (InvocationTargetException e) {
				// Do nothing
			};
		}
		synchronized (modelLock) {
			if (!taskCancelled) {
				items = matcheditems.toArray(new String[matcheditems.size()]);
			}
		}
	}

	/**
	 * Update the current pattern
	 */
	private void updatePattern () {
		String patstr = patternText.getText();
		try {
			synchronized (modelLock) {
				pattern = Pattern.compile(StringManipulation.glob2regexp(patstr));
				patternText.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
				updateMatching(); // FIXME think about where to put this update
			}
		} catch (PatternSyntaxException e) {
			pattern = null;
			patternText.setBackground(PGColor.ERRORBG.get());
		}
	}

	private class LookUpAction extends PGRetargetableAction {

        public LookUpAction(ProofScriptEditor editor) {
	        super(editor);
        }

		/** The main lookup action, run in the display thread. */
		@Override
        public void run() {
			if (isBusy()) {
				return;
			}
			setBusy();

			SessionManager newsm = getSessionManagerForRunningAction();

			if (newsm == null && sessionManager == null) {
				idList.setItems(new String[] { NO_PROVER_MESSAGE });
				clearBusy();
				return;
			}
			if (newsm != sessionManager && newsm != null) {
				if (sessionManager != null) {
					sessionManager.removeListener(idViewPGIPListener);
				}
				sessionManager = newsm; // This is only place our sm is set
				setSyntax(sessionManager.proverInfo.proverSyntax);
				sessionManager.addListener(idViewPGIPListener);
			}
			
			if (!sessionManager.getProverState().isAvailable()) {
				clearBusy();
				idList.setItems(new String[] { NO_PROVER_MESSAGE });
				return;
			}

			CmdElement cmd = new CmdElement(PGIPSyntax.ASKIDS);

			synchronized (modelLock) {
				type = typeCombo.getText().trim();

				if (type.equals(OBJTYPE_KEYWORD)) {
					// no need to query
					items = syntax.getKeywords(ProverSyntax.TYPE_CORE,false);
				    allitems = items;
					idList.setItems(items);
					return;
				}
				cmd.setAttributeValue("objtype",type);

				theory = theoryCombo.getText().trim();
				if (theory.equalsIgnoreCase(ANY_THEORY) || StringManipulation.isWhitespace(theory)) {
					theory = ANY_THEORY; // normalise
				} else {
					cmd.setAttributeValue("thyname",theory);
				}
				taskCancelled = true; // cancel currently running task
			}

			idList.setItems(new String[] { QUERY_MESSAGE });
//			try {
				sessionManager.queueCommand(cmd,this);
//			} catch (ScriptingException e) {
//				idList.setItems(new String[] { NO_PROVER_MESSAGE });
//			}
		}
		
		private final IPGIPListener idViewPGIPListener = new IPGIPListener() { 
			@SuppressWarnings("unchecked")
	        public void pgipEvent(PGIPEvent event) {
				if (event.cause == this &&
						(event instanceof PGIPIncoming || event instanceof PGIPError) &&
						!(event instanceof PGIPReady)) {
					// NB da: this assumes no asynchrony from prover; it also wrongly assumes
					// that every askids will result in a single setids response and the
					// context will match exactly the one given.
					// I've tuned Isabelle to match this behaviour for now, but this needs
					// improvement.  We should have a listener in the ProverSyntax, really,
					// and track relevant updates here in this view.
					if (event instanceof PGIPError) {
						synchronized(modelLock) {
							items = new String[] {NO_IDENTIFIERS_MESSAGE};
							allitems = new String[0];
						}
						updateView();
						clearBusy();
						return;
					}
					List<Element> nids = event.parseTree.selectNodes("//identifier");
					if (nids == null || nids.size()==0) {
						// Sloppily assume that this was a setids response, but no identifiers
						// were returned.
						synchronized(modelLock) {
							items = new String[] {NO_IDENTIFIERS_MESSAGE};
							allitems = new String[0];
						}
						updateView();
						clearBusy();
						return;
					}
					String[] newallitems = new String[nids.size()];
					int i = 0;
					for(Element nid : nids) {
						newallitems[i++] = nid.getText();
					}
					Arrays.sort(newallitems);
					String newtype;
					NameSpace newthy;
					synchronized (modelLock) {
						newtype = type; // FIXME: this is wrong, could have been updated since query performed
						// instead we should get type field from response.
						allitems = newallitems;
						if (theory != ANY_THEORY && syntax.hasTheory(theory)) {
							newthy = syntax.getTheory(theory);
						} else {
							newthy = syntax.getTheory(NameSpace.GLOBAL);
						}
					}
					// Now set our cache of keywords
					// NB: this matches Isabelle CVS 31.1.07 (unqualified names returned)
					newthy.setKeywords(newtype,newallitems,false);
					synchronized (modelLock) {
						if (theories.length <= 1 || newtype.equals(OBJTYPE_THEORY)) {
							updateTheories();
						}
						updateMatching();
					}
					updateView();
					clearBusy();
				}
			}
		};
	};

	private class LookUpActionTimerTask extends TimerTask {
		@Override
        public void run() {
			new DisplayCallable("IdView.LookUpActionTimerTask") {
				@Override
                public Object run() {
					if (isWidgetAvailable()) {  
						lookUpAction.run();
						restoreQueryMemento();
					}
					return null;
				}
			}.runDisplay();
		}
	}

	/**
	 * Updates view on reception of new information.
	 * @see ed.inf.proofgeneral.sessionmanager.events.IPGIPListener#pgipEvent(ed.inf.proofgeneral.sessionmanager.events.PGIPEvent)
	 */
	public void syntaxChangeEvent() {
		// TODO: do updating as appropriate, maybe track Prover Knowledge's investigation?
	}

}
