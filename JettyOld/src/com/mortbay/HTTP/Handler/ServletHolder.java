// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler;

import com.mortbay.Base.*;
import com.mortbay.HTTP.*;
import com.mortbay.Util.*;
import java.io.*;
import java.net.*;
import javax.servlet.*;
import java.util.*;


/* --------------------------------------------------------------------- */
/** Servlet Holder
 * Holds the name, params and some state of a javax.servlet.GenericServlet
 * instance. It implements the ServletConfig interface.
 * This class will organise the loading of the servlet when needed or
 * requested.
 *
 * <p><h4>Note</h4>
 * By default, responses will only use chunking if requested by the
 * by setting the transfer encoding header.  However, if
 * the chunkByDefault is set, then chunking is
 * used if no content length is set.
 *
 * @see com.mortbay.HTTP.Handler.ServletHandler
 * @version $Id$
 * @author Greg Wilkins
 */
public class ServletHolder implements ServletConfig
{
    /* ---------------------------------------------------------------- */
    private String name;
    private Hashtable initParams;
    private HttpServer server;
    private boolean singleThreadModel;
    private boolean chunkByDefault=false;
    private String classPath=null;
    
    private Class servletClass=null;
    private Stack servlets=new Stack();
    private GenericServlet servlet=null;
    private ServletLoader loader=null;

    /* ---------------------------------------------------------------- */
    /** Construct a Servlet property mostly from the servers config
     * file.
     */
    public ServletHolder(String name,String className)
	throws ClassNotFoundException
    {
	this(name,className,"",null);
    }
    
    /* ---------------------------------------------------------------- */
    /** Construct a Servlet property mostly from the servers config
     * file.
     * @param name Servlet name
     * @param className Servlet class name (fully qualified)
     * @param initParams Hashtable of paramters
     */
    public ServletHolder(String name,
			 String className,
			 Hashtable initParams)
	throws ClassNotFoundException
    {
	this(name,className,"",initParams);
    }
    
    /* ---------------------------------------------------------------- */
    /** Construct a Servlet property mostly from the servers config
     * file.
     * @param name Servlet name
     * @param className Servlet class name (fully qualified)
     * @param classPath Servlet class path of directories and jars
     * @param initParams Hashtable of paramters
     */
    public ServletHolder(String name,
			 String className,
			 String classPath,
			 Hashtable initParams)
	throws ClassNotFoundException
    {
	this.name	= name;
	this.classPath  = classPath==null?"":classPath;
	this.initParams=initParams==null?new Hashtable(10):initParams;

	try
	{
	    loader = new ServletLoader(this.classPath);
	    servletClass = loader.loadClass(className,true);
	}
	catch(Exception e)
	{
	    Code.warning("Cannot find servlet class "+className,e);
	    throw new ClassNotFoundException(className);
	}

	if (!javax.servlet.GenericServlet.class.isAssignableFrom(servletClass))
	    Code.fail("Servlet class "+className+" is not a javax.servlet.GenericServlet");

	singleThreadModel =
	    javax.servlet.SingleThreadModel.class
	    .isAssignableFrom(servletClass);
    }
    
    /* ---------------------------------------------------------------- */
    /** Construct a Servlet property mostly from the servers config
     * file.
     * @param name Servlet name
     * @param className Servlet class name (fully qualified)
     * @param initParams Hashtable of paramters
     * @param initialize ignored
     * @deprecated initilized not used anymore
     */
    public ServletHolder(String name,
			 String className,
			 Hashtable initParams,
			 boolean initialize)
	throws ClassNotFoundException
    {
	this(name, className,"",initParams);
    }

    /* ---------------------------------------------------------------- */
    /** 
     * Construct a Servlet Holder from an already instantiated 
     * and initialized servlet.
     * This is useful for back-end applications that need to 
     * customize/initialize on their own a servlet front-end.
     *
     * @param name Servlet name
     * @param sl The servlet object
     */
    public ServletHolder(String name, GenericServlet sl)
    {
	this.name = name;
	this.initParams=new Hashtable(10);
	this.servletClass = sl.getClass();
	this.servlet = sl;
	singleThreadModel =
            javax.servlet.SingleThreadModel.class
            .isAssignableFrom(servletClass);
    }

    /* ------------------------------------------------------------ */
    /** Reload the servlet.
     * Reload the servlet and all associated classes by disposing of
     * the class loader instance.
     */
    public void reload()
    {
	if (servletClass.getClassLoader()!=loader)
	{
	    Code.warning("Cannot reload "+this);
	    return;
	}
	
	try
	{
	    String className=servletClass.getName();
	    loader = new ServletLoader(this.classPath);
	    servletClass = loader.loadClass(className,true);
	    servlets=new Stack();
	    servlet=null;
	}
	catch(Exception e)
	{
	    Code.warning(e);
	}
    }
    
    /* ---------------------------------------------------------------- */
    /** Set server
     */
    void setServer(HttpServer server)
	 throws ServletException,
	     ClassNotFoundException,
	     IllegalAccessException,
	     InstantiationException
    {
	Code.assert(this.server==null || this.server==server,
		    "Can't put ServletHolder in multiple servers");
	this.server = server;
    }

    /* ------------------------------------------------------------ */
    /** Get the servlet.
     * The state of the servlet is unknown, specially if using
     * SingleThreadModel
     * @return The servet
     */
    public GenericServlet getServlet()
	throws UnavailableException
    {
	try{
	    if (servlet==null)
	    {
		synchronized (ServletHolder.class)
		{
		    if (servlet==null)
		    {
			GenericServlet newServlet =
			    newServlet = (GenericServlet)
			    servletClass.newInstance();
			newServlet.init(this);
			servlet=newServlet;
		    }
		}
	    }
	    return servlet;
	}
	catch(Exception e)
	{
	    Code.warning(e);
	    throw new UnavailableException(null,e.toString());
	}    
    }
    
    /* ------------------------------------------------------------ */
    /** Set chunkByDefault
     * @param chunk If True, servlets without content lengths that
     * have not explicitly requested a closing connection, will use
     * HTTP/1.1 chunking if possible.
     */
    public void setChunkByDefault(boolean chunk)
    {
	chunkByDefault=chunk;
    }
    
    /* ------------------------------------------------------------ */
    /** Set chunkByDefault
     * @param chunk If True, servlets without content lengths that
     * have not explicitly requested a closing connection, will use
     * HTTP/1.1 chunking if possible.
     */
    public boolean getChunkByDefault()
    {
	return chunkByDefault;
    }
    
    
    /* ---------------------------------------------------------------- */
    /** Return server as the ServletContext.
     */
    public ServletContext getServletContext()
    {
	return server;
    }

    /* ---------------------------------------------------------------- */
    /**
     * Gets an initialization parameter of the servlet.
     * @param name the parameter name
     */
    public String getInitParameter(String param)
    {
	Object obj = initParams.get(param);
	if (obj == null)
	    return null;
	return obj.toString();
    }
    
    /* ---------------------------------------------------------------- */
    /**
     * Returns an enumeration of strings representing the initialization
     * parameters for this servlet.
     */
    public Enumeration getInitParameterNames()
    {
	return initParams.keys();
    }
	 

    /* ------------------------------------------------------------ */
    /** Service a request with this servlet.
     * @param request The request
     * @param response The response
     * @exception ServletException ServletException
     * @exception UnavailableException UnavailableException
     * @exception IOException IOException
     */
    public void service(HttpRequest request,
			HttpResponse response)
        throws ServletException,
	       UnavailableException,
	       IOException
    {
	response.setChunkByDefault(chunkByDefault);

	GenericServlet useServlet=null;
	
	if (singleThreadModel)    
	{
	    // try getting a servlet from the pool of servlets
	    try{useServlet = (GenericServlet)servlets.pop();}
	    catch(EmptyStackException e)
	    {
		// Create a new one for the pool
		try
		{
		    useServlet = 
			(GenericServlet) servletClass.newInstance();
		    useServlet.init(this);
		}
		catch(Exception e2)
		{
		    Code.warning(e2);
		    useServlet = null;
		}
	    }
	}
	else
	{
	    // Is the singleton instance ready?
	    if (servlet == null)
	    {
		// no so get a lock on the class
		synchronized(ServletHolder.class)
		{
		    // check if still not ready
		    if (servlet == null)
		    {
			// no so build it
			try
			{
			    useServlet = 
				(GenericServlet) servletClass.newInstance();
			    useServlet.init(this);
			    servlet = useServlet;
			}
			catch(Exception e)
			{
			    Code.warning(e);
			    useServlet = servlet = null;
			}
		    }
		}
	    }
	    else
		// yes so use it.
		useServlet = servlet;
	}

	// Check that we got one in the end
	if (useServlet==null)
	    throw new UnavailableException(null,"Could not construct servlet");

	// Service the request
	try
	{
	    useServlet.service(request,response);
	}
	finally
	{
	    // Return to singleThreaded pool
	    if (singleThreadModel && useServlet!=null)
		servlets.push(useServlet);
	}
    }

    /* ------------------------------------------------------------ */
    /** Get the name of the Servlet
     * @return Servlet name
     */
    public String toString()
    {
	return name;
    }
    
}






