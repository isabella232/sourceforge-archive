// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Jetty.JMX;

import javax.management.MBeanException;
import javax.management.MBeanOperationInfo;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import com.mortbay.Util.Code;
import com.mortbay.Util.LifeCycle;


/* ------------------------------------------------------------ */
/** 
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class LifeCycleMBean extends ModelMBeanImpl
{
    /* ------------------------------------------------------------ */
    public LifeCycleMBean()
        throws MBeanException
    {
        super();
    }
    
    /* ------------------------------------------------------------ */
    public LifeCycleMBean(LifeCycle object)
        throws MBeanException
    {
        super(object);
    }
    
    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();
        defineAttribute("started");
        defineAttribute("destroyed");
        defineOperation("start",MBeanOperationInfo.ACTION);
        defineOperation("stop",MBeanOperationInfo.ACTION);
        defineOperation("destroy",MBeanOperationInfo.ACTION);
    }    
}



