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
    private ServerSocket _listen = null;
    private int _soTimeOut=-1;
    private int _maxReadTimeMs=0;
    private int _lingerTimeSecs=30;
    private Acceptor _acceptor=null;
    
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
        setAddress(new InetAddrPort(port));
    }
    
    /* ------------------------------------------------------------------- */
    /** Construct for specific address and port.
     */
    public ThreadedServer(InetAddress address, int port) 
    {
        setAddress(new InetAddrPort(address,port));
    }
    
    /* ------------------------------------------------------------------- */
    /** Construct for specific address and port.
     */
    public ThreadedServer(String host, int port) 
        throws UnknownHostException
    {
        setAddress(new InetAddrPort(host,port));
    }
    
    /* ------------------------------------------------------------------- */
    /** Construct for specific address and port.
     */
    public ThreadedServer(InetAddrPort address) 
    {
        setAddress(address);
    }
    
    
    /* ------------------------------------------------------------ */
    /** Set the server InetAddress and port.
     * @param address The Address to listen on, or 0.0.0.0:port for
     * all interfaces.
     */
    public synchronized void setAddress(InetAddrPort address) 
    {
        if (_address==null)
            _address=address;
        else if (isStarted())
        {
            if (!_address.equals(address))
                throw new IllegalStateException(this+ " is started");
        }
        else
            _address=address;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param host 
     */
    public synchronized void setHost(String host)
        throws UnknownHostException
    {
        if (_address==null)
            _address=new InetAddrPort(host,0);
        else if (isStarted())
        {
            if (!_address.getHost().equals(host))
                throw new IllegalStateException(this+ " is started");
        }
        else
            _address.setHost(host);
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param addr 
     */
    public synchronized void setInetAddress(InetAddress addr)
    {
        if (_address==null)
            _address=new InetAddrPort(addr,0);
        else if (isStarted())
        {
            if (!_address.getInetAddress().equals(addr))
                throw new IllegalStateException(this+ " is started");
        }
        else
            _address.setInetAddress(addr);
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param port 
     */
    public synchronized void setPort(int port)
    {
        if (_address==null)
            _address=new InetAddrPort(port);
        else if (isStarted())
        {
            if (_address.getPort()!=port)
                throw new IllegalStateException(this+ " is started");
        }
        else
            _address.setPort(port);
    }

    /* ------------------------------------------------------------ */
    /** Set Max Read Time.
     * Setting this to a none zero value results in setSoTimeout being
     * called for all accepted sockets.  This causes an
     * InterruptedIOException if a read blocks for this period of time.
     * @param ms Max read time in ms or 0 for no limit.
     */
    public void setMaxReadTimeMs(int ms)
    {
        _maxReadTimeMs=ms;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return milliseconds
     */
    public int getMaxReadTimeMs()
    {
        return _maxReadTimeMs;
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
     * @return port number
     */
    public int getPort()
    {
        if (_address==null)
            return 0;
        return _address.getPort();
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
            Socket s;
            
            if (_soTimeOut!=timeout)
            {
                _soTimeOut=timeout;
                _listen.setSoTimeout(_soTimeOut);
            }
            
            s=_listen.accept();
            
            try {
                if (_maxReadTimeMs>=0)
                    s.setSoTimeout(_maxReadTimeMs);
  		if (_lingerTimeSecs>=0)
  		    s.setSoLinger(true,_lingerTimeSecs);
  		else
  		    s.setSoLinger(false,0);
  	    }
            catch(Exception e){Code.ignore(e);}
            
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
        if (isStarted())
        {
            Code.warning("Already started on "+_address);
            return;
        }

        _listen=newServerSocket(_address,
                                getMaxThreads()>0?(getMaxThreads()+1):50);
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

        _acceptor=new Acceptor();
        _acceptor.start();
        
        super.start();
    }

    /* --------------------------------------------------------------- */
    public void stop()
        throws InterruptedException
    {
        if (_acceptor!=null)
        {
            _acceptor._running=false;
            _acceptor.interrupt();
            Thread.yield();
        }

        synchronized(this)
        {
            if (_acceptor!=null)
                _acceptor.forceStop();
            _acceptor=null;
        }

        try{
            super.stop();
        }
        catch(Exception e)
        {
            Code.warning(e);
        }
        
        _listen=null;
        _acceptor=null;
    }
    
    /* --------------------------------------------------------------- */
    synchronized public void destroy()
    {
        if (_acceptor!=null)
        {
            _acceptor._running=false;
            _acceptor.interrupt();
            Thread.yield();
        }

        synchronized(this)
        {
            if (_acceptor!=null)
                _acceptor.forceStop();
            _acceptor=null;
        }

        try{
            super.destroy();
            
            synchronized(this)
            {
                if (_listen!=null)
                    _listen.close();
            }
        }
        catch(Exception e)
        {
            Code.warning(e);
        }
        
        _listen=null;
        _acceptor=null;
    }


    /* ------------------------------------------------------------ */
    public String toString()
    {
        if (_address==null)    
            return getName()+"@0.0.0.0:0";
        return getName()+"@"+getInetAddrPort();
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class Acceptor extends Thread
    {
        boolean _running=false;
        public void run()
        {
            try
            {
                this.setName("Acceptor "+_listen);
                _running=true;
                while(_running)
                {
                    try
                    {
                        Socket socket=acceptSocket(_listen,_soTimeOut);
                        if (socket!=null)
                        {
                            if (_running)
                                ThreadedServer.this.run(socket);
                            else
                                socket.close();
                        }
                    }
                    catch(Exception e)
                    {
                        Code.warning(e);
                    }
                }
            }
            finally
            {
                Log.event("Stopping "+this.getName());
                try{if (_listen!=null)_listen.close();}
                catch (IOException e) {Code.ignore(e);}
                synchronized(ThreadedServer.this)
                {
                    _listen=null;
                    _acceptor=null;
                }
            }
        }

        void forceStop()
        {
            if(_listen!=null && _address!=null)
            {
                try{
                    InetAddress addr=_address.getInetAddress();
                    if (addr==null || addr.toString().startsWith("0.0.0.0"))
                        addr=InetAddress.getLocalHost();
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
                    Code.warning("problem stopping acceptor "+_listen,e);
                }
            }
        }
    }
    
}
