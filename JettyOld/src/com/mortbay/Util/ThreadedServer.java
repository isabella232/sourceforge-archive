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
 *
 * The class is abstract and derived classes must provide the handling
 * for the connections.
 *
 * @version $Id$
 * @author Greg Wilkins
 */
abstract public class ThreadedServer implements Runnable 
{
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
	    this.stop();
	}
	catch ( Exception e ){
	    Code.warning("Connection problem",e);
	}
    }
  
    
    /* ------------------------------------------------------------------- */
    public InetAddress address()
    {
	return address;
    }
    
    /* ------------------------------------------------------------------- */
    public int port()
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

	// While the thread is running . . .
	while( serverThread != null ) 
	{
	    // Accept an incoming connection
	    try 
	    {
		Socket connection = listen.accept( );
		Code.debug( "Connection: ",connection );
		ConnectionThread.handle(this,connection);
	    } 
	    catch ( Exception e ){
		Code.warning("Listen problem",e);
	    }
	}
    }
    
    
}

// =======================================================================
class ConnectionThread extends Thread
{
    /* ------------------------------------------------------------ */
    static final Stack __threads = new Stack();

	/* ------------------------------------------------------------ */
    static void handle(ThreadedServer server,Socket connection)
    {
	ConnectionThread c=null;
	try{
	    c=(ConnectionThread)__threads.pop();
	}
	catch(EmptyStackException e)
	{
	    Code.ignore(e);
	    c=new ConnectionThread(server);
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
	    while (true)
	    {
		synchronized(this)
		{
		    while (connection==null)
		    {
			Code.debug("Thread ",this," Waiting...");
			wait();
		    }
		    Code.debug("Thread: ",this," Handling ",connection);
		    server.handleConnection(connection);
		    connection=null;
		    __threads.push(this);
		}
	    }
	}
	catch(InterruptedException e)
	{
	    Code.debug(e);
	}
    }
}



 
