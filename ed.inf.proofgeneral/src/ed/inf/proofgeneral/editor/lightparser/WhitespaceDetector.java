/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 *  $RCSfile: WhitespaceDetector.java,v $
 *
 *  Created 2003 by IBM Corporation
 *  part of Eclipse (See copyright notice above)
 */
package ed.inf.proofgeneral.editor.lightparser;

import org.eclipse.jface.text.rules.IWhitespaceDetector;

/**
 * A white space detector, uses java.lang.Character defn of whitespace.
 * Sub-class if you need something different.
 */
public class WhitespaceDetector implements IWhitespaceDetector {

	/* (non-Javadoc)
	 * Method declared on IWhitespaceDetector
	 */
	public boolean isWhitespace(char character) {
		return Character.isWhitespace(character);
	}
}
