// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler;
import com.mortbay.Base.*;
import com.mortbay.HTTP.*;
import java.io.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.*;


/* --------------------------------------------------------------------- */
/** NotFound Handler
 * <p>This handler is a terminating handler of a handler stack.
 * Any request passed to this handler receives a SC_NOT_FOUND
 * response.
 *
 * @see Interface.HttpHandler
 * @version $Id$
 * @author Greg Wilkins
 */
public class NotFoundHandler extends NullHandler
{
    /* ------------------------------------------------------------ */
    /** Constructor from properties.
     * Calls setProperties.
     * @param properties Configuration properties
     */
    public NotFoundHandler(Properties properties)
    {
	setProperties(properties);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public NotFoundHandler()
    {}
    
    /* ------------------------------------------------------------ */
    /** Configure from properties.
     * No configuration parameters for this handler
     * @param properties configuration.
     */
    public void setProperties(Properties properties)
    {}
    
    /* ----------------------------------------------------------------- */
    public void handle(HttpRequest request,
		       HttpResponse response)
	 throws Exception
    {
	String method=request.getMethod();
	if (method.equals(HttpRequest.GET))
	    response.sendError(response.SC_NOT_FOUND);
	else if (method.equals(HttpRequest.POST))
	    response.sendError(response.SC_NOT_FOUND);
	else if (method.equals(HttpRequest.HEAD))
	    response.sendError(response.SC_NOT_FOUND);
	else if (method.equals(HttpRequest.OPTIONS))
	    response.sendError(response.SC_NOT_FOUND);
	else
	    response.sendError(response.SC_METHOD_NOT_ALLOWED);
    }    
}


