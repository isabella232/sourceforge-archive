//========================================================================
//$Id$
//Copyright 200-2004 Mort Bay Consulting Pty. Ltd.
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

import java.io.IOException;

import javax.management.j2ee.statistics.ServletStats;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;


//TODO
// 1. find a way to configure the servlets for which jsr77 filter has to apply
// 2. pass the name of the servlet to the filter itself - can we use the FilterConfig init-param?
// 3. make the statistics inside the filter available on the mbean for the ServletHolder
//    corresponding to the servlet to be compliant with JBoss


/**
 * Jsr77Filter
 * 
 * 
 * A filter to collect JSR77 servlet statistics. One instance of this
 * filter sits in front of every servlet for which statistics are to be gathered.
 *
 * The filter makes available the statistics for the servlet on whose behalf
 * the statistics are being collected.
 * 
 * 
 * @author janb
 */
public class Jsr77Filter implements Filter
{
    private ServletStatsImpl servletStats = null;
    private String servletName = null;
    
    public Jsr77Filter ()
    {}

    /** Initialise the filter
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    public void init (FilterConfig filterConfig) throws ServletException
    {
        servletName = filterConfig.getInitParameter("servlet-name");
        servletStats = new ServletStatsImpl(servletName);  
    }

    /** Collect statistics
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter (ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        long startTime =0L;
        long endTime = 0L;
        try
        {
            //start statistic gathering - get the name of Servlet for which this filter will apply, and therefore
            //on whose behalf we are gathering statistics???
            startTime = System.currentTimeMillis();
            chain.doFilter(request, response);
        }
        finally
        {
            //finish statistic gathering
            endTime = System.currentTimeMillis();
            TimeStatisticImpl statistic = (TimeStatisticImpl)servletStats.getServiceTime();
            statistic.addSample(endTime-startTime, endTime);
        }       
    }
    
    public ServletStats getServletStats ()
    {
        return servletStats;
    }

    /** Destroy filter
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy ()
    {
        // TODO Auto-generated method stub
        
    }
    
    
    

}
