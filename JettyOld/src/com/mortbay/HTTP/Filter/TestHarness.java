// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP.Filter;

import com.mortbay.Base.*;
import java.util.*;
import java.io.*;

public class TestHarness
{
    static int __called=0;
    static Hashtable zero;
    static Double one;
    static String two;
    static String three;
    
    /* ----------------------------------------------------------------- */
    public static String test1()
    {
	Code.debug("CALLED test()");
	__called++;
	return "T"+__called;
    }

    /* ----------------------------------------------------------------- */
    public static void test2(Hashtable a0,Double a1,String a2,String a3)
    {
	zero=a0;
	one=a1;
	two=a2;
	three=a3;
	__called++;
	Code.debug("CALLED uniqTest("+zero+
		   ","+one+
		   ","+two+
		   ","+three+
		   ")");
    }
    
    
    /* ----------------------------------------------------------------- */
    public static void methodTag()
    {
	Test test = new Test("com.mortbay.HTTP.Filter.MethodTag");
	try {
	    new MethodTag("com.mortbay.HTTP.Filter.TestHarness.test1();",
			  null,null).invoke();
	    test.checkEquals(__called,1,"called");
	    
	    Hashtable named = new Hashtable();
	    named.put("three","3");
	    new MethodTag
		("com.mortbay.HTTP.Filter.TestHarness.test2(null ,1.0,two,three);",
		 named,null).invoke();

	    test.checkEquals(zero,null,"arg zero");
	    test.checkEquals(one.doubleValue(),1.0,"arg 1");
	    test.checkEquals(two,"two","arg 2");
	    test.checkEquals(three,"3","arg 3");
	    test.checkEquals(__called,2,"called");
	    
	}
	catch(Exception e){
	    Code.warning(e);
	    test.check(false,e.toString());
	}	
    }
    
    /* ----------------------------------------------------------------- */
    public static void htmlFilter()
    {
	Test test = new Test("com.mortbay.HTTP.Filter.HtmlFilter");
	try
	{
	    ByteArrayOutputStream bout=new ByteArrayOutputStream();
	    HtmlFilter f=new HtmlFilter(null);
	    f.activate();
	    f.test(bout);

	    f.write("TestingTesting 1 2 3".getBytes());
	    f.flush();
	    test.checkEquals("TestingTesting 1 2 3",bout.toString(),
			     "write");
	    bout.reset();
	    f.write("TestingTesting 1 2 3".getBytes());
	    f.flush();
	    test.checkEquals("TestingTesting 1 2 3",bout.toString(),
			     "write");
	    bout.reset();
	    f.write("Testing<Testing<!Testing".getBytes());
	    f.flush();
	    test.checkEquals("Testing<Testing<!Testing",bout.toString(),
			     "lessThanTag");
	    bout.reset();
	    f.write("Testing<<!=>Testing<!!=>Testing".getBytes());
	    f.flush();
	    test.checkEquals("Testing<<!=>Testing<!!=>Testing",bout.toString(),
			     "repeatChar");
	    bout.reset();
	    f.write("Testing<!=>Testing<!=><!=>Testing".getBytes());
	    f.flush();
	    test.checkEquals("TestingTestingTesting",bout.toString(),
			     "emptyTag");
	    bout.reset();
	    Code.setSuppressWarnings(true);
	    f.write("Testing<!=Blah>Testing".getBytes());
	    f.flush();
	    test.checkEquals("TestingTesting",bout.toString(),
			     "nothingTag");
	    Code.setSuppressWarnings(false);

	    
	    bout.reset();
	    f.write("Testing<!=com.mortbay.HTTP.Filter.TestHarness.test1();>Testing".getBytes());
	    f.flush();
	    test.checkEquals("TestingT3Testing",bout.toString(),
			     "insert");
	    
	    bout.reset();
	    f.write("Testing<!=com.mortbay.HTTP.Filter.TestHarness.test2(null,0,\"a=b\",\"<!=\");>Testing".getBytes());
	    f.flush();
	    test.checkEquals(__called,4,"called");
	    test.checkEquals("TestingTesting",bout.toString(),
			     "called");
	    test.checkEquals("a=b",two,"called with =");
	    test.checkEquals("<!=",three,"almost tag");
	    
	}
	catch(Exception e)
	{
	    Code.warning(e);
	    test.check(false,e.toString());
	}
    }
    
    /* ------------------------------------------------------------ */
    public static void main(String[] args)
    {
	try{
	    methodTag();
	    htmlFilter();
	}
	catch(Exception e)
	{
	    Code.warning(e);
	    new Test("com.mortbay.HTTP.Filter.TestHarness")
		.check(false,e.toString());
	}
	finally
	{
	    Test.report();
	}
    }
};
