// ===========================================================================
// Copyright (c) 2003 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.webapps.jetty;
import java.io.IOException;

import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpServletRequest;

import org.mortbay.util.Code;

public  class DemoListener
    implements ServletContextListener,
               ServletRequestListener,
               ServletRequestAttributeListener,
               HttpSessionListener,
               HttpSessionActivationListener,
               HttpSessionAttributeListener
{
    public void contextInitialized ( ServletContextEvent e )
    {
        Code.debug("event contextInitialized: "+e);
    }
    
    public void contextDestroyed ( ServletContextEvent e )
    {
        Code.debug("event contextDestroyed: "+e);
    }
    
    public void sessionCreated ( HttpSessionEvent e )
    {
        Code.debug("event sessionCreated: "+e.getSession().getId());
    }
    
    public void sessionDestroyed ( HttpSessionEvent e )
    {
        Code.debug("event sessionDestroyed: "+e.getSession().getId());
    }
    
    public void sessionWillPassivate(HttpSessionEvent e)
    {
        Code.debug("event sessionWillPassivate: "+e.getSession().getId());
    }
        
    public void sessionDidActivate(HttpSessionEvent e)
    {
        Code.debug("event sessionDidActivate: "+e.getSession().getId());
    }
      
    public void attributeAdded ( HttpSessionBindingEvent e )
    {
        Code.debug("event attributeAdded: "+e.getSession().getId()+
                   " "+e.getName()+"="+e.getValue());
    }
    
    public void attributeRemoved ( HttpSessionBindingEvent e )
    {
        Code.debug("event attributeRemoved: "+e.getSession().getId()+
                   " "+e.getName()+"="+e.getValue());
    }
    
    public void attributeReplaced ( HttpSessionBindingEvent e )
    {
        Code.debug("event attributeReplaced: "+e.getSession().getId()+
                   " "+e.getName()+"="+e.getValue());
    }
    
    public void requestDestroyed ( ServletRequestEvent e )
    {
        Code.debug("event requestDestroyed: "+
                   ((HttpServletRequest)e.getServletRequest()).getRequestURI());
    }
    
    public void requestInitialized ( ServletRequestEvent e )
    {
        Code.debug("event requestInitialized: "+
                   ((HttpServletRequest)e.getServletRequest()).getRequestURI());
    }


    
    public void attributeAdded ( ServletRequestAttributeEvent e )
    {
        Code.debug("event requestAttributeAdded: "+
                   ((HttpServletRequest)e.getServletRequest())
                   .getRequestURI()+
                   " "+e.getName()+"="+e.getValue());
    }
    
    public void attributeRemoved ( ServletRequestAttributeEvent e )
    {
        Code.debug("event requestAttributeRemoved: "+
                   ((HttpServletRequest)e.getServletRequest())
                   .getRequestURI()+
                   " "+e.getName()+"="+e.getValue());
    }
    
    public void attributeReplaced ( ServletRequestAttributeEvent e )
    {
        Code.debug("event requestAttributeReplaced: "+
                   ((HttpServletRequest)e.getServletRequest())
                   .getRequestURI()+
                   " "+e.getName()+"="+e.getValue());
    }
}

