/// ========================================================================
// Copyright (c) 2002 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.plus;

import java.io.IOException;
import java.net.URL;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import org.mortbay.jetty.servlet.WebApplicationContext;
import org.mortbay.util.Code;
import org.mortbay.util.Log;
import org.mortbay.util.Resource;
import org.mortbay.util.MultiException;

/* ------------------------------------------------------------ */
/** The Jetty HttpServer.
 *
 * This specialization of org.mortbay.jetty.Server adds knowledge
 * about JNDI and Transaction Management
 * 
 * @author Miro Halas
 */
public class Server extends org.mortbay.jetty.Server 
{
    private  ArrayList _serviceList;



    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public Server ()
    {
       // Don't reset the instance to null since Jetty classes using XML are
       // inialized out of order and it would reset the value to null.
     
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param configuration The filename or URL of the XML
     * configuration file.
     */
    public Server (
       String configuration
    ) throws IOException
    {
        super(configuration);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param configuration The filename or URL of the XML
     * configuration file.
     */
    public Server(
       Resource configuration
    ) throws IOException
    {
        super(configuration);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param configuration The filename or URL of the XML
     * configuration file.
     */
    public Server(
       URL configuration
    ) throws IOException
    {
        super(configuration);
    }



    /* ------------------------------------------------------------ */
    /**
     * Add a Service to a Server. Examples are transaction service,
     * mail service etc
     *
     * @param service eg TMService, MailService
     */
    public void addService (Service service)
    { 
        if (_serviceList == null)
            _serviceList = new ArrayList(5);

        _serviceList.add (service);
        Code.debug ("Service List contains: "+_serviceList.size()+" services");
    }



    /* ------------------------------------------------------------ */
    /** Start all handlers then listeners.
     * If a subcomponent fails to start, it's exception is added to a
     * org.mortbay.util.MultiException and the start method continues.
     * @exception MultiException A collection of exceptions thrown by
     * start() method of subcomponents of the HttpServer. 
     */
    public synchronized void start()
        throws MultiException
    {
        
        if (_serviceList != null)
        {
            MultiException mex = new MultiException();
            
            //iterate over all the services and start them in order
            Iterator itor = _serviceList.iterator();
            while (itor.hasNext())
            {
                try
                {
                    ((Service)itor.next()).start();
                }
                catch (Exception e)
                {
                    mex.add(e);
                }
            }
            
            mex.ifExceptionThrowMulti();
        }


       // Now start the rest of Jetty
       super.start();
    }

    /* ------------------------------------------------------------ */
    /** Stop all listeners then all contexts.
     * @param graceful If true and statistics are on for a context,
     * then this method will wait for requestsActive to go to zero
     * before stopping that context.
     */
    public synchronized void stop(boolean graceful)
        throws InterruptedException
    {
       // First stop rest of jetty 
       super.stop(graceful);

       // now stop all the services, in the reverse order to starting
       if (_serviceList!=null && _serviceList.size()>0)
       {
           ListIterator listItor = _serviceList.listIterator (_serviceList.size());
           while (listItor.hasPrevious())
           {
               Service s = (Service)listItor.previous();
               if (s!=null)
                   s.stop();
           }
       }
    }

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public static void main(String[] arg)
    {
        String[] dftConfig={"etc/jetty.xml"};
        
        if (arg.length==0)
        {
            Log.event("Using default configuration: etc/jetty.xml");
            arg=dftConfig;
        }

        final org.mortbay.jetty.plus.Server[] servers= new org.mortbay.jetty.plus.Server[arg.length];

        // create and start the servers.
        for (int i=0;i<arg.length;i++)
        {
            try
            {
                servers[i] = new org.mortbay.jetty.plus.Server (arg[i]);
                servers[i].start();

            }
            catch(Exception e)
            {
                Code.warning(e);
            }
        }

        // Create and add a shutdown hook
        if (!Boolean.getBoolean("JETTY_NO_SHUTDOWN_HOOK"))
        {
            try
            {
                Method shutdownHook=
                    java.lang.Runtime.class
                    .getMethod("addShutdownHook",new Class[] {java.lang.Thread.class});
                Thread hook = 
                    new Thread() {
                            public void run()
                            {
                                setName("Shutdown");
                                Log.event("Shutdown hook executing");
                                for (int i=0;i<servers.length;i++)
                                {
				    if (servers[i]==null) continue;
                                    try{servers[i].stop();}
                                    catch(Exception e){Code.warning(e);}
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

        // create and start the servers.
        for (int i=0;i<arg.length;i++)
        {
            try{servers[i].join();}
            catch (Exception e){Code.ignore(e);}
        }
    }

    /* ------------------------------------------------------------ */
    /** Create a new WebApplicationContext.
     * Ths method is called by Server to creat new contexts for web 
     * applications.  Thus calls to addWebApplication that result in 
     * a new Context being created will return an correct class instance.
     * Derived class can override this method to create instance of its
     * own class derived from WebApplicationContext in case it needs more
     * functionality.
     * @param webApp The Web application directory or WAR file.
     * @return WebApplicationContext
     */
    protected WebApplicationContext newWebApplicationContext(
       String webApp
    )
    {
        return new PlusWebAppContext(webApp);
    }
}
