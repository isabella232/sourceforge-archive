// ========================================================================
// Copyright (c) 2002,2003 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.servlet.jmx;

import javax.management.MBeanException;
import org.mortbay.http.jmx.HttpHandlerMBean;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.SessionManager;
import org.mortbay.http.PathMap;
import java.util.Iterator;
import java.util.Map;
import javax.management.ObjectName;


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
        defineAttribute("sessionManager",READ_ONLY,ON_MBEAN);
        _servletHandler=(ServletHandler)getManagedResource();
    }

    /* ------------------------------------------------------------ */
    public ObjectName getSessionManager()
    {
        SessionManager sm=_servletHandler.getSessionManager();
        if (sm==null)
            return null;
        ObjectName[] on=getComponentMBeans(new Object[]{sm},null);
        return on[0];
    }

    
    /* ------------------------------------------------------------ */
    public ObjectName[] getServlets()
    {
        return getComponentMBeans(_servletHandler.getServlets(),null);   
    }
    
    /* ------------------------------------------------------------ */
    public void postRegister(Boolean ok)
    {
        super.postRegister(ok);
        if (ok.booleanValue())
            getSessionManager();
    }
}
