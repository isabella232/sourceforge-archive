// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.jmx;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanException;
import javax.management.ObjectName;
import javax.management.MBeanOperationInfo;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import org.mortbay.jetty.Server;
import org.mortbay.http.HttpServer;
import org.mortbay.http.HttpServer.ComponentEvent;
import org.mortbay.http.HttpServer.ComponentEventListener;
import org.mortbay.http.HttpListener;
import org.mortbay.http.SocketListener;
import org.mortbay.http.HttpContext;
import org.mortbay.jetty.servlet.WebApplicationContext;
import org.mortbay.util.Code;
import org.mortbay.util.Log;
import org.mortbay.util.LogSink;
import org.mortbay.util.LifeCycle;
import org.mortbay.util.OutputStreamLogSink;

import java.beans.beancontext.BeanContextMembershipListener;
import java.beans.beancontext.BeanContextMembershipEvent;

import java.util.Iterator;
import java.util.HashMap;

import java.io.IOException;

/* ------------------------------------------------------------ */
/** JettyServer MBean.
 * This Model MBean class provides the mapping for HttpServer
 * management methods. It also registers itself as a membership
 * listener of the HttpServer, so it can create and destroy MBean
 * wrappers for listeners and contexts.
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class JettyServerMBean extends LifeCycleMBean
    implements HttpServer.ComponentEventListener
{
    private Server _jettyServer;
    private HashMap _mbeanMap = new HashMap();
    private String _configuration;

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @exception MBeanException 
     * @exception InstanceNotFoundException 
     */
    protected JettyServerMBean(Server jettyServer)
        throws MBeanException, InstanceNotFoundException
    {
        super(null);
        _jettyServer=jettyServer;
        _jettyServer.addEventListener(this);
        try{setManagedResource(_jettyServer,"objectReference");}
        catch(InvalidTargetObjectTypeException e){Code.warning(e);}
    }

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @exception MBeanException 
     * @exception InstanceNotFoundException 
     */
    public JettyServerMBean()
        throws MBeanException, InstanceNotFoundException
    {
        this(new Server());
    }

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param configuration URL or File to jetty.xml style configuration file
     * @exception IOException 
     * @exception MBeanException 
     * @exception InstanceNotFoundException 
     */
    public JettyServerMBean(String configuration)
        throws IOException,MBeanException, InstanceNotFoundException
    {
        this();
        _configuration=configuration;
    }

    /* ------------------------------------------------------------ */
    protected String newObjectName(MBeanServer server)
    {
        // Create own ObjectName of the form:
        // package:class=id
        return uniqueObjectName(server, getJettyDomain()+":name=Jetty");
    }

    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();
        
        defineAttribute("logSink");
        defineAttribute("configuration");
        
        defineOperation("addListener",
                        new String[]{"java.lang.String"},
                        IMPACT_ACTION);
        defineOperation("addListener",
                        new String[]{"org.mortbay.http.HttpListener"},
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

        
        defineAttribute("statsOn");
        defineOperation("statsReset",IMPACT_ACTION);
        defineAttribute("connections");
        defineAttribute("connectionsOpen");
        defineAttribute("connectionsOpenMax");
        defineAttribute("connectionsDurationAve");
        defineAttribute("connectionsDurationMax");
        defineAttribute("connectionsRequestsAve");
        defineAttribute("connectionsRequestsMax");
        defineAttribute("errors");
        defineAttribute("requests");
        defineAttribute("requestsActive");
        defineAttribute("requestsActiveMax");
        defineAttribute("requestsDurationAve");
        defineAttribute("requestsDurationMax");
        defineOperation("destroy",IMPACT_ACTION);
    }
    
    /* ------------------------------------------------------------ */
    public synchronized void addComponent(ComponentEvent event)
    {
        try
        {
            Code.debug("Component added ",event);
            Object o=event.getComponent();
            ModelMBeanImpl mbean=null;
            ObjectName oName=null;
            if (o instanceof HttpListener)
            {
            if (o instanceof SocketListener)
                mbean= new SocketListenerMBean(this,(SocketListener)o);
            else
                mbean= new HttpListenerMBean(this,(HttpListener)o);
            }
            else if (o instanceof WebApplicationContext)
                mbean= new WebApplicationMBean(this,(WebApplicationContext)o);
            else if (o instanceof HttpContext)
                mbean= new HttpContextMBean(this,(HttpContext)o);
            else if (o instanceof LogSink)
            {
                LogSink sink =(LogSink)o;
                if (sink instanceof OutputStreamLogSink)
                mbean=new OutputStreamLogSinkMBean(sink,false);
                else
                    mbean= new LogSinkMBean(sink);
                String className = sink.getClass().getName();
                if (className.indexOf(".")>0)
                    className=className.substring(className.lastIndexOf(".")+1);
                
                String name=getObjectName().toString();
                if (sink!=_jettyServer.getLogSink())
                {
                    Iterator ctxs = _jettyServer.getHttpContexts().iterator();
                    while (ctxs.hasNext())
                    {
                        HttpContext c = (HttpContext)ctxs.next();
                        if (sink==c.getLogSink())
                        {
                            name+=",context="+c.getContextPath();
                            break;
                        }
                    }
                }
                
                oName=new ObjectName(uniqueObjectName(getMBeanServer(),
                                                      name+","+
                                                      className+"="));
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
    
    /* ------------------------------------------------------------ */
    public synchronized void removeComponent(ComponentEvent event)
    {
        Code.debug("Component removed ",event);
        
        try
        {
            Object o=event.getComponent();
            ModelMBeanImpl mbean =
                (ModelMBeanImpl)_mbeanMap.remove(o);
            if (mbean==null && o==_jettyServer)
                mbean=this;
            if (mbean!=null)
                mbean.getMBeanServer().unregisterMBean(mbean.getObjectName());
        }
        catch(Exception e)
        {
            Code.warning(e);
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
