// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP.Handler;

import com.mortbay.Base.*;

public class TestHarness
{
    static void testServletLoader()
    {
	Test test = new Test("com.mortbay.HTTP.Handler.ServletLoader");
	try{
	    ServletLoader loader = new FileJarServletLoader("../../../../../lib/gnujsp.jar:../../../../../src");

	    Class c;
	    c=loader.loadClass("java.lang.String",true);
	    test.checkEquals(c,java.lang.String.class,"System class");
	    c=loader.loadClass("org.gjt.jsp.JSPClassLoader",true);
	    try {
		c=null;
		c=loader.loadClass("other.path.fubar",true);
	    }
	    catch(java.lang.ClassNotFoundException e) {}
	    test.check(c==null,"class not found");
	}
	catch(Throwable th)
	{
	    test.check(false,th.toString());
	}
    }
    
    public static void main(String[] args)
    {
	try{
	    testServletLoader();
	}
	finally
	{
	    Test.report();
	}    
    }
    
};
