package ed.inf.proofgeneral.depgraph.graph;

import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

public class MyNodeModel {

	protected ArrayList<MyNode> parents = new ArrayList<MyNode>();

	protected ArrayList<MyNode> children = new ArrayList<MyNode>();

	protected PropertyChangeSupport listener;

	final protected static String ADD_PARENT = "add_parent";

	final protected static String ADD_CHILD = "add_child";

	protected ArrayList<MyConnection> incomingConnections = new ArrayList<MyConnection>();

	protected ArrayList<MyConnection> outcomingConnections = new ArrayList<MyConnection>();

	private final String name;

	private final MyModel model;

	public MyNodeModel(String name, MyModel model) {
		this.model = model;
		this.name = name;
		// System.out.println("node created :" + name);
		listener = new PropertyChangeSupport(this);
	}

	public void addParent(MyNode node) {
		// System.out.println("incomingconnection");
		parents.add(node);
		// MyConnection connection = new MyConnection(node);
		// incomingConnections.add(connection);
	}

	public void addChild(MyNode node) {
		// System.out.println("outcomingconnection");
		children.add(node);
		// MyConnection connection = new MyConnection(node);
		// if (!((MyNode) this).isVisible())
		// connection.setVisible(false);
		// outcomingConnections.add(connection);

	}

	public void addOldChild(MyNode node) {
		// System.out.println("outcomingconnection");
		children.add(node);
		// MyConnection connection = new MyConnection(node);
		// if (!((MyNode) this).isVisible())
		// connection.setVisible(false);
		// outcomingConnections.add(connection);

	}

	public List<MyConnection> getOutgoingConnections() {
		return this.outcomingConnections;
	}

	public List<MyConnection> getIncomingConnections() {
		return incomingConnections;
	}

	public String getName() {
		return name;
	}

	public int countChildren() {
		return outcomingConnections.size();
	}

	public ArrayList<MyNode> getParents() {
		return parents;
	}

	public ArrayList<MyNode> getChildren() {
		return children;
	}

	public MyModel getModel() {

		return model;
	}

}
