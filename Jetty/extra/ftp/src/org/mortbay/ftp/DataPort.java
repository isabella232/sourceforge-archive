// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// Copyright (c) 1996 Optimus Solutions Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.ftp;

import org.mortbay.util.Code;
import org.mortbay.util.IO;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class DataPort extends Thread
{
    /* ------------------------------------------------------------------- */
    public static void main(String[] args)
    {
        DataPort dp = new DataPort(null,System.out);

        System.err.println(dp.getFtpPortCommand());
    }

    /* ------------------------------------------------------------------- */
    private static final int SOCKET_LISTEN_TIMEOUT = 120000;

    /* ------------------------------------------------------------------- */
    private int port=0;
    private InetAddress addr=null;
    private ServerSocket listen = null;
    private Socket connection=null;
    private InputStream in = null;
    private OutputStream out =null;
    private Ftp ftp=null;
    private boolean terminated = false;
    
    /* ------------------------------------------------------------------- */
    /** Passive Constructor. 
     * @param ftp 
     * @param in 
     */
    DataPort(Ftp ftp,InputStream in)
    {
        super("FtpDataIn");
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
    /** Passive Constructor. 
     * @param ftp 
     * @param out 
     */
    DataPort(Ftp ftp, OutputStream out)
    {
        super("FtpDataOut");
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

    
    /*--------------------------------------------------------------*/
    /** Active Constructor. 
     * @param ftp 
     * @param in 
     */
    DataPort(Ftp ftp,InputStream in, InetAddress addr, int port)
    {
        super("ActiveFtpDataIn");
        synchronized(this){
            this.in  = in;
            this.ftp = ftp;
	    this.addr=addr;
	    this.port=port;
            start();
            try{
                wait();
                Code.debug("Connected to "+addr+" "+port);
            }
            catch(InterruptedException e){
                Code.fail("Interrupted");
            }
        }
    }
    
    /* ------------------------------------------------------------------- */
    /** Active Constructor. 
     * @param ftp 
     * @param out 
     */
    DataPort(Ftp ftp, OutputStream out, InetAddress addr, int port)
    {
        super("ActiveFtpDataOut");
        synchronized(this){
            this.out = out;
            this.ftp = ftp;
	    this.addr=addr;
	    this.port=port;
            start();
            try{
                wait();
                Code.debug("Connected to "+addr+" "+port);
            }
            catch(InterruptedException e){
                Code.fail("Interrupted");
            }
        }
    }
    
    /* ------------------------------------------------------------------- */
    final public void run() 
    {
        terminated = false;
        try
        {
            while (connection == null)
            {
		if (addr==null)
		    listen();
		else
		    connect();

		// XXX Lets not loop here on failure 
		terminated=(connection==null);
		
                if (terminated) 
                    return;
            }        
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
            if (connection!=null)
            {
                try{connection.close();
                }catch(Exception e){Code.debug("Close Exception",e);}
                        
                connection = null;
            }
            if (ftp!=null)
                ftp.transferCompleteNotification(null);
        }       
    }
    
    /* ------------------------------------------------------------------- */
    /** Close this DataPort and cancel any transfer notification
     *
     */
    final public void close() 
    {
        Code.debug("Close DataPort");
        terminated = true;
        if (connection != null)
        {
            try {connection.close();}
            catch (IOException ioe) { Code.ignore(ioe);}
            connection = null;
        }
        if (listen != null)
        {
            try {listen.close();}
            catch (IOException ioe) { Code.ignore(ioe);}
            listen = null;
        }
        if (in != null)
        {
            try {in.close();}
            catch (IOException ioe) { Code.ignore(ioe);}
            in = null;
        }
        if (out != null)
        {
            try {out.close();}
            catch (IOException ioe) { Code.ignore(ioe);}
            out = null;
        }
        ftp=null;
    }
    
    /* ------------------------------------------------------------------- */
    private void listen()
         throws IOException
    {
        listen=null;

        // open the listen port
        synchronized(this)
	{
            try{
                listen = new ServerSocket(0);
                port = listen.getLocalPort();
                addr = listen.getInetAddress();
                if (addr==null || addr.getAddress()[0]==0)
                    addr = ftp.getLocalAddress();
            }
            finally{
                notify();
            }
        }

        if (!terminated)
        {
            // wait for connection
            Code.debug("Waiting for connection... ",listen);
            listen.setSoTimeout( SOCKET_LISTEN_TIMEOUT );
            connection = listen.accept();
            Code.debug("Accepted ",connection);
        }
    }

    /* ------------------------------------------------------------------- */
    private void connect()
         throws IOException
    {
        // open the listen port
        synchronized(this)
	{
            try
	    {
		Code.debug("Making connection: ",addr,":"+port+"...");
		connection = new Socket(addr,port);
		Code.debug("Connected "+connection);
            }
            finally{
                notify();
            }
        }
    }
    

    /* ------------------------------------------------------------------- */
    public void handle()
         throws IOException
    {
        // Setup streams
        if (out!=null)
            in=connection.getInputStream();
        else 
            out=connection.getOutputStream();
        
        try{
            // Copy in to out
            IO.copy(in,out);
        }
        finally{
            if (out!=null)
            {
                try
                {
                    out.flush();
                    out.close();
                }
                catch(IOException e)
                {Code.debug("Exception ignored",e);}       
            }
            if (connection!=null)
                connection.close();
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



 
