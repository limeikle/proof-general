package ed.inf.proofgeneral.depgraph.graph;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.DocElement;
import ed.inf.proofgeneral.sessionmanager.PGIPSyntax;
import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.proofgeneral.sessionmanager.events.IPGIPListener;
import ed.inf.proofgeneral.sessionmanager.events.PGIPEvent;

public class Graph implements IPGIPListener {

	public int statElements = 0;

	private FileOutputStream fileWriter;

	private final PrintStream p;

	private MyNode treeHead;

	// private MyLayOut layout = new MyLayOut();

	private final boolean debug = true;

	public boolean connected = false;

	private static Graph graph;

	// static HashMap map = new HashMap();

	private String theoryName = "";

	private final ArrayList<String> theoryDeps = new ArrayList<String>();

	private static HashMap<String, MyModel> trees = new HashMap<String, MyModel>();

	private final ArrayList<PGIPEvent> history = new ArrayList<PGIPEvent>();

	public Graph() {
		this.graph = this;
		try {
			fileWriter = new FileOutputStream("debug" + ".txt");
			System.out.println("file created");
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		}
		p = new PrintStream(fileWriter);

		final SessionManager[] sms = ProofGeneralPlugin.getSessionManagers();
		for (final SessionManager sm : sms) {
			if (sm != null) {
				System.out.println("Session manager conected!");
				sm.addListener(this); // this is mirrored in SessionManager(),
				// to protect against odd starting
				// orders
				connected = true;

			}
		}

	}

	/*
	 * private MyNode createRoot(String rootName) {
	 * 
	 * MyNode result = new MyNode(rootName); print("root: " + rootName); return
	 * result; }
	 */

	public static List<MyModel> getDiagramChildren() {

		final Collection<MyModel> list = trees.values();
		final Iterator<MyModel> it = list.iterator();
		final ArrayList<MyModel> st = new ArrayList<MyModel>();
		while (it.hasNext()) {
			final MyModel mod = it.next();
			// mod.evaluate();
			st.add(mod);
		}
		System.out.println("getDiagramChildren()");
		return st;
	}

	private static HashMap<String, DocElement> docElements = new HashMap<String, DocElement>();

	public static void addDocElement(DocElement element) {
		docElements.put(element.attributeValue(PGIPSyntax.THEOREM_NAME),
				element);
		// System.out.println("addDocElement"
		// + element.attribute(PGIPSyntax.THEOREM_NAME) + ";"
		// + element.getName());
		// System.out.println("addDocElement" + element.rawText);
	}

	public static Graph getDefault() {
		return graph;
	}

	public void pgipEvent(PGIPEvent event) {
		if (eventFilter(event)) {

			// System.out.println("pgipEvent:" + event.getText());
			printFile("pgipEvent:" + event.getText());
			// System.out.println("event type:" + event.type);
			printFile("event type:" + event.type);
			printFile("event all:" + event.toString());
			// process("event all:" + event.toString());
			if (event.type.equals("proofstate")) {
				if (event.getText().indexOf("theory") != -1) {
					final String msg = event.getText();

					theoryName = msg.substring(msg.indexOf("theory") + 7, msg
							.indexOf("=") - 1);
					theoryName = theoryName.trim();
					System.out.println("theoryName:" + theoryName);
					final String inside = msg.substring(msg.indexOf("{") + 1,
							msg.indexOf("}")).trim();
					final StringTokenizer st = new StringTokenizer(inside, ",");
					while (st.hasMoreTokens()) {
						String next = st.nextToken().trim();
						if (next.equals("Main")) {
							next = "HOL";
						}

						System.out.println("theoryDeps:" + next);
						theoryDeps.add(next);
					}
					initTree();
					processHistory();
				}
			}
			if (event.type.equals("metainforesponse")) {
				statElements += 1;
				history.add(event);
				if (trees.size() != 0) {
					processEvent(event);
				}
				// System.out.println("process done");
				final ArrayList<MyModel> ar = getModels();
				final Iterator<MyModel> it = ar.iterator();
				while (it.hasNext()) {
					final MyModel model = it.next();
					print(model.getText());

				}

			}
			if (event.type.equals("abortgoal")) {
				System.out.println(event.toString());
				System.out.println("msg undo " + event.getText());
				event.parseTree.toString();
			}
			if (event.type.equals("undoitem")) {
				System.out.println(event.toString());
				System.out.println("msg undo item " + event.getText());

			}
		}
	}

	private void processHistory() {
		final Iterator<PGIPEvent> it = history.iterator();
		while (it.hasNext()) {
			processEvent(it.next());
		}

	}

	private void processEvent(PGIPEvent event) {
		final String msg = event.getText();
		final StringTokenizer st = new StringTokenizer(msg, "\" ");
		String childName = "";
		final ArrayList<String> parentsName = new ArrayList<String>();
		if (st.hasMoreTokens()) {
			childName = st.nextToken();
		}
		while (st.hasMoreTokens()) {
			final String s = st.nextToken();
			// if (!s.equals(""))
			parentsName.add(s);
		}

		processMessage(childName, parentsName);
	}

	private void processMessage(String childName, ArrayList<String> parentsName) {
		MyModel model = null;
		print("process" + childName + parentsName.toString());
		final String subName = childName.substring(0, childName.indexOf("."));
		final Iterator<String> it = parentsName.iterator();
		final int index = childName.indexOf(".") + 1;
		final String childShort = childName.substring(index);
		while (it.hasNext()) {
			final String parent = it.next();
			if (!parent.equals(childName)) {
				final String parentTheory = parent.substring(0, parent
						.indexOf("."));
				print("parentTheory" + parentTheory);

				if (trees.containsKey(parentTheory)) {
					model = trees.get(parentTheory);

				} else {
					model = new MyModel(parentTheory);
				}
				final String parentShort = parent
						.substring(parent.indexOf(".") + 1);

				model.addChild(parent, childName);
			}
		}
		System.out.println("evaluate " + childShort);

		// evaluate(childName);

	}

	private void evaluate(String childShort) {
		final ArrayList<MyModel> models = getModels();
		final Iterator<MyModel> it = models.iterator();
		while (it.hasNext()) {
			final MyModel model = it.next();
			model.evaluate(childShort);
		}
	}

	private void initTree() {
		final MyModel mainTree = new MyModel(theoryName);
		trees.put(theoryName, mainTree);
		final Iterator<String> it = theoryDeps.iterator();
		while (it.hasNext()) {
			final MyModel tree = new MyModel(it.next());
			trees.put(tree.getText(), tree);

		}

	}

	public void dispose() {
		final SessionManager[] sms = ProofGeneralPlugin.getSessionManagers();
		for (final SessionManager sm : sms) {
			if (sm != null) {
				sm.removeListener(this);
				connected = false;
			}
		}
	}


	public boolean eventFilter(PGIPEvent event) {
		if (event.type != null) {
			if (event.type.equals("proofstate") && trees.size() == 0) {
				return true;
			}
			if (event.type.equals("metainforesponse")) {
				return true;
			}
			if (event.type.equals("undoitem") || event.type.equals("abortgoal")) {
				return true;
			}

		}
		return false;
	}

	public ArrayList<MyModel> getModels() {
		final Collection<MyModel> list = trees.values();
		final Iterator<MyModel> it = list.iterator();
		final ArrayList<MyModel> st = new ArrayList<MyModel>();
		while (it.hasNext()) {
			st.add(it.next());
		}

		return st;

	}

	private void print(String info) {
		if (debug) {
			System.out.println(info);
		}

	}

	public void printFile(String message) {
		if (debug) {
			p.println(message + "\n");
			p.flush();
		}
	}
}
