/*
 *  This file is part of Proof General Eclipse
 *
 *  Created on Jan 13, 2007 by da
 *
 *  Copyright (C) University of Edinburgh and contributing authors.
 *
 */

package ed.inf.proofgeneral.editor.lazyparser.tests;

import static org.junit.Assert.assertEquals;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.eclipse.jface.text.Position;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ed.inf.proofgeneral.editor.lazyparser.Parser;

@RunWith(Parameterized.class)
/**
 */
public class FindMatchElementString {

	private Element e;
	private String s;
    private Position result;

    @Parameters
    public static Collection data() {
        return Arrays.asList(new Object[][]{
        		{"<foo/>", "foo", null},
        		{"<foo/>", "  <foo/>", new Position(2,6)},
        		{"  <foo/>", "<foo/>", new Position(0,6)}
        });
    }

  	private static SAXReader saxReader = new SAXReader();

  	public FindMatchElementString(String xmls, String s, Position result) throws DocumentException {
		Document document = saxReader.read(new StringReader(xmls));
		List list = document.content();
		assert list.size()==1 : "expected single element in test data";
		Element e = (Element) list.get(0);
        this.e = e;
        this.s = s;
		this.result = result;
    }

@Test
public void testFindMatchStringString() {
	Position p;
	p = Parser.findMatch(e,s);
	assertEquals(result,p);
}

}
