/*
 *  $RCSfile: ScriptingException.java,v $
 *
 *  Created on 01 Jun 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.sessionmanager;
import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.utils.process.RunnableWithParams;


/**
 * Indicates an error in the proof scripting process.
 * Note: At present, this exception type is abused in places to indicate non-terminal errors.
 * TODO: Such uses should prob. be replaced with ReportableException.
 @author Daniel Winterstein
 */
public class ScriptingException extends Exception {


/** field caller can set to say in what doc the error occurred */
  public transient ProofScriptDocument errorDoc = null;

  /** field caller can set to say where in the doc the error occurred */
  public int errorOffset = -1;

  public boolean showInDialog = false;
  
    /**
     *
     */
    public ScriptingException() {
        super();
    }

    /**
     * @param message
     */
    public ScriptingException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public ScriptingException(Throwable cause) {
        super(cause);
    }

    public ScriptingException(String message, boolean showInDialog) {
        super(message);
        this.showInDialog = showInDialog;
    }

    /**
     * @param message
     * @param cause
     */
    public ScriptingException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

/* da: I think line information is superfluous, will be displayed in
 * problems view and visible in rulers.  So junk this method.
    public String getMessage() {
    	int line = -1;
    	try {
    	  if (errorDoc != null) line = errorDoc.getLineOfOffset(errorOffset);
    	} catch (Exception e) {}
    	 if (line>=0)
    		return super.getMessage()+" (at line "+(line+1)+")";
    	else
    	return super.getMessage();
    }
*/

  public boolean canGoToError() {
  	return (errorDoc!=null && errorOffset>=0 && errorDoc != null);
  }
	/**
	 * Try to scroll the view to display the error.
	 * @return true if we have fired a goto event (moving to the position still might not work)
	 */
	public boolean goToError() {
		if (!canGoToError()) {
			return false;
		}
		new RunnableWithParams(null) {
			public void run() {
				try {
					// FIXME da: this is in editor now.  Should switch to display
					// document first, then move to it.
					//errorDoc.scrollToViewPosition(errorOffset);
				} catch (Exception e) {
					System.err.println("Couldn't goto error location of "+this);
					if (ProofGeneralPlugin.debug(this)) {
						e.printStackTrace();
					}
				}
		  }
		}.callDefaultDisplayAsyncExec();
		return true;
	}

	/** whether the error is suitable for display in a dialog */
	public boolean isShowInDialog() {
		return showInDialog;
	}
}
