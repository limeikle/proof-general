/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jan 11, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *
 */

package ed.inf.proofgeneral.document;


/**
 * This class will contain methods for generating queues of undo commands to send
 * to the theorem prover on do/undo actions.  Calculations are made based
 * on the XML shadow tree contents of the document (which is always in
 * sync with the document for parsed regions).  In particular, undo commands
 * are calculated following the PGIP model using the document contents
 * rather than a secondary history of previously sent commands.  Compared
 * with the previous version, this new code also allows undo commands to
 * fail, rather than assuming they always succeed.
 * @author David Aspinall
 */

public class DocScripting {


	// TODO

}
