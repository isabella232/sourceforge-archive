// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;
import com.sun.java.util.collections.*;
import com.mortbay.Util.DataClassTest.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class TestHarness
{
    public final static String __CRLF = "\015\012";
    
    /* ------------------------------------------------------------ */
    static void testDateCache()
    {
        Test t = new Test("com.mortbay.Util.DateCache");
        
        DateCache dc = new DateCache("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
        try
        {
            String last=dc.format(System.currentTimeMillis());
            boolean change=false;
            for (int i=0;i<15;i++)
            {
                Thread.sleep(100);
                String date=dc.format(System.currentTimeMillis());
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
        Test t = new Test("com.mortbay.Util.Frame");
        Frame f = new Frame();
        testFrameChecker(t, f, "default constructor",
                         "com.mortbay.Util.TestHarness.testFrame",
                         2, "main", "TestHarness.java");
        f = f.getParent();
        testFrameChecker(t, f, "getParent",
                         "com.mortbay.Util.TestHarness.main",
                         1, "main", "TestHarness.java");
        f = f.getParent();
        t.checkEquals(f, null, "getParent() off top of stack");
        f = new Frame(1);
        testFrameChecker(t, f, "new Frame(1)",
                         "com.mortbay.Util.TestHarness.main",
                         1, "main", "TestHarness.java");
        f = new Frame(1, true);
        testFrameChecker(t, f, "partial",
                         "unknownMethod", 0, "unknownThread", "UnknownFile");
        f.complete();
        testFrameChecker(t, f, "new Frame(1)",
                         "com.mortbay.Util.TestHarness.main",
                         1, "main", "TestHarness.java");
    }
    
    /*-------------------------------------------------------------------*/
    /** 
     */
    static void testCode()
    {
        // Also not a test harness
        Test t = new Test("com.mortbay.Util.Code");
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
            System.err.println("\n\nEXPECT Warning: <stack>");
            Code.warning(e);
            t.check(true,"Assert");
        }
    }
    
    /* ------------------------------------------------------------ */
    public static void testDataHelper()
    {
        Test t = new Test("com.mortbay.Util.DataHelper");
        try{
            T2 t2 = (com.mortbay.Util.DataClassTest.T2)
                DataClass.emptyInstance(com.mortbay.Util.DataClassTest.T2.class);

            t.check(t2!=null,"empty t2 constructed");
            t.checkEquals(t2.a.length,0,"empty array");
            t.check(t2.t1!=null,"empty t1 constructed");
            t.checkEquals(t2.t1.s,"","empty string");
            t.checkEquals(t2.t1.i,0,"zero int");
            t.checkEquals(t2.t1.I.intValue(),0,"zero Integer");

            t2.t1.i=42;
            t2.t1.s="check";
            t2.a=new T1[2];
            t2.a[0]=(com.mortbay.Util.DataClassTest.T1)
                DataClass.emptyInstance(com.mortbay.Util.DataClassTest.T1.class);

            String out = DataClass.toString(t2);
            if (Code.debug())
                System.out.println(out);

            t.checkContains(out,"i = 42;","toString int");
            t.checkContains(out,"check","toString string");
            t.checkContains(out,"[","toString null array open");
            t.checkContains(out,"]","toString null array close");
            t.checkContains(out,"null","toString null array element");
            
        }
        catch(Exception e){
            t.check(false,"Exception: "+e);
        }
    }

    /* ------------------------------------------------------------ */
    public static void testIO()
    {
        Test t = new Test("com.mortbay.Util.IO");
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
        Test t = new Test("com.mortbay.Util.B64Code");
        try{
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
        final Test t = new Test("com.mortbay.Util.BlockingQueue");

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
        Test test = new Test("com.mortbay.Util.UrlEncoded");

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
            test.checkEquals(code.getString("Name2"),null,"dangling get");
        
            code.clear();
            code.decode("Name3");
            test.checkEquals(code.size(),1,"noValue param size");
            test.checkEquals(code.encode(),"Name3","noValue encode");
            test.checkEquals(code.getString("Name3"),null,"noValue get");
        
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
        Test test = new Test("com.mortbay.Util.URI");
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
        
            // Host and port
            uri = new URI("http://host:8080");
            test.checkEquals(uri.getPath(),"/","root");
            
            uri = new URI("http://host:8080/");
            test.checkEquals(uri.getPath(),"/","root");
            
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
            
            uri = new URI("/Test/Nasty%26%3F+URI?c=%26&d=%3F");
            test.checkEquals(uri.getPath(),"/Test/Nasty&? URI","nasty");
            uri.setPath("/test/nasty&? URI");
            uri.getParameters();
            test.checkEquals(uri.toString(),
                             "/test/nasty%26%3F+URI?c=%26&d=%3F","nasty");
            uri=(URI)uri.clone();
            test.checkEquals(uri.toString(),
                             "/test/nasty%26%3F+URI?c=%26&d=%3F","clone");
            
            
        }
        catch(Exception e){
            Code.warning(e);
            test.check(false,e.toString());
        }
    }
    
    /* ------------------------------------------------------------ */
    public static void testQuotedStringTokenizer()
    {
        Test test = new Test("com.mortbay.Util.QuotedStringTokenizer");
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
        Test test = new Test("com.mortbay.Util.LineInput");
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
            test.checkEquals(in.readLine(),"","1."+bs+" blank line");
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
            
            bs=1;
            dataStream=new ByteArrayInputStream(data.getBytes());
            in = new LineInput(dataStream,bs);
            test.checkEquals(in.readLine(),"a","1."+bs+" read first line");
            test.checkEquals(in.readLine(),"b","1."+bs+" remainder line");
            test.checkEquals(in.readLine(),"c","1."+bs+" remainder line");
            test.checkEquals(in.readLine(),"d","1."+bs+" remainder line");
            test.checkEquals(in.readLine(),"","1."+bs+" blank line");
            test.checkEquals(in.readLine(),"","1."+bs+" blank line");
            test.checkEquals(in.readLine(),"E","1."+bs+" read line");
            test.checkEquals(in.readLine(),"","1."+bs+" blank line");
            test.checkEquals(in.readLine(),"","1."+bs+" blank line");
            test.checkEquals(in.readLine(),"f","1."+bs+" read last line");
            test.checkEquals(in.readLine(),"g","1."+bs+" remainder line");
            test.checkEquals(in.readLine(),"h","1."+bs+" remainder line");
            test.checkEquals(in.readLine(),"i","1."+bs+" remainder line");
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
            
        }
        catch(Exception e)
        {
            Code.warning(e);
            test.check(false,e.toString());
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
            super("TestPool",2,4,500);
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
        Test test = new Test("com.mortbay.Util.ThreadPool");
        System.err.print("Testing ThreadPool.");System.err.flush();
        try
        {
            TestThreadPool pool = new TestThreadPool();
            test.check(true,"Constructed");
            pool.start();
            Thread.sleep(100);
            test.check(pool.isStarted(),"Started");
            test.checkEquals(pool.getSize(),2,"Minimum Threads");
            test.checkEquals(pool._calls,0,"Minimum Threads");
            test.checkEquals(pool._waiting,0,"Minimum Threads");
            
            System.err.print(".");System.err.flush();
            Thread.sleep(550);
            test.checkEquals(pool.getSize(),2,"Minimum Threads");
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
            test.checkEquals(pool.getSize(),2,"Job1");
            test.checkEquals(pool._calls,1,"Job1");
            test.checkEquals(pool._waiting,1,"Job1");
            
            pool.run(j2);
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getSize(),3,"Job2");
            test.checkEquals(pool._calls,2,"Job2");
            test.checkEquals(pool._waiting,2,"Job2");

            pool.run(j3);
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getSize(),4,"Job3");
            test.checkEquals(pool._calls,3,"Job3");
            test.checkEquals(pool._waiting,3,"Job3");
            
            pool.run(j4);
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getSize(),4,"Job4");
            test.checkEquals(pool._calls,4,"Job4");
            test.checkEquals(pool._waiting,4,"Job4");
            
            pool.run(j5);
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getSize(),4,"Job5");
            test.checkEquals(pool._calls,4,"Job5");
            test.checkEquals(pool._waiting,4,"Job5");
            
            synchronized(j1){j1.notify();}
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getSize(),4,"max threads");
            test.checkEquals(pool._calls,5,"max threads");
            test.checkEquals(pool._waiting,4,"max threads");
            
            synchronized(j2){j2.notify();}
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getSize(),4,"idle job");
            test.checkEquals(pool._calls,5,"idle job");
            test.checkEquals(pool._waiting,3,"idle job");
            System.err.print(".");System.err.flush();
            Thread.sleep(1000);
            test.checkEquals(pool.getSize(),4,"idle wait");
            test.checkEquals(pool._calls,5,"idle wait");
            test.checkEquals(pool._waiting,3,"idle wait");
            
            synchronized(j3){j3.notify();}
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getSize(),4,"idle job");
            test.checkEquals(pool._calls,5,"idle job");
            test.checkEquals(pool._waiting,2,"idle job");
            System.err.print(".");System.err.flush();
            Thread.sleep(550);
            test.checkEquals(pool.getSize(),3,"idle death");
            test.checkEquals(pool._calls,5,"idle death");
            test.checkEquals(pool._waiting,2,"idle death");

            synchronized(j4){j4.notify();}
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getSize(),3,"idle job");
            test.checkEquals(pool._calls,5,"idle job");
            test.checkEquals(pool._waiting,1,"idle job");
            System.err.print(".");System.err.flush();
            Thread.sleep(550);
            test.checkEquals(pool.getSize(),2,"idle death");
            test.checkEquals(pool._calls,5,"idle death");
            test.checkEquals(pool._waiting,1,"idle death");
            
            synchronized(j5){j5.notify();}
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getSize(),2,"idle job");
            test.checkEquals(pool._calls,5,"idle job");
            test.checkEquals(pool._waiting,0,"idle job");
            System.err.print(".");System.err.flush();
            Thread.sleep(550);
            test.checkEquals(pool.getSize(),2,"min idle");
            test.checkEquals(pool._calls,5,"min idle");
            test.checkEquals(pool._waiting,0,"min idle");
            
            pool.run(j1);
            pool.run(j2);
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getSize(),3,"steady state");
            test.checkEquals(pool._calls,7,"steady state");
            test.checkEquals(pool._waiting,2,"steady state");
            synchronized(j2){j2.notify();}
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            pool.run(j2);
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getSize(),3,"steady state");
            test.checkEquals(pool._calls,8,"steady state");
            test.checkEquals(pool._waiting,2,"steady state");
            synchronized(j1){j1.notify();}
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            pool.run(j2);
            System.err.println(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(pool.getSize(),3,"steady state");
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
            super(new InetAddrPort(8765),2,4,500, 60000);
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
    };
    
    /* ------------------------------------------------------------ */
    static void testThreadedServer()
    {
        Test test = new Test("com.mortbay.Util.ThreadedServer");
        System.err.print("Testing ThreadedServer.");System.err.flush();
        try
        {
            TestThreadedServer server = new TestThreadedServer();
            test.check(true,"Constructed");
            server.start();
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.check(server.isStarted(),"Started");
            test.checkEquals(server._connections,0,"Minimum Threads");
            test.checkEquals(server._jobs,0,"Minimum Threads");
            test.checkEquals(server.getSize(),2,"Minimum Threads");
            System.err.print(".");System.err.flush();
            Thread.sleep(550);
            test.check(server.isStarted(),"Started");
            test.checkEquals(server._connections,0,"Minimum Threads");
            test.checkEquals(server._jobs,0,"Minimum Threads");
            test.checkEquals(server.getSize(),2,"Minimum Threads");
            
            PrintWriter p1 = server.stream();
            System.err.print(".");System.err.flush();
            Thread.sleep(200);
            test.checkEquals(server._connections,1,"New connection");
            test.checkEquals(server._jobs,1,"New connection");
            test.checkEquals(server.getSize(),2,"New connection");
            
            PrintWriter p2 = server.stream();
            System.err.print(".");System.err.flush();
            Thread.sleep(200);
            test.checkEquals(server._connections,2,"New thread");
            test.checkEquals(server._jobs,2,"New thread");
            test.checkEquals(server.getSize(),3,"New thread");
            System.err.print(".");System.err.flush();
            Thread.sleep(550);
            test.checkEquals(server._connections,2,"Steady State");
            test.checkEquals(server._jobs,2,"Steady State");
            test.checkEquals(server.getSize(),3,"Steady State");

            p1.print("Exit\015");
            p1.flush();
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            
            test.checkEquals(server._connections,2,"exit job");
            test.checkEquals(server._jobs,1,"exit job");
            test.checkEquals(server.getSize(),3,"exit job");
            p1 = server.stream();
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(server._connections,3,"reuse thread");
            test.checkEquals(server._jobs,2,"reuse thread");
            test.checkEquals(server.getSize(),3,"reuse thread");
            System.err.print(".");System.err.flush();
            Thread.sleep(550);
            test.checkEquals(server._connections,3,"1 idle");
            test.checkEquals(server._jobs,2,"1 idle");
            test.checkEquals(server.getSize(),3,"1 idle");

            p1.print("Exit\015");
            p1.flush();
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            
            test.checkEquals(server._connections,3,"idle thread");
            test.checkEquals(server._jobs,1,"idle thread");
            test.checkEquals(server.getSize(),3,"idle thread");
            System.err.print(".");System.err.flush();
            Thread.sleep(800);
            test.checkEquals(server._connections,3,"idle death");
            test.checkEquals(server._jobs,1,"idle death");
            test.checkEquals(server.getSize(),2,"idle death");
            
            p1 = server.stream();
            System.err.print(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(server._connections,4,"restart thread");
            test.checkEquals(server._jobs,2,"restart thread");
            test.checkEquals(server.getSize(),3,"restart thread");
            
            PrintWriter p3 = server.stream();
            PrintWriter p4 = server.stream();
            System.err.println(".");System.err.flush();
            Thread.sleep(100);
            test.checkEquals(server._connections,6,"max thread");
            test.checkEquals(server._jobs,4,"max thread");
            test.checkEquals(server.getSize(),4,"max thread");

            server.destroy();
            server.join();
            test.check(!server.isStarted(),"Stopped");
            test.checkEquals(server.getSize(),0,"No Threads");
        }
        catch(Exception e)
        {
            Code.warning(e);
            test.check(false,e.toString());
        }
    }
    
    
    /* ------------------------------------------------------------ */
    /** main
     */
    public static void main(String[] args)
    {
        try
        {
            testQuotedStringTokenizer();            
            testDateCache();
            testTest();
            testLog();
            testFrame();
            testCode();
            testDataHelper();
            testBlockingQueue();
            testIO();
            testUrlEncoded();
            testURI();
            testLineInput();
            testThreadPool();
            testThreadedServer();
            testB64();
            PropertyTreeTest.test();
        }
        catch(Throwable th)
        {
            Code.warning(th);
            Test t = new Test("com.mortbay.Util.TestHarness");
            t.check(false,th.toString());
        }
        finally
        {
            Test.report();
        }
    }
};
