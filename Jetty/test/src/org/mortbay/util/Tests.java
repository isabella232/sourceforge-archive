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
import java.io.UnsupportedEncodingException;
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
                 (string==null?"null":string.substring(offset))+ '"');
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


            // Test string is cached
            dc = new DateCache();
            String s1=dc.format(System.currentTimeMillis());
            dc.format(1);
            String s2=dc.format(System.currentTimeMillis());
            dc.format(System.currentTimeMillis()+10*60*60);
            String s3=dc.format(System.currentTimeMillis());
            assertTrue(s1==s2 || s2==s3);
    }

    /* ------------------------------------------------------------ */
    private void testFrameChecker(Frame f, String desc,
                                  String method, int depth,
                                  String thread, String file)
    {
        checkContains(desc+": method", f.getStack(),  method);
        assertEquals( desc+": depth",  depth,     f.getDepth());
        assertEquals( desc+": thread", thread,    f.getThread());
        if (file!=null)
            checkContains(desc+": file",   f.getFile(),   file);
    }
    
    /* ------------------------------------------------------------ */
    public void testFrame()
    {
        callFrame();
    }
    
    /* ------------------------------------------------------------ */
    public void callFrame()
    {
        Frame f = new Frame();
        int depth = f.getDepth();
        testFrameChecker(f, "method",
                         "org.mortbay.util.Tests.callFrame",
                         depth, "main", "Tests.java");

        f = f.getParent();
        testFrameChecker(f, "getParent",
                         "org.mortbay.util.Tests.testFrame",
                         depth-1, "main", "Tests.java");

        f = new Frame(0);
        testFrameChecker(f, "new Frame(0)",
                         "org.mortbay.util.Tests.callFrame",
                         depth, "main", "Tests.java");

        f = new Frame(1);
        testFrameChecker(f, "new Frame(1)",
                         "org.mortbay.util.Tests.testFrame",
                         depth-1, "main", "Tests.java");

        f = new Frame(2);
        testFrameChecker(f, "new Frame(2)",
                         "java.lang.reflect.Method.invoke",
                         depth-2, "main", null);

        f = new Frame(1, true);
        testFrameChecker(f, "partial",
                         "callFrame", 0, "unknownThread", null);

        f.complete();
        testFrameChecker(f, "complete",
                         "org.mortbay.util.Tests.testFrame",
                         depth-1, "main", "Tests.java");
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
        throws UnsupportedEncodingException
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

	    // Encoder compatibility tests
	    assertEquals("encode(abc)",     "YWJj",         B64Code.encode("abc"));
	    assertEquals("encode(abcd)",    "YWJjZA==",     B64Code.encode("abcd"));
	    assertEquals("encode(abcde)",   "YWJjZGU=",     B64Code.encode("abcde"));
	    assertEquals("encode(abcdef)",  "YWJjZGVm",     B64Code.encode("abcdef"));
	    assertEquals("encode(abcdefg)", "YWJjZGVmZw==", B64Code.encode("abcdefg"));

       // Test the reversibility of the full range of 8 bit values
	    byte[] allValues= new byte[256];
	    for (int i=0; i<256; i++)
         allValues[i] = (byte) i;
	    String input = new String(allValues, StringUtil.__ISO_8859_1);
            String output=B64Code.decode(B64Code.encode(input));

            for (int i=0;i<256;i++)
              assertEquals("DIFF at "+i, (int)input.charAt(i), (int)output.charAt(i));
	    assertEquals( "decode(encode(ALL_128_ASCII_VALUES))", output,input);

    }
    
    
    /* ------------------------------------------------------------ */
    public void testPassword()
    {
        Password f1 = new Password("password","Foo");
        Password f2 = new Password("password",
                                   Password.obfuscate("Foo"));
        Password f3 = new Password("password",
                                   Password.checksum("Foo"));
        
        Password b1 = new Password("password","Bar");
        Password b2 = new Password("password",
                                   Password.obfuscate("Bar"));
        Password b3 = new Password("password",
                                   Password.checksum("Bar"));

        assertTrue("PW to PW",   f1.equals(f1));
        assertTrue("PW to Obf",  f1.equals(f2));
        assertTrue("PW to CS",   !f1.equals(f3));
        assertTrue("Obf to PW",  f2.equals(f1));
        assertTrue("Obf to Obf", f2.equals(f2));
        assertTrue("Obf to CS",  !f2.equals(f3));
        assertTrue("CS to PW",   !f3.equals(f1));
        assertTrue("CS to Obf",  !f3.equals(f2));
        assertTrue("CS to CS",   f3.equals(f3));
        
        assertTrue("PW to Str",  f1.check("Foo"));
        assertTrue("Obf to Str", f2.check("Foo"));
        assertTrue("CS to Str",  f3.check("Foo"));
        
        assertTrue("PW to PW",   !f1.equals(b1));
        assertTrue("PW to Obf",  !f1.equals(b2));
        assertTrue("PW to CS",   !f1.equals(b3));
        assertTrue("Obf to PW",  !f2.equals(b1));
        assertTrue("Obf to Obf", !f2.equals(b2));
        assertTrue("Obf to CS",  !f2.equals(b3));
        assertTrue("CS to PW",   !f3.equals(b1));
        assertTrue("CS to Obf",  !f3.equals(b2));
        assertTrue("CS to CS",   !f3.equals(b3));
        
        assertTrue("PW to Str",  !f1.check("Bar"));
        assertTrue("Obf to Str", !f2.check("Bar"));
        assertTrue("CS to Str",  !f3.check("Bar"));
    }

    /* ------------------------------------------------------------ */
    public void testURI()
    {
        URI uri;

        // No host
        uri = new URI("/");
        assertEquals("root /", uri.getPath(),"/");

        uri = new URI("/Test/URI");
        assertEquals("no params", uri.toString(),"/Test/URI");

        uri = new URI("/Test/URI?");
        assertEquals("no params", uri.toString(),"/Test/URI?");
        uri.getParameters();
        assertEquals("no params", uri.toString(),"/Test/URI");
        
        uri = new URI("/Test/URI?a=1");
        assertEquals("one param", uri.toString(),"/Test/URI?a=1");
    
        uri = new URI("/Test/URI");
        uri.put("b","2 !");
        assertEquals("add param", uri.toString(),"/Test/URI?b=2+%21");

        // Host but no port
        uri = new URI("http://host");
        assertEquals("root host", uri.getPath(),"/");
        assertEquals("root host", uri.toString(),"http://host/");
        
        uri = new URI("http://host/");
        assertEquals("root host/", uri.getPath(),"/");
        
        uri = new URI("http://host/Test/URI");
        assertEquals("no params", uri.toString(),"http://host/Test/URI");

        uri = new URI("http://host/Test/URI?");
        assertEquals("no params", uri.toString(),"http://host/Test/URI?");
        uri.getParameters();
        assertEquals("no params", uri.toString(),"http://host/Test/URI");
        
        uri = new URI("http://host/Test/URI?a=1");
        assertEquals("one param", uri.toString(),"http://host/Test/URI?a=1");
    
        uri = new URI("http://host/Test/URI");
        uri.put("b","2 !");
        assertEquals("add param", uri.toString(),"http://host/Test/URI?b=2+%21");
    
        // Host and port and path
        uri = new URI("http://host:8080");
        assertEquals("root", uri.getPath(),"/");
        
        uri = new URI("http://host:8080/");
        assertEquals("root", uri.getPath(),"/");
        
        uri = new URI("http://host:8080/xxx");
        assertEquals("path", uri.getPath(),"/xxx");

        String anez=UrlEncoded.decodeString("A%F1ez");
        uri = new URI("http://host:8080/"+anez);
        assertEquals("root", uri.getPath(),"/"+anez);            
        
        uri = new URI("http://host:8080/Test/URI");
        assertEquals("no params", uri.toString(),"http://host:8080/Test/URI");

        uri = new URI("http://host:8080/Test/URI?");
        assertEquals("no params", uri.toString(),"http://host:8080/Test/URI?");
        uri.getParameters();
        assertEquals("no params", uri.toString(),"http://host:8080/Test/URI");
        
        uri = new URI("http://host:8080/Test/URI?a=1");
        assertEquals("one param", uri.toString(),"http://host:8080/Test/URI?a=1");
    
        uri = new URI("http://host:8080/Test/URI");
        uri.put("b","2 !");
        assertEquals("add param", uri.toString(),"http://host:8080/Test/URI?b=2+%21");
    
        assertEquals("protocol", uri.getScheme(),"http");
        assertEquals("host", uri.getHost(),"host");
        assertEquals("port", uri.getPort(),8080);

        uri.setScheme("ftp");
        uri.setHost("fff");
        uri.setPort(23);
        assertEquals("add param", uri.toString(),"ftp://fff:23/Test/URI?b=2+%21");
        
    
        uri = new URI("/Test/URI?c=1&d=2");
        uri.put("e","3");
        String s = uri.toString();
        assertTrue("merge params path", s.startsWith("/Test/URI?"));
        assertTrue("merge params c1", s.indexOf("c=1")>0);
        assertTrue("merge params d2", s.indexOf("d=2")>0);
        assertTrue("merge params e3", s.indexOf("e=3")>0);

        uri = new URI("/Test/URI?a=");
        assertEquals("null param", uri.toString(),"/Test/URI?a=");
        uri.getParameters();
        assertEquals("null param", uri.toString(),"/Test/URI?a");
        
        uri = new URI("/Test/Nasty%26%3F%20URI?c=%26&d=+%3F");
        assertEquals("nasty", uri.getPath(),"/Test/Nasty&? URI");
        uri.setPath("/test/nasty&? URI");
        uri.getParameters();
        assertTrue( "nasty",
                    uri.toString().equals("/test/nasty&%3F%20URI?c=%26&d=+%3F")||
                    uri.toString().equals("/test/nasty&%3F%20URI?d=+%3F&c=%26")
                    );
        uri=(URI)uri.clone();
        assertTrue("clone",
                   uri.toString().equals("/test/nasty&%3F%20URI?c=%26&d=+%3F")||
                   uri.toString().equals("/test/nasty&%3F%20URI?d=+%3F&c=%26")
                   );

        assertEquals("null+null", URI.addPaths(null,null),null);
        assertEquals("null+", URI.addPaths(null,""),null);
        assertEquals("null+bbb", URI.addPaths(null,"bbb"),"bbb");
        assertEquals("null+/", URI.addPaths(null,"/"),"/");
        assertEquals("null+/bbb", URI.addPaths(null,"/bbb"),"/bbb");
        
        assertEquals("+null", URI.addPaths("",null),"");
        assertEquals("+", URI.addPaths("",""),"");
        assertEquals("+bbb", URI.addPaths("","bbb"),"bbb");
        assertEquals("+/", URI.addPaths("","/"),"/");
        assertEquals("+/bbb", URI.addPaths("","/bbb"),"/bbb");
        
        assertEquals("aaa+null", URI.addPaths("aaa",null),"aaa");
        assertEquals("aaa+", URI.addPaths("aaa",""),"aaa");
        assertEquals("aaa+bbb", URI.addPaths("aaa","bbb"),"aaa/bbb");
        assertEquals("aaa+/", URI.addPaths("aaa","/"),"aaa/");
        assertEquals("aaa+/bbb", URI.addPaths("aaa","/bbb"),"aaa/bbb");
        
        assertEquals("/+null", URI.addPaths("/",null),"/");
        assertEquals("/+", URI.addPaths("/",""),"/");
        assertEquals("/+bbb", URI.addPaths("/","bbb"),"/bbb");
        assertEquals("/+/", URI.addPaths("/","/"),"/");
        assertEquals("/+/bbb", URI.addPaths("/","/bbb"),"/bbb");
        
        assertEquals("aaa/+null", URI.addPaths("aaa/",null),"aaa/");
        assertEquals("aaa/+", URI.addPaths("aaa/",""),"aaa/");
        assertEquals("aaa/+bbb", URI.addPaths("aaa/","bbb"),"aaa/bbb");
        assertEquals("aaa/+/", URI.addPaths("aaa/","/"),"aaa/");
        assertEquals("aaa/+/bbb", URI.addPaths("aaa/","/bbb"),"aaa/bbb");
        
        assertEquals(";JS+null", URI.addPaths(";JS",null),";JS");
        assertEquals(";JS+", URI.addPaths(";JS",""),";JS");
        assertEquals(";JS+bbb", URI.addPaths(";JS","bbb"),"bbb;JS");
        assertEquals(";JS+/", URI.addPaths(";JS","/"),"/;JS");
        assertEquals(";JS+/bbb", URI.addPaths(";JS","/bbb"),"/bbb;JS");
        
        assertEquals("aaa;JS+null", URI.addPaths("aaa;JS",null),"aaa;JS");
        assertEquals("aaa;JS+", URI.addPaths("aaa;JS",""),"aaa;JS");
        assertEquals("aaa;JS+bbb", URI.addPaths("aaa;JS","bbb"),"aaa/bbb;JS");
        assertEquals("aaa;JS+/", URI.addPaths("aaa;JS","/"),"aaa/;JS");
        assertEquals("aaa;JS+/bbb", URI.addPaths("aaa;JS","/bbb"),"aaa/bbb;JS");
        
        assertEquals("aaa;JS+null", URI.addPaths("aaa/;JS",null),"aaa/;JS");
        assertEquals("aaa;JS+", URI.addPaths("aaa/;JS",""),"aaa/;JS");
        assertEquals("aaa;JS+bbb", URI.addPaths("aaa/;JS","bbb"),"aaa/bbb;JS");
        assertEquals("aaa;JS+/", URI.addPaths("aaa/;JS","/"),"aaa/;JS");
        assertEquals("aaa;JS+/bbb", URI.addPaths("aaa/;JS","/bbb"),"aaa/bbb;JS");
        
        assertEquals("?A=1+null", URI.addPaths("?A=1",null),"?A=1");
        assertEquals("?A=1+", URI.addPaths("?A=1",""),"?A=1");
        assertEquals("?A=1+bbb", URI.addPaths("?A=1","bbb"),"bbb?A=1");
        assertEquals("?A=1+/", URI.addPaths("?A=1","/"),"/?A=1");
        assertEquals("?A=1+/bbb", URI.addPaths("?A=1","/bbb"),"/bbb?A=1");
        
        assertEquals("aaa?A=1+null", URI.addPaths("aaa?A=1",null),"aaa?A=1");
        assertEquals("aaa?A=1+", URI.addPaths("aaa?A=1",""),"aaa?A=1");
        assertEquals("aaa?A=1+bbb", URI.addPaths("aaa?A=1","bbb"),"aaa/bbb?A=1");
        assertEquals("aaa?A=1+/", URI.addPaths("aaa?A=1","/"),"aaa/?A=1");
        assertEquals("aaa?A=1+/bbb", URI.addPaths("aaa?A=1","/bbb"),"aaa/bbb?A=1");
        
        assertEquals("aaa?A=1+null", URI.addPaths("aaa/?A=1",null),"aaa/?A=1");
        assertEquals("aaa?A=1+", URI.addPaths("aaa/?A=1",""),"aaa/?A=1");
        assertEquals("aaa?A=1+bbb", URI.addPaths("aaa/?A=1","bbb"),"aaa/bbb?A=1");
        assertEquals("aaa?A=1+/", URI.addPaths("aaa/?A=1","/"),"aaa/?A=1");
        assertEquals("aaa?A=1+/bbb", URI.addPaths("aaa/?A=1","/bbb"),"aaa/bbb?A=1");
        
        assertEquals(";JS?A=1+null", URI.addPaths(";JS?A=1",null),";JS?A=1");
        assertEquals(";JS?A=1+", URI.addPaths(";JS?A=1",""),";JS?A=1");
        assertEquals(";JS?A=1+bbb", URI.addPaths(";JS?A=1","bbb"),"bbb;JS?A=1");
        assertEquals(";JS?A=1+/", URI.addPaths(";JS?A=1","/"),"/;JS?A=1");
        assertEquals(";JS?A=1+/bbb", URI.addPaths(";JS?A=1","/bbb"),"/bbb;JS?A=1");
        
        assertEquals("aaa;JS?A=1+null", URI.addPaths("aaa;JS?A=1",null),"aaa;JS?A=1");
        assertEquals("aaa;JS?A=1+", URI.addPaths("aaa;JS?A=1",""),"aaa;JS?A=1");
        assertEquals("aaa;JS?A=1+bbb", URI.addPaths("aaa;JS?A=1","bbb"),"aaa/bbb;JS?A=1");
        assertEquals("aaa;JS?A=1+/", URI.addPaths("aaa;JS?A=1","/"),"aaa/;JS?A=1");
        assertEquals("aaa;JS?A=1+/bbb", URI.addPaths("aaa;JS?A=1","/bbb"),"aaa/bbb;JS?A=1");
        
        assertEquals("aaa;JS?A=1+null", URI.addPaths("aaa/;JS?A=1",null),"aaa/;JS?A=1");
        assertEquals("aaa;JS?A=1+", URI.addPaths("aaa/;JS?A=1",""),"aaa/;JS?A=1");
        assertEquals("aaa;JS?A=1+bbb", URI.addPaths("aaa/;JS?A=1","bbb"),"aaa/bbb;JS?A=1");
        assertEquals("aaa;JS?A=1+/", URI.addPaths("aaa/;JS?A=1","/"),"aaa/;JS?A=1");
        assertEquals("aaa;JS?A=1+/bbb", URI.addPaths("aaa/;JS?A=1","/bbb"),"aaa/bbb;JS?A=1");

        assertEquals("parent /aaa/bbb/", URI.parentPath("/aaa/bbb/"),"/aaa/");
        assertEquals("parent /aaa/bbb", URI.parentPath("/aaa/bbb"),"/aaa/");
        assertEquals("parent /aaa/", URI.parentPath("/aaa/"),"/");
        assertEquals("parent /aaa", URI.parentPath("/aaa"),"/");
        assertEquals("parent /", URI.parentPath("/"),null);
        assertEquals("parent null", URI.parentPath(null),null);

        String[][] canonical = 
        {
            {"/aaa/bbb/","/aaa/bbb/"},
            {"/aaa//bbb/","/aaa/bbb/"},
            {"/aaa///bbb/","/aaa/bbb/"},
            {"/aaa/./bbb/","/aaa/bbb/"},
            {"/aaa/../bbb/","/bbb/"},
            {"/aaa/./../bbb/","/bbb/"},
            {"/aaa/bbb/ccc/../../ddd/","/aaa/ddd/"},
            {"./bbb/","bbb/"},
            {"./aaa/../bbb/","bbb/"},
            {"./",""},
            {".//",""},
            {".///",""},
            {"/.","/"},
            {"//.","/"},
            {"///.","/"},
            {"/","/"},
            {"aaa/bbb","aaa/bbb"},
            {"aaa/","aaa/"},
            {"aaa","aaa"},
            {"/aaa/bbb","/aaa/bbb"},
            {"/aaa//bbb","/aaa/bbb"},
            {"/aaa/./bbb","/aaa/bbb"},
            {"/aaa/../bbb","/bbb"},
            {"/aaa/./../bbb","/bbb"},
            {"./bbb","bbb"},
            {"./aaa/../bbb","bbb"},
            {"aaa/bbb/..","aaa/"},
            {"aaa/bbb/../","aaa/"},
            {"./",""},
            {".",""},
            {"",""},
            {"..",null},
            {"./..",null},
            {"aaa/../..",null},
            {"/foo/bar/../../..",null},
            {"/../foo",null},
            {"/foo/.","/foo/"},
            {"a","a"},
            {"a/","a/"},
            {"a/.","a/"},
            {"a/..",""},
            {"a/../..",null},
        };

        for (int t=0;t<canonical.length;t++)
            assertEquals( "canonical "+canonical[t][0],
                          URI.canonicalPath(canonical[t][0]),
                          canonical[t][1]
                          );
        
    }


    /* -------------------------------------------------------------- */
    public void testUrlEncoded()
    {
          
        UrlEncoded code = new UrlEncoded();
        assertEquals("Empty", code.size(),0);

        code.clear();
        code.decode("Name1=Value1");
        assertEquals("simple param size", code.size(),1);
        assertEquals("simple encode", code.encode(),"Name1=Value1");
        assertEquals("simple get", code.getString("Name1"),"Value1");
        
        code.clear();
        code.decode("Name2=");
        assertEquals("dangling param size", code.size(),1);
        assertEquals("dangling encode", code.encode(),"Name2");
        assertEquals("dangling get", code.getString("Name2"),"");
    
        code.clear();
        code.decode("Name3");
        assertEquals("noValue param size", code.size(),1);
        assertEquals("noValue encode", code.encode(),"Name3");
        assertEquals("noValue get", code.getString("Name3"),"");
    
        code.clear();
        code.decode("Name4=Value+4%21");
        assertEquals("encoded param size", code.size(),1);
        assertEquals("encoded encode", code.encode(),"Name4=Value+4%21");
        assertEquals("encoded get", code.getString("Name4"),"Value 4!");
        
        code.clear();
        code.decode("Name4=Value+4%21%20%214");
        assertEquals("encoded param size", code.size(),1);
        assertEquals("encoded encode", code.encode(),"Name4=Value+4%21+%214");
        assertEquals("encoded get", code.getString("Name4"),"Value 4! !4");

        
        code.clear();
        code.decode("Name5=aaa&Name6=bbb");
        assertEquals("multi param size", code.size(),2);
        assertTrue("multi encode "+code.encode(),
                   code.encode().equals("Name5=aaa&Name6=bbb") ||
                   code.encode().equals("Name6=bbb&Name5=aaa")
                   );
        assertEquals("multi get", code.getString("Name5"),"aaa");
        assertEquals("multi get", code.getString("Name6"),"bbb");
    
        code.clear();
        code.decode("Name7=aaa&Name7=b%2Cb&Name7=ccc");
        assertEquals("multi encode",
                        code.encode(),
                         "Name7=aaa&Name7=b%2Cb&Name7=ccc"
                         );
        assertEquals("list get all", code.getString("Name7"),"aaa,b,b,ccc");
        assertEquals("list get", code.getValues("Name7").get(0),"aaa");
        assertEquals("list get", code.getValues("Name7").get(1),"b,b");
        assertEquals("list get", code.getValues("Name7").get(2),"ccc");

        code.clear();
        code.decode("Name8=xx%2C++yy++%2Czz");
        assertEquals("encoded param size", code.size(),1);
        assertEquals("encoded encode", code.encode(),"Name8=xx%2C++yy++%2Czz");
        assertEquals("encoded get", code.getString("Name8"),"xx,  yy  ,zz");
    }
}
