// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Jetty;

import com.mortbay.HTTP.Handler.DumpHandler;
import com.mortbay.HTTP.Handler.NotFoundHandler;
import com.mortbay.HTTP.HandlerContext;
import com.mortbay.HTTP.HttpServer;
import com.mortbay.Util.Code;
import com.mortbay.Util.InetAddrPort;
import com.mortbay.Util.RolloverFileLogSink;


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
            context.addServlet("Dump","/dump,/dump/*","com.mortbay.Servlet.Dump");
            context.addServlet("/session","com.mortbay.Servlet.SessionDump");
            context.addServlet("/Dispatch,/Dispatch/*",
                               "com.mortbay.Servlet.RequestDispatchTest");
            context.addHandler(new DumpHandler());
            
            context=server.getContext(null,"/servlet/*");
            context.setClassPath("./servlets/");
            context.setServingDynamicServlets(true);
            
            context=server.getContext(null,"/javadoc/*");
            context.setResourceBase("./javadoc/");
            context.setServingResources(true);
            
            context=server.getContext(null,"/");
            context.addHandler(new NotFoundHandler());

            // Logger
            RolloverFileLogSink log = new RolloverFileLogSink();
            log.setLogDir("./logs");
            log.setRetainDays(90);
            log.setMultiDay(false);
            log.setAppend(true);
            server.setLogSink(log);
                              
            // Start handlers and listener
            server.start();
        }
        catch(Exception e)
        {
            Code.fail(e);
        }
    }
}



