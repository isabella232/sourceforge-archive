// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;

import com.mortbay.Util.*;
import com.sun.java.util.collections.*;
import java.io.*;
import java.net.*;
import java.util.*;
import com.mortbay.HTTP.Handler.*;
import com.mortbay.HTTP.Handler.Servlet.*;
import java.lang.reflect.*;

/* ------------------------------------------------------------ */
/** Context for a collection of HttpHandlers.
 * Handler Context provides an ordered container for HttpHandlers
 * that share the same path prefix, filebase, resourcebase and/or
 * classpath.
 * <p>
 * A HandlerContext is analageous to a ServletContext in the
 * Servlet API, except that it may contain other types of handler
 * other than servlets.
 * <p>
 * Convenience methods are provided for adding file and servlet
 * handlers.
 *
 * @see HttpServer
 * @see HttpHandler
 * @version 1.0 Sat Jun 17 2000
 * @author Greg Wilkins (gregw)
 */
public class HandlerContext
{
    private HttpServer _httpServer;
    private List _handlers=new ArrayList(3);
    private ServletHandler _servletHandler;
    private String _fileBase;
    private String _classPath;
    private String _resourceBase;
    private Map _attributes = new HashMap(11);
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param httpServer 
     */
    HandlerContext(HttpServer httpServer)
    {
	_httpServer=httpServer;
    }

    
    /* ------------------------------------------------------------ */
    public HttpServer getHttpServer()
    {
	return _httpServer;
    }

    /* ------------------------------------------------------------ */
    public String getFileBase()
    {
	return _fileBase;
    }

    /* ------------------------------------------------------------ */
    /** Sets the file base for the context.
     * Also sets the com.mortbay.HTTP.fileBase context attribute
     * @param fileBase 
     */
    public void setFileBase(String fileBase)
    {
	_fileBase = fileBase;
	_attributes.put("com.mortbay.HTTP.fileBase",fileBase);
    }

    /* ------------------------------------------------------------ */
    public String getClassPath()
    {
	return _classPath;
    }
    
    /* ------------------------------------------------------------ */
    /** Sets the class path for the context.
     * Also sets the com.mortbay.HTTP.classPath context attribute
     * @param fileBase 
     */
    public void setClassPath(String classPath)
    {
	_classPath = classPath;
	_attributes.put("com.mortbay.HTTP.classPath",classPath);
    }

    /* ------------------------------------------------------------ */
    public String getResourceBase()
    {
	return _resourceBase;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * If a relative file is passed, it is converted to a file
     * URL based on the current working directory.
     * Also sets the com.mortbay.HTTP.resouceBase context attribute
     * @param resourceBase 
     */
    public void setResourceBase(String resourceBaseUrl)
    {
	if (resourceBaseUrl!=null)
	{
	    if( resourceBaseUrl.startsWith("./"))
		_resourceBase =
		    "file:"+
		    System.getProperty("user.dir")+
		    resourceBaseUrl.substring(1);
	    else if (resourceBaseUrl.startsWith("/"))
		_resourceBase = "file:"+resourceBaseUrl;
	    else
		_resourceBase = resourceBaseUrl;
	}
	else
	    _resourceBase = resourceBaseUrl;
	_attributes.put("com.mortbay.HTTP.resourceBase",_resourceBase);
    }
    
    
    /* ------------------------------------------------------------ */
    /** Get all handlers
     * @return 
     */
    public List getHandlers()
    {
	return _handlers;
    }

    /* ------------------------------------------------------------ */
    /** Get handler by index.
     * @param i 
     * @return 
     */
    public HttpHandler getHandler(int i)
    {
	return (HttpHandler)_handlers.get(i);
    }
    
    /* ------------------------------------------------------------ */
    /** Get a handler by class.
     * @param handlerClass 
     * @return The first handler that is an instance of the handlerClass
     */
    public synchronized HttpHandler getHandler(Class handlerClass)
    {
	for (int h=0;h<_handlers.size();h++)
	{
	    HttpHandler handler = (HttpHandler)_handlers.get(h);
	    if (handlerClass.isInstance(handler))
		return handler;
	}
	return null;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param i 
     * @param handler 
     */
    public void setHandler(int i,HttpHandler handler)
    {
	_handlers.set(i,handler);
    }
    
    /* ------------------------------------------------------------ */
    /** Add a HttpHandler to the context.
     * @param handler 
     */
    public void addHandler(HttpHandler handler)
    {
	_handlers.add(handler);
	try
	{
	    handler.initialize(this);
	}
	catch(InterruptedException e)
	{
	    Code.warning(e);
	}
    }
    

    /* ------------------------------------------------------------ */
    /** Add a servlet to the context.
     * If no ServletHandler is found in the context, a new one is added.
     * @param pathSpec 
     * @param className 
     * @return The ServletHolder.
     * @exception ClassNotFoundException 
     * @exception InstantiationException 
     * @exception IllegalAccessException 
     */
    public synchronized ServletHolder addServlet(String pathSpec,
						 String className)
	throws ClassNotFoundException,
	       InstantiationException,
	       IllegalAccessException
    {
	if (_servletHandler==null)
	{
	    _servletHandler=getServletHandler();
	    if (_servletHandler==null)
	    {
		_servletHandler=new ServletHandler();
		addHandler(_servletHandler);
	    }
	}
	return _servletHandler.addServlet(pathSpec,className);
    }

    /* ------------------------------------------------------------ */
    public boolean isServingServlets()
    {
	return getServletHandler()!=null;
	
    }
    
    /* ------------------------------------------------------------ */
    public ServletHandler getServletHandler()
    {
	return (ServletHandler)
	    getHandler(com.mortbay.HTTP.Handler.Servlet.ServletHandler.class);
    }
    
    /* ------------------------------------------------------------ */
    /** Setup context for serving dynamic servlets.
     * @param serve If true and there is no DynamicHandler instance in the
     * context, a dynamicHandler is added. If false, all DynamicHandler
     * instances are removed from the context.
     */
    public synchronized void setServingDynamicServlets(boolean serve)
    {
	HttpHandler handler = (DynamicHandler)
	    getHandler(com.mortbay.HTTP.Handler.Servlet.DynamicHandler.class);
	if (serve)
	{
	    if (handler==null)
		addHandler(new DynamicHandler());
	}
	else if (handler!=null)
	    _handlers.remove(handler);
    }

    /* ------------------------------------------------------------ */
    public boolean isServingDynamicServlets()
    {
	return getDynamicHandler()!=null;
    }
    
    /* ------------------------------------------------------------ */
    public DynamicHandler getDynamicHandler()
    {
	return (DynamicHandler)
	    getHandler(com.mortbay.HTTP.Handler.Servlet.DynamicHandler.class);
    }
    
    /* ------------------------------------------------------------ */
    /** Setup context for serving files.
     * @param serve If true and there is no FileHandler instance in the
     * context, a FileHandler is added. If false, all FileHandler instances
     * are removed from the context.
     */
    public synchronized void setServingFiles(boolean serve)
    {
	FileHandler handler = (FileHandler)
	    getHandler(com.mortbay.HTTP.Handler.FileHandler.class);
	if (serve)
	{
	    if (handler==null)
		addHandler(new FileHandler());
	}
	else while (handler!=null)
	{
	    _handlers.remove(handler);
	    handler = (FileHandler)
	    getHandler(com.mortbay.HTTP.Handler.FileHandler.class);
	}
    }
    

    /* ------------------------------------------------------------ */
    public boolean isServingFiles()
    {
	return getFileHandler()!=null;
    }

    /* ------------------------------------------------------------ */
    public FileHandler getFileHandler()
    {
	return (FileHandler)
	    getHandler(com.mortbay.HTTP.Handler.FileHandler.class);
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param name 
     * @return 
     */
    public Object getAttribute(String name)
    {
	return _attributes.get(name);
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return 
     */
    public Enumeration getAttributeNames()
    {
	return Collections.enumeration(_attributes.keySet());
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param name 
     * @param value 
     */
    public void setAttribute(String name, Object value)
    {
	_attributes.put(name,value);
	if ("com.mortbay.HTTP.resourceBase".equals(name))
	    _resourceBase=value.toString();
	else if ("com.mortbay.HTTP.classPath".equals(name))
	    _classPath=value.toString();
	else if ("com.mortbay.HTTP.fileBase".equals(name))
	    _fileBase=value.toString();
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param name 
     */
    public void removeAttribute(String name)
    {
	_attributes.remove(name);
    }   
}










