// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Configure;

import com.mortbay.Base.*;
import com.mortbay.Util.*;
import com.mortbay.Servlets.*;
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
        this(8080, ".", false,false);
    }
    
    /* -------------------------------------------------------------------- */
    public FileServer(int port,
                      String directory,
                      boolean allowPut,
                      boolean allowDelete)
         throws IOException
    {
        // Listen at a single port on the localhost
        addresses=new InetAddrPort[1];
        addresses[0]=new InetAddrPort(null,port);

        // Configure handlers
        httpHandlersMap=new PathMap();

        // Create full stack of HttpHandlers at "/"
        HttpHandler[] httpHandlers = new HttpHandler[2];
        httpHandlersMap.put("/",httpHandlers);
        int h=0;

        // File Handler
        FileHandler fh = new FileHandler(directory);
        
        fh.setPutAllowed(allowPut);
        fh.setDeleteAllowed(allowDelete);
        
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
            String directory=".";
            boolean allowPut=false;
            boolean allowDelete=false;

            int a=0;
            while(args.length>a && args[a].startsWith("-"))
            {
                if ("-allowPut".equals(args[a]))
                    allowPut=true;
                else if ("-allowDelete".equals(args[a]))
                    allowDelete=true;
                else
                {
                    System.err.println("Usage - java com.mortbay.HTTP.Configure.FileServer [options] [ port [ directory ] ]");
                    System.err.println("Options:");
                    System.err.println("  -help");
                    System.err.println("  -allowPut");
                    System.err.println("  -allowDelete");
                    System.exit(1);
                }
                a++;
            }
            
            if (args.length>a)
            {
                port = Integer.parseInt(args[a]);
                a++;
            }
            
            if (args.length>a)
            {
                directory = args[a];
                a++;
            }
            
            FileServer fileServer =
                new FileServer(port, directory, allowPut, allowDelete);

            HttpServer httpServer = new HttpServer(fileServer);
            httpServer.join();
        }
        catch(Exception e){
            Code.warning(e);
        }
    }
}


