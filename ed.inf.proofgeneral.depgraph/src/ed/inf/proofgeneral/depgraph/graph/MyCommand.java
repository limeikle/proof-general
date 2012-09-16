package ed.inf.proofgeneral.depgraph.graph;

import org.eclipse.gef.commands.Command;

public class MyCommand extends Command {

	public MyCommand(String string) {

		super(string);
		System.out.println(string);
	}

}
