// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// Copyright (c) 1996 Optimus Solutions Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.FTP;

import com.mortbay.Base.*;
import com.mortbay.Util.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class DataPort extends Thread
{

    /* ------------------------------------------------------------------- */
    public static void main(String[] args)
    {
	DataPort dp = new DataPort(null,System.out);

	System.err.println(dp.getFtpPortCommand());
    }

    /* ------------------------------------------------------------------- */
    private static final int timeout = 20000;
    
    /* ------------------------------------------------------------------- */
    private int port=0;
    private InetAddress addr=null;
    private Socket connection=null;
    private InputStream in = null;
    private OutputStream out =null;
    private Ftp ftp=null;
    
    /* ------------------------------------------------------------------- */
    DataPort(Ftp ftp,InputStream in)
    {
	synchronized(this){
	    this.in = in;
	    this.ftp= ftp;
	    start();
	    try{
		wait();
		Code.debug("Listening on "+addr+" "+port);
	    }
	    catch(InterruptedException e){
		Code.fail("Interrupted");
	    }
	}
    }
    
    /* ------------------------------------------------------------------- */
    DataPort(Ftp ftp, OutputStream out)
    {
	synchronized(this){
	    this.out = out;
	    this.ftp= ftp;
	    start();
	    try{
		wait();
		Code.debug("Listening on "+addr+" "+port);
	    }
	    catch(InterruptedException e){
		Code.fail("Interrupted");
	    }
	}
    }
    
    /* ------------------------------------------------------------------- */
    final public void run() 
    {
	try{
	    listen();
	    handle();
	}
	catch(Exception e){
	    if (ftp!=null)
	    {
		Code.debug("DataPort failed",e);
		ftp.transferCompleteNotification(e);
		ftp=null;
	    }
	}
	finally{
	    Code.debug("DataPort Complete");
	    if (connection!=null)
	    {
		try{connection.close();
		}catch(Exception e){Code.debug("Close Exception",e);}
			
		Code.debug("DataPort Closed");
	    }
	    if (ftp!=null)
		ftp.transferCompleteNotification(null);
	}	
    }
    
    /* ------------------------------------------------------------------- */
    /** Close this dataport and cancel any transfer notification
     *
     */
    final public void close() 
    {
	Code.debug("Close DataPort");
	ftp=null;
	stop();
    }
    
    /* ------------------------------------------------------------------- */
    public void listen()
	 throws IOException
    {
	ServerSocket listen=null;

	// open the listen port
	synchronized(this){
	    try{
		listen = new ServerSocket(0);
		port = listen.getLocalPort();
		addr = listen.getInetAddress();
		if (addr==null || addr.getAddress()[0]==0)
		    addr = InetAddress.getLocalHost();
	    }
	    finally{
		notify();
	    }
	}

	// wait for connection
	Code.debug("Waiting for connection...");
	connection = listen.accept();
	Code.debug("Accepted "+connection);
    }
    

    /* ------------------------------------------------------------------- */
    public void handle()
	 throws IOException
    {
	Code.debug("Handling ");

	// Setup streams
	boolean closeOut=false;
	if (out!=null)
	    in=connection.getInputStream();
	else if (in!=null)
	{
	    closeOut=true;
	    out=connection.getOutputStream();
	}

	try{
	    // Copy in to out
	    IO.copy(in,out);
	}
	finally{
	    if (closeOut && out!=null)
	    {
		try{
		    Code.debug("Closing out");
		    out.flush();
		    out.close();
		}
		catch(IOException e){
		    Code.debug("Exception ignored",e);
		}	
	    }
	    if (connection!=null)
	    {
		Code.debug("Closing connection");
		connection.close();
	    }
	}
    }
    
    /* ------------------------------------------------------------------- */
    public int getListenPort()
    {
	return port;
    }
    
    /* ------------------------------------------------------------------- */
    public String getFtpPortCommand()
    {
	byte[] ip = addr.getAddress();

	String portCommand = 
	    "PORT "+
	    (0xff&ip[0])+','+(0xff&ip[1])+','+(0xff&ip[2])+','+(0xff&ip[3])+','+
	    port/256+","+port%256;

	return portCommand;
    }
}



 
