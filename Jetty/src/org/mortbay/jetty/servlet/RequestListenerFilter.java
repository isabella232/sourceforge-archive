// ========================================================================
// $Id$
// Copyright 2003-2004 Mort Bay Consulting Pty. Ltd.
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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

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
            new ServletRequestAttributeEvent(_servletContext,request,name,oldValue==null?newValue:oldValue);
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

