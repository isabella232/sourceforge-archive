// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.jetty.servlet;

import java.util.Enumeration;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import org.mortbay.util.Code;
import org.mortbay.http.HttpHandler;
import org.mortbay.http.PathMap;

/* --------------------------------------------------------------------- */
/** 
 * @version $Id$
 * @author Greg Wilkins
 */
public class FilterHolder
    extends Holder
{
    private Filter _filter;
    private Config _config;
    private PathMap _pathSpecs;
    
    /* ---------------------------------------------------------------- */
    public FilterHolder(HttpHandler httpHandler,
                        String name,
                        String className)
    {
        super(httpHandler,name,className);
    }

    /* ------------------------------------------------------------ */
    public void addPathSpec(String pathSpec)
    {
        if (_pathSpecs==null)
            _pathSpecs=new PathMap();
        _pathSpecs.put(pathSpec,pathSpec);
    }
    
    /* ------------------------------------------------------------ */
    public boolean isMappedToPath()
    {
        return _pathSpecs!=null;
    }
    

    /* ------------------------------------------------------------ */
    public boolean appliesTo(String path)
    {
        return
            _pathSpecs!=null &&
            _pathSpecs.getMatch(path)!=null;
    }
    
    /* ------------------------------------------------------------ */
    public String appliedPathSpec(String path)
    {
        if (_pathSpecs==null)
            return null;
        Map.Entry entry = _pathSpecs.getMatch(path);
        if (entry==null)
            return null;
        return (String)entry.getKey();
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
            return ((FilterHandler)_httpHandler).getServletHandler().getServletContext();
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





