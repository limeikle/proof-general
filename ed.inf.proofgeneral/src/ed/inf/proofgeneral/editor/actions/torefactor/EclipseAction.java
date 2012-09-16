/*
 *  $RCSfile: EclipseAction.java,v $
 *
 *  Created on 09 May 2004
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.actions.torefactor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import ed.inf.utils.eclipse.ErrorUI;

public class EclipseAction extends Action implements IWorkbenchWindowActionDelegate {

	/** top-level flag */
	protected boolean topLevelFlag = true;

	/** Error flag. */
	protected boolean errorFlag = false;

	/**
	 * The exception (if any) created by running this action.
	 * Stored here 'cos run can't throw exceptions.
	 */
	protected Exception exception = null;

	// support active editors & selections??
	protected IEditorPart activeEditor = null;

	/**
	 * Gets the last exception, if it exists.
	 * @return the last exception, or null.
	 */
	public Exception getException() {
		return exception;
	}

	/**
	 * Is this a top level action (i.e. did the user call this action)?
	 * @return true by default, or false if set otherwise
	 */
	public boolean isTopLevel() {
		return topLevelFlag;
	}

	/**
	 * Signals that this is not a top-level action (i.e. not called by the user).
	 * @param b false if this is not a top-level action.
	 */
	public void setTopLevel(boolean b) {
		topLevelFlag = b;
	}

	/**
	 * Has there been an error?
	 * @return true if there was an error.
	 */
	public boolean isError() {
		return errorFlag;
	}

	/**
	 * Sets the error flag.
	 * A way of signalling errors, since run can't throw exceptions.
	 * @param b
	 */
	public void setError(boolean b) {
		errorFlag = b;
	}

	/**
	 * @param action ignored.
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
    public void run(IAction action) {
        run();
    }

    /**
     * This will actually carry out the action. Default does nothing.
     */
	@Override
    public void run() { }

	/**
     * @return Returns the activeEditor.
     */
//    public IEditorPart getActiveEditor() {
//        return activeEditor;
//    }
//	public void setActiveEditor(IEditorPart targetEditor) {
//		activeEditor = targetEditor;
//	}
//	protected ISelection selection = null;
//	public void selectionChanged(SelectionChangedEvent event) {
//		selection = event.getSelection();
//	}

	/**
	 * Signal an error (since run can't throw exceptions)
	 * @param e the exception to report;
	 * @param displayDialog whether or not to notify the user.
	 */
	protected void error(Exception e, boolean displayDialog) {
		errorFlag = true;
		exception = e;
		if (!topLevelFlag) {
			return;
		}
		ErrorUI.getDefault().signalError(e, displayDialog);
	}

	/**
	 * Signal an error and display a dialog.
	 * @param e
	 */
	protected void error(Exception e) {
		error(e, true);
	}

	/**
	 * Default does nothing.
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
	 */
    public void dispose() { }

    /**
     * Default does nothing.
     * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
     */
    public void init(IWorkbenchWindow window) { }

    /**
     * Default does nothing.
     * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection) { }

}
