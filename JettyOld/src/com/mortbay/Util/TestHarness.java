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
    public static void testDataHelper()
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
    public static void testBlockingQueue()
	throws Exception
    {
	Test t = new Test("com.mortbay.Util.BlockingQueue");

	final BlockingQueue bq=new BlockingQueue();
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
			   try{Thread.sleep(1000);}
			   catch(InterruptedException e){}
			   bq.put("F");
		       }
		   }
		   ).start();  
	
	t.checkEquals(bq.get(),"F","F");
	t.checkEquals(bq.get(100),null,"null");
    }
    
    /* ------------------------------------------------------------ */
    /** main
     */
    public static void main(String[] args)
    {
	try{
	    testDataHelper();
	    testBlockingQueue();
	    UrlEncoded.test();
	    URI.test();
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
