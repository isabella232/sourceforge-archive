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

import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.j2ee.statistics.ServletStats;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.WebApplicationHandler;
import org.mortbay.jetty.servlet.jsr77.Configuration;
import org.mortbay.jetty.servlet.jsr77.Jsr77Filter;
import org.mortbay.util.LogSupport;
import org.mortbay.util.jmx.ModelMBeanImpl;


/**
 * 
 * Jsr77ServletHolderMBean
 *
 * @author janb
 * @version $Revision$ $Date$
 *
 */
public class Jsr77ServletHolderMBean extends ModelMBeanImpl
{
    private static final Log log = LogFactory.getLog(Jsr77ServletHolderMBean.class);
    private ServletHolder _servletHolder = null;
    private ServletStats _stats = null;
    
    
    public Jsr77ServletHolderMBean ()
    throws MBeanException
    {}
    
    protected void defineManagedResource()
    {
        super.defineManagedResource();
        defineAttribute("stats", READ_ONLY, ON_MBEAN);
        defineAttribute("statisticsProvider", READ_ONLY, ON_MBEAN);

        //yuk! These are here because JBoss wants them. JBoss
        //seemingly has implemented their JSR77 wrappers to suit Tomcat
        defineAttribute("processingTime", READ_ONLY, ON_MBEAN);
        defineAttribute("requestCount", READ_ONLY, ON_MBEAN);
        defineAttribute("minTime",READ_ONLY,ON_MBEAN);
        defineAttribute("maxTime",READ_ONLY, ON_MBEAN);
        _servletHolder=(ServletHolder)getManagedResource();
    }
    
    
    /**StatisticsProvider
     * As per the jsr77 spec, we are providing statistics for a
     * servlet
     * @return true
     */
    public boolean getStatisticsProvider ()
    {
        return true;
    }
    
    /**ServletStats
     * @return the JSR77 servlet stats for the servlet we represent
     */
    public ServletStats getStats ()
    {
        getJsr77Stats();
        return _stats;
    }
    
    /**MaxTime
     * Necessary for JBoss's JSR77 impl.
     * @return the max service time statistic
     */
    public Long getMaxTime ()
    {
        getJsr77Stats();
        
        if (null==_stats)
            return new Long(0L);
        
        return new Long(_stats.getServiceTime().getMaxTime());              
    }
    
    /**MinTime
     * Necessary for JBoss's JSR77 impl.
     * @return the min service time statistic
     */
    public Long getMinTime ()
    {
        getJsr77Stats();
        if (null==_stats)
            return new Long(0L);
        
        return new Long(_stats.getServiceTime().getMinTime());          
    }
   
    
    /**Satisfying JBoss's JSR77 impl
     * @return
     */
    public Long getProcessingTime ()
    {
        return new Long(getTotalTime());
    }
    
    /**Satisfying JBoss's JSR77 impl
     * @return
     */
    public Integer getRequestCount ()
    {
        return new Integer((int)getCount());
    }
    
    /**Count
     * Convenience method. Also helpful for JBoss's JSR77 impl.
     * @return the number of times the servlet service() method has been called.
     */
    private long getCount ()
    {
        getJsr77Stats();
        if (null==_stats)
            return 0L;
        
        return _stats.getServiceTime().getCount();          
    }
    
    /**TotalTime
     * Convenience method. Also helpful for JBoss's JSR77 impl.
     * @return the total time spent in the servlet's service() method.
     */
    private long getTotalTime()
    {
       getJsr77Stats();
        if (null==_stats)
            return 0L;
        
        return _stats.getServiceTime().getTotalTime();    
    }
    
    /** Jsr77Stats
     * Lookup the statistic object for the servlet we represent.
     * Statistics are captured by a filter placed in front of each servlet.
     */
    private void getJsr77Stats ()
    {
        if (null==_stats)
        {
            if (null==_servletHolder)
                return;
            
            String servletName = _servletHolder.getName();
            WebApplicationHandler handler = (WebApplicationHandler)_servletHolder.getHttpHandler();
            
            if (null==handler)
                return;
            
            FilterHolder filterHolder = handler.getFilter (Configuration.FILTER_PREFIX+servletName);
            if (null!=filterHolder)
            {
                Jsr77Filter filter = (Jsr77Filter)filterHolder.getFilter();
                if (null==filter)
                    return;
                
                _stats = filter.getServletStats();
            }
        }
    }
    
   
    public synchronized ObjectName uniqueObjectName(MBeanServer server, String on)
    {
        ObjectName jsr77Name = null;
        String context=_servletHolder.getHttpContext().getContextPath();
        if (context.length()==0)
            context="/";
        
        try
        {
            jsr77Name = new ObjectName(getDefaultDomain()+":J2EEServer=null,J2EEApplication=null,J2EEWebModule="+context+",j2EEType=Servlet,name="+_servletHolder.getName());    
            //oName=new ObjectName(on+",servlet="+_servletHolder.getName()+",stats=jsr77");
        }
        catch(Exception e)
        {
            log.warn(LogSupport.EXCEPTION,e);
        }
        return jsr77Name;
    }
    
 
}
