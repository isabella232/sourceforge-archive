// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler;
import com.mortbay.Base.*;
import com.mortbay.HTTP.*;
import com.mortbay.Util.PropertyTree;
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
    PathMap dynamicMap=null;
    String dynamicClassPath=null;
    Hashtable nameMap=null;
    boolean autoReloadDynamicServlets=true;
    
    /* ----------------------------------------------------------------- */
    /** Construct basic auth handler.
     * @param properties Passed to setProperties
     */
    public ServletHandler(Properties properties)
	throws IOException
    {
	setProperties(properties);
    }
    
    /* ----------------------------------------------------------------- */
    /** Construct with servlet PathMap
     * @param servletMap Map of servlet path to ServletHolder instances
     */
    public ServletHandler(PathMap servletMap)
    {
	this.servletMap=servletMap;
    }

    /* ------------------------------------------------------------ */
    /** Configure from Properties.
     * Properties treated as a PropertyTree with the following fields: <PRE>
     * PATHS : /servlet/:/SERVLET/    # URL Paths for dynamic servlet loading
     * CLASSPATH : ./servlets:        # CLASS Paths for dynamic servlet loading
     * AutoReloadDynamicServlets: True# Should dynamic servlets auto reload 
     * SERVLET.name.CLASS: className  # Class of servlet
     * SERVLET.name.PATHS: /path      # Servlet path
     * SERVLET.name.CHUNK: False      # Should servlet HTTP/1.1 chunk by default
     * SERVLET.name.PROPERTY.key:val  # Servlet property
     * SERVLET.name.PROPERTIES: file  # File of servlet properties
     * </PRE>
     * @param properties Configuration.
     */
    public void setProperties(Properties properties)
	throws IOException
    {
	servletMap=new PathMap();
	
	PropertyTree tree=null;
	if (properties instanceof PropertyTree)
	    tree = (PropertyTree)properties;
	else
	    tree = new PropertyTree(properties);
	Code.debug(tree);

	// Setup dynamic servlets
	Vector dynamicPaths = tree.getVector("PATHS",";");
	dynamicClassPath = tree.getProperty("CLASSPATH");
	autoReloadDynamicServlets = tree.getBoolean("AutoReloadDynamicServlets");
	if (dynamicPaths!=null && dynamicClassPath!=null &&
	    dynamicPaths.size()>0 && dynamicClassPath.length()>0)
	{
	    // check path;
	    new ServletLoader(dynamicClassPath);
	    dynamicMap = new PathMap();
	    for (int p=dynamicPaths.size();p-->0;)
		dynamicMap.put(dynamicPaths.elementAt(p),"");
	}
	
	// Load configured servlets
	PropertyTree servlets = tree.getTree("SERVLET");
	Enumeration names = servlets.getNodes();
	while (names.hasMoreElements())
	{
	    try{
		String servletName = names.nextElement().toString();
		Code.debug("Configuring servlet "+servletName);
		PropertyTree servletTree = servlets.getTree(servletName);
		
		String servletClass = servletTree.getProperty("CLASS");
		String servletClassPath = servletTree.getProperty("CLASSPATH");
		
		Properties servletProperties = getProperties(servletTree);
		ServletHolder servletHolder= new ServletHolder(servletName,
							       servletClass,
							       servletClassPath,
							       servletProperties);
		
		boolean chunk = servletTree.getBoolean("CHUNK");
		servletHolder.setChunkByDefault(chunk);
		
		Vector paths = servletTree.getVector("PATHS",",;");
		for (int p=paths.size();p-->0;)
		    servletMap.put(paths.elementAt(p),servletHolder);
	    }
	    catch(ClassNotFoundException e)
	    {
		Code.ignore(e);
	    }
	}
    }
    
    
    /* ----------------------------------------------------------------- */
    public void handle(HttpRequest request,
		       HttpResponse response)
	 throws Exception
    {
	String address = request.getResourcePath();

	String pathSpec=servletMap.matchSpec(address);


	// try a known servlet
	if (pathSpec != null)
	{
	    ServletHolder holder =
		(ServletHolder)servletMap.get(pathSpec);
	    
	    Code.debug("Pass request to servlet " + holder);

	    request.setServletPath(pathSpec);
	    
	    // service request
	    holder.service(request,response);
	    return;
	}

	if (dynamicMap==null)
	    return;
	
	if (Code.verbose())
	    Code.debug("Looking for "+address +" in "+dynamicMap);
	
	// try a dynamic servlet
	pathSpec=dynamicMap.matchSpec(address);
	if (pathSpec!=null)
	{
	    String servletClass=PathMap.pathInfo(pathSpec,address);
	    int slash=servletClass.indexOf("/");
	    if (slash>=0)
		servletClass=servletClass.substring(0,slash);
	    
	    if (servletClass.endsWith(".class"))
		servletClass=servletClass.substring(0,servletClass.length()-6);
	    Log.event("Dynamic load "+servletClass);
	    
	    ServletHolder holder= new ServletHolder(servletClass,
						    servletClass,
						    dynamicClassPath,
						    null);
	    holder.setServer(httpServer);
	    holder.setAutoReload(autoReloadDynamicServlets);
	    servletMap.put(PathMap.match(pathSpec,address)+servletClass+"%",
			   holder);
	    
	    // service request
	    request.setServletPath(pathSpec);
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


