// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.util.jmx;

import java.util.Iterator;
import java.util.Set;
import java.net.URL;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.loading.MLet;
import org.mortbay.util.Resource;
import org.mortbay.util.Code;
import org.mortbay.util.Log;
import java.lang.reflect.Method;

/* ------------------------------------------------------------ */
/** 
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class Main
{
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
            server =
                MBeanServerFactory.createMBeanServer(ModelMBeanImpl.getDefaultDomain());
            Code.debug("MBeanServer=",server);
            
            // Create and register the MLet
            //mlet = new MLet(new URL[0],Thread.currentThread().getContextClassLoader());
            mlet = new MLet();
            server.registerMBean(mlet,new ObjectName(server.getDefaultDomain(),"service","MLet"));      
            Code.debug("MLet=",mlet);
            
            // Set MLet as classloader for this app
            Thread.currentThread().setContextClassLoader(mlet);

            
            // load config files
            for (int i=0;i<arg.length;i++)
            {
                Log.event("Load "+arg[i]);
                Resource resource = Resource.newResource(arg[i]);
                Set beans=mlet.getMBeansFromURL(resource.getURL().toString());
                Iterator iter=beans.iterator();
                while(iter.hasNext())
                {
                    Object bean=iter.next();
                    if (bean instanceof Throwable)
                    {
                        iter.remove();
                        Code.warning((Throwable)bean);
                    }
                    else if (bean instanceof ObjectInstance)
                    {
                        ObjectInstance oi = (ObjectInstance)bean;

                        if ("com.sun.jdmk.comm.HtmlAdaptorServer".equals(oi.getClassName()))
                        {
                            Log.event("Starting com.sun.jdmk.comm.HtmlAdaptorServer");
                            try{server.invoke(oi.getObjectName(),"start",null,null);}
                            catch(Exception e){Code.warning(e);}
                        }
			
                        else if ("mx4j.adaptor.rmi.jrmp.JRMPAdaptor".equals(oi.getClassName()))
                        {
                            Object[] jndinameargs =  {"jrmp"};
                            String[] jndinametype = {"java.lang.String"};
                            Object[] jndiportargs =  {new Integer(1099)};
                            String[] jndiporttype = {"int"};

                            Log.event("Starting mx4j.tools.naming.NamingService");
			                ObjectName naming = new ObjectName("Naming:type=rmiregistry");
                   			server.createMBean("mx4j.tools.naming.NamingService", naming, null);
			                server.invoke(naming, "start", null, null);

                            try{
                            	server.invoke(oi.getObjectName(),"setJNDIName",jndinameargs,jndinametype);
                            	server.invoke(oi.getObjectName(),"setPort",jndiportargs,jndiporttype);}
                            	catch(Exception e){Code.warning(e);}

                            Log.event("Starting mx4j.adaptor.rmi.jrmp.JRMPAdaptor");

                            try{server.invoke(oi.getObjectName(),"start",null,null);}
                            catch(Exception e){Code.warning(e);} 
                        }


                    }
                }
                
                Code.debug("Loaded "+beans.size(),"MBeans: ",beans);
            }
        }
        catch(Exception e)
        {
            Code.warning(e);
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
        Log.event("started");
        
        if ((!Boolean.getBoolean("JETTY_NO_SHUTDOWN_HOOK")))
        {
            try
            {
                Method shutdownHook=
                    java.lang.Runtime.class.getMethod("addShutdownHook",new Class[] {java.lang.Thread.class});
                Thread hook = new Thread() 
				{
	                public void run()
	                {
	                    setName("Shutdown");
	                    Log.event("Shutdown hook executing");
	                    Set registeredNames = null;
						try 
						{
							//getting jetty server-mbeans
							registeredNames = server.queryNames(new ObjectName("org.mortbay:Server=0"),null);
						} 
						catch (MalformedObjectNameException e) 
						{
							Code.warning("Malformed JMX-Object-Name", e);
						}
						Iterator i = registeredNames.iterator();
	                    while(i.hasNext())
	                    {
	                        ObjectName name = (ObjectName)i.next();
	                        try
	                        {
	                        	if(server.isRegistered(name))
	                        	{
	                        		//do not de-register instances of javax.management.MBeanServerDelegate
	                        		if(!(server.isInstanceOf(name, "javax.management.MBeanServerDelegate")))
	                        		{
	                        		   server.unregisterMBean(name);
	                        		}
	                        	}
	                        }
	                        catch (Exception e)
	                        {
	                        	if(name != null)
	                        	{
	                        		String bname = name.getCanonicalName();
	                        		Code.warning("could not unregister mbean: "+bname+" Exception: " +e);
	                        	}
	                        	else
	                        	{
	                        		Code.warning("could not unregister mbean. Exception: "+e);
	                        	}
	                        	
	                        }
	
	                    }
	                    // Try to avoid JVM crash
	                    try{Thread.sleep(1000);}
	                    catch(Exception e){Code.warning(e);}
	                  }
	                };
                shutdownHook.invoke(Runtime.getRuntime(),
                                    new Object[]{hook});
            }
            catch(Exception e)
            {
                Code.debug("No shutdown hook in JVM ",e);
            }
        }
        
        try
		{        
	        synchronized(mlet)
	        {            
	            mlet.wait();
	        }
        }
        catch(Exception e)
        {
          Log.event("Exception:"+e);
        }
        Log.event("Stopping!");
    }
}
