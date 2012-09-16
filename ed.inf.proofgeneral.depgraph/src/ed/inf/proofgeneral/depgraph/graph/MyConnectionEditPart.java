package ed.inf.proofgeneral.depgraph.graph;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.draw2d.BendpointConnectionRouter;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;

public class MyConnectionEditPart extends AbstractConnectionEditPart implements
		PropertyChangeListener {
	private final boolean debug = true;

	@Override
	protected void createEditPolicies() {

	}

	@Override
	protected void refreshVisuals() {
		final PolylineConnection figure = (PolylineConnection) getFigure();
		// figure.setOpaque(false);
		final PolygonDecoration dec = new PolygonDecoration();
		dec.setScale(20, 5);

		figure.setTargetDecoration(dec);
		// figure.setConnectionRouter(new ManhattanConnectionRouter());
		figure.setConnectionRouter(new BendpointConnectionRouter());

		final MyConnection connx = (MyConnection) getModel();
		if (connx.getSelection()) {
			figure.setForegroundColor(ColorConstants.lightGreen);
			// else
			// figure.setBackgroundColor(ColorConstants.lightBlue);
		} else {
			figure.setForegroundColor(ColorConstants.lightBlue);
		}
		// figure.setVisible(connx.getVisible());
		if (!connx.getVisible()) {
			// figure.setOpaque(false);
			figure.setVisible(false);
		}
	}

	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals(MyConnection.PROPERTY_VSIBILITY)) {
			final MyConnection connx = (MyConnection) getModel();
			if (connx.getVisible() == true) {
				connx.setInvisible();
			} else if (connx.getVisible() == false) {
				connx.setVisible();
			}
			refreshVisuals();
			// print("propertyChange for Connection");
		}
		if (evt.getPropertyName().equals(MyConnection.PROPERTY_COLOR)) {
			print("propertyColor for Connection");
			final PolylineConnection figure = (PolylineConnection) getFigure();
			if (evt.getNewValue().equals(ColorConstants.lightGreen.toString())) {
				;
			}
			figure.setForegroundColor(ColorConstants.lightGreen);
			if (evt.getNewValue().equals(ColorConstants.red.toString())) {
				figure.setForegroundColor(ColorConstants.red);
			}
			if (evt.getNewValue().equals(ColorConstants.white.toString())) {
				figure.setForegroundColor(ColorConstants.white);
			}
		}
	}

	@Override
	public void activate() {
		final MyConnection connx = (MyConnection) getModel();
		connx.addPropertyChangeListener(this);

		super.activate();
	}

	@Override
	public void deactivate() {
		final MyConnection connx = (MyConnection) getModel();
		connx.removePropertyChangeListener(this);
		super.deactivate();
	}

	private void print(String string) {
		if (debug) {
			System.out.println(string);
		}

	}
}
