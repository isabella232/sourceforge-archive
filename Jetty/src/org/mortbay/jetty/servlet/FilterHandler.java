// ===========================================================================
// Copyright (c) 2001 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.jetty.servlet;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.handler.NullHandler;
import org.mortbay.util.Code;
import org.mortbay.util.MultiMap;


/* ------------------------------------------------------------ */
/** 
 * @see
 * @version  $Id$
 * @author Greg Wilkins (gregw)
 */
public class FilterHandler
    extends NullHandler
{
    private HttpContext _httpContext;
    private ServletHandler _servletHandler;
    private int _handlerIndex;
    
    private Map _filterMap=new HashMap();
    private List _pathFilters=new ArrayList();
    private MultiMap _servletFilterMap=new MultiMap();
    
    /* ------------------------------------------------------------ */
    FilterHolder newFilterHolder(String name, String className)
    {
        FilterHolder holder = new FilterHolder(this,name,className);
        _filterMap.put(holder.getName(),holder);
        return holder;
    }
    
    /* ------------------------------------------------------------ */
    public FilterHolder getFilterHolder(String name)
    {
        return (FilterHolder)_filterMap.get(name);
    }

    /* ------------------------------------------------------------ */
    public FilterHolder mapServletToFilter(String servletName,
                                           String filterName)
    {
        FilterHolder holder =(FilterHolder)_filterMap.get(filterName);
        if (holder==null)
            throw new IllegalArgumentException("Unknown filter :"+filterName);
        
        Code.debug("Filter servlet ",servletName," --> ",filterName);

        _servletFilterMap.add(servletName,holder);
        
        return holder;
    }
    
    /* ------------------------------------------------------------ */
    public FilterHolder mapPathToFilter(String pathSpec,
                                        String filterName)
    {
        FilterHolder holder =(FilterHolder)_filterMap.get(filterName);
        if (holder==null)
            throw new IllegalArgumentException("Unknown filter :"+filterName);
        
        Code.debug("Filter path ",pathSpec," --> ",filterName);

        if (!holder.isMappedToPath())
            _pathFilters.add(holder);
        holder.addPathSpec(pathSpec);
        
        return holder;
    }    

    /* ------------------------------------------------------------ */
    public ServletHandler getServletHandler()
    {
        return _servletHandler;
    }

    
    /* ------------------------------------------------------------ */
    public void start()
        throws Exception
    {
        super.start();        
        _httpContext= getHttpContext();
        _handlerIndex = _httpContext.getHttpHandlerIndex(this);
        _servletHandler = (ServletHandler)
            _httpContext.getHttpHandler(ServletHandler.class);
        
        // Start filters
        Iterator iter = _filterMap.values().iterator();
        while (iter.hasNext())
        {
            FilterHolder holder = (FilterHolder)iter.next();
            holder.start();
        }

        Code.debug("Path Filters: "+_pathFilters);
        Code.debug("Servlet Filters: "+_servletFilterMap);
    }
    
    /* ------------------------------------------------------------ */
    public void stop()
        throws  InterruptedException
    {
        // Stop filters
        Iterator iter = _filterMap.values().iterator();
        while (iter.hasNext())
        {
            FilterHolder holder = (FilterHolder)iter.next();
            holder.stop();
        }
        super.stop();
    }
    
    /* ------------------------------------------------------------ */
    public void handle(String pathInContext,
                       String pathParams,
                       HttpRequest httpRequest,
                       HttpResponse httpResponse)
         throws IOException
    {
        if (!isStarted() || _filterMap.size()==0)
            return;
        
        ServletHttpRequest servletHttpRequest =
            _servletHandler.getServletHttpRequest(pathInContext,
                                                  pathParams,
                                                  httpRequest,
                                                  httpResponse);
        ServletHttpResponse servletHttpResponse =
             servletHttpRequest.getServletHttpResponse();

        Chain chain = new Chain(_handlerIndex+1,
                                pathInContext,
                                pathParams,
                                httpRequest,
                                httpResponse);
        try
        {
            chain.doFilter(servletHttpRequest,servletHttpResponse);
        }
        catch(ServletException e)
        {
            Throwable th=e.getRootCause();
            if (th instanceof HttpException)
                throw (HttpException)th;
            if (th instanceof IOException)
                throw (IOException)th;
        }
        finally
        {
            httpRequest.setHandled(true);
        }
    }
    

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class Chain implements FilterChain
    {
        int _nextPathFilter;
        int _nextServletFilter;
        int _nextHandler;
        String _pathInContext;
        String _pathParams;
        HttpRequest _httpRequest;
        HttpResponse _httpResponse;
        List _servletFilters;

        /* ------------------------------------------------------------ */
        Chain(int nextHandler,
              String pathInContext,
              String pathParams,
              HttpRequest httpRequest,
              HttpResponse httpResponse)
        {
            Code.debug("FilterChain: ",pathInContext);
            _nextPathFilter=0;
            _nextServletFilter=0;
            _nextHandler=nextHandler;
            _pathInContext=pathInContext;
            _pathParams=pathParams;
            _httpRequest=httpRequest;
            _httpResponse=httpResponse;
        }
        
        /* ------------------------------------------------------------ */
        public void doFilter(ServletRequest request, ServletResponse response)
            throws IOException,
                   ServletException
        {
            // Find the next applicable path filter
            while(_nextPathFilter<_pathFilters.size())
            {
                FilterHolder holder=(FilterHolder)_pathFilters.get(_nextPathFilter++);
                if (holder.appliesTo(_pathInContext))
                {
                    Filter filter = holder.getFilter();
                    Code.debug("Path doFilter: ",filter);
                    filter.doFilter(request,response,this);
                    return;
                }
            }

            // Find the next applicable servlet filter
            if (_servletFilterMap.size() > 0)
            {
                // Find the list of matching filters
                if (_servletFilters==null)
                {
                    Map.Entry entry=_servletHandler.getHolderEntry(_pathInContext);
                    if (entry!=null)
                    {
                        ServletHolder servletHolder=(ServletHolder)entry.getValue();
                        _servletFilters=_servletFilterMap.getValues(servletHolder.getName());
                    }
                }

                // goto next filter in the list
                if (_servletFilters!=null &&
                    _nextServletFilter<_servletFilters.size())
                {
                    FilterHolder holder=(FilterHolder)
                        _servletFilters.get(_nextServletFilter++);
                    Filter filter = holder.getFilter();
                    Code.debug("Servlet doFilter: ",filter);
                    filter.doFilter(request,response,this);
                    return;
                }
            }

            // Goto the original resource
            _httpContext.handle(_nextHandler,
                                _pathInContext,
                                _pathParams,
                                _httpRequest,
                                _httpResponse);
        }   
    }
    
}
