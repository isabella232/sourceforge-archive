// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Jetty.JMX;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import com.mortbay.Util.Code;
import com.mortbay.Util.Log;

import java.beans.beancontext.BeanContextMembershipListener;
import java.beans.beancontext.BeanContextMembershipEvent;

import java.util.Iterator;


public class DebugMBean extends ModelMBeanImpl
{
    private Code _code;

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @exception MBeanException 
     * @exception InstanceNotFoundException 
     */
    public DebugMBean()
        throws MBeanException, InstanceNotFoundException
    {
        super(Code.instance());
        _code=(Code)getManagedResource(); 
    }

    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();

        defineAttribute("debug");
        defineAttribute("suppressStack");
        defineAttribute("suppressWarnings");
        defineAttribute("verbose");
        defineAttribute("debugPatterns");
        defineAttribute("debugTriggers");
    }
    
    /* ------------------------------------------------------------ */
    protected String newObjectName(MBeanServer server)
    {
        return getJettyDomain()+":name=Debug";
    }
}


