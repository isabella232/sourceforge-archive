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
public class StringUtilTest extends TestCase
{

    /**
     * Constructor for StringUtilTest.
     * @param arg0
     */
    public StringUtilTest(String arg0)
    {
        super(arg0);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testAsciiToLowerCase()
    {
        String lc="ábc def 1ñ3";
        assertEquals(StringUtil.asciiToLowerCase("áBc DeF 1ñ3"), lc);
        assertTrue(StringUtil.asciiToLowerCase(lc)==lc);
    }

    public void testStartsWithIgnoreCase()
    {
        
        assertTrue(StringUtil.startsWithIgnoreCase("ábádefg", "ábá"));
        assertTrue(StringUtil.startsWithIgnoreCase("ábcdefg", "ábc"));
        assertTrue(StringUtil.startsWithIgnoreCase("ábcdefg", "áBc"));
        assertTrue(StringUtil.startsWithIgnoreCase("áBcdefg", "ábc"));
        assertTrue(StringUtil.startsWithIgnoreCase("áBcdefg", "áBc"));
        assertTrue(StringUtil.startsWithIgnoreCase("ábcdefg", ""));
        assertTrue(StringUtil.startsWithIgnoreCase("ábcdefg", null));
        assertTrue(StringUtil.startsWithIgnoreCase("ábcdefg", "ábcdefg"));

        assertFalse(StringUtil.startsWithIgnoreCase(null, "xyz")); 
        assertFalse(StringUtil.startsWithIgnoreCase("ábcdefg", "xyz"));
        assertFalse(StringUtil.startsWithIgnoreCase("á", "xyz")); 
    }

    public void testEndsWithIgnoreCase()
    {
        assertTrue(StringUtil.endsWithIgnoreCase("ábcdáfá", "áfá"));
        assertTrue(StringUtil.endsWithIgnoreCase("ábcdefg", "efg"));
        assertTrue(StringUtil.endsWithIgnoreCase("ábcdefg", "eFg"));
        assertTrue(StringUtil.endsWithIgnoreCase("ábcdeFg", "efg"));
        assertTrue(StringUtil.endsWithIgnoreCase("ábcdeFg", "eFg"));
        assertTrue(StringUtil.endsWithIgnoreCase("ábcdefg", ""));
        assertTrue(StringUtil.endsWithIgnoreCase("ábcdefg", null));
        assertTrue(StringUtil.endsWithIgnoreCase("ábcdefg", "ábcdefg"));

        assertFalse(StringUtil.endsWithIgnoreCase(null, "xyz")); 
        assertFalse(StringUtil.endsWithIgnoreCase("ábcdefg", "xyz"));
        assertFalse(StringUtil.endsWithIgnoreCase("á", "xyz"));  
    }

    public void testIndexFrom()
    {
        assertEquals(StringUtil.indexFrom("ábcd", "xyz"),-1);
        assertEquals(StringUtil.indexFrom("ábcd", "ábcz"),0);
        assertEquals(StringUtil.indexFrom("ábcd", "bcz"),1);
        assertEquals(StringUtil.indexFrom("ábcd", "dxy"),3);
    }

    public void testReplace()
    {
        String s="ábc ábc ábc";
        assertEquals(StringUtil.replace(s, "ábc", "xyz"),"xyz xyz xyz");
        assertTrue(StringUtil.replace(s,"xyz","pqy")==s);
        
        s=" ábc ";
        assertEquals(StringUtil.replace(s, "ábc", "xyz")," xyz ");
        
    }

    public void testUnquote()
    {
        String uq =" not quoted ";
        assertTrue(StringUtil.unquote(uq)==uq);
        assertEquals(StringUtil.unquote("' quoted string '")," quoted string ");
        assertEquals(StringUtil.unquote("\" quoted string \"")," quoted string ");
        assertEquals(StringUtil.unquote("' quoted\"string '")," quoted\"string ");
        assertEquals(StringUtil.unquote("\" quoted'string \"")," quoted'string ");
    }


    public void testNonNull()
    {
        String nn="";
        assertTrue(nn==StringUtil.nonNull(nn));
        assertEquals("",StringUtil.nonNull(null));
    }

    /*
     * Test for boolean equals(String, char[], int, int)
     */
    public void testEqualsStringcharArrayintint()
    {
        assertTrue(StringUtil.equals("ábc", new char[] {'x','á','b','c','z'},1,3));
        assertFalse(StringUtil.equals("axc", new char[] {'x','a','b','c','z'},1,3));
    }

    public void testAppend()
    {
        StringBuffer buf = new StringBuffer();
        buf.append('a');
        StringUtil.append(buf, "abc", 1, 1);
        StringUtil.append(buf, (byte)12, 16);
        StringUtil.append(buf, (byte)16, 16);
        StringUtil.append(buf, (byte)-1, 16);
        StringUtil.append(buf, (byte)-16, 16);
        assertEquals("ab0c10fff0",buf.toString());
        
    }
}
