// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Base;
import java.util.*;

public class TestHarness
{
    /* ------------------------------------------------------------ */
    /** 
     */
    static void dateTest()
    {
        Test t = new Test("com.mortbay.Base.DateCache");
        
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

	dc=new DateCache("EEE, dd MMM yyyy HH:mm:ss 'GMT'",Locale.US);
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
    /** 
     */
    static void logTest()
    {
        // XXX - this is not even a test harness - poor show!
        Log.instance();
        Log.message("TAG","MSG",new Frame());
        Log.event("event");
        Log.warning("warning");
    }

    /* ------------------------------------------------------------ */
    private static void testChecker(Test t, Frame f, String desc,
                                    String method, int depth,
                                    String thread, String file)
    {
        t.checkContains(f._method, method, desc+": method");
        t.checkEquals(f._depth, depth, desc+": depth");
        t.checkEquals(f._thread, thread, desc+": thread");
        t.checkContains(f._file, file, desc+": file");
    }
    
    /* ------------------------------------------------------------ */
    static void frameTest()
    {
        Test t = new Test("com.mortbay.Base.Frame");
        Frame f = new Frame();
        testChecker(t, f, "default constructor",
                    "com.mortbay.Base.TestHarness.frameTest",
                    2, "main", "TestHarness.java");
        f = f.getParent();
        testChecker(t, f, "getParent",
                    "com.mortbay.Base.TestHarness.main",
                    1, "main", "TestHarness.java");
        f = f.getParent();
        t.checkEquals(f, null, "getParent() off top of stack");
        f = new Frame(1);
        testChecker(t, f, "new Frame(1)",
                    "com.mortbay.Base.TestHarness.main",
                    1, "main", "TestHarness.java");
        f = new Frame(1, true);
        testChecker(t, f, "partial",
                    "unknownMethod", 0, "unknownThread", "UnknownFile");
        f.complete();
        testChecker(t, f, "new Frame(1)",
                    "com.mortbay.Base.TestHarness.main",
                    1, "main", "TestHarness.java");
    }
    
    /*-------------------------------------------------------------------*/
    /** 
     */
    static void codeTest()
    {
        // Also not a test harness
        Test t = new Test("com.mortbay.Base.Code");
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
    public static void main(String[] arg)
    {
        try
        {
            dateTest();
            testTest();
            logTest();
            frameTest();
            codeTest();
        }
        catch(Exception e)
        {
            Code.warning(e);
            Test t=new Test(e.toString());
            t.check(false,e.toString());
        }
        finally
        {
            Test.report();
        }
    }
};





