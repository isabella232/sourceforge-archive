// ========================================================================
// Copyright (c) 2003 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.servlet.jmx;

import javax.management.MBeanException;
import org.mortbay.http.jmx.HttpHandlerMBean;
import org.mortbay.jetty.servlet.SessionManager;
import org.mortbay.util.jmx.LifeCycleMBean;


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
        defineAttribute("scavangePeriod"); 
        defineAttribute("useRequestedId"); 
        defineAttribute("workerName");  
        defineAttribute("sessions"); 
    }

}
