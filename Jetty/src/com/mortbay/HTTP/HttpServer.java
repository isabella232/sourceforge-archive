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

/* ------------------------------------------------------------ */
/** HTTP Server.
 *
 *
 * @see
 * @version 1.0 Thu Oct  7 1999
 * @author Greg Wilkins (gregw)
 */
public class HttpServer
{
    /* ------------------------------------------------------------ */
    HashMap _listeners = new HashMap(7);
    HashMap _hostMap = new HashMap(7);
    PathMap _handlerMap = new PathMap();
    HashMap _fileMap = new HashMap(7);
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public HttpServer()
    {
        _hostMap.put(null,_handlerMap);
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
    
    /* ------------------------------------------------------------ */
    /** Define a virtual host alias.
     * All requests to the alias are handled the same as request for
     * the host.
     * @param host 
     * @param alias 
     */
    public void hostAlias(String host, String alias)
    {
        PathMap handlerMap=getHandlerMap(host,true);
        _hostMap.put(alias,handlerMap);
    }
    
    /* ------------------------------------------------------------ */
    /** Map a handler to a path specification.
     * Requests with paths matching the path specification are passed
     * to the handle method of the handler. All matching handlers
     * are offered the request, starting with the best match, until
     * the request is handled.
     *
     * Multiple handlers can be mapped to the same pathSpec and
     * requests are passed to the handlers in the order they
     * were registered.
     * @param host Virtual host name or null.
     * @param pathSpec 
     * @param handler 
     */
    public void mapHandler(String host,String pathSpec, HttpHandler handler)
    {
        List list=(List)getHandlerMap(host,true).get(pathSpec);
        if (list==null)
        {
            list=new ArrayList(8);
            getHandlerMap(host,false).put(pathSpec,list);
        }
        if (!list.contains(handler))
            list.add(handler);
    }
    
    /* ------------------------------------------------------------ */
    /** Unmap a handler from a path specification.
     * @param host Virtual host name or null.
     * @param pathSpec 
     * @param handler 
     */
    public void unmapHandler(String host, String pathSpec, HttpHandler handler)
    {
        List list=(List)getHandlerMap(host,false).get(pathSpec);
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
    /** Add and start a SocketListener.
     * Conveniance method.
     * @param address
     * @return the HttpListener.
     * @exception IOException 
     */
    public HttpListener startListener(InetAddrPort address)
        throws IOException
    {
        HttpListener listener = (HttpListener)_listeners.get(address);
        if (listener==null)
        {
            listener=new SocketListener(this,address);
            _listeners.put(address,listener);
        }

        listener.start();
        return listener;
    }
    
    /* ------------------------------------------------------------ */
    /** Stop a SocketListener.
     * Conveniance method.
     * @param address 
     * @exception InterruptedException 
     */
    public void stopListener(InetAddrPort address)
        throws InterruptedException
    {
        HttpListener listener = (HttpListener)_listeners.get(address);
        if (listener!=null)
            listener.stop();
    }
    
    /* ------------------------------------------------------------ */
    /** Destroy a SocketListener
     * Conveniance method.
     * @param address 
     */
    public void destroyListener(InetAddrPort address)
    {
        HttpListener listener = (HttpListener)_listeners.remove(address);
        if (listener!=null)
            listener.destroy();
    }

    
    /* ------------------------------------------------------------ */
    /** Conveniance method for adding FileHandlers.
     * A single FileHandler instance is maintained for each
     * mapped filename and can be mapped to multiple path specifications.
     * @param host Virtual host name or null.
     * @param pathSpec 
     * @param filename
     * @return The handler.
     */
    public HttpHandler mapFiles(String host, String pathSpec, String filename)
    {
        FileHandler fileHandler = (FileHandler)_fileMap.get(filename);
        if (fileHandler==null)
        {
            fileHandler = new FileHandler(filename,
                                          "index.html",
                                          true, // dir OK
                                          false,// put !OK
                                          false,// delete !OK
                                          64,   // cached files
                                          40960 // cached file size
                                          );
            _fileMap.put(filename,fileHandler);
            fileHandler.start();
        }
        mapHandler(host,pathSpec,fileHandler);
        return fileHandler;
    }

    
    /* ------------------------------------------------------------ */
    /** Start all handlers then listeners.
     */
    public synchronized void startAll()
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
    /** Stop all listeners then handlers.
     * @exception InterruptedException If interrupted, stop may not have
     * been called on everything.
     */
    public synchronized void stopAll()
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
    public synchronized void destroyAll()
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
    /** Service a request.
     * Handle the request by passing it to mapped HttpHandlers.
     * Requests with paths matching a handlers mapped path specification
     * are passed to the handle method of the handler. All matching handlers
     * are offered the request, starting with the best match, until
     * the request is handled.
     *
     * Multiple handlers can be mapped to the same pathSpec and
     * requests are passed to the handlers in the order they
     * were registered.
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
                String pathSpec=(String)entry.getKey();
                List handlers = (List)entry.getValue();
                
                Iterator i2=handlers.iterator();
                while (!request.isHandled() && i2.hasNext())
                {
                    HttpHandler handler = (HttpHandler)i2.next();
                    if (Code.verbose(9))
                    {
                        Code.debug("Try handler ",handler);
                        handler.handle(pathSpec,request,response);
                        if (request.isHandled())
                            Code.debug("Handled by ",handler);
                    }
                    else
                        handler.handle(pathSpec,request,response);
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
    public void enableEncoding(ChunkableInputStream in,
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
    public void enableEncoding(ChunkableOutputStream out,
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
		"-a","8080","-f","/=.","-d","/",
		"-s21","/servlet/*=./servlets",
		"-h","com.mortbay.HTTP.Handler.NotFoundHandler"
	    };
            args=newArgs;
        }
        else if (args.length%2==1)
        {
            System.err.println
                ("Usage - java com.mortbay.HTTP.HttpServer [ -a|f|d|h <value> ... ]");
            System.err.println
                (" -a [<addr>:]<port>  - Listen on [ address & ] port");
            System.err.println
                (" -f <path>=<dir>     - File handler at path Spec to file/directory");
            System.err.println
                (" -d <path>           - Dump handler at path Spec");
            
            System.err.println
                (" -s21 <path>=<path>  - Dynamic servlet2.1 handler at path & path");
            
            System.err.println
                (" -h <path>=<class>   - Map a hander");
            
            System.err.println
                (" -v <host>[=<alias>] - Remaining options for virtual host");

            System.err.println
                ("Default options:");
            System.err.println
                (" -a 8080 -f /=. -d / -s21 /servlet/*=./servlets -h /=com.mortbay.HTTP.Handler.NotFoundHandler");
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
                    // Look for listener
                    if ("-a".equals(args[i]))
                    {
                        // Add listener.
                        i++;
                        InetAddrPort address = new InetAddrPort(args[i]);
                        server.startListener(address);
                    }

                    // Look for dump handler
                    if ("-d".equals(args[i]))
                    {
                        i++;
                        HttpHandler handler=new DumpHandler();
                        server.mapHandler(host,args[i],handler);
                        handler.start();
                    }
                    
                    // Look for file handler
                    if ("-f".equals(args[i]))
                    {
                        i++;
                        String spec=args[i];
                        int e=spec.indexOf("=");
                        if (e>0)
                        {
                            String pathSpec=spec.substring(0,e);
                            String file=spec.substring(e+1);
                            server.mapFiles(host,pathSpec,file);
                        }
                    }
		    
                    // Look for servlet handler
                    if ("-s21".equals(args[i]))
                    {
                        i++;
                        String spec=args[i];
                        int e=spec.indexOf("=");
                        if (e>0)
                        {
                            String pathSpec=spec.substring(0,e);
                            String file=spec.substring(e+1);
			    com.mortbay.HTTP.Handler.Servlet2_1.ServletHandler
				handler = new
				    com.mortbay.HTTP.Handler.Servlet2_1.ServletHandler();
			    handler.addDynamic(pathSpec,file,true,null);
                            server.mapHandler(host,pathSpec,handler);
                            handler.start();
                        }
                    }
                    
                    // Look for Virtual host
                    if ("-v".equals(args[i]))
                    {
                        i++;
                        host=args[i];
                        int e=host.indexOf("=");
                        if (e>0)
                        {
                            String alias=host.substring(e+1);
                            host=host.substring(0,e);
                            server.hostAlias(host,alias);
                        }
                    }
                    
                    // Look for handler
                    if ("-h".equals(args[i]))
                    {
                        i++;
                        String spec=args[i];
                        int e=spec.indexOf("=");
                        if (e>0)
                        {
                            String pathSpec=spec.substring(0,e);
                            String className=spec.substring(e+1);
                            HttpHandler handler = (HttpHandler)
                                Class.forName(className).newInstance();
                            handler.start();
                            server.mapHandler(host,pathSpec,handler);
                        }
                    }
                }
                catch (Exception e)
                {
                    Code.warning(e);
                }
            }
        }
        catch (Exception e)
        {
            Code.warning(e);
        }
    }
}
