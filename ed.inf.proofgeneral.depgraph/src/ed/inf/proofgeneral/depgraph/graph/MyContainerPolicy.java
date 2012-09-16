package ed.inf.proofgeneral.depgraph.graph;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ContainerEditPolicy;
import org.eclipse.gef.requests.CreateRequest;

public class MyContainerPolicy extends ContainerEditPolicy {

	@Override
	protected Command getCreateCommand(CreateRequest arg0) {
		System.out.println("getCreateCommand");
		return null;
	}

	@Override
	public Command getCommand(Request arg0) {

		System.out.println("getCommand" + arg0.getType());
		return null;
	}

}
