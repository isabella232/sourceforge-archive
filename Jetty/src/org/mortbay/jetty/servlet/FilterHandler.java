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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.handler.NullHandler;
import org.mortbay.util.Code;
import org.mortbay.util.MultiException;
import org.mortbay.util.MultiMap;

/* ------------------------------------------------------------ */
/** Filter Handler.
 * Determine and call chains of Filters as configured by the
 * WebApplicationContext.
 * This handler currently implements optional brute force caching of
 * FilterChains. Work (XXX) is needed to either limit the size of the cache
 * or more intelligently merge entries in the cache.
 *
 * @since Servlet 2.3
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

    private boolean _cacheFilterChains=true;
    private Map _chainMap;

    /* ------------------------------------------------------------ */
    public boolean getCacheFilterChains()
    {
        return _cacheFilterChains;
    }
    
    /* ------------------------------------------------------------ */
    /** Set Caching of FilterChains.
     * FilterChains are expensive on processing to calculate and moderately
     * expensive on memory to cache them.   The default is to cache FilterChains.
     * @param cache  If true a
     * FilterChain will be cached for every path presented to this
     * handler. If false FilterChains will be calculated for each
     * request to this handler.
     */
    public void setCacheFilterChains(boolean cache)
    {
        _cacheFilterChains=cache;

        if (isStarted())
        {
            if (_cacheFilterChains)
                _chainMap=new HashMap();
            else
                _chainMap=null;
        }
    }
    
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
    public synchronized void start()
        throws Exception
    {
        super.start();        
        _httpContext= getHttpContext();
        _handlerIndex = _httpContext.getHandlerIndex(this);
        _servletHandler = (ServletHandler)
            _httpContext.getHandler(ServletHandler.class);

        if (_cacheFilterChains)
            _chainMap=new HashMap();

        MultiException mex = new MultiException();
        try
        {
            if (!_servletHandler.isStarted())
                _servletHandler.start();
        }
        catch(MultiException e) {mex=e;}
        catch(Exception e) {mex.add(e);}
        
        // Start filters
        Iterator iter = _filterMap.values().iterator();
        while (iter.hasNext())
        {
            FilterHolder holder = (FilterHolder)iter.next();
            try{holder.start();}
            catch(Exception e) {mex.add(e);}
        }

        Code.debug("Path Filters: "+_pathFilters);
        Code.debug("Servlet Filters: "+_servletFilterMap);

        mex.ifExceptionThrow();
    }
    
    /* ------------------------------------------------------------ */
    public synchronized void stop()
        throws  InterruptedException
    {
        super.stop();
        
        // Stop filters
        Iterator iter = _filterMap.values().iterator();
        while (iter.hasNext())
        {
            FilterHolder holder = (FilterHolder)iter.next();
            holder.stop();
        }
        if (_chainMap!=null)
        {
            _chainMap.clear();
            _chainMap=null;
        }
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

        // Get the servlet wrappers
        HttpServletRequest servletRequest = _servletHandler
            .getHttpServletRequest(pathInContext,
                                   pathParams,
                                   httpRequest,
                                   httpResponse);
        HttpServletResponse servletResponse = _servletHandler
            .getHttpServletResponse(httpResponse);

        FilterChain chain=null;
        
        // Look for a cached chain
        if (_cacheFilterChains)
            chain=(FilterChain)_chainMap.get(pathInContext);
        
        if (chain==null)
            chain = new Chain(_handlerIndex+1,pathInContext);
        
        try {chain.doFilter(servletRequest,servletResponse);}
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
            if (!httpRequest.isHandled())
            {
                try{servletResponse.flushBuffer();}
                catch(IOException e) {Code.ignore(e);}
                httpRequest.setHandled(true);
            }
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
        List _servletFilters;
        CacheChain _cacheChain;
        CacheChain _previousChain;
        
        /* ------------------------------------------------------------ */
        Chain(int nextHandler,
              String pathInContext)
        {
            Code.debug("FilterChain: ",pathInContext);
            _nextPathFilter=0;
            _nextServletFilter=0;
            _nextHandler=nextHandler;
            _pathInContext=pathInContext;
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
                    
                    // Do the cache thang
                    if (_cacheFilterChains)
                    {
                        _previousChain = new CacheChain(filter,_previousChain);
                        if (_cacheChain==null)
                            _cacheChain=_previousChain;
                    }
                    
                    // call the filter
                    if (Code.debug())
                        Code.debug("Path doFilter: ",filter," at ",
                                   holder.appliedPathSpec(_pathInContext));
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

                    // Do the cache thang
                    if (_cacheFilterChains)
                    {
                        _previousChain = new CacheChain(filter,_previousChain);
                        if (_cacheChain==null)
                            _cacheChain=_previousChain;
                    }

                    // Call the filter
                    Code.debug("Servlet doFilter: ",filter);
                    filter.doFilter(request,response,this);
                    
                    return;
                }
            }
            
            // Do the cache thang
            if (_cacheFilterChains)
            {
                _previousChain = new CacheChain(_nextHandler,
                                                _pathInContext,
                                                _previousChain);
                if (_cacheChain==null)
                    _cacheChain=_previousChain;
                _chainMap.put(_pathInContext,_cacheChain);
                Code.debug("Cached chain for ",_pathInContext);
            }

            
            // Goto the original resource
            ServletHttpRequest servletHttpRequest=  ServletHttpRequest.unwrap(request);
            ServletHttpResponse servletHttpResponse=servletHttpRequest.getServletHttpResponse();
            ServletRequest requestWrapper=      servletHttpRequest.getWrapper();
            ServletResponse responseWrapper=    servletHttpResponse.getWrapper();
            HttpRequest httpRequest =               servletHttpRequest.getHttpRequest();
            HttpResponse httpResponse =             httpRequest.getHttpResponse();
            
            try
            {
                servletHttpRequest.setWrapper((ServletRequest)request);
                servletHttpResponse.setWrapper((ServletResponse)response);
                
                _httpContext.handle(_nextHandler,
                                    _pathInContext,
                                    null, // Assume path params have
                                          // already been processed.
                                    httpRequest,
                                    httpResponse);
            }
            finally
            {
                servletHttpRequest.setWrapper(requestWrapper);
                servletHttpResponse.setWrapper(responseWrapper);
            }
        }   
    }
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class CacheChain implements FilterChain
    {
        CacheChain _nextChain;
        Filter _filter;
        int _nextHandler;
        String _pathInContext;

        /* ------------------------------------------------------------ */
        CacheChain(Filter filter, CacheChain previous)
        {
            Code.debug("CacheChain: ",filter);
            _filter=filter;
            if (previous!=null)
                previous._nextChain=this;
        }
        
        /* ------------------------------------------------------------ */
        CacheChain(int nextHandler,String pathInContext,CacheChain previous)
        {
            Code.debug("CacheChain: ",pathInContext);
            _nextHandler=nextHandler;
            _pathInContext=pathInContext;
            if (previous!=null)
                previous._nextChain=this;
        }
        
        /* ------------------------------------------------------------ */
        public void doFilter(ServletRequest request, ServletResponse response)
            throws IOException,
                   ServletException
        {
            if (_nextChain!=null)
            {
                Code.debug("Call cached filter: ",_filter);
                _filter.doFilter(request,response,_nextChain);
            }
            else
            {
                // Goto the original resource
                ServletHttpRequest servletHttpRequest=  ServletHttpRequest.unwrap(request);
                ServletHttpResponse servletHttpResponse=servletHttpRequest.getServletHttpResponse();
                ServletRequest requestWrapper=      servletHttpRequest.getWrapper();
                ServletResponse responseWrapper=    servletHttpResponse.getWrapper();
                HttpRequest httpRequest =               servletHttpRequest.getHttpRequest();
                HttpResponse httpResponse =             httpRequest.getHttpResponse();
            
                try
                {
                    servletHttpRequest.setWrapper((ServletRequest)request);
                    servletHttpResponse.setWrapper((ServletResponse)response);
                    _httpContext.handle(_nextHandler,
                                        _pathInContext,
                                        null, // Assume path params have
                                        // already been processed.
                                        httpRequest,
                                        httpResponse);
                }
                finally
                {
                    servletHttpRequest.setWrapper(requestWrapper);
                    servletHttpResponse.setWrapper(responseWrapper);
                }
            }
        }
    }
}
