/*
 * Created on 7/01/2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.mortbay.util;

import junit.framework.TestCase;

/**
 * @author gregw
 *
 */
public class QuotedStringTokenizerTest extends TestCase
{

    /**
     * Constructor for QuotedStringTokenizerTest.
     * @param arg0
     */
    public QuotedStringTokenizerTest(String arg0)
    {
        super(arg0);
    }

    /*
     * Test for String nextToken()
     */
    public void testTokenizer()
    {
        QuotedStringTokenizer tok = 
            new QuotedStringTokenizer("abc, \"d\\\"'\", 'p\\',y' z", " ,",
            false,false);
            
        assertTrue(tok.hasMoreElements());
        assertTrue(tok.hasMoreTokens());
        assertEquals("abc",tok.nextToken());
        
        assertEquals("d\"'",tok.nextElement());
        assertEquals("p',y",tok.nextToken());
        assertEquals("z",tok.nextToken());
        assertFalse(tok.hasMoreTokens());
    }

    /*
     * Test for String quote(String, String)
     */
    public void testQuoteString()
    {
        assertEquals("abc",QuotedStringTokenizer.quote("abc", " ,"));
        assertEquals("\"a c\"",QuotedStringTokenizer.quote("a c", " ,"));
        assertEquals("\"a'c\"",QuotedStringTokenizer.quote("a'c", " ,"));   
    }


    public void testUnquote()
    {
        assertEquals("abc",QuotedStringTokenizer.unquote("abc"));
        assertEquals("a\"c",QuotedStringTokenizer.unquote("\"a\\\"c\""));
        assertEquals("a'c",QuotedStringTokenizer.unquote("\"a'c\""));
    }

}
