// ===========================================================================
// Copyright (c) 2003 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.jetty.servlet;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.mortbay.util.Code;
import org.mortbay.util.LazyList;

/* ------------------------------------------------------------ */
/** Request Listener Filter.
 * This filter is automatically inserted by WebApplicationHandler
 * when ServletRequestListener or ServletRequestAttributeListeners
 * event listeners are registered with the context.
 */
public  class RequestListenerFilter implements Filter
{
    private ServletContext _servletContext;
    private Object _requestListeners;
    private Object _requestAttributeListeners;
    
    public void init(FilterConfig filterConfig)
        throws ServletException
    {
        _servletContext=filterConfig.getServletContext();
    }

    /* ------------------------------------------------------------ */
    protected void setRequestAttributeListeners(Object list)
    {
        _requestAttributeListeners=list;
    }
    
    /* ------------------------------------------------------------ */
    protected void setRequestListeners(Object list)
    {
        _requestListeners=list;
    }
    
    /* ------------------------------------------------------------ */
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
	throws IOException, ServletException
    {
        HttpServletRequest srequest = (HttpServletRequest)request;
        
        boolean notified=false;
        if (_requestListeners!=null)
        {
            if (request instanceof Dispatcher.DispatcherRequest)
            {
                if (((Dispatcher.DispatcherRequest)request).crossContext())
                {
                    requestInitialized(request);
                    notified=true;
                }
            }
            else
            {
                requestInitialized(request);
                notified=true;
            }
        }

        if (_requestAttributeListeners!=null)
        {
            if (request instanceof Dispatcher.DispatcherRequest)
            {
                if (((Dispatcher.DispatcherRequest)request).crossContext())
                    request=new NotifyRequest(srequest);
            }
            else
                request=new NotifyRequest(srequest);
        }
        

        try
        {
            chain.doFilter(request, response);   
        }
        finally
        {
            if (notified)
                requestDestroyed(request);
        }
    }

    /* ------------------------------------------------------------ */
    public void destroy()
    {
    }
    
    /* ------------------------------------------------------------ */
    private void requestInitialized(ServletRequest request)
    {
        ServletRequestEvent event = new ServletRequestEvent(_servletContext,request);
        for (int i=0;i<LazyList.size(_requestListeners);i++)
            ((ServletRequestListener)LazyList.get(_requestListeners,i))
                        .requestInitialized(event);
    }
    
    /* ------------------------------------------------------------ */
    private void requestDestroyed(ServletRequest request)
    {
        ServletRequestEvent event = new ServletRequestEvent(_servletContext,request);
        for (int i=LazyList.size(_requestListeners);i-->0;)
            ((ServletRequestListener)LazyList.get(_requestListeners,i))
                        .requestDestroyed(event);
    }
    
    /* ------------------------------------------------------------ */
    private void attributeNotify(ServletRequest request,String name,Object oldValue,Object newValue)
    {
        ServletRequestAttributeEvent event =
            new ServletRequestAttributeEvent(_servletContext,request,name,
                                             ((newValue==null)?oldValue:newValue));
        for (int i=0;i<LazyList.size(_requestAttributeListeners);i++)
        {
            ServletRequestAttributeListener listener = 
                ((ServletRequestAttributeListener)LazyList.get(_requestAttributeListeners,i));
            if (oldValue==null)
                listener.attributeAdded(event);
            else if (newValue==null)
                listener.attributeRemoved(event);
            else
                listener.attributeReplaced(event);
        }
    }
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class NotifyRequest extends HttpServletRequestWrapper
    {
        /* ------------------------------------------------------------ */
        NotifyRequest(HttpServletRequest httpServletRequest)
        {
            super(httpServletRequest);
        }
        
        /* ------------------------------------------------------------ */
        public void setAttribute(String name, Object value)
        {
            Object old=getAttribute(name);
            super.setAttribute(name,value);
            attributeNotify(this,name,old,value);
        }
        
        /* ------------------------------------------------------------ */
        public void removeAttribute(String name)
        {   
            Object old=getAttribute(name);
            super.removeAttribute(name);
            attributeNotify(this,name,old,null);
        }
    }
}

