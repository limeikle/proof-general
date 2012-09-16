package ed.inf.proofgeneral.depgraph.graph;

import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.tools.CellEditorLocator;
import org.eclipse.gef.tools.DirectEditManager;

public class MyDirectEditManager extends DirectEditManager {

	public MyDirectEditManager(GraphicalEditPart arg0, Class<?> arg1,
			CellEditorLocator arg2) {
		super(arg0, arg1, arg2);
		System.out.println("MyDirectEditManager");
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void initCellEditor() {

		System.out.println("initCellEditor()");
		// TODO Auto-generated method stub

	}

}
