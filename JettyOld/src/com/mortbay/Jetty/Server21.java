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

/* ------------------------------------------------------------ */
/** Basic WWW server configuration.
 * A simple configuration of Jetty for servlet and file serving,
 * which may be driven programatically of from a properties file.
 * <p>
 *
 * <p><h4>Notes</h4>
 * An IP address of 0.0.0.0 can be used to indicate all local
 * addresses.
 *
 * <p><h4>Usage</h4>
 * The main of this class may be passed an optional configuration
 * file name (default is "JettyServer.prp"). The format of the
 * configuration file allows for multiple named servers to be
 * included as follows
 * <pre>
 * ServerName./pathOfStack.InetAddrPort	         n.n.n.n:pppp
 * ServerName./pathOfStack.Servlet./path         name=package.class[?paramFile]
 * ServerName./pathOfStack.Directory./path       directory/name
 * ServerName./pathOfStack.Directory.allowPut    true|false
 * ServerName./pathOfStack.Directory.allowDelete true|false
 * ServerName./pathOfStack.Directory./path       directory/name
 * ServerName./pathOfStack.Log./path             filename|err|out
 * </pre>
 *
 * @see JettyServer.prp
 * @version 1.0 Sat Sep 26 1998
 * @author Greg Wilkins (gregw)
 */
public class Server extends BaseConfiguration
{
    /* ------------------------------------------------------------ */
    private String serverName=null;
    private HttpServer httpServer=null;
    private Hashtable stackMap=new Hashtable();
    private Hashtable servletHolders=new Hashtable();
    
    private class HandlerStack
    {
	String path=null;
	PathMap servletMap= new PathMap();
	PathMap dirMap= new PathMap();
	boolean dirAllowPut = false;
	boolean dirAllowDelete = false;
	PathMap logMap = new PathMap();
	PathMap authMap = new PathMap();
	PathMap transMap = new PathMap();
	PathMap filterMap = new PathMap();
	PathMap forwardMap = new PathMap();
	boolean proxy = false;
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param port 
     * @exception IOException 
     */
    public Server(String serverName)
	 throws IOException
    {
	this.serverName=serverName;
	
	// No address yet
	addresses=new InetAddrPort[0];
	
    }

    /* ------------------------------------------------------------ */
    private HandlerStack getStack(String stack)
    {
	if (stack==null)
	    stack="";
	HandlerStack s = (HandlerStack)stackMap.get(stack);
	if (s==null)
	{
	    Code.debug("New stack: ",stack);
	    s=new HandlerStack();
	    s.path=stack;
	    stackMap.put(stack,s);
	}
	return s;
    }
    
    /* ------------------------------------------------------------ */
    /** Add an address to listen at.
     * @param addr 
     */
    public void addAddress(InetAddrPort addr)
    {
	Code.debug(serverName,".addAddress: ",addr);
	
	// Expand array
	InetAddrPort[] new_addresses =
	    new InetAddrPort[addresses.length+1];

	// copy old addresses
	System.arraycopy(addresses,0,new_addresses,0,addresses.length);
	
	// add new address
	new_addresses[addresses.length]=addr;

	// set new addresses
	addresses=new_addresses;
    }

    /* ------------------------------------------------------------ */
    /** Add a servlet to the server
     * @param path The path of the servlet (see PathMap for encoding)
     * @param name The name of the servlet
     * @param holder A servlet holder
     * @see com.mortbay.HTTP.PathMap
     */
    public void addServlet(String stack,
			   String path,
			   ServletHolder holder)
    {
	Code.debug(serverName,".addServlet: ",holder," at ",path);
	getStack(stack).servletMap.put(path,holder);
    }
    
    /* ------------------------------------------------------------ */
    /** Add a servlet to the server
     * @param path The path of the servlet (see PathMap for encoding)
     * @param name The name of the servlet
     * @param servlet The class name of the servlet
     * @param paramFile Servlet init param file name May be null
     * @see com.mortbay.HTTP.PathMap
     */
    public void addServlet(String stack,
			   String path,
			   String name,
			   String servlet,
			   String paramFile)
	throws IOException, ServletException
    {
	Code.debug(serverName,".addServlet: ",servlet," at ",path);
		   
	ServletHolder holder = (ServletHolder)servletHolders.get(name);
	if (holder==null)
	{
	    Properties params = new Properties();
	    if (paramFile!=null)
		params.load(new BufferedInputStream(
				new FileInputStream(paramFile)));
		holder=new ServletHolder(name,servlet,params);
	    
	    servletHolders.put(name,holder);
	}

	addServlet(stack,path,holder);
    }
    
    /* ------------------------------------------------------------ */
    /** Add a file directory to the server
     * @param path The path of the url to map
     * @param directory The name of the directory with the files to serve
     * @see com.mortbay.HTTP.PathMap
     */
    public void addDirectory(String stack, String path, String directory)
    {
	Code.debug(serverName,".addDirectory: ",directory," at ",path);
	getStack(stack).dirMap.put(path,directory);
    }

    /* ------------------------------------------------------------ */
    /** Set the allowPut option on the FileHandler for the server
     * @param value The new value for the option
     * @see com.mortbay.HTTP.Handler.FileHandler
     */
    public void allowPut(String stack, Boolean value){
	Code.debug(serverName,".allowPut:", value);
	getStack(stack).dirAllowPut = value.booleanValue();
    }

    /* ------------------------------------------------------------ */
    /** Set the allowDelete option on the FileHandler for the server
     * @param value The new value for the option
     * @see com.mortbay.HTTP.Handler.FileHandler
     */
    public void allowDelete(String stack, Boolean value){
	Code.debug(serverName,".allowDelete:", value);
	getStack(stack).dirAllowDelete = value.booleanValue();
    }
    
    /* ------------------------------------------------------------ */
    /** Add a log sink to the server
     * @param path The path of the servlet (see PathMap for encoding)
     * @param log A file name or "err" or "out"
     * @see com.mortbay.HTTP.PathMap
     */
    public void addLog(String stack, String path, String log)
	throws IOException
    {
	Code.debug(serverName,".addLog: ",log," at ",path);
	OutputStreamWriter out = null;
	if ("out".equals(log))
	    out=new OutputStreamWriter(System.out);
	else if ("err".equals(log))
	    out=new OutputStreamWriter(System.err);
	else
	    out=new OutputStreamWriter(new FileOutputStream(log));
	
	getStack(stack).logMap.put(path,out);
    }

    /* ------------------------------------------------------------ */
    /** Add a authentication realm to the server
     * @param path The path of the realm (see PathMap for encoding)
     * @param name The name of the realm
     * @param filename The realm file
     * @see com.mortbay.HTTP.PathMap
     */
    public void addAuthRealm(String stack,
			     String path,
			     String name,
			     String filename)
	throws IOException
    {
	Code.debug(serverName,".addAuthRealm: ",name," at ",path);
	getStack(stack).authMap.put(path,new BasicAuthRealm(name,filename));
    }
    
    /* ------------------------------------------------------------ */
    /** Add a URL translation
     * @param stack The path of the handler stack to add to.
     *              A null stack is the same as "" or all paths.
     * @param path The path of the translation (see PathMap for encoding)
     * @param translation The new path
     * @see com.mortbay.HTTP.PathMap
     */
    public void addTranslation(String stack,
			       String path,
			       String translation)
    {    
	Code.debug(serverName,".addTranslation: ",translation," at ",path);
	getStack(stack).transMap.put(path,translation);
    }
    
    /* ------------------------------------------------------------ */
    /** Add a Forwarder
     * @param stack The path of the handler stack to add to.
     *              A null stack is the same as "" or all paths.
     * @param path The path of the forward (see PathMap for encoding)
     * @param url The url to forward to
     * @see com.mortbay.HTTP.PathMap
     */
    public void addForward(String stack,
			   String path,
			   String url)
	throws MalformedURLException
    {    
	Code.debug(serverName,".addForward: ",url," at ",path);
	getStack(stack).forwardMap.put(path,new URL(url));
    }
    
    /* ------------------------------------------------------------ */
    /** Add a Filter
     * @param stack The path of the handler stack to add to.
     *              A null stack is the same as "" or all paths.
     * @param path The path of the filter (see PathMap for encoding)
     * @param filterClass the name of the filter class
     * @see com.mortbay.HTTP.PathMap
     */
    public void addFilter(String stack,
			   String path,
			   String filterClass)
    {    
	Code.debug(serverName,".addFilter: ",filterClass," at ",path);
	getStack(stack).filterMap.put(path,filterClass);
    }

    /* ------------------------------------------------------------ */
    /** Add a Proxy
     * @param stack The path of the handler stack to add to.
     *              A null stack is the same as "" or all paths.
     * @see com.mortbay.HTTP.PathMap
     */
    public void addProxy(String stack)
    {    
	Code.debug(serverName,".addProxy: ");
	getStack(stack).proxy=true;
    }

    /* ------------------------------------------------------------ */
    /** Start serving 
     */
    public void start()
	throws Exception
    {
	// Configure handlers
	httpHandlersMap=new PathMap();

	// for each handler stack
	Enumeration e = stackMap.elements();
	while (e.hasMoreElements())
	{
	    HandlerStack stack = (HandlerStack)e.nextElement();
	    
	    // Create full stack of HttpHandlers at stack
	    HttpHandler[] httpHandlers = new HttpHandler[10];
	    Code.debug("Stack: ",stack.path);
	    httpHandlersMap.put(stack.path,httpHandlers);
	    int h=0;

	    // Log Handler
	    if (stack.logMap.size()>0)
		httpHandlers[h++] = new LogHandler(stack.logMap,true, true);

	    // Auth Handler
	    if (stack.authMap.size()>0)
		httpHandlers[h++] = new BasicAuthHandler(stack.authMap);
	    
	    // Translation Handler
	    if (stack.transMap.size()>0)
		httpHandlers[h++] = new TranslateHandler(stack.transMap);

	    // Filter handler
	    if (stack.filterMap.size()>0)
		httpHandlers[h++] = new FilterHandler(stack.filterMap);
	    
	    // Stuff for servlets
	    if (stack.servletMap.size()>0)
	    {
		// Parameter handler
		httpHandlers[h++] = new ParamHandler();
	    
		// Session handler
		httpHandlers[h++] = new SessionHandler();
	    
		// Servlet Handler
		httpHandlers[h++] = new ServletHandler(stack.servletMap);
	    }
	
	    // File Handler
	    if (stack.dirMap.size()>0){
		FileHandler fh = new FileHandler(stack.dirMap);
		httpHandlers[h++] = fh;
		fh.setPutAllowed(stack.dirAllowPut);
		fh.setDeleteAllowed(stack.dirAllowDelete);
	    }
	    
	    // Forward Handler
	    if (stack.forwardMap.size()>0)
		httpHandlers[h++] = new ForwardHandler(stack.forwardMap);

	    // Proxy Handler
	    if (stack.proxy)
		httpHandlers[h++] = new ProxyHandler();
	    
	    // NotFound Handler
	    httpHandlers[h++] = new NotFoundHandler();
	}

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
    public static Hashtable buildServers(Properties props)
	throws Exception
    {
	// create server map
	Hashtable server_map = new Hashtable(); 
	
	// For all properties
	Enumeration e = props.keys();   
	Vector fields = new Vector();
	while (e.hasMoreElements())
	{
	    String key = (String)e.nextElement();
	    String value = props.getProperty(key);

	    // Extract fields
	    fields.removeAllElements();
	    StringTokenizer tok = new StringTokenizer(key,". 	");
	    while (tok.hasMoreTokens())
		fields.addElement(tok.nextToken());
	    
	    // Extract the server name and handler stack
	    if (fields.size()<3)
		throw new Exception("Badly formatted configuration key: "+key);
	    String name = (String)fields.elementAt(0);
	    String stack = (String)fields.elementAt(1);
	    String type = (String)fields.elementAt(2);
	    String path = null;
	    if (fields.size() > 3)
	    {
		path=(String)fields.elementAt(3);
		for (int i=4;i<fields.size();i++)
		    path+="."+fields.elementAt(i);
	    }
	    
	    // Get a server instance for the name
	    Server server=(Server)server_map.get(name);
	    if (server==null)
	    {
		server = new Server(name);
		server_map.put(name,server);
	    }

	    // handle property
	    if ("InetAddrPort".equals(type))
	    {
		StringTokenizer list = new StringTokenizer(value,", 	");
		while (list.hasMoreTokens())
		    server.addAddress(new InetAddrPort(list.nextToken()));
	    }
	    else if ("Servlet".equals(type))
	    {
		if (path==null)
		    throw new Exception("Missing path: "+key);
		
		int eq = value.indexOf("=");
		if (eq<0 || eq==value.length())
		    throw new Exception("Badly formatted servlet value: "+
					value);
		String sname=value.substring(0,eq);
		String cname=value.substring(eq+1);
		
		int q=cname.indexOf("?");
		if (q>0)
		{
		    String paramFile = cname.substring(q+1);
		    cname=cname.substring(0,q);
		    server.addServlet(stack,path,sname,cname,paramFile);
		}
		else
		    server.addServlet(stack,path,sname,cname,null);
	    }
	    else if ("Directory".equals(type))
	    {
		if (path==null)
		    throw new Exception("Missing path: "+key);
		if (path.equals("allowPut"))
		    server.allowPut(stack, Boolean.valueOf(value));
		else if (path.equals("allowDelete"))
		    server.allowDelete(stack, Boolean.valueOf(value));
		else 
		    server.addDirectory(stack,path,value);
	    }
	    else if ("Log".equals(type))
	    {
		if (path==null)
		    throw new Exception("Missing path: "+key);
		server.addLog(stack,path,value);
	    }
	    else if ("Auth".equals(type))
	    {
		if (path==null)
		    throw new Exception("Missing path: "+key);
		
		int eq = value.indexOf("=");
		if (eq<0 || eq==value.length())
		    throw new Exception("Badly formatted auth value: "+
					value);
		server.addAuthRealm(stack,path,
				    value.substring(0,eq),
				    value.substring(eq+1));
	    }
	    else if ("Translate".equals(type))
	    {
		if (path==null)
		    throw new Exception("Missing path: "+key);
		server.addTranslation(stack,path,value);
	    }
	    else if ("Filter".equals(type))
	    {
		if (path==null)
		    throw new Exception("Missing path: "+key);
		server.addFilter(stack,path,value);
	    }
	    else if ("Forward".equals(type))
	    {
		if (path==null)
		    throw new Exception("Missing path: "+key);
		server.addForward(stack,path,value);
	    }
	    else if ("Proxy".equals(type))
	    {
		server.addProxy(stack);
	    }
	    else
		Code.warning("Unknown property: "+
			     name+"."+
			     key+"="+value);
	}

	return server_map;
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
	    Properties props = new Properties();
	    props.load(new BufferedInputStream(new FileInputStream(filename)));
	    Hashtable server_map = buildServers(props);

	    Code.debug(server_map);
	
	    // Start all severs
	    Enumeration e = server_map.elements();
	    while (e.hasMoreElements())
		((Server)e.nextElement()).start();
	    // Join all severs
	    e = server_map.elements();
	    while (e.hasMoreElements())
		((Server)e.nextElement()).join();   
	}
	catch(Throwable e){
	    Code.warning(e);
	}
    }
};
