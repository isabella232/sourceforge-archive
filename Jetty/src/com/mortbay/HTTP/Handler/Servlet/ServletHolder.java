// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler.Servlet;

import com.mortbay.Util.Code;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.SingleThreadModel;
import javax.servlet.UnavailableException;


/* --------------------------------------------------------------------- */
/** Servlet Holder
 * Holds the name, params and some state of a javax.servlet.Servlet
 * instance. It implements the ServletConfig interface.
 * This class will organise the loading of the servlet when needed or
 * requested.
 *
 * @see com.mortbay.HTTP.Handler.ServletHandler
 * @version $Id$
 * @author Greg Wilkins
 */
public class ServletHolder
    extends AbstractMap
{
    /* ---------------------------------------------------------------- */
    private ServletHandler _handler;
    private Context _context;
    private boolean _singleThreadModel;

    private Class _servletClass=null;
    private Stack _servlets=new Stack();
    private Servlet _servlet=null;
    private String _name=null;
    private String _className ;
    private Map _initParams ;
    private boolean _initOnStartup =false;
    private Config _config;
    private Map _roleMap;

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
        _config=new Config();
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
    public boolean isInitOnStartup()
    {
        return _initOnStartup;
    }

    /* ------------------------------------------------------------ */
    public void setInitOnStartup(boolean b)
    {
        _initOnStartup = b;
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
            ClassLoader loader=_context.getHandler().getClassLoader();
            if (loader==null)
                _servletClass=Class.forName(getClassName());
            else
                _servletClass=loader.loadClass(getClassName());
            Code.debug("Servlet Class ",_servletClass);
            if (!javax.servlet.Servlet.class
                .isAssignableFrom(_servletClass))
                Code.fail("Servlet class "+getClassName()+
                          " is not a javax.servlet.Servlet");
        }
        catch(ClassNotFoundException e)
        {
            Code.debug(e);
            throw new UnavailableException(e.toString());
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
    public Servlet getServlet()
        throws UnavailableException
    {
        try{
            if (_servletClass==null)
                initializeClass();

            if (_servlet==null)
            {
                Servlet newServlet =
                    newServlet = (Servlet)_servletClass.newInstance();
                newServlet.init(_config);
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
            throw new UnavailableException(e.toString());
        }    
    }


    /* ---------------------------------------------------------------- */
    public javax.servlet.ServletContext getServletContext()
    {
        return (javax.servlet.ServletContext)_context;
    }

    /* ------------------------------------------------------------ */
    public void setInitParameter(String param,String value)
    {
        put(param,value);
    }

    /* ---------------------------------------------------------------- */
    /**
     * Gets an initialization parameter of the servlet.
     * @param name the parameter name
     */
    public String getInitParameter(String param)
    {
        if (_initParams==null)
            return null;
        Object obj = _initParams.get(param);
        if (obj == null)
            return null;
        return obj.toString();
    }

    /* ------------------------------------------------------------ */
    public Enumeration getInitParameterNames()
    {
        if (_initParams==null)
            return Collections.enumeration(Collections.EMPTY_LIST);
        return Collections.enumeration(_initParams.keySet());
    }

    /* ------------------------------------------------------------ */
    /** Link a user role.
     * Translate the role name used by a servlet, to the link name
     * used by the container.
     * @param name The role name as used by the servlet
     * @param link The role name as used by the container.
     */
    public synchronized void setUserRoleLink(String name,String link)
    {
        if (_roleMap==null)
            _roleMap=new HashMap();
    }
    
    /* ------------------------------------------------------------ */
    /** get a user role link
     * @param name The name of the role
     * @return The name as translated by the link. If no link exists,
     * the name is returned.
     */
    public String getUserRoleLink(String name)
    {
        if (_roleMap==null)
            return name;
        String link=(String)_roleMap.get(name);
        return (link==null)?name:link;
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
        Servlet useServlet=null;

        // reference pool to protect from reloads
        Stack pool=_servlets;

        if (_singleThreadModel)
        {
            // try getting a servlet from the pool of servlets
            try{useServlet = (Servlet)pool.pop();}
            catch(EmptyStackException e)
            {
                // Create a new one for the pool
                try
                {
                    if (_servletClass==null)
                        initializeClass();

                    useServlet =
                            (Servlet) _servletClass.newInstance();
                    useServlet.init(_config);
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
                                (Servlet) _servletClass.newInstance();
                            useServlet.init(_config);
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
            throw new UnavailableException("Could not construct servlet");

        // Service the request
        try
        {
            useServlet.service(request,response);
            response.flushBuffer();
        }
        catch(UnavailableException e)
        {
            if (_singleThreadModel && useServlet!=null)
                useServlet.destroy();
            else
                destroy();
            useServlet=null;
            throw e;
        }
        finally
        {
            // Return to singleThreaded pool
            if (_singleThreadModel && useServlet!=null)
                pool.push(useServlet);
        }
    }

    /* ------------------------------------------------------------ */
    /** Map method.
     * ServletHolder implements the Map interface as a
     * configuration conveniance. The methods are mapped to the
     * servlet properties.
     * @return The entrySet of the initParameter map
     */
    public Set entrySet()
    {
        if (_initParams==null)
            _initParams=new HashMap(3);
        return _initParams.entrySet();
    }

    /* ------------------------------------------------------------ */
    /** Map method.
     * ServletHolder implements the Map interface as a
     * configuration conveniance. The methods are mapped to the
     * servlet properties.
     */
    public Object put(Object name,Object value)
    {
        if (_initParams==null)
            _initParams=new HashMap(3);
        return _initParams.put(name,value);
    }

    /* ------------------------------------------------------------ */
    /** Get the name of the Servlet
     * @return Servlet name
     */
    public String toString()
    {
        return _name;
    }
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    class Config implements ServletConfig
    {
        /* -------------------------------------------------------- */
        public String getServletName()
        {
            return ServletHolder.this.getServletName();
        }
        
        /* -------------------------------------------------------- */
        public javax.servlet.ServletContext getServletContext()
        {
            return (javax.servlet.ServletContext)_context;
        }

        /* -------------------------------------------------------- */
        /**
         * Gets an initialization parameter of the servlet.
         * @param name the parameter name
         */
        public String getInitParameter(String param)
        {
            return ServletHolder.this.getInitParameter(param);
        }
    
        /* -------------------------------------------------------- */
        public Enumeration getInitParameterNames()
        {
            return ServletHolder.this.getInitParameterNames();
        }
    }
}





