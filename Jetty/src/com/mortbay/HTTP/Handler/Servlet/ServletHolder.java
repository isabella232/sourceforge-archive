// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------


package com.mortbay.HTTP.Handler.Servlet;

import java.util.*;
import com.mortbay.HTTP.*;
import com.mortbay.Util.*;
import java.io.*;
import java.net.*;
import javax.servlet.*;
import java.util.*;
import java.lang.reflect.*;


/* --------------------------------------------------------------------- */
/** Servlet Holder
 * Holds the name, params and some state of a javax.servlet.GenericServlet
 * instance. It implements the ServletConfig interface.
 * This class will organise the loading of the servlet when needed or
 * requested.
 *
 * @see com.mortbay.HTTP.Handler.ServletHandler
 * @version $Id$
 * @author Greg Wilkins
 */
public class ServletHolder implements ServletConfig
{   
    /* ---------------------------------------------------------------- */
    private ServletHandler _handler;
    private Context _context;
    private boolean _singleThreadModel;
    
    private Class _servletClass=null;
    private Stack _servlets=new Stack();
    private GenericServlet _servlet=null;
    private String _name=null;    
    private String _className ;
    private Map _properties ;

    /* ---------------------------------------------------------------- */
    /** Construct a Servlet property mostly from the servers config
     * file.
     * @param handler ServletHandler 
     * @param className Servlet class name (fully qualified)
     */
    public ServletHolder(ServletHandler handler,
                         String className)
    {
	_handler=handler;
	_context=_handler.getContext();
        setServletName(className);
	_className=className;
    }
    
    /* ------------------------------------------------------------ */
    public String getServletName()
    {
	return _name;
    }
    
    /* ------------------------------------------------------------ */
    public void setServletName(String name)
    {
	synchronized(_handler)
	{
	    _handler.mapHolder(name,this,_name);
	    _name=name;
	}
    }

    /* ------------------------------------------------------------ */
    public String getClassName()
    {
	return _className;
    }
    
    /* ------------------------------------------------------------ */
    public void setClassName(String className)
    {
	_className = className;
    }

    /* ------------------------------------------------------------ */
    public Map getProperties()
    {
	return _properties;
    }

    /* ------------------------------------------------------------ */
    public void setProperties(Map properties)
    {
	_properties = properties;
    }

    /* ------------------------------------------------------------ */
    public void initialize()
    {
	try{
	    getServlet();
	}
	catch(javax.servlet.UnavailableException e)
	{
	    Code.warning(e);
	    throw new IllegalStateException(e.toString());
	}
    }
    

    /* ------------------------------------------------------------ */
    private void initializeClass()
	throws UnavailableException
    {
        try
        {
	    ServletLoader loader=_context.getHandler().getServletLoader();
	    if (loader==null)
		_servletClass=Class.forName(getClassName());
	    else
		_servletClass=loader.loadClass(getClassName());
	    Code.debug("Servlet Class ",_servletClass);
	    if (!javax.servlet.GenericServlet.class
		.isAssignableFrom(_servletClass))
		Code.fail("Servlet class "+getClassName()+
			  " is not a javax.servlet.GenericServlet");
        }
        catch(ClassNotFoundException e)
        {
            Code.debug(e);
            throw new UnavailableException(null,e.toString());
        }
    }
    
    /* ---------------------------------------------------------------- */
    /** Destroy.
     */
    public synchronized void destroy()
    {
        // Destroy singleton servlet
        if (_servlet!=null)
            _servlet.destroy();
        _servlet=null;
                
        // Destroy stack of servlets
        while (_servlets!=null && _servlets.size()>0)
        {
            Servlet s = (Servlet)_servlets.pop();
            s.destroy();
        }
        _servlets=new Stack();
	_servletClass=null;
    }
    
    
    /* ------------------------------------------------------------ */
    /** Get the servlet.
     * The state of the servlet is unknown, specially if using
     * SingleThreadModel
     * @return The servlet
     */
    public GenericServlet getServlet()
        throws UnavailableException
    {
        try{
	    if (_servletClass==null)
		initializeClass();
	    
            if (_servlet==null)
            {
                GenericServlet newServlet =
                    newServlet = (GenericServlet)_servletClass.newInstance();
                newServlet.init(this);
                synchronized (this)
                {
		    _singleThreadModel =
			newServlet instanceof
			javax.servlet.SingleThreadModel;
		    
		    if (_servlet==null && !_singleThreadModel)
                        _servlet=newServlet;
                }
            }
            return _servlet;
        }
	catch(UnavailableException e)
	{
	    throw e;
	}
        catch(Exception e)
        {
            Code.warning(e);
            throw new UnavailableException(null,e.toString());
        }    
    }

    
    /* ---------------------------------------------------------------- */
    /** 
     * @return 
     */
    public javax.servlet.ServletContext getServletContext()
    {
        return (javax.servlet.ServletContext)_context;
    }

    /* ---------------------------------------------------------------- */
    /**
     * Gets an initialization parameter of the servlet.
     * @param name the parameter name
     */
    public String getInitParameter(String param)
    {
	if (_properties==null)
	    return null;
        Object obj = _properties.get(param);
        if (obj == null)
            return null;
        return obj.toString();
    }
    
    /* ------------------------------------------------------------ */
    public Enumeration getInitParameterNames()
    {
	if (_properties==null)
	    return Collections.enumeration(Collections.EMPTY_LIST);
	return Collections.enumeration(_properties.values());
    }
    
    /* --------------------------------------------------------------- */
    /** Service a request with this servlet.
     */
    public void handle(ServletRequest request,
		       ServletResponse response)
        throws ServletException,
               UnavailableException,
               IOException
    {
	GenericServlet useServlet=null;
	
	// reference pool to protect from reloads
	Stack pool=_servlets;
        
	if (_singleThreadModel)    
	{
	    // try getting a servlet from the pool of servlets
	    try{useServlet = (GenericServlet)pool.pop();}
	    catch(EmptyStackException e)
	    {
		// Create a new one for the pool
		try
		{
		    if (_servletClass==null)
			initializeClass();
	    
		    useServlet = 
                            (GenericServlet) _servletClass.newInstance();
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
	    if (_servlet == null)
	    {
		// no so get a lock on the class
		synchronized(this)
		{
		    // check if still not ready
		    if (_servlet == null)
		    {
			// no so build it
			try
			{
			    if (_servletClass==null)
				initializeClass();
			    useServlet = 
				(GenericServlet) _servletClass.newInstance();
			    useServlet.init(this);
			    _servlet = useServlet;
			    
			    _singleThreadModel =
				_servlet instanceof
				javax.servlet.SingleThreadModel;
			    if (_singleThreadModel)
				_servlet=null;
			}
			catch(UnavailableException e)
			{
			    throw e;
			}
			catch(Exception e)
			{
			    Code.warning(e);
			    useServlet = _servlet = null;
			}
		    }
		    else
			// yes so use it.
			useServlet = _servlet;
		}
	    }
	    else
		// yes so use it.
		useServlet = _servlet;
	}
	
	// Check that we got one in the end
	if (useServlet==null)
	    throw new UnavailableException(null,"Could not construct servlet");

	// Service the request
	try
	{
	    useServlet.service(request,response);
	    response.flushBuffer();
	}
	finally
	{
	    // Return to singleThreaded pool
	    if (_singleThreadModel && useServlet!=null)
		pool.push(useServlet);
	}
    }
    
    /* ------------------------------------------------------------ */
    /** Get the name of the Servlet
     * @return Servlet name
     */
    public String toString()
    {
        return _name;
    }
    
}
