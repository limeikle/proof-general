/*
 *  $RCSfile: IsaPlannerAction.java,v $
 *
 *  Created on 10 Nov 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.actions.defunct;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import ed.inf.proofgeneral.document.CmdElement;
import ed.inf.proofgeneral.sessionmanager.PGIPSyntax;
import ed.inf.proofgeneral.sessionmanager.events.PGIPEvent;


/**
 * @author Daniel Winterstein
 * TODO this isn't an "action" in the sense of the others, it's a method we call (AH)
 */
public class IsaPlannerAction extends PGAction {

    /**
     * Create the action.
     */
    public IsaPlannerAction(IRegion triggerRegion) {
        super();
        //this.triggerRegion = triggerRegion;
        setText("Call IsaPlanner");
        setToolTipText("Calls IsaPlanner for suggestions on how to complete the proof.");
    }

    //IRegion triggerRegion;
    //DocElement sorry;

    public static final String askForSuggestions= "print_techns"; // will become suggest_pp
    public static final String execPlan = "ppp"; // will become do_pp

    /**
     * @see ed.inf.proofgeneral.editor.actions.defunct.EclipseAction#run()
     */
    public void runSingly() {
        //SessionManager sm = getSessionManager();
        CmdElement cmd = new CmdElement(PGIPSyntax.SPURIOUSCOMMAND);
        cmd.setText(askForSuggestions);
//        try {
        	//TODO no longer available, needs some work
//            GotoAction ga = new GotoAction();
//            ga.gotoOffset(triggerRegion.getOffset());
//            sm.queueCommand(cmd,(IProofScriptEditor)getActiveEditor(),this);
//        } catch (ScriptingException x) {
//            error(x);
//        }
    }


    /**
     * Catch the reply to our hello.
     * @see ed.inf.proofgeneral.editor.actions.defunct.PGAction#pgipEvent(ed.inf.proofgeneral.sessionmanager.events.PGIPEvent)
     */
    @Override
    public boolean handleOurPgipEvent(PGIPEvent event) {
    	//return super.handleOurPgipEvent(event);
    	System.out.println("  !! IsaPlanner says: "+event.parseTree.asXML());
    	choosePlan(new String[] {"plan1","plan2"});
    	// FIXME do something, pref setup session manager to ascribe actions to events
    	return true;
    }

    /**
     * Open a dialog so that the user can choose a plan.
     * @param plans the choice of plans
     * @return the choice selected from the given list.
     */
    public String choosePlan(String[] plans) {
    	PlanChoiceDialog chooser = new PlanChoiceDialog(new Shell(Display.getDefault()), plans);
        //ChoosePlanDialog chooser = new ChoosePlanDialog(plans);
        chooser.setBlockOnOpen(true);
        chooser.open();
        String choice = chooser.getChoice();
        return choice;
    }

    /**
     * A dialog which allows the user to chose from a number of different plan options.
     */
    public static class PlanChoiceDialog extends MessageDialog {
		private int choice;
		private final String[] plans;
		private StyledText text;
		private org.eclipse.swt.widgets.List planList = null;

		/**
		 * Create the dialog.  Open using the {@link #open()} method.
		 * @param parentShell the parent shell.
		 * 		   Suggest <code>PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()</code>
		 * @param plans the choice of plan objects.
		 */
		public PlanChoiceDialog(Shell parentShell, String[] plans) {
    		super(parentShell, "Choose a proof plan", null,
    				"Please select a plan from the following list:", MessageDialog.QUESTION,
    				new String[]{"OK","Cancel"}, 0);
    		this.plans = plans; // EI
    		choice = -1;
		}

        /**
         * Calls open, storing the list selection once it has been made.
         * @see org.eclipse.jface.window.Window#open()
         */
        @Override
        public int open() {
        	org.eclipse.swt.widgets.List pl = this.planList;
        	int i = super.open();
        	if (i == MessageDialog.OK) {
            	choice = pl.getSelectionIndex();
        	}
        	return i;
        }

        /**
         * FIXME Needs to create a list of plan names (if more than 1),
         * with a plan details view
         */
        @Override
        protected Control createDialogArea(Composite parent) {
            Composite composite =  (Composite) super.createDialogArea(parent);
            composite.setLayout(new org.eclipse.swt.layout.FillLayout(SWT.VERTICAL));
            if (plans.length > 1) {
                planList = new org.eclipse.swt.widgets.List(composite,SWT.BORDER | SWT.FILL);
                planList.setItems(plans);
            }
            text = new StyledText(composite, SWT.BORDER);
            text.setText("This would be a new plan. But it isn't. sorry.");
            text.setEditable(false);
            return composite;
        }

    	/**
    	 * Gets the selected plan.
    	 * @return the name of the selected plan.
    	 */
    	public String getChoice() {
    		return choice == -1 ? null : plans[choice];
    	}
    }

}
