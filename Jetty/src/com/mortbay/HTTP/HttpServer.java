// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;

import com.mortbay.HTTP.Handler.NotFoundHandler;
import com.mortbay.HTTP.Handler.DumpHandler;
import com.mortbay.Util.Code;
import com.mortbay.Util.DateCache;
import com.mortbay.Util.InetAddrPort;
import com.mortbay.Util.LifeCycle;
import com.mortbay.Util.Log;
import com.mortbay.Util.LogSink;
import com.mortbay.Util.MultiException;
import com.mortbay.Util.URI;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.beans.beancontext.BeanContextSupport;



/* ------------------------------------------------------------ */
/** HTTP Server.
 * Services HTTP requests by maintaining a mapping between
 * a collection of HttpListeners which generate requests and
 * HttpContexts which contain collections of HttpHandlers.
 *
 * This class is configured by API calls.  The
 * com.mortbay.Jetty.Server class uses XML configuration files to
 * configure instances of this class.
 *
 * The HttpServer implements the BeanContext API so that membership
 * events may be generated for HttpListeners, HandlerContexts and WebApplications.
 *
 * @see HttpContext
 * @see HttpHandler
 * @see HttpConnection
 * @see HttpListener
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class HttpServer extends BeanContextSupport implements LifeCycle
{    
    /* ------------------------------------------------------------ */
    private static ArrayList __servers = new ArrayList(3);
    private static List __roServers = Collections.unmodifiableList(__servers);
    
    /* ------------------------------------------------------------ */
    private HashMap _listeners = new HashMap(3);
    private HttpEncoding _httpEncoding ;
    private LogSink _logSink;
    private DateCache _dateCache=
        new DateCache("dd/MMM/yyyy:HH:mm:ss");
    private HashMap _realmMap = new HashMap(3);
    
    // HttpServer[host->PathMap[contextPath->List[HanderContext]]]
    // HandlerContext[List[HttpHandler]]
    private HashMap _hostMap = new HashMap(3);
    
    private HandlerContext _notFoundContext=null;
    private boolean _chunkingForced=false;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public HttpServer()
    {
        __servers.add(this);
    }
    
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
    /** Start all handlers then listeners.
     */
    public synchronized void start()
        throws Exception
    {
        Log.event("start HttpServer version "+Version.__VersionImpl);

        MultiException mex = new MultiException();
        
        if (Code.verbose(99))
        {
            Code.debug("LISTENERS: ",_listeners);
            Code.debug("HANDLER: ",_hostMap);
        }   

        if (_logSink!=null)
            try{_logSink.start();}catch(Exception e){mex.add(e);}
        
        
        Iterator contexts = getHandlerContexts().iterator();
        while(contexts.hasNext())
        {
            HandlerContext context=(HandlerContext)contexts.next();
            if (!context.isStarted())
                try{context.start();}catch(Exception e){mex.add(e);}
        }
        
        Iterator listeners = getListeners().iterator();
        while(listeners.hasNext())
        {
            HttpListener listener =(HttpListener)listeners.next();
            listener.setHttpServer(this);
            if (!listener.isStarted())
                try{listener.start();}catch(Exception e){mex.add(e);}
        }

        mex.ifExceptionThrow();
    }
    
    /* ------------------------------------------------------------ */
    /** Start all handlers then listeners.
     */
    public synchronized boolean isStarted()
    {
        if (_logSink!=null && _logSink.isStarted())
            return true;
        
        Iterator listeners = getListeners().iterator();
        while(listeners.hasNext())
        {
            HttpListener listener =(HttpListener)listeners.next();
            if (listener.isStarted())
                return true;
        }
        Iterator contexts = getHandlerContexts().iterator();
        while(contexts.hasNext())
        {
            HandlerContext context=(HandlerContext)contexts.next();
            if (context.isStarted())
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
            {
                try{listener.stop();}
                catch(Exception e)
                {
                    if (Code.debug())
                        Code.warning(e);
                    else
                        Code.warning(e.toString());
                }
            }
        }
        
        Iterator contexts = getHandlerContexts().iterator();
        while(contexts.hasNext())
        {
            HandlerContext context=(HandlerContext)contexts.next();
            if (context.isStarted())
                context.stop();
        }

        if (_logSink!=null)
            _logSink.stop();
    }

    
    /* ------------------------------------------------------------ */
    /** Stop all listeners then handlers.
     * All the handlers are unmapped and the listeners removed.
     */
    public synchronized void destroy()
    {
        __servers.remove(this);
        Iterator listeners = getListeners().iterator();
        while(listeners.hasNext())
        {
            HttpListener listener =(HttpListener)listeners.next();
            try{listener.destroy();}
            catch(Exception e){Code.warning(e);}
        }
        
        Iterator contexts = getHandlerContexts().iterator();
        while(contexts.hasNext())
        {
            HandlerContext context=(HandlerContext)contexts.next();
            try{context.destroy();}
            catch(Exception e){Code.warning(e);}
        }

        _hostMap.clear();
        _hostMap=null;
        _listeners.clear();
        _listeners=null;
        setLogSink(null);
    }

    /* ------------------------------------------------------------ */
    public synchronized boolean isDestroyed()
    {
        return _hostMap==null;
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

        add(listener);
        return listener;
    }
    
    /* ------------------------------------------------------------ */
    /** Add a HTTP Listener to the server.
     * @param listener The Listener.
     * @exception IllegalArgumentException If the listener is not for this
     * server.
     */
    public void addListener(HttpListener listener)
        throws IllegalArgumentException
    {
        listener.setHttpServer(this);        
        _listeners.put(listener,listener);
        add(listener);
    }
    
    /* ------------------------------------------------------------ */
    /** Add a HTTP Listener to the server.
     * @param listenerClass The Listener classname, or null for the default
     * Listener class.
     * @exception IllegalArgumentException
     */
    public HttpListener addListener(String listenerClass)
        throws IllegalArgumentException
    {
        try
        {
            if (listenerClass==null || listenerClass.length()==0)
                listenerClass="com.mortbay.HTTP.SocketListener";
            Class lc = Class.forName(listenerClass);
            HttpListener listener = (HttpListener) lc.newInstance();
            listener.setHttpServer(this);        
            _listeners.put(listener,listener);
            add(listener);
            return listener;
        }
        catch(Exception e)
        {
            Code.warning(e);
            throw new IllegalArgumentException(e.toString());
        }
    }
    
    /* ------------------------------------------------------------ */
    /** Remove a HTTP Listener.
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
            {
                iterator.remove();
                remove(listener);
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return Set of all listeners.
     */
    public Collection getListeners()
    {
        if (_listeners==null)
            return Collections.EMPTY_LIST;
        return _listeners.values();
    }

    
    /* ------------------------------------------------------------ */
    /** Define a virtual host alias.
     * All requests to the alias are handled the same as request for
     * the host.
     * @param host Host name or IP
     * @param alias Alias hostname or IP
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
     * host and contextPath. Requests are offered to multiple
     * contexts in the order they where added to the HttpServer.
     * @param contextPath
     * @return 
     */
    public HandlerContext addContext(String contextPath)
    {
        return addContext(null,contextPath);
    }

    /* ------------------------------------------------------------ */
    /** Create a new HandlerContext
     * @param contextPathSpec 
     * @return 
     */
    protected HandlerContext newHandlerContext(String contextPathSpec)
    {
        return new HandlerContext(this,contextPathSpec);
    }
    
    /* ------------------------------------------------------------ */
    /** Create and add a new context.
     * Note that multiple contexts can be created for the same
     * host and contextPath. Requests are offered to multiple
     * contexts in the order they where added to the HttpServer.
     * @param host Virtual hostname or null for all hosts.
     * @param contextPathSpec
     * @return 
     */
    public HandlerContext addContext(String host, String contextPathSpec)
    {
        if (host!=null && host.length()==0)
            host=null;
        HandlerContext hc = newHandlerContext(contextPathSpec);
        addContext(host,hc);
        return hc;
    }

    /* ------------------------------------------------------------ */
    /** Add a context.
     * As contexts cannot be publicly created, this may be used to
     * alias an existing context.
     * @param host The virtual host or null for all hosts.
     * @param context 
     */
    public void addContext(String host,
                           HandlerContext context)
    {
        PathMap contextMap=(PathMap)_hostMap.get(host);
        if (contextMap==null)
        {
            contextMap=new PathMap(7);
            _hostMap.put(host,contextMap);
        }

        String contextPathSpec=context.getContextPath();
        if (contextPathSpec.length()>1)
            contextPathSpec+="/*";
        
        List contextList = (List)contextMap.get(contextPathSpec);
        if (contextList==null)
        {
            contextList=new ArrayList(1);
            contextMap.put(contextPathSpec,contextList);
        }

        contextList.add(context);
        context.addHost(host);
        add(context);

        Code.debug("Added ",context," for host ",host);
    }

    /* ------------------------------------------------------------ */
    /** Remove a context or Web application.
     * @param host The virtual host or null for all hosts.
     * @param contextPathSpec
     * @param i Index among contexts of same host and pathSpec.
     * @exception IllegalStateException if context not stopped
     */
    public void removeContext(String host, String contextPathSpec, int i)
        throws IllegalStateException
    {
        PathMap contextMap=(PathMap)_hostMap.get(host);
        if (contextMap!=null)
        {
            List contextList = (List)contextMap.get(contextPathSpec);
            if (contextList!=null)
            {
                if (i< contextList.size())
                {
                    HandlerContext hc=(HandlerContext)contextList.get(i);
                    if (hc!=null && hc.isStarted())
                        throw new IllegalStateException("Context not stopped");
                    remove(hc);
                    contextList.remove(i);
                }
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    /** Remove a context or Web application.
     * @exception IllegalStateException if context not stopped
     */
    public void removeContext(HandlerContext context)
        throws IllegalStateException
    {
        if (context.isStarted())
            throw new IllegalStateException("Context not stopped");
                    
        Iterator i1 = _hostMap.values().iterator();
        while(i1.hasNext())
        {
            PathMap contextMap=(PathMap)i1.next();

            Iterator i2=contextMap.values().iterator();
            while(i2.hasNext())
            {
                List contextList = (List)i2.next();
                if(contextList.remove(context))
                    remove(context);
            }
        }
    }
    
        
    /* ------------------------------------------------------------ */
    /** 
     * @param host The virtual host or null for all hosts.
     * @param contextPathSpec
     * @param i Index among contexts of same host and pathSpec.
     * @return The HandlerContext or null.
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
    /** Get or create context. 
     * @param host The virtual host or null for all hosts.
     * @param contextPath 
     * @return HandlerContext. If multiple contexts exist for the same
     * host and pathSpec, the most recently added context is returned.
     * If no context exists, a new context is defined.
     */
    public HandlerContext getContext(String host, String contextPath)
    { 
        HandlerContext hc=null;

        PathMap contextMap=(PathMap)_hostMap.get(host);
        if (contextMap!=null)
        {
            List contextList = (List)contextMap.get(contextPath);
            if (contextList!=null && contextList.size()>0)
                hc=(HandlerContext)contextList.get(contextList.size()-1);
            
        }
        if (hc==null)
            hc=addContext(host,contextPath);

        return hc;
    }
    
    
 
    /* ------------------------------------------------------------ */
    /** 
     * @return Collection of all handler.
     */
    public synchronized Set getHandlers()
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
    /** 
     * @return Collection of all handler.
     */
    public synchronized Set getHandlerContexts()
    {
        if (_hostMap==null)
            return Collections.EMPTY_SET;
        
        HashSet set = new HashSet(33);
        Iterator maps=_hostMap.values().iterator();
        while (maps.hasNext())
        {
            PathMap pm=(PathMap)maps.next();
            Iterator lists=pm.values().iterator();
            while(lists.hasNext())
            {
                List list=(List)lists.next();
                set.addAll(list);
            }
        }
        return set;
    }

    /* ------------------------------------------------------------ */
    public synchronized LogSink getLogSink()
    {
        return _logSink;
    }
    
    /* ------------------------------------------------------------ */
    /** Set the request log.
     * Set the LogSink to be used for the request log. The log is
     * written in the combined format.
     * @param logSink 
     */
    public synchronized void setLogSink(LogSink logSink)
    {
        if (_logSink!=null)
        {
            add(logSink);
            if(isStarted())
            {
                try
                {
                    _logSink.start();
                }
                catch(Exception e)
                {
                    Code.warning(e);
                    return;
                }
            }
        }
        
        if (_logSink!=null)
        {
            try{
                remove(_logSink);
                _logSink.stop();
            }
            catch(InterruptedException e)
            {
                Code.ignore(e);
            }
            finally
            {
                _logSink.destroy();
            }
        }	
            
        _logSink=logSink;
    }
    
    /* ------------------------------------------------------------ */
    /** Set the request log date format.
     * @param format 
     */
    public synchronized void setLogDateFormat(String format)
    {
        _dateCache=new DateCache(format);
    }
    
    /* ------------------------------------------------------------ */
    /** Get the request log date format.
     * @param format 
     */
    public synchronized String getLogDateFormat()
    {
        return _dateCache.getFormatString();
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
                        List contextList = (List)entry.getValue();
                
                        for (int j=0;j<contextList.size();j++)
                        {
                            HandlerContext context=
                                (HandlerContext)contextList.get(j);
                            
                            if (Code.debug())
                                Code.debug("Try ",context,
                                           ",",new Integer(j));

                            if (context.handle(request,
                                               response))
                                return;
                        }
                    }   
                }
            }
            
            // try no host
            if (request.isHandled() || host==null)
                break;
            host=null;
        }	

        synchronized(this)
        {
            if (_notFoundContext==null)
            {
                _notFoundContext=addContext(null,"/");
                Log.event("Adding NotFoundHandler to "+_notFoundContext);
                _notFoundContext.addHandler(new NotFoundHandler());
                try{_notFoundContext.start();}catch(Exception e){Code.warning(e);}
            }
            if (!_notFoundContext.handle(request,response))
                response.sendError(response.__404_Not_Found);
        }
    }
    
    /* ------------------------------------------------------------ */
    /** Find handler.
     * Find a handler for a URI.  This method is provided for
     * the servlet context getContext method to search for another
     * context by URI.  A list of hosts may be passed to qualify the
     * search.
     * @param uri URI that must be satisfied by the servlet handler 
     * @param hosts null or a list of virtual hosts names to search
     * @return HttpHandler
     */
    public HttpHandler findHandler(Class handlerClass,
                                   String uri,
                                   List hosts)
    {
        uri = URI.stripPath(uri);
        for (int h=0; h<hosts.size() ; h++)
        {
            String host = (String)hosts.get(h);
            
            PathMap contextMap=(PathMap)_hostMap.get(host);
            if (contextMap!=null)
            {
                List contextLists =contextMap.getMatches(uri);
                if(contextLists!=null)
                {
                    for (int i=0;i<contextLists.size();i++)
                    {
                        Map.Entry entry=
                            (Map.Entry)
                            contextLists.get(i);
                        String contextPath=(String)entry.getKey();
                        List contextList = (List)entry.getValue();
                
                        for (int j=0;j<contextList.size();j++)
                        {
                            HandlerContext context=
                                (HandlerContext)contextList.get(j);
                            
                            HttpHandler handler =
                                context.getHandler(handlerClass);

                            if (handler!=null)
                                return handler;
                        }
                    }   
                }
            }
        }	
        return null;
    }
    
    /* ------------------------------------------------------------ */
    /** Log a request and response.
     * The log is written in combined format.
     * @param request 
     * @param response 
     */
    public synchronized void log(HttpRequest request,
                                 HttpResponse response,
                                 int length)
    {
        // Log request - XXX should be in HttpHandler
        if (_logSink!=null && request!=null && response!=null)
        {
            String bytes = ((length>=0)?Integer.toString(length):"-");
            String user = (String)request.getAttribute(HttpRequest.__AuthUser);
            if (user==null)
                user = "-";
            
            String referer = request.getField(HttpFields.__Referer);
            if (referer==null)
                referer="-";
            else
                referer="\""+referer+"\"";
            
            String agent = request.getField(HttpFields.__UserAgent);
            if (agent==null)
                agent="-";
            else
                agent="\""+agent+"\"";	    
            
            String log= request.getRemoteAddr() +
                " - "+
                user +
                " [" +
                _dateCache.format(System.currentTimeMillis())+
                "] \""+
                request.getRequestLine()+
                "\" "+
                response.getStatus()+
                " " +
                bytes +
                " " +
                referer +
                " " +
                agent;

            _logSink.log(log);
        }
    }

    /* ------------------------------------------------------------ */
    public UserRealm addRealm(UserRealm realm)
    {
        return (UserRealm)_realmMap.put(realm.getName(),realm);
    }
    
    /* ------------------------------------------------------------ */
    public UserRealm getRealm(String realmName)
    {
        return (UserRealm)_realmMap.get(realmName);
    }
    
    /* ------------------------------------------------------------ */
    public UserRealm removeRealm(String realmName)
    {
        return (UserRealm)_realmMap.remove(realmName);
    }    
    
    /* ------------------------------------------------------------ */
    public boolean isChunkingForced()
    {
        return _chunkingForced;
    }
    
    /* ------------------------------------------------------------ */
    /** Set Chunking Forced.
     * By default chunking is not forced on resources of known length.
     * @param forced If true, chunking is used for all HTTP/1.1
     * responses, even if a content-length was known.
     */
    public void setChunkingForced(boolean forced)
    {
         _chunkingForced=forced;
    }
    
    /* ------------------------------------------------------------ */
    public static List getHttpServerList()
    {
        return __roServers;
    }

    /* ------------------------------------------------------------ */
    public Map getHostMap()
    {
        return _hostMap;
    }

    /* ------------------------------------------------------------ */
    /** Construct server from command line arguments.
     * @param args 
     */
    public static void main(String[] args)
    {
        if (args.length==0)
        {
            args= new String[] {"8080"};
        }
        else if (args.length==1 && args[0].startsWith("-"))
        {
            System.err.println
                ("\nUsage - java com.mortbay.HTTP.HttpServer [[<addr>:]<port> .. ]");
            System.err.println
                ("\n  Serves files from 'docroot' directory");
            System.err.println
                ("\n  Default port is 8080");
            System.exit(1);
        }
        
        try{
            // Create the server
            HttpServer server = new HttpServer();

            // Default is no virtual host
            String host=null;
            HandlerContext context = server.getContext(host,"/");
            context.setResourceBase("docroot/");
            context.setServingResources(true);
            context.addHandler(new DumpHandler());
            
            // Parse arguments
            for (int i=0;i<args.length;i++)
            {
                InetAddrPort address = new InetAddrPort(args[i]);
                server.addListener(address);
            }
            
            server.start();
        }
        catch (Exception e)
        {
            Code.warning(e);
        }
    }
    
}
