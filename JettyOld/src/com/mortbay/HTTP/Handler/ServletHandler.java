// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler;
import com.mortbay.Base.*;
import com.mortbay.HTTP.*;
import com.mortbay.HTML.*;
import java.io.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.*;


/* --------------------------------------------------------------------- */
/** ServletHandler<p>
 * This handler maps requests to servlets that implement the
 * javax.servlet.http.HttpServlet API.
 * It is configured with a PathMap of paths to ServletHolder instances.
 *
 * @see Interface.HttpHandler
 * @version $Id$
 * @author Greg Wilkins
 */
public class ServletHandler extends NullHandler 
{
    /* ----------------------------------------------------------------- */
    PathMap servletMap;
    Hashtable nameMap=null;
    String fileBase=null;
    
    /* ----------------------------------------------------------------- */
    /** Construct with servlet PathMap
     * @param servletMap Map of servlet path to ServletHolder instances
     */
    public ServletHandler(PathMap servletMap)
    {
	this.servletMap=servletMap;
    }
    
    /* ----------------------------------------------------------------- */
    /** Construct with servlet PathMap
     * @param servletMap Map of servlet path to ServletHolder instances
     * @param fileBase Used to calculate translatedPath from path info
     */
    public ServletHandler(PathMap servletMap,
			  String fileBase)
    {
	this.servletMap=servletMap;
	this.fileBase=fileBase;
    }
    
    /* ----------------------------------------------------------------- */
    public void handle(HttpRequest request,
		       HttpResponse response)
	 throws Exception
    {
	String address = request.getRequestPath();

	String path=servletMap.longestMatch(address);
	
	if (path != null)
	{
	    ServletHolder holder =
		(ServletHolder)servletMap.get(path);
	    
	    Code.debug("Pass request to servlet " + holder);

	    request.setServletPath(path);

	    // handle translated path
	    if (fileBase!=null)
	    {
		// XXX - use servlet version ????
		String filename = fileBase+request.getPathInfo();
		File file= new File(filename);
		request.setPathTranslated(file.getAbsolutePath());
	    }
	    
	    // service request
	    holder.service(request,response);
	}
    }
    
    /* ------------------------------------------------------------ */
    public void setServer(HttpServer server)
	 throws Exception
    {
	Enumeration h = servletMap.elements();
	while (h.hasMoreElements())
	{
	    ServletHolder holder = (ServletHolder)h.nextElement();
	    holder.setServer(server);
	}
	super.setServer(server);
    }
 
    /* ----------------------------------------------------------------- */
    public Enumeration servletNames()
    {
	if (nameMap==null)
	{
	    nameMap=new Hashtable();
	    Enumeration e = servletMap.elements();
	    while (e.hasMoreElements())
	    {
		ServletHolder holder = (ServletHolder)e.nextElement();
		nameMap.put(holder.toString(),holder);
	    }
	}
	return nameMap.keys();
    }
	
    /* ----------------------------------------------------------------- */
    public Servlet servlet(String name)
    {
	try{
	    servletNames();
	    ServletHolder holder = (ServletHolder)nameMap.get(name);
	    if (holder==null)
		return null;
	    return holder.getServlet();
	}
	catch(Exception e){
	    Code.warning(e);
	}
	return null;	
    }
}


