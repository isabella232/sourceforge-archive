//========================================================================
//$Id$
//Copyright 2000-2004 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.mortbay.jetty.servlet.jsr77.jmx;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.management.MBeanException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.WebApplicationContext;
import org.mortbay.util.LogSupport;


/**
 * 
 * ConfigurationMBean
 *
 * @author janb
 * @version $Revision$ $Date$
 *
 */
public class ConfigurationMBean extends org.mortbay.jetty.servlet.jmx.ConfigurationMBean
{
    private static final Log log = LogFactory.getLog(ConfigurationMBean.class);
    private Map jsr77MBeanMap = new HashMap();
    
    public ConfigurationMBean () 
    throws MBeanException
    {}
    
    protected void defineManagedResource()
    {
        super.defineManagedResource();
    }
    
   
    
    /**postRegister
     * Register the other jsr77 mbeans
     * @see javax.management.MBeanRegistration#postRegister(java.lang.Boolean)
     */
    
    public void postRegister(Boolean ok)
    {
        super.postRegister(ok);
        try
        {
            defineJsr77MBeans();
        }
        catch (Exception e)
        {
            log.warn(LogSupport.EXCEPTION,e);
        }
    }
    
    /**postDeregister
     * Deregister also all of the jsr77 mbeans we were responsible for 
     * registering.
     * @see javax.management.MBeanRegistration#postDeregister()
     */
    public void postDeregister ()
    {
        Iterator itor = jsr77MBeanMap.entrySet().iterator();
        while (itor.hasNext())
        {
            try
            {
                Map.Entry entry = (Map.Entry)itor.next();
                getMBeanServer().unregisterMBean((ObjectName)entry.getValue());
            }
            catch (Exception e)
            {
                log.warn (LogSupport.EXCEPTION, e);
            }
        }
    }
    

    
    /**defineJsr77MBeans
     * Make and register an mbean for each of the jsr77 servlet stats
     * @throws Exception
     */
    private void defineJsr77MBeans ()
    throws Exception
    {
        WebApplicationContext context = ((org.mortbay.jetty.servlet.jsr77.Configuration)_config).getWebApplicationContext();       
        ServletHolder[] servlets =  context.getServletHandler().getServlets();
        for (int i=0; null!=servlets && i<servlets.length;i++)
        {
            Jsr77ServletHolderMBean mbean = new Jsr77ServletHolderMBean();
            mbean.setManagedResource(servlets[i], "objectReference");          
            mbean.setBaseObjectName(getBaseObjectName().toString());
            ObjectName oname = getMBeanServer().registerMBean(mbean,null).getObjectName();
            jsr77MBeanMap.put (servlets[i].getName(), oname);
        }
    }
    
 
}
