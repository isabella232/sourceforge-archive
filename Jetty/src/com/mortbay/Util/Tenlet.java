// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Util;
import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.InterruptedException;
import java.lang.reflect.Constructor;

/* ------------------------------------------------------------ */
/** Reverse Telnet class
 * 
 * @see
 * @version 1.0 Sun Apr 23 2000
 * @author Greg Wilkins (gregw)
 */
public class Tenlet extends ThreadedServer 
{
    /* ------------------------------------------------------------ */
    public Tenlet(InetAddrPort addr)
	throws IOException
    {
	super("Tenlet",addr);
    }
    
    /* ------------------------------------------------------------ */
    protected void handleConnection(InputStream in,OutputStream out)
    {
	try
	{
	    IO.copyThread(System.in,out);
	    IO.copy(in,System.err);
	}
	catch(IOException e)
	{
	    Code.warning(e);
	}
	finally
	{
	    System.exit(0);
	}
    }
    
    
    /* ------------------------------------------------------------ */
    public static void main(String[] arg)
    {
	try
	{    
	    if (arg.length!=1)
	    {
		System.err.println("Usage - java com.mortbay.Util.Tenlet [addr]:port");
		System.exit(1);
	    }
	    
	    Tenlet tenlet=new Tenlet(new InetAddrPort(arg[0]));
	    tenlet.start();
	    tenlet.join();
	}
	catch(Exception e)
	{
	    Code.warning(e);
	}
    }
}
