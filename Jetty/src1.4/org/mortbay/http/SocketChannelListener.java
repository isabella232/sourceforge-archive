// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http;
import org.mortbay.http.SocketListener;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpConnection;
import org.mortbay.http.ChunkableInputStream;
import org.mortbay.util.Code;
import org.mortbay.util.InetAddrPort;
import org.mortbay.util.Log;
import org.mortbay.util.ThreadPool;
import org.mortbay.util.ChannelStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.Channels;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Iterator;

/* ------------------------------------------------------------ */
/** Non blocking Listener.
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class SocketChannelListener
    extends ThreadPool
    implements HttpListener
{
    private InetSocketAddress _address;
    private String _scheme=HttpMessage.__SCHEME;
    private int _maxReadTimeMs=0;
    private int _lingerTimeSecs=30;
    private String _integralScheme=HttpMessage.__SSL_SCHEME;
    private String _confidentialScheme=HttpMessage.__SSL_SCHEME;
    private int _integralPort=0;
    private int _confidentialPort=0;
    private int _bufferSize=4096;
    private int _bufferReserve=512;
    
    private transient HttpServer _server;
    private transient boolean _lastLow=false;
    private transient boolean _lastOut=false;
    private transient ServerSocketChannel _acceptChannel;
    private transient Selector _selector;
    private transient SelectorThread _selectorThread;
    private transient ArrayList _idling=new ArrayList();

    /* ------------------------------------------------------------------- */
    public SocketChannelListener()
        throws IOException
    {
        Code.warning("SocketChannelListener is experimental");
    }

    /* ------------------------------------------------------------ */
    public void setHttpServer(HttpServer server)
    {
        Code.assertTrue(_server==null || _server==server,
                        "Cannot share listeners");
        _server=server;
    }

    /* ------------------------------------------------------------ */
    public HttpServer getHttpServer()
    {
        return _server;
    }

    /* ------------------------------------------------------------ */
    public int getBufferSize()
    {
        return _bufferSize;
    }
    
    /* ------------------------------------------------------------ */
    public void setBufferSize(int size)
    {
        _bufferSize=size;
    }
       
    /* ------------------------------------------------------------ */
    public int getBufferReserve()
    {
        return _bufferReserve;
    }
    
    /* ------------------------------------------------------------ */
    public void setBufferReserve(int size)
    {
        _bufferReserve=size;
    }
         
    /* --------------------------------------------------------------- */
    public void setDefaultScheme(String scheme)
    {
        _scheme=scheme;
    }
    
    /* --------------------------------------------------------------- */
    public String getDefaultScheme()
    {
        return _scheme;
    }

    /* ------------------------------------------------------------ */
    public synchronized void setHost(String host)
        throws UnknownHostException
    {
        if (isStarted())
        {
            if (!_address.getHostName().equals(host))
                throw new IllegalStateException(this+ " is started");
        }
        else
            _address= new InetSocketAddress(host,_address==null?0:_address.getPort());
    }

    /* ------------------------------------------------------------ */
    public synchronized void setPort(int port)
    {
        if (isStarted())
        {
            if (_address.getPort()!=port)
                throw new IllegalStateException(this+ " is started");
        }
        else if (_address==null || _address.getHostName()==null)
            _address= new InetSocketAddress(port);
        else
            _address= new InetSocketAddress(_address.getHostName(),port);
    }
    
    
    /* ------------------------------------------------------------ */
    /** 
     * @return Host name
     */
    public String getHost()
    {
        if (_address==null || _address.getAddress()==null)
            return null;
        return _address.getHostName();
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
    /** 
     * @return The InetSocketAddress of the listener
     */
    public InetSocketAddress getInetSocketAddress()
    {
        return _address;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param address The InetSocketAddress of the listener
     */
    public void setInetSocketAddress(InetSocketAddress address)
    {
        _address=address;
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
    public void start()
        throws Exception
    {
        if (isStarted())
            throw new IllegalStateException("Started");
        
	// Create a new server socket and set to non blocking mode
	_acceptChannel=ServerSocketChannel.open();
	_acceptChannel.configureBlocking(false);
        
	// Bind the server socket to the local host and port
	_acceptChannel.socket().bind(_address);

        // create a selector;
        _selector=Selector.open();
        
	// Register accepts on the server socket with the selector.
        _acceptChannel.register(_selector,SelectionKey.OP_ACCEPT);

        // Start selector thread
        _selectorThread=new SelectorThread();
        _selectorThread.start();
            
        // Start the thread Pool
        super.start();
        Log.event("Started SocketChannelListener on "+getInetSocketAddress());
    }
    
    /* ------------------------------------------------------------ */
    public void stop()
        throws InterruptedException
    {
        if (_selectorThread!=null)
        {
            _selectorThread._running=false;
            _selector.wakeup();
            Thread.yield();
        }
        synchronized(this)
        {
            if (_selectorThread!=null)
                _selectorThread.forceStop();
        }
        
        super.stop();
        Log.event("Stopped SocketChannelListener on "+getInetSocketAddress());
    }

    /* ------------------------------------------------------------ */
    public void handle(Object job)
        throws InterruptedException
    {
        Code.debug("handle "+job);
        
        if (job instanceof Connection)
        {
            Connection connection =(Connection)job;
            try
            {
                while (connection.handleNext())
                {
                    ChunkableInputStream cin=connection.getInputStream();
                    if (cin.available()<=0)
                    {
                        Thread.yield();
                        if (cin.available()<=0)
                        {
                            Code.debug("Idling ",connection);
                            synchronized(_idling)
                            {
                                _idling.add(connection);
                            }
                            _selector.wakeup();
                            break;
                        }
                    }
                }
            }
            catch(Exception e)
            {
                Code.warning(e);
            }
        }
        else
            Code.warning("Don't know why I'm here!");
        
    }

    /* ------------------------------------------------------------ */
    protected void stopJob(Thread thread, Object job)
    {
        try
        {
            ((Connection)job).close();
        }
        catch(IOException e)
        {
            Code.warning(e);
        }
    }
    
    /* ------------------------------------------------------------ */
    /** Customize request from socket.
     * @param request
     */
    public void customizeRequest(HttpConnection connection,
                                    HttpRequest request)
    {
    }

    /* ------------------------------------------------------------ */
    /** Persist the connection
     * @param connection.
     */
    public void persistConnection(HttpConnection connection)
    {
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return True if low on idle threads. 
     */
    public boolean isLowOnResources()
    {
        boolean low =
            getThreads()==getMaxThreads() &&
            getIdleThreads()<getMinThreads();
        if (low && !_lastLow)
            Log.event("LOW ON THREADS: "+this);
        else if (!low && _lastLow)
        {
            Log.event("OK on threads: "+this);
            _lastOut=false;
        }
        _lastLow=low;
        return low;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return True if low on idle threads. 
     */
    public boolean isOutOfResources()
    {
        boolean out =
            getThreads()==getMaxThreads() &&
            getIdleThreads()==0;
        if (out && !_lastOut)
            Code.warning("OUT OF THREADS: "+this);
            
        _lastOut=out;
        return out;
    }

    /* ------------------------------------------------------------ */
    public boolean isIntegral(HttpConnection connection)
    {
        return false;
    }
    
    /* ------------------------------------------------------------ */
    public boolean isConfidential(HttpConnection connection)
    {
        return false;
    }

    /* ------------------------------------------------------------ */
    public String getIntegralScheme()
    {
        return _integralScheme;
    }
    
    /* ------------------------------------------------------------ */
    public void setIntegralScheme(String integralScheme)
    {
        _integralScheme = integralScheme;
    }
    
    /* ------------------------------------------------------------ */
    public int getIntegralPort()
    {
        return _integralPort;
    }

    /* ------------------------------------------------------------ */
    public void setIntegralPort(int integralPort)
    {
        _integralPort = integralPort;
    }
    
    /* ------------------------------------------------------------ */
    public String getConfidentialScheme()
    {
        return _confidentialScheme;
    }

    /* ------------------------------------------------------------ */
    public void setConfidentialScheme(String confidentialScheme)
    {
        _confidentialScheme = confidentialScheme;
    }

    /* ------------------------------------------------------------ */
    public int getConfidentialPort()
    {
        return _confidentialPort;
    }

    /* ------------------------------------------------------------ */
    public void setConfidentialPort(int confidentialPort)
    {
        _confidentialPort = confidentialPort;
    }
    
    /* ------------------------------------------------------------ */
    public String toString()
    {
        if (_address==null)    
            return getName()+"@0.0.0.0:0";
        return getName()+"@"+_address;
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public class Connection extends HttpConnection
    {
        Connection(SocketChannel sc, Socket socket)
            throws IOException
        {
            super(SocketChannelListener.this,
                  socket.getInetAddress(),
                  Channels.newInputStream(sc),
                  ChannelStream.newOutputStream(sc),
                  sc);
        }

        public SocketChannel getSocketChannel()
        {
            return (SocketChannel)getConnection();
        }        
        
        public void close()
            throws IOException
        {
            SocketChannel sc = getSocketChannel();
            Socket socket=sc.socket();
            super.close();            
            sc.close();
            if (socket.isOutputShutdown())
                socket.shutdownOutput();
        }
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class SelectorThread extends Thread
    {
        boolean _running=false;
        public void run()
        {
            try
            {
                this.setName("Selector "+_address);
                _running=true;

                while(_running)
                {
                    // Add idling Idle connections.
                    synchronized(_idling)
                    {
                        // Make sure all cancelled keys are handled.
                        if (_idling.size()>0)
                            _selector.selectNow();
                    
                        // Register idling.
                        for (int i=0;i<_idling.size();i++)
                        {
                            Connection c = (Connection)_idling.get(i);
                            SocketChannel sc = c.getSocketChannel();
                            sc.configureBlocking(false);
                            sc.register(_selector,SelectionKey.OP_READ,c);
                            Code.debug("register ",c);
                        }
                        _idling.clear();
                    }
                
                    // Select
                    if (Code.debug())
                    {
                        Code.debug("select() on "+_selector.keys());
                        _selector.select();
                        Code.debug("Selected "+_selector.selectedKeys());
                    }
                    else
                        _selector.select();

                    // handle results of select
                    if (_selector.selectedKeys().size()>0)
                    {    
                        // For all ready selection Keys.
                        Iterator ready=_selector.selectedKeys().iterator();
                        while (ready!=null && ready.hasNext())
                        {
                            SelectionKey key = (SelectionKey)ready.next();
                            ready.remove();
                            if ((key.interestOps()&SelectionKey.OP_READ)!=0)
                            {
                                // Un-Idle a connections
                                HttpConnection connection=(HttpConnection)key.attachment();
                                Code.debug("READ ready ",connection);
                                key.cancel();
                                key.channel().configureBlocking(true);
                                SocketChannelListener.this.run(connection);
                            }
                            else if ((key.interestOps()&SelectionKey.OP_ACCEPT)!=0)
                            {
                                // We have connections to accept.
                                ServerSocketChannel channel = (ServerSocketChannel)key.channel();
                                SocketChannel sc;
                            
                                // Accept new connections
                                while ((sc=channel.accept())!=null)
                                {
                                    Code.debug("Accept: ",sc);
                                    sc.configureBlocking(true);
                                    Socket socket = sc.socket();
                                
                                    try {
                                        if (_maxReadTimeMs>=0)
                                            socket.setSoTimeout(_maxReadTimeMs);
                                        if (_lingerTimeSecs>=0)
                                            socket.setSoLinger(true,_lingerTimeSecs);
                                        else
                                            socket.setSoLinger(false,0);
                                    }
                                    catch(Exception e){Code.ignore(e);}
                                
                                    HttpConnection connection =
                                        new Connection(sc,socket);
                                    SocketChannelListener.this.run(connection);
                                
                                    // only accept single connection
                                    // if low on resources
                                    if (isLowOnResources())
                                        break;
                                }
                            }
                        }
                    }
                }
            }
            catch(Exception e)
            {
                if (_running)
                    Code.warning(e.toString());
                Code.debug(e);
            }
            finally
            {
                if (_running)
                    Code.warning("Stopping "+this.getName());
                else
                    Log.event("Stopping "+this.getName());

                try{if (_acceptChannel!=null)_acceptChannel.close();}
                catch (IOException e) {Code.ignore(e);}
                try{if (_selector!=null)_selector.close();}
                catch (IOException e) {Code.ignore(e);}
                
                _selector=null;
                _acceptChannel=null;
                _selectorThread=null;
            }
        }

        void forceStop()
        {
            Log.event("Force Stop "+this.getName());
        }
    }
}
