// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;
import com.mortbay.Util.*;

import java.io.*;
import java.net.*;
import java.util.*;

/* ------------------------------------------------------------ */
/** Socket HTTP Listener.
 * 
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class SocketListener 
    extends ThreadedServer
    implements HttpListener
{
    /* ------------------------------------------------------------------- */
    private HttpServer _server;
    
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

    /* --------------------------------------------------------------- */
    public void start()
    {
        super.start();
        Log.event("Started SocketListener on "+getInetAddrPort());
    }
    
    /* --------------------------------------------------------------- */
    public void stop()
        throws InterruptedException
    {
        Log.event("Stopping SocketListener on "+getInetAddrPort());
        super.stop();
    }
    
    /* --------------------------------------------------------------- */
    public void destroy()
    {
        Log.event("Destroy SocketListener on "+getInetAddrPort());
        super.destroy();
    }
    
    /* ------------------------------------------------------------ */
    /** Handle Job.
     * Implementation of ThreadPool.handle(), calls handleConnection.
     * @param job A Connection.
     */
    public final void handleConnection(Socket socket)
    {
        try
        {
            Code.debug("ACCEPT:",socket);
	    socket.setSoLinger(false,0);
            HttpConnection connection =
                new HttpConnection(this,socket.getInetAddress(),
				   socket.getInputStream(),
				   socket.getOutputStream());
            connection.handle();
        }
        catch ( Exception e ){
            Code.warning("Connection problem",e);
        }
    }
}






