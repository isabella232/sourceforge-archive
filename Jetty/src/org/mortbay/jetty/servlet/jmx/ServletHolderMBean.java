// ========================================================================
// Copyright (c) 2002,2003 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.servlet.jmx;

import javax.management.MBeanException;
import javax.management.MBeanServer;
import org.mortbay.util.jmx.LifeCycleMBean;
import org.mortbay.jetty.servlet.ServletHolder;
import java.util.Iterator;
import java.util.Map;
import javax.management.ObjectName;
import org.mortbay.util.Code;


/* ------------------------------------------------------------ */
/** 
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class ServletHolderMBean extends HolderMBean 
{
    /* ------------------------------------------------------------ */
    private ServletHolder _holder;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @exception MBeanException 
     */
    public ServletHolderMBean()
        throws MBeanException
    {}
    
    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();
        defineAttribute("initOrder");
        _holder=(ServletHolder)getManagedResource();
    }
    
}
