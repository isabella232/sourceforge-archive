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
 * 
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
            server.addWebApplication(null,"/jetty/*",
                                     "./webapps/jetty.war",
                                     "./etc/webdefault.xml");
            
            context=server.getContext(null,"/demo/*");
            context.setResourceBase("./docroot/");
            context.addServlet("Dump",
                               "/dump/*,*.DUMP",
                               "com.mortbay.Servlet.Dump");
            context.addServlet("Session",
                               "/session",
                               "com.mortbay.Servlet.SessionDump");
            context.addServlet("Dispatch",
                               "/Dispatch/*",
                               "com.mortbay.Servlet.RequestDispatchTest");
            context.addServlet("JSP","*.jsp",
                               "org.apache.jasper.servlet.JspServlet");
            context.setServingResources(true);
            context.addHandler(new DumpHandler());
            
            context=server.addContext(null,"/servlet/*");
            context.setClassPath("./servlets/");
            context.setServingDynamicServlets(true);
            
            context=server.addContext(null,"/javadoc/*");
            context.setResourceBase("./javadoc/");
            context.setServingResources(true);
            
            context=server.addContext(null,"/");
            context.addServlet("Forward",
                               "/",
                               "com.mortbay.Servlet.Forward")
                .put("/","/jetty/index.html");
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
