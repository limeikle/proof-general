/*
 *  $RCSfile: HelpfulControl.java,v $
 *
 *  Created on 27 Oct 2006
 *  part of Proof General for Eclipse
 */
package ed.inf.utils.eclipse.customswt;

import org.eclipse.swt.widgets.Control;

/**
 * Embedded interface.  Classes using HelpfulController should themselves
 * implement HelpfulControl.
 *
 * @see HelpfulController
 */
public interface HelpfulControl {
	
	/**
	 * Allows controls to be obtained for the tooltip and help to be set.
	 */
	public Control[] getControls();

	/**
	 * Sets the tooltip text.
	 * Recommended to call {@link HelpfulController#setToolTipText(HelpfulControl, String)}.
	 */
	public void setToolTipText(String text);

	/**
	 * Sets the context help.
	 * Recommended to call {@link HelpfulController#setToolTipText(HelpfulControl, String)}.
	 */
	public void setHelp(String helpContextId);

}