package com.mortbay.Jetty;

import com.mortbay.HTTP.HttpServer;
import com.mortbay.Util.Code;
import com.mortbay.Util.Resource;
import com.mortbay.Util.XmlConfiguration;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import org.xml.sax.SAXException;


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
        
        for (int i=0;i<arg.length;i++)
        {
            try
            {
                new Server(arg[i]).start();
            }
            catch(Exception e)
            {
                Code.warning(e);
            }
        }
    }
}




