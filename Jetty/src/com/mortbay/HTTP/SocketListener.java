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
/** 
 *
 * @see
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
    SocketListener(InetAddrPort address)
        throws IOException, InterruptedException
    {
        super(address,1,5,30000);
        _server=new HttpServer();
    }

    /* ------------------------------------------------------------ */
    public HttpServer getServer()
    {
        return _server;
    }
    
    /* --------------------------------------------------------------- */
    public String getDefaultProtocol()
    {
        return "http:";
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
    

    /* ------------------------------------------------------------ */
    /** Handle Job.
     * Implementation of ThreadPool.handle(), calls handleConnection.
     * @param job A Connection.
     */
    public final void handleConnection(Socket socket)
    {
        try
        {
            System.err.println("Accepted "+socket);
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







