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
 * This class listens at a socket and gives the connections received
 * to a pool of Threads
 * <P>
 * The class is abstract and derived classes must provide the handling
 * for the connections.
 * <P>
 * If the property THREADED_SERVER_LOW_PRIO is set, then the accept thread
 * runs at a lower priority, so that new connections are only accepted if
 * spare CPU is available.
 * <P>
 * The properties THREADED_SERVER_MIN_THREADS and THREADED_SERVER_MAX_THREADS
 * can be set to control the number of threads created.
 * <P>
 * Currently these settings effect all threaded servers in the same JVM
 * and cannot be individually altered.
 *
 * @version $Id$
 * @author Greg Wilkins
 */
abstract public class ThreadedServer
    extends ThreadPool
    implements Runnable 
{    
    /* ------------------------------------------------------------------- */
    private static boolean __lowPrio =
	(System.getProperty("THREADED_SERVER_LOW_PRIO")!=null);
    
    /* ------------------------------------------------------------ */
    static int __maxThreads =
	Integer.getInteger("THREADED_SERVER_MAX_THREADS",0).intValue();
    
    /* ------------------------------------------------------------ */
    static int __minThreads =
	Integer.getInteger("THREADED_SERVER_MIN_THREADS",0).intValue();

    /* ------------------------------------------------------------------- */
    private Thread serverThread = null;
    private InetAddress address = null;
    private int port=0;
    ServerSocket listen = null;
  
    /* ------------------------------------------------------------------- */
    /* Construct on any free port.
     */
    public ThreadedServer() 
    {
	super(__minThreads,__maxThreads,null,0);
    }
    
    /* ------------------------------------------------------------------- */
    /* Construct on any free port.
     */
    public ThreadedServer(String name) 
    {
	super(__minThreads,__maxThreads,name,0);
    }

    /* ------------------------------------------------------------------- */
    /** Construct for specific port
     */
    public ThreadedServer(int port)
	 throws java.io.IOException
    {
	super(__minThreads,__maxThreads,"Port:"+port,0);
	setAddress(null,port);
    }
    
    /* ------------------------------------------------------------------- */
    /** Construct for specific address and port
     */
    public ThreadedServer(InetAddress address, int port) 
	 throws java.io.IOException
    {
	super(__minThreads,__maxThreads,
	      ((address==null)?"Port:":(address.toString()+":"))+port,0);
	setAddress(address,port);
    }
    
    /* ------------------------------------------------------------------- */
    /** Construct for specific address and port
     */
    public ThreadedServer(InetAddrPort address) 
	 throws java.io.IOException
    {
	super(__minThreads,__maxThreads,address.toString(),0);
	setAddress(address.getInetAddress(),address.getPort());
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param address The address to listen on
     * @param minThreads Minimum number of handler threads.
     * @param maxThreads Maximum number of handler threads.
     * @param maxIdleTime Idle time in Msecs before a handler thread dies.
     * @exception java.io.IOException Problem listening to the socket.
     */
    public ThreadedServer(InetAddrPort address,
			  int minThreads, 
			  int maxThreads,
			  int maxIdleTime) 
	 throws java.io.IOException
    {
	super(minThreads,maxThreads,address.toString(),maxIdleTime);
	setAddress(address.getInetAddress(),address.getPort());
    }
    
    /* ------------------------------------------------------------ */
    /** Handle a job.
     * Implementation of ThreadPool handle method.
     * @param job 
     */
    final protected void handle(Object job)
    {
	Socket s=(Socket)job;
	try
	{
	    handleConnection(s);
	    s.close();
	}
	catch(java.io.IOException e)
	{
	    Code.ignore(e);
	}
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
	    serverThread = new Thread( this, "ThreadedServer" );
	    serverThread.start();
	}
    }
  
    /* ------------------------------------------------------------------- */
    final public void stop() 
    {
	if (Code.debug())
	    Code.debug("Stop listening on ",listen,new Throwable());
	
	if( serverThread != null ) 
	{
	    Thread thread=serverThread;
	    serverThread = null;
	    thread.interrupt( );
	    Thread.yield();
	    if (thread.isAlive())
		thread.stop( );
	}
	if (listen!=null)
	{
	    try
	    {
		listen.close();
	    }
	    catch(IOException e)
	    {
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
	if (getSize()>0)
	{
	    Code.debug( "Min Threads = " + getMinSize() );
	    Code.debug( "Max Threads = " + getMaxSize() );
	}
	
	// While the thread is running . . .
	try{
	    while( serverThread != null ) 
	    {
		// Accept an incoming connection
		try 
		{
		    final Socket connection = listen.accept();
		    Code.debug( "Connection: ",connection );
		    run(connection);
		}
		catch ( Exception e )
		{
		    if (serverThread!=null)
			Code.warning("Listen problem",e);
		    else
			Code.debug(e);
		}
	    }
	}
	finally
	{
	    Code.debug("Stopped listening on " + listen);
	}
    }
}

