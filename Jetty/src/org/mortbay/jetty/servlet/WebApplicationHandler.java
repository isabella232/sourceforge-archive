// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

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

    private transient boolean _started= false;
    private transient WebApplicationContext _webApplicationContext;

    protected transient Object _requestListeners;
    protected transient Object _requestAttributeListeners;
    protected transient Object _sessionListeners;

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
    public FilterHolder defineFilter(String name, String className)
    {
        FilterHolder holder= new FilterHolder(this, name, className);
        _filterMap.put(holder.getName(), holder);
        _filters.add(holder);
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

        if (!known)
            throw new IllegalArgumentException(listener.toString());
    }

    /* ------------------------------------------------------------ */
    public synchronized void removeEventListener(EventListener listener)
    {
        if (_sessionManager != null)
            _sessionManager.removeEventListener(listener);

        _sessionListeners= LazyList.remove(_sessionListeners, listener);
        _requestListeners= LazyList.remove(_requestListeners, listener);
        _requestAttributeListeners= LazyList.remove(_requestAttributeListeners, listener);
    }

    /* ------------------------------------------------------------ */
    public void setSessionManager(SessionManager sm)
    {
        if (isStarted())
            throw new IllegalStateException("Started");

        SessionManager old= getSessionManager();

        int mii= 0;
        boolean setMii= false;

        if (getHttpContext() != null)
        {
            // recover config and remove listeners from old session manager
            if (old != null && old != sm)
            {
                old.initialize(null);
                if (_sessionListeners != null)
                {
                    for (Iterator i= LazyList.iterator(_sessionListeners); i.hasNext();)
                    {
                        EventListener listener= (EventListener)i.next();
                        _sessionManager.removeEventListener(listener);
                    }
                }
                mii= _sessionManager.getMaxInactiveInterval();
                setMii= true;
            }

            // Set listeners and config on new listener.
            if (sm != null && sm != sm)
            {
                if (_sessionListeners != null)
                {
                    for (Iterator i= LazyList.iterator(_sessionListeners); i.hasNext();)
                    {
                        EventListener listener= (EventListener)i.next();
                        sm.addEventListener(listener);
                    }
                }
                sm.initialize(this);
                if (setMii)
                    sm.setMaxInactiveInterval(mii);
            }
        }

        super.setSessionManager(sm);
    }

    /* ------------------------------------------------------------ */
    public boolean isStarted()
    {
        return _started && super.isStarted();
    }

    /* ----------------------------------------------------------------- */
    public synchronized void start() throws Exception
    {
        // Start Servlet Handler
        super.start();
        if (log.isDebugEnabled())
            log.debug("Path Filters: " + _pathFilters);
        if (log.isDebugEnabled())
            log.debug("Servlet Filters: " + _servletFilterMap);
        _started= true;
        if (getHttpContext() instanceof WebApplicationContext)
            _webApplicationContext= (WebApplicationContext)getHttpContext();

        if (LazyList.size(_requestAttributeListeners) > 0 || LazyList.size(_requestListeners) > 0)
        {
            FilterHolder holder=
                new FilterHolder(
                    this,
                    "RequestAttributeListener",
                    "org.mortbay.jetty.servlet.RequestListenerFilter");
            holder.addAppliesTo(FilterHolder.__ALL);
            holder.addPathSpec("/");
            holder.start();
            RequestListenerFilter filter= (RequestListenerFilter)holder.getFilter();
            filter.setRequestAttributeListeners(_requestAttributeListeners);
            filter.setRequestListeners(_requestListeners);
            _pathFilters.add(0, holder);
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

        mex.ifExceptionThrow();
    }

    /* ------------------------------------------------------------ */
    public synchronized void stop() throws InterruptedException
    {
        try
        {
            // Stop servlets
            super.stop();

            // Stop filters
            for (int i= _filters.size(); i-- > 0;)
            {
                FilterHolder holder= (FilterHolder)_filters.get(i);
                holder.stop();
            }
        }
        finally
        {
            _started= false;
            _webApplicationContext= null;
            _sessionListeners= null;
            _requestListeners= null;
            _requestAttributeListeners= null;
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

        if (request instanceof Dispatcher.DispatcherRequest)
        {
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
            requestType= ((Dispatcher.DispatcherRequest)request).getFilterType();
        }
        else
        {
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
            if (!getHttpContext()
                .checkSecurityConstraints(
                    pathInContext,
                    servletHttpRequest.getHttpRequest(),
                    servletHttpResponse.getHttpResponse()))
                return;
        }

        // Build list of filters
        Object filters= null;

        // Path filters
        if (pathInContext != null && _pathFilters.size() > 0)
        {
            for (int i= 0; i < _pathFilters.size(); i++)
            {
                FilterHolder holder= (FilterHolder)_pathFilters.get(i);
                if (holder.appliesTo(pathInContext, requestType))
                    filters= LazyList.add(filters, holder);
            }
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
                        if (holder.appliesTo(requestType))
                            filters= LazyList.add(filters, holder);
                    }
                }
                else
                {
                    FilterHolder holder= (FilterHolder)o;
                    if (holder.appliesTo(requestType))
                        filters= LazyList.add(filters, holder);
                }
            }
        }

        // Do the handling thang
        if (LazyList.size(filters) > 0)
        {
            Chain chain= new Chain(pathInContext, filters, servletHolder);
            chain.doFilter(request, response);
        }
        else
        {
            // Call servlet
            if (servletHolder != null)
            {
                if (LogSupport.isTraceEnabled(log))
                    log.trace("call servlet " + servletHolder);
                servletHolder.handle(request, response);
            }
            else // Not found
                notFound(request, response);
        }
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class Chain implements FilterChain
    {
        String _pathInContext;
        int _filter= 0;
        Object _filters;
        ServletHolder _servletHolder;

        /* ------------------------------------------------------------ */
        Chain(String pathInContext, Object filters, ServletHolder servletHolder)
        {
            _pathInContext= pathInContext;
            _filters= filters;
            _servletHolder= servletHolder;
        }

        /* ------------------------------------------------------------ */
        public void doFilter(ServletRequest request, ServletResponse response)
            throws IOException, ServletException
        {
            if (LogSupport.isTraceEnabled(log))
                log.trace("doFilter " + _filter);

            // pass to next filter
            if (_filter < LazyList.size(_filters))
            {
                FilterHolder holder= (FilterHolder)LazyList.get(_filters, _filter++);
                if (LogSupport.isTraceEnabled(log))
                    log.trace("call filter " + holder);
                Filter filter= holder.getFilter();
                filter.doFilter(request, response, this);
                return;
            }

            // Call servlet
            if (_servletHolder != null)
            {
                if (LogSupport.isTraceEnabled(log))
                    log.trace("call servlet " + _servletHolder);
                _servletHolder.handle(request, response);
            }
            else // Not found
                notFound((HttpServletRequest)request, (HttpServletResponse)response);
        }
    }
}
