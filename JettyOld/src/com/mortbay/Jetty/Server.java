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
/** 
 * 
 * <p><h4>Notes</h4>
 * An IP address of 0.0.0.0 can be used to indicate all local
 * addresses.
 *
 * @version %I% (%E% %U%)
 * @author Greg Wilkins (wilkinsg)
 */
public class Server extends BaseConfiguration
{
    /* ------------------------------------------------------------ */
    private static final Class[] propertyArg = {java.util.Properties.class};

    /* ------------------------------------------------------------ */
    private static final Hashtable serverMap = new Hashtable(7);
	
    /* ------------------------------------------------------------ */
    private HttpServer httpServer=null;
    PropertyTree properties;
    
    /* ------------------------------------------------------------ */
    /** Construct from properties.
     * Properties are assumed to be in the format of a PropertyTree
     * like:<PRE>
     * DefaultSessionTimeout  : 3600
     * LISTENER.name.CLASS    : package.class
     * LISTENER.name.ADDRS    : 0.0.0.0:8080;128.0.0.1:8888
     *</PRE>
     * 
     * @exception IOException 
     */
    public Server(Properties properties )
	throws IOException
    {
	PropertyTree tree=null;
	if (properties instanceof PropertyTree)
	    tree = (PropertyTree)properties;
	else
	    tree = new PropertyTree(properties);
	Code.debug(tree);

	String defaultSessionTimeout=tree.getProperty("DefaultSessionTimeout");

	Enumeration names = tree.getTree("LISTENER").getNodes();
	while (names.hasMoreElements())
	{
	    String listenerName = names.nextElement().toString();
	    Code.debug("Configuring listener "+listenerName);
	    PropertyTree listenerTree = tree.getTree("LISTENER."+listenerName);
	    String className = listenerTree.getProperty("CLASS");
	    
	    Vector addrs = listenerTree.getVector("ADDRS",",; ");
	    addresses = new InetAddrPort[addrs.size()];
	    for (int a=addrs.size();a-->0;)
	    {
		addresses[a]=
		    new InetAddrPort(addrs.elementAt(a).toString());
	    }
	}

	httpHandlersMap=new PathMap();
    }

    /* ------------------------------------------------------------ */
    /** Start serving 
     */
    public void start()
	throws Exception
    {
	// initialize the server
	httpServer = new HttpServer(this);
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
    /** Build a hashtable of Server configurations from a
     * server properties instance
     * @param props The server properties
     * @return Map of server name to server instances.
     */
    public static void buildServers(PropertyTree props)
	throws Exception
    {
	// create server map
	String serverName="*";
	String stackName="*";
	String handlerName="*";

	try
	{
	    Vector servers = props.getVector("SERVERS",";,");
	    for (int s=0;s<servers.size();s++)
	    {
		serverName=(String)servers.elementAt(s);
		
		PropertyTree serverTree = props.getTree(serverName);
		PropertyTree serverProperties=properties(serverTree);
		Server server = new Server(serverProperties);
		serverMap.put(serverName,server);
		
		Code.debug("Configure ",serverName,
			   " ",serverProperties);
		
		Vector stacks = serverTree.getVector("STACKS",";,");
		for (int k=0; k<stacks.size();k++)
		{
		    stackName=(String)stacks.elementAt(k);
		    PropertyTree stackTree = serverTree.getTree(stackName);
		    PropertyTree stackProperties=properties(stackTree);
		    Code.debug("Configure ",serverName,
			       ".",stackName,
			       " ",stackProperties);
	    
		    Vector handlers = stackTree.getVector("HANDLERS",";,");
		    HttpHandler[] stack= new HttpHandler[handlers.size()];
		    
		    Vector paths = stackTree.getVector("PATHS",",;");
		    for (int d=paths.size();d-->0;)
			server.httpHandlersMap.put(paths.elementAt(d),stack);
		    
		    for (int h=0; h<handlers.size();h++)
		    {
			handlerName=(String)handlers.elementAt(h);
			PropertyTree handlerTree = stackTree.getTree(handlerName);
			PropertyTree handlerProperties=properties(handlerTree);
			Code.debug("Configure ",serverName,
				   ".",stackName,
				   ".",handlerName,
				   " ",handlerProperties);
			
			Class handlerClass=Class.forName(handlerTree.getProperty("CLASS"));
			if (!com.mortbay.HTTP.HttpHandler.class.isAssignableFrom(handlerClass))
			    Code.fail(handlerClass+" is not a com.mortbay.HTTP.HttpHandler");

			HttpHandler handlerInstance=null;
			try
			{
			    Constructor handlerContructor =
				handlerClass.getConstructor(propertyArg);
			    Object[] arg={handlerProperties};
			    handlerInstance = (com.mortbay.HTTP.HttpHandler)
				handlerContructor.newInstance(arg);
			}
			catch(NoSuchMethodException nsme)
			{
			    handlerInstance = (com.mortbay.HTTP.HttpHandler)
				handlerClass.newInstance();
			    handlerInstance.setProperties(handlerProperties);
			}
			stack[h]=handlerInstance;
		    }
		    handlerName="*";
		}
		stackName="*";
	    }
	    serverName="*";
	}
	catch (Error e)
	{
	    Code.warning("Configuration error for "+
			 serverName+"."+stackName+"."+handlerName);
	    throw e;
	}
	catch (Exception e)
	{
	    Code.warning("Configuration error for "+
			 serverName+"."+stackName+"."+handlerName);
	    throw e;
	}
	
    }

    /* ------------------------------------------------------------ */
    /** Exract property sub tree.
     * Extract sub tree from file name PROPERTIES key merged with property
     * tree below PROPERTY key.
     * @param props PropertyTree 
     * @return PropertyTree
     */
    static PropertyTree properties(PropertyTree props)
	throws IOException, FileNotFoundException
    {
	PropertyTree properties=new PropertyTree();
	String filename = props.getProperty("PROPERTIES");
	if (filename!=null&&filename.length()>0)
	{
	    Code.debug("Load ",filename.trim());
	    properties.load(new BufferedInputStream(new FileInputStream(filename)));
	}
	PropertyTree property= props.getTree("PROPERTY");
	if (property!=null)
	    properties.load(property);
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

	    // load property file
	    PropertyTree props = new PropertyTree();
	    props.load(new BufferedInputStream(new FileInputStream(filename)));
	    
	    buildServers(props);
	
 	    // Start all severs
   	    Enumeration e = serverMap.elements();
   	    while (e.hasMoreElements())
   		((Server)e.nextElement()).start();
	    
 	    // Join all severs
 	    e = serverMap.elements();
 	    while (e.hasMoreElements())
 		((Server)e.nextElement()).join();   
	}
	catch(Throwable e){
	    Code.warning(e);
	}
    }
};
