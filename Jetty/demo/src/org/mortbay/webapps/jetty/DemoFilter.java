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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/* ------------------------------------------------------------ */
public  class DemoFilter implements Filter
{
    private static Log log = LogFactory.getLog(DemoFilter.class);

    public void init(FilterConfig filterConfig)
        throws ServletException
    {
        if(log.isDebugEnabled())log.debug("init:"+filterConfig);
    }

    /* ------------------------------------------------------------ */
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
	throws IOException, ServletException
    {
        if(log.isDebugEnabled())log.debug("doFilter:"+request);
        synchronized(this)
        {
            Integer called = (Integer)request.getAttribute("DemoFilter called");
            if (called==null)
                called=new Integer(1);
            else
                called=new Integer(called.intValue()+1);
            request.setAttribute("DemoFilter called",called);
        }
        chain.doFilter(request, response);
    }

    public void destroy()
    {
        log.debug("destroy");
    }
}

