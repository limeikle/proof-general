package ed.inf.proofgeneral.depgraph.graph;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

public class MyNodeEditPart extends AbstractGraphicalEditPart implements
		NodeEditPart, PropertyChangeListener {

	private final boolean debug = true;

	private Object manager;

	@Override
	protected IFigure createFigure() {
		final Label label = new Label();
		return label;
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.DIRECT_EDIT_ROLE, new SelectionPolicy());

	}

	@Override
	protected List<MyConnection> getModelSourceConnections() {
		final MyNode node = (MyNode) getModel();
		return node.getOutgoingConnections();
	}

	@Override
	protected List<MyConnection> getModelTargetConnections() {
		final MyNode node = (MyNode) getModel();
		return node.getIncomingConnections();
	}

	@Override
	protected void refreshVisuals() {
		final MyNode node = (MyNode) getModel();
		node.unHide();
		// node.evaluateTriangles();
		final Label label = (Label) getFigure();
		// label.setOpaque(node.getOpaque());
		// System.out.println("selection="+node.getSelection());
		label.setBackgroundColor(ColorConstants.lightBlue);
		final String name = node.getName();
		if (name.startsWith(node.getModel().getText())) {
			label.setText(name.substring(name.indexOf(".") + 1));
		} else {
			label.setText(name);
		}
		// label.getSize();
		label.setOpaque(node.isVisible());
		label.setVisible(node.isVisible());
		label.setBorder(new LineBorder());
		label.setSize(label.getPreferredSize());
		final Rectangle r = new Rectangle(node.getX(), node.getY(), -1, -1);
		((GraphicalEditPart) getParent()).setLayoutConstraint(this, label, r);
		// print("label " + node.getName() + " size " + label.getSize()
		// + " right bounds: " + label.getBounds().right());

		node.setSize(label.getSize().width);

	}

	public ConnectionAnchor getSourceConnectionAnchor(
			ConnectionEditPart connection) {

		// System.out.println("sourceAnchor created for ");
		return new ChopboxAnchor(getFigure());
	}

	public ConnectionAnchor getSourceConnectionAnchor(Request request) {

		return new ChopboxAnchor(getFigure());
	}

	public ConnectionAnchor getTargetConnectionAnchor(
			ConnectionEditPart connection) {

		// System.out.println("targetAnchor created");
		return new ChopboxAnchor(getFigure());
	}

	public ConnectionAnchor getTargetConnectionAnchor(Request request) {

		return new ChopboxAnchor(getFigure());
	}

	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals(MyNode.PROPERTY_VISIBILITY)) {
			refreshChildren();
		}
		if (evt.getPropertyName().equals(MyNode.PROPERTY_COLOR)) {
			System.out.println("change_color " + evt.getNewValue());
			final Label label = (Label) getFigure();
			if (evt.getNewValue().equals(ColorConstants.lightGreen.toString())) {
				;
			}
			label.setBackgroundColor(ColorConstants.lightGreen);
			if (evt.getNewValue().equals(ColorConstants.red.toString())) {
				label.setBackgroundColor(ColorConstants.red);
			}
			if (evt.getNewValue().equals(ColorConstants.white.toString())) {
				label.setVisible(false);
				System.out.println("hide node: " + label.getText());
			}
		}

		// print("propertyChange for Node");

	}

	@Override
	public void activate() {

		final MyNode node = (MyNode) getModel();
		node.addPropertyChangeListener(this);

		super.activate();
	}

	@Override
	public void deactivate() {
		final MyNode node = (MyNode) getModel();
		node.removePropertyChangeListener(this);
		super.deactivate();
	}

	private void print(String string) {
		if (debug) {
			System.out.println(string);
		}

	}

	@Override
	public void performRequest(Request request) {
		System.out.println("req=" + request.getType());

		if (request.getType() == RequestConstants.REQ_SELECTION) {
			;
		}
		if (request.getType() == RequestConstants.REQ_OPEN) {
			final MyNode node = (MyNode) getModel();
			System.out.println("node.showDependencies();");
			node.showDependencies();

		}
		if (request.getType() == NodeAction.DEP1_REQUEST) {
			final MyNode node = (MyNode) getModel();
			System.out.println("node.showDependencies();");
			node.showDependencies();

		}
		if (request.getType() == NodeAction.DEP2_REQUEST) {
			final MyNode node = (MyNode) getModel();
			System.out.println("node.showDependants();");
			node.showDependants();

		}
		if (request.getType() == NodeAction.DEP3_REQUEST) {
			final MyNode node = (MyNode) getModel();
			System.out.println("node.hideNode;");
			node.hideDependencies();

		}
	}

}
