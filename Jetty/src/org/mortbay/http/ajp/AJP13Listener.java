// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http.ajp;


import java.io.IOException;
import java.net.Socket;
import org.mortbay.http.HttpConnection;
import org.mortbay.http.HttpListener;
import org.mortbay.http.HttpMessage;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.HttpServer;
import org.mortbay.util.Code;
import org.mortbay.util.InetAddrPort;
import org.mortbay.util.Log;
import org.mortbay.util.ThreadPool;
import org.mortbay.util.ThreadedServer;


/* ------------------------------------------------------------ */
/**
 * This code uses the AJP13 code from tomcat3.3 as the protocol
 * specification, but is new  implementation.
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class AJP13Listener
    extends ThreadedServer
    implements HttpListener
{
    /* ------------------------------------------------------------------- */
    private HttpServer _server;
    private boolean _lastOut=false;
    private boolean _lastLow=false;
    private String _integralScheme=HttpMessage.__SSL_SCHEME;
    private String _confidentialScheme=HttpMessage.__SSL_SCHEME;
    private int _integralPort=0;
    private int _confidentialPort=0;
    private boolean _identifyListener=false;
    private int _bufferSize=8192;

    /* ------------------------------------------------------------------- */
    public AJP13Listener()
    {}

    /* ------------------------------------------------------------------- */
    public AJP13Listener(InetAddrPort address)
        throws IOException
    {
        super(address);
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
    public boolean getIdentifyListener()
    {
        return _identifyListener;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param identifyListener If true, the listener name is added to all
     * requests as the org.mortbay.http.HttListener attribute
     */
    public void setIdentifyListener(boolean identifyListener)
    {
        _identifyListener = identifyListener;
    }
    
    /* --------------------------------------------------------------- */
    public String getDefaultScheme()
    {
        return HttpMessage.__SCHEME;
    }    
    
    /* --------------------------------------------------------------- */
    public void start()
        throws Exception
    {
        super.start();
        Log.event("Started AJP13Listener on "+getInetAddrPort());
    }

    /* --------------------------------------------------------------- */
    public void stop()
        throws InterruptedException
    {
        super.stop();
        Log.event("Stopped AJP13Listener on "+getInetAddrPort());
    }

    /* ------------------------------------------------------------ */
    /** Handle Job.
     * Implementation of ThreadPool.handle(), calls handleConnection.
     * @param job A Connection.
     */
    public void handleConnection(Socket socket)
        throws IOException
    {
        socket.setTcpNoDelay(true);
        socket.setSoTimeout(getMaxIdleTimeMs());

        new AJP13Connection(this,
                            new AJP13InputStream(socket.getInputStream(),
                                                 socket.getOutputStream(),
                                                 getBufferSize()),
                            new AJP13OutputStream(socket.getOutputStream(),
                                                  getBufferSize()),
                            socket).handle();
    }

    /* ------------------------------------------------------------ */
    /** Customize the request from connection.
     * This method extracts the socket from the connection and calls
     * the customizeRequest(Socket,HttpRequest) method.
     * @param request
     */
    public void customizeRequest(HttpConnection connection,
                                 HttpRequest request)
    {
        if (_identifyListener)
            request.setAttribute(HttpListener.ATTRIBUTE,getName());
        
        Socket socket=(Socket)(connection.getConnection());
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
    }

    /* ------------------------------------------------------------ */
    /** Persist the connection.
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
     * @return True if out of resources. 
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
        return ((AJP13Connection)connection).isSSL();
    }
    
    /* ------------------------------------------------------------ */
    public boolean isConfidential(HttpConnection connection)
    {
        return ((AJP13Connection)connection).isSSL();
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
}
