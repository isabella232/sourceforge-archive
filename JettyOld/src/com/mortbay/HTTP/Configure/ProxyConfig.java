// ========================================================================
// Copyright (c) 1998 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP.Configure;

import com.mortbay.Base.*;
import com.mortbay.Util.*;
import com.mortbay.HTTP.*;
import com.mortbay.HTTP.Handler.*;
import java.io.*;
import java.net.*;
import javax.servlet.*;
import java.util.*;

public class ProxyConfig extends BaseConfiguration
{
    
    /* -------------------------------------------------------------------- */
    public ProxyConfig(int port)
	 throws IOException
    {
	this(new InetAddrPort(InetAddress.getLocalHost(),port));
    }
    
    /* -------------------------------------------------------------------- */
    public ProxyConfig(InetAddrPort addrPort)
	 throws IOException
    {
	// Listen at a single port on the localhost
	addresses=new InetAddrPort[1];
	addresses[0]=new InetAddrPort(addrPort.getInetAddress(),
				      addrPort.getPort());

	// Create single stack of HttpHandlers at "/"
	httpHandlersMap=new PathMap();
	HttpHandler[] httpHandlers = new HttpHandler[3];
	httpHandlersMap.put("http:",httpHandlers);
	httpHandlersMap.put("ftp:",httpHandlers);
	httpHandlersMap.put("file:",httpHandlers);
	httpHandlersMap.put("/",httpHandlers);
	int h=0;

	// handlers
	httpHandlers[h++] = new LogHandler(true, true);
	httpHandlers[h++] = new ProxyHandler();
	httpHandlers[h++] = new NotFoundHandler();	
    }


    /* -------------------------------------------------------------------- */
    /** Sample Main
     * Configures the Dump servlet and starts the server
     */
    public static void main(String args[])
    {
	try{
	    InetAddrPort addrPort= null;
	    
	    switch (args.length)
	    {
	      case 0:
		  addrPort = new InetAddrPort(8081);
		  break;
	      case 1:
		  addrPort = new InetAddrPort(Integer.parseInt(args[0]));
		  break;
	      case 2:
		  addrPort = new InetAddrPort(InetAddress.getByName(args[0]),
					      Integer.parseInt(args[1]));
		  break;
	      default:
		  System.err.println("Usage - java com.mortbay.HTTP.Configure.ProxyConfig [ [host] port ]");
		  System.exit(1);
	    }
	    
	    HttpConfiguration config = new ProxyConfig(addrPort);
	    HttpServer httpServer = new HttpServer(config);
	    httpServer.join();
	}
	catch(Exception e){
	    Code.warning("Demo Failed",e);
	    System.err.println("Usage - java com.mortbay.HTTP.Configure.ProxyConfig [ [host] port ]");
	    System.exit(1);
	}
    }
};
