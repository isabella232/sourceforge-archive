// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Jetty;
//import com.sun.java.util.collections.*; XXX-JDK1.1

import com.mortbay.Util.*;
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
 * @version $Id$
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

	    if (arg.length==0)
		server.addListener(new InetAddrPort(8080));
	    else
	        for (int l=0;l<arg.length;l++)
		    server.addListener(new InetAddrPort(arg[l]));
	    
	    // Configure handlers
	    HandlerContext context;
	    server.addWebApplication(null,
				     "/",
				     "./webapps/jetty");
	    
	    context=server.getContext(null,"/handler/*");
	    context.setResourceBase("./FileBase/");
	    context.setServingResources(true);
	    context.addServlet("/dump,/dump/*","com.mortbay.Servlet.Dump");
	    context.addServlet("/session","com.mortbay.Servlet.SessionDump");
	    context.addHandler(new DumpHandler());
	    
	    context=server.getContext(null,"/servlet/*");
	    context.setClassPath("./servlets/");
	    context.setServingDynamicServlets(true);
	    
	    context=server.getContext(null,"/javadoc/*");
	    context.setResourceBase("./javadoc/");
	    context.setServingResources(true);
	    
	    context=server.getContext(null,"/");
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

