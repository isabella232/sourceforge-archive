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
    public void testTokenizer0()
    {
        QuotedStringTokenizer tok = 
            new QuotedStringTokenizer("abc\n\"d\\\"'\"\n'p\\',y'\nz");
        checkTok(tok,false,false);
    }

    /*
     * Test for String nextToken()
     */
    public void testTokenizer1()
    {
        QuotedStringTokenizer tok = 
            new QuotedStringTokenizer("abc, \"d\\\"'\",'p\\',y' z", 
                                      " ,");
        checkTok(tok,false,false);
    }

    /*
     * Test for String nextToken()
     */
    public void testTokenizer2()
    {
        QuotedStringTokenizer tok = 
            new QuotedStringTokenizer("abc, \"d\\\"'\",'p\\',y' z", " ,",
            false);
        checkTok(tok,false,false);
        
        tok = new QuotedStringTokenizer("abc, \"d\\\"'\",'p\\',y' z", " ,",
                                        true);
        checkTok(tok,true,false);
    }
    
    /*
     * Test for String nextToken()
     */
    public void testTokenizer3()
    {
        QuotedStringTokenizer tok;
        
        tok = new QuotedStringTokenizer("abc, \"d\\\"'\",'p\\',y' z", " ,",
                                        false,false);
        checkTok(tok,false,false);
        
        tok = new QuotedStringTokenizer("abc, \"d\\\"'\",'p\\',y' z", " ,",
                                        false,true);
        checkTok(tok,false,true);
        
        tok = new QuotedStringTokenizer("abc, \"d\\\"'\",'p\\',y' z", " ,",
                                        true,false);
        checkTok(tok,true,false);
        
        tok = new QuotedStringTokenizer("abc, \"d\\\"'\",'p\\',y' z", " ,",
                                        true,true);
        checkTok(tok,true,true);
    }
    
    private void checkTok(QuotedStringTokenizer tok,boolean delim,boolean quotes)
    {
        assertTrue(tok.hasMoreElements());
        assertTrue(tok.hasMoreTokens());
        assertEquals("abc",tok.nextToken());
        if (delim)assertEquals(",",tok.nextToken());
        if (delim)assertEquals(" ",tok.nextToken());
            
        assertEquals(quotes?"\"d\\\"'\"":"d\"'",tok.nextElement());
        if (delim)assertEquals(",",tok.nextToken());
        assertEquals(quotes?"'p\\',y'":"p',y",tok.nextToken());
        if (delim)assertEquals(" ",tok.nextToken());
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
