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
     * JRun.Properties : file         # handle Servlet-Runner and JRUN properties
     * JRun.Mappings : file           # handle Servlet-Runner and JRUN properties
     * SERVLET.name.CLASS: className  # Class of servlet
     * SERVLET.name.PATHS: /path      # Servlet path
     * SERVLET.name.CHUNK: False      # Should servlet HTTP/1.1 chunk by default
     * SERVLET.name.PROPERTY.key:val  # Servlet property
     * SERVLET.name.PROPERTIES: file  # File of servlet properties
     * SERVLET.name.Initialize: False # Initialize when loaded. 
     * SERVLET.name.Loader: className # ServletLoader for servlet
     * </PRE>
     * @param properties Configuration.
     * @exception java.io.IOException
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
	dynamicLoader = tree.getProperty("Loader");
	autoReloadDynamicServlets = tree.getBoolean("AutoReloadDynamicServlets");
	if (dynamicPaths!=null && dynamicClassPath!=null &&
	    dynamicPaths.size()>0 && dynamicClassPath.length()>0)
	{
	    // check path;
	    new FileJarServletLoader(dynamicClassPath);
	    dynamicMap = new PathMap();
	    for (int p=dynamicPaths.size();p-->0;)
		dynamicMap.put(dynamicPaths.elementAt(p),"");
	}

	// check if we have Servlet-Runner Style Properties
        String propFilename = tree.getProperty("JRun.Properties");
        String mappFilename = tree.getProperty("JRun.Mappings");
        if ( propFilename != null && mappFilename != null )
        {
            // we have Servlet-Runner Style Properties
            setJRunProperties( propFilename, mappFilename );
            return;
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
	    
	    ServletHolder holder= new ServletHolder(dynamicLoader,
						    servletClass,
						    servletClass,
						    dynamicClassPath,
						    null);
	    holder.setServer(httpServer);
	    holder.setAutoReload(autoReloadDynamicServlets);
	    servletMap.put(pathSpec,holder);
	    
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



    /* ------------------------------------------------------------ */
    /** Set JRun properties
     * @author klausspd@hotmail.com
     * @param propFilename File of servlet properties like: <PRE>
     *    dump.code = com.mortbay.Servlets.Dump
     *    dump.args = arg1=value1,arg2=value2
     *    dump.preload = false
     * </PRE>
     * @param mappFilename File of URI mappings.
     * @exception IOException 
     */
    public void setJRunProperties(String propFilename, String mappFilename)
        throws IOException
    {
        Properties servletProps = new Properties();
        Properties mappingsProps = new Properties();
        try
        {
            if ( propFilename != null )
                servletProps.load( new BufferedInputStream(
                                   new FileInputStream( propFilename )));
            if ( mappFilename != null )
                mappingsProps.load( new BufferedInputStream(
                                    new FileInputStream( mappFilename )));
        }
        catch( IOException ioe )
        {
            Code.fail( "Can't read props files due to: " + ioe );
        }

        servletMap = new PathMap();
        
        // get all different servlet names from properties file
        // expecting somthing like:
        // dump.code = com.mortbay.Servlets.Dump
        // dump.args = arg1=value1,arg2=value2
        // dump.preload = false
        String servletName;
        Vector allServletNames = new Vector();
        for( Enumeration enum = servletProps.propertyNames();
	     enum.hasMoreElements(); )
        {
            String key = (String)enum.nextElement();
            try
            {
                servletName = key.substring(0, key.indexOf("."));
                if(!allServletNames.contains(servletName))
                    allServletNames.addElement(servletName);
            }
            catch( Exception ex )
            {
		Code.warning( "Malformed servlet property: " + key );
            }
        }

        // for all servletnames get the classnames,
        // initparameters and the preload option
        Hashtable servletParamTable = null;
        ServletHolder holder = null;
        String servletClass = null;
        String servletParam = null;
        String servletPreload = null;
        for( Enumeration enum = allServletNames.elements();
                enum.hasMoreElements(); )
        {
            servletName = (String)enum.nextElement();
            Code.debug( "reading props for servletName:" + servletName );

            // extracting classname
            servletClass = servletProps.getProperty( servletName + ".code" );
            if (servletClass == null)
                continue;
            Code.debug( "className:" + servletClass );
            
            // extracting servlet init parameters
            servletParam = servletProps.getProperty( servletName + ".args" );
            if ( servletParam != null )
            {
                servletParamTable = new Hashtable();
                for( StringTokenizer strT = new StringTokenizer(servletParam, ",", false);
                            strT.hasMoreTokens(); )
                {
                    String assignment = strT.nextToken();
                    int i = assignment.indexOf("=");
                    if(i > 0)
                    {
                        String key = assignment.substring(0, i).trim();
                        String value = assignment.substring(i + 1, assignment.length()).trim();
                        servletParamTable.put(key, value);
                    }
                }
            }
            
	    try {
                holder = new ServletHolder( servletName, servletClass, servletParamTable );
                Code.debug( "created holder:" + holder + " for servletName:" + servletName );
	    }
	    catch(ClassNotFoundException e)
	    {
		Code.warning(e);
	    }
            
            // we remember all servlets even if they don't
            // have a mapping (servletMap) they could be invoked
            // dynamically so we keep the initargs and might do a preload
            nameMap.put( servletName, holder );
            
            // preload servlet if asked for
            servletPreload = servletProps.getProperty( servletName + ".preload" );
            holder.setInitialize( servletPreload != null &&
                                    servletPreload.equalsIgnoreCase( "true" ) );
        }

        // all servlets that have a mapping in the mapping file
        // will be added to servletMap
        String path;
        for( Enumeration mappings = mappingsProps.propertyNames();
                mappings.hasMoreElements(); )
        {
            path = (String)mappings.nextElement();
            servletName = mappingsProps.getProperty(path).trim();
            Code.debug( "reading mapping:" + path + " for servletName:" + servletName );
            if ( path.startsWith( "." ) )
                path = "*" + path;
            holder = (ServletHolder)nameMap.get( servletName );
            // check if we know that servlet from the propFile
            if ( holder == null )
            {
                Code.debug( "holder is null for servletName:" + servletName );
                // we don't know a servlet with this name
                // from the properties file
                if ( servletName.equalsIgnoreCase( "invoker" ) )
                {
                    // but it is a mapping for the servlet invoker
                    // we are very strikt with the invoker mappings
                    // otherwise it gets to complicate
                    // a prefix mapping should look like this:
                    // /xxx/ -> URI: /xxx/ + servlet classname + pathinfo
                    // a suffix mapping should look like this:
                    // .yyy -> URI: /xyz/ + servlet classname + .yyy
                    if ( path.indexOf( "*" ) > 0 )
                        Code.fail( "Malformed invoker mapping:" + path );
                    else if ( ( path.startsWith( "/" ) && path.endsWith( "/" ) ) ||
                              ( path.startsWith( "*" ) && path.length() > 1 ) )
                    {
                        if ( dynamicMap == null )
                            dynamicMap = new PathMap();
                        dynamicMap.put( path, "" );
                    }
                    continue;
                }

                // we assume this mapping is for a servlet 
                // that can be found in the classpath
                // servletName = servletClass and no parameters
                try {    
                    holder = new ServletHolder( servletName, servletName );
                    nameMap.put( servletName, holder );
	        }
	        catch(ClassNotFoundException e)
	        {
		    Code.warning(e);
	        }
            }
          
            if ( holder != null )
            {
                // the class for this servlet is loaded
                // so we can remember it's name and path info
                Code.debug( "addServlet: " + servletName + " at " + path );
                servletMap.put( path, holder );
            }
        }
    }    
}




