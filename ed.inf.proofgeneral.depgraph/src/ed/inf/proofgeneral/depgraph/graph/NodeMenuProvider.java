package ed.inf.proofgeneral.depgraph.graph;

import java.util.List;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;

public class NodeMenuProvider extends ContextMenuProvider {

	private ActionRegistry actionRegistry;
	GraphicalViewer viewer;
	private Action sampleAction;

	public NodeMenuProvider(EditPartViewer viewer, ActionRegistry registry) {
		super(viewer);
		// setActionRegistry(registry);
		System.out.println("NodeMenuProvider created " + registry.toString());
		this.viewer = (GraphicalViewer) viewer;
		this.actionRegistry = registry;
		setRemoveAllWhenShown(true);
		viewer.setContextMenu(this);

	}

	@Override
	public void buildContextMenu(IMenuManager manager) {
		manager.removeAll();
		GEFActionConstants.addStandardActionGroups(manager);
		// manager.removeAll();
		IAction action;
		if (viewer.getSelectedEditParts().isEmpty()) {
			System.out.println("selection empty");
		} else {
			final List<?> parts = viewer.getSelectedEditParts();
			for (int i = 0; i < parts.size(); i++) {
				final Object node = parts.get(i);
				if (node instanceof MyNodeEditPart) {
					final String name = ((MyNode) ((MyNodeEditPart) node)
							.getModel()).getName();
					System.out.println("node: " + name);
				}

			}
			manager.add(new Separator("actions"));
			// System.out.println("getActionRegistry().toString()
			// "+getActionRegistry().toString());
			action = getActionRegistry().getAction(NodeAction.DEP1_REQUEST);
			((NodeAction) action).setSelectedObjects(viewer
					.getSelectedEditParts());
			manager.add(action);
			action = getActionRegistry().getAction(NodeAction.DEP2_REQUEST);
			((NodeAction) action).setSelectedObjects(viewer
					.getSelectedEditParts());
			manager.add(action);
			action = getActionRegistry().getAction(NodeAction.DEP3_REQUEST);
			((NodeAction) action).setSelectedObjects(viewer
					.getSelectedEditParts());
			manager.add(action);
		}

	}

	private void setActionRegistry(ActionRegistry registry) {

		actionRegistry = registry;
	}

	protected ActionRegistry getActionRegistry() {
		return actionRegistry;
	}

	private void menuAboutToShow() {

		System.out.println("bebbebe");
	}
}
