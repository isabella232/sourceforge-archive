package com.mortbay.Jetty;

import com.mortbay.HTTP.HttpServer;
import com.mortbay.HTTP.HandlerContext;
import com.mortbay.Util.Code;
import com.mortbay.Util.Log;
import com.mortbay.Util.Resource;
import com.mortbay.XML.XmlConfiguration;
import com.mortbay.Jetty.Servlet.ServletHandlerContext;
import com.mortbay.Jetty.Servlet.WebApplicationContext;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.Runtime;
import java.net.URL;
import org.xml.sax.SAXException;
import java.util.List;
import java.util.Map;


/* ------------------------------------------------------------ */
/** Config file driven HttpServer.
 *
 * This class initializes HttpServer instances from xml config files
 * that follow the XmlConfiguration dtd.
 *
 * @see HttpServer
 * @see XmlConfiguration
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
     * @param configuration 
     */
    public Server(String configuration)
        throws IOException
    {
        this(Resource.newResource(configuration).getURL());
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param configuration 
     */
    public Server(Resource configuration)
        throws IOException
    {
        this(configuration.getURL());
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param configuration 
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
    /** 
     * @param configuration 
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
    /** Create a new ServletHandlerContext
     * @param contextPathSpec 
     * @return 
     */
    protected HandlerContext newHandlerContext(String contextPathSpec)
    {
        return new ServletHandlerContext(this,contextPathSpec);
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
     * @param webApp The Web application directory or WAR file.
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
        Thread hook =
            new Thread() {
                    public void run() {
                        Log.event("Shutdown hook executing");
                        for (int i=0;i<servers.length;i++)
                        {
                            try{servers[i].stop();}
                            catch(Exception e){Code.warning(e);}
                        }
                    }
                };

        try
        {
            Method shutdownhook=
                java.lang.Runtime.class.getMethod("addShutdownHook",new
                    Class[] {java.lang.Thread.class});
            shutdownhook.invoke(Runtime.getRuntime(),
                                new Object[]{hook});
        }
        catch(Exception e)
        {
            Code.debug("No shutdown hook",e);
        }
    }
}




