// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.jmx;

import javax.management.MBeanException;
import javax.management.MBeanOperationInfo;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import org.mortbay.util.Code;
import org.mortbay.util.ThreadPool;
import org.mortbay.util.LifeCycle;


/* ------------------------------------------------------------ */
/** 
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class ThreadPoolMBean extends LifeCycleMBean
{
    /* ------------------------------------------------------------ */
    public ThreadPoolMBean()
        throws MBeanException
    {
        super();
    }
    
    /* ------------------------------------------------------------ */
    public ThreadPoolMBean(LifeCycle object)
        throws MBeanException
    {
        super(object);
    }
    
    
    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();
        if (getManagedResource() instanceof ThreadPool)
        {
            defineAttribute("name");
            defineAttribute("threadClass");
            defineAttribute("threads");
            defineAttribute("idleThreads");
            defineAttribute("minThreads");
            defineAttribute("maxThreads");
            defineAttribute("maxIdleTimeMs");
            defineAttribute("maxStopTimeMs");
        }
    }    
}
