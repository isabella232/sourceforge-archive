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
/** HTTP Listener
 *
 * @see HttpConnection
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public interface HttpListener extends LifeCycle
{
    public abstract HttpServer getServer();
    public abstract String getDefaultScheme();
    public abstract String getHost();
    public abstract int getPort();
    

    /* ------------------------------------------------------------ */
    /** Start the listener.
     * All requests are ignored until start is called.
     */
    public abstract void start();
    
    /* ------------------------------------------------------------ */
    /** Stop the listener.
     * New requests are refused and the handler may attempt to wait
     * for existing requests to complete. The caller may interrupt
     * the stop call is waiting is taking too long.
     */
    public abstract void stop()
        throws InterruptedException;
    
    /* ------------------------------------------------------------ */
    /** Destroy the handler.
     * New requests are refused and all current requests are immediately
     * terminated.
     */
    public abstract void destroy();
}
