// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Jetty.JMX;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MBeanOperationInfo;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import com.mortbay.HTTP.HttpServer;
import com.mortbay.HTTP.WebApplicationContext;
import com.mortbay.Util.Code;
import com.mortbay.Util.Log;
import com.mortbay.Util.LifeCycle;

import java.beans.beancontext.BeanContextMembershipListener;
import java.beans.beancontext.BeanContextMembershipEvent;

import java.util.Iterator;

/* ------------------------------------------------------------ */
/** Web Application MBean.
 * Note that while Web Applications are HandlerContexts, the MBean is
 * not derived from HandlerContextMBean as they are managed differently.
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class WebApplicationMBean extends LifeCycleMBean
{
    private WebApplicationContext _webApplication;
    private HttpServerMBean _httpServerMBean;

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @exception MBeanException 
     * @exception InstanceNotFoundException 
     */
    public WebApplicationMBean(HttpServerMBean server,WebApplicationContext context)
        throws MBeanException, InstanceNotFoundException
    {
        super(context);
        _httpServerMBean=server;
        _webApplication=context;
    }

    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();

        defineAttribute("contextPath",false);
        defineAttribute("classPath",false);
        defineAttribute("classLoader",false);
        defineAttribute("displayName",false);
        defineAttribute("defaultsDescriptor",false);
        defineAttribute("deploymentDescriptor",false);
        defineAttribute("dynamicServletPathSpec",false);
        defineAttribute("hosts",false);
        defineAttribute("handlers",false);
        defineAttribute("realm",false);
        defineAttribute("redirectNullPath");
        defineAttribute("resourceBase",false);
        defineAttribute("servingResources",false);
        defineAttribute("WAR",false);
        
        defineOperation("getInitParameter",
                        new String[] {STRING},
                        IMPACT_INFO);
        defineOperation("getInitParameterNames",
                        NO_PARAMS,
                        IMPACT_INFO);
        
        defineOperation("setAttribute",
                        new String[] {STRING,OBJECT},
                        IMPACT_ACTION);
        defineOperation("getAttribute",
                        new String[] {STRING},
                        IMPACT_INFO);
        defineOperation("getAttributeNames",
                        NO_PARAMS,
                        IMPACT_INFO);
        defineOperation("removeAttribute",
                        new String[] {STRING},
                        IMPACT_ACTION);
    }
    
    /* ------------------------------------------------------------ */
    protected String newObjectName(MBeanServer server)
    {
        return
            uniqueObjectName(server,_httpServerMBean.getObjectName().toString()+
                             ",context="+
                             _webApplication.getContextPath());
    }
}
