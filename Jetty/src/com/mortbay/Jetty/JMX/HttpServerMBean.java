// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Jetty.JMX;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ObjectName;
import javax.management.MBeanOperationInfo;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import com.mortbay.Jetty.Server;
import com.mortbay.HTTP.HttpServer;
import com.mortbay.HTTP.HttpListener;
import com.mortbay.HTTP.HandlerContext;
import com.mortbay.HTTP.WebApplicationContext;
import com.mortbay.Util.Code;
import com.mortbay.Util.Log;
import com.mortbay.Util.LogSink;
import com.mortbay.Util.LifeCycle;
import com.mortbay.Util.WriterLogSink;

import java.beans.beancontext.BeanContextMembershipListener;
import java.beans.beancontext.BeanContextMembershipEvent;

import java.util.Iterator;
import java.util.HashMap;

import java.io.IOException;

/* ------------------------------------------------------------ */
/** HttpServer MBean.
 * This Model MBean class provides the mapping for HttpServer
 * management methods. It also registers itself as a membership
 * listener of the HttpServer, so it can create and destroy MBean
 * wrappers for listeners and contexts.
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class HttpServerMBean extends LifeCycleMBean
    implements BeanContextMembershipListener
{
    private Server _jettyServer;
    private HashMap _mbeanMap = new HashMap();
    private String _configuration;

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @exception MBeanException 
     * @exception InstanceNotFoundException 
     */
    public HttpServerMBean()
        throws MBeanException, InstanceNotFoundException
    {
        super(null);
        _jettyServer=new Server();
        try{setManagedResource(_jettyServer,"objectReference");}
        catch(InvalidTargetObjectTypeException e){Code.warning(e);}
    }

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param configuration URL or File to jetty.xml style configuration file
     * @exception IOException 
     * @exception MBeanException 
     * @exception InstanceNotFoundException 
     */
    public HttpServerMBean(String configuration)
        throws IOException,MBeanException, InstanceNotFoundException
    {
        this();
        _configuration=configuration;
    }

    /* ------------------------------------------------------------ */
    protected String newObjectName()
    {
        // Create own ObjectName of the form:
        // package:class=id
        return uniqueObjectName(getMBeanServer().getDefaultDomain()+":name=Jetty");
    }

    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();
        
        defineAttribute("configuration");
        defineAttribute("logSink");
        defineAttribute("logDateFormat");
        
        defineOperation("addListener",
                        new String[]{"java.lang.String"},
                        IMPACT_ACTION);
        defineOperation("addListener",
                        new String[]{"com.mortbay.HTTP.HttpListener"},
                        IMPACT_ACTION);
        defineOperation("addContext",
                        new String[]{"java.lang.String","java.lang.String"},
                        IMPACT_ACTION);
        defineOperation("addHostAlias",
                        new String[]{"java.lang.String","java.lang.String"},
                        IMPACT_ACTION);
        defineOperation("addWebApplication",
                        new String[]{"java.lang.String",
                                     "java.lang.String",
                                     "java.lang.String",
                                     "java.lang.String",
                                     "boolean"},
                        IMPACT_ACTION);
        
    }
    
    /* ------------------------------------------------------------ */
    public synchronized void childrenAdded(BeanContextMembershipEvent event)
    {
        Code.debug("Children added ",event);
        Iterator iter = event.iterator();
        while(iter.hasNext())
        {
            try
            {
                Object o=iter.next();
                ModelMBeanImpl mbean=null;
                ObjectName oName=null;
                if (o instanceof HttpListener)
                    mbean= new HttpListenerMBean(this,(HttpListener)o);
                else if (o instanceof WebApplicationContext)
                    mbean= new WebApplicationMBean(this,(WebApplicationContext)o);
                else if (o instanceof HandlerContext)
                    mbean= new HandlerContextMBean(this,(HandlerContext)o);
                else if (o instanceof LogSink)
                {
                    mbean= (o instanceof WriterLogSink)
                        ? new WriterLogSinkMBean((WriterLogSink)o,false)
                        : new LogSinkMBean((LogSink)o);
                    String className = o.getClass().getName();
                    if (className.indexOf(".")>0)
                        className=className.substring(className.lastIndexOf(".")+1);
                    oName=new ObjectName(getObjectName().toString()+
                                         ",RequestLog="+className);                     
                }
                else if (o instanceof LifeCycle)
                    mbean = new LifeCycleMBean((LifeCycle)o);

                if (mbean!=null)
                {
                    getMBeanServer().registerMBean(mbean,oName);
                    _mbeanMap.put(o,mbean);
                }
            }
            catch(Exception e)
            {
                Code.warning(e);
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    public synchronized void childrenRemoved(BeanContextMembershipEvent event)
    {
        Code.debug("Children removed ",event);
        
        Iterator iter = event.iterator();
        while(iter.hasNext())
        {
            try
            {
                Object o=iter.next();
                ModelMBeanImpl mbean =
                    (ModelMBeanImpl)_mbeanMap.remove(o);
                if (mbean!=null)
                    getMBeanServer().unregisterMBean(mbean.getObjectName());
            }
            catch(Exception e)
            {
                Code.warning(e);
            }
        }
    }

    
    /* ------------------------------------------------------------ */
    /** 
     * @param ok 
     */
    public void postRegister(Boolean ok)
    {
        super.postRegister(ok);
        
        if (ok.booleanValue())
        {
            _jettyServer.addBeanContextMembershipListener(this);

            if (_configuration!=null)
            {
                try
                {
                    _jettyServer.configure(_configuration);
                    _jettyServer.start();
                }
                catch(Exception e)
                {
                    Code.warning(e);
                }
            }
        }
    }
}
