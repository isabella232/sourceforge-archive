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
    String dynamicLoader=null;
    Hashtable nameMap=null;
    boolean autoReloadDynamicServlets=true;
    String pathTranslated=null;
    
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
     * Loader : className             # ServletLoader for dynamic servlets
     * PathTranslated : path          # Optional prefix for getPathTranslated
     * SERVLET.name.CLASS: className  # Class of servlet
     * SERVLET.name.CLASSPATH: path   # CLASSPATH to load servlet from
     * SERVLET.name.PATHS: /path      # Servlet path
     * SERVLET.name.CHUNK: False      # Should servlet HTTP/1.1 chunk by default
     * SERVLET.name.PROPERTY.key:val  # Servlet property
     * SERVLET.name.PROPERTIES: file  # File of servlet properties
     * SERVLET.name.Initialize: False # Initialize when loaded. 
     * SERVLET.name.Loader: className # ServletLoader for servlet
     *
     * </PRE>
     * @param properties Configuration.
     * @exception java.io.IOException
     */
    public synchronized void setProperties(Properties properties)
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
	dynamicLoader = tree.getProperty("Loader");
	autoReloadDynamicServlets = tree.getBoolean("AutoReloadDynamicServlets");
	pathTranslated = tree.getProperty("PathTranslated");
	
	if (dynamicPaths!=null && dynamicClassPath!=null &&
	    dynamicPaths.size()>0 && dynamicClassPath.length()>0)
	{
	    // check path;
	    new FileJarServletLoader(dynamicClassPath);
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
		if ("*".equals(servletName))
		    continue;
		
		Code.debug("Configuring servlet "+servletName);
		PropertyTree servletTree = servlets.getTree(servletName);
		
		String servletClass = servletTree.getProperty("CLASS");
		String servletClassPath = servletTree.getProperty("CLASSPATH");
		String servletLoaderName = servletTree.getProperty("Loader");
		Properties servletProperties = getProperties(servletTree);
		ServletHolder servletHolder=
		    new ServletHolder(servletLoaderName,
				      servletName,
				      servletClass,
				      servletClassPath,
				      servletProperties);
		
		boolean chunk = servletTree.getBoolean("CHUNK");
		servletHolder.setChunkByDefault(chunk);
		Vector paths = servletTree.getVector("PATHS",",;");
		for (int p=paths.size();p-->0;)
		    servletMap.put(paths.elementAt(p),servletHolder);
		servletHolder.setInitialize(servletTree.getBoolean("Initialize"));
	    }
	    catch(ClassNotFoundException e)
	    {
		Code.warning(e);
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
	    if (pathTranslated!=null && request.getPathInfo()!=null)
	    {
		request.setPathTranslated(pathTranslated+
					  request.getPathInfo()
					  .replace('/',File.separatorChar));
	    }
	    
	    
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
	    pathSpec=PathMap.match(pathSpec,address)+servletClass+"%";
	    
	    if (servletClass.endsWith(".class"))
		servletClass=servletClass.substring(0,servletClass.length()-6);


	    Log.event("Dynamic load "+servletClass);
	    ServletHolder holder=null;
	    synchronized(this)
	    {
		if (servletMap!=null)
		{
		    holder= new ServletHolder(dynamicLoader,
					      servletClass,
					      servletClass,
					      dynamicClassPath,
					      null);
		    holder.setServer(httpServer);
		    holder.setAutoReload(autoReloadDynamicServlets);
		    servletMap.put(pathSpec,holder);
		}
	    }
	    
	    // service request
	    if (holder!=null)
	    {
		request.setServletPath(pathSpec);
		holder.service(request,response);
	    }
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
    
    /* ------------------------------------------------------------ */
    /** Destroy Handler.
     * Destroy all servlets
     */
    public synchronized void destroy()
    {
	Enumeration e = servletMap.elements();
	while (e.hasMoreElements())
	{
	    ServletHolder holder = (ServletHolder)e.nextElement();
	    holder.destroy();
	}
    }
}




