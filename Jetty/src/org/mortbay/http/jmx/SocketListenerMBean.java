// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http.jmx;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;

/* ------------------------------------------------------------ */
/** 
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class SocketListenerMBean extends HttpListenerMBean
{
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @exception MBeanException 
     * @exception InstanceNotFoundException 
     */
    public SocketListenerMBean()
        throws MBeanException
    {
        super();
    }

    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();
        defineAttribute("lowResourcePersistTimeMs");
        defineAttribute("identifyListener");
    }
}
