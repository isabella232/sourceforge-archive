// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Configure;

import com.mortbay.Base.*;
import com.mortbay.Util.*;
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
 * java com.mortbay.HTTP.Configure.FileServer [-help] [port  [urlPath [classPath]]]
 * </PRE>
 * @see com.mortbay.HTTP.HttpConfiguration
 * @version $Id$
 * @author Greg Wilkins
*/
public class ServletServer extends BaseConfiguration
{
    
    /* -------------------------------------------------------------------- */
    public ServletServer(int port,
                         String servletPath,
                         String classPath)
         throws IOException
    {
        // Listen at a single port on the localhost
        addresses=new InetAddrPort[1];
        addresses[0]=new InetAddrPort(null,port);

        // Create single stack of HttpHandlers at "/"
        httpHandlersMap=new PathMap();
        HttpHandler[] httpHandlers = new HttpHandler[4];
        httpHandlersMap.put("/",httpHandlers);
        int h=0;

        // Parameter handler
        httpHandlers[h++] = new SessionHandler();
        
        // Parameter handler
        httpHandlers[h++] = new ParamHandler();
        
        // Servlet Handler
        Properties properties = new Properties();
        properties.put("PATHS",servletPath);
        properties.put("CLASSPATH",classPath);
        httpHandlers[h++] = new ServletHandler(properties);

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
            String servletPath="/";
            String classPath=".";

            int a=0;
            while(args.length>a && args[a].startsWith("-"))
            {
                System.err.println("Usage - java com.mortbay.HTTP.Configure.FileServer [options] [ port  [ urlPath [ classPath ] ] ]");
                System.err.println("Options:");
                System.err.println("  -help");
                System.exit(1);
                a++;
            }
            
            if (args.length>a)
            {
                port = Integer.parseInt(args[a]);
                a++;
            }
            
            if (args.length>a)
            {
                servletPath = args[a];
                a++;
            }
            
            if (args.length>a)
            {
                classPath = args[a];
                a++;
            }
            
            ServletServer servletServer =
                new ServletServer(port,servletPath,classPath);

            HttpServer httpServer = new HttpServer(servletServer);
            httpServer.join();
        }
        catch(Exception e){
            Code.warning(e);
        }
    }
}
