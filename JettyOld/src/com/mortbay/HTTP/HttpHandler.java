// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.*;
import java.io.*;


/* ------------------------------------------------------------------ */
/** Abstract handler of incoming HTTP requests. The available handlers
 * have their handleIn methods called in a configured order until
 * a response is sent.
 * Derived handlers may choose to ignore requests, modify them,
 * or handle them.
 * <P>Notes<br>
 * While a handler is similar to a javax.servlet.http.HttpServlet,
 * it is passed com.mortbay.HTTP requests and responses, which
 * allow modifications and greater control over IO.  HttpHandlers
 * are used when developing "ways" of dealing with requests.  Servlets
 * should be used to handle specific requests where possible.
 * @see com.mortbay.HTTP.HttpServer
 * @version $Id$
 * @author Greg Wilkins
 */
public interface HttpHandler 
{
    /* ----------------------------------------------------------------- */
    public void setProperties(Properties properties)
        throws IllegalStateException, IOException;
    
    /* ----------------------------------------------------------------- */
    /** Constructor
     * @param request The HTTP requests to be handled
     * @param response The HTTP response to be used.
     */
    public void handle(HttpRequest request,
                       HttpResponse response)
         throws Exception;
    
    /* ----------------------------------------------------------------- */
    /** Translate a path string.
     * Used by getRealPath method. 
     * @return the translated path
     */
    public String translate(String path);        
         
    /* ----------------------------------------------------------------- */
    /** Return enumeration of servlet Names within this handler or
     * null if no servlets   
     */
    public Enumeration servletNames();
        
    /* ----------------------------------------------------------------- */
    /** Return servlet by Name within this handler or
     * null if no servlets   
     */
    public Servlet servlet(String name);
        
    /* ------------------------------------------------------------ */
    /** Set server.
     * This method will be called when the configuration is given to
     * a particular server.  If a handler holds the the value
     * passed, it cannot be used in more than one server configuration
     * instances.
     */
    public void setServer(HttpServer server)
        throws Exception;

    /* ------------------------------------------------------------ */
    /** Destroy Handler.
     * Called by HttpServer.stop().
     */
    public void destroy();
}
            




