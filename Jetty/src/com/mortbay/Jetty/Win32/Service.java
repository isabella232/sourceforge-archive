// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
//
// Derived from SCMEventManager.java by Bill Giel/KC Multimedia and Design Group, Inc.,
// ========================================================================

package com.mortbay.Jetty.Win32;
import com.mortbay.HTTP.HttpServer;
import com.mortbay.HTTP.HttpServer;
import com.mortbay.Util.Code;
import com.mortbay.Util.Log;
import com.mortbay.Util.WriterLogSink;
import com.mortbay.Util.FileLogSink;
import com.mortbay.Util.Resource;
import com.mortbay.Util.XmlConfiguration;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Vector;
import org.xml.sax.SAXException;

/* ------------------------------------------------------------ */
/** Run Jetty as a Win32 service.
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
public class Service 
{
    /* ------------------------------------------------------------ */
    static String serviceLogFile=
    System.getProperty("SERVICE_LOG_FILE","logs"+File.separator+"service.log");
    
    /* ------------------------------------------------------------ */
    public static final int SERVICE_CONTROL_STOP = 1;
    public static final int SERVICE_CONTROL_PAUSE = 2;
    public static final int SERVICE_CONTROL_CONTINUE = 3;
    public static final int SERVICE_CONTROL_INTERROGATE = 4;
    public static final int SERVICE_CONTROL_SHUTDOWN = 5;
    public static final int SERVICE_CONTROL_PARAMCHANGE = 6;

    /* ------------------------------------------------------------ */
    private static Vector _servers;
    private static Vector _configs;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    private Service()
    {}
    
    /* ------------------------------------------------------------ */
    public static void dispatchSCMEvent(int eventID)
    {
        switch(eventID)
        {
          case SERVICE_CONTROL_STOP:
          case SERVICE_CONTROL_PAUSE:
              stopAll();
              break;
              
          case SERVICE_CONTROL_CONTINUE :
              startAll();
              break;
              
          case SERVICE_CONTROL_SHUTDOWN:
              destroyAll();
              break;
              
          case SERVICE_CONTROL_PARAMCHANGE:
              stopAll();
              destroyAll();
              createAll();
              startAll();
              break;

          default:
              break;
        }
    }
    
    /* ------------------------------------------------------------ */
    private static void createAll()
    {
        if (_configs!=null)
        {
            synchronized(_configs)
            {
                _servers=new Vector();
                for(int i=0;i<_configs.size();i++)
                {
                    try
                    {
                        HttpServer server = new HttpServer();
                        Resource config = Resource.newResource((String)_configs.get(i));
                        XmlConfiguration xml=new XmlConfiguration(config.getURL());
                        xml.configure(server);
                        _servers.add(server);
                    }
                    catch(Exception e)
                    {
                        Code.warning(_configs.get(i)+" configuration problem: ",e);
                    }
                }
            }
        }
    }
    
    
    /* ------------------------------------------------------------ */
    private static void startAll()
    { 
        if (_configs!=null)
        {
            synchronized(_configs)
            {
                for(int i=0;i<_servers.size();i++)
                {
                    HttpServer server = (HttpServer)_servers.get(i);
                    if (!server.isStarted())
                        server.start();
                }
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    private static void stopAll()
    { 
        if (_configs!=null)
        {
            synchronized(_configs)
            {
                for(int i=0;i<_servers.size();i++)
                {
                    HttpServer server = (HttpServer)_servers.get(i);
                    try{server.stop();}catch(InterruptedException e){}
                }
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    private static void destroyAll()
    {
        if (_configs!=null)
        {
            synchronized(_configs)
            {
                for(int i=0;i<_servers.size();i++)
                {
                    HttpServer server = (HttpServer)_servers.get(i);
                    server.destroy();
                }
                _servers.clear();
                _servers=null;
            }
        }
    }

    /* ------------------------------------------------------------ */
    public static void main(String[] arg)
    {
        try
        {
	    String logOptions = System.getProperty("LOG_OPTIONS","tLTs");
            String dateFormat = System.getProperty("LOG_DATE_FORMAT",
                                                   "yyyyMMddHHmmssSSSzzz ");
            String timezone = System.getProperty("LOG_TIMEZONE","GMT");

            if (Code.debug())
                Log.instance().add(new WriterLogSink());
                             
	    FileLogSink sink= new FileLogSink(serviceLogFile);
	    sink.setOptions(dateFormat,timezone,
                            (logOptions.indexOf(Log.TIMESTAMP) >= 0),
                            (logOptions.indexOf(Log.LABEL) >= 0),
                            (logOptions.indexOf(Log.TAG) >= 0),
                            (logOptions.indexOf(Log.STACKSIZE) >= 0),
                            (logOptions.indexOf(Log.STACKTRACE) >= 0),
                            (logOptions.indexOf(Log.ONELINE) >= 0));
            Log.instance().add(sink);
        }
        catch(Exception e)
        {
            Code.warning(e);
        }
        
        if (arg.length==0)
            arg=new String[] {"etc/jetty.xml"};
        
        try
        {
            _configs=new Vector();
            for (int i=0;i<arg.length;i++)
                _configs.add(arg[i]);
            createAll();
            startAll();
        }
        catch(Exception e)
        {
            Code.warning(e);
        }
    }
}

