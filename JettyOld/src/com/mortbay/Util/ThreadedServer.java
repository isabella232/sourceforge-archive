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
abstract public class ThreadedServer
    implements Runnable 
{    
    /* ------------------------------------------------------------ */
    private static int _threadId=0;
    
    /* ------------------------------------------------------------ */
    static int __maxThreads =
        Integer.getInteger("THREADED_SERVER_MAX_THREADS",255).intValue();
    
    /* ------------------------------------------------------------ */
    static int __minThreads =
        Integer.getInteger("THREADED_SERVER_MIN_THREADS",1).intValue();

    /* ------------------------------------------------------------ */
    String __threadClass =
        System.getProperty("THREADED_SERVER_THREAD_CLASS",
                           "java.lang.Thread");
    
    /* ------------------------------------------------------------------- */
    private InetAddrPort address = null;    
    ServerSocket listen = null;

    private Hashtable _threadSet;
    private int _maxThreads = __maxThreads;
    private int _minThreads = __minThreads;
    private int _maxIdleTimeMs=0;
    private String _name="ThreadedServer";
    private int _accepting=0;
    private boolean _running=false;
    
    /**
     *  P.Mclachlan: Allow the class of thread used
     *  to change (to any subclass of 'java.lang.Thread').  This
     *  can be useful in certain embedded webserver circumstances.
     */
    private Class _threadClass;
    private Constructor _constructThread;
    private Object[] _constructThis;

    /* ------------------------------------------------------------------- */
    /* Construct
     */
    public ThreadedServer() 
    {
        try
        {
            _threadClass = Class.forName( __threadClass );
            Code.debug("Using thread class '", _threadClass.getName(),"'");
        }
        catch( Exception e )
        {
            Code.warning( "Invalid thread class (ignored) ",e );
            _threadClass = java.lang.Thread.class;
        }

        setThreadClass(_threadClass);
    }
    
    /* ------------------------------------------------------------------- */
    /* Construct
     */
    public ThreadedServer(String name) 
    {
        this();
        _name=name;
    }

    /* ------------------------------------------------------------------- */
    /** Construct for specific port
     */
    public ThreadedServer(int port)
         throws java.io.IOException
    {
        this();
        setAddress(new InetAddrPort(null,port));
    }
    
    /* ------------------------------------------------------------------- */
    /** Construct for specific address and port
     */
    public ThreadedServer(InetAddress address, int port) 
         throws java.io.IOException
    {
        this();
        setAddress(new InetAddrPort(address,port));
    }
    
    /* ------------------------------------------------------------------- */
    /** Construct for specific address and port
     */
    public ThreadedServer(InetAddrPort address) 
         throws java.io.IOException
    {
        this();
        setAddress(address);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param address The address to listen on
     * @param minThreads Minimum number of handler threads.
     * @param maxThreads Maximum number of handler threads.
     * @param maxIdleTime Idle time in milliseconds before a handler thread dies.
     * @exception java.io.IOException Problem listening to the socket.
     */
    public ThreadedServer(InetAddrPort address,
                          int minThreads, 
                          int maxThreads,
                          int maxIdleTime) 
         throws java.io.IOException
    {
        this();
        _minThreads=minThreads==0?1:minThreads;
        _maxThreads=maxThreads==0?__maxThreads:maxThreads;
        _maxIdleTimeMs=maxIdleTime;
        setAddress(address);
    }
    
    /* ------------------------------------------------------------ */
    /** Set the Thread class.
     * Sets the class used for threads in the thread pool. The class
     * must have a constractor taking a Runnable.
     * @param threadClass 
     */
    public void setThreadClass(Class threadClass) 
    {
        _threadClass=threadClass;
                
        if( _threadClass == null || !Thread.class.isAssignableFrom( _threadClass ) )
        {
            Code.warning( "Invalid thread class (ignored) "+
                          _threadClass.getName() );
            _threadClass = java.lang.Thread.class;
        }

        try
        {
            Class[] args ={java.lang.Runnable.class};
            _constructThread = _threadClass.getConstructor(args);
        }
        catch(Exception e)
        {
            Code.warning("Invalid thread class (ignored)",e);
            setThreadClass(java.lang.Thread.class);
        }

        Object[] args = {this};
        _constructThis=args;
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
    protected  void handleConnection(Socket connection)
    {
        try
        {
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
    /** 
     * @return IP Address and port
     */
    public InetAddrPort getInetAddrPort()
    {
        return address;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return IP Address
     * @deprecated Use getInetAddress()
     */
    public InetAddress address()
    {
        return address.getInetAddress();
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return IP Address
     */
    public InetAddress getInetAddress()
    {
        return address.getInetAddress();
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return port number
     * @deprecated Use getPort()
     */
    public int port()
    {
        return address.getPort();
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return port number
     */
    public int getPort()
    {
        return address.getPort();
    }

    /* ------------------------------------------------------------ */
    public int getSize()
    {
        return _threadSet.size();
    }
    
    /* ------------------------------------------------------------ */
    public int getMinSize()
    {
        return _minThreads;
    }
    
    /* ------------------------------------------------------------ */
    public int getMaxSize()
    {
        return _maxThreads;
    }
    
    /* ------------------------------------------------------------------- */
    public synchronized void setAddress(InetAddress address,
                                        int port) 
         throws java.io.IOException
    {
        setAddress(new InetAddrPort(address,port));
    }
    
    
    /* ------------------------------------------------------------------- */
    public synchronized void setAddress(InetAddrPort address) 
         throws java.io.IOException
    {
        this.address = address;
        if (_threadSet!=null && _running)
        {
            Code.debug( "Restart for ", address );
            stop();
            start();
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
     * @return Accepted Socket
     * @exception java.io.IOException 
     */
    protected Socket accept(ServerSocket serverSocket)
         throws java.io.IOException
    {
        Socket socket = serverSocket.accept();
        if (_maxIdleTimeMs>0)
            socket.setSoTimeout(_maxIdleTimeMs);
        return socket;
    }
    
    
    /* ------------------------------------------------------------------- */
    /* Start the ThreadedServer listening
     */
    synchronized public void start()
         throws java.io.IOException
    {
        if (listen!=null)
        {
            Code.debug("Already started on ",address);
            return;
        }
        
        Code.debug( "Start Listener for ", address );

        listen=newServerSocket(address,_maxThreads>0?(_maxThreads+1):__maxThreads);
        address=new InetAddrPort(listen.getInetAddress(),
                                 listen.getLocalPort());
        
        // Set any idle timeout
        if (_maxIdleTimeMs>0)
            listen.setSoTimeout(_maxIdleTimeMs);
        _accepting=0;

        // Start the threads
        _running=true;
        _threadSet=new Hashtable(_maxThreads+_maxThreads/2+13);
        for (int i=0;i<_minThreads;i++)
            newThread();
    }

    /* ------------------------------------------------------------------- */
    private synchronized void newThread()
    {
        try
        {
            Thread thread=
                (Thread)_constructThread.newInstance(_constructThis);

            synchronized(com.mortbay.Util.ThreadedServer.class)
            {
                thread.setName(_name+"-"+(_threadId++));
            }
            
            _threadSet.put(thread,thread);
            thread.start();
        }
        catch( java.lang.reflect.InvocationTargetException e )
        {
            Code.fail(e);
        }
        catch( IllegalAccessException e )
        {
            Code.fail(e);
        }
        catch( InstantiationException e )
        {
            Code.fail(e);
        }
    }
    
    /* ------------------------------------------------------------------- */
    synchronized public void stop() 
    {
        Code.debug("Stop listening on ",listen);

        if (_threadSet==null)
            return;
        
        _running=false;
        
        // Close the port
        if (listen!=null)
        {
            try
            {
                listen.close();
            }
            catch(IOException e)
            {
                Code.ignore(e);
            }
        }
        
        // interrupt the threads
        Enumeration enum=_threadSet.keys();
        while(enum.hasMoreElements())
        {
            Thread thread=(Thread)enum.nextElement();
            thread.interrupt();
        }

        
        // wait a while for all threads to die
        try{
            long end_wait=System.currentTimeMillis()+5000;
            while (_threadSet.size()>0 && end_wait>System.currentTimeMillis())
                wait(5000);

            // Stop any still running
            if (_threadSet.size()>0)
            {
                enum=_threadSet.keys();
                while(enum.hasMoreElements())
                {
                    Thread thread=(Thread)enum.nextElement();
                    if (thread.isAlive())
                        thread.stop( );
                }
                
                // wait until all threads are dead.
                while(_threadSet.size()>0)
                {
                    Code.debug("waiting for threads to stop...");
                    wait(5000);
                }
            }
        }
        catch(InterruptedException e)
        {
            Code.warning(e);
        }
        
        _threadSet.clear();
        _threadSet=null;
        listen=null;
    }
    
  
    /* ------------------------------------------------------------------- */
    final public void join() 
        throws java.lang.InterruptedException
    {
        while(_threadSet!=null && _threadSet.size()>0)
        {
            Thread thread=null;
            synchronized(this)
            {
                Enumeration enum=_threadSet.keys();
                if(enum.hasMoreElements())
                    thread=(Thread)enum.nextElement();
            }
            if (thread!=null)
                thread.join();
        }
    }
  
  
    /* ------------------------------------------------------------------- */
    final public void run( ) 
    {
        Thread thread=Thread.currentThread();
        String name=thread.getName();
        int runs=0;
        
        Code.debug( "Listen on ", listen );
        try
        {
            while(_running) 
            {
                Socket connection=null;
                // Accept an incoming connection
                try 
                {
                    // increment accepting count
                    synchronized(this){_accepting++;}               
                    
                    // wait for a connection
                    connection=accept(listen);
                }
                catch ( InterruptedIOException e )
                {
                    if (Code.verbose(99))
                        Code.debug(e);
                    
                    synchronized(this)
                    {
                        // If we are still running, interrupt was due to accept timeout
                        if (Code.verbose(99))
                            Code.debug("Threads="+_threadSet.size());
                        if (_running && _threadSet.size()>_minThreads)
                        {
                            // Kill thread if it is in excess of the minimum.
                            Code.debug("Idle death: "+thread);
                            _threadSet.remove(thread);
                            break;
                        }
                    }
                }
                catch ( java.net.SocketException e )
                {
                    // XXX - this is caught and ignored due strange
                    // exception from linux java1.2.v1a
                    Code.ignore(e);
                }
                catch ( IOException e )
                {
                    Code.ignore(e);
                }
                finally
                {
                    // If not more threads accepting and this
                    // thread is not idle - start a new thread.
                    synchronized(this)
                    {
                        if (Code.verbose(99))
                            Code.debug("Threads="+_threadSet.size());
                        
                        if (--_accepting==0 &&
                            _running &&
                            connection!=null &&
                            _threadSet.size()<_maxThreads)
                        {
                            Code.debug("New Thread");
                            newThread();
                        }
                    }
                }

                // handle the connection
                if (connection!=null)
                {
                    try
                    {
                        if (Code.debug())
                        {
                            thread.setName(name+"/"+runs++);
                            if (Code.verbose())
                                Code.debug("Handling ",connection);
                        }
                        handleConnection(connection);
                        connection.close();
                    }
                    catch ( Exception e )
                    {
                        Code.warning(e);
                    }
                    finally
                    {
                        connection=null;
                    }
                }
            }
        }
        finally
        {
            synchronized(this)
            {
                if (_threadSet!=null)
                    _threadSet.remove(thread);
                notify();
            }
            Code.debug("Stopped listening on " + listen+
                       "\nthreads="+_threadSet.size());
        }
    }
}








