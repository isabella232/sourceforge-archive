// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler;
import com.mortbay.Base.*;
import com.mortbay.HTTP.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.*;


/* --------------------------------------------------------------------- */
/** Null HttpHandler
 * Conveniance base class with null handlers for all methods
 *
 * @see Interface.HttpHandler
 * @version $Id$
 * @author Greg Wilkins
 */
public class NullHandler implements HttpHandler 
{
    /* ----------------------------------------------------------------- */
    protected HttpServer httpServer=null;
    
    /* ----------------------------------------------------------------- */
    public void handle(HttpRequest request,
			 HttpResponse response)
	 throws Exception
    {}
    
    /* ----------------------------------------------------------------- */
    public String translate(String path)
    {
	return path;
    }
	 
    /* ----------------------------------------------------------------- */
    public Enumeration servletNames()
    {
	return null;
    } 
	
    /* ----------------------------------------------------------------- */
    public Servlet servlet(String name)
    {
	return null;
    } 

    /* ------------------------------------------------------------ */
    public void setServer(HttpServer server)
	 throws Exception
    {
	httpServer=server;
    }
}



