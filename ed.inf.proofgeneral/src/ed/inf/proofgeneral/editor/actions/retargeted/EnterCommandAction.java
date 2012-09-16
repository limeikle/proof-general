/*
 *  $RCSfile: EnterCommandAction.java,v $
 *
 *  Created on 21 Apr 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.actions.retargeted;

import java.io.StringReader;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.widgets.Shell;

import ed.inf.proofgeneral.document.CmdElement;
import ed.inf.proofgeneral.document.DocElement;
import ed.inf.proofgeneral.editor.ProofScriptEditor;
import ed.inf.proofgeneral.sessionmanager.ScriptingException;

/**
 * @author Daniel Winterstein
 * send selected text out to the prover as a command
 * TODO should access objects through the map, modify program node, etc.
 */
public class EnterCommandAction extends PGRetargetableAction {

    public DocElement lastCommandSent = null;

	public EnterCommandAction(ProofScriptEditor ed) {
//		requiredProverStatusLevel = PROVER_AVAILABLE;
//		requiredScriptStatusLevel = IGNORE_SCRIPT;
		super(ed);
		this.setToolTipText("Enter a command to send directly to the theorem prover");
		this.setText("Enter Command");
		this.setDescription("Enter Command Description");
	}

	@Override
    public void run() {
	  try {
		  setBusy();
	      //DocElement command = new DocElement(PGIPSyntax.SPURIOUSCOMMAND,null);
	      InputDialog dialog = new InputDialog(new Shell(),"Enter Command","Note: this should be a non side-effecting command!",
	                null,null);
	      dialog.open();
	      String s = dialog.getValue();
	      if (s==null || s.equals("")) {
	    	  return;
	      }
	      s = "<spuriouscmd>"+ s + "</spuriouscmd>";
	      SAXReader reader = new SAXReader();
          Document document;
          document = reader.read( new StringReader(s));
          Element cmd1 = (Element) document.content().get(0);
	      //SessionManager sm = getSessionManager();
	      //Element pr = (Element) sm.getParser().dumbParseText(s).elements().get(0);
	      CmdElement cmd2 = new CmdElement(cmd1.getName());
	      cmd2.setText(cmd1.getText());
	      cmd2.setAttributes(cmd1.attributes());
	      getSessionManagerForRunningAction().queueCommand(cmd2,this);
	  } catch (Exception e) {
			if (!(e instanceof ScriptingException)) {
				e.printStackTrace();
			}
			error(e);
	  } finally {
	      // TODO: should send and wait, trap error using old method below
		  clearBusy();
	  }
	}


	// FIXME: reinstate this code
//	@Override
//    public void pgipEventCausedError(CommandCausedErrorEvent event) {
//		super.pgipEventCausedError(event);
//		Object data = event.parseTree;
//		if (data !=null && data==lastCommandSent) {
//			String emsg = StringManipulation.convertLineBreak("Could not send command");
//			emsg += StringManipulation.convertLineBreak("   "+lastCommandSent.getText());
//			emsg += event.getText();
//			ErrorUI.getDefault().signalError(new Exception(emsg));
//		}
//  }

}
