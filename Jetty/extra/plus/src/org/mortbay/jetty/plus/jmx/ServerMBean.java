// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.plus.jmx;

import java.io.IOException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.mortbay.http.jmx.HttpServerMBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.util.LogSupport;


/* ------------------------------------------------------------ */
/** JettyPlus Server MBean.
 * This Model MBean class provides the mapping for HttpServer
 * management methods. It also registers itself as a membership
 * listener of the HttpServer, so it can create and destroy MBean
 * wrappers for listeners and contexts.
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class ServerMBean extends org.mortbay.jetty.jmx.ServerMBean
{
    private static Log log = LogFactory.getLog(ServerMBean.class);
    org.mortbay.jetty.plus.Server _jettyServer;
    String _configuration;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @exception MBeanException 
     * @exception InstanceNotFoundException 
     */
    protected ServerMBean(org.mortbay.jetty.plus.Server jettyServer)
        throws MBeanException, InstanceNotFoundException
    {
        super(jettyServer);
    }

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @exception MBeanException 
     * @exception InstanceNotFoundException 
     */
    public ServerMBean()
        throws MBeanException, InstanceNotFoundException
    {
        this(new org.mortbay.jetty.plus.Server());
    }

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param configuration URL or File to jetty.xml style configuration file
     * @exception IOException 
     * @exception MBeanException 
     * @exception InstanceNotFoundException 
     */
    public ServerMBean(String configuration)
        throws IOException,MBeanException, InstanceNotFoundException
    {
        this(new org.mortbay.jetty.plus.Server());
        _configuration=configuration;
    }

    /* ------------------------------------------------------------ */
    protected ObjectName newObjectName(MBeanServer server)
    {
        return uniqueObjectName(server, getDefaultDomain()+":Server=");
    }

    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();
        
        _jettyServer=(org.mortbay.jetty.plus.Server)getManagedResource();
    }
    
    
    /* ------------------------------------------------------------ */
    /** 
     * @param ok 
     */
    public void postRegister(Boolean ok)
    {
        super.postRegister(ok);
        
        if (ok.booleanValue())
        {
            if (_configuration!=null)
            {
                try
                {
                    _jettyServer.configure(_configuration);
                    _jettyServer.start();
                }
                catch(Exception e)
                {
		    log.warn(e);
                }
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    public void postDeregister()
    {
        _configuration=null;   
        super.postDeregister();
    }
}
