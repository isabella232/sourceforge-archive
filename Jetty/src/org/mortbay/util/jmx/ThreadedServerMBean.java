// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.util.jmx;

import javax.management.MBeanException;

import org.mortbay.util.ThreadedServer;

/* ------------------------------------------------------------ */
/** 
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class ThreadedServerMBean extends ThreadPoolMBean
{
    /* ------------------------------------------------------------ */
    public ThreadedServerMBean()
        throws MBeanException
    {
        super();
    }
    
    /* ------------------------------------------------------------ */
    public ThreadedServerMBean(ThreadedServer object)
        throws MBeanException
    {
        super(object);
    }
    
    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();

        defineAttribute("host");
        defineAttribute("port");
        defineAttribute("tcpNoDelay");
        defineAttribute("lingerTimeSecs");
    }    
}
