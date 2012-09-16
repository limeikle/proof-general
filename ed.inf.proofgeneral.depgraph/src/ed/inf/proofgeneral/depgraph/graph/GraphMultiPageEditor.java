package ed.inf.proofgeneral.depgraph.graph;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.gef.ui.actions.ActionBarContributor;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.gef.ui.actions.EditorPartAction;
import org.eclipse.gef.ui.actions.PrintAction;
import org.eclipse.gef.ui.actions.RedoAction;
import org.eclipse.gef.ui.actions.SaveAction;
import org.eclipse.gef.ui.actions.SelectAllAction;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.gef.ui.actions.StackAction;
import org.eclipse.gef.ui.actions.UndoAction;
import org.eclipse.gef.ui.actions.UpdateAction;
import org.eclipse.gef.ui.parts.SelectionSynchronizer;
import org.eclipse.gef.ui.properties.UndoablePropertySheetEntry;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.PropertySheetPage;

import ed.inf.proofgeneral.document.outline.PGContentOutlinePage;

/**
 * 
 */
public class GraphMultiPageEditor extends MultiPageEditorPart implements
		IResourceChangeListener, CommandStackListener, ISelectionListener {

	public static PGContentOutlinePage fOutlinePage = null;

	private static GraphMultiPageEditor graph = null;

	private EditDomain editDomain;

	private ActionRegistry actionRegistry;

	private SelectionSynchronizer synchronizer;

	private boolean isDirty;

	private final List<String> nodeActions = new ArrayList<String>();

	private final List<String> editPartActionIDs = new ArrayList<String>();

	private final List<String> stackActionIDs = new ArrayList<String>();

	private final List<String> selectionActions = new ArrayList<String>();

	private final List<String> stackActions = new ArrayList<String>();

	private final List<String> propertyActions = new ArrayList<String>();

	private final List<String> editorActionIDs = new ArrayList<String>();

	private final boolean debug = true;

	private final ArrayList<GraphView> viewers = new ArrayList<GraphView>();

	/**
	 * This class listens for command stack changes of the pages contained in
	 * this editor and decides if the editor is dirty or not.
	 * 
	 * @author Gunnar Wagenknecht
	 */

	private final MultiPageCommandStackListener commandStackListener = new MultiPageCommandStackListener(
			this);

	/*
	 * private CommandStackListener commandStackListener = new
	 * CommandStackListener() {
	 * 
	 * public void commandStackChanged(EventObject event) {
	 * updateActions(stackActionIDs); setDirty(getCommandStack().isDirty()); } };
	 */
	private final ISelectionListener selectionListener = new ISelectionListener() {

		public void selectionChanged(IWorkbenchPart part, ISelection selection) {
			// System.out.println("update selection");
			// if (this.equals(getSite().getPage().getActiveEditor()))
			part.setFocus();
			updateActions(selectionActions);
			updateActions(nodeActions);

		}

	};

	/**
	 * Creates a multi-page editor .
	 */
	public GraphMultiPageEditor() {
		super();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
		graph = this;
		getEditDomain();
	}

	@Override
	public boolean isDirty() {
		return isDirty;
	}

	protected void setDirty(boolean b) {

		if (isDirty != b) {
			isDirty = b;
			firePropertyChange(IEditorPart.PROP_DIRTY);
		}
	}

	/**
	 * Saves the multi-page editor's document.
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
		// getEditor(0).doSave(monitor);
		getCommandStack().markSaveLocation();
	}

	/**
	 * Saves the multi-page editor's document as another file. Also updates the
	 * text for page 0's tab, and updates this multi-page editor's input to
	 * correspond to the nested editor's.
	 */
	@Override
	public void doSaveAs() {
		final IEditorPart editor = getEditor(0);
		editor.doSaveAs();
		setPageText(0, editor.getTitle());
		setInput(editor.getEditorInput());
		getCommandStack().markSaveLocation();
	}

	/*
	 * (non-Javadoc) Method declared on IEditorPart
	 */
	public void gotoMarker(IMarker marker) {
		setActivePage(0);
		IDE.gotoMarker(getEditor(0), marker);
	}

	/**
	 * The <code>MultiPageEditorExample</code> implementation of this method
	 * checks that the input is an instance of <code>IFileEditorInput</code>.
	 */
	@Override
	public void init(IEditorSite site, IEditorInput editorInput)
			throws PartInitException {
		if (!(editorInput instanceof IFileEditorInput)) {
			throw new PartInitException(
					"Invalid Input: Must be IFileEditorInput");
		}
		setSite(site);
		setInput(editorInput);
		getCommandStack().addCommandStackListener(getCommandStackListener());
		getSite().getWorkbenchWindow().getSelectionService()
				.addSelectionListener(getSelectionListener());
		initializeActionRegistry();

	}

	/**
	 * The <code>MultiPageEditorPart</code> implementation of this
	 * <code>IWorkbenchPart</code> method disposes all nested editors.
	 * Subclasses may extend.
	 */
	@Override
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		getCommandStack().removeCommandStackListener(getCommandStackListener());
		getSite().getWorkbenchWindow().getSelectionService()
				.removeSelectionListener(getSelectionListener());
		getEditDomain().setActiveTool(null);
		getActionRegistry().dispose();
		super.dispose();
	}

	/*
	 * (non-Javadoc) Method declared on IEditorPart.
	 */
	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	/**
	 * Closes all project files on project close.
	 */
	public void resourceChanged(final IResourceChangeEvent event) {
		if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					final IWorkbenchPage[] pages = getSite()
							.getWorkbenchWindow().getPages();
					for (final IWorkbenchPage element : pages) {
						if (((FileEditorInput) graph.getEditorInput())
								.getFile().getProject().equals(
										event.getResource())) {
							final IEditorPart editorPart = element
									.findEditor(graph.getEditorInput());
							element.closeEditor(editorPart, true);
						}
					}
				}
			});
		}
	}

	public EditDomain getEditDomain() {
		if (editDomain == null) {
			editDomain = new DefaultEditDomain(this);
		}
		return editDomain;
	}

	/**
	 * Constructs the editor part
	 */

	/**
	 * Creates actions for this editor. Subclasses should override this method
	 * to create and register actions with the {@link ActionRegistry}.
	 */
	protected void createActions() {
		final ActionRegistry registry = getActionRegistry();
		IAction action;

		action = new UndoAction(this);
		registry.registerAction(action);
		getStackActions().add(action.getId());

		action = new RedoAction(this);
		registry.registerAction(action);
		getStackActions().add(action.getId());

		action = new SelectAllAction(this);
		registry.registerAction(action);

		action = new DeleteAction((IWorkbenchPart) this);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new SaveAction(this);
		registry.registerAction(action);
		getPropertyActions().add(action.getId());

		action = new PrintAction(this);
		registry.registerAction(action);

		action = new NodeAction(this, NodeAction.DEP1_REQUEST);
		registry.registerAction(action);
		getNodeActions().add(action.getId());

		action = new NodeAction(this, NodeAction.DEP2_REQUEST);
		registry.registerAction(action);
		getNodeActions().add(action.getId());

		action = new NodeAction(this, NodeAction.DEP3_REQUEST);
		registry.registerAction(action);
		getNodeActions().add(action.getId());
	}

	private List<String> getNodeActions() {
		// TODO Auto-generated method stub
		return nodeActions;
	}

	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#firePropertyChange(int)
	 */
	@Override
	protected void firePropertyChange(int property) {
		super.firePropertyChange(property);
		// updateActions(propertyActions);
		updateActions(nodeActions);
	}

	/**
	 * Lazily creates and returns the action registry.
	 * 
	 * @return the action registry
	 */
	protected ActionRegistry getActionRegistry() {
		if (actionRegistry == null) {
			actionRegistry = new ActionRegistry();
			// print("ActionRegistry created"+actionRegistry.toString());
		}
		return actionRegistry;
	}

	/**
	 * Returns the adapter for the specified key.
	 * 
	 * <P>
	 * <EM>IMPORTANT</EM> certain requests, such as the property sheet, may be
	 * made before or after {@link #createPartControl(Composite)} is called. The
	 * order is unspecified by the Workbench.
	 * 
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class type) {

		if (type == IContentOutlinePage.class) {
			if (fOutlinePage != null) {
				return fOutlinePage;
			} else {
				return null;
			}
		}

		if (type == org.eclipse.ui.views.properties.IPropertySheetPage.class) {
			final PropertySheetPage page = new PropertySheetPage();
			page
					.setRootEntry(new UndoablePropertySheetEntry(
							getCommandStack()));
			return page;
		}

		if (type == CommandStack.class) {
			return getCommandStack();
		}
		if (type == ActionRegistry.class) {
			return getActionRegistry();
		}

		return super.getAdapter(type);
	}

	/**
	 * Returns the command stack.
	 * 
	 * @return the command stack
	 */
	protected CommandStack getCommandStack() {
		return getEditDomain().getCommandStack();
	}

	private CommandStackListener getCommandStackListener() {
		// TODO Auto-generated method stub
		return commandStackListener;
	}

	private ISelectionListener getSelectionListener() {

		return selectionListener;
	}

	/**
	 * Returns the list of {@link IAction IActions} dependant on property
	 * changes in the Editor. These actions should implement the
	 * {@link UpdateAction} interface so that they can be updated in response to
	 * property changes. An example is the "Save" action.
	 * 
	 * @return the list of property-dependant actions
	 */
	protected List<String> getPropertyActions() {
		return propertyActions;
	}

	/**
	 * Returns the list of {@link IAction IActions} dependant on changes in the
	 * workbench's {@link ISelectionService}. These actions should implement
	 * the {@link UpdateAction} interface so that they can be updated in
	 * response to selection changes. An example is the Delete action.
	 * 
	 * @return the list of selection-dependant actions
	 */
	protected List<String> getSelectionActions() {
		return selectionActions;
	}

	/**
	 * Returns the selection syncronizer object. The synchronizer can be used to
	 * sync the selection of 2 or more EditPartViewers.
	 * 
	 * @return the syncrhonizer
	 */
	protected SelectionSynchronizer getSelectionSynchronizer() {
		if (synchronizer == null) {
			synchronizer = new SelectionSynchronizer();
		}
		return synchronizer;
	}

	/**
	 * Returns the list of {@link IAction IActions} dependant on the
	 * CommmandStack's state. These actions should implement the
	 * {@link UpdateAction} interface so that they can be updated in response to
	 * command stack changes. An example is the "undo" action.
	 * 
	 * @return the list of stack-dependant actions
	 */
	protected List<String> getStackActions() {
		return stackActions;
	}

	/**
	 * Hooks the GraphicalViewer to the rest of the Editor. By default, the
	 * viewer is added to the SelectionSynchronizer, which can be used to keep 2
	 * or more EditPartViewers in sync. The viewer is also registered as the
	 * ISelectionProvider for the Editor's PartSite.
	 */
	/*
	 * protected void hookGraphicalViewer(GraphicalViewer viewer) {
	 * print("hookGraphicalViewer()"); if (viewer == null) //
	 * print("getGraphicalViewer()==null");
	 * getSelectionSynchronizer().addViewer(viewer);
	 * getSite().setSelectionProvider(viewer); }
	 */
	/**
	 * Initializes the ActionRegistry. This registry may be used by {@link
	 * ActionBarContributor ActionBarContributors} and/or
	 * {@link ContextMenuProvider ContextMenuProviders}.
	 * <P>
	 * This method may be called on Editor creation, or lazily the first time
	 * {@link #getActionRegistry()} is called.
	 */
	protected void initializeActionRegistry() {
		createActions();
		// updateActions(propertyActions);
		// updateActions(stackActions);
		updateActions(nodeActions);
	}

	/**
	 * @see org.eclipse.ui.ISelectionListener#selectionChanged(IWorkbenchPart,
	 *      ISelection)
	 */
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		// If not the active editor, ignore selection changed.
		if (this.equals(getSite().getPage().getActiveEditor())) {
			updateActions(nodeActions);
		}
		updateActions(selectionActions);

	}

	/**
	 * Sets the ActionRegistry for this EditorPart.
	 * 
	 * @param registry
	 *            the registry
	 */
	protected void setActionRegistry(ActionRegistry registry) {
		actionRegistry = registry;
	}

	/**
	 * Sets the EditDomain for this EditorPart.
	 * 
	 * @param ed
	 *            the domain
	 */
	protected void setEditDomain(DefaultEditDomain ed) {
		this.editDomain = ed;
	}

	/**
	 * @see org.eclipse.ui.IWorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {

		// print("setFocus()");
		// if (getGraphicalViewer() == null)
		// ;
		// print("getGraphicalViewer()==null");
		// getGraphicalViewer().getControl().setFocus();
		// print("done setFocus()");
	}

	/**
	 * A convenience method for updating a set of actions defined by the given
	 * List of action IDs. The actions are found by looking up the ID in the
	 * {@link #getActionRegistry() action registry}. If the corresponding
	 * action is an {@link UpdateAction}, it will have its
	 * <code>update()</code> method called.
	 * 
	 * @param actionIds
	 *            the list of IDs to update
	 */
	protected void updateActions(List<String> actionIds) {
		final ActionRegistry registry = getActionRegistry();
		final Iterator<String> iter = actionIds.iterator();
		while (iter.hasNext()) {
			final IAction action = registry.getAction(iter.next());
			if (action instanceof UpdateAction) {
				((UpdateAction) action).update();
			}
		}
	}

	public void showDependencyTree() {
		// Composite composite = new Composite(getContainer(), SWT.NONE);
		final String stat = "";

		System.out.println("show dep graph1");
		if (this.getPageText(this.getActivePage()) != null
				&& this.getPageText(this.getActivePage()).equals("")) {
			System.out.println("show dep graph2");
			this.removePage(0);
			// this.removePage(1);
			// this.removePage(2);
			displayPages();
		}
		System.out.println("show dep graph3");
		final Graph graph = Graph.getDefault();
		final ArrayList<MyModel> models = graph.getModels();
		for (int i = 0; i < models.size(); i++) {
			print("setContest for page " + i);
			final GraphicalViewer viewer = viewers.get(i).getGraphicalViewer();
			final MyModel mod = models.get(i);
			stat.concat("model: " + mod.getText() + " contains "
					+ mod.getChildren().size() + "processed messages: "
					+ graph.statElements + "\n");
			mod.evaluate();
			// print(mod.map.entrySet().toString());
			if (mod != null) {
				viewer.setContents(mod);
				mod.getLayout().arrange();
				viewer.setContents(mod);
			}
			print("end setContest for page " + i);
			System.out.println(stat);
		}

	}

	public static GraphMultiPageEditor getDefault() {
		if (graph == null) {
			// System.out.println("GraphMultiPageEditor getDefault()=null");
		}
		return graph;
	}

	protected Object getContent() {
		return null;

	}

	protected void addEditPartAction(SelectionAction action) {
		getActionRegistry().registerAction(action);
		editPartActionIDs.add(action.getId());

	}

	protected void addStackAction(StackAction action) {
		getActionRegistry().registerAction(action);
		stackActionIDs.add(action.getId());
	}

	protected void addEditorAction(EditorPartAction action) {
		getActionRegistry().registerAction(action);
		editorActionIDs.add(action.getId());
	}

	protected void addAction(IAction action) {
		getActionRegistry().registerAction(action);

	}

	protected void addNodeAction(EditorPartAction action) {
		getActionRegistry().registerAction(action);
		nodeActions.add(action.getId());
	}

	public void commandStackChanged(EventObject event) {
		// TODO Auto-generated method stub
		updateActions(stackActionIDs);
		setDirty(getCommandStack().isDirty());
	}

	protected void displayPages() {
		final Composite composite = new Composite(getContainer(), SWT.NONE);
		System.out.println("start dependency graph with edit domain");
		int index = 0;
		final Graph graph = Graph.getDefault();
		final ArrayList<MyModel> models = graph.getModels();
		final Iterator<MyModel> it = models.iterator();
		while (it.hasNext()) {
			final MyModel model = it.next();
			final GraphView view = new GraphView(this, getEditDomain());
			// view.createGraphicalViewer(composite);
			try {

				index = addPage(view, view.getEditorInput());
				// graphicalViewer.setContents(model);
			} catch (final PartInitException e1) {
				e1.printStackTrace();
			}
			final GraphicalViewer graphicalViewer = view.getGraphicalViewer();
			if (graphicalViewer == null) {
				print("graphicalViewer==null");
			}
			final NodeMenuProvider provider = new NodeMenuProvider(
					graphicalViewer, getActionRegistry());
			graphicalViewer.setContextMenu(provider);
			getSite().registerContextMenu(
					"ed.inf.proofgeneral.dependencygraph.mymenu", provider,
					graphicalViewer);
			viewers.add(index, view);
			setPageText(index, model.getText());

		}
	}

	@Override
	protected void createPages() {
		final Composite composite = new Composite(getContainer(), SWT.NONE);
		System.out.println("start dependency graph with edit domain");
		addPage(composite);
		// addPage(composite);
		// addPage(composite);

	}

	// dependencyView=new DependencyView(getEditDomain());

	private void print(String msg) {
		if (debug) {
			System.out.println(msg);
		}

	}
}
