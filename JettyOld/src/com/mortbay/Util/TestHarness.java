// ========================================================================
// Copyright (c) 1997 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;

import com.mortbay.Base.Code;
import com.mortbay.Base.Test;
import com.mortbay.Util.Test.*;
import java.io.*;
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
