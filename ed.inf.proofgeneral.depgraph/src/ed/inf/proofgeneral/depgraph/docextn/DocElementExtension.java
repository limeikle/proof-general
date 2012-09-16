package ed.inf.proofgeneral.depgraph.docextn;

import ed.inf.proofgeneral.depgraph.graph.Graph;
import ed.inf.proofgeneral.sessionmanager.PGIPSyntax;

public class DocElementExtension {
	/**
     * @param name
     */
    private void addToGraph(String name) {
		if (Graph.getDefault()!=null
				&& (name.equals(PGIPSyntax.OPENGOAL)
						||name.equals(PGIPSyntax.OPENTHEORY))) {
			// FIXME: this is the method we need to hook up to the model
			// Graph.getDefault().addDocElement(this);
		}
    }

}
