/*
 *  $RCSfile: CommandQueuedWithValues.java,v $
 *
 *  Created on 03 May 2005 by Alex Heneveld
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.sessionmanager;

import ed.inf.proofgeneral.document.CmdElement;
import ed.inf.proofgeneral.sessionmanager.events.IPGIPListener;
import ed.inf.utils.datastruct.MutableObject;


public class CommandQueuedWithValues extends CommandQueued {

	Boolean backgroundThread = null;
	MutableObject privateListener = null;

	/** as CommandQueued, but allows setting values (set null to use previous)
	 * @param command
	 * @param cause
	 * @param backgroundThread
	 * @param privateListener the PGIPListener wrapped; if null use previous,
	 * if wrapped null, explicitly set no private listeners for this command
	 * @param manager TODO
	 */
	public CommandQueuedWithValues(SessionManager manager, CmdElement command, Object cause,
			Boolean backgroundThread, 
			MutableObject privateListener) {
		super(manager, command, cause);
		this.backgroundThread = backgroundThread;
		this.privateListener = privateListener;
	}
	Boolean tempBkgdThread = null;
	Boolean tempLogging = null;
	MutableObject tempListener = null;
	@Override
	public void preFire() {
		if (backgroundThread!=null) {
			tempBkgdThread = Boolean.valueOf(this.sessionManager.dontProcessOutputInDisplayThread);
			this.sessionManager.dontProcessOutputInDisplayThread = backgroundThread.booleanValue();
		}
//		if (logging!=null) {
//		tempLogging = Boolean.valueOf(this.sessionManager.logging);
//		this.sessionManager.logging = logging.booleanValue();
//		}
		if (privateListener!=null) {
			tempListener = new MutableObject(this.sessionManager.getEventQueuePrivateListener());
			this.sessionManager.setEventQueuePrivateLister((IPGIPListener)privateListener.get());
		}
	}
	@Override
	public void postFire() {
		if (tempBkgdThread!=null) {
			this.sessionManager.dontProcessOutputInDisplayThread = tempBkgdThread.booleanValue();
		}
//		if (tempLogging!=null) {
//		this.sessionManager.logging = tempLogging.booleanValue();
//		}
		if (tempListener!=null) {
			this.sessionManager.setEventQueuePrivateLister((IPGIPListener)tempListener.get());
		}
	}
}