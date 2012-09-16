/*
 *  $RCSfile: PGCodeScanner.java,v $
 *
 *  Created on 22 Apr 2004 by Daniel Winterstein
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.lightparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.preferences.PreferenceNames;
import ed.inf.proofgeneral.sessionmanager.PGIPSyntax;
import ed.inf.proofgeneral.sessionmanager.ProverSyntax;
import ed.inf.proofgeneral.sessionmanager.ProverSyntax.ISyntaxChangeListener;
import ed.inf.proofgeneral.ui.theme.PGColors.PGColor;
import ed.inf.utils.datastruct.ArrayListOperations;

/**
 * Code scanner used to do syntax highlighting.
 * Defines comments and strings, and colours keywords (using PGIPSyntax).
 * @see PGIPSyntax
 * 
 * @author Daniel Winterstein
 * @author David Aspinall
 */

// TODO: Make only one token syntax per prover syntax (construct based on prover
// syntax instead of document, make singleton per syntax).  Wait until we have
// marker annotations working/profiled before doing this.

public class PGCodeScanner extends RuleBasedScanner {

    protected ProofScriptDocument doc;
    private final ProverSyntax proverSyntax;

	/**
	 * Token maps, map normal tokens to locked, etc. variants
	 */
	private final Map<Token,Token> lockedTokens = new HashMap<Token,Token>();
	private final Map<Token,Token> processedTokens = new HashMap<Token,Token>();
	//private Map<Token,Token> nextStepTokens = new HashMap<Token,Token>();

	/**
	 * Creates a Proof Script code scanner for syntax highlighting.
	 * Note that dependency on document here is used to make scanner
	 * that depends on processed position for syntax highlighting!
	 */
	public PGCodeScanner(ProofScriptDocument doc) {
	    this.doc = doc;
	    this.proverSyntax = doc.getProverSyntax();
	    proverSyntax.addSyntaxChangeListener(new ISyntaxChangeListener() {
			public void syntaxChangeEvent() {
	            update();
            }
	    });
	    update();
	}
	
	/**
	 * Update the scanner
	 */
	public void update() {
		List<IRule> rules= this.makeRules();
		IRule[] result= new IRule[rules.size()];
		rules.toArray(result);
		setRules(result);
	}
	
	/**
	 * Makes tokens, sets the default token and makes the rules.
	 * @return a list of rules.
	 */
	protected List<IRule> makeRules() {

		IToken tokenKEYWORD = makeToken(PGIPSyntax.KEYWORD);
		IToken tokenTRIGGER = makeToken(PGIPSyntax.TRIGGERWORD);
		IToken tokenSTRING = makeToken(PGIPSyntax.STRING);
		IToken tokenDEFAULT = makeToken(PGIPSyntax.DEFAULT);
		setDefaultReturnToken(tokenDEFAULT);

	    List<IRule> rules = new ArrayList<IRule>();
		// Add rule for single line comments.
		//rules.add(new EndOfLineRule("//", comment));

	    // FIXME da: sometimes the document or prover syntax hasn't been set (e.g. loading
	    // an external file).  Workaround for now to prevent ugly messages in broken views.
	    if (doc!=null && proverSyntax!=null) {
	    	
	    	// Add rules for strings
    		char esc = proverSyntax.escapeCharacter;
	    	for (char sd : proverSyntax.stringDelimiters) {
	    		rules.add(new MultiLineRule(String.valueOf(sd), String.valueOf(sd), tokenSTRING, esc));
	    	}

	    	// Note that you can only have one WordRule per scanner, or they block each other.
	    	WordRule wordRule= new WordRule(new WordDetector(), tokenDEFAULT);

	    	// Add word rule for triggers.
	    	for (int i= 0; i < proverSyntax.triggerWords.length; i++) {
	    		wordRule.addWord(proverSyntax.triggerWords[i], tokenTRIGGER);
	    	}
	    	
	    	// Add word rule for keywords.
	    	// TODO: different colours for different types
	    	String[] keywords= proverSyntax.getAllKeywords();
	    	for (int i= 0; i < keywords.length; i++) {
	    		if (ArrayListOperations.member(keywords[i],proverSyntax.triggerWords)) {
	    			continue; // already has a colouring
	    		}
	    		wordRule.addWord(keywords[i], tokenKEYWORD);
	    	}
	    	
	    	rules.add(wordRule);
	    }
		return rules;
	}

    /**
     * Tokeniser method.
     * @see org.eclipse.jface.text.rules.RuleBasedScanner#nextToken()
     */
    @Override
    public IToken nextToken() {
    	Token token = (Token) super.nextToken();
    	if (!token.isOther()) {
    		return token; //TODO this ignores locking on default tokens!
    	}
    	if (ProofGeneralPlugin.getBooleanPref(PreferenceNames.PREF_DEBUG_USE_MARKER_ANNOTATIONS)) {
    		return token;
    	}
    	int offset = getTokenOffset();
    	if (offset>doc.getLockOffset()) {
    		return token;
    	} else if (offset>doc.getProcessedOffset()) {
    		return lockedTokens.get(token);
    	} else {
    		return processedTokens.get(token);
    	}
    }

    /**
     * Make the tokens for this syntax type
     * Returns the normal (unlocked) token
     * Other tokens are stored in the token Maps, with the normal token as the key
     * @param type
     * @return a tokenised string
     */
    public Token makeToken(String type) {
        return makeToken(type,SWT.NORMAL);
    }

	/**
     * Make the tokens for this syntax type
     * Returns the normal (unlocked) token
     * Other tokens are stored in the token Maps, with the normal token as the key
     * @param type
     * @return a string, tokenised with the required style
     */
    public Token makeToken(String type,int swtStyle) {
        Token t;
        Token lt;
        Token pt;
        Color fg = PGColor.getColorForTokenType(type);
        t = new Token(new TextAttribute(fg,null,swtStyle));
        // NB: with marker code, next 4 lines are not needed.
        lt = new Token(new TextAttribute(fg,PGColor.BUSY.get(),swtStyle));
        pt = new Token(new TextAttribute(fg,PGColor.PROCESSED.get(),swtStyle));
        // org.eclipse.ui.IWorkbench.getThemeManager()
        // da: don't think this state is used currently.  It could become the redo area.
        // nst = new Token(new TextAttribute(fg,colorProvider.getColor(PGIPSyntax.NEXTSTEP),swtStyle));
        lockedTokens.put(t,lt);
        processedTokens.put(t,pt);
        // nextStepTokens.put(t,nst);
        return t;
    }

}
