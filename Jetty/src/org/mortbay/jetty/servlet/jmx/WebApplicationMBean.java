// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.servlet.jmx;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;

/* ------------------------------------------------------------ */
/** Web Application MBean.
 * Note that while Web Applications are HttpContexts, the MBean is
 * not derived from HttpContextMBean as they are managed differently.
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class WebApplicationMBean extends ServletHttpContextMBean
{
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @exception MBeanException 
     * @exception InstanceNotFoundException 
     */
    public WebApplicationMBean()
        throws MBeanException
    {}

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
