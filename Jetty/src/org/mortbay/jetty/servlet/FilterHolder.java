// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.jetty.servlet;

import org.mortbay.http.HandlerContext;
import java.util.Enumeration;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import org.mortbay.util.Code;

/* --------------------------------------------------------------------- */
/** 
 * @see org.mortbay.jetty.ServletHandler
 * @version $Id$
 * @author Greg Wilkins
 */
public class FilterHolder
    extends Holder
{
    private Filter _filter;
    private Config _config;
    private ServletHandler _servletHandler;
    
    /* ---------------------------------------------------------------- */
    public FilterHolder(ServletHandler servletHandler,
                        String name,
                        String className)
    {
        super(servletHandler,name,className);
    }
    
    /* ------------------------------------------------------------ */
    public void start()
        throws Exception
    {
        super.start();
        
        if (!javax.servlet.Filter.class
            .isAssignableFrom(_class))
        {
            super.stop();
            throw new IllegalStateException("Servlet class "+_class+
                                            " is not a javax.servlet.Filter");
        }

        _filter=(Filter)newInstance();
        _config=new Config();
        _filter.init(_config); 
    }

    /* ------------------------------------------------------------ */
    public void stop()
    {
        if (_filter!=null)
            _filter.destroy();
        _filter=null;
        _config=null;
        super.stop();   
    }
    
    
    /* ---------------------------------------------------------------- */
    public synchronized void destroy()
    {
        Code.notImplemented();
    }
    
    /* ------------------------------------------------------------ */
    public Filter getFilter()
    {
        return _filter;
    }

    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    class Config implements FilterConfig
    {
        /* ------------------------------------------------------------ */
        public String getFilterName()
        {
            return FilterHolder.this.getName();
        }

        /* ------------------------------------------------------------ */
        public ServletContext getServletContext()
        {
            return _servletHandler.getServletContext();
        }
        
        /* -------------------------------------------------------- */
        public String getInitParameter(String param)
        {
            return FilterHolder.this.getInitParameter(param);
        }
    
        /* -------------------------------------------------------- */
        public Enumeration getInitParameterNames()
        {
            return FilterHolder.this.getInitParameterNames();
        }
    }
    
}





