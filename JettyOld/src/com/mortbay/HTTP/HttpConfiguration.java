// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP;

import com.mortbay.Base.*;
import com.mortbay.Util.*;
import java.io.*;
import java.net.*;
import javax.servlet.*;
import java.util.*;



/* ---------------------------------------------------------------- */
/** Abstract HTTP Server Configuration
 * This interface defines the  methods that the HttpServer calls
 * to configure itself.
 * It decouples the server from the configuration technology used.
 * In most uses either a very simple configuration will be
 * provided or the implementation of this interface will map the
 * calls to a configuration file.
 */
public interface HttpConfiguration 
{
    // The name of the default session max idle time attribute
    public static final String DefaultSessionMaxIdleTime = 
        "DefaultSessionMaxIdleTime";

    /* ------------------------------------------------------------ */
    /** The IP addresses and ports the HTTP server listens on
     */
    public InetAddrPort[] addresses();

    /* ------------------------------------------------------------ */
    /** The HttpHandlers
     * The PathMap returned by this method maps request paths to
     * arrays of HttpHandler instances. When an incoming request is
     * received, its full requestPath is used to select the array of
     * handlers from this pathMap and then each handler is called in
     * turn until the request is handled.
     * Simple configurations will normally map "/" to a single array
     * of handlers.  Handler instances can be placed in more than one
     * array within this map.
     */
    public PathMap httpHandlersMap();
    
    /* ------------------------------------------------------------ */
    /** The ExceptionHandlers
     * The PathMap returned by this method maps request paths to
     * arrays of ExceptionHandler instances. When a HttpHandler throws an
     * exception, the requests full requestPath is used to select
     * an array of handlers from this pathMap and then each handler is
     * called in turn until the exception is handled.
     * Simple configurations will normally map "/" to a single array
     * of handlers.  Handler instances can be placed in more than one
     * array within this map.
     */
    public PathMap exceptionHandlersMap();

    /* ------------------------------------------------------------ */
    /** Translate Mime type
     */
    public String getMimeType(String file);

    
    /* ------------------------------------------------------------ */
    /** Returns an attribute of the server given the specified key name.
     */
    public Object getAttribute(String name);

    
    /* ------------------------------------------------------------ */
    /** Writes a message to the servlet log file.
     */
    public void log(String message);
    
}
