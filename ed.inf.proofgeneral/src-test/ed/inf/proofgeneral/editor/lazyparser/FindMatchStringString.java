/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jan 13, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *
 */

package ed.inf.proofgeneral.editor.lazyparser;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.jface.text.Position;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ed.inf.proofgeneral.editor.lazyparser.Parser;

@RunWith(Parameterized.class)
/**
 * Test class for {@link ed.inf.proofgeneral.editor.lazyparser.Parser#findMatch(java.lang.String, java.lang.String)}.
 *
 */
public class FindMatchStringString {

		private String s1;
		private String s2;
	    private Position result;

	    @Parameters
	    public static Collection data() {
	        return Arrays.asList(new Object[][]{
	        		{" foo", "   ", null},
	        		{" foo", "    f", null},
	                {"foo", "bar", null},
	                {"foo", "foo", new Position(0,3)},
	                {"foob", "foobar", new Position(0,4)},
	                {"foo", "  foo", new Position(2,3)},
	                {"  foo", "foo", new Position(0,3)},
	                {"f o o", "foo", null}
	        });
	    }

	    public FindMatchStringString(String s1, String s2, Position result) {
	        this.s1 = s1;
	        this.s2 = s2;
	        this.result = result;
	    }

	@Test
	public void testFindMatchStringString() {
		Position p;
		p = Parser.findMatch(s1,s2);
		assertEquals(result,p);
	}

}
