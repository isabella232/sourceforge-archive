// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.util.jmx;

import javax.management.MBeanException;
import javax.management.MBeanOperationInfo;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import org.mortbay.util.Code;
import org.mortbay.util.LifeCycle;


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
    {}
    
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
        defineOperation("start",MBeanOperationInfo.ACTION);
        defineOperation("stop",MBeanOperationInfo.ACTION);
    }    
}



