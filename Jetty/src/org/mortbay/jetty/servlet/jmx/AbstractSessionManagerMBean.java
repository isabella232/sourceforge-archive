// ========================================================================
// Copyright (c) 2003 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.servlet.jmx;

import javax.management.MBeanException;

import org.mortbay.jetty.servlet.SessionManager;


/* ------------------------------------------------------------ */
/** 
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class AbstractSessionManagerMBean extends SessionManagerMBean
{
    /* ------------------------------------------------------------ */
    public AbstractSessionManagerMBean()
        throws MBeanException
    {}
    
    /* ------------------------------------------------------------ */
    public AbstractSessionManagerMBean(SessionManager object)
        throws MBeanException
    {
        super(object);
    }
    
    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();
        defineAttribute("maxInactiveInterval"); 
        defineAttribute("scavengePeriod"); 
        defineAttribute("useRequestedId"); 
        defineAttribute("workerName");  
        defineAttribute("sessions"); 
    }

}
