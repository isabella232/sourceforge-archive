// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty;

import org.mortbay.http.handler.DumpHandler;
import org.mortbay.http.handler.ForwardHandler;
import org.mortbay.http.HandlerContext;
import org.mortbay.http.HashUserRealm;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SecurityConstraint;
import org.mortbay.http.SocketListener;
import org.mortbay.jetty.servlet.ServletHandlerContext;
import org.mortbay.util.Code;
import org.mortbay.util.InetAddrPort;
import org.mortbay.util.OutputStreamLogSink;


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
                               "org.mortbay.servlet.Dump");
            context.addServlet("Session",
                               "/session",
                               "org.mortbay.servlet.SessionDump");
            context.addServlet("Dispatch",
                               "/Dispatch/*",
                               "org.mortbay.servlet.RequestDispatchTest");
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
            context.addServlet("CGI","/","org.mortbay.servlet.CGI")
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
            OutputStreamLogSink log = new OutputStreamLogSink("logs/yyyy_mm_dd.request.log");
            log.setRetainDays(90);
            log.setAppend(true);
            log.setFlushOn(false);
            server.setLogSink(log);
                              
            // Start handlers and listener
            server.start();
            
            // Admin server
            HttpServer admin = new Server();
            admin.addRealm(realm);
            listener = (SocketListener)
                admin.addListener(new InetAddrPort("127.0.0.1:8888"));
            listener.setMaxIdleTimeMs(60000);
            listener.setMaxReadTimeMs(60000);
            context=(ServletHandlerContext)admin.addContext(null,"/");
            context.setRealm("Jetty Demo Realm");
            context.addAuthConstraint("/","server-administrator");
            context.addServlet("Admin","/","org.mortbay.servlet.AdminServlet");
            context.addServlet("Debug","/Debug/*","org.mortbay.servlet.Debug");
            admin.start();
        }
        catch(Exception e)
        {
            Code.fail(e);
        }
    }
}

