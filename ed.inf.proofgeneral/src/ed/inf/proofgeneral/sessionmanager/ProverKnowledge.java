/*
 *  $RCSfile: ProverKnowledge.java,v $
 *
 *  Created on 27 Apr 2005 by Alex Heneveld
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.sessionmanager;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.CmdElement;
import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.document.ProofScriptMarkers;
import ed.inf.proofgeneral.editor.PGTextHover;
import ed.inf.proofgeneral.editor.actions.torefactor.GetCommandResponseAction;
import ed.inf.proofgeneral.editor.actions.torefactor.GetCommandResponseAction.Session;
import ed.inf.proofgeneral.pgip2html.Converter;
import ed.inf.proofgeneral.preferences.PreferenceNames;
import ed.inf.proofgeneral.sessionmanager.events.IPGIPListener;
import ed.inf.proofgeneral.sessionmanager.events.PGIPEvent;
import ed.inf.utils.MiscUtils;
import ed.inf.utils.datastruct.ArrayListOperations;
import ed.inf.utils.datastruct.MutableInteger;
import ed.inf.utils.datastruct.NumericStringUtils;
import ed.inf.utils.datastruct.StringManipulation;
import ed.inf.utils.process.PooledRunnable;

/**
 * ProverKnowledge
 * <p/>
 * Class for keeping track (on the java side) of
 * the knowledge the prover has in the current state,
 * ie available rules and definitions.
 * <p/>
 * on <opentheory/> response:
 * - refreshes theory list
 * - refreshes path
 * <p/>
 * then query list axioms and lemmas in each file,
 * then query individual axioms and lemmas;
 * <p/>
 * offer command to "clear cache" of knowledge:
 * - for data not from any heap [applies to all heaps]
 * - for data not from current heap [applies only to current heap]
 * - for all data
 * <p/>
 *
 */
public class ProverKnowledge implements IPGIPListener {

	// CLEANUP
	//SessionManager sm;
	//ProverState proverState;  da: this doesn't seem to be used, can remove?
	//PGIPSyntax syntax;
	Converter converter;

	public ProverKnowledge(//SessionManager sm, ProverState proverState,
			//PGIPSyntax syntax, 
			Converter converter) {
		// da: unneeded fields (seem to grab the active session manager
		// below instead), CLEANUP
		//this.sm = sm;
		//this.proverState = proverState;
		//this.syntax = syntax;
		this.converter = converter;  // needed for rendering text
	}


	//seems never used -AH
	//List allItems = new ArrayList();

	//public void add()

	public interface IKnowledgeItem {
		public String getId();
		public ArrayList<String> getNames();
		public String getType();
		public TheoryFile getTheory();
		public String getStatement();
//	TODO maybe want a fancier type for HTML statements (eg Element)
		public String getStatementHtml();
	}

	public interface ILazyKnowledgeItem extends IKnowledgeItem {
		public int loadFullyBg(final int numTries, final Runnable callback);
		public void loadFullyFg(int numTries);
	}


	public class KnowledgeItem implements IKnowledgeItem {
		public String type;
		/** the id by which the theorem prover refers to this; often same as name, perhaps qualified, perhaps with _def appended */
		public String id;
		public ArrayList<String> names;
		public TheoryFile theory;
		public IMarker marker = null;
		Element statementElement;
		String statementHtml;
		String statement;

		public KnowledgeItem(String id, String type, String name, TheoryFile theory,
				String statement) {
			this.id = id;
			this.type = type;
			this.names = new ArrayList<String>();
			if (name!=null) {
				names.add(name);
			}
			this.theory = theory;
			this.statement = statement;
		}

		public void setStatements(Element statement) {
			statementElement = statement; // UrF: unread field??
			this.statement = statement.getStringValue();
			statementHtml = converter.getDisplayText(statement);
		}
		public void setStatementsVal(Element statement) {
			statementElement = statement;
			this.statement = statement.getStringValue();
			statementHtml = converter.getDisplayText(statement);
		}

		public String getId() {
			return id;
		}
		public ArrayList<String> getNames() {
			return names;
		}
		public boolean hasName() {
			return names.size() > 0;
		}
		public String getType() {
			return type;
		}
		public TheoryFile getTheory() {
			return theory;
		}
		public String getStatement() {
			return statement;
		}
		public String getStatementHtml() {
			if (statementHtml!=null) {
				return statementHtml;
			}
			if (statement!=null) {
				return converter.getDisplayHtml(statement); // was: Converter.stringToXml(statement);
			}
			return null;
		}
	}

	public class TheoryFile extends KnowledgeItem {
		/** full path to the theory, including filename; null means unknown */
		public String file;
		public String getFile() {
			return file;
		}
		/** theory files usually only have one name */
		public String getName() {
			return names.get(0);
		}

		/** whether we are this theory file is the current edit/parse used by the SessionManager
		 *  (ie, whether it is "open" in the prover) */
		public boolean isCurrent = false;

		public TheoryFile(String id, String name, String file) {
			super(id, "theory", name, null, "theory "+name);
			theory = this;
			this.file = file;
		}
	}

	/** a theorem, lemma, axiom, or constant, stored in a theory file */
	public class LazyTheoryItem extends KnowledgeItem implements ILazyKnowledgeItem {
		boolean loaded = false;
		public LazyTheoryItem(String id, String name, TheoryFile theory) {
			super(id, null, name, theory, null);
		}
		public LazyTheoryItem(String id, String type, String name, TheoryFile theory) {
			super(id, type, name, theory, null);
		}

		boolean loading = false;
		/** tries to load the object fully, calling callback (if not null) when done
		 * (ignores the callback if the object is already loaded fully)
		 *
		 * @param numTries number of times to try it (normally 1); if 0, tries immediately without waiting;
		 *   if 1 (or greater), waits for prover then tries, repeats that many times;
		 *  if -1, tries (waiting) an unlimited number of times
		 * @param callback Runnable to run when done (done on failure,
		 *   or if another thread is loading it, done when it finishes;
		 *   but not run if it information already loaded)
		 * @return -1 if it was already loaded, 1 if someone else is loading it;
		 *   0 if we are loading it in the background
		 */
		public int loadFullyBg(final int numTries, final Runnable callback) {
// da: DC, disable this for now:
//          if (loaded) { return -1; }
			if (!ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_GRAB_PROVER_KNOWLEDGE)) {
				return -1;
			}
			synchronized (LazyTheoryItem.this) {
				if (loaded) {
					return -1;
				}
				if (loading) {
					if (callback!=null) {
						new PooledRunnable() {
							public void run() {
								try {
									synchronized (LazyTheoryItem.this) {
										while (loading) {
											LazyTheoryItem.this.wait();
										}
									}
								} catch (InterruptedException e) {
								} finally {
									callback.run();
								}
							}
						}.start();
					}
					return -2;
				}
				loading = true;
			}
			new PooledRunnable() {
				public void run() {
					try {
						loadFullyFg(numTries);
					} finally {
						if (callback!=null) {
							callback.run();
						}
					}
				}
			}.start();
			return 0;
		}


		public void checkForMarker() {
			if (!ProofGeneralPlugin.isEclipseMode()) {
				return;
			}
			try {
				IMarker m = null;
				IResource r =null;
				//if (doc!=null) r = doc.resource;  //TODO do we need the doc's resource?
				r = ResourcesPlugin.getWorkspace().getRoot();
				m = PGTextHover.findMatchingMarker(r, names.get(0));  //TODO should maybe be ID ?
				if (m!=null) {
					m.setAttribute(ProofScriptMarkers.TOOLTIP, getStatementHtml());
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		public boolean isLoaded() {
			return loaded;
		}
		public void loadFullyFg(int numTries) {
			try {
				Element defn = null;
				int tries = 0;
				while (defn==null && tries<numTries) {
					CmdElement showid = new CmdElement(PGIPSyntax.SHOWID);
					showid.addAttribute("name", id);
					showid.addAttribute("objtype", "theorem"); // FIXME: objtype theorem
					if (numTries==0) {
						// TODO da: this should be a PGIP command:  <showid thyname="X" objtype="Y" name="Z"/>
						// The response will be <idvalue [same attrs]><pgmltext>....</pgmltext></idvalue>
						// We should cache idvalue responses automatically in the knowledge
						// by a listener for idvalues (the prover might send them when it feels like)
						// and use a callback/blocking call/one-time listener from command queue here.
						// See TRAC #37

						defn = GetCommandResponseAction.getDefault().doCommand(showid);
					} else {
						defn = GetCommandResponseAction.getDefault().doCommandWaiting(showid);
					}
					tries++;
				}
				if (defn!=null) {
					//loaded successfully
					synchronized (LazyTheoryItem.this) {
						setStatementsVal(defn);
						checkForMarker();
						loaded = true;
					}
				} else {
					//couldn't load-- if there's a callback, we could wait then try again, or print error?
				}
				//System.out.println("GOT defn for "+id+" as:\n"+defn.asXML());
			} catch (InterruptedException e) {
				// do nothing
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				synchronized (LazyTheoryItem.this) {
					loading = false;
					LazyTheoryItem.this.notifyAll();
				}
			}
		}
	}

	public class KnowledgeMultiItem extends LazyTheoryItem {
		IKnowledgeItem wrapped1;
		IKnowledgeItem wrapped2;
		public KnowledgeMultiItem(IKnowledgeItem wrapped1, IKnowledgeItem wrapped2) {
			super(wrapped1.getId(), null, wrapped1.getTheory());
			this.names.addAll(wrapped1.getNames());
			this.names.addAll(wrapped2.getNames());
			this.type = wrapped1.getType();
			this.wrapped1 = wrapped1;
			this.wrapped2 = wrapped2;
		}
		@Override
        public int loadFullyBg(final int numTries, final Runnable callback) {
			//if (loaded) {
			//	return -1;
			//}
			if (!ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_GRAB_PROVER_KNOWLEDGE)) {
				return -1;
			}
			boolean do1 = (wrapped1!=null && (wrapped1 instanceof LazyTheoryItem) && (!((LazyTheoryItem)wrapped1).loaded));
			boolean do2 = (wrapped2!=null && (wrapped2 instanceof LazyTheoryItem) && (!((LazyTheoryItem)wrapped2).loaded));
			if (!do1 && !do2) {
				synchronized (KnowledgeMultiItem.this) {
					loaded = true;
					return -1;
				}
			}
			if (do1 && !do2) {
				return ((LazyTheoryItem)wrapped1).loadFullyBg(numTries, new Runnable() {
					public void run() {
						combineAfterLoad(callback);
					}
				});
			}
			if (!do1 && do2) {
				return ((LazyTheoryItem)wrapped2).loadFullyBg(numTries, new Runnable() {
					public void run() {
						combineAfterLoad(callback);
					}
				});
			}
			//got to do both of 'em
			final MutableInteger numDone = new MutableInteger(0);
			Runnable comboCallback = new Runnable() {
				public void run() {
					synchronized (numDone) {
						numDone.inc();
						if (numDone.get()==2) {
							//done them both, set values and run callback
							combineAfterLoad(callback);
						}
					}
				}
			};
			return Math.max(((LazyTheoryItem)wrapped1).loadFullyBg(numTries, comboCallback),
					((LazyTheoryItem)wrapped2).loadFullyBg(numTries, comboCallback));
		}
		private void combineAfterLoad(Runnable callback) {
			synchronized (KnowledgeMultiItem.this) {
				loaded = true;
				statement = wrapped1.getStatement()+"\n\nALSO "+wrapped2.getId()+": "+wrapped2.getStatement();
				statementHtml = wrapped1.getStatementHtml()+"<p></p>\n"+
				wrapped2.getType()+" <b>"+wrapped2.getId()+"</b>: "+wrapped2.getStatementHtml();
				if (callback!=null) {
					callback.run();
				}
			}
		}
	}

	//--------- this listens for events

	public void pgipEvent(PGIPEvent e) {
		//don't think this is needed any more??
	}

	// ------------- model state changes effect on knowledge

	/** an internal marker for the current knowledge action;
	 *  anything calling this should call currentKnowledgeActionDone(this) when it finishes,
	 *  inside a ProverKnowledge.this synchronized block
	 */
	PooledRunnable currentKnowledgeAction = null;
	void currentKnowledgeActionDone(PooledRunnable runningAction) {
		if (runningAction==currentKnowledgeAction) {
			currentKnowledgeAction = null;
		} else {
			System.out.println("marking "+runningAction+" as done when "+currentKnowledgeAction+" is active");
		//TODO make sure this gets called
		}
	}


	/** called by the ProverState when a new theory is opened
	 * @param cmd the element sent out
	 * @param response the list of PGIPEvents returned
	 */
	public void onNewTheory(final Element cmd, final List<PGIPEvent> response) {
		//	SEND: <opentheory thyname="DerivE" parentnames="ContinuityIntegrabilityDefinitions;">
		//	  theory DerivE = ContinuityIntegrabilityDefinitions:</opentheory>
		//	GET:
		//	  READ: <pgip class = "pg" origin = "Isabelle/Isar" id = "/alex/1099428354.476" refseq = "7" refid = "PG-Eclipse" seq = "13">
		//	         <proofstate><pgml><statedisplay>theory DerivE =
		//		READ:   {ProtoPure, CPure, HOL, Set, Typedef, Fun, Product_Type, Lfp, Gfp,...
		//		READ:     Parity, PreList, List, Map, Hilbert_Choice, Infinite_Set, Extraction,
		//		READ:     Refute, Reconstruction, Main, #}</statedisplay></pgml></proofstate></pgip>

		//run the theory setup in the foreground (the event thread), then commands to the prover in the bg, as interruptible and synced tasks
		try {
			stopCKA();
			synchronized (this) {

				//then clear theories in use
				clear();

				//get the theory name and file
				String cThyName = null;
				String cThyFile = null;
				try {
					cThyName = cmd.attributeValue("thyname");
					if (ProofGeneralPlugin.isEclipseMode()) {
						//outside of eclipse mode the file is probably user input, we can ignore
						//(later we will look for it in the path)
						ProofScriptDocument doc = ProofGeneralPlugin.getSomeSessionManager().getActiveScript();
						if (doc != null) {
							cThyFile = doc.getResource().getFullPath().toString(); 
						}
					}
					//input = null;
					//org.eclipse.ui.ide.IDE.openEditor(page,marker,false);
					//org.eclipse.ui.ide.IDE.openEditor((IWorkbenchPage)null, (IFile)null, false);
				} catch (Exception e) {
					if (cThyName==null) {
						cThyName = "UnknownThyXXX"+StringManipulation.makeRandomId(4);
						System.err.println("ProverKnowledge had problems discovering the current theory from command "+cmd.asXML()+"; "+e+" (using "+cThyName+")");
					} else {
						//got a name, but no file
						System.err.println("ProverKnowledge -- no active editor.");
					}
				}
				currentTheoryFile = new TheoryFile(cThyName, cThyName, cThyFile);
				currentTheoryFile.isCurrent = true;


				if (ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_GRAB_PROVER_KNOWLEDGE)) {

					currentKnowledgeAction = new PooledRunnable("ProverKnowledge.onNewTheory") {
						public void run() {
							synchronized (ProverKnowledge.this) {
								GetCommandResponseAction.Session pkCommandSession = null;
								try {
									pkCommandSession =
										GetCommandResponseAction.getSessionWaiting("ProverKnowledge.onNewTheory", 3000);  //only do when idle for 3s

									long startTime = System.currentTimeMillis();
									System.out.println(NumericStringUtils.makeDateString()+": loading prover knowledge (can take up to one minute if GC kicks in, but usually faster)");

									discoverPath(pkCommandSession);

									//then set up the path of theory we are listening to
									{
										if (currentTheoryFile==null || !currentTheoryFile.hasName()) {
											return; //theory was disrupted
										}
										if ((currentTheoryFile.file == null || 
												 currentTheoryFile.getFile().indexOf(currentTheoryFile.getName())==-1) &&
												(ProofGeneralPlugin.isEclipseMode())) {
											//no editor found, or editor did not correspond to the theory
											System.err.println("ProverKnowledge -- active file "+currentTheoryFile.file+" has different name for theory, "+currentTheoryFile.getName());
											currentTheoryFile.file = findPathOfTheory(currentTheoryFile.getName());
											if (currentTheoryFile.file!=null) {
												System.err.println("ProverKnowledge -- found "+currentTheoryFile.file+" instead for theory "+currentTheoryFile.getName());
											}
										}
									}

									loadFromProverPrivate(pkCommandSession, startTime);

								} catch (InterruptedException e) {  //had to interrupt this
								} catch (ProverDeadException e) {
									clear();
								} catch (Exception e) {
									System.err.println("error trying to get ProverKnowledge in "+this);
									e.printStackTrace();
								} finally {
									synchronized (ProverKnowledge.this) {
										//System.out.println("DISPOSING "+this+" "+pkCommandSession);
										if (pkCommandSession!=null) {
											pkCommandSession.dispose();
										}
										currentKnowledgeActionDone(this);
									}
								}
							} //release sync lock
						}
					};
					if (RUN_IN_BACKGROUND) {
						currentKnowledgeAction.start();
					} else {
						currentKnowledgeAction.run();
					}
				}
			}//release sync lock
		} catch (Exception e) {
			System.err.println("error trying to get ProverKnowledge");
			e.printStackTrace();
		}
	}

	/** tries to remove all items associated with the given id;
	 * those  that are multi-items can't (yet) get removed, but are set 'out of date' */
	private void removeItem(String id) {
		IKnowledgeItem oldItem = getItem(id);
		if (oldItem instanceof KnowledgeItem) {
			((KnowledgeItem)oldItem).statement = "(out of date)";
			((KnowledgeItem)oldItem).statementHtml = "(out of date)";
		}
		String subname = id;
		do {
			IKnowledgeItem ki = getItem(subname);
			if (ki!=null && ki.getId().equals(id)) {
				activeItems.remove(subname);
			}
			if (subname.indexOf('.')<=0) {
				break;
			}
			subname = subname.substring(subname.indexOf('.')+1);
		} while (subname.length()>0);
	}

	private void addNames(IKnowledgeItem ki, String fqn) {
		String subname = fqn;
		do {
			if (!ki.getNames().contains(subname)) {
				ki.getNames().add(subname);
			}
			if (subname.indexOf('.')<=0) {
				break;
			}
			subname = subname.substring(subname.indexOf('.')+1);
		} while (subname.length()>0);
	}

	/** adds the item to the activeItems list if it isn't there;
	 *  if something of the same name is there, it adds another entry as a 'multiple item'
	 *  (unless it is the same, in which case it isn't added)
	 * @param thi item to add
	 * @param tryUpdate whether to simply update the record if it already exists for this ID
	 * (assuming 'name' set is the same)
	 * @param i whether to add at beginning (0) or end (-1)
	 */
	private void addItem(IKnowledgeItem thi, boolean tryUpdate, int i) {
		addNames(thi, thi.getId());
		if (tryUpdate) {
			//TODO this is quick and dirty; may fail if retrieve Multi, or if names change
			IKnowledgeItem ki2 = getItem(thi.getId());
			if (ki2!=null && !ki2.equals(thi)) {
				if (ki2 instanceof LazyTheoryItem) {
					((LazyTheoryItem)ki2).loaded = false;
				}
				if (ki2 instanceof KnowledgeItem) {
					KnowledgeItem ki3 = (KnowledgeItem)ki2;
					ki3.names = thi.getNames();
					ki3.statement = thi.getStatement();
					ki3.statementHtml = thi.getStatementHtml();
					ki3.theory = thi.getTheory();
					ki3.type = thi.getType();
				}
			}
		}
		Iterator kii = thi.getNames().iterator();
		while (kii.hasNext()) {
			String name = (String)kii.next();
			IKnowledgeItem ki = activeItems.get(name);
			if (ki==null) {
				activeItems.put(name, thi);
				continue;
			}
			if (ki.getId().equals(thi.getId())) {
				continue;
			}
			//combine existing ki with new thi
			if (i==-1) {
				activeItems.put(name, new KnowledgeMultiItem(ki, thi));
			} else {
				activeItems.put(name, new KnowledgeMultiItem(thi, ki));
			}
		}
	}

	/**
	 * Stop the currentKnowledgeAction.
	 */
	private void stopCKA() {
		//mark anything running (waiting) as interrupted, it will finish before we take the sync lock
		// FIXME BUG? da: FindBugs reports inconsistent sync here IS
		if (currentKnowledgeAction!=null) {
			//System.out.println("interrupting old action "+currentKnowledgeAction);
			currentKnowledgeAction.interrupt();
		}
	}

	/** loads the methods from the theorem prover
	 * @throws InterruptedException
	 * @throws ProverDeadException
	 * @returns number of methods loaded
	 */
	public int loadMethods(GetCommandResponseAction.Session pkSession, TheoryFile theory)
	throws InterruptedException, ProverDeadException {
		String meths = pkSession.doCommandWaiting(
				"ML {* print_methods (theory \""+theory.getName()+"\");  *}"
		).getStringValue();
		//should be of form
		//methods:
		//  some_tac: a description
		//  another_tac: description
		//val it = () : unit
		int count = 0;
		try {
			StringTokenizer st = new StringTokenizer(meths, "\n");
			if (!st.hasMoreTokens()) {
				throw new IllegalArgumentException("no data");
			}
			String meth = st.nextToken();
			// Skip a blank line
			if (meth.trim().length()==0 && st.hasMoreTokens()) {
				meth = st.nextToken();
			}
			if (!meth.trim().equals("methods:")) {
				throw new IllegalArgumentException("first line not 'methods:'");
			}
			String name = null;
			String id = null;
			KnowledgeItem ki = null;
			while (st.hasMoreTokens()) {
				meth = st.nextToken().trim();
				if (meth.startsWith("val it =") || meth.length()==0) {
					continue;
				}
				int i = meth.indexOf(':');
				if (i==-1 || (!meth.matches("\\s*[^ \\t]+\\s*:.*"))) {   //don't allow spaces in definitions
					if (name==null || id==null || ki==null) {
						throw new IllegalArgumentException("line not of form 'method: description'");
					}
					//the new "subst" is the culprit here... just put data in previous item
					ki.statement = (ki.statement==null || ki.statement.length()==0 ? "" : ki.statement+" ")+meth;
					continue;
				}
				id = meth.substring(0, i).trim();
				name = id;
				if (name.indexOf('.')>=0) {
					name = name.substring(name.lastIndexOf('.')+1);  //convert things like HOL.rule
				}
				String descr = meth.substring(i+1).trim();
				ki = new KnowledgeItem(id, "tactic", name, theory, descr);
				addItem(ki, false, 0);
				count++;
			}
		} catch (IllegalArgumentException e) {
			System.err.println("ProverKnowledge, methods is not of expected form ("+e.getMessage()+"); method/tactic data not available; got "+meths);
		}
		return count;
	}

	//----------------------------------- other actions

	public void onUndoneOpenTheory() {
		stopCKA();
		clear();
	}

	public void onUndoneCloseTheory() {
		//stopCKA();
		//clear();
		// da: NB: one-step only supported.  More may be needed
		// in general case (stack-pop), but multiple-theory files
		// are problematic in Isabelle and not really supported.
		currentTheoryFile = prevTheoryFile;
	}

	public void clear() {
		prevTheoryFile = null;
		currentTheoryFile = null;
		activeItems.clear();
		activeTheories.clear();
	}

	/** whether theorem requesting should run in the background or not;
	 *  if this is false, this shouldn't get run in an event thread,
	 *  because that thread may need events that will never run
	 */
	protected static final boolean RUN_IN_BACKGROUND = true;

	protected static final boolean print_undone_items = true;  //TODO remove

	private static String cutFromStart(String s, String cut) {
		if (s.startsWith(cut)) {
			return s.substring(cut.length());
		}
		return s;
	}


	KnowledgeItem currentTheorem = null;
	final static String UNTITLED_THEOREM = "(untitled)";

	public void onNewTheoremStart(Element xe) {
		//<opengoal thmname="crap">lemma crap: "bloo (%x. x+1) = (%x. x+1)"</opengoal>
		//if (print_undone_items) System.err.println("NEW THEOREM: "+xe.asXML());
		try {
			String thmname = null;

			Attribute ta = xe.attribute(PGIPSyntax.THEOREM_NAME);
			if (ta!=null) {
				thmname = ta.getValue();
			}

			//good to use field constant, but shouldn't exit if not set, just continue with untitled, -AH
//			if (xe.attribute(PGIPSyntax.THEOREM_NAME) == null) return;
//			String thmname = xe.attribute(PGIPSyntax.THEOREM_NAME).getValue();
			// da: get this error a lot but it seems spurious (perhaps undo mishandled)?
			if (currentTheorem != null) {
				System.err.println("ProverKnowledge: starting a new theorem "+(thmname==null ? "(untitled)" : thmname)+
						" when it looks like we were still working on "+currentTheorem.id);
			}
			if (ta!=null) {
				currentTheorem = new LazyTheoryItem((currentTheoryFile==null ? "": currentTheoryFile.getName()+".")+thmname, thmname, currentTheoryFile);
			} else {
				currentTheorem = new KnowledgeItem(UNTITLED_THEOREM, "", UNTITLED_THEOREM, currentTheoryFile, "");
			}

			currentTheorem.type = "unknown";

			//try to get a parse for the statement
			String thmstatement = xe.getStringValue().trim();
			String type = null;
			if (thmstatement.startsWith("lemma")) {
				type = "lemma";
			} else if (thmstatement.startsWith("theorem")) {
				type = "theorem";
			}
			if (type==null) {
				return; //can't get statement
			}
			currentTheorem.type = type;
			thmstatement = cutFromStart(thmstatement, type).trim();
			if (thmname!=null) {
				thmstatement = cutFromStart(thmstatement, thmname+":").trim();
			}
			thmstatement = cutFromStart(thmstatement, "\"").trim();
			if (thmstatement.endsWith("\"")) {
				thmstatement = thmstatement.substring(0, thmstatement.length()-1).trim();
			}
			currentTheorem.statement = thmstatement;
		} catch (Exception e) {
			System.err.println("ProverKnowledge: unable to parse new theorem from "+xe.asXML()+": "+e);
		}
	}
	public void onNewTheoremCancel(Element xe, ArrayList response) {
		currentTheorem = null;
	}
	public void onNewTheoremFinish(Element xe, ArrayList response) {
		// not called on discardgoal, only on (outermost) postponegoal/closegoal
		if (currentTheorem==null) {
			System.err.println("ProverKnowledge: no current theorem on finish, "+xe.asXML());
			return;
		}
		if (!currentTheorem.id.equals(UNTITLED_THEOREM)) {
			addItem(currentTheorem, true, 0);
			((LazyTheoryItem)currentTheorem).loadFullyBg(3, null); // BC ??
		}
		currentTheorem = null;
//		PGIPEvent last_response = null;
//		Iterator ri = response.iterator();
//		while (ri.hasNext()) {
//		Object r = ri.next();
//		if (r instanceof PGIPIncoming) last_response = (PGIPIncoming)r;
//		}
//
//		if (last_response==null || last_response.parseTree==null) return;
//		//could try to get a better form of the statement from last_response
//		try {
//		System.out.println("GOT: "+last_response.parseTree.asXML());
//		Element nr = (Element)last_response.parseTree.elements().get(0);  //the 'normalresponse' element
//		parse this...
//		} catch (Exception e) {}
	}

	public void onUndoneTheorem(Element xe, Element opener, ArrayList response) {
		Attribute thmnameAttr = null; 
		String thmname = "";
		currentTheorem = null;
		if (opener != null) {
			thmnameAttr = opener.attribute(PGIPSyntax.THEOREM_NAME);
		}
		if (thmnameAttr == null && xe != null) {
			thmnameAttr = xe.attribute(PGIPSyntax.THEOREM_NAME);
		}
		if (thmnameAttr != null) {
			thmname = thmnameAttr.getValue();
		}
		if (thmname.equals("")) {
			//probably didn't have a title
			if (ProofGeneralPlugin.debug(this)) {
				System.err.println("ProverKnowledge.onUndoneTheorem:  can't figure out what theorem we are undoing; keeping its knowledge.");
			}
			return;
		}
		removeItem(thmname);
	}

	public void onNewTheoryItem(Element xe, ArrayList response) {
		//<theoryitem name="hoo" objtype="theorem">axioms hoo:  "bloo F = F"</theoryitem>
		//<theoryitem name="bloo" objtype="term">constdefs bloo  ::
		//  "(nat\&lt;Rightarrow&gt;nat)\&lt;Rightarrow&gt;(nat\&lt;Rightarrow&gt;nat)"
		//  "bloo F == (THE f. ALL x::nat.( F x = f x))"</theoryitem>
		try {
			Attribute nameAt = xe.attribute("name");
			if (nameAt==null) {
				return;  //eg things like datatype
		    }
			String name = nameAt.getValue();
			LazyTheoryItem ki = new LazyTheoryItem((currentTheoryFile==null ? "": currentTheoryFile.getName()+".")+name, name, currentTheoryFile);
			ki.type = "unknown";

			//try to get a parse for the statement
			String thmstatement = xe.getStringValue().trim();
			String type = null;
			if (thmstatement.startsWith("constdef")) {
				type = "constant";
			} else if (thmstatement.startsWith("axiom")) {
				type = "axiom";
			}
			if (type==null) {
				//don't recognise the type
				if (thmstatement.startsWith("def") ||
						thmstatement.startsWith("const") ||
						thmstatement.startsWith("lemmas")) {
					//skip; we could make a note of these ... but consts at least are defined elsewhere (this just gives type info)
				} else {
					if (print_undone_items) {
						System.err.println("Prover knowledge: unknown theory item, "+thmstatement);
					}
				}
			} else {
				if (type.equals("constant")) {
					addNames(ki, ki.id+"_def");
					addNames(ki, ki.id);
					ki.id = ki.id+"_def";
				}
				ki.type = type;
				addItem(ki, true, 0);
				int colon = thmstatement.indexOf(':');
				if (colon>0) {
					thmstatement = thmstatement.substring(colon+1);
					while (thmstatement.startsWith(":")) {
						thmstatement = thmstatement.substring(1);
					}
					thmstatement = thmstatement.trim();
					thmstatement = cutFromStart(thmstatement, "\"").trim();
					if (thmstatement.endsWith("\"")) {
						thmstatement = thmstatement.substring(0, thmstatement.length()-1).trim();
					}
					ki.statement = thmstatement;
				}
				//now try to request what it actually is...
				ki.loadFullyBg(3, null);
			}
		} catch (Exception e) {
			System.err.println("ProverKnowledge: unable to parse new theorem from "+xe.asXML()+": "+e);
		}

	}

	public void onUndoneTheoryItem(Element xe, ArrayList response) {
		try {
			Attribute nameAt = xe.attribute("name");
			if (nameAt==null) {
				return;
			}
			String name = nameAt.getValue();
			if (currentTheoryFile==null) {
				return;
			}
			String fqn = currentTheoryFile.getName()+"."+name;
			IKnowledgeItem ki = getItem(fqn);
			if (ki==null) {
				ki=getItem(fqn);
				if (ki!=null && ki.getId().indexOf('.')>0 && !ki.getId().startsWith(currentTheoryFile.getName())) {
					ki=null;
				}
			}
			if (ki==null) {
				return;
			}
			removeItem(ki.getId());
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}

	/**
	 * TODO: current prover path - but are these os paths?? paths relative to where - the cwd??
	 * If so they need to be changed if the cwd is changed (which isn't happening).
	 * This is not used much yet. Anyone wishing to link core functionality to this should examine it more.
	 */
	ArrayList<String> currentProverPath = new ArrayList<String>();



	/** looks for a theory in the current prover path (by appending .thy and looking for a filename);
	 *  returns full path, or null if it can't find it (eg it's from the heap) */
	public String findPathOfTheory(String theory) {
		Iterator pi = currentProverPath.iterator();
		while (pi.hasNext()) {
			try {
				String ps = (String)pi.next();
				IPath pa = new Path(ps);
// FIXME da: this is a bit tricky.  The paths may be relative to the loaded documents.
// We might add to the path when we change the working directory with setProverCWD.
// But it's better to find out paths (based on URLs) from the prover directly.
//				if (!pa.isAbsolute() && sm.proverInfo.getProverStartDir()!=null)
//					pa = new Path(sm.proverInfo.getProverStartDir().getCanonicalPath()).append(pa);
				pa = pa.addTrailingSeparator().append(theory+".thy"); // TODO: this ".thy" is hard coding, & should be removed
				String fn = pa.toOSString();
				File f = new File(fn);
				if (f.exists()) {
					return fn;
				}
			} catch (Exception e) {
				//possible io exceptions?
				e.printStackTrace();
			}
		}
		return null;
	}

	//---- current prover knowledge

	TheoryFile currentTheoryFile = null;
	TheoryFile prevTheoryFile = null; // TODO da: maybe keep a stack here
	ArrayList<TheoryFile> activeTheories = new ArrayList<TheoryFile>();
	TreeMap<String, IKnowledgeItem> activeItems = new TreeMap<String, IKnowledgeItem>();
	//TODO want a more complex data type which allows multiple items of same name
	//because this will only store one copy of an item, a new item of the same name will delete the old,
	//then if that theory is undone it will be lost

	/** returns a copy of the map of all items starting with s */
	public TreeMap<String, IKnowledgeItem> itemsStartingWith(String s) {
		if (s==null || s.length()==0) {
			return new TreeMap<String, IKnowledgeItem>(activeItems);
		}
		String start = s;
		String end = s.substring(0, s.length()-1)+((char)(s.charAt(s.length()-1)+1));
		//System.out.println("returning all from "+start+" to "+end);
		return new TreeMap<String, IKnowledgeItem>(activeItems.subMap(start, end));
		//TODO may want this case insensitive ?
	}

	/** returns a list of tree map entries starting with s and matching regex */
	public ArrayList<Entry> itemsStartingWithAndMatching(String s, String regex) {
		ArrayList<Entry> result = new ArrayList<Entry>();
		if (s==null || s.length()==0) {
			return result;
		}
		TreeMap tm = itemsStartingWith(s);
		Iterator tmi = tm.entrySet().iterator();
		Pattern cregex = Pattern.compile(regex);
		while (tmi.hasNext()) {
			Entry e = (Entry) tmi.next();
			if (cregex.matcher(e.getKey().toString()).matches()) {
				result.add(e);
			}
		}
		return result;
	}

	public KnowledgeItem getItem(String ki) {
		return (KnowledgeItem)activeItems.get(ki);
	}


	/** discovers and sets the prover path by sending it appropriate commands
	 * @throws InterruptedException
	 * @throws ProverDeadException
	 */
	// FIXME da: this is Isabelle specific.  We should add a PGIP command for this,
	// to return a list of URIs, maybe.
	private void discoverPath(GetCommandResponseAction.Session pkSession) throws InterruptedException, ProverDeadException {
		//then get the path
		currentProverPath.clear();
		Element pathElt = pkSession.doCommandWaiting(
				//"ML {* show_path {} *}"   //following command removes all env vars
				// da: Isabelle 2007: pack->implode, unpack->explode
				"ML {* map (Path.implode o Path.expand o Path.explode) (show_path ()) *}"
		);
		if (pathElt == null) {
			return;
		}
		String path = pathElt.getStringValue();
		//should be of form val it = [".", "$ISABELLE_HOME/src/HOL/Library"] : string list

		int pathI = path.indexOf('[')+1;
		int pathJ = -1;
		int pathEnd = path.indexOf(']');
		if (pathI==-1 || pathEnd<pathI) {
			System.err.println("ProverKnowledge path is not of expected form, no path will be available;\npath is "+path);
		} else {
			while ( pathI < pathEnd && (pathJ = MiscUtils.minNonNeg(path.indexOf(',', pathI), pathEnd))>0 ) {
				String f = path.substring(pathI, pathJ).trim();
				pathI = pathJ+1;
				if (f.startsWith("\"") && f.endsWith("\"")) {
					f = f.substring(1, f.length()-1);
				}
				currentProverPath.add(f);
			}
		} // end else
		if (ProofGeneralPlugin.debug(this)) {
			System.out.println("ProverKnowledge: Path is "+ArrayListOperations.list2string(currentProverPath, ", "));
		}
	}


//	/**
//	 * @param response
//	 * @return
//	 */
//	private PGIPEvent getLastIncoming(final List response) {
//		PGIPEvent last_response = null;
//		Iterator ri = response.iterator();
//		while (ri.hasNext()) {
//			Object r = ri.next();
//			if (r instanceof PGIPIncoming) last_response = (PGIPIncoming)r;
//		}
//		return last_response;
//	}


	/**
	 * @param xe
	 */
	public void onCloseTheory(Element xe) {
		prevTheoryFile = currentTheoryFile;
		currentTheoryFile = null;  //that's all we do, so rollovers still work (though this isn't important)
	}

	public void loadFromProver(String currentTheoryFileName) throws InterruptedException, ProverDeadException {
		Session pkCommandSession = GetCommandResponseAction.getSessionWaiting("ProverKnowledge.loadFromProver", 0);
		long startTime = System.currentTimeMillis();
		currentTheoryFile = new TheoryFile(currentTheoryFileName, currentTheoryFileName, currentTheoryFileName+".thy");
		try {
			loadFromProverPrivate(pkCommandSession, startTime);
		} finally {
			pkCommandSession.dispose();
		}
	}

	private void loadFromProverPrivate(GetCommandResponseAction.Session pkCommandSession, long startTime) throws InterruptedException, ProverDeadException {
		//load methods
		int numMethods = loadMethods(pkCommandSession, currentTheoryFile);

		//finally set up items for all other theories
		//this used to work, but now we don't get the complete signature back from isabelle (May 05)
//		PGIPEvent last_response = getLastIncoming(response);
//
//		if (last_response==null || last_response.parseTree==null) {
//		System.err.println("ProverKnowledge needs a response when new theory is opened, to see what other theories are being used");
//		return;
//		}
//		String theoryList = last_response.parseTree.getStringValue();
		//so do this instead
		String theoryList = pkCommandSession.doCommandWaiting(
				"ML {* Theory.ancestors_of (theory \""+currentTheoryFile.getName()+"\");  *}"
		).getStringValue();

		int theoryI = theoryList.indexOf('{')+1;
		int theoryJ = -1;
		int theoryEnd = theoryList.indexOf('}');
		if (theoryI==-1 || theoryEnd<theoryI) {
			System.err.println("ProverKnowledge open theory response is not of expected form, not loading knowledge items\nresponse is: "+
					theoryList);
			return;
		}
		while ( theoryI < theoryEnd && (theoryJ = MiscUtils.minNonNeg(theoryList.indexOf(',', theoryI), theoryEnd))>0 ) {
			String theory = theoryList.substring(theoryI, theoryJ).trim();
			theoryI = theoryJ+1;
			//System.out.println("using theory: "+theory);
			if (theory.length()>0 && !"#".equals(theory)) {
				TheoryFile tf = new TheoryFile(theory, theory, findPathOfTheory(theory));
				activeTheories.add(tf);
				activeItems.put(theory, tf);
			}
		}

		//put this at the end of theories being used
		activeTheories.add(currentTheoryFile);
		addItem(currentTheoryFile, true, 0);

		//System.err.println("\n\n\n    STARTING OUTPUT MODE  "+OUT_MODE+"\n\n");

		//now find out all lemmas and axioms in each of these theories!
		Iterator ti = activeTheories.iterator();
		int i=0;
		String axioms = null;
		String lemmas = null;

		int numAxioms = 0;
		int numLemmas = 0;

		//						//GET THMS and AXIOMS from ONE BIG COMMAND  ... 900s initially, for me
		//						if (OUT_MODE==0) {
		//							StringBuffer bigCommandAxms = new StringBuffer("ML {* writeln (\"\\n\" ^ ");
		//							StringBuffer bigCommandThms = new StringBuffer("ML {* writeln (\"\\n\" ^ ");
		//							while (ti.hasNext()) {
		//								TheoryFile tf = (TheoryFile)ti.next();
		//								if (tf.isCurrent == false) {
		//									bigCommandAxms.append("\"AXIOMS "+tf.name+" : \" ^ (commas (map (fst) (axioms_of (theory \""+tf.name+"\")))) ^ \"\\n\" ^ ");
		//									bigCommandAxms.append("\"LEMMAS "+tf.name+" : \" ^ (commas (map (fst) (thms_of (theory \""+tf.name+"\")))) ^ \"\\n\" ^ ");
		//								}
		//							}
		//							bigCommandAxms.append(" \"\\nDONE\\n\" ) *}");
		//							bigCommandThms.append(" \"\\nDONE\\n\" ) *}");
		//							axioms = GetCommandResponseAction.doCommandWaiting(bigCommandAxms.toString()).getStringValue();
		//							lemmas = GetCommandResponseAction.doCommandWaiting(bigCommandThms.toString()).getStringValue();
		//						}

		//with smaller commands ... about twice as long;
		//but is more easily interruptible, less hacks
		while (ti.hasNext()) {
			i++;
			TheoryFile tf = (TheoryFile)ti.next();
			if (tf.isCurrent == false) {
				try {
					axioms = pkCommandSession.doCommandWaiting("ML {* "+
							"commas (map (fst) (axioms_of (theory \""+tf.getName()+"\"))); *}").getStringValue();
					lemmas = pkCommandSession.doCommandWaiting("ML {* "+
							"commas (map (fst) (PureThy.thms_of (theory \""+tf.getName()+"\"))); *}").getStringValue();

					//parse this output, generate items
					//System.out.println("AXIOMS theory "+tf.name+":\n");//+axioms);

					int[] result = parseAxiomsAndLemmas(axioms, lemmas, tf, false);
					numAxioms += result[0];
					numLemmas += result[1];

				} catch (NullPointerException e) {
					System.err.println("couldn't get axioms or theorems for theory "+tf.getName());
					e.printStackTrace();
				} catch (StringIndexOutOfBoundsException e) {
					System.err.println("couldn't get axioms or theorems for theory "+tf.getName());
					System.err.println("  axioms: "+axioms);
					System.err.println("  lemmas: "+lemmas);
					e.printStackTrace();
				}
			}
		}

		if (i==0) {
			System.err.println("ProverKnowledge didn't find any theories; much auto-complete and tooltip information will be unavailable.");
			//happens sometimes, usu if model breaks
		}
		else {
			startTime = System.currentTimeMillis() - startTime;
			System.out.println(NumericStringUtils.makeDateString()+" ["+NumericStringUtils.makeTimeString(startTime)+"]: "+
					"loaded prover knowledge ("+numMethods+" methods, "+activeTheories.size()+" theories, "+
					numAxioms+" axioms/definitions and "+numLemmas+" lemmas)");
		}
		return;

		//						OUT_MODE = (OUT_MODE+1)%4;
		//takes about 25s for 100 theories ... now 2.3s for 84 theories, but something (GC?) can slow it down hugely

		//TODO ideally we would allow cached theory items, particularly if it's slow
	}

	/* reloads all elements from the current theory, setting up and clearing a command session iff the argument is null;
	 * TODO local knowledge should be inferred automatically; right now it is needed manually to get some entities, eg primrecs, that cannot be inferred from PGIPEvents
	 * @throws InterruptedException
	 * @throws ProverDeadException */
// da: doesn't seem to be used at present, and refers to old ProofGeneral.show_context() no longer available.
//	public void reparseCurrentTheory(Session pkCommandSession, int numTries) throws InterruptedException, ProverDeadException {
//		if (currentTheoryFile==null) {
//			return;
//		}
//		boolean setSession = false;
//		if (pkCommandSession==null) {
//			pkCommandSession = GetCommandResponseAction.getSessionWaiting("ProverKnowledge.loadFromProver", numTries);
//			setSession = true;
//		}
//		try {
//			String axioms = pkCommandSession.doCommandWaiting("ML {* "+
//			"commas (map (fst) (ProofGeneral.show_context() |> Theory.axioms_of)); *}").getStringValue();
//			parseAxiomsAndLemmas(axioms, null, currentTheoryFile, true);
//		} finally {
//			if (setSession) {
//				pkCommandSession.dispose();
//			}
//		}
//	}
//
	protected int[] parseAxiomsAndLemmas(String axioms, String lemmas, TheoryFile tf, boolean update) {
		int numAxioms = 0;
		int numLemmas = 0;
		String list = null;
		if (axioms!=null && axioms.indexOf('\"') != -1) {
			list = axioms.substring(axioms.indexOf('\"')+1, axioms.lastIndexOf('\"'));
			int k=0;
			while (k<list.length()) {
				int k2 = list.indexOf(',', k);
				if (k2==-1) {
					k2=list.length();
				}
				String item = list.substring(k, k2).trim();
				k = k2+1;
				//add item
				if (item==null || item.length()==0) {
					//		  								item = General.makeRandomId(6);
					//		  								name = "";
					//probably shouldn't add items with no name
				} else {
					String name = item;
					//now we skip post-processing, eg removal of theory etc
					if (name.length()==0) {
						//also don't add it
					} else {
						LazyTheoryItem thi = new LazyTheoryItem(item, name, tf);
						thi.type = "axiom";
						thi.id = item;
						numAxioms++;
						if (thi.getId().endsWith("_def")) {
							thi.type = "constant";
							addNames(thi, thi.getId());
							addNames(thi, thi.getId().substring(0, name.length()-4));
						}
						addItem(thi, update, -1);
					}
				}
			}
		}

		//System.out.println("LEMMAS theory "+tf.name+":\n");//+lemmas);

		if (lemmas!=null) {
			if (lemmas.indexOf('\"')>=0) {
				list = lemmas.substring(lemmas.indexOf('\"')+1, lemmas.lastIndexOf('\"'));
			//was getting error because thms_of is no longer avail on top level
			//but this wasn't helping; probably cut it.  fixed above, PureThy.thms_of.  -AH
//			else if (lemmas.indexOf('{')>=0 && lemmas.indexOf('}')>=0)
//			list = lemmas.substring(lemmas.indexOf('{')+1, lemmas.lastIndexOf('}'));
			} else {
				System.err.println("ProverKnowledge:  unknown format for lemmas list, "+lemmas);
				list = lemmas;
			}

			int k=0;
			while (k<list.length()) {
				int k2 = list.indexOf(',', k);
				if (k2==-1) {
					k2=list.length();
				}
				String item = list.substring(k, k2).trim();
				k = k2+1;
				//add item
				if (item==null || item.length()==0) {
					//		  								item = General.makeRandomId(6);
					//		  								name = "";

					//probably shouldn't add this one
				} else {
					String name = item;
					if (name.length()==0) {
						//also don't add it
					} else {
						KnowledgeItem existsCopy = (KnowledgeItem)activeItems.get(name);
						if (existsCopy==null || !existsCopy.id.equals(item)) {
							//only add if there is not already a copy (because axioms are also reported as theorems)
							LazyTheoryItem thi = new LazyTheoryItem(item, name, tf);
							thi.type = "theorem";   //TODO can we distinguish between theorems and lemmas?
							thi.id = item;
							numLemmas++;
							//System.out.println("  got "+item);
							if (thi.getId().endsWith("_def")) {
								thi.type = "constant";
								if (thi.getId().endsWith("_def")) {
									thi.type = "constant";
									addNames(thi, thi.getId());
									addNames(thi, thi.getId().substring(0, name.length()-4));
								}
							}
							addItem(thi, update, -1);
						}
					}
				}
			}
		}

		return new int[] { numAxioms, numLemmas };
	}


}
