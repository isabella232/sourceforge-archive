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


/* ======================================================================= */
/** Threaded socket server.
 * This class listens at a socket and gives the connections received
 * to a pool of Threads
 * <P>
 * The class is abstract and derived classes must provide the handling
 * for the connections.
 * <P>
 * The properties THREADED_SERVER_MIN_THREADS and THREADED_SERVER_MAX_THREADS
 * can be set to control the number of threads created.
 * <P>
 * @version $Id$
 * @author Greg Wilkins
 */
abstract public class ThreadedServer extends ThreadPool
{    
    /* ------------------------------------------------------------------- */
    private InetAddrPort _address = null;    
    ServerSocket _listen = null;
    int _maxReadTimeMs=-1;
    
    /* ------------------------------------------------------------------- */
    /* Construct
     */
    public ThreadedServer() 
    {}

    /* ------------------------------------------------------------------- */
    /** Construct for specific port
     */
    public ThreadedServer(int port)
    {
        setAddress(new InetAddrPort(port));
    }
    
    /* ------------------------------------------------------------------- */
    /** Construct for specific address and port
     */
    public ThreadedServer(InetAddress address, int port) 
    {
        setAddress(new InetAddrPort(address,port));
    }
    
    /* ------------------------------------------------------------------- */
    /** Construct for specific address and port
     */
    public ThreadedServer(String host, int port) 
	throws UnknownHostException
    {
        setAddress(new InetAddrPort(host,port));
    }
    
    /* ------------------------------------------------------------------- */
    /** Construct for specific address and port
     */
    public ThreadedServer(InetAddrPort address) 
    {
        setAddress(address);
    }
    
    /* ------------------------------------------------------------ */
    /** Set the server InetAddress and port.
     * @param address The InetAddress address or null for all interfaces.
     * @param port The port.
     * @exception IOException 
     * @exception InterruptedException 
     */
    public synchronized void setAddress(InetAddress address,int port) 
    {
        setAddress(new InetAddrPort(address,port));
    }
    
    /* ------------------------------------------------------------ */
    /** Set the server InetAddress and port.
     * @param host
     * @param port The port.
     * @exception IOException 
     * @exception InterruptedException 
     */
    public synchronized void setAddress(String host,int port) 
	throws UnknownHostException
    {
        setAddress(new InetAddrPort(host,port));
    }
    
    
    /* ------------------------------------------------------------ */
    /** Set the server InetAddress and port.
     * @param address The Address to listen on, or 0.0.0.0:port for
     * all interfaces.
     * @exception IOException 
     */
    public synchronized void setAddress(InetAddrPort address) 
    {
        _address = address;
        if (isStarted())
        {
            Code.debug( "Restart for ", address );
            destroy();
            start();
        }
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param host 
     */
    public synchronized void setHost(String host)
	throws UnknownHostException
    {
	setAddress(new InetAddrPort(host,_address==null?0:_address.getPort()));
        
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param addr 
     */
    public synchronized void setInetAddress(InetAddress addr)
    {
	setAddress(new InetAddrPort(addr,_address==null?0:_address.getPort()));
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param port 
     */
    public synchronized void setPort(int port)
    {
	setAddress(_address==null?new InetAddrPort(port)
	    :new InetAddrPort(_address.getInetAddress(),port));
    }


    /* ------------------------------------------------------------ */
    /** 
     * @param ms 
     */
    public void setMaxReadTimeMs(int ms)
    {
	_maxReadTimeMs=ms;
    }
    
    
    /* ------------------------------------------------------------ */
    /** 
     * @return IP Address and port in a new Instance of InetAddrPort.
     */
    public InetAddrPort getInetAddrPort()
    {
        return new InetAddrPort(_address);
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return IP Address
     */
    public InetAddress getInetAddress()
    {
        return _address.getInetAddress();
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return port number
     */
    public int getPort()
    {
        return _address.getPort();
    }
    
    /* ------------------------------------------------------------------- */
    /** Handle new connection
     * This method should be overridden by the derived class to implement
     * the required handling.  It is called by a thread created for it and
     * does not need to return until it has finished it's task
     */
    protected void handleConnection(InputStream in,OutputStream out)
    {
        throw new Error("Either handlerConnection must be overridden");
    }

    /* ------------------------------------------------------------------- */
    /** Handle new connection
     * If access is required to the actual socket, override this method
     * instead of handleConnection(InputStream in,OutputStream out).
     * The default implementation of this just calls
     * handleConnection(InputStream in,OutputStream out).
     */
    protected void handleConnection(Socket connection)
    {
        try
        {
            Code.debug("Handle ",connection);
            InputStream in  = connection.getInputStream();
            OutputStream out = connection.getOutputStream();

            handleConnection(in,out);
            out.flush();
            
            in=null;
            out=null;
        }
        catch ( Exception e ){
            Code.warning("Connection problem",e);
        }
        finally
        {
            try {connection.close();}
            catch ( Exception e ){Code.warning("Connection problem",e);}
            connection=null;
        }
    }
    
    /* ------------------------------------------------------------ */
    /** Handle Job.
     * Implementation of ThreadPool.handle(), calls handleConnection.
     * @param job A Connection.
     */
    public final void handle(Object job)
    {
        Socket connection =(Socket)job;
        handleConnection(connection);
    }
    
    
    
    /* ------------------------------------------------------------ */
    /** New server socket.
     * Creates a new servers socket. May be overriden by derived class
     * to create specialist serversockets (eg SSL).
     * @param address Address and port
     * @param acceptQueueSize Accept queue size
     * @return The new ServerSocket
     * @exception java.io.IOException 
     */
    protected ServerSocket newServerSocket(InetAddrPort address,
                                           int acceptQueueSize)
         throws java.io.IOException
    {
        if (address==null)
            return new ServerSocket(0,acceptQueueSize);

        return new ServerSocket(address.getPort(),
                                acceptQueueSize,
                                address.getInetAddress());
    }
    
    /* ------------------------------------------------------------ */
    /** Accept socket connection.
     * May be overriden by derived class
     * to create specialist serversockets (eg SSL).
     * @param serverSocket
     * @return Accepted Socket
     */
    protected Socket acceptSocket(ServerSocket serverSocket)
    {
        try
        {
	    Socket s;
	    synchronized(_listen)
	    {
		s=_listen.accept();
	    }
            if (_maxReadTimeMs>0)
                s.setSoTimeout(_maxReadTimeMs);
            return s;
        }
        catch ( java.net.SocketException e )
        {
            // XXX - this is caught and ignored due strange
            // exception from linux java1.2.v1a
            Code.ignore(e);
        }
        catch(InterruptedIOException e)
        {
            if (Code.verbose(99))
                Code.ignore(e);
        }
        catch(IOException e)
        {
            Code.warning(e);
        }
        return null;
    }
    
        
    /* ------------------------------------------------------------ */
    /** Get a job.
     * Implementation of ThreadPool.getJob that calls acceptSocket
     * @param timeoutMs Time to wait for a Job.  This is ignored as the
     *                  accept timeout has already been set on the server
     *                  socket.
     * @return An accepted connection.
     */
    protected final Object getJob(int timeoutMs)
    {
        return acceptSocket(_listen);
    }
    
    /* ------------------------------------------------------------------- */
    /* Start the ThreadedServer listening
     */
    synchronized public void start()
    {
        if (isStarted())
        {
            Code.warning("Already started on "+_address);
            return;
        }

        try
        {
            _listen=newServerSocket(_address,
                                    getMaxSize()>0?(getMaxSize()+1):50);
            _address=new InetAddrPort(_listen.getInetAddress(),
                                      _listen.getLocalPort());
            
            if (getMaxIdleTimeMs()>0)
                _listen.setSoTimeout((int)getMaxIdleTimeMs());
            
            super.start();
        }
        catch(IOException e)
        {
            Code.warning(e);
            destroy();
            throw new Error(e.toString());
        }        
    }


    /* ------------------------------------------------------------ */
    /** Disabled.
     * This ThreadPool method is not applicable to the ThreadedServer.
     * @param job 
     */
    public final void run(Object job)
    {
        throw new IllegalStateException("Can't run jobs on ThreadedServer");
    }


}





