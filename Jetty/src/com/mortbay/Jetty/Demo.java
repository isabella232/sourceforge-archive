// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Jetty;

import com.mortbay.HTTP.Handler.DumpHandler;
import com.mortbay.HTTP.Handler.ForwardHandler;
import com.mortbay.HTTP.HandlerContext;
import com.mortbay.HTTP.HashUserRealm;
import com.mortbay.HTTP.HttpServer;
import com.mortbay.HTTP.SecurityConstraint;
import com.mortbay.HTTP.SocketListener;
import com.mortbay.Jetty.Servlet.ServletHandlerContext;
import com.mortbay.Util.Code;
import com.mortbay.Util.InetAddrPort;
import com.mortbay.Util.WriterLogSink;


/* ------------------------------------------------------------ */
/** Demo Jetty Server.
 * Programmatically configure HttpServer instances for demo site.
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
            ServletHandlerContext context;

            // Realm
            HashUserRealm realm=
                new HashUserRealm("Jetty Demo Realm",
                                  "etc/demoRealm.properties");
            
            // Make server
            Server server = new Server();
            server.addRealm(realm);
            
            SocketListener listener=null;
            if (arg.length==0)
            {
                listener = (SocketListener)
                    server.addListener(new InetAddrPort(8080));
                listener.setMaxIdleTimeMs(60000);
                listener.setMaxReadTimeMs(60000);
            }
            else
            {
                for (int l=0;l<arg.length;l++)
                {
                    listener = (SocketListener)
                        server.addListener(new InetAddrPort(arg[l]));
                    listener.setMaxIdleTimeMs(60000);
                    listener.setMaxReadTimeMs(60000);
                }
            }
            
            // Configure handlers
            server.addWebApplication(null,
                                     "/jetty/*",
                                     "webapps/jetty",
                                     "etc/webdefault.xml",
                                     false);
            
            context=(ServletHandlerContext)server.getContext(null,"/demo/*");
            context.setResourceBase("docroot/");

            context.addServlet("Dump",
                               "/dump/*,*.DUMP",
                               "com.mortbay.Servlet.Dump");
            context.addServlet("Session",
                               "/session",
                               "com.mortbay.Servlet.SessionDump");
            context.addServlet("Dispatch",
                               "/Dispatch/*",
                               "com.mortbay.Servlet.RequestDispatchTest");
            context.addServlet("JSP","*.jsp,*.jsP,*.jSp,*.jSP,*.Jsp,*.JsP,*.JSp,*.JSP",
                               "org.apache.jasper.servlet.JspServlet");
            context.setServingResources(true);
            context.addHandler(new DumpHandler());
            
            ForwardHandler fh = new ForwardHandler("/dump/forwardedRoot");
            fh.addForward("/forward/*","/dump/forwarded");
            context.addHandler(0,fh);
            
            context=(ServletHandlerContext)server.addContext(null,"/javadoc/*");
            context.setResourceBase("javadoc/");
            context.setServingResources(true);

            context=(ServletHandlerContext)server.addContext(null,"/cgi-bin/*");
            context.setResourceBase("cgi-bin/");
            context.addServlet("CGI","/","com.mortbay.Servlet.CGI")
                .put("Path","/bin:/usr/bin:/usr/local/bin");
            
            context=(ServletHandlerContext)server.addContext(null,"/");
            context.addHandler(new ForwardHandler("/jetty/index.html"));
            context.setRealm("Jetty Demo Realm");
            context.addAuthConstraint("/admin/*","content-administrator");
            context.setClassPath("servlets/");
            context.setDynamicServletPathSpec("/servlet/*");
            context.getServletHandler().setServeDynamicSystemServlets(false);
            context.setResourceBase("etc/dtd");
            context.setServingResources(true);
            
            // Logger
            WriterLogSink log = new WriterLogSink("logs/yyyy_mm_dd.request.log");
            log.setRetainDays(90);
            log.setAppend(true);
            server.setLogSink(log);
                              
            // Start handlers and listener
            server.start();
            
            // Admin server
            HttpServer admin = new HttpServer();
            admin.addRealm(realm);
            listener = (SocketListener)
                admin.addListener(new InetAddrPort("127.0.0.1:8888"));
            listener.setMaxIdleTimeMs(60000);
            listener.setMaxReadTimeMs(60000);
            context=(ServletHandlerContext)admin.addContext(null,"/");
            context.setRealm("Jetty Demo Realm");
            context.addAuthConstraint("/","server-administrator");
            context.addServlet("Admin","/","com.mortbay.Servlet.AdminServlet");
            context.addServlet("Debug","/Debug/*","com.mortbay.Servlet.Debug");
            admin.start();
        }
        catch(Exception e)
        {
            Code.fail(e);
        }
    }
}

