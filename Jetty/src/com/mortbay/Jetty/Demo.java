// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Jetty;

import com.mortbay.HTTP.Handler.DumpHandler;
import com.mortbay.HTTP.HandlerContext;
import com.mortbay.HTTP.HashUserRealm;
import com.mortbay.HTTP.HttpServer;
import com.mortbay.HTTP.SecurityConstraint;
import com.mortbay.HTTP.SocketListener;
import com.mortbay.Util.Code;
import com.mortbay.Util.InetAddrPort;
import com.mortbay.Util.RolloverFileLogSink;


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
            HandlerContext context;

            // Realm
            HashUserRealm realm=
                new HashUserRealm("Jetty Demo Realm",
                                  "./etc/demoRealm.properties");
            
            // Make server
            HttpServer server = new HttpServer();
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
            server.addWebApplication(null,"/jetty/*",
                                     "webapps/jetty.war",
                                     "etc/webdefault.xml");
            
            context=server.getContext(null,"/demo/*");
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
            context.addServlet("JSP","*.jsp",
                               "org.apache.jasper.servlet.JspServlet");
            context.setServingResources(true);
            context.addHandler(new DumpHandler());
            
            context=server.addContext(null,"/javadoc/*");
            context.setResourceBase("javadoc/");
            context.setServingResources(true);

            context=server.addContext(null,"/cgi-bin/*");
            context.setResourceBase("cgi-bin/");
            context.addServlet("CGI","/","com.mortbay.Servlet.CGI")
                .put("Path","/bin:/usr/bin:/usr/local/bin");
            
            context=server.addContext(null,"/");
            context.setRealm("Jetty Demo Realm");
            context.addSecurityConstraint
                ("/admin/*",
                 new SecurityConstraint("admin","content-administrator"));
            context.setClassPath("servlets/");
            context.setDynamicServletPathSpec("/servlet/*");
            context.addServlet("Forward","/","com.mortbay.Servlet.Forward")
                .put("/","/jetty/index.html");
            context.addServlet("Admin","/admin/*","com.mortbay.HTTP.AdminServlet");
            
            
            // Logger
            RolloverFileLogSink log = new RolloverFileLogSink();
            log.setLogDir("logs");
            log.setRetainDays(90);
            log.setMultiDay(false);
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
            context=admin.addContext(null,"/");
            context.setRealm("Jetty Demo Realm");
            context.addSecurityConstraint
                ("/",
                 new SecurityConstraint("admin",
                                        "content-administrator"));
            context.addServlet("Admin","/","com.mortbay.HTTP.AdminServlet");
            admin.start();
        }
        catch(Exception e)
        {
            Code.fail(e);
        }
    }
}

