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
import com.mortbay.Jetty.Servlet.WebApplicationContext;
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
public class WebApplicationMBean extends ServletHandlerContextMBean
{
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @exception MBeanException 
     * @exception InstanceNotFoundException 
     */
    public WebApplicationMBean(HttpServerMBean server,WebApplicationContext context)
        throws MBeanException, InstanceNotFoundException
    {
        super(server,context);
    }

    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();

        defineAttribute("displayName",false);
        defineAttribute("defaultsDescriptor",true);
        defineAttribute("deploymentDescriptor",false);
        defineAttribute("WAR",true);
        defineAttribute("extractWAR",true);
    }
    
}
