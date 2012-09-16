package ed.inf.proofgeneral.depgraph.graph;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.swt.graphics.Color;

public class MyConnection {

	static final String PROPERTY_COLOR = "property_color";

	private boolean visible = true;

	private final MyNode source;

	private final MyNode target;

	public static String PROPERTY_VSIBILITY = "property_visibility";

	private PropertyChangeSupport listeners;

	private final boolean selection = false;

	public MyConnection(MyNode node1, MyNode node2) {
		source = node1;
		target = node2;

		if (listeners == null) {
			listeners = new PropertyChangeSupport(this);
		}
	}

	public boolean getVisible() {
		// TODO Auto-generated method stub
		return visible;
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		listeners.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		listeners.removePropertyChangeListener(listener);
	}

	public void setVisible() {
		if (visible != true) {
			visible = true;
			// System.out.println("Connection is visible: ");
			listeners.firePropertyChange(this.PROPERTY_VSIBILITY, false, true);
		}
	}

	public void setInvisible() {
		// TODO Auto-generated method stub
		if (visible != false) {
			visible = false;
			System.out.println("Connection is invisible: ");
			listeners.firePropertyChange(this.PROPERTY_VSIBILITY, true, false);
		}
	}

	public void changeColor(Color color) {
		if (this.visible != false) {
			listeners.firePropertyChange(this.PROPERTY_COLOR,
					ColorConstants.lightBlue, color.toString());
		}

	}

	protected boolean getSelection() {
		return selection;
	}

	public MyNode getSource() {
		return source;
	}

	public MyNode getTarget() {
		return target;

	}
}
