/*
 *  $RCSfile: PartitionScanner.java,v $
 *
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor.lightparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;

import ed.inf.proofgeneral.editor.PGSourceViewerConfiguration;
import ed.inf.proofgeneral.sessionmanager.PGIPSyntax;
import ed.inf.proofgeneral.sessionmanager.ProverInfo;
import ed.inf.proofgeneral.sessionmanager.ProverSyntax;
import ed.inf.proofgeneral.sessionmanager.ProverSyntax.Tag;

/**
 * Sets up the syntax comments, strings, and special areas.
 */
public class PartitionScanner extends RuleBasedPartitionScanner {

    /** Set of singleton instances, one per syntax obejct. */
    private static Map<ProverInfo, PartitionScanner> defScanners = new HashMap<ProverInfo, PartitionScanner>();

	protected ProverSyntax pSyntax;

    public static final String LIGHT_PG_PARTITIONING = "__light_pg_partitioning";
    public static final String COMMENT = PGIPSyntax.COMMENT;
    public static final String DEFAULT = PGIPSyntax.DEFAULT;
    public static final String PGTAG = PGIPSyntax.PGTAG.name;
    public static final String[] PARTITION_TYPES= new String[] {COMMENT,PGTAG}; // MS
    public static final String[] LIGHT_PARSE_TYPES= new String[] {DEFAULT,COMMENT,PGTAG}; // MS

    private static final IToken pgtagTOKEN = new Token(PGIPSyntax.PGTAG);
	private static final IToken commentTOKEN= new Token(PGIPSyntax.COMMENT);
    
    /**
     * Get (or make) the partition scanner for the given syntax object.
     */
    public synchronized static PartitionScanner getDefault(ProverInfo pi) {
        if (!defScanners.containsKey(pi)) {
            defScanners.put(pi,new PartitionScanner(pi));
        }
        return defScanners.get(pi);
    }

	/**
	 * Creates the partitioner and sets up the appropriate rules.
	 */
	protected PartitionScanner(ProverInfo pi) {
		super();
		pSyntax = pi.proverSyntax;
		setRules();
	}

	/**
	 * Return the partitioning types for the given prover.  
	 * Used in @{link {@link PGSourceViewerConfiguration#getConfiguredContentTypes(org.eclipse.jface.text.source.ISourceViewer)}}
	 * @param pi
	 * @return partition types for the prover given
	 */
	public static String[] getPartitionTypes(ProverInfo pi) {
		// TODO da: add partition types for prover-depenedent regions, possibly strings
		return new String[] { IDocument.DEFAULT_CONTENT_TYPE,
		         			  COMMENT };		
	}
	
	
	/*
	 * Set the syntax rules
	 */
	protected void setRules() {

		List<IPredicateRule> rules= new ArrayList<IPredicateRule>();

		// Add rule for strings and character constants.
		for (char c : pSyntax.stringDelimiters) {
			String sd = String.valueOf(c);
			rules.add(new SingleLineRule(sd, sd, Token.UNDEFINED, pSyntax.escapeCharacter));
		}

		// Add rules for comments
        for (Tag commentTag : pSyntax.commentTags) {
        	if (commentTag.end == null) {
        		rules.add(new SingleLineRule(commentTag.start, 
        				null, // da: new, test "\n", // FIXME: prover LINEEND?
        				commentTOKEN));
        	} else {
        		// FIXME TRAC #77 Can we make multiline comments here?
        		rules.add(new MultiLineRule(commentTag.start, commentTag.end,
        				commentTOKEN, (char) 0, true));
        	}
        }

        // add a rule for special areas
		rules.add(new MultiLineRule(PGIPSyntax.PGTAG.start, PGIPSyntax.PGTAG.end, pgtagTOKEN, (char) 0, true));

		IPredicateRule[] result= new IPredicateRule[rules.size()];
		rules.toArray(result);
		setPredicateRules(result);
	}


}
