// ===========================================================================
// Copyright (c) 1996-2003 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.webapps.jetty;
import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/* ------------------------------------------------------------ */
public  class DemoFilter implements Filter
{
    private static Log log = LogFactory.getLog(DemoFilter.class);
    private String type="Unknown";
    private boolean wrap;

    public void init(FilterConfig filterConfig)
        throws ServletException
    {
        if(log.isDebugEnabled())log.debug("init:"+filterConfig);
        type=filterConfig.getInitParameter("type");
        wrap=new Boolean(filterConfig.getInitParameter("wrap")).booleanValue();
    }

    /* ------------------------------------------------------------ */
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
	throws IOException, ServletException
    {
        if(log.isDebugEnabled())log.debug("doFilter["+type+"]:"+((HttpServletRequest)request).getRequestURI());
        synchronized(this)
        {
            Integer called = (Integer)request.getAttribute("DemoFilter-"+type);
            if (called==null)
                called=new Integer(1);
            else
                called=new Integer(called.intValue()+1);
            request.setAttribute("DemoFilter-"+type,called);
        }
        
        if (wrap)
            chain.doFilter(new HttpServletRequestWrapper((HttpServletRequest)request),
                           new HttpServletResponseWrapper((HttpServletResponse)response));
        else                               
            chain.doFilter(request, response);
    }

    public void destroy()
    {
        log.debug("destroy");
    }
}

