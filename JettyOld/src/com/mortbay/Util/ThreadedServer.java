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
    
    /* ------------------------------------------------------------ */
    static int __maxThreads =
	Integer.getInteger("THREADED_SERVER_MAX_THREADS",0).intValue();

    /* ------------------------------------------------------------------- */
    private Thread serverThread = null;
    private InetAddress address = null;
    private int port=0;
    ServerSocket listen = null;
    ThreadPool _threadPool;
  
    /* ------------------------------------------------------------------- */
    /* Construct on any free port.
     */
    public ThreadedServer() 
    {
	_threadPool=new ThreadPool(__maxThreads);
    }
    
    /* ------------------------------------------------------------------- */
    /* Construct on any free port.
     */
    public ThreadedServer(String name) 
    {
	_threadPool=new ThreadPool(__maxThreads,name);
    }

    /* ------------------------------------------------------------------- */
    /** Construct for specific port
     */
    public ThreadedServer(int port)
	 throws java.io.IOException
    {
	String name="Port:"+port;
	_threadPool=new ThreadPool(__maxThreads,name);
	setAddress(null,port);
    }
    
    /* ------------------------------------------------------------------- */
    /** Construct for specific address and port
     */
    public ThreadedServer(InetAddress address, int port) 
	 throws java.io.IOException
    {
	String name=((address==null)?"Port:":(address.toString()+":"))+port;
	_threadPool=new ThreadPool(__maxThreads,name);
	setAddress(address,port);
    }
    
    /* ------------------------------------------------------------------- */
    /** Construct for specific address and port
     */
    public ThreadedServer(InetAddrPort address) 
	 throws java.io.IOException
    {
	String name=address.toString();
	_threadPool=new ThreadPool(__maxThreads,name);
	setAddress(address.getInetAddress(),address.getPort());
    }
    
    /* ------------------------------------------------------------------- */
    /** Construct for specific address, port and ThreadPool size
     */
    public ThreadedServer(InetAddrPort address,
			  int threadPoolSize) 
	 throws java.io.IOException
    {
	String name=address.toString();
	_threadPool = new ThreadPool(threadPoolSize,name);
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
	if (_threadPool.getSize()>0)    
	    Code.debug( "Max Threads = " + _threadPool.getSize() );
	
	// While the thread is running . . .
	try{
	    while( serverThread != null ) 
	    {
		// Accept an incoming connection
		try 
		{
		    final Socket connection = listen.accept();
		    Code.debug( "Connection: ",connection );
		    Runnable handler = new Runnable()
		    {
			public void run()
			{
			    handleConnection(connection);
			}
		    };
		    _threadPool.run(handler);
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

