// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http.jmx;

import javax.management.MBeanException;
import org.mortbay.util.jmx.ThreadedServerMBean;

/* ------------------------------------------------------------ */
/** 
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class HttpListenerMBean
    extends ThreadedServerMBean
{
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @exception MBeanException 
     */
    public HttpListenerMBean()
        throws MBeanException
    {}
    
    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();
        defineAttribute("defaultScheme");
        defineAttribute("lowOnResources",false);
        defineAttribute("outOfResources",false);
        defineAttribute("confidentialPort");
        defineAttribute("confidentialScheme");
        defineAttribute("integralPort");
        defineAttribute("integralScheme"); 
        defineAttribute("bufferSize");  
        defineAttribute("bufferReserve"); 
    }    
}
