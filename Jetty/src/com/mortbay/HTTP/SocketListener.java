// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;
import com.mortbay.Util.Code;
import com.mortbay.Util.InetAddrPort;
import com.mortbay.Util.Log;
import com.mortbay.Util.ThreadedServer;
import java.io.IOException;
import java.net.Socket;


/* ------------------------------------------------------------ */
/** Socket HTTP Listener.
 * The behaviour of the listener can be controlled with the
 * attributues of the ThreadedServer and ThreadPool from which it is
 * derived. Specifically: <PRE>
 * MinThreads    - Minumum threads waiting to service requests.
 * MaxThread     - Maximum thread that will service requests.
 * MaxIdleTimeMs - Time for an idle thread to wait for a request.
 * MaxReadTimeMs - Time that a read on a request can block.
 * </PRE>
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class SocketListener
    extends ThreadedServer
    implements HttpListener
{
    /* ------------------------------------------------------------------- */
    private HttpServer _server;
    private int _lowResourcePersistTimeMs=2000;
    private int _throttled=0;
    
    /* ------------------------------------------------------------------- */
    public SocketListener()
        throws IOException
    {}

    /* ------------------------------------------------------------------- */
    public SocketListener(InetAddrPort address)
        throws IOException
    {
        super(address);
    }

    /* ------------------------------------------------------------ */
    public void setHttpServer(HttpServer server)
    {
        Code.assert(_server==null || _server==server,
                    "Cannot share listeners");
        _server=server;
    }

    /* ------------------------------------------------------------ */
    public HttpServer getHttpServer()
    {
        return _server;
    }

    /* --------------------------------------------------------------- */
    public String getDefaultScheme()
    {
        return "http";
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return time in ms that connections will persist if listener is
     * low on resources.
     */
    public int getLowResourcePersistTimeMs()
    {
        return _lowResourcePersistTimeMs;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param ms time in ms that connections will persist if listener is
     * low on resources. 
     */
    public void setLowResourcePersistTimeMs(int ms)
    {
        _lowResourcePersistTimeMs=ms;
    }
    
    /* --------------------------------------------------------------- */
    public void start()
        throws Exception
    {
        super.start();
        Log.event("Started SocketListener on "+getInetAddrPort());
    }

    /* --------------------------------------------------------------- */
    public void stop()
        throws InterruptedException
    {
        super.stop();
        Log.event("Stopped SocketListener on "+getInetAddrPort());
    }

    /* --------------------------------------------------------------- */
    public void destroy()
    {
        Log.event("Destroy SocketListener on "+getInetAddrPort());
        super.destroy();
        if (_server!=null)
            _server.removeListener(this);
        _server=null;
    }

    /* ------------------------------------------------------------ */
    /** Handle Job.
     * Implementation of ThreadPool.handle(), calls handleConnection.
     * @param job A Connection.
     */
    public void handleConnection(Socket socket)
        throws IOException
    {
        HttpConnection connection =
            new HttpConnection(this,
                               socket.getInetAddress(),
                               socket.getInputStream(),
                               socket.getOutputStream(),
                               socket);
        connection.handle();
    }

    /* ------------------------------------------------------------ */
    /** Customize the request from connection.
     * This method extracts the socket from the connection and calls
     * the customizeRequest(Socket,HttpRequest) method.
     * @param request
     */
    public final void customizeRequest(HttpConnection connection,
                                       HttpRequest request)
    {
        Socket socket=(Socket)(connection.getConnection());

        try
        {
            if (_throttled>0 && socket.getSoTimeout()!=getMaxReadTimeMs())
            {
                _throttled--;
                socket.setSoTimeout(getMaxReadTimeMs());
            }
        }
        catch(Exception e)
        {
            Code.warning(e);
        }
        customizeRequest(socket,request);
    }

    /* ------------------------------------------------------------ */
    /** Customize request from socket.
     * Derived versions of SocketListener may specialize this method
     * to customize the request with attributes of the socket used (eg
     * SSL session ids).
     * @param request
     */
    protected void customizeRequest(Socket socket,
                                    HttpRequest request)
    {
        // Do nothing
    }

    /* ------------------------------------------------------------ */
    /** Persist the connection
     * If the listener is low on resources, the connection read
     * timeout is set to lowResourcePersistTimeMs.  The
     * customizeRequest method is used to reset this to the normal
     * value after a request has been read.
     * @param connection.
     */
    public final void persistConnection(HttpConnection connection)
    {
        if (isLowOnResources())
        {
            try
            {
                _throttled++;
                Socket socket=(Socket)(connection.getConnection());
                socket.setSoTimeout(_lowResourcePersistTimeMs);
            }
            catch(Exception e)
            {
                Code.warning(e);
            }
        }
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return True if low on idle threads. 
     */
    public boolean isLowOnResources()
    {
        return
            getThreads()==getMaxThreads() &&
            getIdleThreads()<getMinThreads();
    }
    

}






