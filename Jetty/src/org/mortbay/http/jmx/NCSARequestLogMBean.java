// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http.jmx;

import javax.management.MBeanException;

import org.mortbay.util.jmx.LifeCycleMBean;

/* ------------------------------------------------------------ */
/** 
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class NCSARequestLogMBean extends LifeCycleMBean
{
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param sink 
     * @exception MBeanException 
     * @exception InstanceNotFoundException 
     */
    public NCSARequestLogMBean()
        throws MBeanException
    {}    

    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();
        defineAttribute("filename");
        defineAttribute("datedFilename");
        defineAttribute("logDateFormat");
        defineAttribute("logTimeZone");
        defineAttribute("retainDays");
        defineAttribute("extended");
        defineAttribute("append");
    }
}
