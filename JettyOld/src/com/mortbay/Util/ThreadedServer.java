// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Util;
import com.mortbay.Base.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.InterruptedException;


/* ======================================================================= */
/** Threaded socket server.
 * This class listens at a socket and spawns a new thread for each
 * request that connects.
 * <P>
 * The class is abstract and derived classes must provide the handling
 * for the connections.
 * <P>
 * If the property THREADED_SERVER_LOW_PRIO is set, then the accept thread
 * runs at a lower priority, so that new connections are only accepted if
 * spare CPU is available.
 * <P>
 * The property THREADED_SERVER_MAX_THREADS can be set to limit the
 * number of threads created.
 * <P>
 * Currently these settings effect all threaded servers in the same JVM
 * and cannot be individually altered.
 *
 * @version $Id$
 * @author Greg Wilkins
 */
abstract public class ThreadedServer implements Runnable 
{    
    /* ------------------------------------------------------------------- */
    private static boolean __lowPrio =
	(System.getProperty("THREADED_SERVER_LOW_PRIO")!=null);

    /* ------------------------------------------------------------------- */
    private Thread serverThread = null;
    private InetAddress address = null;
    private int port=0;
    ServerSocket listen = null; 
  
    /* ------------------------------------------------------------------- */
    /* Construct on any free port.
     */
    public ThreadedServer() 
    {}

    /* ------------------------------------------------------------------- */
    /** Construct for specific port
     */
    public ThreadedServer(int port)
	 throws java.io.IOException
    {
	setAddress(null,port);
    }
    
    /* ------------------------------------------------------------------- */
    /** Construct for specific address and port
     */
    public ThreadedServer(InetAddress address, int port) 
	 throws java.io.IOException
    {
	setAddress(address,port);
    }
    
    /* ------------------------------------------------------------------- */
    /** Construct for specific address and port
     */
    public ThreadedServer(InetAddrPort address) 
	 throws java.io.IOException
    {
	setAddress(address.getInetAddress(),address.getPort());
    }
    
    /* ------------------------------------------------------------------- */
    /** Handle new connection
     * This method should be overriden by the derived class to implement
     * the required handling.  It is called by a thread created for it and
     * does not need to return until it has finished it's task
     */
    protected void handleConnection(InputStream in,OutputStream out)
    {
	throw new Error("Either handlerConnection must be overridden");
    }

    /* ------------------------------------------------------------------- */
    /** Handle new connection
     * If access is required to the actual socket, overide this method
     * instead of handleConnection(InputStream in,OutputStream out).
     * The default implementation of this just calls
     * handleConnection(InputStream in,OutputStream out).
     */
    protected  void handleConnection(Socket connection)
    {
	try
	{
	    InputStream in  = connection.getInputStream();
	    OutputStream out = connection.getOutputStream();

	    handleConnection(in,out);

	    out.flush();
	    connection.close();
	    in=null;
	    out=null;
	    connection=null;
	}
	catch ( Exception e ){
	    Code.warning("Connection problem",e);
	}
    }
  
    
    /* ------------------------------------------------------------ */
    /** 
     * @return IP Address
     * @deprecated Use getInetAddress()
     */
    public InetAddress address()
    {
	return address;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return IP Address
     */
    public InetAddress getInetAddress()
    {
	return address;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return port number
     * @deprecated Use getPort()
     */
    public int port()
    {
	return port;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return port number
     */
    public int getPort()
    {
	return port;
    }
    
    /* ------------------------------------------------------------------- */
    public synchronized void setAddress(InetAddress address,
					int port) 
	 throws java.io.IOException
    {
	this.address = address;
	this.port = port;
	if (serverThread!=null && serverThread.isAlive())
	{
	    stop();
	    start();
	}
    }

    /* ------------------------------------------------------------------- */
    /* Start the ThreadedServer listening
     */
    final public void start()
	 throws java.io.IOException
    {
	Code.debug( "Start Listener for " + address + ":" + port );
	
	listen = address==null
	    ? new ServerSocket( port )
	    : new ServerSocket( port, 50, address);
	port = listen.getLocalPort();
	    
	if( serverThread == null )
	{
	    serverThread = new Thread( this );
	    serverThread.start();
	}
    }
  
    /* ------------------------------------------------------------------- */
    final public void stop() 
    {
	Code.debug("Stop listening on "+listen,new Throwable());
	if( serverThread != null ) 
	{
	    serverThread.stop( );
	    serverThread = null;
	}
	if (listen!=null)
	{
	    try{
		listen.close();
	    }
	    catch(IOException e){
		Code.debug("Ignored",e);
	    }
	    listen=null;
	}
    }
    
  
    /* ------------------------------------------------------------------- */
    final public void join() 
    throws java.lang.InterruptedException
    {
	if( serverThread != null )
	    serverThread.join();
    }
  
  
    /* ------------------------------------------------------------------- */
    final public void run( ) 
    {
	Code.debug( "Listener running on " + listen );
	if (__lowPrio)
	{
	    Code.debug( "Listening at lower priority");
	    serverThread.setPriority(serverThread.getPriority()-1);
	}
	if (ConnectionThread.__maxThreads>0)    
	    Code.debug( "Max Threads = " + ConnectionThread.__maxThreads );
	
	// While the thread is running . . .
	try{
	    while( serverThread != null ) 
	    {
		// Accept an incoming connection
		try 
		{
		    Socket connection = listen.accept( );
		    Code.debug( "Connection: ",connection );
		    ConnectionThread.handle(this,connection);
		} 
		catch ( Exception e )
		{
		    Code.warning("Listen problem",e);
		}
	    }
	}
	finally
	{
	    Code.debug("Stopped listening on " + listen);
	}
    }
}

// =======================================================================
class ConnectionThread extends Thread
{
    /* ------------------------------------------------------------ */
    static int __maxThreads =
	Integer.getInteger("THREADED_SERVER_MAX_THREADS",0).intValue();

    /* ------------------------------------------------------------ */
    static final Stack __threads = new Stack();
    static int __nthreads=0;
    
    /* ------------------------------------------------------------ */
    static void handle(ThreadedServer server,Socket connection)
    {
	ConnectionThread c=null;
	synchronized(__threads)
	{
	    try
	    {
		while (c==null)
		{
		    try {
			if (!__threads.empty())
			{
			    // use free thread
			    c=(ConnectionThread)__threads.pop();
			}
			else if(__maxThreads<=0 || __nthreads<__maxThreads)
			{
			    // new thread
			    c=new ConnectionThread(server);
			}
			else
			{
			    // wait for thread
			    __threads.wait();
			}
		    }
		    catch (EmptyStackException e1) {Code.ignore(e1);}
		}
	    }
	    catch (InterruptedException e2) {Code.ignore(e2);}
	}
	c.handle(connection);
    }
    
    /* ------------------------------------------------------------ */
    final ThreadedServer server;
    Socket connection = null;

	/* ------------------------------------------------------------ */
	/** Constructor. 
	 * @param server 
	 */
    ConnectionThread(ThreadedServer server)
    {
	this.server = server;
	start();
    }

    /* ------------------------------------------------------------ */
    synchronized void handle(Socket connection)
    {
	this.connection=connection;
	this.notify();
    }

    /* ------------------------------------------------------------ */
    /** Loop and wait for connections to handle by calling back to
     * the ThreadedServer
     */
    final public void run() 
    {
	try
	{
	    synchronized(__threads)
	    {
		__nthreads++;
	    }
	    synchronized(this)
	    {
		while (true)
		{
		    while (connection==null)
		    {
			Code.debug("Thread ",this," Waiting...");
			wait();
		    }
		    Code.debug("Thread: ",this," Handling ",connection);
		    server.handleConnection(connection);
		    connection=null;
		    synchronized(__threads)
		    {
			__threads.push(this);
			__threads.notify();
		    }
		}
	    }
	}
	catch(InterruptedException e)
	{
	    Code.warning(e);
	}
	catch(Throwable e)
	{
	    Code.warning(e);
	}
	finally
	{
	    synchronized(__threads)
	    {
		__nthreads--;
		__threads.notify();
	    } 
	}
    }
}



 
