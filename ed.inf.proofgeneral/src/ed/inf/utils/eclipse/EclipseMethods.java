/*
 *  $RCSfile: EclipseMethods.java,v $
 *
 *  Created on 06 Jun 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.eclipse;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.editor.ProofScriptEditor;
import ed.inf.proofgeneral.preferences.PreferenceNames;
import ed.inf.proofgeneral.preferences.PrefsPageBackend;
import ed.inf.proofgeneral.sessionmanager.CommandFailedException;
import ed.inf.proofgeneral.sessionmanager.ScriptingException;
import ed.inf.utils.datastruct.MutableObject;
import ed.inf.utils.eclipse.customswt.AssocListFieldEditor;
import ed.inf.utils.exception.ExceptionHandling;
import ed.inf.utils.exception.KnownException;
import ed.inf.utils.file.DirectoryUtils;
import ed.inf.utils.process.RunnableWithParams;

/**
 * Some general-purpose static Eclipse methods for use anywhere.
 * Originally ed.inf.winterstein.methods.EclipseMethods;
 * @author Daniel Winterstein
 */
public class EclipseMethods {

    /**
     * This class should not be instantiated; all methods are static.
     */
    private EclipseMethods() { }

	/**
	 * Test whether or not a font can display specific unicode character.
	 * This creates and disposes an awt font, so it is probably not efficient.
	 * Caching the answers is recommended.
	 * @param swtFont
	 * @param s a string of unicode characters to test
	 * @return true if the character can be displayed.
	 */
// da: disabled now, let's try without this.  We can supply complete symbol
// tables and let people make cut-down versions if they need to.
//	public static boolean unicodeStringTest(Font swtFont, String s) {
//	    assert s != null : "Illegal argument";
//		FontData[] fda = swtFont.getFontData();
//		FontData fd = fda[0];
//		// TODO: map between swt and awt styles
//		java.awt.Font awtFont = new java.awt.Font(fd.getName(),0,fd.getHeight());
//		//TODO Eclipse SDK (on GTK at least) will fallback to use other fonts, so characters may be usable even when this is false
//		//(not sure how to discover which are visible and which aren't ... but so far most of them seem to be!)
//		// da: this method causes low level NullPointerException on my Linux/GTK,
//	    // really seems to slow startup, at least, in debugger.
//		return awtFont.canDisplayUpTo(s) == -1;  // indicates can display string
//	}


    /**
	 * Displays an error dialog. Note: Works from background threads as well as
	 * the display thread.
	 *
	 * @param e
	 */
	public static void errorDialog(Exception e) {
		try {
			// multi-line messages get first line in top and following lines in body
			// otherwise the message
			String msg = "Cause unknown.";
			String msgBody = "";
			if (e instanceof CommandFailedException) {

				msg = ((CommandFailedException) e).getExceptionMessage();
				if (msg.indexOf('\n') > 0) {
					msgBody = msg.substring(msg.indexOf('\n')).trim()
							+ "\n\n\n";
					msg = msg.substring(0, msg.indexOf('\n')).trim();
				} else {
					msgBody = "";
				}
				String cmd = ((CommandFailedException) e).getCommand();
				if (cmd != null && cmd.length() > 0) {
					msgBody = msgBody
							+ "The error occurred while processing the command:\n\n"
							+ cmd;
				}
			} else if (e instanceof ScriptingException) {
				msg = e.getMessage();
				if (msg.indexOf('\n') > 0) {
					msgBody = msg.substring(msg.indexOf('\n')).trim()
							+ "\n\n\n";
					msg = msg.substring(0, msg.indexOf('\n')).trim();
				} else {
					msgBody = "";
				}
			} else if (e instanceof KnownException) {
				// for known exceptions, just take the message, or if no message, just the class
				msg = e.getMessage();
				msgBody = "";
				if (msg == null) {
					msg = e.getClass().getName();
					if (msg.lastIndexOf('.') >= 0) {
						msg = msg.substring(msg.lastIndexOf('.') + 1);
					}
					msgBody = ExceptionHandling.getStackTraceString(e);
				}
			} else {
				msg = e.getMessage();
				if (msg == null || msg.length() == 0) {
					// clean up the text we display at the top, if there is no message (not really important)
					if (e instanceof NullPointerException) {
						msg = "A null-pointer error occurred.";
					}
					else if (e instanceof ScriptingException) {
						msg = "A scripting error occurred.";
					}
					else {
						msg = "Cause unknown.";
					}
				}
				msgBody = ExceptionHandling.getStackTraceString(e);
			}

			final String msg1 = msg;
			final String msg2 = msgBody;

			Bundle ourBundle = Platform.getBundle(ProofGeneralPlugin.PLUGIN_ID);
			// NB: was this:
			// String name = ProofGeneralPlugin.getDefault().getDescriptor().getUniqueIdentifier();
			if (ourBundle != null) {  // Bundle can be removed during shutdown. 

				String name = ourBundle.getSymbolicName(); 

				final IStatus status = new Status(IStatus.ERROR, name, IStatus.ERROR, msg1, e);
				if (ProofGeneralPlugin.debug(EclipseMethods.class)) {
					ProofGeneralPlugin.getDefault().getLog().log(status);
				}
				final String title = "Error";

				// String type = e.getClass().getName();
				// type = type.substring(type.lastIndexOf(".")+1);
				// msg = type +":\n "+msg; //unnecessary
				// final Exception exc = e;
				final String[] buttons = new String[] { "OK" };
				final int image = MessageDialog.ERROR;

				Display.getDefault().asyncExec(
						new Runnable() {
							public void run() {
								Shell shell = getShell();
								MessageDialog d = new MessageDialog(shell, title,
										null, msg1, image, buttons, 0) {
									@Override
									protected Control createCustomArea(
											Composite parent) {
										Label l = new Label(parent, SWT.VERTICAL);
										l.setText(msg2);
										return l;
									}
								};
								d.open();
								// messageDialog(title,fMsg,new String[]
								// {"OK"},false,MessageDialog.ERROR);
								// ErrorDialog.openError(new Shell(), title, fMsg,
								// status);
							}
						});
				// another version of openError exists which has some filtering
			}

		} catch (RuntimeException e1) {
			e1.printStackTrace();
		}
	}

    /**
	 * Opens a (blocking) multiple-choice message dialog.
	 *
	 * @param title message dialog title
	 * @param msg the message itself
	 * @param buttons a selection of button labels
	 * @return the label of the button selected by the user.
	 * @see #messageDialogAsync(String, String, String[], boolean, int)
	 */
    public static String messageDialog(String title,String msg, String[] buttons) {
        boolean toggle = false;
        int image=MessageDialog.QUESTION;
        return messageDialog(title,msg,buttons,toggle,image);
    }

    /**
     * Lock to prevent more than one open dialog, and give the user chance
     * to ask for response to be remembered before dealing with a huge stack...
     */
    private static Object dialogLock = new Object();

    /**
	 * Opens a modal (blocking) multiple-choice message dialog.
     * @param title message dialog title
     * @param msg the message itself
     * @param buttons a selection of button labels
     * @param toggle if true, include a "Don't show me this message again" toggle
     * @return the label of the button selected by the user.
     */
    public static String messageDialog(String title,String msg,
            							String[] buttons, boolean toggle,
            							int image) {
    	synchronized (dialogLock) {
    		Shell shell = getShell();
    		if (!toggle) {
    			MessageDialog d = new MessageDialog(shell, title,null,msg, image, buttons, 1);
    			d.setBlockOnOpen(true);
    			//d.getShell().moveAbove(null);
    			int pushed = d.open();
    			if (pushed<0) {
    				return null;
    			}
    			return buttons[pushed];
    		}
    		String blocked = ProofGeneralPlugin.getDefault().getPluginPreferences()
    		.getString(PreferenceNames.PREF_BLOCKED_MESSAGES);
    		Map<String,String> blocks = AssocListFieldEditor.makeMap(blocked);
    		String mkey = AssocListFieldEditor.makeKey(title+": "+msg);
    		if (blocks.get(mkey.intern()) != null) { // block the popup dialog
    			return blocks.get(mkey.intern());
    		}
    		MessageDialogWithToggle d = new MessageDialogWithToggle(shell, title,null,
    				msg,
    				image,
    				buttons,
    				1,
    				"Don't show me this message again",false);
    		d.setBlockOnOpen(true);
    		int pushed = d.open();
    		if (d.getToggleState()) { // store the answer
    			blocks.put(mkey,buttons[pushed]);
    			String s = AssocListFieldEditor.makeString(blocks);
    			PrefsPageBackend.setPref(PreferenceNames.PREF_BLOCKED_MESSAGES,s);
    		}
    		if (pushed<0) {
    			return null;
    		}
    		return buttons[pushed];
    	}
    }


    /**
     * Returns the Editor for a particular file.
     * @author G. Longman (Modified)
     * @param file the file which we need an editor for
     * @return the editor.
     */
    public static IEditorPart findEditorFor(IFile file) {
    	IWorkbench workbench = PlatformUI.getWorkbench();
        if (workbench==null) {
        	return null;
        }
        IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
        for (IWorkbenchWindow window : windows) {
            for (IWorkbenchPage page : window.getPages()) {
                for (IEditorReference ref : page.getEditorReferences()) {
                    IEditorPart editor = ref.getEditor(true);
                    if (editor == null) {
                        continue;
                    }
                    IEditorInput input = editor.getEditorInput();
                    IFile editorFile = (IFile) input.getAdapter(IFile.class);
                    if (editorFile != null && editorFile.equals(file)) {
						return editor;
					}
                }
            }
        }
        return null;
    }
    
    /**
     * Get the active editor.
     * @return the active editor, or null if none can be found
     */
    public static IEditorPart getActiveEditor() {
    	try {
    		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
    	} catch (Exception e) {
    		return null;
    	}
    }

	/**
	 * Find an editor for the given document.
	 * @param activedoc
	 * @param activePage
	 * @return an editor part for the given document, or null.
	 */
	public static ProofScriptEditor findEditorForDoc(ProofScriptDocument activedoc, IWorkbenchPage activePage) {
		IEditorReference[] editorRefs = activePage.getEditorReferences();
		ProofScriptEditor edpart = null;
		for (IEditorReference editorRef : editorRefs) {
			IEditorPart existinged = editorRef.getEditor(true);  // potentially time consuming
			if (existinged != null && existinged instanceof ProofScriptEditor) {
				ProofScriptEditor ed = (ProofScriptEditor) existinged;
				if (activedoc.equals(ed.getDocument())) {
						edpart = ed;
						break;
				}
			}
		}
		return edpart;
	}

	/**
	 * Find a shell from the site of the active editor, or make a new one
	 * @return a shell 
	 */
	public static Shell getShell() {
		Shell shell = null; 
		try {
			shell = getActiveEditor().getSite().getShell();
		} catch (Exception e) {	}
		if (shell==null) {
			shell = new Shell(); // does this option really work?
		}
		return shell;
	}

/**
     * Prompts the user to enter a string.  Should be run from UI thread.
  	 * @param title dialog title
  	 * @param message dialog message
  	 * @return the string entered by the user, or null if none
  	 */
 	public static String messagePromptDialog(String title, String message) {
 		Shell shell = getShell();
 		InputDialog id = new InputDialog(shell, title, message, "", null);

 		if (id.open() != InputDialog.OK) {
 			return null;
 		}
 		return id.getValue();
  	 }

	/** prompts the user to enter a string (can be called from outwith the async thread)
	 * @param title
	 * @param message
	 * @return the string entered by the user, or null if none
	 */
	public static String messagePromptDialogAsync(final String title, final String message) {
		final MutableObject result = new MutableObject(null);
		synchronized (result) {
			if (!(new RunnableWithParams(null) {
				public void run() {
					try {
						result.set(messagePromptDialog(title, message));
					} finally {
						synchronized (result) {
							//System.out.println("notifying");
							result.notifyAll();
						}
					}
				}
			}.callDefaultDisplayAsyncExec())) {
				//only run if the message is being displayed in another thread (else it has already been displayed)
				//System.out.println("waiting");
				try {
					result.wait(); // FIXME: Wa
				} catch (InterruptedException e) {}
			}
		}
		return (String)result.get();
	}

	/** displays a dialog in the async thread, waiting for it
	 */
	public static String messageDialogAsync(String title,String msg,
			String[] buttons) {
        boolean toggle = false;
        int image=MessageDialog.QUESTION;
        return messageDialogAsync(title,msg,buttons,toggle,image);
	}

	public static String messageDialogAsync(final String title, final String msg,
			final String[] buttons, final boolean toggle,
			final int image) {
		final MutableObject result = new MutableObject(null);
		synchronized (result) {
			if (!(new RunnableWithParams(null) {
				public void run() {
					try {
						result.set(
                                messageDialog(title, msg, buttons, toggle, image)
                        );
					} finally {
						synchronized (result) {
							//System.out.println("notifying");
							result.notifyAll();
						}
					}
				}
			}.callDefaultDisplayAsyncExec())) {
				//only run if the message is being displayed in another thread (else it has already been displayed)
				//System.out.println("waiting");
				try {
					result.wait(); // FIXME: Wa
				} catch (InterruptedException e) {}
			}
			return (String)result.get();
		}
	}

	/**
	 * Given an Eclipse IFile object, return the actual filename with complete OS-specific path.
	 * @param file the file object to realise
	 * @return the OS-specific path of passed file, or a temporary path if null.
	 */
	public static String getOSPath(IResource file) {
		String spath = null;
		try {
			if (file == null) {
				throw new NullPointerException("File was null");
			}
			IPath path = file.getLocation();
			spath = path.toOSString();
		} catch (NullPointerException e) {
			spath = DirectoryUtils.getNewTempFile("","thy");
			System.err.println("Null file passed, returning "+spath+" instead.");
			e.printStackTrace();
		}
		return spath;
	}

}
