package org.mortbay.jetty;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpServer;
import org.mortbay.jetty.servlet.ServletHttpContext;
import org.mortbay.jetty.servlet.WebApplicationContext;
import org.mortbay.util.Code;
import org.mortbay.util.Log;
import org.mortbay.util.Resource;
import org.mortbay.xml.XmlConfiguration;


/* ------------------------------------------------------------ */
/** The Jetty HttpServer.
 *
 * This specialization of org.mortbay.http.HttpServer adds knowledge
 * about servlets and their specialized contexts.   It also included
 * support for initialization from xml configuration files
 * that follow the XmlConfiguration dtd.
 *
 * HttpContexts created by Server are of the type
 * org.mortbay.jetty.servlet.ServletHttpContext unless otherwise
 * specified.
 *
 * This class also provides a main() method which starts a server for
 * each config file passed on the command line.  If the system
 * property JETTY_NO_SHUTDOWN_HOOK is not set to true, then a shutdown
 * hook is thread is registered to stop these servers.   
 *
 * @see org.mortbay.xml.XmlConfiguration
 * @see org.mortbay.jetty.servlet.ServletHttpContext
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class Server extends HttpServer
{
    private String _configuration;

    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public Server()
    {}
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param configuration The filename or URL of the XML
     * configuration file.
     */
    public Server(String configuration)
        throws IOException
    {
        this(Resource.newResource(configuration).getURL());
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param configuration The filename or URL of the XML
     * configuration file.
     */
    public Server(Resource configuration)
        throws IOException
    {
        this(configuration.getURL());
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param configuration The filename or URL of the XML
     * configuration file.
     */
    public Server(URL configuration)
        throws IOException
    {
        _configuration=configuration.toString();
        try
        {
            XmlConfiguration config=new XmlConfiguration(configuration);
            config.configure(this);
        }
        catch(IOException e)
        {
            throw e;
        }
        catch(Exception e)
        {
            Code.warning(e);
            throw new IOException("Jetty configuration problem: "+e);
        }
    }
    
    /* ------------------------------------------------------------ */
    /**  Configure the server from an XML file.
     * @param configuration The filename or URL of the XML
     * configuration file.
     */
    public void configure(String configuration)
        throws IOException
    {
        if (_configuration!=null)
            throw new IllegalStateException("Already configured with "+_configuration);
        URL url=Resource.newResource(configuration).getURL();
        _configuration=url.toString();
        try
        {
            XmlConfiguration config=new XmlConfiguration(url);
            config.configure(this);
        }
        catch(IOException e)
        {
            throw e;
        }
        catch(Exception e)
        {
            Code.warning(e);
            throw new IOException("Jetty configuration problem: "+e);
        }
    }
    
    /* ------------------------------------------------------------ */
    public String getConfiguration()
    {
        return _configuration;
    }
    
    /* ------------------------------------------------------------ */
    /** Create a new ServletHttpContext.
     * Ths method is called by HttpServer to creat new contexts.  Thus
     * calls to addContext or getContext that result in a new Context
     * being created will return an
     * org.mortbay.jetty.servlet.ServletHttpContext instance.
     * @param contextPathSpec 
     * @return ServletHttpContext
     */
    protected HttpContext newHttpContext(String contextPathSpec)
    {
        return new ServletHttpContext(this,contextPathSpec);
    }


    /* ------------------------------------------------------------ */
    /**  Add Web Applications.
     * Add auto webapplications to the server.  The name of the
     * webapp directory or war is used as the context name. If a
     * webapp is called "root" it is added at "/".
     * @param host Virtual host name or null
     * @param webapps Directory file name or URL to look for auto webapplication.
     * @param defaults The defaults xml filename or URL which is
     * loaded before any in the web app. Must respect the web.dtd.
     * Normally this is passed the file $JETTY_HOME/etc/webdefault.xml
     * @param extractWar If true, WAR files are extracted to a
     * temporary directory.
     * @exception IOException 
     */
    public void addWebApplications(String host,
                                   String webapps,
                                   String defaults,
                                   boolean extractWar)
        throws IOException
    {
        Resource r=Resource.newResource(webapps);
        if (!r.exists())
            throw new IllegalArgumentException("No such webapps resource "+r);
        
        if (!r.isDirectory())
            throw new IllegalArgumentException("Not directory webapps resource "+r);
        
        String[] files=r.list();
        for (int f=0;files!=null && f<files.length;f++)
        {
            String app = r.addPath(files[f]).toString();
            String context=files[f];
            
            if (context.equalsIgnoreCase("CVS/") ||
                context.equalsIgnoreCase("CVS") ||
                context.startsWith("."))
                continue;
            
            if (context.toLowerCase().endsWith(".war") ||
                context.toLowerCase().endsWith(".jar"))
                context=context.substring(0,context.length()-4);
            if (context.equalsIgnoreCase("root")||
                context.equalsIgnoreCase("root/"))
            {
                Log.event("ROOT Context: "+app);
                context="/";
            }
            else
                context="/"+context;

            addWebApplication(host,
                              context,
                              app,
                              defaults,
                              extractWar);                  
        }
        
    }
    
    /* ------------------------------------------------------------ */
    /** Add Web Application.
     * @param contextPathSpec The context path spec. Which must be of
     * the form / or /path/*
     * @param webApp The Web application directory or WAR file.
     * @param defaults The defaults xml filename or URL which is
     * loaded before any in the web app. Must respect the web.dtd.
     * Normally this is passed the file $JETTY_HOME/etc/webdefault.xml
     * @return The WebApplicationContext
     * @exception IOException 
     */
    public WebApplicationContext addWebApplication(String contextPathSpec,
                                                   String webApp,
                                                   String defaults)
        throws IOException
    {
        return addWebApplication(null,
                                 contextPathSpec,
                                 webApp,
                                 defaults,
                                 false);
    }
    
    /* ------------------------------------------------------------ */
    /** Add Web Application.
     * @param contextPathSpec The context path spec. Which must be of
     * the form / or /path/*
     * @param webApp The Web application directory or WAR as file or URL.
     * @param defaults The defaults xml filename or URL which is
     * loaded before any in the web app. Must respect the web.dtd.
     * Normally this is passed the file $JETTY_HOME/etc/webdefault.xml
     * @param extractWar If true, WAR files are extracted to a
     * temporary directory.
     * @return The WebApplicationContext
     * @exception IOException 
     */
    public WebApplicationContext addWebApplication(String contextPathSpec,
                                                   String webApp,
                                                   String defaults,
                                                   boolean extractWar)
        throws IOException
    {
        return addWebApplication(null,
                                 contextPathSpec,
                                 webApp,
                                 defaults,
                                 extractWar);
    }
    
    /* ------------------------------------------------------------ */
    /**  Add Web Application.
     * @param host Virtual host name or null
     * @param contextPathSpec The context path spec. Which must be of
     * the form / or /path/*
     * @param webApp The Web application directory or WAR file.
     * @param defaults The defaults xml filename or URL which is
     * loaded before any in the web app. Must respect the web.dtd.
     * Normally this is passed the file $JETTY_HOME/etc/webdefault.xml
     * @param extractWar If true, WAR files are extracted to a
     * temporary directory.
     * @return The WebApplicationContext
     * @exception IOException 
     */
    public WebApplicationContext addWebApplication(String host,
                                                   String contextPathSpec,
                                                   String webApp,
                                                   String defaults,
                                                   boolean extractWar)
        throws IOException
    {
        WebApplicationContext appContext =
            new WebApplicationContext(this,
                                      contextPathSpec,
                                      webApp,
                                      defaults,
                                      extractWar);
        addContext(host,appContext);
        Log.event("Web Application "+appContext+" added");
        return appContext;
    }
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public static void main(String[] arg)
    {
        String[] dftConfig={"etc/jetty.xml"};
        
        if (arg.length==0)
        {
            System.err.println("Using default configuration: etc/jetty.xml");
            arg=dftConfig;
        }

        final Server[] servers=new Server[arg.length];

        // create and start the servers.
        for (int i=0;i<arg.length;i++)
        {
            try
            {
                servers[i] = new Server(arg[i]);
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
    }
}




