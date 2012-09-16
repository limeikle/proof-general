package ed.inf.proofgeneral.depgraph.graph;

import java.util.List;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.ui.IWorkbenchPart;

public class ShowDependencies extends SelectionAction {

	private static final String DEP1_REQUEST = "dep1"; //$NON-NLS-1$

	public static final String NODE = "node";

	Request request;

	public ShowDependencies(IWorkbenchPart part) {
		super(part);
		request = new Request(DEP1_REQUEST);
		setText("show dependencies");
		setId(NODE);

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
		// cc.setDebugLabel("Increment/Decrement LEDs");//$NON-NLS-1$
		for (int i = 0; i < editparts.size(); i++) {
			final EditPart part = editparts.get(i);
			cc.add(part.getCommand(request));
		}
		return cc;
	}

	@Override
	public void run() {
		execute(getCommand());
	}

}
