// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;

import com.mortbay.Base.Code;
import com.mortbay.Base.Test;
import com.mortbay.Util.Test.*;
import java.net.*;

public class TestHarness
{
    /* ------------------------------------------------------------ */
    public static void test()
    {
	Test t = new Test("com.mortbay.Util.DataHelper");
	try{
	    T2 t2 = (com.mortbay.Util.Test.T2)
		DataClass.emptyInstance(com.mortbay.Util.Test.T2.class);

	    t.check(t2!=null,"empty t2 constructed");
	    t.checkEquals(t2.a.length,0,"empty array");
	    t.check(t2.t1!=null,"empty t1 constructed");
	    t.checkEquals(t2.t1.s,"","empty string");
	    t.checkEquals(t2.t1.i,0,"zero int");
	    t.checkEquals(t2.t1.I.intValue(),0,"zero Integer");

	    t2.t1.i=42;
	    t2.t1.s="check";
	    t2.a=new T1[2];
	    t2.a[0]=(com.mortbay.Util.Test.T1)
		DataClass.emptyInstance(com.mortbay.Util.Test.T1.class);

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
    /** main
     */
    public static void main(String[] args)
    {
	test();
	UrlEncoded.test();
	URI.test();
	Test.report();
    }

};
