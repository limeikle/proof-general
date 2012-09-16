/*
 *  $RCSfile: WordDetector.java,v $
 *
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.lightparser;

import org.eclipse.jface.text.rules.IWordDetector;

/**
 * A word detector. Uses java.lang. Subclass if you need something different.
 */
public class WordDetector implements IWordDetector {

	/**
	 * {@link IWordDetector#isWordPart(char)}
	 */
	public boolean isWordPart(char character) {
		// System.err.print("_"+character); // DEBUG
		return Character.isJavaIdentifierPart(character);
	}
	
	/**
	 * {@link IWordDetector#isWordStart(char)}
	 */
	public boolean isWordStart(char character) {
		// System.err.print("|"+character); // DEBUG
		return Character.isJavaIdentifierStart(character);
		//if ( Character.isJavaIdentifierStart(character) ) return true;
		//System.out.println("char-"+Character.getNumericValue(character));
		//if ( Character.getNumericValue(character) == -1 ) return true;
		//return false;
	}
}
