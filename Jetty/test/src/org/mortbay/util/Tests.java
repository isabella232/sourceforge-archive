// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.JarURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.Permission;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;
import java.util.jar.JarFile;

import junit.framework.*;


/* ------------------------------------------------------------ */
/** Util meta Tests.
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class Tests extends junit.framework.TestCase
{
    public final static String __CRLF = "\015\012";
    public static String __userDir =
        System.getProperty("user.dir",".");
    public static URL __userURL=null;
    private static String __relDir="";

    public Tests(String name) 
    {
      super(name);
    }
    
    public static junit.framework.Test suite() {
      return new TestSuite(Tests.class);
    }

    /* ------------------------------------------------------------ */
    /** main.
     */
    public static void main(String[] args)
    {
      junit.textui.TestRunner.run(suite());
    }    
    
    static
    {
        try{
            File file = new File(__userDir);
            __userURL=file.toURL();
            if (!__userURL.toString().endsWith("/Util/"))
            {
                __userURL=new URL(__userURL.toString()+
                                  "src/org/mortbay/util/");
                FilePermission perm = (FilePermission)
                    __userURL.openConnection().getPermission();
                __userDir=new File(perm.getName()).getCanonicalPath();
                __relDir="src/org/mortbay/util/".replace('/',
                                                         File.separatorChar);
            }                
        }
        catch(Exception e)
        {
            Code.fail(e);
        }
    }

    /*-------------------------------------------------------------------*/
    /** Check that string contains a substring.
     *  @return Index of substring
     */
    private int checkContains(String check, String string, String subString)
    {
        return realCheckContains(check, string,0,subString);
    }
    
    /*-------------------------------------------------------------------*/
    /** Check that string contains a substring.
     *  @return Index of substring
     */
    private int checkContains(String check,
                              String string,
                              int offset,
                              String subString)
    {
        return realCheckContains(check, string,offset,subString);
    }
    
    /*-------------------------------------------------------------------*/
    /** Check that string contains a substring.
     *  @return Index of substring
     */
    private int realCheckContains(String check, 
                                  String string,
                                  int offset,
                                  String subString)
    {
        int index=-1;
        if ((string==null && subString==null)
            || (string!=null && (subString==null ||
                                 (index=string.indexOf(subString,offset))>=0)))
        {
          // do nothing
        }
        else
        {
            fail('"' + subString + "\" not contained in \"" +
                             string.substring(offset) + '"');
        }
        return index;
    }


    

    /* ------------------------------------------------------------ */
    public void testDateCache() throws Exception
    {
        //@WAS: Test t = new Test("org.mortbay.util.DateCache");
        //                            012345678901234567890123456789
        DateCache dc = new DateCache("EEE, dd MMM yyyy HH:mm:ss zzz ZZZ",
                                     Locale.US);
            dc.setTimeZone(TimeZone.getTimeZone("GMT"));
            String last=dc.format(System.currentTimeMillis());
            boolean change=false;
            for (int i=0;i<15;i++)
            {
                Thread.sleep(100);
                String date=dc.format(System.currentTimeMillis());
                assertEquals( "Same Date", 
                              last.substring(0,17),
                              date.substring(0,17));

                if (last.substring(17).equals(date.substring(17)))
                    change=true;
                else
                {
                    int lh=Integer.parseInt(last.substring(17,19));
                    int dh=Integer.parseInt(date.substring(17,19));
                    int lm=Integer.parseInt(last.substring(20,22));
                    int dm=Integer.parseInt(date.substring(20,22));
                    int ls=Integer.parseInt(last.substring(23,25));
                    int ds=Integer.parseInt(date.substring(23,25));

                    // This won't work at midnight!
                    assertTrue(  "Time changed",
                            ds==ls+1 ||
                            ds==0 && dm==lm+1 ||
                            ds==0 && dm==0 && dh==lh+1);
                }
                last=date;
            }
            assertTrue("time changed", change);
    }

    /* ------------------------------------------------------------ */
    private void testFrameChecker(Frame f, String desc,
                                  String method, int depth,
                                  String thread, String file)
    {
        checkContains(desc+": depth", f._method, method);
        assertEquals( desc+": depth", f._depth, depth);
        assertEquals( desc+": depth", f._thread, thread);
        checkContains(desc+": depth", f._file, file);
    }
    
    /* ------------------------------------------------------------ */
    public void testFrame()
    {
        Frame f = new Frame();
        testFrameChecker(f, "default constructor",
                         "org.mortbay.util.TestHarness.testFrame",
                         2, "main", "TestHarness.java");
        f = f.getParent();
        testFrameChecker(f, "getParent",
                         "org.mortbay.util.TestHarness.main",
                         1, "main", "TestHarness.java");
        f = f.getParent();
        assertEquals("getParent() off top of stack", f, null);
        f = new Frame(1);
        testFrameChecker(f, "new Frame(1)",
                         "org.mortbay.util.TestHarness.main",
                         1, "main", "TestHarness.java");
        f = new Frame(1, true);
        testFrameChecker(f, "partial",
                         "unknownMethod", 0, "unknownThread", "UnknownFile");
        f.complete();
        testFrameChecker(f, "new Frame(1)",
                         "org.mortbay.util.TestHarness.main",
                         1, "main", "TestHarness.java");
    }



    /* ------------------------------------------------------------ */
    public void testIO() throws InterruptedException
    {
        // Only a little test
        ByteArrayInputStream in = new ByteArrayInputStream
            ("The quick brown fox jumped over the lazy dog".getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        IO.copyThread(in,out);
        Thread.sleep(500);

        assertEquals( "copyThread",
                      out.toString(),
                      "The quick brown fox jumped over the lazy dog");
    }

    /* ------------------------------------------------------------ */
    public static void testB64()
    {
	    // Perform basic reversibility tests
       assertEquals("decode(encode())",       B64Code.decode(B64Code.encode("")),"");
       assertEquals("decode(encode(a))",      B64Code.decode(B64Code.encode("a")),"a");
       assertEquals("decode(encode(ab))",     B64Code.decode(B64Code.encode("ab")),"ab");
       assertEquals("decode(encode(abc))",    B64Code.decode(B64Code.encode("abc")),"abc");
       assertEquals("decode(encode(abcd))",   B64Code.decode(B64Code.encode("abcd")),"abcd");
       assertEquals("decode(encode(^@))",     B64Code.decode(B64Code.encode("\000")),"\000");
       assertEquals("decode(encode(a^@))",    B64Code.decode(B64Code.encode("a\000")),"a\000");
       assertEquals("decode(encode(ab^@))",   B64Code.decode(B64Code.encode("ab\000")),"ab\000");
       assertEquals("decode(encode(abc^@))",  B64Code.decode(B64Code.encode("abc\000")),"abc\000");
       assertEquals("decode(encode(abcd^@))", B64Code.decode(B64Code.encode("abcd\000")),"abcd\000");

	    // Test the reversibility of the full range of 8 bit values
	    byte[] allValues= new byte[256];
	    for (int i=0; i<256; i++)
         allValues[i] = (byte) i;
	    String input = new String(allValues);
            String output=B64Code.decode(B64Code.encode(input));

            for (int i=0;i<256;i++)
                if (input.charAt(i)!=output.charAt(i))
                    System.err.println("DIFF at "+i+" "+
                                       ((int)input.charAt(i))+
                                       "!="+
                                       ((int)output.charAt(i)));
	    assertEquals( "decode(encode(ALL_128_ASCII_VALUES))", output,input);

	    // Encoder compatibility tests
	    assertEquals("YWJj","encode(abc)",         B64Code.encode("abc"));
	    assertEquals("YWJjZA==","encode(abc)",     B64Code.encode("abcd"));
	    assertEquals("YWJjZGU=","encode(abc)",     B64Code.encode("abcde"));
	    assertEquals("YWJjZGVm","encode(abc)",     B64Code.encode("abcdef"));
	    assertEquals("YWJjZGVmZw==","encode(abc)", B64Code.encode("abcdefg"));
    }
    
    
    
    
}
