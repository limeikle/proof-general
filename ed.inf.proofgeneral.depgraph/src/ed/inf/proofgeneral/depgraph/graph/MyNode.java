package ed.inf.proofgeneral.depgraph.graph;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.swt.graphics.Color;

public class MyNode extends MyNodeModel {

	private boolean visible = true;

	private final MyLayOut layout;

	private static int MAX_X = 0;

	private static int MAX_Y = 0;

	public final static String PROPERTY_COLOR = "property_color";

	public final static String PROPERTY_VISIBILITY = "property_visibility";

	private final PropertyChangeSupport listeners;

	private int level;

	private int x;

	private int y;

	private final boolean debug = true;

	private int size = 0;

	private final boolean selection = false;

	private final int color = 0;

	private boolean hide = false;

	public MyNode(String name, MyLayOut layout, MyModel model) {
		super(name, model);
		this.layout = layout;
		listeners = new PropertyChangeSupport(this);

	}

	@Override
	public void addChild(MyNode node) {

		super.addChild(node);
		node.addParent(this);

		final MyConnection connection = new MyConnection(this, node);
		this.outcomingConnections.add(connection);
		node.incomingConnections.add(connection);
		layout.addNode(node);
		// setVisible(visible);
	}

	@Override
	public void addOldChild(MyNode node) {

		super.addOldChild(node);

		node.addParent(this);
		final MyConnection connection = new MyConnection(this, node);
		this.outcomingConnections.add(connection);
		node.incomingConnections.add(connection);

		layout.addOldNode(node);
		// setVisible(visible);

	}

	public void setVisible() {
		if (this.visible != true) {
			this.visible = true;
			listeners.firePropertyChange(this.PROPERTY_VISIBILITY, false, true);
			System.out.println("node is visible: " + this.getName());

			Iterator<MyConnection> it = this.getIncomingConnections()
					.iterator();
			while (it.hasNext()) {
				(it.next()).setVisible();
			}
			it = this.getOutgoingConnections().iterator();
			while (it.hasNext()) {
				(it.next()).setVisible();
			}

		}

	}

	public void setInvisible() {
		if (this.visible != false) {
			this.visible = false;
			listeners.firePropertyChange(this.PROPERTY_VISIBILITY, true, false);
			System.out.println("node is invisible: " + this.getName());
			Iterator<MyConnection> it = this.getIncomingConnections()
					.iterator();
			while (it.hasNext()) {
				(it.next()).setInvisible();
			}
			it = this.getOutgoingConnections().iterator();
			while (it.hasNext()) {
				(it.next()).setInvisible();
			}

		}

	}

	public void deleteNode() {
		collapseConnections();
		setInvisible();

	}

	public void collapseConnections() {
		Iterator<MyConnection> it = this.getIncomingConnections().iterator();
		while (it.hasNext()) {
			(it.next()).setInvisible();
		}
		it = this.getOutgoingConnections().iterator();
		while (it.hasNext()) {
			(it.next()).setInvisible();
		}

	}

	public void collapseNode(MyNode node) {
		node.collapseConnections();
		final ArrayList<MyNode> children = node.children;
		final Iterator<MyNode> it = children.iterator();
		while (it.hasNext()) {

		}

	}

	public boolean isVisible() {

		return visible;
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		listeners.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		listeners.removePropertyChangeListener(listener);
	}

	private void print(String string) {
		if (debug) {
			System.out.println(string);
		}

	}

	public void removeConnectionTo(MyNode node) {
		final Iterator<MyConnection> it = this.outcomingConnections.iterator();
		while (it.hasNext()) {
			final MyConnection connx = it.next();
			if ((connx).getTarget() == node) {
				if (this.children.contains(node)) {
					this.children.remove(node);
				}
				if (this.outcomingConnections.contains(connx)) {
					this.outcomingConnections.remove(connx);
				}
				if (node.parents.contains(this)) {
					node.parents.remove(this);
				}
				if (node.incomingConnections.contains(connx)) {
					node.incomingConnections.remove(connx);
				}
			}
		}

	}

	public void setSize(int i) {
		size = i;

	}

	public int getSize() {
		return size;

	}

	public void showDependencies() {
		print("showdependencies: " + this.getName());
		this.changeColor(ColorConstants.lightGreen);
		final ArrayList<MyNode> deps = this.getChildren();
		final Iterator<MyNode> it = deps.iterator();
		while (it.hasNext()) {
			final MyNode dep = (it.next());
			dep.changeColor(ColorConstants.lightGreen);
			final List<?> connx = dep.getIncomingConnections();
			final Iterator<?> itr = connx.iterator();
			while (itr.hasNext()) {
				final MyConnection con = ((MyConnection) itr.next());
				if (deps.contains(con.getSource())) {
					con.changeColor(ColorConstants.lightGreen);
				}
			}

		}
		final List<MyConnection> connx = this.getOutgoingConnections();
		final Iterator<MyConnection> itr = connx.iterator();
		while (itr.hasNext()) {
			(itr.next()).changeColor(ColorConstants.lightGreen);
		}
	}

	public void showDependants() {
		print("showdependants: " + this.getName());
		this.changeColor(ColorConstants.red);
		final ArrayList<MyNode> deps = this.getParents();
		final Iterator<MyNode> it = deps.iterator();
		while (it.hasNext()) {
			final MyNode dep = (it.next());
			dep.changeColor(ColorConstants.red);

			final List<MyConnection> connx = dep.getOutgoingConnections();
			final Iterator<MyConnection> itr = connx.iterator();
			while (itr.hasNext()) {
				final MyConnection con = (itr.next());
				if (deps.contains(con.getTarget())) {
					con.changeColor(ColorConstants.red);
				}
			}

		}
		final List<MyConnection> connx = this.getIncomingConnections();
		final Iterator<MyConnection> itr = connx.iterator();
		while (itr.hasNext()) {
			(itr.next()).changeColor(ColorConstants.red);
		}
	}

	private void changeColor(Color color) {
		if (this.visible != false) {
			listeners.firePropertyChange(this.PROPERTY_COLOR,
					ColorConstants.lightBlue, color.toString());
		}

	}

	public void hideDependencies() {
		print("hide dependencies: " + this.getName());
		final ArrayList<MyNode> deps = new ArrayList<MyNode>();

		final List<MyConnection> connx = this.getOutgoingConnections();
		final Iterator<MyConnection> itr = connx.iterator();
		while (itr.hasNext()) {
			final MyConnection currentConnx = itr.next();
			if (currentConnx.getVisible()) {
				deps.add(currentConnx.getTarget());
			}
			currentConnx.changeColor(ColorConstants.white);
		}
		this.hide = true;
		final Iterator<MyNode> it1 = deps.iterator();
		while (it1.hasNext()) {
			final MyNode child = (it1.next());
			child.hide = true;
		}

		final Iterator<MyNode> it2 = deps.iterator();
		while (it2.hasNext()) {
			final MyNode child = (it2.next());
			if (child.hidedParents()) {
				child.changeColor(ColorConstants.white);

			} else {
				child.hide = false;
			}
		}
		final Iterator<MyNode> it3 = deps.iterator();
		while (it3.hasNext()) {
			final MyNode child = (it3.next());
			if (child.hide) {
				child.hideDependencies();
			}

		}

	}

	protected boolean hidedParents() {
		final ArrayList<MyNode> parents = new ArrayList<MyNode>();
		final List<MyConnection> connx = getIncomingConnections();
		final Iterator<MyConnection> itr = connx.iterator();
		while (itr.hasNext()) {
			final MyConnection currentConnx = itr.next();
			if (currentConnx.getVisible()) {
				parents.add(currentConnx.getSource());
			}
		}
		final Iterator<MyNode> it = parents.iterator();
		while (it.hasNext()) {
			final MyNode parent = it.next();
			if (!parent.hide) {
				return false;
			}
		}
		return true;
	}

	protected boolean getSelection() {
		return selection;
	}

	public void setLevel(int number) {
		level = number;
	}

	public int getLevel() {
		return level;
	}

	public void setX(int number) {
		x = number;
	}

	public int getX() {
		return x;
	}

	public void setY(int number) {
		y = number;
	}

	public int getY() {
		return y;
	}

	public void setOpaqueConnx(MyNode node1, MyNode node2) {
		final ArrayList<MyConnection> cons = (ArrayList<MyConnection>) node1
				.getOutgoingConnections();
		final Iterator<MyConnection> it = cons.iterator();
		while (it.hasNext()) {
			final MyConnection currentConnx = it.next();
			if (currentConnx.getSource().equals(node1)
					&& currentConnx.getTarget().equals(node2)) {
				currentConnx.setInvisible();
				System.out.println("connection: " + node1.getName() + " to "
						+ node2.getName() + " is invisible");
			}

		}

	}

	protected void unHide() {
		this.hide = false;
	}

}
/*
 * private boolean connected(MyNode node) { ArrayList children = getChildren();
 * Iterator it = children.iterator(); while (it.hasNext()) { MyNode child =
 * (MyNode) it.next(); if (child.children.contains(node)) { //
 * print(this.getName() + " is connected with " + // node.getName()); return
 * true; } else return child.connected(node); } // print(this.getName() + " is
 * not connected with " + node.getName()); // TODO Auto-generated method stub
 * return false; }
 * 
 * public void evaluateConnections1(MyNode node) { // System.out.println("this=" +
 * this.getName()); ArrayList parents = getParents(); Iterator it =
 * parents.iterator(); while (it.hasNext()) { MyNode parent = (MyNode)
 * it.next(); // System.out.println("parent=" + parent.getName()); if
 * (parent.children.contains(node)) { print(parent.getName() + " is connected!!!
 * with " + node.getName()); parent.removeConnectionTo(node); //
 * node.removeConnectionFrom(parent); print(parent.getName() + " evaluated from " +
 * node.getName()); }
 * 
 * parent.evaluateConnections1(node); } }
 * 
 * public boolean evaluateConnections2(MyNode node) { MyNode root = this; //
 * System.out.println("this=" + this.getName()); ArrayList children =
 * node.getChildren(); Iterator it = children.iterator(); while (it.hasNext()) {
 * MyNode child = (MyNode) it.next(); // System.out.println("parent=" +
 * child.getName()); if (child.getParents().contains(root)) {
 * root.removeConnectionTo(child); print(root.getName() + " is connected!!! with " +
 * child.getName());
 * 
 * print(root.getName() + " evaluated to " + child.getName()); return false; }
 * ArrayList nodeChildren = child.getChildren(); Iterator it2 =
 * nodeChildren.iterator(); while (it.hasNext()) { MyNode nodeChild = (MyNode)
 * it2.next(); root.evaluateConnections2(nodeChild); } } // print(this.getName() + "
 * is not !!!! connected with " + // node.getName()); // TODO Auto-generated
 * method stub return true; }
 */
/*
 * public boolean evaluateTriangles() { System.out.println("evaluate triangles
 * for: " + this.getName()); // MyNode root = this; ArrayList children =
 * this.getChildren(); Iterator it1 = children.iterator(); while (it1.hasNext()) {
 * MyNode child = (MyNode) it1.next(); if (!eval(child)) return false; //
 * child.evaluateTriangles(); } System.out.println("end for " + this.getName());
 * return true; }
 */

/*
 * public boolean eval(MyNode target) { boolean result = true;
 * 
 * System.out.println("evaluate triangles for: " + this.getName() + " " +
 * target.getName()); ArrayList children = target.getChildren(); Iterator it =
 * children.iterator(); while (it.hasNext()) { MyNode child = (MyNode)
 * it.next();
 * 
 * if (child.getParents().contains(this)) { System.out.println("remove: " +
 * this.getName() + " " + child.getName()); // this.removeConnectionTo(child); //
 * this.collapseConnections(); return false; } }
 * 
 * return true; }
 */

/*
 * public void removeConnectionFrom(MyNode node) { Iterator it =
 * this.incomingConnections.iterator(); while (it.hasNext()) { MyConnection
 * connx = (MyConnection) it.next(); if ((connx).getSource() == node) if (
 * this.children.contains(node)) this.children.remove(node);
 * this.incomingConnections.remove(connx); node.parents.remove(this);
 * node.outcomingConnections.remove(connx); } }
 */

