// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;

import com.mortbay.Util.*;
import com.sun.java.util.collections.*;
import java.io.*;
import java.net.*;
import java.util.*;
import com.mortbay.HTTP.Handler.*;
import com.mortbay.HTTP.Handler.Servlet.*;
import java.lang.reflect.*;

/* ------------------------------------------------------------ */
/** HTTP Server.
 *
 * @see
 * @version 1.0 Thu Oct  7 1999
 * @author Greg Wilkins (gregw)
 */
public class HttpServer implements LifeCycle
{
    /* ------------------------------------------------------------ */
    HashMap _listeners = new HashMap(7);
    HashMap _hostMap = new HashMap(7);
    PathMap _handlerMap = new PathMap();

    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public HttpServer()
    {
        _hostMap.put(null,_handlerMap);
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
        _handlerMap.clear();
        _listeners.clear();
        _hostMap.put(null,_handlerMap);
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
    /** Add a SocketListener.
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
            listener=new SocketListener(this,address);
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
        if (listener.getServer()!=this)
            throw new IllegalArgumentException("Listener is not for this server");
        
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
            com.sun.java.util.collections.Map$Entry entry=
                (com.sun.java.util.collections.Map$Entry) iterator.next();
            if (entry.getValue()==listener)
                iterator.remove();
        }
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return Set of all listeners.
     */
    public Set getListeners()
    {
        return new HashSet(_listeners.values());
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
        PathMap handlerMap=getHandlerMap(host,true);
        _hostMap.put(alias,handlerMap);
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
     * @param host Virtual host name or null.
     * @param contextPath 
     * @param handler 
     */
    public void addHandler(String host,String contextPath, HttpHandler handler)
    {
        List list=(List)getHandlerMap(host,true).get(contextPath);
        if (list==null)
        {
            list=new ArrayList(8);
            getHandlerMap(host,false).put(contextPath,list);
        }
        if (!list.contains(handler))
            list.add(handler);
    }
    
    /* ------------------------------------------------------------ */
    /** Unmap a handler from a path specification.
     * @param host Virtual host name or null.
     * @param contextPath 
     * @param handler 
     */
    public void removeHandler(String host,
			      String contextPath,
			      HttpHandler handler)
    {
        List list=(List)getHandlerMap(host,false).get(contextPath);
        if (list!=null)
            list.remove(handler);
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return Collection of all listeners.
     */
    public Set getHandlers()
    {
        HashSet set = new HashSet(33);
        Iterator maps=_hostMap.values().iterator();
        while (maps.hasNext())
        {
            PathMap pm=(PathMap)maps.next();
            Iterator handlerStacks=pm.values().iterator();
            while(handlerStacks.hasNext())
            {
                List stack=(List)handlerStacks.next();
                Iterator handlers=stack.iterator();
                while(handlers.hasNext())
                    set.add(handlers.next());
            }
        }
        return set;
    }
    
    /* ------------------------------------------------------------ */
    String _contextHost;
    public String getContextHost() {return _contextHost;}    
    public void setContextHost(String  v)
    {clearDefaultContext();this._contextHost = v;}
    
    /* ------------------------------------------------------------ */
    String _contextPath="/";
    public String getContextPath() {return _contextPath;}
    public void setContextPath(String  v)
    {clearDefaultContext();this._contextPath = v;}

    /* ------------------------------------------------------------ */
    FileHandler _contextFileHandler;
    public FileHandler getContextFileHandler()
    {return _contextFileHandler;}
    public void setContextFileHandler(FileHandler  v)
    {this._contextFileHandler = v;}
    
    /* ------------------------------------------------------------ */
    ServletHandler _contextServletHandler;
    public ServletHandler getContextServletHandler()
    {return _contextServletHandler;}
    public void setContextServletHandler(ServletHandler  v)
    {this._contextServletHandler = v;}

    /* ------------------------------------------------------------ */
    ServletHandler _contextDynamicServletHandler;
    public ServletHandler getContextDynamicServletHandler()
    {return _contextDynamicServletHandler;}
    public void setContextDynamicServletHandler(ServletHandler  v)
    {this._contextServletHandler = v;}
    
    /* ------------------------------------------------------------ */
    private void clearDefaultContext()
    {
	_contextFileHandler=null;
	_contextServletHandler=null;
	_contextDynamicServletHandler=null;
    }

    /* ------------------------------------------------------------ */
    /**
     * Creates a FileHandler for the context if one does not exist.
     * If a ServletHandler exists for the context, it's FileBase is
     * also set.
     * @param directory 
     */
    public synchronized void setContextFileBase(String directory)
    {
	if (_contextFileHandler==null)
	{
	    _contextFileHandler = new FileHandler();
	    addHandler(_contextHost,_contextPath,_contextFileHandler);
	}
	_contextFileHandler.setFileBase(directory);
	if (_contextServletHandler!=null)
	    _contextServletHandler.setFileBase(directory);
    }

    
  /* ------------------------------------------------------------ */
    public synchronized ServletHolder addContextServlet(String pathSpec,
							String className)
	throws ClassNotFoundException,
	       InstantiationException,
	       IllegalAccessException
    {
	if (_contextServletHandler==null)
	{
	    _contextServletHandler=new ServletHandler();
	    addHandler(_contextHost,_contextPath,_contextServletHandler);
	}
	return _contextServletHandler.addServlet(pathSpec,className);
    }

    
    /* ------------------------------------------------------------ */
    public synchronized void setContextDynamicServletClassPath(String classPath)
	throws ClassNotFoundException,
	       InstantiationException,
	       IllegalAccessException
    {
	if (_contextDynamicServletHandler==null)
	{
	    _contextDynamicServletHandler=
		new DynamicHandler();
	    addHandler(_contextHost,_contextPath,
		       _contextDynamicServletHandler);
	}
	_contextDynamicServletHandler.setClassPath(classPath);
    }

    
    
    /* ------------------------------------------------------------ */
    /** Service a request.
     * Handle the request by passing it to mapped HttpHandlers.
     * Requests with paths matching a handlers mapped path specification
     * are passed to the handle method of the handler. All matching handlers
     * are offered the request, starting with the best match, until
     * the request is handled.
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
        // find all matching handlers.
        List matches = (List)getHandlerMap(request.getHost(),false)
            .getMatches(request.getPath());

        // Try handlers, starting from best match.
        if (matches==null)
            response.sendError(response.__404_Not_Found);
        else
        {
            Iterator i1=matches.iterator();
            while (!request.isHandled() && i1.hasNext())
            {
                com.sun.java.util.collections.Map$Entry entry=
                    (com.sun.java.util.collections.Map$Entry)i1.next();
                String contextPath=(String)entry.getKey();
                List handlers = (List)entry.getValue();
                
                Iterator i2=handlers.iterator();
                while (!request.isHandled() && i2.hasNext())
                {
                    HttpHandler handler = (HttpHandler)i2.next();
                    if (Code.verbose(9))
                    {
                        Code.debug("Try handler ",handler);
                        handler.handle(contextPath,request,response);
                        if (request.isHandled())
                            Code.debug("Handled by ",handler);
                    }
                    else
                        handler.handle(contextPath,request,response);
                }
            }

            if (!request.isHandled())
                response.sendError(response.__404_Not_Found);
        }
    }


    /* ------------------------------------------------------------ */
    /** Enable a transfer encoding.
     * Enable a transfer encoding on a ChunkableInputStream.
     * @param in 
     * @param coding Coding name 
     * @param parameters Coding parameters or null
     * @exception HttpException 
     */
    public static void enableEncoding(ChunkableInputStream in,
				      String coding,
				      Map parameters)
        throws HttpException
    {
        try
        {
            if ("gzip".equals(coding))
            {
                if (parameters!=null && parameters.size()>0)
                    throw new HttpException(HttpResponse.__501_Not_Implemented,
                                            "gzip parameters");
                in.insertFilter(java.util.zip.GZIPInputStream.class
                                .getConstructor(ChunkableInputStream.__filterArg),
                                null);
            }
            else if ("deflate".equals(coding))
            {
                if (parameters!=null && parameters.size()>0)
                    throw new HttpException(HttpResponse.__501_Not_Implemented,
                                            "deflate parameters");
                in.insertFilter(java.util.zip.InflaterInputStream.class
                                .getConstructor(ChunkableInputStream.__filterArg),
                                null);
            }
            else if (!HttpFields.__Identity.equals(coding))
                throw new HttpException(HttpResponse.__501_Not_Implemented);
        }
        catch (HttpException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            Code.warning(e);
            throw new HttpException(HttpResponse.__500_Internal_Server_Error);
        }
    }
    
    /* ------------------------------------------------------------ */
    /** Enable a transfer encoding.
     * Enable a transfer encoding on a ChunkableOutputStream.
     * @param out
     * @param coding Coding name 
     * @param parameters Coding parameters or null
     * @exception HttpException 
     */
    public static void enableEncoding(ChunkableOutputStream out,
				      String coding,
				      Map parameters)
        throws HttpException
    {
        try
        {
            if ("gzip".equals(coding))
            {
                if (parameters!=null && parameters.size()>0)
                    throw new HttpException(HttpResponse.__501_Not_Implemented,
                                            "gzip parameters");
                out.insertFilter(java.util.zip.GZIPOutputStream.class
                                 .getConstructor(ChunkableOutputStream.__filterArg),
                                 null);
            }
            else if ("deflate".equals(coding))
            {
                if (parameters!=null && parameters.size()>0)
                    throw new HttpException(HttpResponse.__501_Not_Implemented,
                                            "deflate parameters");
                out.insertFilter(java.util.zip.DeflaterOutputStream.class
                                 .getConstructor(ChunkableOutputStream.__filterArg),
                                null);
            }
            else if (!HttpFields.__Identity.equals(coding))
                throw new HttpException(HttpResponse.__501_Not_Implemented);
        }
        catch (HttpException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            Code.warning(e);
            throw new HttpException(HttpResponse.__500_Internal_Server_Error);
        }
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
		"-contextPath","/",
		"-fileBase",".",
		"-dump",
		"-handler","com.mortbay.HTTP.Handler.NotFoundHandler",
		"-contextPath","/servlet/*",
		"-dynamic","./servlets",
		"8080",
	    };
            args=newArgs;
        }
        else if (args.length%2==1)
        {
            System.err.println
                ("Usage - java com.mortbay.HTTP.HttpServer [ options .. ] [[<addr>:]<port> ..]");
            System.err.println
                ("[<addr>:]<port>       - Listen on [ address & ] port");
            System.err.println
                (" -contextPath <path>  - Set default context path. Default=\"/\"");
            System.err.println
                (" -contextHost <name>  - Set default context host name");
	    
            System.err.println
                (" -contextAlias <alias>- Add Alias for context host");

            System.err.println
                (" -handler <class>     - Add a hander to the context");
            
            System.err.println
                (" -fileBase <dir>      - Set contexts File Base");
            System.err.println
                (" -dynamic <classPath> - Set contexts Dynamic servlets class path");
            
            System.err.println
                (" -servlet <pathSpec>=<class>");
            System.err.println
                ("                      - Add Servlet at path to context");
            
            System.err.println
                (" -dump                - Add a Dump handler to context");
            
            System.err.println
                ("Default options:");
            System.err.println
                ("  -contextPath /\n"+
		 "   -fileBase .\n"+
		 "   -dump\n"+
		 "  -handler com.mortbay.HTTP.Handler.NotFoundHandler\n"+
		 "  -contextPath /servlet/*\n"+
		 "  -dynamic ./servlets\n"+
		 "  8080");
            System.exit(1);
        }
        
        try{
            // Create the server
            HttpServer server = new HttpServer();

            // Default is no virtual host
            String host=null;
            
            // Parse arguments
            for (int i=0;i<args.length;i++)
            {
                try
                {
                    // Look for dump handler
                    if ("-contextPath".equals(args[i]))
		    {
                        server.setContextPath(args[++i]);
		    }
		    else if ("-contextHost".equals(args[i]))
		    {
                        server.setContextHost(args[++i]);
		    }
		    else if ("-contextAlias".equals(args[i]))
		    {
			server.addHostAlias(server.getContextHost(),
					    args[++i]);
		    }
		    else if ("-handler".equals(args[i]))
		    {
                        String className=args[++i];
			HttpHandler handler = (HttpHandler)
			    Class.forName(className).newInstance();
			server.addHandler(server.getContextHost(),
					  server.getContextPath(),
					  handler);
		    }
		    else if ("-fileBase".equals(args[i]))
                    {
			server.setContextFileBase(args[++i]);
		    }
		    else if ("-dynamic".equals(args[i]))
		    {
			server.setContextDynamicServletClassPath(args[++i]);
		    }
		    else if ("-servlet".equals(args[i]))
                    {
                        String spec=args[++i];
                        int e=spec.indexOf("=");
                        if (e>0)
                        {
                            String pathSpec=spec.substring(0,e);
                            String className=spec.substring(e+1);
			    server.addContextServlet(pathSpec,className);
                        }
                    }
		    else if ("-dump".equals(args[i]))
		    {
                        String className="com.mortbay.HTTP.Handler.DumpHandler";
			HttpHandler handler = (HttpHandler)
			    Class.forName(className).newInstance();
			server.addHandler(server.getContextHost(),
					  server.getContextPath(),
					  handler);
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
    
    /* ------------------------------------------------------------ */
    private PathMap getHandlerMap(String host, boolean create)
    {
        PathMap virtual;
        if (host!=null && host.length()>0)
        {
            virtual=(PathMap)_hostMap.get(host);
            if (virtual==null && create)
            {
                virtual=new PathMap();
                _hostMap.put(host,virtual);
            }
            
            if (virtual==null)
                 virtual=_handlerMap;
        }
        else
            virtual=_handlerMap;        
        return virtual;
    }
    
}













