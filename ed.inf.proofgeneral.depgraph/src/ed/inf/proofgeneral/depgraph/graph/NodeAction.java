package ed.inf.proofgeneral.depgraph.graph;

import java.util.List;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.ui.IWorkbenchPart;

public class NodeAction extends SelectionAction {

	private static final String NODE_REQUEST = "node"; //$NON-NLS-1$

	public static final String NODE = "node";
	public static final String DEP1_REQUEST = "show_descendants";
	public static final String DEP2_REQUEST = "show_ascendants";
	public static final String DEP3_REQUEST = "hide_descendants";

	private final String name;

	Request request;

	private List<EditPart> editParts;

	public NodeAction(IWorkbenchPart part, String name) {
		super(part);
		System.out.println("IWorkbenchPart " + part.toString());
		this.name = name;
		request = new Request(name);
		request.setType(name);
		setText(name);
		setId(name);

	}

	@Override
	protected boolean calculateEnabled() {
		return canPerformAction();
	}

	private boolean canPerformAction() {
		return true;
	}

	private Command getCommand() {
		final List<EditPart> editparts = getSelectedObjects();
		final CompoundCommand cc = new CompoundCommand();
		for (int i = 0; i < editparts.size(); i++) {
			final EditPart part = editparts.get(i);
			cc.add(part.getCommand(request));
			part.performRequest(request);
			System.out.println("request: " + part.getModel().toString() + "-->"
					+ request.getType());
		}
		return cc;
	}

	@Override
	public void run() {
		execute(getCommand());
	}

	public void setSelectedObjects(List<EditPart> editParts) {
		this.editParts = editParts;
	}

	@Override
	public List<EditPart> getSelectedObjects() {
		return editParts;

	}
	//
}
