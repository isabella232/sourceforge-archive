// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.util.jmx;

import java.util.Iterator;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.loading.MLet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.util.LogSupport;

/* ------------------------------------------------------------ */
/** 
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class Main
{
    private static Log log = LogFactory.getLog(Main.class);

    static MLet mlet;
        
    /* ------------------------------------------------------------ */
    /** 
     * @param arg 
     */
    static void startMLet(String[] arg)
    {
        try
        {
            // Create a MBeanServer
            final MBeanServer server =
                MBeanServerFactory.createMBeanServer(ModelMBeanImpl.getDefaultDomain());
            if(log.isDebugEnabled())log.debug("MBeanServer="+server);
            
            // Create and register the MLet
            mlet = new MLet();
            server.registerMBean(mlet,
                                 new ObjectName(server.getDefaultDomain(),
                                                "service", "MLet"));
            if(log.isDebugEnabled())log.debug("MLet="+mlet);
            
            // Set MLet as classloader for this app
            Thread.currentThread().setContextClassLoader(mlet);

            
            // load config files
            for (int i=0;i<arg.length;i++)
            {
                log.info("Load "+arg[i]);
                Set beans=mlet.getMBeansFromURL(arg[i]);
                Iterator iter=beans.iterator();
                while(iter.hasNext())
                {
                    Object bean=iter.next();
                    if (bean instanceof Throwable)
                    {
                        iter.remove();
                        log.warn((Throwable)bean);
                    }
                    else if (bean instanceof ObjectInstance)
                    {
                        ObjectInstance oi = (ObjectInstance)bean;

                        if ("com.sun.jdmk.comm.HtmlAdaptorServer".equals(oi.getClassName()))
                        {
                            log.info("Starting com.sun.jdmk.comm.HtmlAdaptorServer");
                            try{server.invoke(oi.getObjectName(),"start",null,null);}
                            catch(Exception e){log.warn(LogSupport.EXCEPTION,e);}
                        }
                    }
                }
                
                if(log.isDebugEnabled())log.debug("Loaded "+beans.size()+" MBeans: "+beans);
            }
        }
        catch(Exception e)
        {
            log.warn(LogSupport.EXCEPTION,e);
        }
    }

    /* ------------------------------------------------------------ */
    public static void main(String[] arg)
        throws Exception
    {
        if (arg.length==0)
        {
            System.err.println("Usage - java org.mortbay.util.jmx.Main <mletURL>...");
            System.exit(1);
        }
        startMLet(arg);
        synchronized(mlet)
        {
            mlet.wait();
        }
    }
}
