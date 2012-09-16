package ed.inf.proofgeneral.depgraph.graph;

import java.util.List;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.XYLayout;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

public class MyModelEditPart extends AbstractGraphicalEditPart {
	@Override
	protected IFigure createFigure() {
		final Figure f = new Figure();
		f.setOpaque(true);
		// f.setLayoutManager((LayoutManager) new DirectedGraphLayout());
		f.setLayoutManager(new XYLayout());
		// f.setLayoutManager(new FlowLayout());
		// DirectedGraphLayout dd=new DirectedGraphLayout();
		// dd.visit(new MyModel());
		// f.setLayoutManager(new XYLayout());
		// f.setLayoutManager((LayoutManager) layout);
		// f.setLayoutManager(new ScrollBarLayout());

		return f;
	}

	@Override
	protected void createEditPolicies() {

	}

	@Override
	protected List<MyNode> getModelChildren() {
		// System.out.println("getModelChildren())");
		final MyModel model = (MyModel) getModel();
		return model.getChildren();

	}

}
