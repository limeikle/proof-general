/*
 *  $RCSfile: ChooseLogicDialog.java,v $
 *
 *  Created on 17 Apr 2005 by Alex Heneveld
 *  part of Proof General for Eclipse
 */

// FIXME: this action is entirely ISABELLE SPECIFIC

package ed.inf.proofgeneral.prover.isabelle.editor.actions;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import ed.inf.utils.eclipse.ErrorUI;
import ed.inf.utils.process.RunnableWithParams;

/**
 * ChooseLogicDialog
 *
 */
public class ChooseLogicDialog extends Dialog {

	/**
	 * @param parentShell
	 */
	protected ChooseLogicDialog(Shell parentShell) {
		super(parentShell);
	}

	List logicsList = null;
	Text text = null; // , cwdText;
	Label errorMessageText = null;
	Button saveLogicPreference = null;

	private static final int COLOR_ERROR_TEXT = SWT.COLOR_RED;

	@Override
    protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite)super.createDialogArea(parent);

		Group logGroup = new Group(composite, SWT.BORDER);
		logGroup.setText("Choose Logic");

		logGroup.setLayout(new GridLayout());
		logGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

		{
			Label label = new Label(logGroup, SWT.WRAP);
			label.setText("Choose the logic to run with Isabelle:");
			GridData data = new GridData(GridData.GRAB_HORIZONTAL
					| GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL
					| GridData.VERTICAL_ALIGN_CENTER);
			data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
			label.setLayoutData(data);
			label.setFont(parent.getFont());

			logicsList = new List(logGroup, SWT.BORDER);
			logicsList.add("(please wait while logics are loading)");
			logicsList.add("");
			logicsList.add("");
			logicsList.add("");
			logicsList.add("");
			logicsList.add("");
			logicsList.add("");
			logicsList.add("");
			logicsList.add("");
			logicsList.add("");
			logicsList.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
					| GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_VERTICAL ));

			ListViewer viewer = new ListViewer(logicsList);
			viewer.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					String[] s = logicsList.getSelection();
					if (s.length==1) {
						text.setText(s[0]);
					}
					if (s.length!=1) {
						errorMessageText.setText("You can only choose one logic.");
					}
				}
			});
			logicsList.setEnabled(false);
			Listener listDblClickListener = new Listener () {
				public void handleEvent (Event e) {
					if (e.type == SWT.MouseDoubleClick) {
						okPressed();
					}
				}
			};
			logicsList.addListener(SWT.MouseDoubleClick, listDblClickListener);

			//set logics to refresh in separate list
			new RunnableWithParams(null) {
				public void run() {
					refreshLogics();
				}
			}.start();
			//System.out.println("just said to start refreshLogics");
		}

    //from InputDialog
    {
			Label label = new Label(logGroup, SWT.WRAP);
			label.setText("If the logic is not in the list above, enter it here:");
			GridData data = new GridData(GridData.GRAB_HORIZONTAL
					| GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL
					| GridData.VERTICAL_ALIGN_CENTER);
			data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
			label.setLayoutData(data);
			label.setFont(parent.getFont());
		}
		text = new Text(logGroup, SWT.SINGLE | SWT.BORDER);
		text.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL));

		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				errorMessageText.setText("");
			}
		});
//    Listener textEnterListener = new Listener () {
//      public void handleEvent (Event e) {
//         switch (e.type) {
//            case SWT.
//               okPressed();
//               break;
//         }
//      }
//   };
//   logicsList.addListener(SWT.Resize, listDblClickListener);
		saveLogicPreference = new Button(logGroup, SWT.CHECK);
		saveLogicPreference.setText("Save this logic as a preference");
		saveLogicPreference.setSelection(false);

//    Group cwdGroup = new Group(composite, SWT.BORDER);
//    cwdGroup.setText("Current Working Directory");
//		cwdGroup.setLayout(new GridLayout());
//		cwdGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
//    {
//			Label label = new Label(cwdGroup, SWT.WRAP);
//			label.setText("If a new working directory is needed, enter it here:");
//			GridData data = new GridData(GridData.GRAB_HORIZONTAL
//					| GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL
//					| GridData.VERTICAL_ALIGN_CENTER);
//			data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
//			label.setLayoutData(data);
//			label.setFont(parent.getFont());
//		}

//    Composite c2 =
//       new Composite(cwdGroup, 0);

//    GridLayout cwdGL = new GridLayout(2, false);
//    cwdGL.marginWidth = 0;
//    cwdGL.marginHeight = 0;
//    c2.setLayout(cwdGL);
//    c2.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

//    cwdText = new Text(c2, SWT.SINGLE | SWT.BORDER);
//    cwdText.setLayoutData(new GridData(GridData.FILL_BOTH));
//		cwdText.setText(
//				(userCwd!=null ? userCwd : System.getProperty("user.dir")));
//		cwdText.addModifyListener(new ModifyListener() {
//			public void modifyText(ModifyEvent e) {
////				errorMessageText.setText("");
//			}
//		});

//		Button cwdDB = new Button(c2, SWT.BORDER | SWT.PUSH);
//		cwdDB.setText("...");
//		cwdDB.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_CENTER));
//		cwdDB.addSelectionListener(new SelectionListener() {
//			public void widgetSelected(SelectionEvent e) {
//				DirectoryDialog dd = new DirectoryDialog(new Shell());
//				String s = cwdText.getText();
//				if (s==null || s.length()==0)
//					s = System.getProperty("user.dir");
//				if (s!=null && s.length()>0) dd.setText(s);
//				s = dd.open();
//				if (s!=null)
//					cwdText.setText(s);
//			}
//			public void widgetDefaultSelected(SelectionEvent e) {
//			}
//		});


		Composite cem = new Composite(composite, 0);
		cem.setLayout(new GridLayout());
		cem.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL));
		errorMessageText = new Label(cem, SWT.READ_ONLY | SWT.NO_FOCUS);
		//errorMessageText.setEnabled(false);
		//errorMessageText.setEditable(false);
		errorMessageText.setBackground(errorMessageText.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		errorMessageText.setForeground(errorMessageText.getDisplay().getSystemColor(COLOR_ERROR_TEXT));

		applyDialogFont(composite);

    return composite;
 }

	@Override
    protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID, "&Restart Prover",
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

  public static final String ISABELLE_FIND_LOGICS_COMMAND = "isatool findlogics";


  protected void refreshLogics() {
  	//System.out.println("starting 'refreshLogics'");
		ArrayList<String> logics = new ArrayList<String>();
		String errorMessage = null;
		//long time = System.currentTimeMillis();
		BufferedInputStream in = null;
		try {
			StringBuffer logic = new StringBuffer();
			Process p = Runtime.getRuntime().exec(ISABELLE_FIND_LOGICS_COMMAND);
			in = new BufferedInputStream(p.getInputStream());
			int i=0;
			while ((i=in.read())!=-1) {
				char c = (char)i;
				if (c!=' ') {
					logic.append(c);
				} else if (logic.length()>0) {
					logics.add(logic.toString());
					logic = new StringBuffer();
				}
			}
		} catch (IOException e) {
			errorMessage = "Error loading logics: "+e.getMessage();
			//e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) { // nothing
				}
			}
		}
		//System.out.println("LOGICS (found in "+General.makeTimeString(System.currentTimeMillis()-time)+")");
		new RunnableWithParams(new Object[] { logics.iterator(), errorMessage }) {
			@SuppressWarnings("unchecked")
			public void run() {
				try {
					logicsList.setEnabled(false);
					logicsList.removeAll();
					Iterator<String> li = (Iterator<String>)(p[0]);
					if (li.hasNext()) {
						boolean selected = false;
						while (li.hasNext()) {
							String l = li.next().toString();
							//System.out.println("--"+l);
							logicsList.add(l);
							if (l.equals(defaultLogic)) {
								logicsList.setSelection(logicsList.getItemCount()-1);
								selected = true;
							}
						}
						if (!selected) {
							errorMessageText.setText("Default logic '"+defaultLogic+"' not found.");
						}
						if (text.getText().length()==0) {
							text.setText(defaultLogic);
						}
						logicsList.setEnabled(true);
					} else {
						logicsList.add("(no logics could be loaded; type logic name below, e.g. '"+defaultLogic+"')");
						if (text.getText().length()==0) {
							text.setText(defaultLogic);
						}
						String nerrorMessage = (String)p[1];
						if (nerrorMessage!=null) {
							errorMessageText.setText(nerrorMessage);
						}
					}
				} catch (Exception e) {
					ErrorUI.getDefault().signalWarning(
							new Exception("Unable to get list of logics", e));
				}
			}
		}.callDefaultDisplayAsyncExec();
  }

  @Override
protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    newShell.setText("Start Isabelle (with parameters)"); //TODO  or say REstart isabelle?
  }

	@Override
    protected void okPressed() {
//		String logics[] = logicsList.getSelection();
//		for (int i=0; i<logics.length; i++) {
//			System.out.println("SELECTED LOGIC: "+logics[i]);
//		}
//		System.out.println();
//		if (logics.length==1) {
//			System.out.println("calling super");
//			super.okPressed();
//		}
		if (text.getText().length()>0) {
			super.okPressed();
		} else {
			errorMessageText.setText("A logic must be selected.");
		}
	}

	String selectedLogic = null;
//	String userCwd = null;

	@Override
    public boolean close() {
		selectedLogic = text.getText();
//		userCwd = cwdText.getText();
		saveLogicPrefValue = saveLogicPreference.getSelection();

		boolean result = super.close();
	  //do local de-initialising
	  return result;
	}

	/**
	 * @return the logic selected by the user
	 */
	public String getSelectedLogic() {
		if (getShell() == null || getShell().isDisposed()) {
			return selectedLogic;
		}
		return text.getText();
	}

//	public String getUserCwd() {
//		if (getShell() == null || getShell().isDisposed())
//			return userCwd;
//		else
//			return cwdText.getText();
//	}
//	public void setUserCwd(File proverCwd) {
//		if (proverCwd!=null)
//			userCwd = proverCwd.getPath();
//	}

	String defaultLogic = "";
	public void setDefaultLogic(String logic) {
		defaultLogic = logic;
	}

	boolean saveLogicPrefValue = false;
	public boolean getSaveLogicPreference() {
		if (getShell() == null || getShell().isDisposed()) {
			return saveLogicPrefValue;
		}
		return saveLogicPreference.getSelection();
	}

}
