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
import com.mortbay.HTTP.Filter.*;
import com.mortbay.HTTP.Configure.*;
import java.io.*;
import java.net.*;
import javax.servlet.*;
import java.util.*;


/* ------------------------------------------------------------ */
/** File serving HTTP configuration
 * This simple HTTP configuration serves files from the current
 * directory.
 *
 * @version $Revision$ $Date$
 * @author Greg Wilkins (gregw)
 */
public class FileServer extends BaseConfiguration
{
    /* -------------------------------------------------------------------- */
    public FileServer()
	 throws IOException
    {
	this(8080, false);
    }
    
    /* -------------------------------------------------------------------- */
    public FileServer(int port, boolean allowAll)
	 throws IOException
    {
	// Listen at a single port on the localhost
	addresses=new InetAddrPort[1];
	addresses[0]=new InetAddrPort();
	addresses[0].inetAddress=null;
	addresses[0].port=port;

	// Configure handlers
	httpHandlersMap=new PathMap();

	// Create full stack of HttpHandlers at "/"
	HttpHandler[] httpHandlers = new HttpHandler[2];
	httpHandlersMap.put("/",httpHandlers);
	int h=0;

	// File Handler
	FileHandler fh = new FileHandler(".");
	fh.setPutAllowed(true);
	fh.setDeleteAllowed(true);
	httpHandlers[h++] = fh;

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
	    int port = 8080;
	    if (args.length > 0)
		port = Integer.parseInt(args[0]);
	    
	    FileServer fileServer;
	    if (args.length > 1)
		fileServer = new FileServer(port, true);
	    else
		fileServer = new FileServer(port, false);

	    HttpServer httpServer = new HttpServer(fileServer);
	    httpServer.join();
	}
	catch(Exception e){
	    Code.warning("FileServer Failed",e);
	}
    }
}
