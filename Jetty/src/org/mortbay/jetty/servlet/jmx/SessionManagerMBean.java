// ========================================================================
// Copyright (c) 2003 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.servlet.jmx;

import javax.management.MBeanException;
import org.mortbay.jetty.servlet.SessionManager;
import org.mortbay.util.jmx.LifeCycleMBean;


/* ------------------------------------------------------------ */
/** 
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class SessionManagerMBean extends LifeCycleMBean
{
    /* ------------------------------------------------------------ */
    public SessionManagerMBean()
        throws MBeanException
    {}
    
    /* ------------------------------------------------------------ */
    public SessionManagerMBean(SessionManager object)
        throws MBeanException
    {
        super(object);
    }
}
