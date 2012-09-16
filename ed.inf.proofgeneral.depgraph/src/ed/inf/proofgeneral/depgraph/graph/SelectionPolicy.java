package ed.inf.proofgeneral.depgraph.graph;

import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.DirectEditPolicy;
import org.eclipse.gef.requests.DirectEditRequest;

public class SelectionPolicy extends DirectEditPolicy {

	@Override
	protected Command getDirectEditCommand(DirectEditRequest arg0) {
		System.out.println("direct edit Command " + arg0.getType());

		final Command command = new MyCommand("new");

		// TODO Auto-generated method stub
		return command;
	}

	@Override
	protected void showCurrentEditValue(DirectEditRequest arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public Command getCommand(Request arg0) {
		Command result = null;
		if (arg0.getType().equals(RequestConstants.REQ_OPEN)) {
			result = new MyCommand("open now!");
			System.out.println("SelectionPolicy :REQ_OPEN");

		}
		if (arg0.getType().equals(RequestConstants.REQ_DELETE)) {
			result = new MyCommand("delete me!");
			System.out.println("SelectionPolicy :REQ_DELETE");

		}
		System.out.println("SelectionPolicy getCommand " + arg0.getType());

		return result;
	}

}
