// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;
//import com.sun.java.util.collections.*; XXX-JDK1.1

import com.mortbay.Util.*;
import java.io.*;
import java.net.*;
import java.util.*;
import com.mortbay.HTTP.Handler.*;
import com.mortbay.HTTP.Handler.Servlet.*;
import java.lang.reflect.*;

/* ------------------------------------------------------------ */
/** HTTP Server.
 * Services HTTP requests by maintaining a mapping between
 * a collection of HttpListeners which generate requests and
 * HttpContexts which contain collections of HttpHandlers.
 *
 * @see HttpContext
 * @see HttpHandler
 * @see HttpConnection
 * @see HttpListener
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class HttpServer implements LifeCycle
{
    
    /* ------------------------------------------------------------ */
    HashMap _listeners = new HashMap(7);
    
    // HttpServer[host->PathMap[contextPath->List[HanderContext]]]
    // HandlerContext[List[HttpHandler]]
    HashMap _hostMap = new HashMap(7);
    private HttpEncoding _httpEncoding ;

    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public HttpServer()
    {}


    /* ------------------------------------------------------------ */
    /** 
     * @return 
     */
    public HttpEncoding getHttpEncoding()
    {
	if (_httpEncoding==null)
	    _httpEncoding=new HttpEncoding();
	return _httpEncoding;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param httpEncoding 
     */
    public void setHttpEncoding(HttpEncoding httpEncoding)
    {
	_httpEncoding = httpEncoding;
    }    

    
    /* ------------------------------------------------------------ */
    public void initialize(Object config)
    {
	Code.notImplemented();
    }
    
    
    /* ------------------------------------------------------------ */
    /** Start all handlers then listeners.
     */
    public synchronized void start()
    {
	if (Code.verbose(99))
	{
	    Code.debug("LISTENERS: ",_listeners);
	    Code.debug("HANDLER: ",_hostMap);
	}
	
	
        Iterator handlers = getHandlers().iterator();
        while(handlers.hasNext())
        {
            HttpHandler handler=(HttpHandler)handlers.next();
            if (!handler.isStarted())
                handler.start();
        }
        
        Iterator listeners = getListeners().iterator();
        while(listeners.hasNext())
        {
            HttpListener listener =(HttpListener)listeners.next();
	    listener.setHttpServer(this);
            if (!listener.isStarted())
                listener.start();
        }
    }
    
    /* ------------------------------------------------------------ */
    /** Start all handlers then listeners.
     */
    public synchronized boolean isStarted()
    {
        Iterator handlers = getHandlers().iterator();
        while(handlers.hasNext())
        {
            HttpHandler handler=(HttpHandler)handlers.next();
            if (handler.isStarted())
		return true;
        }
        
        Iterator listeners = getListeners().iterator();
        while(listeners.hasNext())
        {
            HttpListener listener =(HttpListener)listeners.next();
            if (listener.isStarted())
		return true;
        }
	return false;
    }
    
    /* ------------------------------------------------------------ */
    /** Stop all listeners then handlers.
     * @exception InterruptedException If interrupted, stop may not have
     * been called on everything.
     */
    public synchronized void stop()
        throws InterruptedException
    {
        Iterator listeners = getListeners().iterator();
        while(listeners.hasNext())
        {
            HttpListener listener =(HttpListener)listeners.next();
            if (listener.isStarted())
                listener.stop();
        }
        
        Iterator handlers = getHandlers().iterator();
        while(handlers.hasNext())
        {
            HttpHandler handler=(HttpHandler)handlers.next();
            if (handler.isStarted())
                handler.stop();
        }
    }

    
    /* ------------------------------------------------------------ */
    /** Stop all listeners then handlers.
     * All the handlers are unmapped and the listeners removed.
     */
    public synchronized void destroy()
    {
        Iterator listeners = getListeners().iterator();
        while(listeners.hasNext())
        {
            HttpListener listener =(HttpListener)listeners.next();
            listener.destroy();
        }
        
        Iterator handlers = getHandlers().iterator();
        while(handlers.hasNext())
        {
            HttpHandler handler=(HttpHandler)handlers.next();
            handler.destroy();
        }

        _hostMap.clear();
        _listeners.clear();
    }

    /* ------------------------------------------------------------ */
    public synchronized boolean isDestroyed()
    {
        Iterator handlers = getHandlers().iterator();
        while(handlers.hasNext())
        {
            HttpHandler handler=(HttpHandler)handlers.next();
            if (!handler.isDestroyed())
		return false;
        }
        
        Iterator listeners = getListeners().iterator();
        while(listeners.hasNext())
        {
            HttpListener listener =(HttpListener)listeners.next();
            if (!listener.isDestroyed())
		return false;
        }
	return true;
    }
    
    /* ------------------------------------------------------------ */
    /** Create and add a SocketListener.
     * Conveniance method.
     * @param address
     * @return the HttpListener.
     * @exception IOException 
     */
    public HttpListener addListener(InetAddrPort address)
        throws IOException
    {
        HttpListener listener = (HttpListener)_listeners.get(address);
        if (listener==null)
        {
            listener=new SocketListener(address);
	    listener.setHttpServer(this);
            _listeners.put(address,listener);
        }

        return listener;
    }
    
    /* ------------------------------------------------------------ */
    /** Add a HTTP Listener to the server.
     * @param listener The Listener.
     * @exception IllegalArgumentException If the listener is not for this server. 
     */
    public void addListener(HttpListener listener)
        throws IllegalArgumentException
    {
	listener.setHttpServer(this);        
        _listeners.put(listener,listener);
    }
    
    /* ------------------------------------------------------------ */
    /** Remove a HTTP Listener
     * @param listener 
     */
    public void removeListener(HttpListener listener)
    {
        Iterator iterator = _listeners.entrySet().iterator();
        while(iterator.hasNext())
        {
            Map.Entry entry=
                (Map.Entry) iterator.next();
            if (entry.getValue()==listener)
                iterator.remove();
        }
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return Set of all listeners.
     */
    public Collection getListeners()
    {
        return _listeners.values();
    }

    
    /* ------------------------------------------------------------ */
    /** Define a virtual host alias.
     * All requests to the alias are handled the same as request for
     * the host.
     * @param host 
     * @param alias 
     */
    public void addHostAlias(String host, String alias)
    {
        Object contextMap=_hostMap.get(host);
	if (contextMap==null)
	    throw new IllegalArgumentException("No Such Host: "+host);
        _hostMap.put(alias,contextMap);
    }


    /* ------------------------------------------------------------ */
    /** Create and add a new context.
     * Note that multiple contexts can be created for the same
     * host and contextPathSpec. Requests are offered to multiple
     * contexts in the order they where added to the HttpServer.
     * @param contextPathSpec
     * @return 
     */
    public HandlerContext addContext(String contextPathSpec)
    {
	return addContext(null,contextPathSpec);
    }
    
    /* ------------------------------------------------------------ */
    /** Create and add a new context.
     * Note that multiple contexts can be created for the same
     * host and contextPathSpec. Requests are offered to multiple
     * contexts in the order they where added to the HttpServer.
     * @param host Virtual hostname or null for all hosts.
     * @param contextPathSpec
     * @return 
     */
    public HandlerContext addContext(String host, String contextPathSpec)
    {
	HandlerContext hc = new HandlerContext(this);
	addContext(host,contextPathSpec,hc);
	return hc;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param host The virtual host or null for all hosts.
     * @param contextPathSpec 
     * @param i Index among contexts of same host and pathSpec.
     * @return The HandlerContext of null.
     */
    public HandlerContext getContext(String host, String contextPathSpec, int i)
    {
	HandlerContext hc=null;

	PathMap contextMap=(PathMap)_hostMap.get(host);
	if (contextMap!=null)
	{
	    List contextList = (List)contextMap.get(contextPathSpec);
	    if (contextList!=null)
	    {
		if (i>=contextList.size())
		    return null;
		hc=(HandlerContext)contextList.get(i);
	    }
	}

	return hc;
    }

    
    /* ------------------------------------------------------------ */
    /** 
     * @param host The virtual host or null for all hosts.
     * @param contextPathSpec 
     * @return HandlerContext. If multiple contexts exist for the same
     * host and pathSpec, the most recently added context is returned.
     * If no context exists, a new context is defined.
     */
    public HandlerContext getContext(String host, String contextPathSpec)
    {
	HandlerContext hc=null;

	PathMap contextMap=(PathMap)_hostMap.get(host);
	if (contextMap!=null)
	{
	    List contextList = (List)contextMap.get(contextPathSpec);
	    if (contextList!=null && contextList.size()>0)
		hc=(HandlerContext)contextList.get(contextList.size()-1);
	    
	}
	if (hc==null)
	    hc=addContext(host,contextPathSpec);

	return hc;
    }
    
    /* ------------------------------------------------------------ */
    /** Add a context.
     * As contexts cannot be publicly created, this may be used to
     * alias an existing context.
     * @param host The virtual host or null for all hosts.
     * @param contextPathSpec 
     * @return 
     */
    public void addContext(String host,
			   String contextPathSpec,
			   HandlerContext context)
    {
	PathMap contextMap=(PathMap)_hostMap.get(host);
	if (contextMap==null)
	{
	    contextMap=new PathMap(7);
	    _hostMap.put(host,contextMap);
	}

	List contextList = (List)contextMap.get(contextPathSpec);
	if (contextList==null)
	{
	    contextList=new ArrayList(1);
	    contextMap.put(contextPathSpec,contextList);
	}

	contextList.add(context);
    }

    
    /* ------------------------------------------------------------ */
    /** 
     * @param contextPathSpec 
     * @param directory 
     * @exception IOException 
     */
    public WebApplicationContext addWebApplication(String contextPathSpec,
						   String directory)
	throws IOException
    {
	return addWebApplication(null,contextPathSpec,directory);
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param host 
     * @param contextPathSpec 
     * @param directory 
     * @exception IOException 
     */
    public WebApplicationContext addWebApplication(String host,
						   String contextPathSpec,
						   String directory)
	throws IOException
    {
	WebApplicationContext appContext =
	    new WebApplicationContext(this,directory);
	addContext(host,contextPathSpec,appContext);
	Log.event("Web Application "+appContext+" added at "+
		  (host!=null?(host+":"+contextPathSpec):contextPathSpec));
	return appContext;
    }
    
    
    
    /* ------------------------------------------------------------ */
    /** Add a handler to a path specification.
     * Requests with paths matching the path specification are passed
     * to the handle method of the handler. All matching handlers
     * are offered the request, starting with the best match, until
     * the request is handled.
     *
     * Multiple handlers can be mapped to the same contextPath and
     * requests are passed to the handlers in the order they
     * were registered.
     *
     * @param host Virtual host name or null.
     * @param contextPath 
     * @param handler 
     */
    public void addHandler(String host,String contextPathSpec, HttpHandler handler)
    {
	HandlerContext hc = getContext(host,contextPathSpec);
	hc.addHandler(handler);
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return Collection of all handler.
     */
    public Set getHandlers()
    {
        HashSet set = new HashSet(33);
        Iterator maps=_hostMap.values().iterator();
        while (maps.hasNext())
        {
            PathMap pm=(PathMap)maps.next();
            Iterator lists=pm.values().iterator();
            while(lists.hasNext())
            {
                List list=(List)lists.next();
                Iterator contexts=list.iterator();
                while(contexts.hasNext())
		{
		    HandlerContext context = (HandlerContext) contexts.next();
                    set.addAll(context.getHandlers());
		}
            }
        }
        return set;
    }
    

    /* ------------------------------------------------------------ */
    /** Service a request.
     * Handle the request by passing it to the HttpHandler contained in
     * the mapped HandlerContexts.
     * The requests host and path are used to select a list of
     * HandlerContexts. Each HttpHandler in these context is offered
     * the request in turn, until the request is handled.
     *
     * If no handler handles the request, 404 Not Found is returned.
     *
     * @param request 
     * @param response 
     * @exception IOException 
     * @exception HttpException 
     */
    public void service(HttpRequest request,HttpResponse response)
        throws IOException, HttpException
    {
	String host=request.getHost();

	while (true)
	{
	    PathMap contextMap=(PathMap)_hostMap.get(host);
	    if (contextMap!=null)
	    {
		List contextLists =contextMap.getMatches(request.getPath());
		if(contextLists!=null)
		{
		    if (Code.verbose(99))
			Code.debug("Contexts at ",request.getPath(),
				   ": ",contextLists);
		    
		    for (int i=0;i<contextLists.size();i++)
		    {
			Map.Entry entry=
			    (Map.Entry)
			    contextLists.get(i);
			String contextPathSpec=(String)entry.getKey();
			List contextList = (List)entry.getValue();
                
			for (int j=0;j<contextList.size();j++)
			{
			    HandlerContext context=
				(HandlerContext)contextList.get(j);
			    
			    if (Code.debug())
				Code.debug("Try context [",contextPathSpec,
					   ",",new Integer(j),
					   "]=",context);

			    List handlers=context.getHandlers();
			    for (int k=0;k<handlers.size();k++)
			    {
				HttpHandler handler =
				    (HttpHandler)handlers.get(k);
	    
				if (Code.debug())
				    Code.debug("Try handler ",handler);
				
				handler.handle(contextPathSpec,
					       request,
					       response);

				if (request.isHandled())
				{
				    if (Code.debug())
					Code.debug("Handled by ",handler);
				    response.complete();
				    return;
				}
			    }
			}
		    }   
		}
	    }
	    
	    // try no host
            if (request.isHandled() || host==null)
		break;
	    host=null;
	}
	

	response.sendError(response.__404_Not_Found);
	
    }
    
    /* ------------------------------------------------------------ */
    /** Construct server from command line arguments.
     * @param args 
     */
    public static void main(String[] args)
    {
        if (args.length==0)
        {
            String[] newArgs=
	    {
		"-appContext","/=./webapps/default",
		"-context","/",
		"-classPath","./servlets",
		"-dynamic",
		"-resourceBase","./FileBase/",
		"-resources",
		"8080",
	    };
            args=newArgs;
        }
        else if (args.length==1 && args[0].startsWith("-"))
        {
            System.err.println
                ("\nUsage - java com.mortbay.HTTP.HttpServer [ options .. ] [[<addr>:]<port> .. ]");
            System.err.println
                ("\n  [<addr>:]<port>               - Listen on [ address & ] port");
            System.err.println
                ("  -appContext [<host>:]<pathSpec>=<directory>  ");
            System.err.println
                ("                                - Define web application directory");
            System.err.println
                ("  -context [<host>:]<pathSpec>  - Define new context. Default=\"/\"");
            System.err.println
                ("  -alias <alias>                - Add Alias for current context host");
	    
            System.err.println
                ("  -classPath <paths>            - Set contexts classpath.");	
            System.err.println
                ("  -resourceBase <dir>           - Set contexts File Base");    

            System.err.println
                ("  -handler <class>              - Add a hander to the context");
            
            System.err.println
                ("  -servlet <pathSpec>=<class>   - Add Servlet at path to context");
            System.err.println
                ("  -dynamic                      - Context serves dynamic servlets");
            System.err.println
                ("  -resources                    - Context serves resources as files");
            System.err.println
                ("  -dump                         - Add a Dump handler to context");
            
            System.err.println
                ("\nDefault options:");
            System.err.println
                ("  -appContext /=./webapps/default\n"+
		 "  -content /\n"+
		 "  -classPath ./servlets\n"+
		 "  -dynamic\n"+
		 "  -resourceBase ./FileBase/\n"+
		 "  -resources\n"+
		 "  8080");
            System.exit(1);
        }
        
        try{
            // Create the server
            HttpServer server = new HttpServer();

            // Default is no virtual host
	    String host=null;
	    HandlerContext context = server.getContext(host,"/");	    
	    
            // Parse arguments
            for (int i=0;i<args.length;i++)
            {
                try
                {
                    // Look for dump handler
                    if ("-appContext".equals(args[i]))
		    {
			String appHost=null;
			String spec=args[++i];
			String dir=null;
                        int c=spec.indexOf(":");
                        int e=spec.indexOf("="); 

			
                        if (c>0 && c<e)
                        {
			    appHost=spec.substring(0,c);
			    dir=spec.substring(e+1);
                            spec=spec.substring(c+1,e);
                        }
			else if (e>0)
			{
			    dir=spec.substring(e+1);
                            spec=spec.substring(0,e);
			}
			server.addWebApplication(appHost,spec,dir);
		    }
                    // Look for context
                    else if ("-context".equals(args[i]))
		    {
                        String spec=args[++i];
			host=null;
                        int e=spec.indexOf(":");
                        if (e>0)
                        {
			    host=spec.substring(0,e);
                            spec=spec.substring(e+1);
                        }
			context = server.getContext(host,spec);
		    }
		    else if ("-alias".equals(args[i]))
		    {
			server.addHostAlias(host,args[++i]);
		    }
		    else if ("-classPath".equals(args[i]))
                    {
			context.setClassPath(args[++i]);
		    }
		    else if ("-resourceBase".equals(args[i]))
                    {
			context.setResourceBase(args[++i]);
		    }
		    else if ("-handler".equals(args[i]))
		    {
                        String className=args[++i];
			HttpHandler handler = (HttpHandler)
			    Class.forName(className).newInstance();
			context.addHandler(handler);
		    }
		    else if ("-servlet".equals(args[i]))
                    {
                        String spec=args[++i];
                        int e=spec.indexOf("=");
                        if (e>0)
                        {
                            String pathSpec=spec.substring(0,e);
                            String className=spec.substring(e+1);
			    context.addServlet(pathSpec,className);
                        }
                    }
		    else if ("-dynamic".equals(args[i]))
		    {
			context.setServingDynamicServlets(true);
		    }
		    else if ("-resources".equals(args[i]))
                    {
			context.setServingResources(true);
		    }
		    else if ("-dump".equals(args[i]))
		    {
                        String className="com.mortbay.HTTP.Handler.DumpHandler";
			HttpHandler handler = (HttpHandler)
			    Class.forName(className).newInstance();
			context.addHandler(handler);
		    }
                    else
                    {
                        // Add listener.
                        InetAddrPort address = new InetAddrPort(args[i]);
                        server.addListener(address);
                    }
                }
                catch (Exception e)
                {
                    Code.warning(e);
                }
            }
	    server.start();
        }
        catch (Exception e)
        {
            Code.warning(e);
        }
    }
}













