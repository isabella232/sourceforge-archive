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

package org.mortbay.util.jmx;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.loading.MLet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.util.LogSupport;
import org.mortbay.util.Resource;

/* ------------------------------------------------------------ */
/**
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class Main
{

    private static Log log = LogFactory.getLog(Main.class);

    static MLet mlet;

    static MBeanServer server = null;

    /* ------------------------------------------------------------ */
    /**
     * @param arg
     */
    static void startMLet(String[] arg)
    {
        try
        {
            // Create a MBeanServer
            server = MBeanServerFactory.createMBeanServer(ModelMBeanImpl.getDefaultDomain());
            if (log.isDebugEnabled()) log.debug("MBeanServer=" + server);

            // Create and register the MLet
            mlet = new MLet(new URL[0],Thread.currentThread().getContextClassLoader());
            server.registerMBean(mlet, new ObjectName(server.getDefaultDomain(), "service",
                            "MLet"));
            if (log.isDebugEnabled()) log.debug("MLet=" + mlet);

            // Set MLet as classloader for this app
            Thread.currentThread().setContextClassLoader(mlet);

            // load config files
            for (int i = 0; i < arg.length; i++)
            {
                log.info("Load " + arg[i]);
                Resource resource = Resource.newResource(arg[i]);
                Set beans = mlet.getMBeansFromURL(resource.getURL().toString());
                Iterator iter = beans.iterator();
                while (iter.hasNext())
                {
                    Object bean = iter.next();
                    if (bean instanceof Throwable)
                    {
                        iter.remove();
                        log.warn((Throwable) bean);
                    }
                    else if (bean instanceof ObjectInstance)
                    {
                        ObjectInstance oi = (ObjectInstance) bean;

                        if ("com.sun.jdmk.comm.HtmlAdaptorServer".equals(oi.getClassName()))
                        {
                            log.info("Starting com.sun.jdmk.comm.HtmlAdaptorServer");
                            try
                            {
                                server.invoke(oi.getObjectName(), "start", null, null);
                            }
                            catch (Exception e)
                            {
                                log.warn(LogSupport.EXCEPTION, e);
                            }
                        }

                        else if ("mx4j.adaptor.rmi.jrmp.JRMPAdaptor".equals(oi.getClassName()))
                        {
                            Object[] jndinameargs = { "jrmp"};
                            String[] jndinametype = { "java.lang.String"};
                            Object[] jndiportargs = { new Integer(1099)};
                            String[] jndiporttype = { "int"};

                            log.info("Starting mx4j.tools.naming.NamingService");
                            ObjectName naming = new ObjectName("Naming:type=rmiregistry");
                            server.createMBean("mx4j.tools.naming.NamingService", naming, null);
                            server.invoke(naming, "start", null, null);

                            try
                            {
                                server.invoke(oi.getObjectName(), "setJNDIName", jndinameargs,
                                        jndinametype);
                                server.invoke(oi.getObjectName(), "setPort", jndiportargs,
                                        jndiporttype);
                            }
                            catch (Exception e)
                            {
                                log.warn(e);
                            }

                            log.info("Starting mx4j.adaptor.rmi.jrmp.JRMPAdaptor");

                            try
                            {
                                server.invoke(oi.getObjectName(), "start", null, null);
                            }
                            catch (Exception e)
                            {
                                log.warn(e);
                            }
                        }

                    }
                }

                if (log.isDebugEnabled())
                        log.debug("Loaded " + beans.size() + " MBeans: " + beans);
            }
        }
        catch (Exception e)
        {
            log.warn(LogSupport.EXCEPTION, e);
        }
    }

    /* ------------------------------------------------------------ */
    public static void main(String[] arg) throws Exception
    {
        if (arg.length == 0)
        {
            System.err.println("Usage - java org.mortbay.util.jmx.Main <mletURL>...");
            System.exit(1);
        }
        startMLet(arg);

        if ((!Boolean.getBoolean("JETTY_NO_SHUTDOWN_HOOK")))
        {
            try
            {
                Method shutdownHook = java.lang.Runtime.class.getMethod("addShutdownHook",
                        new Class[] { java.lang.Thread.class});
                Thread hook = new Thread()
                {

                    public void run()
                    {
                        setName("Shutdown");
                        log.info("Shutdown hook executing");
                        Set registeredNames = null;
                        try
                        {
                            //getting jetty server-mbeans
                            registeredNames = server.queryNames(new ObjectName(
                                    "org.mortbay:Server=0"), null);
                        }
                        catch (MalformedObjectNameException e)
                        {
                            log.warn("Malformed JMX-Object-Name", e);
                        }
                        Iterator i = registeredNames.iterator();
                        while (i.hasNext())
                        {
                            ObjectName name = (ObjectName) i.next();
                            try
                            {
                                if (server.isRegistered(name))
                                {
                                    //do not de-register instances of
                                    // javax.management.MBeanServerDelegate
                                    if (!(server.isInstanceOf(name,
                                            "javax.management.MBeanServerDelegate")))
                                    {
                                        server.unregisterMBean(name);
                                    }
                                }
                            }
                            catch (Exception e)
                            {
                                if (name != null)
                                {
                                    String bname = name.getCanonicalName();
                                    log.warn("could not unregister mbean: " + bname
                                            + " Exception: " + e);
                                }
                                else
                                {
                                    log.warn("could not unregister mbean. Exception: " + e);
                                }

                            }

                        }
                        // Try to avoid JVM crash
                        try
                        {
                            Thread.sleep(1000);
                        }
                        catch (Exception e)
                        {
                            log.warn(e);
                        }
                    }
                };
                shutdownHook.invoke(Runtime.getRuntime(), new Object[] { hook});
            }
            catch (Exception e)
            {
                log.debug("No shutdown hook in JVM ", e);
            }
        }

        try
        {
            synchronized (mlet)
            {
                mlet.wait();
            }
        }
        catch (Exception e)
        {
            log.info("Exception:" + e);
        }
    }
}
