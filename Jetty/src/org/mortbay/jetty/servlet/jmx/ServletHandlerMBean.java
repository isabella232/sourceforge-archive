// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.servlet.jmx;

import javax.management.MBeanException;
import org.mortbay.http.jmx.HttpHandlerMBean;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.http.PathMap;
import java.util.Iterator;
import java.util.Map;


/* ------------------------------------------------------------ */
/** 
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class ServletHandlerMBean extends HttpHandlerMBean  
{
    /* ------------------------------------------------------------ */
    private ServletHandler _servletHandler;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @exception MBeanException 
     */
    public ServletHandlerMBean()
        throws MBeanException
    {}
    
    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();
        defineAttribute("usingCookies"); 
        defineAttribute("servlets",READ_ONLY,ON_MBEAN);
        _servletHandler=(ServletHandler)getManagedResource();
    }

    /* ------------------------------------------------------------ */
    public String[] getServlets()
    {
        PathMap sm = _servletHandler.getServletMap();
        String[] s = new String[sm.size()];
        int i=0;
        Iterator iter = sm.entrySet().iterator();
        while(iter.hasNext())
        {
            Map.Entry entry = (Map.Entry)iter.next();
            s[i++]=entry.toString();
        }
        
        return s;
    }
}
