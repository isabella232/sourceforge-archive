// ========================================================================
// Copyright (c) 2001 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.servlet;

import org.mortbay.http.HttpConnection;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpServer;
import javax.servlet.ServletContext;
import org.mortbay.util.Code;

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
    public ServletHttpContext()
    {
        super();
    }

    /* ------------------------------------------------------------ */
    /** Get the ThreadLocal HttpConnection.
     * Get the ThreadLocal HttpConnection.
     * @return HttpConnection for this thread.
     */
    protected HttpConnection getHttpConnection()
    {
        return super.getHttpConnection();
    }
    
    /* ------------------------------------------------------------ */
    /** Set the ServletContext.
     * Called by the ServletHandler to 
     * @param servletContext 
     */
    void setServletContext(ServletContext servletContext)
    {
        if (_servletContext!=null && servletContext!=_servletContext)
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
        return getServletHandler().addServlet(name,pathSpec,className,null);
    }

    /* ------------------------------------------------------------ */
    /** Setup context for serving dynamic servlets.
     * @deprecated Use org.mortbay.jetty.servlet.Invoker
     */
    public synchronized void setDynamicServletPathSpec(String pathSpecInContext)
    {
        Code.warning("setDynamicServletPathSpec is deprecated.");
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
