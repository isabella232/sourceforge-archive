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
/** XXX
 *
 * @see HttpConnection
 * @version 1.0 Sat Oct  2 1999
 * @author Greg Wilkins (gregw)
 */
abstract public class HttpListener extends ThreadPool
{
    private HttpServer _server;
    
    /* ------------------------------------------------------------ */
    /** Constructor. XXX
     * @param server 
     * @param name 
     * @param minThreads 
     * @param maxThreads 
     * @param maxIdleTimeMs 
     * @exception IOException 
     */
    protected HttpListener(HttpServer server,
                           String name,
                           int minThreads,
                           int maxThreads,
                           int maxIdleTimeMs)
        throws IOException
    {
        super(name,minThreads,maxThreads,maxIdleTimeMs);
        _server=server;
    }

    /* ------------------------------------------------------------ */
    /** Get the listeners HttpServer 
     * @return HttpServer.
     */
    public HttpServer getServer()
    {
        return _server;
    }
    
    public abstract String getDefaultProtocol();
    public abstract String getHost();
    public abstract int getPort();
};










