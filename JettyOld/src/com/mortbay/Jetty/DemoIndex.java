// ========================================================================
// Copyright (c) 1998 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Jetty;

import com.mortbay.Base.*;
import com.mortbay.Servlets.*;
import com.mortbay.HTML.*;
import com.mortbay.HTTP.*;
import java.util.*;

public class DemoIndex extends IndexServlet
{
    /* ------------------------------------------------------------ */
    public DemoIndex()
    {
	super(buildIndex());
    }
    
    /* ------------------------------------------------------------ */
    private static PathMap buildIndex()
    {
	PathMap index = new PathMap();

	Hashtable section = new Hashtable();
	section.put(Page.Title,"Jetty - Java HTTP Server");
	section.put(Page.Heading,"<B>Jetty</B><BR>Java HTTP Server");
	section.put(Page.Section,"Home");
	section.put("Text",
		    "Jetty is an <A HREF=http://www.opensource.org>Open Source</A> HTTP/1.1 Server written in 100% "+
		    "Java. It is designed to be embeddable, extensible and "+
		    "flexible, thus making it an ideal platform for serving " +
		    "dynamic HTTP requests from any Java application.  "+
		    "Jetty supports the <A HREF=/javadoc/javax/servlet/overview-summary.html>javax.servlet&nbsp;API</A> defined "+
		    "by <A HREF=http://www.javasoft.com>JavaSoft</A> plus many more features.");
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
		"Open Source License"
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
	    {   "File upload",
		"/Jetty/Demo/upload.html",
		"Handle multi part MIME requests"
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
	    },
	    {
		"Servlet Dispatch",
		"/Jetty/Demo/servletdispatch.html",
		"Servlet utility class for URL to Object mapping"
	    },
	    {
		"Property Trees",
		"/Jetty/Demo/propertytree.html",
		"Nested Properties objects with wildcard default values"
	    },
	    {
		"Java Server Pages",
		"/Jetty/Demo/jsp.html",
		"GNUJSP implementation of Java Server Pages"
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
	    {   "Jetty API Documentation",
		"/javadoc/overview-summary.html",
		"The generated class reference documentation."
	    },
	    {   "Servlet API Documentation",
		"/javadoc/javax/servlet/overview-summary.html",
		"The class reference for the standard API."
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

	return index;
    }	
};
