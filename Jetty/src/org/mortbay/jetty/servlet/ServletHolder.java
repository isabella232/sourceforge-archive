// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.jetty.servlet;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.UnavailableException;
import org.mortbay.util.Code;
import org.mortbay.util.URI;


/* --------------------------------------------------------------------- */
/** Servlet Instance and Context Holder.
 * Holds the name, params and some state of a javax.servlet.Servlet
 * instance. It implements the ServletConfig interface.
 * This class will organise the loading of the servlet when needed or
 * requested.
 *
 * @see org.mortbay.jetty.ServletHandler
 * @version $Id$
 * @author Greg Wilkins
 */
public class ServletHolder extends Holder
    implements Comparable
{
    /* ---------------------------------------------------------------- */
    private ServletHandler _servletHandler;
    private Stack _servlets;
    private Servlet _servlet;
    
    private int _initOrder;
    private boolean _initOnStartup=false;
    private Config _config;
    private Map _roleMap;
    private String _path;
    
    /* ---------------------------------------------------------------- */
    /** Constructor.
     */
    ServletHolder(ServletHandler handler,
                  String name,
                  String className)
    {
        super(handler,name,className);
    }

    /* ---------------------------------------------------------------- */
    /** Constructor. 
     */
    ServletHolder(ServletHandler handler,
                  String name,
                  String className,
                  String forcedPath)
    {
        this(handler,name,className);
        _path=forcedPath;
    }

    /* ------------------------------------------------------------ */
    /**
     * @deprecated Use getInitOrder()
     */
    public boolean isInitOnStartup()
    {
        return _initOrder!=0 || _initOnStartup;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @deprecated Use setInitOrder(int)
     */
    public void setInitOnStartup(boolean b)
    {
        _initOrder=b?0:-1;
    }
    
    /* ------------------------------------------------------------ */
    public int getInitOrder()
    {
        return _initOrder;
    }

    /* ------------------------------------------------------------ */
    /** Set the initialize order.
     * Holders with order<0, are initialized on use. Those with
     * order>=0 are initialized in increasing order when the handler
     * is started.
     */
    public void setInitOrder(int order)
    {
        _initOnStartup=true;
        _initOrder = order;
    }

    /* ------------------------------------------------------------ */
    /** Comparitor by init order.
     */
    public int compareTo(Object o)
    {
        if (o instanceof ServletHolder)
        {
            ServletHolder sh= (ServletHolder)o;
            if (sh==this)
                return 0;
            if (sh._initOrder<_initOrder)
                return 1;
            if (sh._initOrder>_initOrder)
                return -1;
            int c=_className.compareTo(sh._className);
            if (c==0)
                c=_name.compareTo(sh._name);
            if (c==0)
                c=this.hashCode()>o.hashCode()?1:-1;
            return c;
        }
        return 1;
    }

    /* ------------------------------------------------------------ */
    public boolean equals(Object o)
    {
        return compareTo(o)==0;
    }

    /* ---------------------------------------------------------------- */
    public ServletContext getServletContext()
    {
        return _servletHandler.getServletContext();
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
    /** get a user role link.
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
    
    /* ------------------------------------------------------------ */
    public void start()
        throws Exception
    {
        _servletHandler=(ServletHandler)_httpHandler;
            
        super.start();
        
        if (!javax.servlet.Servlet.class
            .isAssignableFrom(_class))
        {
            Exception ex = new IllegalStateException("Servlet "+_class+
                                            " is not a javax.servlet.Servlet");
            super.stop();
            throw ex;
        }
        

        if (javax.servlet.SingleThreadModel.class
            .isAssignableFrom(_class))
            _servlets=new Stack();

        if (isInitOnStartup())
        {
            _servlet=(Servlet)newInstance();
            _config=new Config();
            _servlet.init(_config);
        }
    }

    /* ------------------------------------------------------------ */
    public void stop()
    {
        if (_servlet!=null)
            _servlet.destroy();
        _servlet=null;
        
        while (_servlets!=null && _servlets.size()>0)
        {
            Servlet s = (Servlet)_servlets.pop();
            s.destroy();
        }
        _config=null;
        super.stop();
        _servletHandler=null;
    }
    

    /* ------------------------------------------------------------ */
    /** Get the servlet.
     * @return The servlet
     */
    public synchronized Servlet getServlet()
        throws UnavailableException
    {
        try
        {
            if (_servlet==null)
                _servlet=(Servlet)newInstance();
        
            if (_config==null)
            {
                _config=new Config();
                _servlet.init(_config);
            }
            
            if (_servlets!=null)
            {
                Servlet servlet=null;
                if (_servlets.size()==0)
                {
                    servlet= (Servlet)newInstance();
                    servlet.init(_config);
                }
                else
                    servlet = (Servlet)_servlets.pop();

                return servlet;
            }

            return _servlet;
        }
        catch(Exception e)
        {
            Code.warning(e);
            throw new UnavailableException(e.toString());
        }    
    }
    
    /* --------------------------------------------------------------- */
    /** Service a request with this servlet.
     */
    public void handle(HttpServletRequest request,
                       HttpServletResponse response)
        throws ServletException,
               UnavailableException,
               IOException
    {
        if (_class==null)
            throw new UnavailableException("Servlet class not initialized");
        
        Servlet servlet=getServlet();
        
        // Check that we got one in the end
        if (servlet==null)
            throw new UnavailableException("Could not construct servlet");

        // Service the request
        boolean servlet_error=true;
        try
        {
            // Handle aliased path
            if (_path!=null)
            {
                request.setAttribute("javax.servlet.include.request_uri",
                                     URI.addPaths(request.getContextPath(),_path));
                request.setAttribute("javax.servlet.include.servlet_path",_path);
            }

            servlet.service(request,response);
            response.flushBuffer();
            servlet_error=false;
        }
        catch(UnavailableException e)
        {
            if (_servlets!=null && servlet!=null)
                servlet.destroy();
            servlet=null;
            throw e;
        }
        finally
        {
            if (servlet_error)
                request.setAttribute("javax.servlet.error.servlet_name",getName());

            // Return to singleThreaded pool
            synchronized(this)
            {
                if (_servlets!=null && servlet!=null)
                    _servlets.push(servlet);
            }
        }
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    class Config implements ServletConfig
    {   
        /* -------------------------------------------------------- */
        public String getServletName()
        {
            return getName();
        }
        
        /* -------------------------------------------------------- */
        public ServletContext getServletContext()
        {
            return _servletHandler.getServletContext();
        }

        /* -------------------------------------------------------- */
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





