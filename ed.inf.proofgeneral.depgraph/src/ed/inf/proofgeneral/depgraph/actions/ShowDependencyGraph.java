package ed.inf.proofgeneral.depgraph.actions;

import ed.inf.proofgeneral.depgraph.graph.GraphMultiPageEditor;
import ed.inf.proofgeneral.depgraph.views.Views;
import ed.inf.proofgeneral.actions.ShowView;

public class ShowDependencyGraph extends ShowView {
	public ShowDependencyGraph() {
		super(Views.DEPENDENCY_GRAPH);
	//	new GraphMultiPageEditor();
	}

	@Override
    public void run() {

		System.out.println("ShowDependencyGraph");

		// DependencyGraph graph = DependencyGraph.getDefault();
		GraphMultiPageEditor graph = GraphMultiPageEditor.getDefault();
		if (graph == null) {
			System.out.println("graph is null");
			// graph=new GraphMultiPageEditor();

		} else
			graph.showDependencyTree();
		System.out.println("editor show dep graph done");

	}

}
