// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http.jmx;

import javax.management.MBeanException;

import org.mortbay.util.jmx.ThreadPoolMBean;

/* ------------------------------------------------------------ */
/** 
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class SocketChannelListenerMBean extends ThreadPoolMBean
{
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @exception MBeanException 
     * @exception InstanceNotFoundException 
     */
    public SocketChannelListenerMBean()
        throws MBeanException
    {
        super();
    }

    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();
        defineAttribute("host");
        defineAttribute("port");
        defineAttribute("maxReadTimeMs");
        defineAttribute("lingerTimeSecs");
        defineAttribute("lowOnResources");
        defineAttribute("outOfResources");
        defineAttribute("defaultScheme");
    }
}
