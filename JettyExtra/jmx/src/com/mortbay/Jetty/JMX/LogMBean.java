// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Jetty.JMX;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.ReflectionException;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import com.mortbay.Util.Code;
import com.mortbay.Util.Log;
import com.mortbay.Util.LogSink;
import com.mortbay.Util.OutputStreamLogSink;
import com.mortbay.Util.WriterLogSink;

import java.util.HashMap;
import java.util.Iterator;


public class LogMBean extends ModelMBeanImpl
{
    Log _log;
    HashMap _sinks=new HashMap();
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @exception MBeanException 
     * @exception InstanceNotFoundException 
     */
    public LogMBean()
        throws MBeanException, InstanceNotFoundException
    {
        super(Log.instance());
        _log=(Log)getManagedResource(); 
    }

    /* ------------------------------------------------------------ */
    protected void defineManagedResource()
    {
        super.defineManagedResource();

        defineAttribute("logSinks",false);
        defineOperation("add",
                        new String[]{STRING},
                        IMPACT_ACTION);
        defineOperation("add",
                        new String[]{"com.mortbay.Util.LogSink"},
                        IMPACT_ACTION);
        defineOperation("disableLog",
                        NO_PARAMS,
                        IMPACT_ACTION);
        defineOperation("message",
                        new String[]{STRING,STRING},
                        IMPACT_ACTION);
        
    }
    
    /* ------------------------------------------------------------ */
    protected String newObjectName(MBeanServer server)
    {
        return getJettyDomain()+":name=Log";
    }

    /* ------------------------------------------------------------ */
    public void postRegister(Boolean ok)
    {
        super.postRegister(ok);    
        if (ok.booleanValue())
            rescanSinks();
    }
    
    /* ------------------------------------------------------------ */
    public Object getAttribute(String name)
        throws AttributeNotFoundException,
               MBeanException,
               ReflectionException
    {
        if ("logSinks".equals(name))
            rescanSinks();
        return super.getAttribute(name);
    }
    
    /* ------------------------------------------------------------ */
    public Object invoke(String name, Object[] params, String[] signature)
        throws MBeanException,
               ReflectionException
    {
        Object o=super.invoke(name,params,signature);
        if ("add".equals(name))
            rescanSinks();
        return o;
    }
    
    /* ------------------------------------------------------------ */
    private synchronized void rescanSinks()
    {
        LogSink[] sinks = _log.getLogSinks();

        // Add new beans
        for(int i=0;i<sinks.length;i++)
        {
            LogSink sink=sinks[i];

            LogSinkMBean bean=(LogSinkMBean)_sinks.get(sink);
            if (bean==null)
            {
                try
                {
                    if (sink instanceof OutputStreamLogSink)
                        bean=new OutputStreamLogSinkMBean(sink,true);
                    else if (sink instanceof WriterLogSink)
                        bean=new WriterLogSinkMBean(sink,true);
                    else
                        bean=new LogSinkMBean(sink);
                    getMBeanServer().registerMBean(bean,null);
                    _sinks.put(sink,bean);
                }
                catch(Exception e)
                {
                    Code.warning(e);
                }
            }
        }

        // delete old beans
        if (_sinks.size()!=sinks.length)
        {
            Iterator iter=_sinks.keySet().iterator();
        keys:
            while(iter.hasNext())
            {
                try
                {
                    LogSink sink=(LogSink)iter.next();
                    for(int i=0;i<sinks.length;i++)
                        if(sink==sinks[i]) continue keys;
                
                    LogSinkMBean bean=(LogSinkMBean)_sinks.remove(sink);
                    
                    getMBeanServer().unregisterMBean(bean.getObjectName());
                }
                catch(Exception e)
                {
                    Code.warning(e);
                }
            }
        }
    }
}
