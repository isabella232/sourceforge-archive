// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Util;
import com.mortbay.Base.*;
import java.io.*;

/* ======================================================================== */
/** IO Utilities
 * Provides stream handling utilities in
 * singleton Threadpool implementation accessed by static members.
 */
public class IO extends ThreadPool
{
    /* ------------------------------------------------------------------- */
    public static int bufferSize = 8192;
    /* ------------------------------------------------------------------- */
    private static IO __instance=null;
    public static IO instance()
    {
	if (__instance==null)
	{
	    synchronized(com.mortbay.Util.IO.class)
	    {
		if (__instance==null)
		    __instance=new IO();
	    }
	}
	return __instance;
    }
    
    /* ------------------------------------------------------------------- */
    static class Job
    {
	InputStream in;
	OutputStream out;
	Reader read;
	Writer write;

	Job(InputStream in,OutputStream out)
	{
	    this.in=in;
	    this.out=out;
	    this.read=null;
	    this.write=null;
	}
	Job(Reader read,Writer write)
	{
	    this.in=null;
	    this.out=null;
	    this.read=read;
	    this.write=write;
	}
    }
    
    /* ------------------------------------------------------------------- */
    /** Copy Stream in to Stream out until EOF or exception
     * in own thread
     */
    public static void copyThread(InputStream in, OutputStream out)
    {
	try{
	    instance().run(new Job(in,out));
	}
	catch(InterruptedException e)
	{
	    Code.warning(e);
	}
    }
    
    /* ------------------------------------------------------------------- */
    /** Copy Stream in to Stream out until EOF or exception
     */
    public static void copy(InputStream in, OutputStream out)
	 throws IOException
    {
	copy(in,out,-1);
    }
    
    /* ------------------------------------------------------------------- */
    /** Copy Stream in to Stream out until EOF or exception
     * in own thread
     */
    public static void copyThread(Reader in, Writer out)
    {
	try
	{
	    instance().run(new Job(in,out));
	}
	catch(InterruptedException e)
	{
	    Code.warning(e);
	}
    }
    
    /* ------------------------------------------------------------------- */
    /** Copy Reader to Writer out until EOF or exception
     */
    public static void copy(Reader in, Writer out)
	 throws IOException
    {
	copy(in,out,-1);
    }
    
    /* ------------------------------------------------------------------- */
    /** Copy Stream in to Stream for byteCount bytes or until EOF or exception
     */
    public static void copy(InputStream in,
			    OutputStream out,
			    long byteCount)
	 throws IOException
    {
	try{
		
	    byte buffer[] = new byte[bufferSize];
	    int len=bufferSize;

	    if (byteCount>=0)
	    {
		while (byteCount>0)
		{
		    if (byteCount<bufferSize)
			len=in.read(buffer,0,(int)byteCount);
		    else
			len=in.read(buffer,0,bufferSize);		    

		    if (len==-1)
			break;
		    
		    byteCount -= len;
		    out.write(buffer,0,len);
		}
	    }
	    else
	    {
		while (true)
		{
		    len=in.read(buffer,0,bufferSize);
		    if (len==-1)
			break;
		    out.write(buffer,0,len);
		}
	    }
	}
	finally{
	    out.flush();
	}
    }  
    
    /* ------------------------------------------------------------------- */
    /** Copy Reader to Writer for byteCount bytes or until EOF or exception
     */
    public static void copy(Reader in,
			    Writer out,
			    long byteCount)
	 throws IOException
    {
	try{
		
	    char buffer[] = new char[bufferSize];
	    int len=bufferSize;

	    if (byteCount>=0)
	    {
		while (byteCount>0)
		{
		    if (byteCount<bufferSize)
			len=in.read(buffer,0,(int)byteCount);
		    else
			len=in.read(buffer,0,bufferSize);		    

		    if (len==-1)
			break;
		    
		    byteCount -= len;
		    out.write(buffer,0,len);
		}
	    }
	    else
	    {
		while (true)
		{
		    len=in.read(buffer,0,bufferSize);
		    if (len==-1)
			break;
		    out.write(buffer,0,len);
		}
	    }
	}
	finally{
	    out.flush();
	}
    }
    
    /* ------------------------------------------------------------ */
    /** Run copy for copyThread()
     */
    public void handle(Object o)
    {
	Job job=(Job)o;
	try {
	    if (job.in!=null)
		copy(job.in,job.out,-1);
	    else
		copy(job.read,job.write,-1);
	}
	catch(IOException e)
	{
	    Code.ignore(e);
	    try{
		if (job.out!=null)
		    job.out.close();
		if (job.write!=null)
		    job.write.close();
	    }
	    catch(IOException e2)
	    {
		Code.ignore(e2);
	    }
	}
    }
}









