// ========================================================================
// Copyright (c) 1998 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Jetty;

import com.mortbay.Base.*;
import com.mortbay.Util.*;
import com.mortbay.Servlets.*;
import com.mortbay.HTML.*;
import com.mortbay.HTTP.*;
import com.mortbay.HTTP.Handler.*;
import com.mortbay.HTTP.Filter.*;
import com.mortbay.HTTP.Configure.*;
import java.io.*;
import java.net.*;
import javax.servlet.*;
import java.util.*;
import java.lang.reflect.*;



/* ------------------------------------------------------------ */
/** A configuration file based Jetty HttpServer.
 * This HttpConfiguration class uses PropertyTree based 
 * files to configure and run 1 or more Jetty HttpServer
 * instances.
 * 
 * The loadConfigurationFile and buildServers static methods
 * construct 1 or more instances of Server that can be started
 * with the startAll method.
 *
 * <p><h4>Notes</h4>
 * An IP address of 0.0.0.0 can be used to indicate all local
 * addresses.
 *
 * @see com.mortbay.Util.PropertyTree
 * @see com.mortbay.HTTP.HttpServer
 * @see com.mortbay.HTTP.PathMap
 * @version %I% (%E% %U%)
 * @author Greg Wilkins (wilkinsg)
 */
public class Server extends BaseConfiguration
{
    /* ------------------------------------------------------------ */
    private static final Class[] propertyArg =
    {java.util.Properties.class};

    /* ------------------------------------------------------------ */
    private static final Hashtable __serverMap = new Hashtable(7);
    private static Properties __globalProperties=null;
    
    /* ------------------------------------------------------------ */
    private String serverName=null;
    private HttpServer httpServer=null;
    
    /* ------------------------------------------------------------ */
    /** Get a JVM wide server property.
     */
    public static String getGlobalProperty(String name)
    {
        return __globalProperties.getProperty(name);
    }

    /* ------------------------------------------------------------ */
    /** Load server configuration file.
     * @param filename The configuration file (see buildServers for
     *                 format).
     * @exception Exception Problem loading file or configuring
     *            servers.
     */
    public static void loadConfigurationFile(String filename)
        throws Exception
    {
        // load property file
        PropertyTree props = new PropertyTree();
        props.load(new BufferedInputStream(new FileInputStream(filename)));
        
        buildServers(props);
    }

    /* ------------------------------------------------------------ */
    /** Configure 1 or more Server instances.
     * Build server instances from the contents of the
     * passed property tree. The contents of the
     * property tree must be structured as follows: <PRE>
     * SERVERS                  : servername1;servername2
     * PROPERTY.GlobalProperty  : GlobalValue
     * PROPERTIES               : FileOfGlobalProperties.prp
     * servername1.*            : *
     * servername2.*            : *
     * </PRE>
     * For each server listed in SERVERS, the buildServer
     * method is called with the PropertyTree built from
     * all properties starting with "servername.".
     *
     * The "DefaultPageType" parameter can be set in the
     * Global paramters to define the default Page class for
     * all servers.
     *
     * @param serversTree The server properties
     */
    public static void buildServers(PropertyTree serversTree)
        throws Exception
    {
        synchronized(com.mortbay.Jetty.Server.class)
        {
            String serverName="*";
        
            try
            {
                // Handle general server properties
                __globalProperties=properties(serversTree);
                String pageType=
                    __globalProperties.getProperty("DefaultPageType");
                if (pageType!=null && pageType.length()>0)
                    Page.setDefaultPageType(pageType);
            
                Vector servers =
                    serversTree.getVector("SERVERS",";,");
                Code.assert(servers != null,
                            "Missing mandatory configuration entry: 'SERVERS'");
                for (int s=0;s<servers.size();s++)
                {
                    serverName=(String)servers.elementAt(s);
                    PropertyTree serverTree =
                        serversTree.getTree(serverName);
                    buildServer(serverName,serverTree);
                }
            }
            catch (Error e)
            {
                Code.warning("Configuration error for "+serverName);
                throw e;
            }
            catch (Exception e)
            {
                Code.warning("Configuration error for "+serverName);
                throw e;
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    /** Build and configure a Server from a PropertyTree.
     * A new server is constructed and 1 or more handler stacks are
     * added to it from the configuration described in the
     * PropertyTree, such as: <PRE>
     * CLASS                   : com.mortbay.HTTP.HttpServer
     * STACKS                  : stackname1;stackname2;...
     * EXCEPTIONS              : exstackname1;exstackname2;...
     * PROPERTY.ServerProperty : ServerValue
     * PROPERTIES              : FileOfServerProperties.
     * LISTENER.name.CLASS     : com.mortbay.HTTP.HttpListener
     * LISTENER.name.ADDRS     : 0.0.0.0:8080
     * stackname1.*            : *
     * stackname2.*            : *
     * exstackname1.*          : *
     * exstackname2.*          : *
     * </PRE>
     * The server is constructed by passing it the server
     * properties and the listener PropertyTree.  Then for each
     * listed handler stack, the addHandlerStack method is called
     * called with the PropertyTree built from all properties starting
     * with "stackname."
     *
     * Note that if this method is called from buildServers, then all
     * properties in the original file will be prefixed with "servername.".
     * @param serverName The name of the server.
     * @param serverTree configuration property tree.
     * @exception Exception 
     */
    public static void buildServer(String serverName,
                                   PropertyTree serverTree)
        throws Exception
    {
        synchronized(com.mortbay.Jetty.Server.class)
        {
            String stackName="*";
            try
            {
                PropertyTree serverProperties=properties(serverTree);
                PropertyTree listeners = serverTree.getTree("LISTENER");
                Server server = new Server(serverName,
                                           listeners,
                                           serverProperties);
                
                Code.debug("Configure Server",serverName,
                           " ",serverProperties);
                
                Vector stacks = serverTree.getVector("STACKS",";,");
                Code.assert(stacks != null,
                            "Missing mandatory configuration entry: '"+
                            serverName+".STACKS'");
                for (int k=0; k<stacks.size();k++)
                {
                    stackName=(String)stacks.elementAt(k);
                    PropertyTree stackTree = serverTree.getTree(stackName);
                    server.addHandlerStack(stackName,stackTree);
                }

                stacks = serverTree.getVector("EXCEPTIONS",";,");
                if (stacks==null)
                    Code.debug("Missing optional configuration entry: '"+
                               serverName+".EXCEPTIONS'");
                for (int k=0; stacks!=null && k<stacks.size();k++)
                {
                    stackName=(String)stacks.elementAt(k);
                    PropertyTree stackTree = serverTree.getTree(stackName);
                    server.addExceptionStack(stackName,stackTree);
                }
            }
            catch (Error e)
            {
                Code.warning("Configuration error for "+serverName+
                             "."+stackName);
                throw e;
            }
            catch (Exception e)
            {
                Code.warning("Configuration error for "+serverName+
                             "."+stackName);
                throw e;
            }
        }
    }    

    /* ------------------------------------------------------------ */
    /** Get all configured servers.
     * @return Enumeration of Server instances.
     */
    public static Enumeration servers()
    {
        return __serverMap.elements();
    }
    
    /* ------------------------------------------------------------ */
    /** Start all configured servers.
     */
    public static void startAll()
        throws Exception
    {
        Enumeration e = __serverMap.elements();
        while (e.hasMoreElements())
            ((Server)e.nextElement()).start();
    }
    
    /* ------------------------------------------------------------ */
    /** Stop all configured servers.
     */
    public static void stopAll()
    {
        Enumeration e = __serverMap.elements();
        while (e.hasMoreElements())
            ((Server)e.nextElement()).stop(); 

        //  P.Mclachlan <pdm@acm.org> (Sat Jun 19, 1999)
        //    Restore the static variables to their initial
        //    state, ready if we are run again.
        __serverMap.clear();
        //  end P.Mclachlan
    }
    
    /* ------------------------------------------------------------ */
    /** shutdown
     * @deprecated Use stopAll.
     */
    public static void shutdown()
    {
        Enumeration e = __serverMap.elements();
        while (e.hasMoreElements())
            ((Server)e.nextElement()).stop(); 

        //  P.Mclachlan <pdm@acm.org> (Sat Jun 19, 1999)
        //    Restore the static variables to their initial
        //    state, ready if we are run again.
        __serverMap.clear();
        //  end P.Mclachlan
    }
    

    /* ------------------------------------------------------------ */
    /** Constructor.
     * Construct a server from a property tree of listeners and
     * a properti instance of other parameters.  The server will be
     * constructed without any handlers and addHandlerStack will need
     * to be called before starting the server.
     *
     * The listeners PropertyTree should have the following structure:<PRE>
     * name1.CLASS   : com.mortbay.HTTP.HttpListener
     * name1.ADDRS   : ipaddress:port;ipaddress:port;...
     * name2.CLASS   : com.mortbay.HTTP.HttpListener
     * name2.ADDRS   : ipaddress:port;ipaddress:port;...
     * </PRE>
     *
     * Currently the following properties are defined for the
     * server property instance:<PRE>
     * SessionMaxInactiveInterval : Max idle time MS before session death
     * MinListenerThreads         : Min listener threads per listener
     * MaxListenerThreads         : Max listener threads per listener
     * MaxListenerThreadIdleMs    : Max idle time Ms before listen thread
     *                              death
     * MimeMap                    : Property file of MIME mappings
     *
     * </PRE>
     * Note that if this method is called from buildServer, then all
     * properties in the original file will be prefixed with
     * "servername.stackname.".
     *
     * @param serverName The servers name
     * @param listeners Listener PropertyTree
     * @param properties Server properties
     * @exception Exception 
     */
    public Server(String serverName,
                  PropertyTree listeners,
                  Properties properties)
        throws Exception
    {
        synchronized(com.mortbay.Jetty.Server.class)
        {
            this.serverName=serverName;
            __serverMap.put(serverName,this);

            if (Code.debug())
            {
                Code.debug(serverName," listeners=",
                           DataClass.toString(listeners));
                Code.debug(serverName," properties=",
                           DataClass.toString(properties));
            }
            
            // Set properties for HttpConfiguration
            this.properties=properties;

            // Empty path map for handlers
            httpHandlersMap=new PathMap();
        
            // Extract listeners
            Vector listener_classes = new Vector();
            Vector listener_addresses = new Vector();
            Enumeration names = listeners.getRealNodes();
            while (names.hasMoreElements())
            {
                String listenerName = names.nextElement().toString();
                if ("*".equals(listenerName))
                    continue;

                Code.debug("Configuring listener "+listenerName);
                PropertyTree listenerTree = listeners.getTree(listenerName);
            
                String className = listenerTree.getProperty("CLASS");
                Code.assert(className != null,
                            "Missing mandatory configuration entry: '"+
                            serverName+".LISTENER."+listenerName+".CLASS'");
                Class listenerClass = Class.forName(className);

                Vector addrs = listenerTree.getVector("ADDRS",",; ");
                Code.assert(addrs != null,
                            "Missing mandatory configuration entry: '"+
                            serverName+".LISTENER."+listenerName+".ADDRS'");
                for (int a=addrs.size();a-->0;)
                {
                    InetAddrPort addr_port =
                        new InetAddrPort(addrs.elementAt(a).toString());
                    listener_classes.addElement(listenerClass);
                    listener_addresses.addElement(addr_port);
                }
            }
            addresses = new InetAddrPort[listener_addresses.size()];
            listener_addresses.copyInto(addresses);
            listenerClasses = new Class[listener_classes.size()];
            listener_classes.copyInto(listenerClasses);

            // Get mime map
            String mimeFile = properties.getProperty("MimeMap");
            if (mimeFile!=null && mimeFile.length()>0)
            {
                Properties mimeProps = new Properties();
                mimeProps.load(new FileInputStream(mimeFile));
                mimeMap=mimeProps;
            }
        }
    }

    /* ------------------------------------------------------------ */
    /** The server name
     * @return The server name.
     */
    public String getServerName()
    {
        return serverName;
    }
    
    /* ------------------------------------------------------------ */
    /** Add a handler stack to the server.
     * A stack of handlers is contructed and configured from
     * a PropertyTree with a structure like:<PRE>
     * PATHS                   : pathSpec;pathSpec;...
     * HANDLERS                : handlername1;handlername2;...
     * handlername1.CLASS      : package.handler1class
     * handlername1.PROPERTY.* : *
     * handlername1.PROPERTIES : handler1PropertyFile.prp
     * handlername2.*          : *
     * ...
     * </PRE>
     * The stack of handlers is constructed in the order specified
     * by the HANDLERS parameter and registered at each of the
     * PathMap path specifications listed in PATHS.
     * Each handler is constructed with a constructor taking a
     * properties instance which is initialized from the
     * PROPERTY and/or PROPERTIES paramaters.
     *
     * Note that if this method is called from buildServer, then all
     * properties in the original file will be prefixed with
     * "servername.stackname.".
     * @param stackName The name of the handler stack
     * @param stackTree PopertyTree describing the stack
     * @exception Exception 
     */
    public synchronized void addHandlerStack(String stackName,
                                             PropertyTree stackTree)
        throws Exception
    {
        Code.debug("Configure Stack ",serverName,
                   ".",stackName,
                   " ",stackTree);
        
        if (stackTree.get("EXCEPTIONS")!=null)
            Code.warning("2.2.0 Style exception configuration in '"+
                         serverName+"."+stackName+
                         "' is not supported");
        
        Vector handlers = stackTree.getVector("HANDLERS",";,");
        Code.assert(handlers != null,
                    "Missing mandatory configuration entry: '"+
                    serverName+"."+stackName+".HANDLERS'");
        
        HttpHandler[] stack= new HttpHandler[handlers.size()];
        
        Vector paths = stackTree.getVector("PATHS",",;");
        Code.assert(paths != null,
                    "Missing mandatory configuration entry: '"+
                    serverName+"."+stackName+".PATHS'");

        for (int d=paths.size();d-->0;)
            httpHandlersMap.put(paths.elementAt(d),stack);
        
        for (int h=0; h<handlers.size();h++)
        {
            String handlerName=(String)handlers.elementAt(h);
            PropertyTree handlerTree = stackTree.getTree(handlerName);
            PropertyTree handlerProperties=properties(handlerTree);
            
            Code.debug("Configure Handler ",serverName,
                       ".",stackName,
                       ".",handlerName,
                       " ",handlerProperties);

            
            String className = handlerTree.getProperty("CLASS");
            Code.assert(className != null,
                        "Missing mandatory configuration entry: '"+
                        serverName+"."+stackName+"."+handlerName+".CLASS'");
            Class handlerClass = Class.forName(className);
            if (!com.mortbay.HTTP.HttpHandler.class.isAssignableFrom(handlerClass))
                Code.fail(handlerClass+" is not a com.mortbay.HTTP.HttpHandler");
            
            HttpHandler handlerInstance=null;
            try
            {
                Constructor handlerConstructor =
                    handlerClass.getConstructor(propertyArg);
                Object[] arg={handlerProperties};
                handlerInstance = (com.mortbay.HTTP.HttpHandler)
                    handlerConstructor.newInstance(arg);
            }
            catch(NoSuchMethodException nsme)
            {
                handlerInstance = (com.mortbay.HTTP.HttpHandler)
                    handlerClass.newInstance();
                handlerInstance.setProperties(handlerProperties);
            }
            stack[h]=handlerInstance;
        }
    }

    
    /* ------------------------------------------------------------ */
    /** Add an exception handler stack to the server.
     * A stack of exception handlers is contructed and configured from
     * a PropertyTree with a structure like:<PRE>
     * PATHS                   : pathSpec;pathSpec;...
     * HANDLERS                : handlername1;handlername2;...
     * handlername1.CLASS      : package.handler1class
     * handlername1.PROPERTY.* : *
     *      * ...
     * </PRE>
     * The stack of handlers is constructed in the order specified
     * by the HANDLERS parameter and registered at each of the
     * PathMap path specifications listed in PATHS.
     *
     * Note that if this method is called from buildServer, then all
     * properties in the original file will be prefixed with
     * "servername.stackname.".
     * @param stackName The name of the exception handler stack
     * @param stackTree PopertyTree describing the stack
     * @exception Exception 
     */
    public synchronized void addExceptionStack(String stackName,
                                               PropertyTree stackTree)
        throws Exception
    {
        Code.debug("Configure Ex Stack ",serverName,
                   ".",stackName,
                   " ",stackTree);
        
        Vector handlers = stackTree.getVector("HANDLERS",";,");
        Code.assert(handlers != null,
                    "Missing mandatory configuration entry: '"+
                    serverName+"."+stackName+".HANDLERS'");
        ExceptionHandler[] stack
            = new ExceptionHandler[handlers.size()];
        
        Vector paths = stackTree.getVector("PATHS",",;");
        Code.assert(paths != null,
                    "Missing mandatory configuration entry: '"+
                    serverName+"."+stackName+".PATHS'");

        for (int d=paths.size();d-->0;)
        {
            if (exceptionHandlersMap==null)
                exceptionHandlersMap=new PathMap();
            exceptionHandlersMap.put(paths.elementAt(d),stack);
        }
        
        for (int h=0; h<handlers.size();h++)
        {
            String handlerName=(String)handlers.elementAt(h);
            PropertyTree handlerTree = stackTree.getTree(handlerName);
            
            Code.debug("Configure Ex Handler ",serverName,
                       ".",stackName,
                       ".",handlerName);

            String className = handlerTree.getProperty("CLASS");
            Code.assert(className != null,
                        "Missing mandatory configuration entry: '"+
                        serverName+"."+stackName+"."+handlerName+".CLASS'");
            Class handlerClass = Class.forName(className);
            if (!com.mortbay.HTTP.ExceptionHandler.class.isAssignableFrom(handlerClass))
                Code.fail(handlerClass+" is not a com.mortbay.HTTP.ExceptionHandler");
            
            ExceptionHandler handlerInstance = (com.mortbay.HTTP.ExceptionHandler)
                handlerClass.newInstance();
            
            stack[h]=handlerInstance;
        }
    }

    /* ------------------------------------------------------------ */
    /** Start serving.
     */
    public void start()
        throws Exception
    {
        // initialize the server
        httpServer = new HttpServer(this);
    }
    
    /* ------------------------------------------------------------ */
    /** Stop serving.
     */
    public void stop()
    {
        if (httpServer != null)
            httpServer.stop();
    }


    /* ------------------------------------------------------------ */
    /** join 
     */
    public void join()
        throws InterruptedException
    {
        httpServer.join();
    }


    /* ------------------------------------------------------------ */
    /** Extract property sub tree.
     * Extract sub tree from file name PROPERTIES key merged with property
     * tree below PROPERTY key.
     * @param props PropertyTree 
     * @return PropertyTree
     */
    static PropertyTree properties(PropertyTree props)
        throws IOException, FileNotFoundException
    {
        PropertyTree properties= props.getTree("PROPERTY");
        if (properties==null)
            properties=new PropertyTree();

        String filename = props.getProperty("PROPERTIES");
        if (filename!=null&&filename.length()>0)
        {
            Code.debug("Load ",filename);
            properties.load(new BufferedInputStream(new FileInputStream(filename)));
        }
        
        return properties;
    }
    
    
    /* ------------------------------------------------------------ */
    /** main
     * @param args optional property file name 
     */
    public static void main(String args[])
    {
        try{
            String filename = "JettyServer.prp";
            if (args.length==1)
                filename = args[0];
            else if ( ! new File(filename).exists())
                filename = "etc/JettyServer.prp";
            loadConfigurationFile(filename);  
        }
        catch(Throwable e)
        {
            Code.warning(e);
            System.err.println("Usage - java com.mortbay.Jetty.Server [config.prp]");
                System.err.println("Default config files are \"JettyServer.prp\", \"etc/JettyServer.prp\".");
                System.exit(1);
        }
        
        try{
            startAll();
        }
        catch(Throwable e)
        {
            Code.warning(e);
        }
    }
};
