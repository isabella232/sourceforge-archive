// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.security;

import java.io.UnsupportedEncodingException;

import junit.framework.TestSuite;

import org.mortbay.util.StringUtil;


/* ------------------------------------------------------------ */
/** Util meta Tests.
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class SecurityTest extends junit.framework.TestCase
{
    public SecurityTest(String name)
    {
      super(name);
    }
    
    public static junit.framework.Test suite() {
        TestSuite suite = new TestSuite(SecurityTest.class);
        return suite;                  
    }

    /* ------------------------------------------------------------ */
    /** main.
     */
    public static void main(String[] args)
    {
      junit.textui.TestRunner.run(suite());
    }    
    
    
    /* ------------------------------------------------------------ */
    public static void testB64()
        throws UnsupportedEncodingException
    {
	    // Perform basic reversibility tests
       assertEquals("decode(encode())","",       B64Code.decode(B64Code.encode("")));
       assertEquals("decode(encode(a))","a",      B64Code.decode(B64Code.encode("a")));
       assertEquals("decode(encode(ab))","ab",     B64Code.decode(B64Code.encode("ab")));
       assertEquals("decode(encode(abc))","abc",    B64Code.decode(B64Code.encode("abc")));
       assertEquals("decode(encode(abcd))","abcd",   B64Code.decode(B64Code.encode("abcd")));
       assertEquals("decode(encode(^@))","\000",     B64Code.decode(B64Code.encode("\000")));
       assertEquals("decode(encode(a^@))","a\000",    B64Code.decode(B64Code.encode("a\000")));
       assertEquals("decode(encode(ab^@))","ab\000",   B64Code.decode(B64Code.encode("ab\000")));
       assertEquals("decode(encode(abc^@))","abc\000",  B64Code.decode(B64Code.encode("abc\000")));
       assertEquals("decode(encode(abcd^@))","abcd\000", B64Code.decode(B64Code.encode("abcd\000")));

	    // Encoder compatibility tests
	    assertEquals("encode(abc)",         B64Code.encode("abc"),     "YWJj");
	    assertEquals("encode(abcd)",     B64Code.encode("abcd"),    "YWJjZA==");
	    assertEquals("encode(abcde)",     B64Code.encode("abcde"),   "YWJjZGU=");
	    assertEquals("encode(abcdef)",     B64Code.encode("abcdef"),  "YWJjZGVm");
	    assertEquals("encode(abcdefg)", B64Code.encode("abcdefg"), "YWJjZGVmZw==");

       // Test the reversibility of the full range of 8 bit values
	    byte[] allValues= new byte[256];
	    for (int i=0; i<256; i++)
         allValues[i] = (byte) i;
	    String input = new String(allValues, StringUtil.__ISO_8859_1);
            String output=B64Code.decode(B64Code.encode(input));

            for (int i=0;i<256;i++)
              assertEquals("DIFF at "+i, (int)output.charAt(i), (int)input.charAt(i));
	    assertEquals( "decode(encode(ALL_128_ASCII_VALUES))", output,input);

    }
    
    
    /* ------------------------------------------------------------ */
    public void testPassword()
    {
        Password f1 = new Password("Foo");
        Password f2 = new Password(Password.obfuscate("Foo"));
        
        Password b1 = new Password("Bar");
        Password b2 = new Password(Password.obfuscate("Bar"));

        assertTrue("PW to PW",   f1.equals(f1));
        assertTrue("PW to Obf",  f1.equals(f2));
        assertTrue("Obf to PW",  f2.equals(f1));
        assertTrue("Obf to Obf", f2.equals(f2));
        
        assertTrue("PW to Str",  f1.check("Foo"));
        assertTrue("Obf to Str", f2.check("Foo"));
        
        assertTrue("PW to PW",   !f1.equals(b1));
        assertTrue("PW to Obf",  !f1.equals(b2));
        assertTrue("Obf to PW",  !f2.equals(b1));
        assertTrue("Obf to Obf", !f2.equals(b2));
        
        assertTrue("PW to Str",  !f1.check("Bar"));
        assertTrue("Obf to Str", !f2.check("Bar"));
    }

    
    /* ------------------------------------------------------------ */
    public void testCredential()
    {
        Credential[] creds =
            {
                    new Password("Foo"),
                    Credential.getCredential("Foo"),
                    Credential.getCredential(Credential.Crypt.crypt("user","Foo")),
                    Credential.getCredential(Credential.MD5.digest("Foo"))
            };

        assertTrue("c[0].check(c[0])", creds[0].check(creds[0]));
        assertTrue("c[0].check(c[1])", creds[0].check(creds[1]));
        assertTrue("c[0].check(c[2])", creds[0].check(creds[2]));
        assertTrue("c[0].check(c[3])", creds[0].check(creds[3]));

        assertTrue("c[1].check(c[0])", creds[1].check(creds[0]));
        assertTrue("c[1].check(c[1])", creds[1].check(creds[1]));
        assertTrue("c[1].check(c[2])", creds[1].check(creds[2]));
        assertTrue("c[1].check(c[3])", creds[1].check(creds[3]));

        assertTrue("c[2].check(c[0])", creds[2].check(creds[0]));
        assertTrue("c[2].check(c[1])", creds[2].check(creds[1]));
        assertTrue("c[2].check(c[2])",!creds[2].check(creds[2]));
        assertTrue("c[2].check(c[3])",!creds[2].check(creds[3]));

        assertTrue("c[3].check(c[0])", creds[3].check(creds[0]));
        assertTrue("c[3].check(c[1])", creds[3].check(creds[1]));
        assertTrue("c[3].check(c[2])",!creds[3].check(creds[2]));
        assertTrue("c[3].check(c[3])",!creds[3].check(creds[3]));
       
    }

}
