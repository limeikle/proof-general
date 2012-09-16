package ed.inf.proofgeneral.depgraph.graph;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackListener;

class MultiPageCommandStackListener implements CommandStackListener {
	private final GraphMultiPageEditor editor;

	/** the observed command stacks */
	private final List<CommandStack> commandStacks = new ArrayList<CommandStack>();

	public MultiPageCommandStackListener(GraphMultiPageEditor editor) {
		this.editor = editor;

	}

	/**
	 * Adds a <code>CommandStack</code> to observe.
	 * 
	 * @param commandStack
	 */
	public void addCommandStack(CommandStack commandStack) {
		commandStacks.add(commandStack);
		commandStack.addCommandStackListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.CommandStackListener#commandStackChanged(java.util.EventObject)
	 */
	public void commandStackChanged(EventObject event) {
		if (((CommandStack) event.getSource()).isDirty()) {
			// at least one command stack is dirty,
			// so the multi page editor is dirty too
			setDirty(true);
		} else {
			// probably a save, we have to check all command stacks
			boolean oneIsDirty = false;
			for (final CommandStack stack : commandStacks) {
				if (stack.isDirty()) {
					oneIsDirty = true;
					break;
				}
			}
			setDirty(oneIsDirty);
		}
	}

	protected void setDirty(boolean b) {
		// TODO Auto-generated method stub
		editor.setDirty(b);
	}

	/**
	 * Disposed the listener
	 */
	public void dispose() {
		for (final CommandStack commandStack : commandStacks) {
			commandStack.removeCommandStackListener(this);
		}
		commandStacks.clear();
	}

	/**
	 * Marks every observed command stack beeing saved. This method should be
	 * called whenever the editor/model was saved.
	 */
	public void markSaveLocations() {
		for (final CommandStack stack : commandStacks) {
			stack.markSaveLocation();
		}
	}
}
