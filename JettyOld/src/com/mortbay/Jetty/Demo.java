// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Jetty;

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


/* ------------------------------------------------------------------------ */
/** Demo Http Configuration
 * <P>This class hard code the HttpServer configuration for the Jetty
 * demonstration site.  It is expected that only in a few circumstances
 * would hard coding be a desirable way to configure the server, more often
 * BaseConfiguration will be extended to read the local format of config
 * used.
 *
 * <P>Usage<PRE>
 * java com.mortbay.Jetty.Demo [port]
 * </PRE>
 * Currently this must be run in the home directory of the Jetty release,
 * as there are reletive file paths to FileBase and the Src hierarchy coded
 * into the configuration.  A more diligant approach to configuration will
 * allow a position independant configuration to be written.
 *
 * @see com.mortbay.HTTP.HttpConfiguration
 * @version $Id$
 * @author Greg Wilkins
*/
public class Demo extends BaseConfiguration
{
    /* -------------------------------------------------------------------- */
    public Demo()
	 throws IOException
    {
	this(8080);
    }
    
    /* -------------------------------------------------------------------- */
    public Demo(int port)
	 throws IOException
    {	
	// Set default PageType
	Page.setDefaultPageType("com.mortbay.Jetty.JettyLaF");
	// Page.setDefaultPageType("com.mortbay.Jetty.TabLaF");

	// Listen at a single port on the localhost
	addresses=new InetAddrPort[1];
	addresses[0]=new InetAddrPort(port);
	
	// Configure handlers
	httpHandlersMap=new PathMap();

	// Create full stack of HttpHandlers at "/"
	HttpHandler[] httpHandlers = new HttpHandler[10];
	httpHandlersMap.put("/",httpHandlers);
	int h=0;

	// Log Handler
	PathMap logMap = new PathMap();
	HttpHandler log = new LogHandler(logMap,true, true);
	httpHandlers[h++] = log;
	logMap.put("",new OutputStreamWriter(System.out));
	
	// Parameter handler
	httpHandlers[h++] = new ParamHandler();
	
	// Session handler
	httpHandlers[h++] = new SessionHandler();

	// Authentication handler
	PathMap authMap = new PathMap();
	httpHandlers[h++] = new BasicAuthHandler(authMap);
	BasicAuthRealm realm = new BasicAuthRealm("Basic");
	BasicAuthRealm other = new BasicAuthRealm("Other");
	authMap.put("/Chat$",realm);
	authMap.put("/Auth%",realm);
	authMap.put("/Other%",other);
	realm.put("jetty","jetty");
	realm.put("tom","tom");
	realm.put("dick","dick");
	realm.put("harry","harry");
	other.put("other","other");
	
	// Translate handler
	PathMap transMap = new PathMap();
	httpHandlers[h++] = new TranslateHandler(transMap);
	transMap.put("/$","/Jetty");          // for home page
	transMap.put("/Auth%","/");           // for auth demo
	transMap.put("/Other%","/");          // for auth demo
	transMap.put("/Translate%","/Dump/"); // for dump demo
	transMap.put("/ProxyDump%","http://localhost:"+port+"/Dump/");
	                                      // translate for proxy demo
	
	// Filter handler
	PathMap filterMap = new PathMap();
	httpHandlers[h++] = new FilterHandler(filterMap);
	filterMap.put("/","com.mortbay.HTTP.Filter.HtmlFilter");
	
	// Servlet Handler
	PathMap servletMap= new PathMap();
	httpHandlers[h++] = new ServletHandler(servletMap,"FileBase");
	addServlets(servletMap);

	// File Handler
	if(!new File("FileBase").exists())
	    Code.fail("ERROR - Must be run in directory containing FileBase");

	httpHandlers[h++] = new FileHandler("FileBase");

	// Forward Handler
	PathMap forwardMap = new PathMap();
	httpHandlers[h++] = new ForwardHandler(forwardMap);
	
	// Forward for forward demo:
	forwardMap.put("/Forward%",new URL("http://localhost:"+port));
	
	// Forward for proxy demo. Can only get here with http: prefix
	// if translated earlier - otherwise would have ended up
	// in proxy stack.
	forwardMap.put("http:",new URL("http://localhost:"+(port+1)));

	
	// NotFound Handler
	httpHandlers[h++] = new NotFoundHandler();
	

	// Create short stack of HttpHandlers at "/javadoc%"
	httpHandlers = new HttpHandler[3];
	httpHandlersMap.put("/javadoc%",httpHandlers);
	h=0;
	httpHandlers[h++] = log;
	httpHandlers[h++] = new FileHandler(".");
	httpHandlers[h++] = new NotFoundHandler();
    }

    /* -------------------------------------------------------------------- */
    /** Add servlet configuration to a pathMap
     */
    void addServlets(PathMap servletMap)
    {
	Hashtable params = null;
	ServletHolder holder=null;
	

	// Add various Dump servlets
	params = new Hashtable();
	params.put("param","foo");
	params.put("propertyB","bar");
	ServletHolder dumpHolder =
	    new ServletHolder("Dump","com.mortbay.Servlets.Dump",
			      params,false);	
	servletMap.put("/Dump",dumpHolder);
	servletMap.put("/Session",
		       new ServletHolder("SessionDump",
					 "com.mortbay.Servlets.SessionDump"));
	servletMap.put("/Config",
		       new ServletHolder("ConfigDump",
					 "com.mortbay.Servlets.ConfigDump"));
	servletMap.put("/Debug",
		       new ServletHolder("Debug",
					 "com.mortbay.Servlets.DebugServlet"));

	// Add Sunsoft's HelloWorld
	servletMap.put("/Hello",
		       new ServletHolder("Hello","HelloWorldServlet"));

	// Chat Servlet
	ServletHolder chat=
	    new ServletHolder("Chat","com.mortbay.Servlets.Chat");
	servletMap.put("/Chat",chat);
	servletMap.put("/ChatNoAuth",chat);
	
	// Generate HTML servlets
	servletMap.put("/Demo/push",
		       new ServletHolder("Multi",
					 "com.mortbay.Jetty.MultiPartCount"));
	servletMap.put("/Demo/generate",
		       new ServletHolder("Generate",
					 "com.mortbay.Jetty.GenerateServlet"));
	servletMap.put("/Demo/generateLaf",
		       new ServletHolder("GenerateLAf",
					 "com.mortbay.Jetty.GenerateLafServlet"));

	
	// Look and Feel wrapper servlet.
	params = new Hashtable();
	params.put("FileBase","FileBase");
	holder = new ServletHolder("LafWrapper",
				   "com.mortbay.Servlets.LookAndFeelServlet",
				   params,true);
	servletMap.put("/Jetty/Demo/",holder);
	servletMap.put("/Jetty/Info/",holder);
	servletMap.put("/Jetty/Config/",holder);
	servletMap.put("/Jetty/Program/",holder);
	


	// Build the index
	// This was designed to be read from a config file, so it is a
	// bit big for direct coding and a very bad way of doing this.

	PathMap index = new PathMap();
	
	Hashtable section = new Hashtable();
	section.put(Page.Title,"Jetty - Java HTTP Server");
	section.put(Page.Heading,"<B>Jetty</B><BR>Java HTTP Server");
	section.put(Page.Section,"Home");
	section.put("Text",
		    "Jetty is a HTTP Server written entirely in "+
		    "Java. It is designed to be embedable, extensible and "+
		    "flexible, thus making it an ideal platform for serving " +
		    "dynamic HTTP requests from any Java application.  "+
		    "Jetty supports the javax.servlet API defined "+
		    "by JavaSoft plus many more features.");
	String [][] jettyItems =
	{
	    {   "General Information",
		"/Jetty/Info",
		"Information about Jetty, it's features and license"
	    },
	    {   "Demonstrations",
		"/Jetty/Demo",
		"Demonstrations of the standard and advanced features of Jetty"
	    },
	    {   "Programming",
	        "/Jetty/Program",
	        "Introduction to programming Jetty and the API reference"
	    },
	    {   "Configuration",
		"/Jetty/Config",
		"How to Configure Jetty"
	    }
	};
	section.put("Items",jettyItems);
	index.put("/Jetty|",section);
	
	section = new Hashtable();
	section.put(Page.Heading,"Jetty Information");
	section.put(Page.Section,"Info");
	String [][] infoItems =
	{
	    {   "Features",
		"/Jetty/Info/features.html",
		"So what's Cool about Jetty?"
	    },
	    {   "History",
		"/Jetty/Info/history.html",
		"In the beginning, there was ..."
	    },
	    {   "Support",
		"/Jetty/Info/support.html",
		"Support services and consulting."
	    },
	    {   "License",
		"/Jetty/Info/license.html",
		"Introducing the concept of Guilt-ware"
	    }
	};
	section.put("Items",infoItems);
	index.put("/Jetty/Info|",section);
	
	section = new Hashtable();
	section.put(Page.Heading,"Jetty Demonstrations");
	section.put(Page.Section,"Demo");
	String [][] demoItems =
	{
	    {   "Basic Features",null,null
	    },
	    {   "Serving Files",
		"/Jetty/Demo/file.html",
		"Normal HTTP serving of files"
	    },
	    {   "Serving Servlet",
		"/Jetty/Demo/servlet.html",
		"Supports the javax.servlet.http API"
	    },
	    {   "Dump a Request",
		"/Jetty/Demo/dump.html",
		"Show the data available on each request"
	    },
	    {   "Dynamic Features",null,null
	    },
	    {   "HTML",
		"/Jetty/Demo/generate.html",
		"Generation of HTML"
	    },
	    {   "Look and Feel",
		"/Jetty/Demo/lookAndFeel.html",
		"Generation of common page layout."
	    },
	    {   "Menu Generation",
		"/Jetty/Demo/indexServlet.html",
		"Generation of Menu pages (like this one)"
	    },
	    {   "Authentication",
		"/Jetty/Demo/auth.html",
		"Basic authentication support"
	    },
	    {   "Server Push",
		"/Jetty/Demo/push.html",
		"Server push and multi part MIME"
	    },
	    {   "Chat Room",
		"/Jetty/Demo/chat.html",
		"A chat room that demonstrates the combination of frames,"+
		"<BR>look&feels, authentication and server push."
	    },
	    {   "Advanced Features",null,null
	    },
	    {   "Sessions",
		"/Jetty/Demo/sessions.html",
		"User and Browser sessions maintained with Cookies"
	    },
	    {   "Includes",
		"/Jetty/Demo/include.html",
		"Generated Server-side includes"
	    },
	    {   "Filters",
		"/Jetty/Demo/filter.html",
		"Filtered Server-side includes and generic response filtering"
	    },
	    {
		"Embed URL",
		"/Jetty/Demo/embed.html",
		"Server-side embedding of a URL fetched document"
	    },
	    {   "Forwarding",
		"/Jetty/Demo/forward.html",
		"Forwarding a request to another server"
	    },
	    {   "Proxy Server",
		"/Jetty/Demo/proxy.html",
		"Handling a proxy request"
	    },
	    {
		"Debugging",
		"/Debug",
		"Servlet to set debug output"
	    }
	};
	section.put("Items",demoItems);
	index.put("/Jetty/Demo|",section);
	
	section = new Hashtable();
	section.put(Page.Heading,"Jetty Programming");
	section.put(Page.Section,"Program");
	String [][] progItems =
	{
	    {   "Overview",
		"/Jetty/Program/overview.html",
		"The general approach for developing sites with servlets"
	    },
	    {   "Handlers Vs. Servlets",
		"/Jetty/Program/handlerVservlet.html",
		"When to write a Handlers and when to write a Servlet"
	    },
	    {   "Generated Vs. Templated",
		"/Jetty/Program/generatedVtemplate.html",
		"HTML generation and HTML authoring"
	    },
	    {   "API Documentation",
		"/javadoc/packages.html",
		"The generated class reference documentation."
	    }
	};
	section.put("Items",progItems);
	index.put("/Jetty/Program|",section);
	
	section = new Hashtable();
	section.put(Page.Heading,"Jetty Configuration");
	section.put(Page.Section,"Config");
	String [][] configItems =
	{
	    {   "Overview",
		"/Jetty/Config/overview.html",
		"The Jetty configuration approach"
	    },
	    {   "PathMap",
		"/Jetty/Config/pathMap.html",
		"Utility configuration dictionary for selecting object by request path"
	    },
	    {   "Debug options",
		"/Jetty/Config/debug.html",
		"Controlling the debug output"
	    },
	    {   "Demo server configuration",
		"/Jetty/Config/current.html",
		"This servers configuration"
	    }
	};
	section.put("Items",configItems);
	index.put("/Jetty/Config|",section);
		
	attributes.put("demoIndex",index);
	params = new Hashtable();
	params.put("indexAttr","demoIndex");
	holder=
	    new ServletHolder("index",
			      "com.mortbay.Servlets.IndexServlet",
			      params,
			      true);
	servletMap.put("/Jetty|",holder);
	servletMap.put("/Jetty/Info|",holder);
	servletMap.put("/Jetty/Demo|",holder);
	servletMap.put("/Jetty/Program|",holder);
	servletMap.put("/Jetty/Config|",holder);
	    
    }
    
    /* -------------------------------------------------------------------- */
    /** Sample Main
     * Configures the Dump servlet and starts the server
     */
    public static void main(String args[])
    {
	try{
	    int port = 8080;
	    if (args.length==1)
		port = Integer.parseInt(args[0]);
	    
	    HttpServer httpServer = new HttpServer(new Demo(port));
	    HttpServer proxyServer = new HttpServer(new ProxyConfig(port+1));
	    httpServer.join();
	    proxyServer.join();
	}
	catch(Exception e){
	    Code.warning("Demo Failed",e);
	}
    }
}
