// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Configure;

import com.mortbay.Base.*;
import com.mortbay.Util.*;
import com.mortbay.Servlets.*;
import com.mortbay.HTML.*;
import com.mortbay.HTTP.*;
import com.mortbay.HTTP.Handler.*;
import java.io.*;
import java.net.*;
import javax.servlet.*;
import java.util.*;


/* ------------------------------------------------------------------------ */
/** Simple servlet configuration
 * The HTTP configuration allows a single servlet to be configured and
 * served from command line options.
 * <P>Usage<PRE>
 * java com.mortbay.HTTP.Configure.SimpleServletConfig path name class [port]
 * </PRE>
 * @see com.mortbay.HTTP.HttpConfiguration
 * @version $Id$
 * @author Greg Wilkins
*/
public class SimpleServletConfig extends BaseConfiguration
{
    /* -------------------------------------------------------------------- */
    public SimpleServletConfig(String servletPath,
			       String servletName,
			       String servletClass)
	 throws IOException
    {
	this(servletPath,servletName,servletClass,8080);
    }
    
    /* -------------------------------------------------------------------- */
    public SimpleServletConfig(String servletPath,
			       String servletName,
			       String servletClass,
			       int port)
	 throws IOException
    {
	// Listen at a single port on the localhost
	addresses=new InetAddrPort[1];
	addresses[0]=new InetAddrPort();
	addresses[0].inetAddress = InetAddress.getLocalHost();
	addresses[0].port=port;

	// Create single stack of HttpHandlers at "/"
	httpHandlersMap=new PathMap();
	HttpHandler[] httpHandlers = new HttpHandler[3];
	httpHandlersMap.put("/",httpHandlers);
	int h=0;

	// Parameter handler
	httpHandlers[h++] = new ParamHandler();
	
	// Servlet Handler
	PathMap servletMap= new PathMap();
	servletMap.put(servletPath,
		       new ServletHolder(servletName,
					 servletClass));
	httpHandlers[h++] = new ServletHandler(servletMap);

	// NotFound Handler
	httpHandlers[h++] = new NotFoundHandler();
    }


    /* -------------------------------------------------------------------- */
    /** Sample Main
     * Configures the Dump servlet and starts the server
     */
    public static void main(String args[])
    {
	try{	    
	    HttpConfiguration config = null;

	    switch (args.length)
	    {
	      case 3:
		  config = new SimpleServletConfig(args[0],
						   args[1],
						   args[2]);
		  break;
	      case 4:
		  config = new SimpleServletConfig(args[0],
						   args[1],
						   args[2],
						   Integer.parseInt(args[3]));
	      default:
		  System.err.println("Usage - java com.mortbay.HTTP.Configure.SimpleServletConfig path name class [port]");
		  System.exit(1);
	    }
	    
	    HttpServer httpServer = new HttpServer(config);
	    httpServer.join();
	}
	catch(Exception e){
	    Code.warning("Demo Failed",e);
	}
    }
}
