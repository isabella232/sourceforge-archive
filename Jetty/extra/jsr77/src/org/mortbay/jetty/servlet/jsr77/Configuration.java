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

package org.mortbay.jetty.servlet.jsr77;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.servlet.Dispatcher;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.WebApplicationContext;



/**
 * 
 * Configuration
 *
 * @author janb
 * 
 *
 */
public class Configuration implements WebApplicationContext.Configuration
{
    public static final String FILTER_PREFIX = "JSR77Filter-";
    private static final Log log = LogFactory.getLog(Configuration.class);
    private WebApplicationContext _context = null;
    
    
    public Configuration ()
    {}
    
    
    public void setWebApplicationContext (WebApplicationContext context)
    {
        _context = context;
    }
    
    public WebApplicationContext getWebApplicationContext ()
    {
        return _context;
    }
    
    /**
     * Not implemented for JSR77
     * @see org.mortbay.jetty.servlet.WebApplicationContext.Configuration#configureClassPath()
     */
    public void configureClassPath () throws Exception
    {       
    }

    /**
     * not implemented for JSR77
     * @see org.mortbay.jetty.servlet.WebApplicationContext.Configuration#configureDefaults()
     */
    public void configureDefaults () throws Exception
    {   
    }

    /** 
     * Configure a filter per servlet that will accumulate JSR77 style
     * statistics.
     * @see org.mortbay.jetty.servlet.WebApplicationContext.Configuration#configureWebApp()
     */
    public void configureWebApp () throws Exception
    {
        if(_context.isStarted())
        {
            if (log.isDebugEnabled()){log.debug("Cannot configure webapp after it is started");};
            return;
        }
           
        
       //get the context, ask it for all of its servlets, configure a Jsr77Filter for 
       //each of them as the LAST filter
        if (null!=_context)
        {
           ServletHolder[] servlets =  _context.getServletHandler().getServlets();
           for (int i=0; null!=servlets && i<servlets.length; i++)
           {
               String filterName = FILTER_PREFIX+servlets[i].getName();
               FilterHolder holder = _context.getWebApplicationHandler().defineFilter(filterName, Jsr77Filter.class.getName());              
               _context.getWebApplicationHandler().addFilterServletMapping(servlets[i].getName(), filterName,Dispatcher.__ALL);
               holder.put ("servlet-name", servlets[i].getName());        
               if (log.isDebugEnabled())
               {
                   log.debug("Configured JSR77 filter "+filterName);
               }
           }
        }        
    }

}
