// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Jetty;

import com.mortbay.Util.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.util.*;
import com.mortbay.HTTP.*;
import com.mortbay.HTTP.Handler.*;
import com.mortbay.HTTP.Handler.Servlet.*;
import java.lang.reflect.*;


/* ------------------------------------------------------------ */
/** 
 *
 * @see
 * @version 1.0 Sun Jun 18 2000
 * @author Greg Wilkins (gregw)
 */
public class Demo
{
    public static void main(String[] arg)
    {
	try
	{    
	    // Make server
	    HttpServer server = new HttpServer();
	    
	    server.addListener(new InetAddrPort(8080));
	    
	    // Configure handlers
	    HandlerContext context;
	    server.addWebApplication(null,
				     "/",
				     "./webapps/jetty");
	    context=server.getContext(null,"/handler/*");
	    context.setFileBase(".");
	    context.addServlet("/dump,/dump/*","com.mortbay.Servlet.Dump");
	    context.addServlet("/session","com.mortbay.Servlet.SessionDump");
	    context.setServingFiles(true);
	    context.addHandler(new DumpHandler());
	    context.addHandler(new NotFoundHandler());
	    
	    context=server.getContext(null,"/servlet/*");
	    context.setClassPath("./servlets");
	    context.setServingDynamicServlets(true);
	    context.addHandler(new NotFoundHandler());
	    
	    // Start handlers and listener
	    server.start();
	}
	catch(Exception e)
	{
	    Code.fail(e);
	}
    }
}
