// ========================================================================
// $Id$
// Copyright 1996-2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.mortbay.jetty.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpResponse;
import org.mortbay.util.LazyList;
import org.mortbay.util.LogSupport;
import org.mortbay.util.MultiException;
import org.mortbay.util.MultiMap;
import org.mortbay.util.StringUtil;
import org.mortbay.util.TypeUtil;

/* --------------------------------------------------------------------- */
/** WebApp HttpHandler.
 * This handler extends the ServletHandler with security, filter and resource
 * capabilities to provide full J2EE web container support.
 * <p>
 * @since Jetty 4.1
 * @see org.mortbay.jetty.servlet.WebApplicationContext
 * @version $Id$
 * @author Greg Wilkins
 */
public class WebApplicationHandler extends ServletHandler
{
    private static Log log= LogFactory.getLog(WebApplicationHandler.class);

    private Map _filterMap= new HashMap();
    private List _pathFilters= new ArrayList();
    private List _filters= new ArrayList();
    private MultiMap _servletFilterMap= new MultiMap();
    private boolean _acceptRanges= true;
    private boolean _filterChainsCached=true;

    private transient WebApplicationContext _webApplicationContext;

    protected transient Object _requestListeners;
    protected transient Object _requestAttributeListeners;
    protected transient Object _sessionListeners;
    protected transient Object _contextAttributeListeners;
    protected transient FilterHolder jsr154FilterHolder;
    protected transient JSR154Filter jsr154Filter;
    protected transient HashMap _chainCache[];

    /* ------------------------------------------------------------ */
    public boolean isAcceptRanges()
    {
        return _acceptRanges;
    }

    /* ------------------------------------------------------------ */
    /** Set if the handler accepts range requests.
     * Default is false;
     * @param ar True if the handler should accept ranges
     */
    public void setAcceptRanges(boolean ar)
    {
        _acceptRanges= ar;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the jsr154Filter.
     */
    public JSR154Filter getJsr154Filter()
    {
        return jsr154Filter;
    }
    
    /* ------------------------------------------------------------ */
    public FilterHolder defineFilter(String name, String className)
    {
        FilterHolder holder= new FilterHolder(this, name, className);
        _filterMap.put(holder.getName(), holder);
        _filters.add(holder);
        addComponent(holder);
        return holder;
    }

    /* ------------------------------------------------------------ */
    public FilterHolder getFilter(String name)
    {
        return (FilterHolder)_filterMap.get(name);
    }

    /* ------------------------------------------------------------ */
    public FilterHolder mapServletToFilter(String servletName, String filterName)
    {
        FilterHolder holder= (FilterHolder)_filterMap.get(filterName);
        if (holder == null)
            throw new IllegalArgumentException("Unknown filter :" + filterName);
        if (log.isDebugEnabled())
            log.debug("Filter servlet " + servletName + " --> " + filterName);
        _servletFilterMap.add(servletName, holder);
        holder.addServlet(servletName);
        return holder;
    }

    /* ------------------------------------------------------------ */
    public List getFilters()
    {
        return _filters;
    }

    /* ------------------------------------------------------------ */
    public FilterHolder mapPathToFilter(String pathSpec, String filterName)
    {
        FilterHolder holder= (FilterHolder)_filterMap.get(filterName);
        if (holder == null)
            throw new IllegalArgumentException("Unknown filter :" + filterName);

        if (log.isDebugEnabled())
            log.debug("Filter path " + pathSpec + " --> " + filterName);

        if (!holder.isMappedToPath())
            _pathFilters.add(holder);
        holder.addPathSpec(pathSpec);

        return holder;
    }

    /* ------------------------------------------------------------ */
    public synchronized void addEventListener(EventListener listener)
        throws IllegalArgumentException
    {
        boolean known= false;

        if ((listener instanceof HttpSessionActivationListener)
            || (listener instanceof HttpSessionAttributeListener)
            || (listener instanceof HttpSessionBindingListener)
            || (listener instanceof HttpSessionListener))
        {
            if (_sessionManager != null)
                _sessionManager.addEventListener(listener);
            _sessionListeners= LazyList.add(_sessionListeners, listener);
            known= true;
        }

        if (listener instanceof ServletRequestListener)
        {
            known= true;
            _requestListeners= LazyList.add(_requestListeners, listener);
        }

        if (listener instanceof ServletRequestAttributeListener)
        {
            known= true;
            _requestAttributeListeners= LazyList.add(_requestAttributeListeners, listener);
        }

        if (listener instanceof ServletContextAttributeListener)
        {
            known= true;
            _contextAttributeListeners= LazyList.add(_contextAttributeListeners, listener);
        }

        super.addEventListener(listener);
    }

    /* ------------------------------------------------------------ */
    public synchronized void removeEventListener(EventListener listener)
    {
        if (_sessionManager != null)
            _sessionManager.removeEventListener(listener);

        _sessionListeners= LazyList.remove(_sessionListeners, listener);
        _requestListeners= LazyList.remove(_requestListeners, listener);
        _requestAttributeListeners= LazyList.remove(_requestAttributeListeners, listener);
        _contextAttributeListeners= LazyList.remove(_contextAttributeListeners, listener);
        super.removeEventListener(listener);
    }

    /* ------------------------------------------------------------ */
    public void setSessionManager(SessionManager sm)
    {
        if (isStarted())
            throw new IllegalStateException("Started");

        SessionManager old= getSessionManager();

        if (getHttpContext() != null)
        {
            // recover config and remove listeners from old session manager
            if (old != null && old != sm)
            {
                if (_sessionListeners != null)
                {
                    for (Iterator i= LazyList.iterator(_sessionListeners); i.hasNext();)
                    {
                        EventListener listener= (EventListener)i.next();
                        _sessionManager.removeEventListener(listener);
                    }
                }
            }

            // Set listeners and config on new listener.
            if (sm != null && old != sm)
            {
                if (_sessionListeners != null)
                {
                    for (Iterator i= LazyList.iterator(_sessionListeners); i.hasNext();)
                    {
                        EventListener listener= (EventListener)i.next();
                        sm.addEventListener(listener);
                    }
                }
            }
        }

        super.setSessionManager(sm);
    }

    /* ----------------------------------------------------------------- */
    protected synchronized void doStart() throws Exception
    {
        // Start Servlet Handler
        super.doStart();
        if (log.isDebugEnabled())
            log.debug("Path Filters: " + _pathFilters);
        if (log.isDebugEnabled())
            log.debug("Servlet Filters: " + _servletFilterMap);
        
        if (getHttpContext() instanceof WebApplicationContext)
            _webApplicationContext= (WebApplicationContext)getHttpContext();
        
        if (_filterChainsCached)
        {
            _chainCache=new HashMap[FilterHolder.__ERROR+1];
            _chainCache[FilterHolder.__REQUEST]=new HashMap();
            _chainCache[FilterHolder.__FORWARD]=new HashMap();
            _chainCache[FilterHolder.__INCLUDE]=new HashMap();
            _chainCache[FilterHolder.__ERROR]=new HashMap();
        }
    }

    /* ------------------------------------------------------------ */
    public void initializeServlets() throws Exception
    {
        // initialize Filters
        MultiException mex= new MultiException();
        Iterator iter= _filters.iterator();
        while (iter.hasNext())
        {
            FilterHolder holder= (FilterHolder)iter.next();
            try
            {
                holder.start();
            }
            catch (Exception e)
            {
                mex.add(e);
            }
        }

        // initialize Servlets
        try
        {
            super.initializeServlets();
        }
        catch (Exception e)
        {
            mex.add(e);
        }

        jsr154FilterHolder=getFilter("jsr154");
        if (jsr154FilterHolder!=null)
            jsr154Filter= (JSR154Filter)jsr154FilterHolder.getFilter();
        log.debug("jsr154filter="+jsr154Filter);
        
        if (LazyList.size(_requestAttributeListeners) > 0 || LazyList.size(_requestListeners) > 0)
        {
            if (jsr154Filter==null)
                log.warn("Filter jsr154 not defined for RequestAttributeListeners");
            else
            {
                jsr154Filter.setRequestAttributeListeners(_requestAttributeListeners);
                jsr154Filter.setRequestListeners(_requestListeners);
            }
        }

        mex.ifExceptionThrow();
    }

    /* ------------------------------------------------------------ */
    protected synchronized void doStop() throws Exception
    {
        try
        {
            // Stop servlets
            super.doStop();

            // Stop filters
            for (int i= _filters.size(); i-- > 0;)
            {
                FilterHolder holder= (FilterHolder)_filters.get(i);
                holder.stop();
            }
        }
        finally
        {
            _webApplicationContext= null;
            _sessionListeners= null;
            _requestListeners= null;
            _requestAttributeListeners= null;
            _contextAttributeListeners= null;
        }
    }

    /* ------------------------------------------------------------ */
    protected String getErrorPage(int status, ServletHttpRequest request)
    {
        String error_page= null;
        Class exClass= (Class)request.getAttribute(ServletHandler.__J_S_ERROR_EXCEPTION_TYPE);

        if (ServletException.class.equals(exClass))
        {
            error_page= _webApplicationContext.getErrorPage(exClass.getName());
            if (error_page == null)
            {
                Throwable th= (Throwable)request.getAttribute(ServletHandler.__J_S_ERROR_EXCEPTION);
                while (th instanceof ServletException)
                    th= ((ServletException)th).getRootCause();
                if (th != null)
                    exClass= th.getClass();
            }
        }

        if (error_page == null && exClass != null)
        {
            while (error_page == null && exClass != null && _webApplicationContext != null)
            {
                error_page= _webApplicationContext.getErrorPage(exClass.getName());
                exClass= exClass.getSuperclass();
            }

            if (error_page == null)
            {}
        }

        if (error_page == null && _webApplicationContext != null)
            error_page= _webApplicationContext.getErrorPage(TypeUtil.toString(status));

        return error_page;
    }

    /* ------------------------------------------------------------ */
    protected void dispatch(
        String pathInContext,
        HttpServletRequest request,
        HttpServletResponse response,
        ServletHolder servletHolder)
        throws ServletException, UnavailableException, IOException
    {
        // Determine request type.
        int requestType= 0;
        
        if (request instanceof ServletHttpRequest)
        {
            // This is NOT a dispatched request.
            ServletHttpRequest servletHttpRequest= (ServletHttpRequest)request;
            ServletHttpResponse servletHttpResponse= (ServletHttpResponse)response;  
            
            // Request
            requestType= FilterHolder.__REQUEST;
            // protect web-inf and meta-inf
            if (StringUtil.startsWithIgnoreCase(pathInContext, "/web-inf")
                    || StringUtil.startsWithIgnoreCase(pathInContext, "/meta-inf"))
            {
                response.sendError(HttpResponse.__404_Not_Found);
                return;
            }
            
            // Security Check
            if (!getHttpContext().checkSecurityConstraints(
                            pathInContext,
                            servletHttpRequest.getHttpRequest(),
                            servletHttpResponse.getHttpResponse()))
                return;
        }
        else
        {
            // This is a dispatched request.
            
            // Handle dispatch to j_security_check
            HttpContext context= getHttpContext();
            if (context != null
                    && context instanceof ServletHttpContext
                    && pathInContext != null
                    && pathInContext.endsWith(FormAuthenticator.__J_SECURITY_CHECK))
            {
                ServletHttpRequest servletHttpRequest=
                    (ServletHttpRequest)context.getHttpConnection().getRequest().getWrapper();
                ServletHttpResponse servletHttpResponse= servletHttpRequest.getServletHttpResponse();
                ServletHttpContext servletContext= (ServletHttpContext)context;
                
                if (!servletContext
                        .jSecurityCheck(
                                pathInContext,
                                servletHttpRequest.getHttpRequest(),
                                servletHttpResponse.getHttpResponse()))
                    return;
            }
            
            // Forward or error
            requestType=-1;
            if (jsr154Filter!=null)
            {
                Dispatcher.DispatcherRequest dr=jsr154Filter.getDispatchRequest();
                if (dr!=null)
                    requestType=dr.getFilterType();
            }
            if (requestType<0)
                requestType= ((Dispatcher.DispatcherRequest)request).getFilterType();
        }
        
        // Build and/or cache filter chain
        FilterChain chain=null;
        if (_filterChainsCached && _chainCache[requestType].containsKey(pathInContext))
        {
            chain=(FilterChain)_chainCache[requestType].get(pathInContext);
        }
        else
        {
            // Build list of filters
            Object filters= null;
            
            // Path filters
            if (pathInContext != null)
            {
                for (int i= 0; i < _pathFilters.size(); i++)
                {
                    FilterHolder holder= (FilterHolder)_pathFilters.get(i);
                    if (holder.appliesToPath(pathInContext, requestType))
                        filters= LazyList.add(filters, holder);
                }
            } 
            else if (jsr154Filter!=null)
            {
                // Slight hack for Named servlets
                // TODO query JSR how to apply filter to all dispatches
                filters=LazyList.add(filters,jsr154FilterHolder);
            }
            
            // Servlet filters
            if (servletHolder != null && _servletFilterMap.size() > 0)
            {
                Object o= _servletFilterMap.get(servletHolder.getName());
                if (o != null)
                {
                    if (o instanceof List)
                    {
                        List list= (List)o;
                        for (int i= 0; i < list.size(); i++)
                        {
                            FilterHolder holder= (FilterHolder)list.get(i);
                            if (holder.appliesToServlet(servletHolder.getName(),requestType))
                                filters=LazyList.add(filters, holder);
                        }
                    }
                    else
                    {
                        FilterHolder holder= (FilterHolder)o;
                        if (holder.appliesToServlet(servletHolder.getName(),requestType))
                            filters=LazyList.add(filters, holder);
                    }
                }
            }
        
            if (LazyList.size(filters) > 0)
            {
                if (_filterChainsCached)
                {
                    chain= new CachedChain(filters, servletHolder);
                    _chainCache[requestType].put(pathInContext,chain);
                }
                else
                    chain= new Chain(filters, servletHolder);
            } 
            else if (_filterChainsCached)
                _chainCache[requestType].put(pathInContext,null);
        }
        
        if (log.isDebugEnabled()) log.debug("chain="+chain);
        
        // Do the handling thang
        if (chain!=null)
            chain.doFilter(request, response);
        else if (servletHolder != null)
            servletHolder.handle(request, response);    
        else // Not found
            notFound(request, response);
    }
    

    /* ------------------------------------------------------------ */
    public synchronized void setContextAttribute(String name, Object value)
    {
        Object old= super.getContextAttribute(name);
        super.setContextAttribute(name, value);

        if (_contextAttributeListeners != null)
        {
            ServletContextAttributeEvent event=
                new ServletContextAttributeEvent(getServletContext(), name, old != null ? old : value);
            for (int i= 0; i < LazyList.size(_contextAttributeListeners); i++)
            {
                ServletContextAttributeListener l=
                    (ServletContextAttributeListener)LazyList.get(_contextAttributeListeners, i);
                if (old == null)
                    l.attributeAdded(event);
                else
                    if (value == null)
                        l.attributeRemoved(event);
                    else
                        l.attributeReplaced(event);
            }
        }
    }

    /* ------------------------------------------------------------ */
    public synchronized void removeContextAttribute(String name)
    {
        Object old= super.getContextAttribute(name);
        super.removeContextAttribute(name);

        if (old != null && _contextAttributeListeners != null)
        {
            ServletContextAttributeEvent event= new ServletContextAttributeEvent(getServletContext(), name, old);
            for (int i= 0; i < LazyList.size(_contextAttributeListeners); i++)
            {
                ServletContextAttributeListener l=
                    (ServletContextAttributeListener)LazyList.get(_contextAttributeListeners, i);
                l.attributeRemoved(event);
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the filterChainsCached.
     */
    public boolean isFilterChainsCached()
    {
        return _filterChainsCached;
    }
    
    /* ------------------------------------------------------------ */
    /** Cache filter chains.
     * If true, filter chains are cached by the URI path within the
     * context.  Caching should not be used if the webapp encodes
     * information in URLs. 
     * @param filterChainsCached The filterChainsCached to set.
     */
    public void setFilterChainsCached(boolean filterChainsCached)
    {
        _filterChainsCached = filterChainsCached;
    }
    

    /* ----------------------------------------------------------------- */
    public void destroy()
    {
        Iterator iter = _filterMap.values().iterator();
        while (iter.hasNext())
        {
            Object sh=iter.next();
            iter.remove();
            removeComponent(sh);
        }
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class Chain implements FilterChain
    {
        int _filter= 0;
        Object _filters;
        ServletHolder _servletHolder;

        /* ------------------------------------------------------------ */
        Chain(Object filters, ServletHolder servletHolder)
        {
            _filters= filters;
            _servletHolder= servletHolder;
        }

        /* ------------------------------------------------------------ */
        public void doFilter(ServletRequest request, ServletResponse response)
            throws IOException, ServletException
        {
            if (log.isTraceEnabled())
                log.trace("doFilter " + _filter);

            // pass to next filter
            if (_filter < LazyList.size(_filters))
            {
                FilterHolder holder= (FilterHolder)LazyList.get(_filters, _filter++);
                if (log.isTraceEnabled())
                    log.trace("call filter " + holder);
                Filter filter= holder.getFilter();
                filter.doFilter(request, response, this);
                return;
            }

            // Call servlet
            if (_servletHolder != null)
            {
                if (log.isTraceEnabled())
                    log.trace("call servlet " + _servletHolder);
                _servletHolder.handle(request, response);
            }
            else // Not found
                notFound((HttpServletRequest)request, (HttpServletResponse)response);
        }
        
        public String toString()
        {
            StringBuffer b = new StringBuffer();
            for (int i=0; i<LazyList.size(_filters);i++)
            {
                b.append(LazyList.get(_filters, i).toString());
                b.append("->");
            }
            b.append(_servletHolder);
            return b.toString();
        }
    }
    

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class CachedChain implements FilterChain
    {
        FilterHolder _filterHolder;
        ServletHolder _servletHolder;
        CachedChain _next;

        /* ------------------------------------------------------------ */
        CachedChain(Object filters, ServletHolder servletHolder)
        {
            if (LazyList.size(filters)>0)
            {
                _filterHolder=(FilterHolder)LazyList.get(filters, 0);
                filters=LazyList.remove(filters,0);
                _next=new CachedChain(filters,servletHolder);
            }
            else
                _servletHolder=servletHolder;
        }

        public void doFilter(ServletRequest request, ServletResponse response) 
            throws IOException, ServletException
        {
            // pass to next filter
            if (_filterHolder!=null)
            {
                if (log.isTraceEnabled())
                    log.trace("call filter " + _filterHolder);
                Filter filter= _filterHolder.getFilter();
                filter.doFilter(request, response, _next);
                return;
            }

            // Call servlet
            if (_servletHolder != null)
            {
                if (log.isTraceEnabled())
                    log.trace("call servlet " + _servletHolder);
                _servletHolder.handle(request, response);
            }
            else // Not found
                notFound((HttpServletRequest)request, (HttpServletResponse)response);
        }
        
        public String toString()
        {
            if (_filterHolder!=null)
                return _filterHolder+"->"+_next.toString();
            if (_servletHolder!=null)
                return _servletHolder.toString();
            return "null";
        }
    }
}
