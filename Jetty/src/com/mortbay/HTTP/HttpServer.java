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
    PathMap _handlerMap = new PathMap();
    HashMap _fileMap = new HashMap(7);
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public HttpServer()
    {}
    
    /* ------------------------------------------------------------ */
    public void startListener(InetAddrPort address)
        throws IOException
    {
        HttpListener listener = (HttpListener)_listeners.get(address);
        if (listener==null)
        {
            listener=new SocketListener(this,address);
            _listeners.put(address,listener);
        }

        listener.start();
    }
    
    /* ------------------------------------------------------------ */
    public void stopListener(InetAddrPort address)
        throws InterruptedException
    {
        HttpListener listener = (HttpListener)_listeners.get(address);
        if (listener!=null)
            listener.stop();
    }
    
    /* ------------------------------------------------------------ */
    public void destroyListener(InetAddrPort address)
    {
        HttpListener listener = (HttpListener)_listeners.remove(address);
        if (listener!=null)
            listener.destroy();
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return Collection of all listeners.
     */
    public Collection getListeners()
    {
        return _listeners.values();
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
     * @param pathSpec 
     * @param handler 
     */
    public void mapHandler(String pathSpec, HttpHandler handler)
    {
        List list=(List)_handlerMap.get(pathSpec);
        if (list==null)
        {
            list=new ArrayList(8);
            _handlerMap.put(pathSpec,list);
        }
        if (!list.contains(handler))
            list.add(handler);
    }
    
    /* ------------------------------------------------------------ */
    /** Unmap a handler from a path specification.
     * @param pathSpec 
     * @param handler 
     */
    public void unmapHandler(String pathSpec, HttpHandler handler)
    {
        List list=(List)_handlerMap.get(pathSpec);
        if (list!=null)
            list.remove(handler);
    }


    /* ------------------------------------------------------------ */
    /** Converniance method for adding FileHandlers.
     * A single FileHandler instance is maintained for each
     * mapped filename and can be mapped to multiple path specifications.
     * @param pathSpec 
     * @param filename 
     */
    public void mapFiles(String pathSpec, String filename)
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
        mapHandler(pathSpec,fileHandler);
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
        List matches = (List)_handlerMap.getMatches(request.getPath());

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
    /** 
     * @param args 
     */
    public static void main(String[] args)
    {
        if (args.length==0)
        {
            String[] newArgs= {"-a","8080","-f","/=.","-d","/"};
            args=newArgs;
        }
        else if (args.length%2==1)
        {
            System.err.println
                ("Usage - java com.mortbay.HTTP.HttpServer [ -a <value> ... ]");
            System.err.println
                (" -a [<addr>:]<port>  - Listen on [ address & ] port");
            System.err.println
                (" -f <path>=<dir>     - File handler at path Spec to file/directory");
            System.err.println
                (" -d <path>           - Dump handler at path Spec");
            System.err.println
                ("Default options: -a 8080 -f /=. -d /");
            
            System.exit(1);
        }
        
        try{
            // Create the server
            HttpServer server = new HttpServer();

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
                        server.mapHandler(args[i],handler);
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
                            server.mapFiles(pathSpec,file);
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
};
