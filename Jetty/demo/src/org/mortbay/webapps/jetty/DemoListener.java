// ===========================================================================
// Copyright (c) 2003 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.webapps.jetty;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public  class DemoListener
    implements ServletContextListener,
               ServletRequestListener,
               ServletRequestAttributeListener,
               HttpSessionListener,
               HttpSessionActivationListener,
               HttpSessionAttributeListener
{
    private static Log log = LogFactory.getLog(DemoListener.class);

    public void contextInitialized ( ServletContextEvent e )
    {
        if(log.isDebugEnabled())log.debug("event contextInitialized: "+e.getServletContext());
    }
    
    public void contextDestroyed ( ServletContextEvent e )
    {
        if(log.isDebugEnabled())log.debug("event contextDestroyed: "+e.getServletContext());
    }
    
    public void sessionCreated ( HttpSessionEvent e )
    {
        if(log.isDebugEnabled())log.debug("event sessionCreated: "+e.getSession().getId());
    }
    
    public void sessionDestroyed ( HttpSessionEvent e )
    {
        if(log.isDebugEnabled())log.debug("event sessionDestroyed: "+e.getSession().getId());
    }
    
    public void sessionWillPassivate(HttpSessionEvent e)
    {
        if(log.isDebugEnabled())log.debug("event sessionWillPassivate: "+e.getSession().getId());
    }
        
    public void sessionDidActivate(HttpSessionEvent e)
    {
        if(log.isDebugEnabled())log.debug("event sessionDidActivate: "+e.getSession().getId());
    }
      
    public void attributeAdded ( HttpSessionBindingEvent e )
    {
        if(log.isDebugEnabled())log.debug("event attributeAdded: "+e.getSession().getId()+
                   " "+e.getName()+"="+e.getValue());
    }
    
    public void attributeRemoved ( HttpSessionBindingEvent e )
    {
        if(log.isDebugEnabled())log.debug("event attributeRemoved: "+e.getSession().getId()+
                   " "+e.getName()+"="+e.getValue());
    }
    
    public void attributeReplaced ( HttpSessionBindingEvent e )
    {
        if(log.isDebugEnabled())log.debug("event attributeReplaced: "+e.getSession().getId()+
                   " "+e.getName()+"="+e.getValue());
    }
    
    public void requestDestroyed ( ServletRequestEvent e )
    {
        if(log.isDebugEnabled())log.debug("event requestDestroyed: "+
                   ((HttpServletRequest)e.getServletRequest()).getRequestURI());
    }
    
    public void requestInitialized ( ServletRequestEvent e )
    {
        if(log.isDebugEnabled())log.debug("event requestInitialized: "+
                   ((HttpServletRequest)e.getServletRequest()).getRequestURI());
    }


    
    public void attributeAdded ( ServletRequestAttributeEvent e )
    {
        if(log.isDebugEnabled())log.debug("event requestAttributeAdded: "+
                   ((HttpServletRequest)e.getServletRequest())
                   .getRequestURI()+
                   " "+e.getName()+"="+e.getValue());
    }
    
    public void attributeRemoved ( ServletRequestAttributeEvent e )
    {
        if(log.isDebugEnabled())log.debug("event requestAttributeRemoved: "+
                   ((HttpServletRequest)e.getServletRequest())
                   .getRequestURI()+
                   " "+e.getName()+"="+e.getValue());
    }
    
    public void attributeReplaced ( ServletRequestAttributeEvent e )
    {
        if(log.isDebugEnabled())log.debug("event requestAttributeReplaced: "+
                   ((HttpServletRequest)e.getServletRequest())
                   .getRequestURI()+
                   " "+e.getName()+"="+e.getValue());
    }
}

