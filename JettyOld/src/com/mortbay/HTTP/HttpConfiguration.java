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
/** Abstract HTTP Server Configuration.
 * This interface defines the  methods that the HttpServer calls
 * to configure itself.
 * It decouples the server from the configuration technology used.
 * In most uses either a very simple configuration will be
 * provided or the implementation of this interface will map the
 * calls to a configuration file.
 */
public interface HttpConfiguration 
{
    /** Property name for the default session max idle time */
    public static final String SessionMaxInactiveInterval = 
        "SessionMaxInactiveInterval";
    
    /** Property name for the minimum listener threads */
    public static final String MinListenerThreads="MinListenerThreads";
    
    /** Property name for the maximum listener threads */
    public static final String MaxListenerThreads="MaxListenerThreads";
    
    /** Property name for the maximum listener idle time */
    public static final String MaxListenerThreadIdleMs="MaxListenerThreadIdleMs";
    
    /** Property name for the getResource url base (eg. "file:" */
    public static final String ResourceBase="ResourceBase";
    
    /** Property name for User ID of the server (UNIX ONLY) */
    public static final String SetUserID="SetUserID";

    /* ------------------------------------------------------------ */
    /** The IP addresses and ports the HTTP server listens on
     */
    public InetAddrPort[] addresses();

    /* ------------------------------------------------------------ */
    /** The HttpListener classes.
     * The classes derived from HttpListener (or HttpListener) used
     * to listen to the corresponding address from addresses().
     */
    public Class[] listenerClasses();

    /* ------------------------------------------------------------ */
    /** The HttpHandlers.
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
    /** The ExceptionHandlers.
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
    /** Returns an attribute of the server.
     * @deprecated Use getProperty
     */
    public Object getAttribute(String name);

    /* ------------------------------------------------------------ */
    /** Returns an attribute of the server given the specified key name.
     */
    public String getProperty(String name);

    /* ------------------------------------------------------------ */
    /** Returns the properties.
     * This may be modified and care must be taken in it's use.
     * Modifications may or may not effect the currently running
     * server (dynamic reconfiguration is under consideration).
     */
    public Properties getProperties();
    
    /* ------------------------------------------------------------ */
    /** Writes a message to the servlet log file.
     */
    public void log(String message);
    
}
