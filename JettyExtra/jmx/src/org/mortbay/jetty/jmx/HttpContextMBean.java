// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.jmx;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanException;
import javax.management.MBeanOperationInfo;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import org.mortbay.http.HttpServer;
import org.mortbay.http.HttpContext;
import org.mortbay.util.Code;
import org.mortbay.util.Log;
import org.mortbay.util.LifeCycle;
import org.mortbay.util.LogSink;

import java.beans.beancontext.BeanContextMembershipListener;
import java.beans.beancontext.BeanContextMembershipEvent;

import java.util.Iterator;

/* ------------------------------------------------------------ */
/** 
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class HttpContextMBean extends LifeCycleMBean
{
    private HttpContext _httpContext;
    private JettyServerMBean _jettyServerMBean;
    private ModelMBeanImpl _logMBean;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @exception MBeanException 
     * @exception InstanceNotFoundException 
     */
    public HttpContextMBean(JettyServerMBean server,HttpContext context)
        throws MBeanException, InstanceNotFoundException
    {
        super(context);
        _jettyServerMBean=server;
        _httpContext=context;
    }

    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();

        defineAttribute("logSink");
        defineAttribute("contextPath");
        defineAttribute("classPath");
        defineAttribute("classLoader");
        defineAttribute("realm");
        defineAttribute("redirectNullPath");
        defineAttribute("resourceBase");
        defineAttribute("servingResources");
        defineAttribute("hosts");
        defineAttribute("handlers");
        
        defineAttribute("statsOn");
        defineOperation("statsReset",IMPACT_ACTION);
        defineAttribute("requests");
        defineAttribute("responses1xx");
        defineAttribute("responses2xx");
        defineAttribute("responses3xx");
        defineAttribute("responses4xx");
        defineAttribute("responses5xx");
        
        
        defineOperation("setInitParameter",
                        new String[] {STRING,STRING},
                        IMPACT_ACTION);
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
        
        defineOperation("addHttpHandler",
                        new String[] {INT, "org.mortbay.http.HttpHandler"},
                        IMPACT_ACTION);
        defineOperation("getHttpHandler",
                        new String[] {INT},
                        IMPACT_INFO);
        defineOperation("removeHttpHandler",
                        new String[] {INT},
                        IMPACT_ACTION);
        
        defineOperation("setMimeMapping",
                        new String[] {STRING,STRING},
                        IMPACT_ACTION);
    }
    
    
    /* ------------------------------------------------------------ */
    protected String newObjectName(MBeanServer server)
    {
        return
            uniqueObjectName(server,_jettyServerMBean.getObjectName().toString()+
                             ",context="+
                             _httpContext.getContextPath());
    }
}


