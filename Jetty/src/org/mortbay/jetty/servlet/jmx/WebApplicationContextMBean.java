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

package org.mortbay.jetty.servlet.jmx;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.management.MBeanException;
import javax.management.ObjectName;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.servlet.WebApplicationContext;
import org.mortbay.util.LogSupport;

/* ------------------------------------------------------------ */
/** Web Application MBean.
 * Note that while Web Applications are HttpContexts, the MBean is
 * not derived from HttpContextMBean as they are managed differently.
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class WebApplicationContextMBean extends ServletHttpContextMBean
{
    private static final Log log = LogFactory.getLog(WebApplicationContextMBean.class);
    private WebApplicationContext _webappContext;
    private Map _configurations = new HashMap();
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @exception MBeanException 
     */
    public WebApplicationContextMBean()
        throws MBeanException
    {}

    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();

        defineAttribute("displayName",false);
        defineAttribute("defaultsDescriptor",true);
        defineAttribute("WAR",true);
        defineAttribute("extractWAR",true);
        _webappContext=(WebApplicationContext)getManagedResource();
    }
    
    
    /** postRegister
     * Register mbeans for all of the jsr77 servlet stats
     * @see javax.management.MBeanRegistration#postRegister(java.lang.Boolean)
     */
    public void postRegister(Boolean ok)
    {
        super.postRegister(ok);
        //register as a listener on the WebApplicationContext to
        //find out when it is started so that all of the jsr77
        //objects will have been created, and therefore we can
        //register mbeans for them
        try
        {
            _webappContext.addEventListener (
                    new ServletContextListener()
                    {
                        public void contextInitialized (ServletContextEvent e)
                        {
                            getConfigurations();
                        }
                        public void contextDestroyed (ServletContextEvent e)
                        {
                            
                        }
                    });
            
        }
        catch (Exception e)
        {
            log.warn(LogSupport.EXCEPTION,e);
        }
    }
    
    
    
    /**postDeregister
     * Unregister mbeans we created for the Configuration objects.
     * @see javax.management.MBeanRegistration#postDeregister()
     */
    public void postDeregister ()
    {
        super.postDeregister();
        
        Iterator itor = _configurations.values().iterator();
        while (itor.hasNext())
        {
            try
            {
                getMBeanServer().unregisterMBean((ObjectName)itor.next());
            }
            catch (Exception e)
            {
                log.warn(LogSupport.EXCEPTION, e);
            }
        }
    }
   
    
    /**getConfigurations
     * Make mbeans for all of the Configurations applied to the
     * WebApplicationContext
     * @return
     */
    public ObjectName[] getConfigurations ()
    { 
        return getComponentMBeans(_webappContext.getConfigurations(),_configurations); 
    }
    
}
