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


/* ------------------------------------------------------------ */
/** Util meta TestHarness.
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class TestHarness
{
    public final static String __CRLF = "\015\012";
    public static String __userDir =
        System.getProperty("user.dir",".");
    public static URL __userURL=null;
    private static String __relDir="";
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

    
    /* ------------------------------------------------------------ */
    static void testDateCache()
    {
        Test t = new Test("org.mortbay.util.DateCache");
        //                            012345678901234567890123456789
        DateCache dc = new DateCache("EEE, dd MMM yyyy HH:mm:ss zzz ZZZ",
                                     Locale.US);
        dc.setTimeZone(TimeZone.getTimeZone("GMT"));
        try
        {
            String last=dc.format(System.currentTimeMillis());
            boolean change=false;
            for (int i=0;i<15;i++)
            {
                Thread.sleep(100);
                String date=dc.format(System.currentTimeMillis());
                System.err.println(date);
                t.checkEquals(last.substring(0,17),
                              date.substring(0,17),"Same Date");

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
                    t.check(ds==ls+1 ||
                            ds==0 && dm==lm+1 ||
                            ds==0 && dm==0 && dh==lh+1,
                            "Time changed");
                }
                last=date;
            }
            t.check(change,"time changed");
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
            
        }
    }
    
    /* ------------------------------------------------------------ */
    /** 
     */
    static void testTest()
    {
        Test t1 = new Test("Test all pass");
        Test t2 = new Test(Test.SelfFailTest);
        t2.check(false,"THESE TESTS ARE EXPECTED TO FAIL");
        t1.check(true,"Boolean check that passes");
        t2.check(false,"Boolean check that fails");
        t1.checkEquals("Foo","Foo","Object comparison that passes");
        t2.checkEquals("Foo","Bar","Object comparison that fails");
        t1.checkEquals(1,1,"Long comparison that passes");
        t2.checkEquals(1,2,"Long comparison that fails");
        t1.checkEquals(1.1,1.1,"Double comparison that passes");
        t2.checkEquals(1.1,2.2,"Double comparison that fails");
        t1.checkEquals('a','a',"Char comparison that passes");
        t2.checkEquals('a','b',"Char comparison that fails");
        t1.checkContains("ABCD","BC","Contains check that passes");
        t2.checkContains("ABCD","CB","Contains check that fails");
    }
    
    /*-------------------------------------------------------------------*/
    static void testLog()
    {
        // XXX - this is not even a test harness - poor show!
        Log.instance();
        System.err.println("\n\nEXPECT TESTTAG: TEST Message");
        Log.message("TESTTAG","TEST Message",new Frame());
        System.err.println("\n\nEXPECT: Test event");
        Log.event("Test event");
        System.err.println("\n\nEXPECT: Test warning");
        Log.warning("Test warning");
    }

    /* ------------------------------------------------------------ */
    private static void testFrameChecker(Test t, Frame f, String desc,
                                         String method, int depth,
                                         String thread, String file)
    {
        t.checkContains(f._method, method, desc+": method");
        t.checkEquals(f._depth, depth, desc+": depth");
        t.checkEquals(f._thread, thread, desc+": thread");
        t.checkContains(f._file, file, desc+": file");
    }
    
    /* ------------------------------------------------------------ */
    static void testFrame()
    {
        Test t = new Test("org.mortbay.util.Frame");
        Frame f = new Frame();
        testFrameChecker(t, f, "default constructor",
                         "org.mortbay.util.TestHarness.testFrame",
                         2, "main", "TestHarness.java");
        f = f.getParent();
        testFrameChecker(t, f, "getParent",
                         "org.mortbay.util.TestHarness.main",
                         1, "main", "TestHarness.java");
        f = f.getParent();
        t.checkEquals(f, null, "getParent() off top of stack");
        f = new Frame(1);
        testFrameChecker(t, f, "new Frame(1)",
                         "org.mortbay.util.TestHarness.main",
                         1, "main", "TestHarness.java");
        f = new Frame(1, true);
        testFrameChecker(t, f, "partial",
                         "unknownMethod", 0, "unknownThread", "UnknownFile");
        f.complete();
        testFrameChecker(t, f, "new Frame(1)",
                         "org.mortbay.util.TestHarness.main",
                         1, "main", "TestHarness.java");
    }
    
    /*-------------------------------------------------------------------*/
    /** 
     */
    static void testCode()
    {
        // Also not a test harness
        Test t = new Test("org.mortbay.util.Code");
        Code code = Code.instance();

        System.err.println("RUNNING CODE TESTS. Failures expected and must be visually checked");
        
        code._debugOn=false;
        Code.debug("YOU SHOULD NOT SEE THIS");
        
        code._debugOn=true;
        System.err.println("\n\nEXPECT DEBUG: Test debug message");
        Code.debug("Test debug message");
        System.err.println("\n\nEXPECT DEBUG: Test debug with stack");
        Code.debug("Test debug with stack",new Throwable());
        System.err.println("\n\nEXPECT DEBUG: Test debug with various");
        Code.debug("Test debug with various",new Throwable(),"\n",code);
        
        code._debugPatterns = new java.util.Vector();
        code._debugPatterns.addElement("ZZZZZ");
        Code.debug("YOU SHOULD NOT SEE THIS");
        Code.debug("YOU SHOULD"," NOT SEE ","THIS");
        
        code._debugPatterns.addElement("TestHarness");
        System.err.println("\n\nEXPECT DEBUG: Test debug pattern");
        Code.debug("Test debug pattern");
        code._debugPatterns = null;

        System.err.println("\n\nEXPECT WARNING: Test warning");
        Code.warning("Test warning");
        
        Code.setDebug(false);
        Code.setDebugTriggers("FOO,BAR");
        Code.debug("YOU SHOULD NOT SEE THIS");
        Code.triggerOn("BLAH");
        Code.debug("YOU SHOULD NOT SEE THIS");
        System.err.println("\n\nEXPECT TRIGGER: ON FOO");
        Code.triggerOn("FOO");
        System.err.println("\n\nEXPECT DEBUG: triggered");
        Code.debug("triggered");
        System.err.println("\n\nEXPECT TRIGGER: ON BAR");
        Code.triggerOn("BAR");
        System.err.println("\n\nEXPECT DEBUG: triggered");
        Code.debug("triggered");
        Code.triggerOn("FOO");
        System.err.println("\n\nEXPECT TRIGGER: OFF FOO");
        Code.triggerOff("FOO");
        System.err.println("\n\nEXPECT DEBUG: triggered");
        Code.debug("triggered");
        System.err.println("\n\nEXPECT TRIGGER: OFF BAR");
        Code.triggerOff("BAR");
        Code.debug("YOU SHOULD NOT SEE THIS");
        Code.triggerOff("BLAH");
        Code.debug("YOU SHOULD NOT SEE THIS");
        
        Code.setDebug(false);
        
        try
        {
            System.err.println("\n\nEXPECT FAIL: Fail test");
            Code.fail("Fail test");
            t.check(false,"fail");
        }
        catch(CodeException e)
        {
            Code.debug(e);
            t.check(true,"fail");
        }
        
        try
        {
            Code.assert(true,"assert");
            Code.assertEquals("String","String","equals");
            Code.assertEquals(1,1,"equals");
            Code.assertContains("String","rin","contains");         
            
            System.err.println("\n\nEXPECT ASSERT: Assert fail");
            Code.assertEquals("foo","bar","assert fail");
            t.check(false,"Assert");
        }
        catch(CodeException e)
        {
            t.check(true,"Assert");
            System.err.println("\n\nEXPECT Warning: Assert with stack");
            Code.warning(e);
        }
    }
    
    /* ------------------------------------------------------------ */
    public static void testIO()
    {
        Test t = new Test("org.mortbay.util.IO");
        try{
            // Only a little test
            ByteArrayInputStream in = new ByteArrayInputStream
                ("The quick brown fox jumped over the lazy dog".getBytes());
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            IO.copyThread(in,out);
            Thread.sleep(500);

            t.checkEquals(out.toString(),
                          "The quick brown fox jumped over the lazy dog",
                          "copyThread");
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,"Exception: "+e);
        }
    }
    
    /* ------------------------------------------------------------ */
    public static void testB64()
    {
        Test t = new Test("org.mortbay.util.B64Code");
        try{
	    // Perform basic reversibility tests
            t.checkEquals(B64Code.decode(B64Code.encode("")),"","decode(encode())");
            t.checkEquals(B64Code.decode(B64Code.encode("a")),"a","decode(encode(a))");
            t.checkEquals(B64Code.decode(B64Code.encode("ab")),"ab","decode(encode(ab))");
            t.checkEquals(B64Code.decode(B64Code.encode("abc")),"abc","decode(encode(abc))");
            t.checkEquals(B64Code.decode(B64Code.encode("abcd")),"abcd","decode(encode(abcd))");
            t.checkEquals(B64Code.decode(B64Code.encode("\000")),"\000","decode(encode(^@))");
            t.checkEquals(B64Code.decode(B64Code.encode("a\000")),"a\000","decode(encode(a^@))");
            t.checkEquals(B64Code.decode(B64Code.encode("ab\000")),"ab\000","decode(encode(ab^@))");
            t.checkEquals(B64Code.decode(B64Code.encode("abc\000")),"abc\000","decode(encode(abc^@))");
            t.checkEquals(B64Code.decode(B64Code.encode("abcd\000")),"abcd\000","decode(encode(abcd^@))");

	    // Test the reversibility of the full range of 8 bit values
	    char[] allValues= new char[128];
	    for (int i=0; i<128; i++)
		allValues[i] = (char) i;
	    String input = new String(allValues);
            String output=B64Code.decode(B64Code.encode(input));
            for (int i=0;i<128;i++)
                if (input.charAt(i)!=output.charAt(i))
                    System.err.println("DIFF at "+i+" "+
                                       ((int)input.charAt(i))+
                                       "!="+
                                       ((int)output.charAt(i)));
	    t.checkEquals(output,input,
			  "decode(encode(ALL_128_ASCII_VALUES))");

	    // Encoder compatibility tests
	    t.checkEquals(B64Code.encode("abc"),"YWJj","encode(abc)");
	    t.checkEquals(B64Code.encode("abcd"),"YWJjZA==","encode(abc)");
	    t.checkEquals(B64Code.encode("abcde"),"YWJjZGU=","encode(abc)");
	    t.checkEquals(B64Code.encode("abcdef"),"YWJjZGVm","encode(abc)");
	    t.checkEquals(B64Code.encode("abcdefg"),"YWJjZGVmZw==","encode(abc)");
	}
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,"Exception: "+e);
        }
    }
    
    /* ------------------------------------------------------------ */
    public static void testPassword()
    {
        Test t = new Test("org.mortbay.util.Password");
        try{
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

            t.check(f1.equals(f1),"PW to PW");
            t.check(f1.equals(f2),"PW to Obf");
            t.check(!f1.equals(f3),"PW to CS");
            t.check(f2.equals(f1),"Obf to PW");
            t.check(f2.equals(f2),"Obf to Obf");
            t.check(!f2.equals(f3),"Obf to CS");
            t.check(!f3.equals(f1),"CS to PW");
            t.check(!f3.equals(f2),"CS to Obf");
            t.check(f3.equals(f3),"CS to CS");
            
            t.check(f1.check("Foo"),"PW to Str");
            t.check(f2.check("Foo"),"Obf to Str");
            t.check(f3.check("Foo"),"CS to Str");
            
            t.check(!f1.equals(b1),"PW to PW");
            t.check(!f1.equals(b2),"PW to Obf");
            t.check(!f1.equals(b3),"PW to CS");
            t.check(!f2.equals(b1),"Obf to PW");
            t.check(!f2.equals(b2),"Obf to Obf");
            t.check(!f2.equals(b3),"Obf to CS");
            t.check(!f3.equals(b1),"CS to PW");
            t.check(!f3.equals(b2),"CS to Obf");
            t.check(!f3.equals(b3),"CS to CS");
            
            t.check(!f1.check("Bar"),"PW to Str");
            t.check(!f2.check("Bar"),"Obf to Str");
            t.check(!f3.check("Bar"),"CS to Str");
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,"Exception: "+e);
        }
    }
    

    /* ------------------------------------------------------------ */
    public static void testBlockingQueue()
        throws Exception
    {
        System.err.print("Testing BlockingQueue.");
        System.err.flush();
        final Test t = new Test("org.mortbay.util.BlockingQueue");

        final BlockingQueue bq=new BlockingQueue(5);
        t.checkEquals(bq.size(),0,"empty");
        bq.put("A");
        t.checkEquals(bq.size(),1,"size");
        t.checkEquals(bq.get(),"A","A");
        t.checkEquals(bq.size(),0,"size");
        bq.put("B");
        bq.put("C");
        bq.put("D");
        t.checkEquals(bq.size(),3,"size");
        t.checkEquals(bq.get(),"B","B");
        t.checkEquals(bq.size(),2,"size");
        bq.put("E");
        t.checkEquals(bq.size(),3,"size");
        t.checkEquals(bq.get(),"C","C");
        t.checkEquals(bq.get(),"D","D");
        t.checkEquals(bq.get(),"E","E");

        new Thread(new Runnable()
                   {
                       public void run(){
                           try{
                               Thread.sleep(1000);
                               System.err.print(".");
                               System.err.flush();
                               bq.put("F");
                           }
                           catch(InterruptedException e){}
                       }
                   }
                   ).start();  
        
        t.checkEquals(bq.get(),"F","F");
        t.checkEquals(bq.get(100),null,"null");
        
        bq.put("G1");
        bq.put("G2");
        bq.put("G3");
        bq.put("G4");
        bq.put("G5");
        
        new Thread(new Runnable()
                   {
                       public void run(){
                           try{
                               Thread.sleep(500);
                               System.err.print(".");
                               System.err.flush();
                               t.checkEquals(bq.get(),"G1","G1");
                           }
                           catch(InterruptedException e){}
                       }
                   }
                   ).start();  
        try{
            bq.put("G6",100);
            t.check(false,"put timeout");
        }
        catch(InterruptedException e)
        {
            t.checkContains(e.toString(),"Timed out","put timeout");
        }
        
        bq.put("G6");
        t.checkEquals(bq.get(),"G2","G2");
        t.checkEquals(bq.get(),"G3","G3");
        t.checkEquals(bq.get(),"G4","G4");
        t.checkEquals(bq.get(),"G5","G5");
        t.checkEquals(bq.get(),"G6","G6");
        t.checkEquals(bq.get(100),null,"that's all folks");
        System.err.println();
    }
    
    /* -------------------------------------------------------------- */
    public static void testUrlEncoded()
    {
        Test test = new Test("org.mortbay.util.UrlEncoded");

        try{
                
            UrlEncoded code = new UrlEncoded();
            test.checkEquals(code.size(),0,"Empty");

            code.clear();
            code.decode("Name1=Value1");
            test.checkEquals(code.size(),1,"simple param size");
            test.checkEquals(code.encode(),"Name1=Value1","simple encode");
            test.checkEquals(code.getString("Name1"),"Value1","simple get");
            
            code.clear();
            code.decode("Name2=");
            test.checkEquals(code.size(),1,"dangling param size");
            test.checkEquals(code.encode(),"Name2","dangling encode");
            test.checkEquals(code.getString("Name2"),"","dangling get");
        
            code.clear();
            code.decode("Name3");
            test.checkEquals(code.size(),1,"noValue param size");
            test.checkEquals(code.encode(),"Name3","noValue encode");
            test.checkEquals(code.getString("Name3"),"","noValue get");
        
            code.clear();
            code.decode("Name4=Value+4%21");
            test.checkEquals(code.size(),1,"encoded param size");
            test.checkEquals(code.encode(),"Name4=Value+4%21","encoded encode");
            test.checkEquals(code.getString("Name4"),"Value 4!","encoded get");
            
            code.clear();
            code.decode("Name5=aaa&Name6=bbb");
            test.checkEquals(code.size(),2,"multi param size");
            test.check(code.encode().equals("Name5=aaa&Name6=bbb") ||
                       code.encode().equals("Name6=bbb&Name5=aaa"),
                       "multi encode");
            test.checkEquals(code.getString("Name5"),"aaa","multi get");
            test.checkEquals(code.getString("Name6"),"bbb","multi get");
        
            code.clear();
            code.decode("Name7=aaa&Name7=b%2Cb&Name7=ccc");
            test.checkEquals(code.encode(),
                             "Name7=aaa&Name7=b%2Cb&Name7=ccc",
                             "multi encode");
            test.checkEquals(code.getString("Name7"),"aaa,b,b,ccc","list get all");
            test.checkEquals(code.getValues("Name7").get(0),"aaa","list get");
            test.checkEquals(code.getValues("Name7").get(1),"b,b","list get");
            test.checkEquals(code.getValues("Name7").get(2),"ccc","list get");
        }
        catch(Exception e){
            Code.warning(e);
            test.check(false,e.toString());
        }
    }
    
    /* ------------------------------------------------------------ */
    public static void testURI()
    {
        Test test = new Test("org.mortbay.util.URI");
        try
        {
            URI uri;

            // No host
            uri = new URI("/");
            test.checkEquals(uri.getPath(),"/","root /");
    
            uri = new URI("/Test/URI");
            test.checkEquals(uri.toString(),"/Test/URI","no params");
    
            uri = new URI("/Test/URI?");
            test.checkEquals(uri.toString(),"/Test/URI?","no params");
            uri.getParameters();
            test.checkEquals(uri.toString(),"/Test/URI","no params");
            
            uri = new URI("/Test/URI?a=1");
            test.checkEquals(uri.toString(),"/Test/URI?a=1","one param");
        
            uri = new URI("/Test/URI");
            uri.put("b","2 !");
            test.checkEquals(uri.toString(),"/Test/URI?b=2+%21","add param");

            // Host but no port
            uri = new URI("http://host");
            test.checkEquals(uri.getPath(),"/","root host");
            test.checkEquals(uri.toString(),"http://host/","root host");
            
            uri = new URI("http://host/");
            test.checkEquals(uri.getPath(),"/","root host/");
            
            uri = new URI("http://host/Test/URI");
            test.checkEquals(uri.toString(),"http://host/Test/URI","no params");
    
            uri = new URI("http://host/Test/URI?");
            test.checkEquals(uri.toString(),"http://host/Test/URI?","no params");
            uri.getParameters();
            test.checkEquals(uri.toString(),"http://host/Test/URI","no params");
            
            uri = new URI("http://host/Test/URI?a=1");
            test.checkEquals(uri.toString(),"http://host/Test/URI?a=1","one param");
        
            uri = new URI("http://host/Test/URI");
            uri.put("b","2 !");
            test.checkEquals(uri.toString(),"http://host/Test/URI?b=2+%21","add param");
        
            // Host and port and path
            uri = new URI("http://host:8080");
            test.checkEquals(uri.getPath(),"/","root");
            
            uri = new URI("http://host:8080/");
            test.checkEquals(uri.getPath(),"/","root");
            
            uri = new URI("http://host:8080/xxx");
            test.checkEquals(uri.getPath(),"/xxx","path");

            String anez=UrlEncoded.decodeString("A%F1ez");
            uri = new URI("http://host:8080/"+anez);
            test.checkEquals(uri.getPath(),"/"+anez,"root");            
            
            uri = new URI("http://host:8080/Test/URI");
            test.checkEquals(uri.toString(),"http://host:8080/Test/URI","no params");
    
            uri = new URI("http://host:8080/Test/URI?");
            test.checkEquals(uri.toString(),"http://host:8080/Test/URI?","no params");
            uri.getParameters();
            test.checkEquals(uri.toString(),"http://host:8080/Test/URI","no params");
            
            uri = new URI("http://host:8080/Test/URI?a=1");
            test.checkEquals(uri.toString(),"http://host:8080/Test/URI?a=1","one param");
        
            uri = new URI("http://host:8080/Test/URI");
            uri.put("b","2 !");
            test.checkEquals(uri.toString(),"http://host:8080/Test/URI?b=2+%21","add param");
        
            test.checkEquals(uri.getScheme(),"http","protocol");
            test.checkEquals(uri.getHost(),"host","host");
            test.checkEquals(uri.getPort(),8080,"port");

            uri.setScheme("ftp");
            uri.setHost("fff");
            uri.setPort(23);
            test.checkEquals(uri.toString(),"ftp://fff:23/Test/URI?b=2+%21","add param");
            
        
            uri = new URI("/Test/URI?c=1&d=2");
            uri.put("e","3");
            String s = uri.toString();
            test.check(s.startsWith("/Test/URI?"),"merge params path");
            test.check(s.indexOf("c=1")>0,"merge params c1");
            test.check(s.indexOf("d=2")>0,"merge params d2");
            test.check(s.indexOf("e=3")>0,"merge params e3");

            uri = new URI("/Test/URI?a=");
            test.checkEquals(uri.toString(),"/Test/URI?a=","null param");
            uri.getParameters();
            test.checkEquals(uri.toString(),"/Test/URI?a","null param");
            uri.setEncodeNulls(true);
            test.checkEquals(uri.toString(),"/Test/URI?a=","null= param");
            
            uri = new URI("/Test/Nasty%26%3F%20URI?c=%26&d=+%3F");
            test.checkEquals(uri.getPath(),"/Test/Nasty&? URI","nasty");
            uri.setPath("/test/nasty&? URI");
            uri.getParameters();
            test.checkEquals(uri.toString(),
                             "/test/nasty&%3F%20URI?c=%26&d=+%3F","nasty");
            uri=(URI)uri.clone();
            test.checkEquals(uri.toString(),
                             "/test/nasty&%3F%20URI?c=%26&d=+%3F","clone");


            test.checkEquals(URI.addPaths(null,null),null,"null+null");
            test.checkEquals(URI.addPaths(null,""),null,"null+");
            test.checkEquals(URI.addPaths(null,"bbb"),"bbb","null+bbb");
            test.checkEquals(URI.addPaths(null,"/"),"/","null+/");
            test.checkEquals(URI.addPaths(null,"/bbb"),"/bbb","null+/bbb");
            
            test.checkEquals(URI.addPaths("",null),"","+null");
            test.checkEquals(URI.addPaths("",""),"","+");
            test.checkEquals(URI.addPaths("","bbb"),"bbb","+bbb");
            test.checkEquals(URI.addPaths("","/"),"/","+/");
            test.checkEquals(URI.addPaths("","/bbb"),"/bbb","+/bbb");
            
            test.checkEquals(URI.addPaths("aaa",null),"aaa","aaa+null");
            test.checkEquals(URI.addPaths("aaa",""),"aaa","aaa+");
            test.checkEquals(URI.addPaths("aaa","bbb"),"aaa/bbb","aaa+bbb");
            test.checkEquals(URI.addPaths("aaa","/"),"aaa/","aaa+/");
            test.checkEquals(URI.addPaths("aaa","/bbb"),"aaa/bbb","aaa+/bbb");
            
            test.checkEquals(URI.addPaths("/",null),"/","/+null");
            test.checkEquals(URI.addPaths("/",""),"/","/+");
            test.checkEquals(URI.addPaths("/","bbb"),"/bbb","/+bbb");
            test.checkEquals(URI.addPaths("/","/"),"/","/+/");
            test.checkEquals(URI.addPaths("/","/bbb"),"/bbb","/+/bbb");
            
            test.checkEquals(URI.addPaths("aaa/",null),"aaa/","aaa/+null");
            test.checkEquals(URI.addPaths("aaa/",""),"aaa/","aaa/+");
            test.checkEquals(URI.addPaths("aaa/","bbb"),"aaa/bbb","aaa/+bbb");
            test.checkEquals(URI.addPaths("aaa/","/"),"aaa/","aaa/+/");
            test.checkEquals(URI.addPaths("aaa/","/bbb"),"aaa/bbb","aaa/+/bbb");
            
            test.checkEquals(URI.addPaths(";JS",null),";JS",";JS+null");
            test.checkEquals(URI.addPaths(";JS",""),";JS",";JS+");
            test.checkEquals(URI.addPaths(";JS","bbb"),"bbb;JS",";JS+bbb");
            test.checkEquals(URI.addPaths(";JS","/"),"/;JS",";JS+/");
            test.checkEquals(URI.addPaths(";JS","/bbb"),"/bbb;JS",";JS+/bbb");
            
            test.checkEquals(URI.addPaths("aaa;JS",null),"aaa;JS","aaa;JS+null");
            test.checkEquals(URI.addPaths("aaa;JS",""),"aaa;JS","aaa;JS+");
            test.checkEquals(URI.addPaths("aaa;JS","bbb"),"aaa/bbb;JS","aaa;JS+bbb");
            test.checkEquals(URI.addPaths("aaa;JS","/"),"aaa/;JS","aaa;JS+/");
            test.checkEquals(URI.addPaths("aaa;JS","/bbb"),"aaa/bbb;JS","aaa;JS+/bbb");
            
            test.checkEquals(URI.addPaths("aaa/;JS",null),"aaa/;JS","aaa;JS+null");
            test.checkEquals(URI.addPaths("aaa/;JS",""),"aaa/;JS","aaa;JS+");
            test.checkEquals(URI.addPaths("aaa/;JS","bbb"),"aaa/bbb;JS","aaa;JS+bbb");
            test.checkEquals(URI.addPaths("aaa/;JS","/"),"aaa/;JS","aaa;JS+/");
            test.checkEquals(URI.addPaths("aaa/;JS","/bbb"),"aaa/bbb;JS","aaa;JS+/bbb");
            
            test.checkEquals(URI.addPaths("?A=1",null),"?A=1","?A=1+null");
            test.checkEquals(URI.addPaths("?A=1",""),"?A=1","?A=1+");
            test.checkEquals(URI.addPaths("?A=1","bbb"),"bbb?A=1","?A=1+bbb");
            test.checkEquals(URI.addPaths("?A=1","/"),"/?A=1","?A=1+/");
            test.checkEquals(URI.addPaths("?A=1","/bbb"),"/bbb?A=1","?A=1+/bbb");
            
            test.checkEquals(URI.addPaths("aaa?A=1",null),"aaa?A=1","aaa?A=1+null");
            test.checkEquals(URI.addPaths("aaa?A=1",""),"aaa?A=1","aaa?A=1+");
            test.checkEquals(URI.addPaths("aaa?A=1","bbb"),"aaa/bbb?A=1","aaa?A=1+bbb");
            test.checkEquals(URI.addPaths("aaa?A=1","/"),"aaa/?A=1","aaa?A=1+/");
            test.checkEquals(URI.addPaths("aaa?A=1","/bbb"),"aaa/bbb?A=1","aaa?A=1+/bbb");
            
            test.checkEquals(URI.addPaths("aaa/?A=1",null),"aaa/?A=1","aaa?A=1+null");
            test.checkEquals(URI.addPaths("aaa/?A=1",""),"aaa/?A=1","aaa?A=1+");
            test.checkEquals(URI.addPaths("aaa/?A=1","bbb"),"aaa/bbb?A=1","aaa?A=1+bbb");
            test.checkEquals(URI.addPaths("aaa/?A=1","/"),"aaa/?A=1","aaa?A=1+/");
            test.checkEquals(URI.addPaths("aaa/?A=1","/bbb"),"aaa/bbb?A=1","aaa?A=1+/bbb");
            
            test.checkEquals(URI.addPaths(";JS?A=1",null),";JS?A=1",";JS?A=1+null");
            test.checkEquals(URI.addPaths(";JS?A=1",""),";JS?A=1",";JS?A=1+");
            test.checkEquals(URI.addPaths(";JS?A=1","bbb"),"bbb;JS?A=1",";JS?A=1+bbb");
            test.checkEquals(URI.addPaths(";JS?A=1","/"),"/;JS?A=1",";JS?A=1+/");
            test.checkEquals(URI.addPaths(";JS?A=1","/bbb"),"/bbb;JS?A=1",";JS?A=1+/bbb");
            
            test.checkEquals(URI.addPaths("aaa;JS?A=1",null),"aaa;JS?A=1","aaa;JS?A=1+null");
            test.checkEquals(URI.addPaths("aaa;JS?A=1",""),"aaa;JS?A=1","aaa;JS?A=1+");
            test.checkEquals(URI.addPaths("aaa;JS?A=1","bbb"),"aaa/bbb;JS?A=1","aaa;JS?A=1+bbb");
            test.checkEquals(URI.addPaths("aaa;JS?A=1","/"),"aaa/;JS?A=1","aaa;JS?A=1+/");
            test.checkEquals(URI.addPaths("aaa;JS?A=1","/bbb"),"aaa/bbb;JS?A=1","aaa;JS?A=1+/bbb");
            
            test.checkEquals(URI.addPaths("aaa/;JS?A=1",null),"aaa/;JS?A=1","aaa;JS?A=1+null");
            test.checkEquals(URI.addPaths("aaa/;JS?A=1",""),"aaa/;JS?A=1","aaa;JS?A=1+");
            test.checkEquals(URI.addPaths("aaa/;JS?A=1","bbb"),"aaa/bbb;JS?A=1","aaa;JS?A=1+bbb");
            test.checkEquals(URI.addPaths("aaa/;JS?A=1","/"),"aaa/;JS?A=1","aaa;JS?A=1+/");
            test.checkEquals(URI.addPaths("aaa/;JS?A=1","/bbb"),"aaa/bbb;JS?A=1","aaa;JS?A=1+/bbb");

            test.checkEquals(URI.parentPath("/aaa/bbb/"),"/aaa/","parent /aaa/bbb/");
            test.checkEquals(URI.parentPath("/aaa/bbb"),"/aaa/","parent /aaa/bbb");
            test.checkEquals(URI.parentPath("/aaa/"),"/","parent /aaa/");
            test.checkEquals(URI.parentPath("/aaa"),"/","parent /aaa");
            test.checkEquals(URI.parentPath("/"),null,"parent /");
            test.checkEquals(URI.parentPath(null),null,"parent null");

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
                test.checkEquals(URI.canonicalPath(canonical[t][0]),
                                 canonical[t][1],
                                 "canonical "+canonical[t][0]);
            
        }
        catch(Exception e)
        {
            Code.warning(e);
            test.check(false,e.toString());
        }
    }
    
    /* ------------------------------------------------------------ */
    public static void testQuotedStringTokenizer()
    {
        Test test = new Test("org.mortbay.util.QuotedStringTokenizer");
        try
        {
            QuotedStringTokenizer tok;
            
            tok=new QuotedStringTokenizer
                ("aaa, bbb, 'ccc, \"ddd\", \\'eee\\''",", ");
            test.check(tok.hasMoreTokens(),"hasMoreTokens");
            test.check(tok.hasMoreTokens(),"hasMoreTokens");
            test.checkEquals(tok.nextToken(),"aaa","aaa");
            test.check(tok.hasMoreTokens(),"hasMoreTokens");
            test.checkEquals(tok.nextToken(),"bbb","bbb");
            test.check(tok.hasMoreTokens(),"hasMoreTokens");
            test.checkEquals(tok.nextToken(),"ccc, \"ddd\", 'eee'","quoted");
            test.check(!tok.hasMoreTokens(),"hasMoreTokens");
            test.check(!tok.hasMoreTokens(),"hasMoreTokens");
            
            tok=new QuotedStringTokenizer
                ("aaa, bbb, 'ccc, \"ddd\", \\'eee\\''",", ",false,true);
            test.checkEquals(tok.nextToken(),"aaa","aaa");
            test.checkEquals(tok.nextToken(),"bbb","bbb");
            test.checkEquals(tok.nextToken(),"'ccc, \"ddd\", \\'eee\\''","quoted");
            
            tok=new QuotedStringTokenizer
                ("aa,bb;\"cc\",,'dd',;'',',;','\\''",";,");
            test.checkEquals(tok.nextToken(),"aa","aa");
            test.checkEquals(tok.nextToken(),"bb","bb");
            test.checkEquals(tok.nextToken(),"cc","cc");
            test.checkEquals(tok.nextToken(),"dd","dd");
            test.checkEquals(tok.nextToken(),"","empty");
            test.checkEquals(tok.nextToken(),",;","delimiters");
            test.checkEquals(tok.nextToken(),"'","escaped");
            
            tok=new QuotedStringTokenizer
                ("xx,bb;\"cc\",,'dd',;'',',;','\\''",";,",true);
            test.checkEquals(tok.nextToken(),"xx","xx");
            test.checkEquals(tok.nextToken(),",",",");
            test.checkEquals(tok.nextToken(),"bb","bb");
            test.checkEquals(tok.nextToken(),";",";");
            test.checkEquals(tok.nextToken(),"cc","cc");
            test.checkEquals(tok.nextToken(),",",",");
            test.checkEquals(tok.nextToken(),",",",");
            test.checkEquals(tok.nextToken(),"dd","dd");
            test.checkEquals(tok.nextToken(),",",",");
            test.checkEquals(tok.nextToken(),";",";");
            test.checkEquals(tok.nextToken(),"","empty");
            test.checkEquals(tok.nextToken(),",",",");
            test.checkEquals(tok.nextToken(),",;","delimiters");
            test.checkEquals(tok.nextToken(),",",",");
            test.checkEquals(tok.nextToken(),"'","escaped");
            
            tok=new QuotedStringTokenizer
                ("aaa;bbb,ccc;ddd",";");
            test.checkEquals(tok.nextToken(),"aaa","aaa");
            test.check(tok.hasMoreTokens(),"hasMoreTokens");
            test.checkEquals(tok.nextToken(","),"bbb","bbb");
            test.checkEquals(tok.nextToken(),"ccc;ddd","ccc;ddd");
            
            test.checkEquals(tok.quote("aaa"," "),"aaa","no quote");
            test.checkEquals(tok.quote("a a"," "),"\"a a\"","quote");
            test.checkEquals(tok.quote("a'a"," "),"\"a'a\"","quote");
            test.checkEquals(tok.quote("a,a",","),"\"a,a\"","quote");
            test.checkEquals(tok.quote("a\\a",""),"\"a\\\\a\"","quote");
            
        }
        catch(Exception e)
        {
            Code.warning(e);
            test.check(false,e.toString());
        }
    }

    /* ------------------------------------------------------------ */
    /** 
     */
    static final void testLineInput()
    {
        Test test = new Test("org.mortbay.util.LineInput");
        try
        {
                
            String data=
                "abcd\015\012"+
                "E\012"+
                "\015"+
                "fghi";
            
            ByteArrayInputStream dataStream;
            LineInput in;
                
            dataStream=new ByteArrayInputStream(data.getBytes());
            in = new LineInput(dataStream);
            
            test.checkEquals(in.readLine(),"abcd","1 read first line");
            test.checkEquals(in.readLine(),"E","1 read line");
            test.checkEquals(in.readLine(),"","1 blank line");
            test.checkEquals(in.readLine(),"fghi","1 read last line");
            test.checkEquals(in.readLine(),null,"1 read EOF");
            test.checkEquals(in.readLine(),null,"1 read EOF again");

            int bs=7;
            dataStream=new ByteArrayInputStream(data.getBytes());
            in = new LineInput(dataStream,bs);
            test.checkEquals(in.readLine(),"abcd","1."+bs+" read first line");
            test.checkEquals(in.readLine(),"E","1."+bs+" read line");
            test.checkEquals(in.readLine(),"","1."+bs+" blank line");
            test.checkEquals(in.readLine(),"fghi","1."+bs+" read last line");
            test.checkEquals(in.readLine(),null,"1."+bs+" read EOF");
            test.checkEquals(in.readLine(),null,"1."+bs+" read EOF again");
            
            bs=6;
            dataStream=new ByteArrayInputStream(data.getBytes());
            in = new LineInput(dataStream,bs);
            test.checkEquals(in.readLine(),"abcd","1."+bs+" read first line");
            test.checkEquals(in.readLine(),"E","1."+bs+" read line");
            test.checkEquals(in.readLine(),"","1."+bs+" blank line");
            test.checkEquals(in.readLine(),"fghi","1."+bs+" read last line");
            test.checkEquals(in.readLine(),null,"1."+bs+" read EOF");
            test.checkEquals(in.readLine(),null,"1."+bs+" read EOF again");
            
            bs=5;
            dataStream=new ByteArrayInputStream(data.getBytes());
            in = new LineInput(dataStream,bs);
            test.checkEquals(in.readLine(),"abcd","1."+bs+" read first line");
            test.checkEquals(in.readLine(),"E","1."+bs+" read line");
            test.checkEquals(in.readLine(),"","1."+bs+" blank line");
            test.checkEquals(in.readLine(),"fghi","1."+bs+" read last line");
            test.checkEquals(in.readLine(),null,"1."+bs+" read EOF");
            test.checkEquals(in.readLine(),null,"1."+bs+" read EOF again");
            
            bs=4;
            dataStream=new ByteArrayInputStream(data.getBytes());
            in = new LineInput(dataStream,bs);
            test.checkEquals(in.readLine(),"abcd","1."+bs+" read first line");
            test.checkEquals(in.readLine(),"","1."+bs+" blank line");
            test.checkEquals(in.readLine(),"E","1."+bs+" read line");
            test.checkEquals(in.readLine(),"","1."+bs+" blank line");
            test.checkEquals(in.readLine(),"fghi","1."+bs+" read last line");
            test.checkEquals(in.readLine(),null,"1."+bs+" read EOF");
            test.checkEquals(in.readLine(),null,"1."+bs+" read EOF again");
            
            bs=3;
            dataStream=new ByteArrayInputStream(data.getBytes());
            in = new LineInput(dataStream,bs);
            test.checkEquals(in.readLine(),"abc","1."+bs+" read first line");
            test.checkEquals(in.readLine(),"d","1."+bs+" remainder line");
            test.checkEquals(in.readLine(),"E","1."+bs+" read line");
            test.checkEquals(in.readLine(),"","1."+bs+" blank line");
            test.checkEquals(in.readLine(),"fgh","1."+bs+" read last line");
            test.checkEquals(in.readLine(),"i","1."+bs+" remainder line");
            test.checkEquals(in.readLine(),null,"1."+bs+" read EOF");
            test.checkEquals(in.readLine(),null,"1."+bs+" read EOF again");
            
            bs=2;
            dataStream=new ByteArrayInputStream(data.getBytes());
            in = new LineInput(dataStream,bs);
            test.checkEquals(in.readLine(),"ab","1."+bs+" read first line");
            test.checkEquals(in.readLine(),"cd","1."+bs+" remainder line");
            test.checkEquals(in.readLine(),"","1."+bs+" blank line");
            test.checkEquals(in.readLine(),"E","1."+bs+" read line");
            test.checkEquals(in.readLine(),"","1."+bs+" blank line");
            test.checkEquals(in.readLine(),"fg","1."+bs+" read last line");
            test.checkEquals(in.readLine(),"hi","1."+bs+" remainder line");
            test.checkEquals(in.readLine(),null,"1."+bs+" read EOF");
            test.checkEquals(in.readLine(),null,"1."+bs+" read EOF again");
            
            
            dataStream=new ByteArrayInputStream(data.getBytes());
            in = new LineInput(dataStream);
            char[] b = new char[8];
            test.checkEquals(in.readLine(b,0,8),4,"2 read first line");
            test.checkEquals(in.readLine(b,0,8),1,"2 read line");
            test.checkEquals(in.readLine(b,0,8),0,"2 blank line");
            test.checkEquals(in.readLine(b,0,8),4,"2 read last line");
            test.checkEquals(in.readLine(b,0,8),-1,"2 read EOF");
            test.checkEquals(in.readLine(b,0,8),-1,"2 read EOF again");

            
            dataStream=new ByteArrayInputStream(data.getBytes());
            in = new LineInput(dataStream);
            test.checkEquals(in.readLineBuffer().size,4,"3 read first line");
            test.checkEquals(in.readLineBuffer().size,1,"3 read line");
            test.checkEquals(in.readLineBuffer().size,0,"3 blank line");
            test.checkEquals(in.readLineBuffer().size,4,"3 read last line");
            test.checkEquals(in.readLineBuffer(),null,"3 read EOF");
            test.checkEquals(in.readLineBuffer(),null,"3 read EOF again");
            
            dataStream=new ByteArrayInputStream(data.getBytes());
            in = new LineInput(dataStream);
            test.checkEquals(in.readLineBuffer(2).size,2,"4 read first line");
            test.checkEquals(in.readLineBuffer(2).size,2,"4 read rest of first line");
            test.checkEquals(in.readLineBuffer(2).size,1,"4 read line");
            test.checkEquals(in.readLineBuffer(2).size,0,"4 blank line");
            test.checkEquals(in.readLineBuffer(2).size,2,"4 read last line");
            test.checkEquals(in.readLineBuffer(2).size,2,"4 read rest of last line");
            test.checkEquals(in.readLineBuffer(2),null,"4 read EOF");
            test.checkEquals(in.readLineBuffer(2),null,"4 read EOF again");

            dataStream=new ByteArrayInputStream(data.getBytes());
            in = new LineInput(dataStream);
            in.setByteLimit(8);
            test.checkEquals(in.readLine(),"abcd","read first line");
            test.checkEquals(in.readLine(),"E","read line");
            test.checkEquals(in.readLine(),null,"read EOF");
            test.checkEquals(in.readLine(),null,"read EOF again");

            dataStream=new ByteArrayInputStream(data.getBytes());
            in = new LineInput(dataStream);
            test.checkEquals(in.readLine(),"abcd","1 read first line");
            in.setByteLimit(0);
            test.checkEquals(in.skip(4096),0,"bytelimit==0");
            in.setByteLimit(-1);
            test.checkEquals(in.readLine(),"E","1 read line");
            test.checkEquals(in.readLine(),"","1 blank line");
            in.setByteLimit(1);
            test.checkEquals(in.skip(4096),1,"bytelimit==1");
            in.setByteLimit(-1);
            test.checkEquals(in.readLine(),"ghi","1 read last line");
            test.checkEquals(in.readLine(),null,"1 read EOF");
            test.checkEquals(in.readLine(),null,"1 read EOF again");

            String dataCR=
                "abcd\015"+
                "E\015"+
                "\015"+
                "fghi";
            dataStream=new ByteArrayInputStream(dataCR.getBytes());
            in = new LineInput(dataStream,5);
            test.checkEquals(in.readLine(),"abcd","CR read first line");
            test.checkEquals(in.readLine(),"E","CR read line");
            test.checkEquals(in.readLine(),"","CR blank line");
            test.checkEquals(in.readLine(),"fghi","CR read last line");
            test.checkEquals(in.readLine(),null,"CR read EOF");
            test.checkEquals(in.readLine(),null,"CR read EOF again");            
            
            String dataLF=
                "abcd\012"+
                "E\012"+
                "\012"+
                "fghi";
            dataStream=new ByteArrayInputStream(dataLF.getBytes());
            in = new LineInput(dataStream,5);
            test.checkEquals(in.readLine(),"abcd","LF read first line");
            test.checkEquals(in.readLine(),"E","LF read line");
            test.checkEquals(in.readLine(),"","LF blank line");
            test.checkEquals(in.readLine(),"fghi","LF read last line");
            test.checkEquals(in.readLine(),null,"LF read EOF");
            test.checkEquals(in.readLine(),null,"LF read EOF again");

            String dataCRLF=
                "abcd\015\012"+
                "E\015\012"+
                "\015\012"+
                "fghi";
            dataStream=new ByteArrayInputStream(dataCR.getBytes());
            in = new LineInput(dataStream,5);
            test.checkEquals(in.readLine(),"abcd","CRLF read first line");
            test.checkEquals(in.readLine(),"E","CRLF read line");
            test.checkEquals(in.readLine(),"","CRLF blank line");
            test.checkEquals(in.readLine(),"fghi","CRLF read last line");
            test.checkEquals(in.readLine(),null,"CRLF read EOF");
            test.checkEquals(in.readLine(),null,"CRLF read EOF again");
     

            String dataEOF=
                "abcd\015\012"+
                "efgh\015\012"+
                "ijkl\015\012";
            dataStream=new ByteArrayInputStream(dataEOF.getBytes());
            in = new LineInput(dataStream,14);
            test.checkEquals(in.readLine(),"abcd","EOF read first line");
            in.setByteLimit(6);
            test.checkEquals(in.readLine(),"efgh","EOF read second line");
            test.checkEquals(in.readLine(),null,"read EOF");
            in.setByteLimit(-1);
            test.checkEquals(in.readLine(),"ijkl","EOF read second line");
        
            String dataEOL=
                "abcdefgh\015\012"+
                "ijklmnop\015\012"+
                "12345678\015\012"+
                "87654321\015\012";
            
            dataStream=new PauseInputStream(dataEOL.getBytes(),11);
            in = new LineInput(dataStream,100);
            test.checkEquals(in.readLine(),"abcdefgh","EOL read 1");
            test.checkEquals(in.readLine(),"ijklmnop","EOL read 2");
            test.checkEquals(in.readLine(),"12345678","EOL read 3");
            test.checkEquals(in.readLine(),"87654321","EOL read 4");

            dataStream=new PauseInputStream(dataEOL.getBytes(),100);
            in = new LineInput(dataStream,11);
            test.checkEquals(in.readLine(),"abcdefgh","EOL read 1");
            test.checkEquals(in.readLine(),"ijklmnop","EOL read 2");
            test.checkEquals(in.readLine(),"12345678","EOL read 3");
            test.checkEquals(in.readLine(),"87654321","EOL read 4");
            
            dataStream=new PauseInputStream(dataEOL.getBytes(),50);
            in = new LineInput(dataStream,19);
            test.checkEquals(in.readLine(),"abcdefgh","EOL read 1");
            test.checkEquals(in.readLine(),"ijklmnop","EOL read 2");
            in.setByteLimit(5);
            test.checkEquals(in.readLine(),"12345","EOL read 3 limited");
            in.setByteLimit(-1);
            test.checkEquals(in.readLine(),"678","EOL read 4 unlimited");
            test.checkEquals(in.readLine(),"87654321","EOL read 5");

            for (int s=20;s>1;s--)
            {
                dataStream=new PauseInputStream(dataEOL.getBytes(),s);
                in = new LineInput(dataStream,100);
                test.checkEquals(in.readLine(),"abcdefgh",s+" read 1");
                test.checkEquals(in.readLine(),"ijklmnop",s+" read 2");
                test.checkEquals(in.readLine(),"12345678",s+" read 3");
                test.checkEquals(in.readLine(),"87654321",s+" read 4");
            }

        }
        catch(Exception e)
        {
            Code.warning(e);
            test.check(false,e.toString());
        }
    }

    /* ------------------------------------------------------------ */
    private static class PauseInputStream extends ByteArrayInputStream
    {
        int size;
        int c;
        
        PauseInputStream(byte[] data,int size)
        {
            super(data);
            this.size=size;
            c=size;
        }
        
        public synchronized int read()
        {
            c--;
            if(c==0)
                c=size;
            return super.read();
        }
        
        /* ------------------------------------------------------------ */
        public synchronized int read(byte b[], int off, int len)
        {
            if (len>c)
                len=c;
            if(c==0)
            {
                Code.debug("read(b,o,l)==0");
                c=size;
                return 0;
            }
            
            len=super.read(b,off,len);
            if (len>=0)
                c-=len;
            return len;
        }

        /* ------------------------------------------------------------ */
        public int available()
        {   
            if(c==0)
            {
                Code.debug("available==0");
                c=size;
                return 0;
            }
            return c;
        }
    }
    
    /* ------------------------------------------------------------ */
    static class TestThreadPool extends ThreadPool
    {
        /* -------------------------------------------------------- */
        int _calls=0;
        int _waiting=0;
        String _lock="lock";
        
        /* -------------------------------------------------------- */
        TestThreadPool()
            throws Exception
        {
            setName("TestPool");
            setMinThreads(2);
            setMaxThreads(4);
            setMaxIdleTimeMs(500);
        }
        
        /* -------------------------------------------------------- */
        protected void handle(Object job)
            throws InterruptedException
        {
            synchronized(_lock)
            {
                _calls++;
                _waiting++;
            }
            synchronized(job)
            {
                Code.debug("JOB wait: ",job);
                job.wait();
                Code.debug("JOB wake: ",job);
            }
            synchronized(_lock)
            {
                _waiting--;
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    static void testThreadPool()
    {
        Test test = new Test("org.mortbay.util.ThreadPool");
        System.err.print("Testing ThreadPool.");System.err.flush();
        try
        {
            TestThreadPool pool = new TestThreadPool();
            test.check(true,"Constructed");
            pool.start();
            Thread.sleep(100);
            test.check(pool.isStarted(),"Started");
            test.checkEquals(pool.getThreads(),2,"Minimum Threads");
            test.checkEquals(pool._calls,0,"Minimum Threads");
            test.checkEquals(pool._waiting,0,"Minimum Threads");
            
            System.err.print(".");System.err.flush();
            Thread.sleep(550);
            test.checkEquals(pool.getThreads(),2,"Minimum Threads");
            test.checkEquals(pool._calls,0,"Minimum Threads");
            test.checkEquals(pool._waiting,0,"Minimum Threads");

            String j1="Job1";
            String j2="Job2";
            String j3="Job3";
            String j4="Job4";
            String j5="Job5";

            pool.run(j1);
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getThreads(),2,"Job1");
            test.checkEquals(pool._calls,1,"Job1");
            test.checkEquals(pool._waiting,1,"Job1");
            
            pool.run(j2);
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getThreads(),3,"Job2");
            test.checkEquals(pool._calls,2,"Job2");
            test.checkEquals(pool._waiting,2,"Job2");

            pool.run(j3);
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getThreads(),4,"Job3");
            test.checkEquals(pool._calls,3,"Job3");
            test.checkEquals(pool._waiting,3,"Job3");
            
            pool.run(j4);
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getThreads(),4,"Job4");
            test.checkEquals(pool._calls,4,"Job4");
            test.checkEquals(pool._waiting,4,"Job4");
            
            pool.run(j5);
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getThreads(),4,"Job5");
            test.checkEquals(pool._calls,4,"Job5");
            test.checkEquals(pool._waiting,4,"Job5");
            
            synchronized(j1){j1.notify();}
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getThreads(),4,"max threads");
            test.checkEquals(pool._calls,5,"max threads");
            test.checkEquals(pool._waiting,4,"max threads");
            
            synchronized(j2){j2.notify();}
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getThreads(),4,"idle job");
            test.checkEquals(pool._calls,5,"idle job");
            test.checkEquals(pool._waiting,3,"idle job");
            System.err.print(".");System.err.flush();
            Thread.sleep(1000);
            test.checkEquals(pool.getThreads(),4,"idle wait");
            test.checkEquals(pool._calls,5,"idle wait");
            test.checkEquals(pool._waiting,3,"idle wait");
            
            synchronized(j3){j3.notify();}
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getThreads(),4,"idle job");
            test.checkEquals(pool._calls,5,"idle job");
            test.checkEquals(pool._waiting,2,"idle job");
            System.err.print(".");System.err.flush();
            Thread.sleep(550);
            test.checkEquals(pool.getThreads(),3,"idle death");
            test.checkEquals(pool._calls,5,"idle death");
            test.checkEquals(pool._waiting,2,"idle death");

            synchronized(j4){j4.notify();}
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getThreads(),3,"idle job");
            test.checkEquals(pool._calls,5,"idle job");
            test.checkEquals(pool._waiting,1,"idle job");
            System.err.print(".");System.err.flush();
            Thread.sleep(550);
            test.checkEquals(pool.getThreads(),2,"idle death");
            test.checkEquals(pool._calls,5,"idle death");
            test.checkEquals(pool._waiting,1,"idle death");
            
            synchronized(j5){j5.notify();}
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getThreads(),2,"idle job");
            test.checkEquals(pool._calls,5,"idle job");
            test.checkEquals(pool._waiting,0,"idle job");
            System.err.print(".");System.err.flush();
            Thread.sleep(550);
            test.checkEquals(pool.getThreads(),2,"min idle");
            test.checkEquals(pool._calls,5,"min idle");
            test.checkEquals(pool._waiting,0,"min idle");
            
            pool.run(j1);
            pool.run(j2);
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getThreads(),3,"steady state");
            test.checkEquals(pool._calls,7,"steady state");
            test.checkEquals(pool._waiting,2,"steady state");
            synchronized(j2){j2.notify();}
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            pool.run(j2);
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getThreads(),3,"steady state");
            test.checkEquals(pool._calls,8,"steady state");
            test.checkEquals(pool._waiting,2,"steady state");
            synchronized(j1){j1.notify();}
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            pool.run(j2);
            System.err.println(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getThreads(),3,"steady state");
            test.checkEquals(pool._calls,9,"steady state");
            test.checkEquals(pool._waiting,2,"steady state");
            
        }
        catch(Exception e)
        {
            Code.warning(e);
            test.check(false,e.toString());
        }
    }
        

    /* ------------------------------------------------------------ */
    static class TestThreadedServer extends ThreadedServer
    {
        int _jobs=0;
        int _connections=0;
        HashSet _sockets=new HashSet();
        
        /* -------------------------------------------------------- */
        TestThreadedServer()
            throws Exception
        {
            super(new InetAddrPort(8765));
            setMinThreads(2);
            setMaxThreads(4);
            setMaxIdleTimeMs(500);
            setMaxReadTimeMs(60000);
        }
        
        /* -------------------------------------------------------- */
        protected void handleConnection(InputStream in,OutputStream out)
        {
            try
            {
                synchronized(this.getClass())
                {
                    Code.debug("Connection ",in);
                    _jobs++;
                    _connections++;
                }
                
                String line=null;
                LineInput lin= new LineInput(in);
                while((line=lin.readLine())!=null)
                {
                    Code.debug("Line ",line);		    
                    if ("Exit".equals(line))
                        return;
                }
            }
            catch(Error e)
            {
                Code.ignore(e);
            }
            catch(Exception e)
            {
                Code.ignore(e);
            }
            finally
            {    
                synchronized(this.getClass())
                {
                    _jobs--;
                    Code.debug("Disconnect: ",in);
                }
            }
        }

        /* -------------------------------------------------------- */
        PrintWriter stream()
            throws Exception
        {
            InetAddrPort addr = new InetAddrPort();
            addr.setInetAddress(InetAddress.getByName("127.0.0.1"));
            addr.setPort(8765);
            Socket s = new Socket(addr.getInetAddress(),addr.getPort());
            _sockets.add(s);
            Code.debug("Socket ",s);
            return new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
        }    
    }
    
    /* ------------------------------------------------------------ */
    static void testThreadedServer()
    {
        Test test = new Test("org.mortbay.util.ThreadedServer");
        System.err.print("Testing ThreadedServer.");System.err.flush();
        try
        {
            TestThreadedServer server = new TestThreadedServer();
            server.setMaxReadTimeMs(5000);
            test.check(true,"Constructed");
            server.start();
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.check(server.isStarted(),"Started");
            test.checkEquals(server._connections,0,"Minimum Threads");
            test.checkEquals(server._jobs,0,"Minimum Threads");
            test.checkEquals(server.getThreads(),2,"Minimum Threads");
            System.err.print(".");System.err.flush();
            Thread.sleep(550);
            test.check(server.isStarted(),"Started");
            test.checkEquals(server._connections,0,"Minimum Threads");
            test.checkEquals(server._jobs,0,"Minimum Threads");
            test.checkEquals(server.getThreads(),2,"Minimum Threads");
            
            PrintWriter p1 = server.stream();
            System.err.print(".");System.err.flush();
            Thread.sleep(200);
            test.checkEquals(server._connections,1,"New connection");
            test.checkEquals(server._jobs,1,"New connection");
            test.checkEquals(server.getThreads(),2,"New connection");
            
            PrintWriter p2 = server.stream();
            System.err.print(".");System.err.flush();
            Thread.sleep(200);
            test.checkEquals(server._connections,2,"New thread");
            test.checkEquals(server._jobs,2,"New thread");
            test.checkEquals(server.getThreads(),3,"New thread");
            System.err.print(".");System.err.flush();
            Thread.sleep(550);
            test.checkEquals(server._connections,2,"Steady State");
            test.checkEquals(server._jobs,2,"Steady State");
            test.checkEquals(server.getThreads(),3,"Steady State");

            p1.print("Exit\015");
            p1.flush();
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            
            test.checkEquals(server._connections,2,"exit job");
            test.checkEquals(server._jobs,1,"exit job");
            test.checkEquals(server.getThreads(),3,"exit job");
            p1 = server.stream();
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(server._connections,3,"reuse thread");
            test.checkEquals(server._jobs,2,"reuse thread");
            test.checkEquals(server.getThreads(),3,"reuse thread");
            System.err.print(".");System.err.flush();
            Thread.sleep(550);
            test.checkEquals(server._connections,3,"1 idle");
            test.checkEquals(server._jobs,2,"1 idle");
            test.checkEquals(server.getThreads(),3,"1 idle");

            
            p1.print("Exit\015");
            p1.flush();
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            
            test.checkEquals(server._connections,3,"idle thread");
            test.checkEquals(server._jobs,1,"idle thread");
            test.checkEquals(server.getThreads(),3,"idle thread");
            System.err.print(".");System.err.flush();
            Thread.sleep(800);
            test.checkEquals(server._connections,3,"idle death");
            test.checkEquals(server._jobs,1,"idle death");
            test.checkEquals(server.getThreads(),2,"idle death");
            
            
            p1 = server.stream();
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(server._connections,4,"restart thread");
            test.checkEquals(server._jobs,2,"restart thread");
            test.checkEquals(server.getThreads(),3,"restart thread");
            
            PrintWriter p3 = server.stream();
            PrintWriter p4 = server.stream();
            System.err.println(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(server._connections,6,"max thread");
            test.checkEquals(server._jobs,4,"max thread");
            test.checkEquals(server.getThreads(),4,"max thread");

            server.stop();
            server.join();
            test.check(!server.isStarted(),"Stopped");
            test.checkEquals(server.getThreads(),0,"No Threads");
        }
        catch(Exception e)
        {
            Code.warning(e);
            test.check(false,e.toString());
        }
    }    

    /* ------------------------------------------------------------ */
    static void testSingletonList()
    {
        Test t = new Test("org.mortbay.util.SingletonList");
        
        try
        {
            Object o="X";
            SingletonList sl = SingletonList.newSingletonList(o);
            t.checkEquals(sl.size(),1,"SingletonList.size()");
            t.checkEquals(sl.get(0),o,"SingletonList.get(0)");
            Iterator i=sl.iterator();
            ListIterator li=sl.listIterator();
            t.check(i.hasNext(),"SingletonList.iterator().hasNext()");
            t.check(li.hasNext(),"SingletonList.listIterator().hasNext()");
            t.check(!li.hasPrevious(),"SingletonList.listIterator().hasPrevious()");

            t.checkEquals(i.next(),o,"SingletonList.iterator().next()");
            t.check(!i.hasNext(),"SingletonList.iterator().hasNext()");
            t.check(li.hasNext(),"SingletonList.listIterator().hasNext()");
            t.check(!li.hasPrevious(),"SingletonList.listIterator().hasPrevious()");
            
            t.checkEquals(li.next(),o,"SingletonList.listIterator().next()");
            t.check(!i.hasNext(),"SingletonList.iterator().hasNext()");
            t.check(!li.hasNext(),"SingletonList.listIterator().hasNext()");
            t.check(li.hasPrevious(),"SingletonList.listIterator().hasPrevious()");
            
            t.checkEquals(li.previous(),o,"SingletonList.listIterator().previous()");
            t.check(!i.hasNext(),"SingletonList.iterator().hasNext()");
            t.check(li.hasNext(),"SingletonList.listIterator().hasNext()");
            t.check(!li.hasPrevious(),"SingletonList.listIterator().hasPrevious()");
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }
    
    /* ------------------------------------------------------------ */
    static void testLazyList()
    {
        Test t = new Test("org.mortbay.util.LazyList");
        
        try
        {
            LazyList list = null;
            Object o1="X1";
            Object o2="X2";

            // empty list
            List empty = LazyList.getList(list);
            t.checkEquals(empty.size(),0,"empty LazyList");

            // singleton list
            list=LazyList.add(list,o1);
            
            t.checkEquals(list.size(),1,"singleton LazyList.size()");
            t.checkEquals(list.get(0),o1,"singleton LazyList.get(0)");
            Iterator i=list.iterator();
            ListIterator li=list.listIterator();
            t.check(i.hasNext(),"singleton LazyList.iterator().hasNext()");
            t.check(li.hasNext(),"singleton LazyList.listIterator().hasNext()");
            t.check(!li.hasPrevious(),"singleton LazyList.listIterator().hasPrevious()");

            t.checkEquals(i.next(),o1,"singleton LazyList.iterator().next()");
            t.check(!i.hasNext(),"singleton LazyList.iterator().hasNext()");
            t.check(li.hasNext(),"singleton LazyList.listIterator().hasNext()");
            t.check(!li.hasPrevious(),"singleton LazyList.listIterator().hasPrevious()");
            
            t.checkEquals(li.next(),o1,"singleton LazyList.listIterator().next()");
            t.check(!i.hasNext(),"singleton LazyList.iterator().hasNext()");
            t.check(!li.hasNext(),"singleton LazyList.listIterator().hasNext()");
            t.check(li.hasPrevious(),"singleton LazyList.listIterator().hasPrevious()");
            
            t.checkEquals(li.previous(),o1,"singleton LazyList.listIterator().previous()");
            t.check(!i.hasNext(),"singleton LazyList.iterator().hasNext()");
            t.check(li.hasNext(),"singleton LazyList.listIterator().hasNext()");
            t.check(!li.hasPrevious(),"singleton LazyList.listIterator().hasPrevious()");


            // normal list
            list=LazyList.add(list,o2);
            
            t.checkEquals(list.size(),2,"normal LazyList.size()");
            t.checkEquals(list.get(0),o1,"normal LazyList.get(0)");
            t.checkEquals(list.get(1),o2,"normal LazyList.get(0)");
            i=list.iterator();
            li=list.listIterator();
            t.check(i.hasNext(),"normal LazyList.iterator().hasNext()");
            t.check(li.hasNext(),"normal LazyList.listIterator().hasNext()");
            t.check(!li.hasPrevious(),"normal LazyList.listIterator().hasPrevious()");

            t.checkEquals(i.next(),o1,"normal LazyList.iterator().next()");
            t.check(i.hasNext(),"normal LazyList.iterator().hasNext()");
            t.check(li.hasNext(),"normal LazyList.listIterator().hasNext()");
            t.check(!li.hasPrevious(),"normal LazyList.listIterator().hasPrevious()");
            
            t.checkEquals(li.next(),o1,"normal LazyList.listIterator().next()");
            t.check(i.hasNext(),"normal LazyList.iterator().hasNext()");
            t.check(li.hasNext(),"normal LazyList.listIterator().hasNext()");
            t.check(li.hasPrevious(),"normal LazyList.listIterator().hasPrevious()");

            t.checkEquals(i.next(),o2,"normal LazyList.iterator().next()");
            t.check(!i.hasNext(),"normal LazyList.iterator().hasNext()");
            t.check(li.hasNext(),"normal LazyList.listIterator().hasNext()");
            t.check(li.hasPrevious(),"normal LazyList.listIterator().hasPrevious()");
            
            t.checkEquals(li.next(),o2,"normal LazyList.listIterator().next()");
            t.check(!i.hasNext(),"normal LazyList.iterator().hasNext()");
            t.check(!li.hasNext(),"normal LazyList.listIterator().hasNext()");
            t.check(li.hasPrevious(),"normal LazyList.listIterator().hasPrevious()");
            
            t.checkEquals(li.previous(),o2,"normal LazyList.listIterator().previous()");
            t.check(!i.hasNext(),"normal LazyList.iterator().hasNext()");
            t.check(li.hasNext(),"normal LazyList.listIterator().hasNext()");
            t.check(li.hasPrevious(),"normal LazyList.listIterator().hasPrevious()");
            
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }
    
    /* ------------------------------------------------------------ */
    static void testStringMap()
    {
        Test t = new Test("org.mortbay.util.StringMap");
        
        try
        {
            StringMap map = new StringMap();

            map.put("K1","V1");
            t.checkEquals(map.get("K1"),"V1","1V1");
            map.put("K2","V2");
            t.checkEquals(map.get("K1"),"V1","2V1");
            t.checkEquals(map.get("K2"),"V2","2V2");
            map.put("0","V0");
            t.checkEquals(map.get("0"),"V0","3V0");
            t.checkEquals(map.get("K1"),"V1","3V1");
            t.checkEquals(map.get("K2"),"V2","3V2");
            
            map.put("K03","V3");
            t.checkEquals(map.get("0"),"V0","4V0");
            t.checkEquals(map.get("K1"),"V1","4V1");
            t.checkEquals(map.get("K2"),"V2","4V2");
            t.checkEquals(map.get("K03"),"V3","4V3");
            t.checkEquals(map.get("???"),null,"4null");
            
            t.checkEquals(map.getEntry("x0x",1,1).getValue(),"V0","5V0");
            t.checkEquals(map.getEntry("xK1x",1,2).getValue(),"V1","5V1");
            t.checkEquals(map.getEntry("xK2x",1,2).getValue(),"V2","5V2");
            t.checkEquals(map.getEntry("xK03x",1,3).getValue(),"V3","5V3");
            t.checkEquals(map.getEntry("???",1,1),null,"5null");
            
            t.checkEquals(map.getEntry("xKx",1,1),null,"5K");
            
            t.checkEquals(map.getEntry("x0x".toCharArray(),1,1).getValue(),"V0","6V0");
            t.checkEquals(map.getEntry("xK1x".toCharArray(),1,2).getValue(),"V1","6V1");
            t.checkEquals(map.getEntry("xK2x".toCharArray(),1,2).getValue(),"V2","6V2");
            t.checkEquals(map.getEntry("xK03x".toCharArray(),1,3).getValue(),"V3","6V3");
            t.checkEquals(map.getEntry("???".toCharArray(),1,1),null,"6null");
            
            t.checkEquals(map.getEntry("x0x".getBytes(),1,1).getValue(),"V0","7V0");
            t.checkEquals(map.getEntry("xK1x".getBytes(),1,2).getValue(),"V1","7V1");
            t.checkEquals(map.getEntry("xK2x".getBytes(),1,2).getValue(),"V2","7V2");
            t.checkEquals(map.getEntry("xK03x".getBytes(),1,3).getValue(),"V3","7V3");
            t.checkEquals(map.getEntry("???".getBytes(),1,1),null,"7null");
            
            map.setIgnoreCase(false);
            t.checkEquals(map.size(),4,"8size");
            t.checkEquals(map.get("0"),"V0","8V0");
            t.checkEquals(map.get("k1"),null,"8V1");
            t.checkEquals(map.get("k2"),null,"8V2");
            t.checkEquals(map.get("k03"),null,"8V3");
            t.checkEquals(map.get("???"),null,"8null");
            map.setIgnoreCase(true);
            t.checkEquals(map.size(),4,"9size");
            t.checkEquals(map.get("0"),"V0","9V0");
            t.checkEquals(map.get("k1"),"V1","9V1");
            t.checkEquals(map.get("k2"),"V2","9V2");
            t.checkEquals(map.get("k03"),"V3","9V3");
            t.checkEquals(map.get("???"),null,"9null");

            map.put(null,"Vn");
            t.checkEquals(map.size(),5,"10size");
            t.checkEquals(map.get("0"),"V0","10V0");
            t.checkEquals(map.get("k1"),"V1","10V1");
            t.checkEquals(map.get("k2"),"V2","10V2");
            t.checkEquals(map.get("k03"),"V3","10V3");
            t.checkEquals(map.get("???"),null,"10null");
            t.checkEquals(map.get(null),"Vn","10Vn");

            map.remove("XXX");
            t.checkEquals(map.size(),5,"11size5");
            map.remove("k2");
            t.checkEquals(map.size(),4,"11size4");
            map.remove(null);
            t.checkEquals(map.size(),3,"11size3");
            
            
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }    
    
    /* ------------------------------------------------------------ */
    static void testMultiMap()
    {
        Test t = new Test("org.mortbay.util.MultiMap");
        
        try
        {
            MultiMap mm = new MultiMap();

            mm.put("K1","V1");
            t.checkEquals(mm.get("K1"),"V1","as Map");
            t.checkEquals(mm.getValues("K1").get(0),"V1","as List");
            mm.add("K1","V2");
            t.checkEquals(mm.getValues("K1").get(0),"V1","add List");
            t.checkEquals(mm.getValues("K1").get(1),"V2","add List");

            mm.put("K2",new Integer(2));
            t.checkEquals(mm.getValues("K2").get(0),new Integer(2),"as Object");

            MultiMap m2=(MultiMap)mm.clone();
            m2.add("K1","V3");
            t.checkEquals(mm.getValues("K1").size(),2,"unchanged List");
            t.checkEquals(mm.getValues("K1").get(0),"V1","unchanged List");
            t.checkEquals(mm.getValues("K1").get(1),"V2","unchanged List");
            t.checkEquals(m2.getValues("K1").get(0),"V1","clone List");
            t.checkEquals(m2.getValues("K1").get(1),"V2","clone List");
            t.checkEquals(m2.getValues("K1").get(2),"V3","clone List");
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }

    
    /* --------------------------------------------------------------- */
    public static void testResource()
    {
        Test t = new Test("org.mortbay.util.Resource");
        try
        {
            Resource r;
            r = Resource.newResource(__userURL+"TestHarness.java");
            
            t.check(r.exists(),"File URL exists");            
            t.check(!r.isDirectory(),"File URL not directory");
            
            r = Resource.newResource(__userDir+File.separator+
                                     "TestHarness.java");
            t.check(r.exists(),"Abs File exists");
            t.check(!r.isDirectory(),"Abs URL not directory");
            
            r = Resource.newResource(__relDir+"TestHarness.java");
            t.check(r.exists(),"Rel File exists");
            t.check(!r.isDirectory(),"Rel URL not directory");


            String us=__userURL.toString();
            us=us.substring(0,us.length()-1);
            
            Resource rt = Resource.newResource(us);
            Resource rt1=rt.addPath("TestData");
            
            t.check(rt1.exists(),"Test File exists");
            t.check(rt1.isDirectory(),"Test URL is directory");
            Resource rt2=rt.addPath("TestData/");
            t.check(rt2.exists(),"Test File exists");
            t.check(rt2.isDirectory(),"Test URL is directory");
            
            
            Resource b = Resource.newResource(__userURL.toString());
            
            r = b.addPath("Resource.java");
            t.check(r.exists(),"AddPath resource exists");
            r = b.addPath("UnknownFile");
            t.check(!r.exists(),"AddPath resource ! exists");
            r = b.addPath("/Resource.java");
            t.check(r.exists(),"AddPath resource exists");
            r = b.addPath("/UnknownFile");
            t.check(!r.exists(),"AddPath resource ! exists");

            
            b = Resource.newResource(us);
            r = b.addPath("Resource.java");
            t.check(r.exists(),"AddPath resource exists");
            r = b.addPath("UnknownFile");
            t.check(!r.exists(),"AddPath resource ! exists");
            r = b.addPath("/Resource.java");
            t.check(r.exists(),"AddPath resource exists");
            r = b.addPath("/UnknownFile");
            t.check(!r.exists(),"AddPath resource ! exists");            

            r = Resource.newResource(us);
            t.check(r.exists(),"Dir URL exists");
            t.check(r.isDirectory(),"Dir URL directory");
            r = r.addPath("Resource.java");
            t.check(r.exists(),"AddPath resource exists");
            r = r.addPath("UnknownFile");
            t.check(!r.exists(),"AddPath resource ! exists");

            // Test JAR Resource
            Resource j = Resource.newResource("jar:file:/somejar.jar!/content/");
            t.checkEquals(j.getFile(),null,"no file for jar:");
            t.check(!j.exists(),"no jar file with content");
            j = Resource.newResource("jar:file:/somejar.jar!/");
            t.check(!j.exists(),"no jar file");
            
            j = Resource.newResource("jar:"+__userURL+
                                     "TestData/test.zip!/");
            t.check(j instanceof JarFileResource,"Jar Resource");
            t.checkEquals(j.getFile(),null,"no file for jar:");
            t.checkContains(j.getName(),"jar:file:","jar name");
            t.checkContains(j.getName(),"/TestData/test.zip!/","jar name");
            t.check(j.exists(),"jar exists");
            t.check(j.isDirectory(),"root directory");
            t.check(j.lastModified()!=-1,"Last Modified");

            r=j.addPath("Unknown");
            t.checkContains(r.toString(),"!/Unknown","jar unknown content");
            t.check(!r.exists(),"jar unknown content");
            t.check(!r.isDirectory(),"unknown directory");
            r=r.addPath("/Unknown/");
            t.checkContains(r.toString(),"!/Unknown/","jar unknown content/");
            t.check(!r.exists(),"jar unknown content/");
            t.check(!r.isDirectory(),"unknown/ directory");

            r=j.addPath("subdir");
            t.checkContains(r.toString(),"!/subdir","jar directory");
            t.check(!r.exists(),"jar directory exists");
            t.check(!r.isDirectory(),"jar directory");
            
            r=j.addPath("/subdir/");
            t.checkContains(r.toString(),"!/subdir/","jar directory/");
            t.check(r.exists(),"jar directory/");
            t.check(r.isDirectory(),"jar directory/");
            t.check(r.lastModified()!=-1,"Last Modified");
            String[] l = r.list();
            t.checkEquals(l.length,3,"List directory");
            
            r=j.addPath("alphabet");
            t.checkContains(r.toString(),"!/alphabet","jar file");
            t.check(r.exists(),"jar file");
            t.check(!r.isDirectory(),"jar file");
            t.check(r.lastModified()!=-1,"Last Modified");
            InputStream in = r.getInputStream();
            String data=IO.toString(in);
            t.checkContains(data,"ABCDEFGHIJKLMNOPQRSTUVWXYZ","Fetched file");
            
            r=j.addPath("/subdir/alphabet");
            t.checkContains(r.toString(),"!/subdir/alphabet","jar file");
            t.check(r.exists(),"jar file");
            t.check(!r.isDirectory(),"jar file");
            t.check(r.lastModified()!=-1,"Last Modified");
            in = r.getInputStream();
            data=IO.toString(in);
            t.checkContains(data,"ABCDEFGHIJKLMNOPQRSTUVWXYZ","Fetched file");  

            // Test HTTP Resource
            try{
                Resource h = Resource.newResource("http://jetty.mortbay.org/demo/files/");
                in = h.getInputStream();
                t.check(h.exists(),"http exists");
                t.check(h.isDirectory(),"http directory");
                t.check(h.lastModified()!=-1,"http Modified");
                data=IO.toString(in);
                t.checkContains(data,"file.txt","Fetched http"); 
                t.checkContains(data,"file.html","Fetched http");
                t.check(h.exists(),"http exists");
                t.check(h.lastModified()!=-1,"http Modified");
                in = h.getInputStream();
                data=IO.toString(in);
                t.checkContains(data,"file.txt","Fetched http"); 
                t.checkContains(data,"file.html","Fetched http");
                
                h=h.addPath("file.txt");
                t.check(h.exists(),"http exists");
                t.check(!h.isDirectory(),"http !directory");
                
                h= Resource.newResource("http://jetty.mortbay.org/xxxx");
                t.check(!h.exists(),"http !exists "+h);
                t.check(!h.isDirectory(),"http !directory ");
            }
            catch(java.net.UnknownHostException uhe)
            {
                Code.ignore(uhe);
                Code.warning("org.mortbay.util.Resource Not fully tested. An internet connection is required");
            }
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }

    /* ------------------------------------------------------------ */
    public static void testJarURL()
    {
        Test t = new Test("org.mortbay.util.Zip");
        try
        {
            // Test jar update
            File tmpJar = File.createTempFile("test",".jar");
            tmpJar.deleteOnExit();
            
            URL jar1 = new URL(__userURL+"TestData/test.zip");
            System.err.println(jar1);
            IO.copy(jar1.openStream(),new FileOutputStream(tmpJar));
            URL url1 = new URL("jar:"+tmpJar.toURL()+"!/");
            JarURLConnection jc1 = (JarURLConnection)url1.openConnection();
            JarFile j1=jc1.getJarFile();
            System.err.println("T1:");
            Enumeration e = j1.entries();
            while(e.hasMoreElements())
                System.err.println(e.nextElement());
            
            
            URL jar2 = new URL(__userURL+"TestData/alt.zip");
            System.err.println(jar2);
            IO.copy(jar2.openStream(),new FileOutputStream(tmpJar));
            URL url2 = new URL("jar:"+tmpJar.toURL()+"!/");
            JarURLConnection jc2 = (JarURLConnection)url2.openConnection();
            JarFile j2=jc2.getJarFile();
            System.err.println("T2:");
            e = j2.entries();
            while(e.hasMoreElements())
                System.err.println(e.nextElement());
            
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
        }
    }
    
    /* ------------------------------------------------------------ */
    /** main.
     */
    public static void main(String[] args)
    {
        try
        {
       	    testTest();
            testLog();
            testFrame();
            testCode();
            testPassword();
            testStringMap();
            testSingletonList();
            testLazyList();
            testMultiMap();
            testQuotedStringTokenizer();            
            testDateCache();
            testBlockingQueue();
            testIO();
            testUrlEncoded();
            testURI();
            testLineInput();
            testThreadPool();
            testThreadedServer();
            testB64();
            testResource();

            //testJarURL();
        }
        catch(Throwable th)
        {
            Code.warning(th);
            Test t = new Test("org.mortbay.util.TestHarness");
            t.check(false,th.toString());
        }
        finally
        {
            Test.report();
        }
    }
}
