// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.util.jmx;

import javax.management.MBeanException;

import org.mortbay.util.ThreadPool;


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
    public ThreadPoolMBean(ThreadPool object)
        throws MBeanException
    {
        super(object);
    }
    
    
    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();
        defineAttribute("name");
        defineAttribute("poolName");
        defineAttribute("threads");
        defineAttribute("idleThreads");
        defineAttribute("minThreads");
        defineAttribute("maxThreads");
        defineAttribute("maxIdleTimeMs");
        defineAttribute("threadsPriority");
    }    
}
