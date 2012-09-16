/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jul 8, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */

package ed.inf.proofgeneral.sessionmanager;

import java.util.HashMap;
import java.util.Map;

import ed.inf.proofgeneral.ProofGeneralProverRegistry;
import ed.inf.proofgeneral.ProofGeneralProverRegistry.Prover;
import ed.inf.proofgeneral.ProofGeneralProverRegistry.ProverRegistryException;

/**
 * Factory class for constructing session managers and associating them with 
 * provers.
 * @author David Aspinall
 * @author Daniel Winterstein
 * @author Alex Heneveld
 */
public class SessionManagerFactory {

	/**
	 * A session manager handles communications with a prover.
	 * For each prover, we have exactly one session manager;
	 * this is an association from provers to session managers.
	 */
	protected static final Map<String, SessionManager> sessionManagers =
				new HashMap<String, SessionManager>();

	/** The registry of provers. */
	private final ProofGeneralProverRegistry registry;
	
	/**
	 * Create a session manager factory, which creates session managers
	 * on demand, and records the association between prover names and session
	 * managers.
	 * @param registry the global registry of provers, which will be associated with the factory
	 */
	public SessionManagerFactory(ProofGeneralProverRegistry registry) {
		this.registry = registry;
	}
	
	/**
	 * Select the appropriate session manager (create it if necc.)
	 * @param fileName
	 * @return appropriate prover session manager, or null if not available (eg is shutting down)
	 */
	public SessionManager getSessionManagerForFile(String fileName) throws ProverRegistryException {
		int lastDot = fileName.lastIndexOf('.');
		if (lastDot == -1) {
			throw new ProverRegistryException("No file extension found: filename to prover association uses extensions.");
		}
		String fileExtn = fileName.substring(lastDot+1);
		Prover prover = registry.getProverForFileExtension(fileExtn);
		String pName = prover.getName();
		return getSessionManagerForProver(pName);
	}

	/**
	 * Test to see if we have a session manager (and prover) configured for the
	 * given file name.  This may result in the session manager being created
	 * (and prover started) as a side-effect.
	 * @param fileName
	 * @return true if there is a session manager for the given file name 
	 */
	public boolean hasSessionManagerForFile(String fileName) {
		try {
			getSessionManagerForFile(fileName);
			return true;
		} catch (ProverRegistryException e) {
			return false;
		}
	}
	
	/**
	 * Gets the Session Manager controlling the given prover.  If there is
	 * no session manager associated, one is created.
	 * @param proverName
	 * @return the session manager for this prover.
	 */
	public SessionManager getSessionManagerForProver(String proverName) throws ProverRegistryException {
		synchronized (sessionManagers) {
			SessionManager sm = sessionManagers.get(proverName); 
			if (sm != null) {
				return sm;
			}
			Prover prover = registry.getProver(proverName);
			sm = new SessionManager(prover);
			sessionManagers.put(proverName,sm);
			return sm;
		}
	}

	/**
	 * @return all open session managers
	 */
	public SessionManager[] getSessionManagers() {
		SessionManager[] sms = new SessionManager[sessionManagers.size()];
		return sessionManagers.values().toArray(sms);
	}
	
	/**
	 * @return the number of initialised session managers
	 */
	public int numberOfSessionManagers() {
		return sessionManagers.size();
	}
}
