package ed.inf.proofgeneral.depgraph.graph;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;

public class MyGraphicalEditpartFactory implements EditPartFactory {

	public EditPart createEditPart(EditPart context, Object model) {
		EditPart part = null;
		if (model instanceof Graph) {
			part = new GraphPart();
		}
		if (model instanceof MyModel) {
			part = new MyModelEditPart();
		} else if (model instanceof MyNode) {
			part = new MyNodeEditPart();
		} else if (model instanceof MyConnection) {
			part = new MyConnectionEditPart();
		}
		part.setModel(model);
		return part;
	}

}
