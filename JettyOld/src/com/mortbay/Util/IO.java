// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Util;
import com.mortbay.Base.*;
import java.io.*;

/* ======================================================================== */
/** IO Utilities
 */
public class IO extends Thread
{
    /* ------------------------------------------------------------------- */
    public static int bufferSize = 4096;
    /* ------------------------------------------------------------------- */
    private InputStream in=null;
    private OutputStream out=null;
    private Reader read=null;
    private Writer write=null;

    /* ------------------------------------------------------------------- */
    private IO(InputStream in, OutputStream out)
    {
	this.in=in;
	this.out=out;
	start();
    }
    
    /* ------------------------------------------------------------------- */
    private IO(Reader in, Writer out)
    {
	this.read=in;
	this.write=out;
	start();
    }
    

    
    /* ------------------------------------------------------------------- */
    /** Copy Stream in to Stream out until EOF or exception
     * in own thread
     */
    public static void copyThread(InputStream in, OutputStream out)
    {
	new IO(in,out);
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
	new IO(in,out);
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
    public void run()
    {
	try {
	    if (in!=null)
		copy(in,out,-1);
	    else
		copy(read,write,-1);
	}
	catch(IOException e)
	{
	    Code.ignore(e);
	    try{
		out.close();
	    }
	    catch(IOException e2)
	    {
		Code.ignore(e2);
	    }
	}
    }

    
}
