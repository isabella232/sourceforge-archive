// ========================================================================
// Copyright (c) 2002,2003 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.servlet.jmx;

import javax.management.MBeanException;
import javax.management.MBeanServer;
import org.mortbay.util.jmx.LifeCycleMBean;
import org.mortbay.jetty.servlet.FilterHolder;
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
public class FilterHolderMBean extends HolderMBean 
{
    /* ------------------------------------------------------------ */
    private FilterHolder _holder;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @exception MBeanException 
     */
    public FilterHolderMBean()
        throws MBeanException
    {}
    
    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();
        defineAttribute("paths");
        defineAttribute("servlets");
        _holder=(FilterHolder)getManagedResource();
    }
    
}
