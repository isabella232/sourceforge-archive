// ========================================================================
// Copyright (c) 2001 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.servlet;

import org.mortbay.http.HttpServer;
import org.mortbay.http.HandlerContext;

import org.mortbay.util.Code;
import org.mortbay.util.Resource;
import org.mortbay.xml.XmlConfiguration;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import org.xml.sax.SAXException;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;

/* ------------------------------------------------------------ */
/** ServletHandlerContext.
 * Extends HandlerContext with conveniance methods for adding servlets.
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class ServletHandlerContext extends HandlerContext
{
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param server 
     * @param contextPathSpec 
     */
    public ServletHandlerContext(HttpServer server,String contextPathSpec)
    {
        super(server,contextPathSpec);
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
    /** Get the context ServletHandler.
     * Conveniance method. If no ServletHandler exists, a new one is added to
     * the context.
     * @return ServletHandler
     */
    public synchronized ServletHandler getServletHandler()
    {
        ServletHandler servletHandler= (ServletHandler)
            getHandler(org.mortbay.jetty.servlet.ServletHandler.class);
        if (servletHandler==null)
        {
            servletHandler=new ServletHandler();
            addHandler(servletHandler);
        }
        return servletHandler;
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
    public String toString()
    {
        return "Servlet"+super.toString(); 
    }
}
