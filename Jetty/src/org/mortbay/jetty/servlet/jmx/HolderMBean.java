// ========================================================================
// Copyright (c) 2002,2003 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.servlet.jmx;

import javax.management.MBeanException;
import javax.management.MBeanServer;
import org.mortbay.util.jmx.LifeCycleMBean;
import org.mortbay.jetty.servlet.Holder;
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
public class HolderMBean extends LifeCycleMBean  
{
    /* ------------------------------------------------------------ */
    private Holder _holder;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @exception MBeanException 
     */
    public HolderMBean()
        throws MBeanException
    {}
    
    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();
        
        defineAttribute("name");
        defineAttribute("displayName");
        defineAttribute("className");
        defineAttribute("initParameters",READ_ONLY,ON_MBEAN);
        
        _holder=(Holder)getManagedResource();
    }
    
    /* ---------------------------------------------------------------- */
    public String getInitParameters()
    {
        return ""+_holder.getInitParameters();
    }
    
    /* ------------------------------------------------------------ */
    public synchronized ObjectName uniqueObjectName(MBeanServer server,
                                                    String objectName)
    {
        try
        {
            String name=_holder.getDisplayName();
            if (name==null || name.length()==0)
                name=_holder.getClassName();
            return new ObjectName(objectName+",name="+name);
        }
        catch(Exception e)
        {
            Code.warning(e);
            return super.uniqueObjectName(server,objectName);
        }
    }
}
