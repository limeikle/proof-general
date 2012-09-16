/*
 *  $RCSfile: PGCompletionProcessor.java,v $
 *
 *  Created on 18 Apr 2004
 *  part of Proof General for Eclipse
 */
package ed.inf.proofgeneral.editor;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationPresenter;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import ed.inf.proofgeneral.ProofGeneralPlugin;
import ed.inf.proofgeneral.document.ProofScriptDocument;
import ed.inf.proofgeneral.sessionmanager.ProverInfo;
import ed.inf.proofgeneral.sessionmanager.ProverKnowledge;
import ed.inf.proofgeneral.sessionmanager.ProverSyntax;
import ed.inf.proofgeneral.sessionmanager.SessionManager;
import ed.inf.proofgeneral.symbols.Symbol;

public class PGCompletionProcessor implements IContentAssistProcessor {

	//public String[] keywords = {};

	protected IContextInformationValidator fValidator= new Validator();

	public PGCompletionProcessor(SessionManager sm) {
		this.sessionMgr = sm;
	}
	private final SessionManager sessionMgr;
	private ProverSyntax syntax;
	private ProverInfo proverInfo = null;
	private ProverKnowledge proverKnowledge = null;

	ProverSyntax getProverSyntax() {
		if (syntax!=null) {
			return syntax;   //ok to cache
		}
		ProverInfo pi = getProverInfo();
		if (pi != null) {
			syntax = pi.proverSyntax;
		}
		return syntax;
	}

	ProverInfo getProverInfo() {
		if (proverInfo==null) {
			if (sessionMgr != null) {
				proverInfo=sessionMgr.proverInfo;
			}
		}
		return proverInfo;
	}

	ProverKnowledge getProverKnowledge() {
		//if (proverKnowledge!=null) return proverKnowledge;   //never cache
		try {
			proverKnowledge=sessionMgr.getProverState().getProverKnowledge();
		} catch (Exception e) {}
		return proverKnowledge;
	}

	/**
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
			int documentOffset) {
		try {
			ProofScriptDocument doc = (ProofScriptDocument)viewer.getDocument();
			int fl=1;
			try {
				while (documentOffset-fl>=0) {
					char c = doc.getChar(documentOffset-fl);
                    // FIXME da: we're hardwiring Isabelle  identifier convention & symbol prefix here
					if (!PGTextHover.isWordChar(c) && c!='.' && c!='<') {
						break;
					}
					fl++;
				} 
				if (documentOffset-fl<=0) {
					return null;  // No completions for empty fragment
				}
				if (doc.getChar(documentOffset-fl)=='\\') {
					String fragment = doc.get(documentOffset-fl,fl);
//					if (ProofGeneralPlugin.debug(this)) {
//						System.out.println("Symbol completion fragment: #"+fragment+"#");
//					}
					// add the symbols
					List<Symbol> symbols = doc.getProver().getSymbols().getAllActiveSymbolsWithPrefix(fragment);
					PGCompletionProposal[] allProposals = new PGCompletionProposal[symbols.size()];
					int i = 0;
					for (Symbol sym : symbols) {
						String appearance;
						String asym = sym.ascii;
						if (sym.unicode != null) {
							appearance = sym.unicode;
						} else if (sym.html != null) {
							appearance = "\toutput as HTML: " + sym.html; // TODO: convert to plain text
						} else if (sym.shortcut != null) {
							appearance = "\toutput as: " + sym.shortcut;
						} else {
							appearance = "";
						}
						String replacement;
						if (doc.isUsingSymbols() && sym.unicode != null) {
							replacement = sym.unicode;
							asym = replacement + " \t " + asym;
						} else {
							replacement = sym.ascii;
							asym = replacement + " \t" + appearance;
						}
						if (sym.shortcut != null) {
							asym = asym + "\t (shortcut: " + sym.shortcut + ")";
						}
						PGCompletionProposal comp =
								PGCompletionProposal.symbolProposal(replacement, 
										documentOffset-fragment.length(), 
										fragment.length(),
										asym);
						allProposals[i++]=comp;
					}
					return allProposals;
				}
			} catch (BadLocationException x) {
				x.printStackTrace();
				return null;
			}

			fl--; // ignore the last character
			if (fl == 0) {
				return null; // No characters, stop making completions
			}
			String fragment = doc.get(documentOffset-fl,fl);
//			if (ProofGeneralPlugin.debug(this)) {
//				System.out.println("Completion fragment: #"+fragment+"#");
//				//TODO being inside a quote should prompt a new context, where the quoted item at left is irrelevant
//				//this new context could allow all *_def items, as well as matching entries from the current proof state
//			}
			//TODO  don't use an array, cache some of this info, maybe
			TreeMap<String,CompletionProposal> allProposals = new TreeMap<String,CompletionProposal>();

			//add the prover knowledge items
		    for (Map.Entry kim : getProverKnowledge().itemsStartingWith(fragment).entrySet()) {
				String kin = (String)kim.getKey();
//				ProverKnowledge.IKnowledgeItem ki = (ProverKnowledge.IKnowledgeItem)kim.getValue();
//				String help =
//					//helpSupplier.getHelpString(kw,(ProofScriptDocument)doc);
//					"<b>"+ki.getType()+"</b> "+kin;
				IContextInformation info = null;   //not really helpful... could show things like params, etc
				//if (help!=null) info = new ContextInformation(help+" (ctx)", help+" (info)");
				allProposals.put(kin, new CompletionProposal(kin,
						documentOffset-fragment.length(),
						fragment.length(),
						kin.length(), null, kin,
						info, "SEARCH::"+kin
//						ki.type+" <b>"+ki.id+"</b>\n<p> <p>\n"+
//						(ki.getStatementHtml()!=null ? ki.getStatementHtml() :
//						"(not loaded into ProofGeneral yet; complete and do two passes of rollover to see more info)")
				));
				//TODO applied lemmas etc should place before the word if it is after a "_tac"
			}

			//add the keywords
			String[] keywords = getProverSyntax().getAllKeywords();
			for (String kw : keywords) {
				if (kw.startsWith(fragment)) {
					//String help = sessionMgr.hoverHelp.getHelpString(kw, (ProofScriptDocument)doc, null);
					   //now presenter gets this
					//IContextInformation info = null;
					//if (help!=null) info = new ContextInformation(help+" (ctx)", help+" (info)");   //not so useful
					allProposals.put(kw, new CompletionProposal(kw,
							documentOffset-fragment.length(),
							fragment.length(),
							kw.length(),null,kw,null,// da: was info
								"SEARCH::"+kw
					));
				}
			}


			return allProposals.values().toArray(new ICompletionProposal[allProposals.values().size()]);
		} catch (Exception e) {
			System.err.println("Unexpected error when computing code completion proposals: "+e);
			if (ProofGeneralPlugin.debug(this)) {
				e.printStackTrace();
			}
			return new ICompletionProposal[0];
		}
	}

	@SuppressWarnings("boxing")
    public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
		IContextInformation[] result= new IContextInformation[1];
		result[0]= new ContextInformation(
				MessageFormat.format("no proposals",
						new Object[] {}),
				MessageFormat.format("characters {1} to {2}",
								new Object[] { 0, documentOffset - 5, documentOffset + 5 }));

//		IContextInformation[] result= new IContextInformation[5];
//		for (int i= 0; i < result.length; i++)
//			result[i]= new ContextInformation(
//					MessageFormat.format("proposal {0} at position {1}",
//							new Object[] { new Integer(i), new Integer(documentOffset)}),
//							MessageFormat.format("proposal {0} valid from {1} to {2}",
//									new Object[] { new Integer(i), new Integer(documentOffset - 5), new Integer(documentOffset + 5)}));
		return result;
		//CompletionProcessor.Proposal.ContextInfo.pattern={0} valid 5 characters around insertion point
		//CompletionProcessor.Proposal.hoverinfo.pattern=Java keyword: {0}
	}




	public char[] getContextInformationAutoActivationCharacters() {
		//return null;
		return new char[] { '.', '\\' };
		  //new char[] { '#' };
	}


	public IContextInformationValidator getContextInformationValidator() {
		return fValidator;
	}


	public String getErrorMessage() {
		return null;
	}


	public char[] getCompletionProposalAutoActivationCharacters() {
		//return null;
		return new char[] {'.','\\'};  // TODO: in context only?
	}


	/**
	 * Simple content assist tip closer. The tip is valid in a range
	 * of 15 characters around its popup location.
	 */
	protected static class Validator implements IContextInformationValidator, IContextInformationPresenter {

		protected int fInstallOffset;
		public boolean isContextInformationValid(int offset) {
			return Math.abs(fInstallOffset - offset) < 15;
		}

		public void install(IContextInformation info, ITextViewer viewer, int offset) {
			fInstallOffset= offset;
		}

		public boolean updatePresentation(int documentPosition, TextPresentation presentation) {
			return false;
		}
	}

}
