// ========================================================================
// Copyright (c) 2001 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.servlet;

import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpServer;
import javax.servlet.ServletContext;


/* ------------------------------------------------------------ */
/** ServletHttpContext.
 * Extends HttpContext with conveniance methods for adding servlets.
 * Enforces a single ServletHandler per context.
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class ServletHttpContext extends HttpContext
{
    /* ------------------------------------------------------------ */
    private ServletContext _servletContext;
    private ServletHandler _servletHandler;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param server 
     * @param contextPathSpec 
     */
    public ServletHttpContext(HttpServer server,String contextPathSpec)
    {
        super(server,contextPathSpec);
    }

    /* ------------------------------------------------------------ */
    /** Set the ServletContext.
     * Called by the ServletHandler to 
     * @param servletContext 
     */
    void setServletContext(ServletContext servletContext)
    {
        if (_servletContext!=null)
            throw new IllegalStateException("ServletContext already set");
        _servletContext=servletContext;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return The ServletContext. 
     */
    public ServletContext getServletContext()
    {
        return _servletContext;
    }
    
    /* ------------------------------------------------------------ */
    /** Set the ServletHandler.
     * Called by the ServletHandler to 
     * @param servletHandler 
     */
    void setServletHandler(ServletHandler servletHandler)
    {
        if (_servletHandler!=null && _servletHandler!=servletHandler)
            throw new IllegalStateException("ServletHandler already set");
        _servletHandler=servletHandler;
    }
    
    /* ------------------------------------------------------------ */
    /** Get the context ServletHandler.
     * Conveniance method. If no ServletHandler exists, a new one is added to
     * the context.
     * @return ServletHandler
     */
    public synchronized ServletHandler getServletHandler()
    {
        if (_servletHandler==null)
            _servletHandler= (ServletHandler) getHandler(ServletHandler.class);
        if (_servletHandler==null)
        {
            _servletHandler=new ServletHandler();
            addHandler(_servletHandler);
        }
        return _servletHandler;
    }
    
    
    /* ------------------------------------------------------------ */
    /** Add a servlet to the context.
     * Conveniance method.
     * If no ServletHandler is found in the context, a new one is added.
     * @param name The name of the servlet.
     * @param pathSpec The pathspec within the context
     * @param className The classname of the servlet.
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
        return addServlet(className,pathSpec,className);
    }
    
    /* ------------------------------------------------------------ */
    /** Add a servlet to the context.
     * If no ServletHandler is found in the context, a new one is added.
     * @param name The name of the servlet.
     * @param pathSpec The pathspec within the context
     * @param className The classname of the servlet.
     * @return The ServletHolder.
     * @exception ClassNotFoundException 
     * @exception InstantiationException 
     * @exception IllegalAccessException 
     */
    public synchronized ServletHolder addServlet(String name,
                                                 String pathSpec,
                                                 String className)
        throws ClassNotFoundException,
               InstantiationException,
               IllegalAccessException
    {
        return getServletHandler().addServlet(name,pathSpec,className);
    }

    /* ------------------------------------------------------------ */
    /** Setup context for serving dynamic servlets.
     * Conveniance method.  A Dynamic servlet is one which is mapped from a
     * URL containing the class name of the servlet - which is dynamcially
     * loaded when the first request is received.
     * @param pathSpecInContext The path within the context at which
     * dynamic servlets are launched. eg /servlet/*
     */
    public synchronized void setDynamicServletPathSpec(String pathSpecInContext)
    {
        ServletHandler handler = (ServletHandler)
            getHandler(org.mortbay.jetty.servlet.ServletHandler.class);
        if (pathSpecInContext!=null)
        {
            if (handler==null)
                handler=getServletHandler();
            handler.setDynamicServletPathSpec(pathSpecInContext);
        }
        else if (handler!=null)
            removeHandler(handler);
    }

    /* ------------------------------------------------------------ */
    public String getDynamicServletPathSpec()
    {
        ServletHandler handler = (ServletHandler)
            getHandler(org.mortbay.jetty.servlet.ServletHandler.class);
        if (handler!=null)
            return handler.getDynamicServletPathSpec();
        return null;
    }

    /* ------------------------------------------------------------ */
    public void stop()
        throws InterruptedException
    {
        super.stop();
        _servletHandler=null;
        _servletContext=null;
    }
    
    /* ------------------------------------------------------------ */
    public String toString()
    {
        return "Servlet"+super.toString(); 
    }
    
}
