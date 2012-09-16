package ed.inf.proofgeneral.depgraph.graph;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

public class GraphView extends EditorPart {

	private EditDomain editDomain;

	private GraphicalViewer graphicalViewer;

	private final MyGraphicalEditpartFactory editpartFactory = new MyGraphicalEditpartFactory();

	private final boolean debug = true;

	private final GraphMultiPageEditor editor;

	/**
	 * Constructs the editor part
	 * 
	 * @param editor
	 */
	public GraphView(GraphMultiPageEditor editor, EditDomain editDomain) {

		setEditDomain(editDomain);
		this.editor = editor;
	}

	/**
	 * Called to configure the graphical viewer before it receives its contents.
	 * This is where the root editpart should be configured. Subclasses should
	 * extend or override this method as needed.
	 */
	protected void configureGraphicalViewer() {

		getGraphicalViewer().getControl().setBackground(
				ColorConstants.listBackground);
		getGraphicalViewer().setEditPartFactory(editpartFactory);

	}

	/**
	 * Creates the GraphicalViewer on the specified <code>Composite</code>.
	 * 
	 * @param parent
	 *            the parent composite
	 * @return
	 */
	protected GraphicalViewer createGraphicalViewer(Composite parent) {
		if (getGraphicalViewer() == null) {
			final GraphicalViewer viewer = new ScrollingGraphicalViewer();
			setGraphicalViewer(viewer);
			print("setGraphicalViewer()");
			viewer.createControl(parent);
			configureGraphicalViewer();
			hookGraphicalViewer();
			initializeGraphicalViewer();
		}
		return getGraphicalViewer();
	}

	/**
	 * Realizes the Editor by creating it's Control.
	 * <P>
	 * WARNING: This method may or may not be called by the workbench prior to
	 * {@link #dispose()}.
	 * 
	 * @param parent
	 *            the parent composite
	 */
	@Override
	public void createPartControl(Composite parent) {
		createGraphicalViewer(parent);

	}

	/**
	 * /** Returns the edit domain.
	 * 
	 * @return the edit domain
	 */
	protected EditDomain getEditDomain() {
		return editDomain;
	}

	/**
	 * Returns the graphical viewer.
	 * 
	 * @return the graphical viewer
	 */
	protected GraphicalViewer getGraphicalViewer() {
		return graphicalViewer;
	}

	/**
	 * Override to set the contents of the GraphicalViewer after it has been
	 * created.
	 * 
	 * @see #createGraphicalViewer(Composite)
	 */
	protected void initializeGraphicalViewer() {
	}

	/**
	 * Sets the EditDomain for this EditorPart.
	 * 
	 * @param ed
	 *            the domain
	 */
	protected void setEditDomain(EditDomain ed) {
		this.editDomain = ed;
	}

	/**
	 * @see org.eclipse.ui.IWorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		getGraphicalViewer().getControl().setFocus();
	}

	/**
	 * Hooks the GraphicalViewer to the rest of the Editor. By default, the
	 * viewer is added to the SelectionSynchronizer, which can be used to keep 2
	 * or more EditPartViewers in sync. The viewer is also registered as the
	 * ISelectionProvider for the Editor's PartSite.
	 */
	protected void hookGraphicalViewer() {

		// print("hookGraphicalViewer()");
		if (getGraphicalViewer() == null) {
			print("getGraphicalViewer()==null");
		}
		editor.getSelectionSynchronizer().addViewer(getGraphicalViewer());
		editor.getSite().setSelectionProvider(getGraphicalViewer());
	}

	/**
	 * Sets the graphicalViewer for this EditorPart.
	 * 
	 * @param viewer
	 *            the graphical viewer
	 */
	protected void setGraphicalViewer(GraphicalViewer viewer) {
		getEditDomain().addViewer(viewer);
		this.graphicalViewer = viewer;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isSaveAsAllowed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setSite(site);
		setInput(input);

		// TODO Auto-generated method stub

	}

	@Override
	public boolean isDirty() {
		// TODO Auto-generated method stub
		return false;
	}

	protected MyGraphicalEditpartFactory getEditpartFactory() {
		return editpartFactory;
	}

	public void showDependencyTree(MyModel model) {

		System.out.println("show dep graph");
		if (model != null) {
			System.out.println("Start set Content");
			model.evaluate();
			getGraphicalViewer().setContents(model);
			System.out.println("finish set Content");
		} else {
			System.out.println("tree null");
			// int index=addPage(getGraphicalViewer().getControl());
		}
	}

	private void print(String msg) {
		if (debug) {
			System.out.println(msg);
		}

	}

	public void changeDependencyTree(MyModel model) {
		model.deleteNode(0);

	}

}
