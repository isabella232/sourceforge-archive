// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;
import com.mortbay.Util.DataClassTest.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class TestHarness
{
    
    /* ------------------------------------------------------------ */
    /** 
     */
    static void testDateCache()
    {
        Test t = new Test("com.mortbay.Util.DateCache");
        
        DateCache dc = new DateCache();
        try
        {
            for (int i=0;i<25;i++)
            {
                Thread.sleep(100);
                System.err.println(dc.format(System.currentTimeMillis()));
            }
            t.check(true,"Very poor test harness");
        }
        catch(Exception e)
        {
            Code.warning(e);
            t.check(false,e.toString());
            
        }
    }
    
    static void testTest()
    {
        Test t1 = new Test("Test all pass");
        Test t2 = new Test(Test.SelfFailTest);

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
        Log.message("TAG","MSG",new Frame());
        Log.event("Test event");
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
        
        code._debugOn=false;
        Code.debug("message");
        
        code._debugOn=true;
        Code.debug("message");
        Code.debug("message",new Throwable());
        Code.debug("object",new Throwable(),"\n",code);
        
        code._debugPatterns = new Vector();
        code._debugPatterns.addElement("ZZZZZ");
        Code.debug("YOU SHOULD NOT SEE THIS");
        Code.debug("YOU SHOULD"," NOT SEE ","THIS");
        
        code._debugPatterns.addElement("ISS.Base");
        Code.debug("message");
        
        code._debugPatterns = null;

        Code.warning("warning");
        
        Code.setDebug(false);
        Code.setDebugTriggers("FOO,BAR");
        Code.debug("YOU SHOULD NOT SEE THIS");
        Code.triggerOn("BLAH");
        Code.debug("YOU SHOULD NOT SEE THIS");
        Code.triggerOn("FOO");
        Code.debug("triggered");
        Code.triggerOn("BAR");
        Code.debug("triggered");
        Code.triggerOn("FOO");
        Code.triggerOff("FOO");
        Code.debug("triggered");
        Code.triggerOff("BAR");
        Code.debug("YOU SHOULD NOT SEE THIS");
        Code.triggerOff("BLAH");
        Code.debug("YOU SHOULD NOT SEE THIS");
        
        Code.setDebug(true);
        
        try
        {
            Code.fail("fail");
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
            
            Code.assertEquals("foo","bar","assert");
            t.check(false,"Assert");
        }
        catch(CodeException e)
        {
            Code.warning(e);
            t.check(true,"Assert");
        }
        t.check(true,"Output must be visually inspected");
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
            t.check(false,"Exception: "+e);
        }
    }

    /* ------------------------------------------------------------ */
    public static void testBlockingQueue()
        throws Exception
    {
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
    }
    
    /* -------------------------------------------------------------- */
    public static void testUrlEncoded()
    {
        Test test = new Test("com.mortbay.Util.UrlEncoded");

        try{
                
            UrlEncoded code = new UrlEncoded();
            test.checkEquals(code.size(),0,"Empty");

            code.clear();
            code.read("Name1=Value1");
            test.checkEquals(code.size(),1,"simple param size");
            test.checkEquals(code.encode(),"Name1=Value1","simple encode");
            test.checkEquals(code.get("Name1"),"Value1","simple get");

            code.clear();
            code.read("Name2=");
            test.checkEquals(code.size(),1,"dangling param size");
            test.checkEquals(code.encode(),"Name2","dangling encode");
            test.checkEquals(code.get("Name2"),UrlEncoded.noValue,"dangling get");
        
            code.clear();
            code.read("Name3");
            test.checkEquals(code.size(),1,"noValue param size");
            test.checkEquals(code.encode(),"Name3","noValue encode");
            test.checkEquals(code.get("Name3"),UrlEncoded.noValue,"noValue get");
        
            code.clear();
            code.read("Name4=Value+4%21");
            test.checkEquals(code.size(),1,"encoded param size");
            test.checkEquals(code.encode(),"Name4=Value+4%21","encoded encode");
            test.checkEquals(code.get("Name4"),"Value 4!","encoded get");

            code.clear();
            code.read("Name5=aaa&Name6=bbb");
            test.checkEquals(code.size(),2,"multi param size");
            test.check(code.encode().equals("Name5=aaa&Name6=bbb") ||
                       code.encode().equals("Name6=bbb&Name5=aaa"),
                       "multi encode");
            test.checkEquals(code.get("Name5"),"aaa","multi get");
            test.checkEquals(code.get("Name6"),"bbb","multi get");
        
            code.clear();
            code.read("Name7=aaa&Name7=b%2Cb&Name7=ccc");
            test.checkEquals(code.encode(),
                             "Name7=aaa&Name7=b%2Cb&Name7=ccc",
                             "multi encode");
            test.checkEquals(code.get("Name7"),"aaa,b,b,ccc","list get all");
            test.checkEquals(code.getValues("Name7")[0],"aaa","list get");
            test.checkEquals(code.getValues("Name7")[1],"b,b","list get");
            test.checkEquals(code.getValues("Name7")[2],"ccc","list get");
        }
        catch(Exception e){
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

            uri = new URI("/Test/URI");
            test.checkEquals(uri.toString(),"/Test/URI","no params");
    
            uri = new URI("/Test/URI?a=1");
            test.checkEquals(uri.toString(),"/Test/URI?a=1","one param");
        
            uri = new URI("/Test/URI");
            uri.put("b","2 !");
            test.checkEquals(uri.toString(),"/Test/URI?b=2+%21","add param");
        
            uri = new URI("/Test/URI?c=1&d=2");
            uri.put("e","3");
            String s = uri.toString();
            test.check(s.startsWith("/Test/URI?"),"merge params path");
            test.check(s.indexOf("c=1")>0,"merge params c1");
            test.check(s.indexOf("d=2")>0,"merge params d2");
            test.check(s.indexOf("e=3")>0,"merge params e3");

            uri = new URI("/Test/URI?a=");
            test.checkEquals(uri.toString(),"/Test/URI?a=","null param");
            uri.parameters();
            test.checkEquals(uri.toString(),"/Test/URI?a","null param");
            uri.encodeNulls(true);
            test.checkEquals(uri.toString(),"/Test/URI?a=","null= param");
        }
        catch(Exception e){
            test.check(false,e.toString());
        }
    }
    
    /* ------------------------------------------------------------ */
    /** main
     */
    public static void main(String[] args)
    {
        try{
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
