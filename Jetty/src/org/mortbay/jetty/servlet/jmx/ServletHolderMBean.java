// ========================================================================
// Copyright (c) 2002,2003 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.servlet.jmx;

import javax.management.MBeanException;
import javax.management.MBeanServer;
import org.mortbay.util.jmx.LifeCycleMBean;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.ServletHandler;
import java.util.Iterator;
import java.util.ArrayList;
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
        defineAttribute("paths",READ_ONLY,ON_MBEAN);
        _holder=(ServletHolder)getManagedResource();
    }

    /* ------------------------------------------------------------ */
    public String[] getPaths()
    {
        ServletHandler handler = (ServletHandler)_holder.getHttpHandler();
        Map servletMap = handler.getServletMap();
        ArrayList paths = new ArrayList(servletMap.size());
        Iterator iter = servletMap.entrySet().iterator();
        while (iter.hasNext())
        {
            Map.Entry entry =(Map.Entry)iter.next();
            if (entry.getValue()==_holder)
                paths.add(entry.getKey());
        }
        return (String[])paths.toArray(new String[paths.size()]);
    }
    
    
}
