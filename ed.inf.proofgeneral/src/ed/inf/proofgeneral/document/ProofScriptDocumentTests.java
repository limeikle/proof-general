/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Dec 27, 2006 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *    
 */
// da: commented this out temporarily because it prevents the product build working.
// What is a good way of including unit tests within the same project but excluding
// them from the product build?

//
//package ed.inf.proofgeneral.document;
//
//import static org.junit.Assert.*;
//
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//
///**
// * Tests for ProofScriptDocument class.
// * 
// * PLAN:
// * - Generate some static documents based on fixed output from parseresult
// *   (e.g. strings here with XML content (best), or maybe test files from a resource).
// * - Write tests for methods for creating and manipulating document contents,
// *   using cleaned up ProofScriptDocument API.
// *
// */
//
//public class ProofScriptDocumentTests {
//
//	/**
//	 * @throws java.lang.Exception
//	 */
//	@Before
//	public void setUp() throws Exception {
//	}
//
//	/**
//	 * @throws java.lang.Exception
//	 */
//	@After
//	public void tearDown() throws Exception {
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#getProverSyntax()}.
//	 */
//	@Test
//	public void testGetProverSyntax() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#setLockOffset(int)}.
//	 */
//	@Test
//	public void testSetLockOffset() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#setProcessedOffset(int)}.
//	 */
//	@Test
//	public void testSetProcessedOffset() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#setParseOffset(int)}.
//	 */
//	@Test
//	public void testSetParseOffset() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#getLockOffset()}.
//	 */
//	@Test
//	public void testGetLockOffset() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#getProcessedOffset()}.
//	 */
//	@Test
//	public void testGetProcessedOffset() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#getEditOffset()}.
//	 */
//	@Test
//	public void testGetEditOffset() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#getParseOffset()}.
//	 */
//	@Test
//	public void testGetParseOffset() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#getTitle()}.
//	 */
//	@Test
//	public void testGetTitle() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#getSyntax()}.
//	 */
//	@Test
//	public void testGetSyntax() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#getResource()}.
//	 */
//	@Test
//	public void testGetResource() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#getRootElement()}.
//	 */
//	@Test
//	public void testGetRootElement() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#getOpenElement()}.
//	 */
//	@Test
//	public void testGetOpenElement() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#setOpenElement(ed.inf.proofgeneral.sessionmanager.ContainerElement)}.
//	 */
//	@Test
//	public void testSetOpenElement() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#closeOpenElement()}.
//	 */
//	@Test
//	public void testCloseOpenElement() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#createMarker(ed.inf.proofgeneral.document.DocElement)}.
//	 */
//	@Test
//	public void testCreateMarker() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#fireParseTreeChangedEvent(int, int)}.
//	 */
//	@Test
//	public void testFireParseTreeChangedEvent() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#getText(org.eclipse.jface.text.Position)}.
//	 */
//	@Test
//	public void testGetText() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#setEditOffset(int)}.
//	 */
//	@Test
//	public void testSetEditOffset() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#openContainer(ed.inf.proofgeneral.document.DocElement)}.
//	 */
//	@Test
//	public void testOpenContainer() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#unlock()}.
//	 */
//	@Test
//	public void testUnlock() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#unlock(int)}.
//	 */
//	@Test
//	public void testUnlockInt() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#lock()}.
//	 */
//	@Test
//	public void testLock() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#lockCommandsUpto(int)}.
//	 */
//	@Test
//	public void testLockCommandsUpto() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#partitionChangeBroadcast(int, int)}.
//	 */
//	@Test
//	public void testPartitionChangeBroadcast() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#isScrollingOnAction()}.
//	 */
//	@Test
//	public void testIsScrollingOnAction() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#setScrollOnAction(boolean)}.
//	 */
//	@Test
//	public void testSetScrollOnAction() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#commandSent(ed.inf.proofgeneral.document.DocElement)}.
//	 */
//	@Test
//	public void testCommandSent() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#scrollToViewPosition(int)}.
//	 */
//	@Test
//	public void testScrollToViewPosition() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#commandProcessed(ed.inf.proofgeneral.document.DocElement)}.
//	 */
//	@Test
//	public void testCommandProcessed() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#commandUndone(ed.inf.proofgeneral.document.DocElement)}.
//	 */
//	@Test
//	public void testCommandUndone() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#findPrevious(java.lang.String, int)}.
//	 */
//	@Test
//	public void testFindPrevious() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#init(java.lang.String, ed.inf.proofgeneral.sessionmanager.PGIPSyntax, ed.inf.proofgeneral.sessionmanager.ProverSyntax, org.eclipse.core.resources.IResource)}.
//	 */
//	@Test
//	public void testInit() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#ProofScriptDocument()}.
//	 */
//	@Test
//	public void testProofScriptDocument() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#isEditAllowed(int, int)}.
//	 */
//	@Test
//	public void testIsEditAllowed() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#replace(int, int, java.lang.String)}.
//	 */
//	@Test
//	public void testReplaceIntIntString() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#set(java.lang.String)}.
//	 */
//	@Test
//	public void testSetString() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#forceSet(java.lang.String)}.
//	 */
//	@Test
//	public void testForceSet() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#isLocked()}.
//	 */
//	@Test
//	public void testIsLocked() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#isFullyProcessed()}.
//	 */
//	@Test
//	public void testIsFullyProcessed() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#isFullyUnprocessed()}.
//	 */
//	@Test
//	public void testIsFullyUnprocessed() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#isActiveForScripting()}.
//	 */
//	@Test
//	public void testIsActiveForScripting() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#setActiveForScripting()}.
//	 */
//	@Test
//	public void testSetActiveForScripting() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#clearActiveForScripting()}.
//	 */
//	@Test
//	public void testClearActiveForScripting() {
//		fail("Not yet implemented"); // TODO
//	}
//
//	/**
//	 * Test method for {@link ed.inf.proofgeneral.document.ProofScriptDocument#toString()}.
//	 */
//	@Test
//	public void testToString() {
//		fail("Not yet implemented"); // TODO
//	}
//
//}
