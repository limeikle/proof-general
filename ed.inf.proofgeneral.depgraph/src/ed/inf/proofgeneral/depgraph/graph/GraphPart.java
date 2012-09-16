package ed.inf.proofgeneral.depgraph.graph;

import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

public class GraphPart extends AbstractGraphicalEditPart {

	@Override
	protected IFigure createFigure() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void createEditPolicies() {
		// TODO Auto-generated method stub

	}

	@Override
	protected List<MyModel> getModelChildren() {

		return Graph.getDiagramChildren();
	}

}
