// ========================================================================
// $Id$
// Copyright 1999-2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.mortbay.jetty.plus.jmx;

import java.io.IOException;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


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
        
        defineOperation ("addService", new String[] {"org.mortbay.jetty.plus.Service"}, IMPACT_ACTION);
        
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
