// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Jetty.JMX;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanOperationInfo;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import com.mortbay.HTTP.HttpServer;
import com.mortbay.HTTP.HttpListener;
import com.mortbay.Util.Code;
import com.mortbay.Util.Log;
import com.mortbay.Util.LifeCycle;

import java.beans.beancontext.BeanContextMembershipListener;
import java.beans.beancontext.BeanContextMembershipEvent;

import java.util.Iterator;

/* ------------------------------------------------------------ */
/** 
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class HttpListenerMBean extends LifeCycleMBean
{
    private HttpListener _httpListener;
    private HttpServerMBean _httpServerMBean;

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @exception MBeanException 
     * @exception InstanceNotFoundException 
     */
    public HttpListenerMBean(HttpServerMBean server,HttpListener listener)
        throws MBeanException, InstanceNotFoundException
    {
        super(listener);
        _httpServerMBean=server;
        _httpListener=listener;
    }

    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();
        defineAttribute("host");
        defineAttribute("port");
        defineAttribute("defaultScheme");
    }
    
    /* ------------------------------------------------------------ */
    protected String newObjectName()
    {
        return uniqueObjectName(_httpServerMBean.getObjectName().toString());
    }
}
