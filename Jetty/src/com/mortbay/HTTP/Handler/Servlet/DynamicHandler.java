// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler.Servlet;
//import com.sun.java.util.collections.*; XXX-JDK1.1

import com.mortbay.HTTP.*;
import com.mortbay.Util.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import java.net.URL;


/* --------------------------------------------------------------------- */
/** ServletHandler<p>
 * This handler maps requests to servlets that implement the
 * javax.servlet.http.HttpServlet API.
 * It is configured with a PathMap of paths to ServletHolder instances.
 *
 * @version $Id$
 * @author Greg Wilkins
 */
public class DynamicHandler extends ServletHandler 
{
    private Set _paths = new HashSet();
	
    /* ------------------------------------------------------------ */
    Map _properties ;
    public Map getProperties()
    {
	return _properties;
    }
    public void setProperties(Map properties)
    {
	_properties = properties;
    }
    
    /* ----------------------------------------------------------------- */
    public DynamicHandler()
    {}
    
    /* ----------------------------------------------------------------- */
    public void start()
    {
        Log.event("DynamicHandler started for "+
		  getHandlerContext().getClassPath());
	super.start();
    }
    
    /* ----------------------------------------------------------------- */
    public void handle(String contextPathSpec,
		       HttpRequest httpRequest,
                       HttpResponse httpResponse)
         throws IOException
    {
	// Try a previously added servlet
	super.handle(contextPathSpec,httpRequest,httpResponse);
	
	// Return if the response has been updated
	if (httpResponse.getState()!=HttpMessage.__MSG_EDITABLE)
	    return;
	
	// try a dynamic servlet
	synchronized(this)
	{
	    String path = httpRequest.getPath();
	    if (!_paths.contains(path))
	    {
		_paths.add(path);
		Code.debug(path," from ",
			   getHandlerContext().getClassPath());
		
		String servletClass=
		    PathMap.pathInfo(contextPathSpec,path).substring(1);
		int slash=servletClass.indexOf("/");
		if (slash>=0)
		    servletClass=servletClass.substring(0,slash);            
		if (servletClass.endsWith(".class"))
		servletClass=servletClass.substring(0,servletClass.length()-6);
		
		path="/"+servletClass;
		
		ServletHolder holder=null;
		try{
		    holder=newServletHolder(servletClass);
		    holder.setProperties(getProperties());
		    holder.getServlet();
		}
		catch(Exception e)
		{
		    Code.ignore(e);
		    return;
		}
		    
		Log.event("Dynamic load '"+servletClass+"' at "+path);
		addHolder(path,holder);
		addHolder(path+".class",holder);
		addHolder(path+"/*",holder);
		addHolder(path+".class/*",holder);
	    }
            
	}
	
	// service request
	super.handle(contextPathSpec,httpRequest,httpResponse);
    }   
}

