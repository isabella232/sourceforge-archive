// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Jetty.JMX;

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

import com.mortbay.HTTP.HttpServer;
import com.mortbay.HTTP.HandlerContext;
import com.mortbay.Util.Code;
import com.mortbay.Util.Log;
import com.mortbay.Util.LifeCycle;
import com.mortbay.Util.LogSink;
import com.mortbay.Util.WriterLogSink;

import java.beans.beancontext.BeanContextMembershipListener;
import java.beans.beancontext.BeanContextMembershipEvent;

import java.util.Iterator;

/* ------------------------------------------------------------ */
/** 
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class HandlerContextMBean extends LifeCycleMBean
{
    private HandlerContext _handlerContext;
    private HttpServerMBean _httpServerMBean;
    private ModelMBeanImpl _logMBean;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @exception MBeanException 
     * @exception InstanceNotFoundException 
     */
    public HandlerContextMBean(HttpServerMBean server,HandlerContext context)
        throws MBeanException, InstanceNotFoundException
    {
        super(context);
        _httpServerMBean=server;
        _handlerContext=context;
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
        
        defineOperation("addHandler",
                        new String[] {INT, "com.mortbay.HTTP.HttpHandler"},
                        IMPACT_ACTION);
        defineOperation("getHandler",
                        new String[] {INT},
                        IMPACT_INFO);
        defineOperation("removeHandler",
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
            uniqueObjectName(server,_httpServerMBean.getObjectName().toString()+
                             ",context="+
                             _handlerContext.getContextPath());
    }
}


