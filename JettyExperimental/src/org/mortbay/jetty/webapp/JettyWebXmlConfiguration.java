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

package org.mortbay.jetty.webapp;

import org.mortbay.resource.Resource;
import org.mortbay.xml.XmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * JettyWebConfiguration.
 * 
 * Looks for Xmlconfiguration files in WEB-INF.  Searches in order for the first of jetty6-web.xml, jetty-web.xml or web-jetty.xml
 *
 * @author janb
 * @version $Revision$ $Date$
 *
 */
public class JettyWebXmlConfiguration implements Configuration
{
    private static Logger log= LoggerFactory.getLogger(JettyWebXmlConfiguration.class);
    private WebAppHandler _context;

    
    /**
     * @see org.mortbay.jetty.servlet.WebApplicationContext.Configuration#setWebApplicationContext(org.mortbay.jetty.servlet.WebApplicationContext)
     */
    public void setWebAppHandler (WebAppHandler context)
    {
       _context = context;
    }

    public WebAppHandler getWebAppHandler ()
    {
        return _context;
    }
    
    /** configureClassPath
     * Not used.
     * @see org.mortbay.jetty.servlet.WebApplicationContext.Configuration#configureClassPath()
     */
    public void configureClassLoader () throws Exception
    {
    }

    /** configureDefaults
     * Not used.
     * @see org.mortbay.jetty.servlet.WebApplicationContext.Configuration#configureDefaults()
     */
    public void configureDefaults () throws Exception
    {
    }

    /** configureWebApp
     * Apply web-jetty.xml configuration
     * @see org.mortbay.jetty.servlet.WebApplicationContext.Configuration#configureWebApp()
     */
    public void configureWebApp () throws Exception
    {
        //cannot configure if the _context is already started
        if (_context.isStarted())
        {
            if (log.isDebugEnabled()){log.debug("Cannot configure webapp after it is started");};
            return;
        }
        
        if(log.isDebugEnabled())
            log.debug("Configuring web-jetty.xml");
        
        Resource webInf=getWebAppHandler().getWebInf();
        // handle any WEB-INF descriptors
        if(webInf!=null&&webInf.isDirectory())
        {
            // do jetty.xml file
            Resource jetty=webInf.addPath("jetty6-web.xml");
            if(!jetty.exists())
                jetty=webInf.addPath("jetty-web.xml");
            if(!jetty.exists())
                jetty=webInf.addPath("web-jetty.xml");
            
            if(jetty.exists())
            {
                if(log.isDebugEnabled())
                    log.debug("Configure: "+jetty);
                XmlConfiguration jetty_config=new XmlConfiguration(jetty.getURL());
                jetty_config.configure(getWebAppHandler());
            }
        }
        
    }
    /** configureWebApp
     * Apply web-jetty.xml configuration
     * @see org.mortbay.jetty.servlet.WebApplicationContext.Configuration#configureWebApp()
     */
    public void deconfigureWebApp () throws Exception
    {
    
    }
}
