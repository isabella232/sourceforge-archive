// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.util;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;


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
    private int _soTimeOut=-1;
    private int _lingerTimeSecs=30;
    
    private transient Acceptor _acceptor=null;  
    private transient ServerSocket _listen = null;
    private transient boolean _running=false;
    
    /* ------------------------------------------------------------------- */
    /* Construct
     */
    public ThreadedServer() 
    {}

    /* ------------------------------------------------------------ */
    /** 
     * @return The ServerSocket
     */
    public ServerSocket getServerSocket()
    {
        return _listen;
    }
    
    /* ------------------------------------------------------------------- */
    /** Construct for specific port.
     */
    public ThreadedServer(int port)
    {
        setInetAddrPort(new InetAddrPort(port));
    }
    
    /* ------------------------------------------------------------------- */
    /** Construct for specific address and port.
     */
    public ThreadedServer(InetAddress address, int port) 
    {
        setInetAddrPort(new InetAddrPort(address,port));
    }
    
    /* ------------------------------------------------------------------- */
    /** Construct for specific address and port.
     */
    public ThreadedServer(String host, int port) 
        throws UnknownHostException
    {
        setInetAddrPort(new InetAddrPort(host,port));
    }
    
    /* ------------------------------------------------------------------- */
    /** Construct for specific address and port.
     */
    public ThreadedServer(InetAddrPort address) 
    {
        setInetAddrPort(address);
    }    
    
    /* ------------------------------------------------------------ */
    /** Set the server InetAddress and port.
     * @param address The Address to listen on, or 0.0.0.0:port for
     * all interfaces.
     */
    public synchronized void setInetAddrPort(InetAddrPort address) 
    {
        if (_address!=null && _address.equals(address))
            return;

        if (isStarted())
            Log.warning(this+" is started");
        
        _address=address;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return IP Address and port in a new Instance of InetAddrPort.
     */
    public InetAddrPort getInetAddrPort()
    {
        if (_address==null)
            return null;
        return new InetAddrPort(_address);
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param host 
     */
    public synchronized void setHost(String host)
        throws UnknownHostException
    {
        if (_address!=null && _address.getHost()!=null && _address.getHost().equals(host))
            return;

        if (isStarted())
            Log.warning(this+" is started");

        if (_address==null)
            _address=new InetAddrPort(host,0);
        else
            _address.setHost(host);
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return Host name
     */
    public String getHost()
    {
        if (_address==null || _address.getInetAddress()==null)
            return null;
        return _address.getHost();
    }

    
    /* ------------------------------------------------------------ */
    /** 
     * @param addr 
     */
    public synchronized void setInetAddress(InetAddress addr)
    {
        if (_address!=null &&
            _address.getInetAddress()!=null &&
            _address.getInetAddress().equals(addr))
            return;

        if (isStarted())
            Log.warning(this+" is started");

        if (_address==null)
            _address=new InetAddrPort(addr,0);
        else
            _address.setInetAddress(addr);
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return IP Address
     */
    public InetAddress getInetAddress()
    {
        if (_address==null)
            return null;
        return _address.getInetAddress();
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param port 
     */
    public synchronized void setPort(int port)
    {
        if (_address!=null && _address.getPort()==port)
            return;
        
        if (isStarted())
            Log.warning(this+" is started");
        
        if (_address==null)
            _address=new InetAddrPort(port);
        else
            _address.setPort(port);
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return port number
     */
    public int getPort()
    {
        if (_address==null)
            return 0;
        return _address.getPort();
    }
    
    /* ------------------------------------------------------------ */
    /** Set Max Read Time.
     * @deprecated. maxIdleTime is used instead.
     */
    public void setMaxReadTimeMs(int ms)
    {
        Code.warning("setMaxReadTimeMs is deprecated. Use setMaxIdleTimeMs()");
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return milliseconds
     */
    public int getMaxReadTimeMs()
    {
        return getMaxIdleTimeMs();
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param sec seconds to linger or -1 to disable linger.
     */
    public void setLingerTimeSecs(int ls)
    {
        _lingerTimeSecs=ls;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return seconds.
     */
    public int getLingerTimeSecs()
    {
        return _lingerTimeSecs;
    }
    
    
    
    /* ------------------------------------------------------------------- */
    /** Handle new connection.
     * This method should be overridden by the derived class to implement
     * the required handling.  It is called by a thread created for it and
     * does not need to return until it has finished it's task
     */
    protected void handleConnection(InputStream in,OutputStream out)
    {
        throw new Error("Either handlerConnection must be overridden");
    }

    /* ------------------------------------------------------------------- */
    /** Handle new connection.
     * If access is required to the actual socket, override this method
     * instead of handleConnection(InputStream in,OutputStream out).
     * The default implementation of this just calls
     * handleConnection(InputStream in,OutputStream out).
     */
    protected void handleConnection(Socket connection)
        throws IOException
    {
        Code.debug("Handle ",connection);
        InputStream in  = connection.getInputStream();
        OutputStream out = connection.getOutputStream();
        
        handleConnection(in,out);
        out.flush();
        
        in=null;
        out=null;
        connection.close();
    }
    
    /* ------------------------------------------------------------ */
    /** Handle Job.
     * Implementation of ThreadPool.handle(), calls handleConnection.
     * @param job A Connection.
     */
    public void handle(Object job)
    {
        Socket socket =(Socket)job;
        try
        {
            handleConnection(socket);
        }
        catch(Exception e){Code.warning("Connection problem",e);}
        finally
        {
            try {socket.close();}
            catch(Exception e){Code.warning("Connection problem",e);}
        }
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
     * @param timeout The time to wait for a connection. Normally
     *                 passed the ThreadPool maxIdleTime.
     * @return Accepted Socket
     */
    protected Socket acceptSocket(ServerSocket serverSocket,
                                  int timeout)
    {
        try
        {
            Socket s=null;
            
            if (_soTimeOut!=timeout)
            {
                _soTimeOut=timeout;
                _listen.setSoTimeout(_soTimeOut);
            }

            if (_listen!=null)
            {
                s=_listen.accept();
                
                try {
                    if (getMaxIdleTimeMs()>=0)
                        s.setSoTimeout(getMaxIdleTimeMs());
                    if (_lingerTimeSecs>=0)
                        s.setSoLinger(true,_lingerTimeSecs);
                    else
                        s.setSoLinger(false,0);
                }
                catch(Exception e){Code.ignore(e);}
            }
            return s;
        }
        catch(java.net.SocketException e)
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
    
    /* ------------------------------------------------------------------- */
    /* Start the ThreadedServer listening
     */
    synchronized public void start()
        throws Exception
    {
        try
        {
            if (isStarted())
                return;
            
            _listen=newServerSocket(_address,
                                    (getMaxThreads()>0?(getMaxThreads()+1):50));
            if (_address==null)
                _address=new InetAddrPort(_listen.getInetAddress(),_listen.getLocalPort());
            else
            {
                if(_address.getInetAddress()==null)
                    _address.setInetAddress(_listen.getInetAddress());
                if(_address.getPort()==0)
                    _address.setPort(_listen.getLocalPort());
            }
            
            _soTimeOut=getMaxIdleTimeMs();
            if (_soTimeOut>=0)
                _listen.setSoTimeout(_soTimeOut);

            _running=true;
            _acceptor=new Acceptor();
            _acceptor.setDaemon(isDaemon());
            _acceptor.start();
            
            super.start();
        }
        catch(Exception e)
        {
            Code.warning("Failed to start: "+this);
            throw e;
        }
    }
    

    /* --------------------------------------------------------------- */
    public void stop()
        throws InterruptedException
    {
        synchronized(this)
        {
            // Signal that we are stopping
            _running=false;
            
            // Close the listener socket.
            Code.debug("closing ",_listen);
            try {if (_listen!=null)_listen.close();}catch(IOException e){Code.warning(e);}
            
            // Do we have an acceptor thread (running or not)
            Thread.yield();
            if (_acceptor!=null)
            {
                // Tell the acceptor to exit and wake it up
                _acceptor.interrupt();
                wait(getMaxIdleTimeMs());
                
                // Do we still have an acceptor thread? It is playing hard to stop!
                // Try forcing the stop to be noticed by making a connection to self.
                if (_acceptor!=null)
                {
                    _acceptor.forceStop();     
                    // Assume that worked and go on as if it did.
                    _acceptor=null;
                }
            }
        }

        // Stop the thread pool
        try{super.stop();}catch(Exception e){Code.warning(e);}

        // Clean up
        _listen=null;
        _acceptor=null;
    }
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /** Kill a job.
     * This method closes IDLE and socket associated with a job
     * @param thread 
     * @param job 
     */
    protected void stopJob(Thread thread,Object job)
    {
         if (job instanceof Socket)
         {
             try{((Socket)job).close();}
             catch(Exception e){Code.ignore(e);}
         }
         super.stopJob(thread,job);
    }

    
    /* ------------------------------------------------------------ */
    public String toString()
    {
        if (_address==null)    
            return getName()+"@0.0.0.0:0";
        if (_listen!=null)
            return getName()+
                "@"+_listen.getInetAddress().getHostAddress()+
                ":"+_listen.getLocalPort();
        return getName()+"@"+getInetAddrPort();
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class Acceptor extends Thread
    {
        /* ------------------------------------------------------------ */
        public void run()
        {
            ThreadedServer threadedServer = ThreadedServer.this;
            Thread acceptor = Thread.currentThread();
            int priority=Thread.currentThread().getPriority();
            try
            {
                this.setName("Acceptor "+_listen);
                while(_running)
                {
                    try
                    {
                        // Accept a socket
                        Socket socket=acceptSocket(_listen,_soTimeOut);

                        // Handle the socket
                        if (_running)
                        {
                            if (socket==null)
                                threadedServer.shrink();
                            else
                                threadedServer.run(socket);
                        }
                        else if (socket!=null)
                            socket.close();
                    }
                    catch(InterruptedException e)
                    {}
                    catch(Exception e)
                    {
                        if (_running)
                            Code.warning(e);
                        else
                            Code.debug(e);
                    }
                    catch(Error e)
                    {
                        Code.warning(e);
                        break;
                    }  
                }
            }
            finally
            {
                if (_running)
                    Code.warning("Stopping "+this.getName());
                else
                    Log.event("Stopping "+this.getName());
                synchronized(threadedServer)
                {
                    _acceptor=null;
                    threadedServer.notifyAll();
                }
            }
        }

        /* ------------------------------------------------------------ */
        void forceStop()
        {
            if(_listen!=null && _address!=null)
            {
		InetAddress addr=_address.getInetAddress();
                try{
                    if (addr==null || addr.toString().startsWith("0.0.0.0"))
                        addr=InetAddress.getByName("127.0.0.1");
                    Code.debug("Self connect to close listener ",addr,
                               ":"+_address.getPort());
                    Socket socket = new
                        Socket(addr,_address.getPort());
                    Thread.yield();
                    socket.close();
                    Thread.yield();
                }
                catch(IOException e)
                {
                    Code.debug("problem stopping acceptor ",addr,": ",e);
                }
            }
        }
    }
}
