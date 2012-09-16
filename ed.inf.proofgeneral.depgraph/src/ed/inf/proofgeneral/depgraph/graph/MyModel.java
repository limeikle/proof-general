package ed.inf.proofgeneral.depgraph.graph;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class MyModel {

	private final MyLayOut layout = new MyLayOut();

	private String text = "Hello";

	private final boolean debug = true;

	private FileOutputStream fileWriter;

	private PrintStream p;

	private final MyNode treeHead;

	private final HashMap<String, MyNode> map = new HashMap<String, MyNode>();

	public MyModel(String name) {

		treeHead = new MyNode(name, layout, this);
		addNode(treeHead);
		setText(name);

	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	private String getRootName(String msg) {
		final String result = msg.substring(msg.indexOf("thms") + 7, msg
				.indexOf("/value") - 2);
		return result;
	}

	private MyNode createRoot(String rootName) {

		final MyNode result = new MyNode(rootName, layout, this);
		print("root: " + rootName);
		return result;
	}

	private void print(String info) {
		if (debug) {
			System.out.println(info);
		}
		p.println(info + "\n");
		p.flush();

	}

	public List<MyNode> getChildren() {

		final Collection<MyNode> list = map.values();
		final Iterator<MyNode> it = list.iterator();
		final ArrayList<MyNode> st = new ArrayList<MyNode>();
		while (it.hasNext()) {
			st.add(it.next());
		}
		// System.out.println("getChildren()");
		return st;
	}

	public void addNode(MyNode node) {
		map.put(node.getName(), node);
	}

	public MyNode getNode(String name) {
		return map.get(name);
	}

	public void deleteNode(int i) {
		final MyNode node = map.get("DependencyTree");
		// System.out.println("delete node 0");
		// node.deleteNode();

	}

	public boolean evaluate() {
		treeHead.collapseConnections();
		treeHead.setInvisible();
		return true;
	}

	public boolean evaluate(String name) {

		final MyNode node = map.get(name);
		final ArrayList<MyNode> parents = node.parents;
		final Iterator<MyNode> it = parents.iterator();
		while (it.hasNext()) {
			final MyNode parent = it.next();
			if ((node.getLevel() - parent.getLevel()) > 1) {
				final Iterator<MyNode> ip = parents.iterator();
				while (ip.hasNext()) {
					final MyNode child = ip.next();
					if (child.getLevel() > parent.getLevel()
							&& child.parents.contains(parent)) {
						parent.setOpaqueConnx(parent, node);
						break;
					}

				}

			}

		}

		return true;
	}

	public void addChild(String rootName, String child) {
		if (map.containsKey(rootName)) {
			final MyNode oldRoot = map.get(rootName);
			if (map.containsKey(child)) {
				final MyNode oldChild = map.get(child);
				oldRoot.addOldChild(oldChild);
			} else {
				final MyNode node = new MyNode(child, layout, this);
				oldRoot.addChild(node);
				map.put(node.getName(), node);
			}
		} else {
			final MyNode newRoot = new MyNode(rootName, layout, this);
			treeHead.addChild(newRoot);
			map.put(rootName, newRoot);
			if (map.containsKey(child)) {
				final MyNode oldChild = map.get(child);
				newRoot.addOldChild(oldChild);
			} else {
				final MyNode node = new MyNode(child, layout, this);
				newRoot.addChild(node);
				map.put(node.getName(), node);
			}
		}
	}

	public MyLayOut getLayout() {

		return layout;
	}

}

/*
 * private void check(MyNode node1, MyNode node2) {
 * System.out.println(node1.getName()+"-"+node2.getName()); for(int
 * i=node1.getLevel()+1; i<node2.getLevel(); i++) { ArrayList
 * nodes=layout.getLevelNodes(i); Iterator it = nodes.iterator(); while
 * (it.hasNext()) { MyNode checker = (MyNode) it.next();
 * if(node1.children.contains(checker)&&node2.parents.contains(checker))
 * node1.removeConnectionTo(node2); } } } }
 */
/*
 * public boolean evaluate5() { System.out.println("evaluate model");
 * 
 * Iterator iter = treeHead.getChildren().iterator(); while (iter.hasNext()) //
 * if (!((MyNode) iter.next()).evaluateTriangles()) // return false;
 * 
 * return true; }
 */
/*
 * public void evaluate6() { evaluate6(); for (int i = layout.size() - 1; i >=
 * 0; i--) {
 * 
 * ArrayList list1 = layout.getLevelNodes(i); Iterator it = list1.iterator();
 * while (it.hasNext()) { MyNode node = (MyNode) it.next(); ArrayList parents =
 * node.getParents(); for (int j = 0; j < parents.size(); j++) { if (((MyNode)
 * parents.get(j)).getLevel() == (node.getLevel() - 1)) { check((MyNode)
 * parents.get(j),node); } } } } }
 */
