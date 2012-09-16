package ed.inf.proofgeneral.depgraph.graph;

/*
 * import java.util.HashMap; import java.util.Map;
 * 
 * 
 * 
 * //import org.eclipse.core.runtime.adaptor.IModel; import
 * org.eclipse.swt.widgets.Composite; import org.eclipse.ui.part.ViewPart;
 * 
 * 
 * 
 * import org.eclipse.core.resources.IMarker; import
 * org.eclipse.core.runtime.IProgressMonitor; import
 * org.eclipse.gef.DefaultEditDomain; import org.eclipse.gef.EditDomain; import
 * org.eclipse.gef.ui.parts.GraphicalEditor;
 * 
 * import ed.inf.proofgeneral.ProofGeneralPlugin;
 * 
 * public class DependencyView extends GraphicalEditor{ private static
 * DependencyView graph=null;
 * 
 * public DependencyView(EditDomain domain) { System.out.println("start
 * dependency graph"); //setEditDomain(domain); graph=this; }
 * 
 * 
 * protected void configureGraphicalViewer() { super.configureGraphicalViewer();
 * //Sets the viewer's background to System "white"
 * getGraphicalViewer().setEditPartFactory(new MyGraphicalEditpartFactory()); }
 * 
 * protected void initializeGraphicalViewer() { System.out.println("initialise
 * dep1 graph"); // MyModel model=new MyModel(); //model.create();
 * System.out.println("model created" );
 * //getGraphicalViewer().setContents(model); System.out.println("content
 * done"); }
 * 
 * public void showDependencyTree(MyModel model){ System.out.println("show dep
 * graph"); if(model!=null){ System.out.println("Start set Content");
 * //getGraphicalViewer().refreshChildren();
 * getGraphicalViewer().setContents(model); System.out.println("finish set
 * Content"); } else System.out.println("tree null"); }
 * 
 * 
 * public static DependencyView getDefault() { if (graph==null) { try {
 * ProofGeneralPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow()
 * .getActivePage().showView("ed.inf.proofgeneral.views.dependencygraph"); }
 * catch (Exception e) { e.printStackTrace(); } } return graph; }
 * 
 * public void gotoMarker(IMarker marker) { } public void
 * doSave(IProgressMonitor monitor) { } public void doSaveAs() { }
 * 
 * public boolean isDirty() { return false; } public boolean isSaveAsAllowed() {
 * return false; }
 *  }
 */
