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
 * Temparary implementation, just to get the ball rolling.
 * 
 * @version 1.0 Fri Oct  8 1999
 * @author Greg Wilkins (gregw)
 */
public class SocketListener 
    extends ThreadedServer
    implements HttpListener
{
    /* ------------------------------------------------------------------- */
    private HttpServer _server;
    
    /* ------------------------------------------------------------------- */
    SocketListener(HttpServer server, InetAddrPort address)
        throws IOException
    {
        super(address,1,5,300000,300000);
        _server=server;
    }

    /* ------------------------------------------------------------ */
    public HttpServer getServer()
    {
        return _server;
    }
    
    /* --------------------------------------------------------------- */
    public String getDefaultScheme()
    {
        return "http";
    }

    /* --------------------------------------------------------------- */
    public String getHost()
    {
        return super.getInetAddress().getHostName();
    }
    
    /* --------------------------------------------------------------- */
    public int getPort()
    {
        return super.getPort();
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
            Log.event("Accepted "+socket);
            ChunkableInputStream in  =
                new ChunkableInputStream(socket.getInputStream());
            ChunkableOutputStream out =
                new ChunkableOutputStream(socket.getOutputStream());

            HttpConnection connection = new HttpConnection(this,in,out);
            connection.handle();
        }
        catch ( Exception e ){
            Code.warning("Connection problem",e);
        }
        finally
        {
            try {socket.close();}
            catch ( Exception e ){Code.warning("Connection problem",e);}
            socket=null;
        }
    }
};







