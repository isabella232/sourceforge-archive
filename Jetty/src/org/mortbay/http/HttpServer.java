// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.mortbay.http.handler.DumpHandler;
import org.mortbay.http.handler.NotFoundHandler;
import org.mortbay.util.Code;
import org.mortbay.util.InetAddrPort;
import org.mortbay.util.LifeCycle;
import org.mortbay.util.Log;
import org.mortbay.util.LogSink;
import org.mortbay.util.MultiException;
import org.mortbay.util.StringMap;
import org.mortbay.util.URI;



/* ------------------------------------------------------------ */
/** HTTP Server.
 * Services HTTP requests by maintaining a mapping between
 * a collection of HttpListeners which generate requests and
 * HttpContexts which contain collections of HttpHandlers.
 *
 * This class is configured by API calls.  The
 * org.mortbay.jetty.Server class uses XML configuration files to
 * configure instances of this class.
 *
 * The HttpServer implements the BeanContext API so that membership
 * events may be generated for HttpListeners, HttpContexts and WebApplications.
 *
 * @see HttpContext
 * @see HttpHandler
 * @see HttpConnection
 * @see HttpListener
 * @see org.mortbay.jetty.Server
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class HttpServer implements LifeCycle
{
    /* ------------------------------------------------------------ */
    private static ArrayList __servers = new ArrayList(3);
    private static List __roServers = Collections.unmodifiableList(__servers);
    
    /* ------------------------------------------------------------ */
    private HashMap _listeners = new HashMap(3);
    private HttpEncoding _httpEncoding ;
    private HashMap _realmMap = new HashMap(3);
    
    // HttpServer[host->PathMap[contextPath->List[HanderContext]]]
    // HttpContext[List[HttpHandler]]
    private StringMap _hostMap = new StringMap();
    
    private HttpContext _notFoundContext=null;
    private boolean _chunkingForced=false;
    
    private LogSink _requestLogSink;
    private RequestLogFormat _requestLogFormat;
    private List _eventListeners;
    private List _components;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public HttpServer()
    {
        __servers.add(this);
        _hostMap.setIgnoreCase(true);
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return The HttpEncoding helper instance.
     */
    public HttpEncoding getHttpEncoding()
    {
        if (_httpEncoding==null)
            _httpEncoding=new HttpEncoding();
        return _httpEncoding;
    }
    
    /* ------------------------------------------------------------ */
    /** The HttpEncoding instance is used to extend the transport
     * encodings supprted by this server.
     * @param httpEncoding The HttpEncoding helper instance.
     */
    public void setHttpEncoding(HttpEncoding httpEncoding)
    {
        _httpEncoding = httpEncoding;
    }
    
    /* ------------------------------------------------------------ */
    /** Start all handlers then listeners.
     * If a subcomponent fails to start, it's exception is added to a
     * org.mortbay.util.MultiException and the start method continues.
     * @exception MultiException A collection of exceptions thrown by
     * start() method of subcomponents of the HttpServer. 
     */
    public synchronized void start()
        throws MultiException
    {
        Log.event("Starting "+Version.__VersionImpl);
        MultiException mex = new MultiException();
        
        if (Code.verbose(99))
        {
            Code.debug("LISTENERS: ",_listeners);
            Code.debug("HANDLER: ",_hostMap);
        }   

        if (_requestLogSink!=null)
        {
            if (!_requestLogSink.isStarted())
            {
                try{
                    _requestLogSink.start();
                    Log.event("Started "+_requestLogSink);
                }
                catch(Exception e){mex.add(e);}
            }

            if (_requestLogFormat==null)
            {
                String logDateFormat="dd/MMM/yyyy:HH:mm:ss ZZZ";
                try
                {
                    // XXX - this shows that the design of LogSink is WRONG!
                    Method gldf = _requestLogSink.getClass().getMethod("getLogDateFormat",null);
                    logDateFormat=(String)gldf.invoke(_requestLogSink,null);
                }
                catch(Exception e)
                {
                    Code.ignore(e);
                }
                _requestLogFormat=new NCSARequestLogFormat(logDateFormat,
                                                           TimeZone.getDefault().getID(),
                                                           true);
            }
        }
        
        Iterator contexts = getHttpContexts().iterator();
        while(contexts.hasNext())
        {
            HttpContext context=(HttpContext)contexts.next();
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
        
        mex.ifExceptionThrowMulti();
        Log.event("Started "+this);
    }
    
    /* ------------------------------------------------------------ */
    /** Start all handlers then listeners.
     */
    public synchronized boolean isStarted()
    {
        if (_requestLogSink!=null && _requestLogSink.isStarted())
            return true;
        
        Iterator listeners = getListeners().iterator();
        while(listeners.hasNext())
        {
            HttpListener listener =(HttpListener)listeners.next();
            if (listener.isStarted())
                return true;
        }
        Iterator contexts = getHttpContexts().iterator();
        while(contexts.hasNext())
        {
            HttpContext context=(HttpContext)contexts.next();
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
        
        Iterator contexts = getHttpContexts().iterator();
        while(contexts.hasNext())
        {
            HttpContext context=(HttpContext)contexts.next();
            if (context.isStarted())
                context.stop();
        }

        if (_requestLogSink!=null)
        {
            _requestLogSink.stop();
            Log.event("Stopped "+_requestLogSink);
        }
        Log.event("Stopped "+this);
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
                listenerClass="org.mortbay.http.SocketListener";
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
    /** Create a new HttpContext.
     * Specialized HttpServer classes may specialize this method to
     * return subclasses of HttpContext.
     * @param contextPathSpec 
     * @return A new instance of HttpContext or a subclass of HttpContext
     */
    protected HttpContext newHttpContext(String contextPathSpec)
    {
        return new HttpContext(this,contextPathSpec);
    }
    
    /* ------------------------------------------------------------ */
    /** Create and add a new context.
     * Note that multiple contexts can be created for the same
     * host and contextPath. Requests are offered to multiple
     * contexts in the order they where added to the HttpServer.
     * @param contextPath
     * @return A HttpContext instance created by a call to newHttpContext.
     */
    public HttpContext addContext(String contextPath)
    {
        return addContext(null,contextPath);
    }

    
    /* ------------------------------------------------------------ */
    /** Create and add a new context.
     * Note that multiple contexts can be created for the same
     * host and contextPath. Requests are offered to multiple
     * contexts in the order they where added to the HttpServer.
     * @param host Virtual hostname or null for all hosts.
     * @param contextPathSpec
     * @return A HttpContext instance created by a call to newHttpContext.
     */
    public HttpContext addContext(String host, String contextPathSpec)
    {
        if (host!=null && host.length()==0)
            host=null;
        HttpContext hc = newHttpContext(contextPathSpec);
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
                           HttpContext context)
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
                    HttpContext hc=(HttpContext)contextList.get(i);
                    if (hc!=null && hc.isStarted())
                        throw new IllegalStateException("Context not stopped");
                    remove(hc);
                    contextList.remove(i);
                }
            }
            if (contextList.size()==0)
                contextMap.remove(contextPathSpec);
        }
    }
    
    /* ------------------------------------------------------------ */
    /** Remove a context or Web application.
     * @exception IllegalStateException if context not stopped
     */
    public void removeContext(HttpContext context)
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
                if (contextList.size()==0)
                    i2.remove();
            }
        }
    }
    
        
    /* ------------------------------------------------------------ */
    /** 
     * @param host The virtual host or null for all hosts.
     * @param contextPathSpec
     * @param i Index among contexts of same host and pathSpec.
     * @return The HttpContext or null.
     */
    public HttpContext getContext(String host, String contextPathSpec, int i)
    {
        HttpContext hc=null;

        PathMap contextMap=(PathMap)_hostMap.get(host);
        if (contextMap!=null)
        {
            List contextList = (List)contextMap.get(contextPathSpec);
            if (contextList!=null)
            {
                if (i>=contextList.size())
                    return null;
                hc=(HttpContext)contextList.get(i);
            }
        }

        return hc;
    }

    
    /* ------------------------------------------------------------ */
    /** Get or create context. 
     * @param host The virtual host or null for all hosts.
     * @param contextPath 
     * @return HttpContext. If multiple contexts exist for the same
     * host and pathSpec, the most recently added context is returned.
     * If no context exists, a new context is created by a call to newHttpContext.
     */
    public HttpContext getContext(String host, String contextPath)
    { 
        HttpContext hc=null;

        PathMap contextMap=(PathMap)_hostMap.get(host);
        if (contextMap!=null)
        {
            List contextList = (List)contextMap.get(contextPath);
            if (contextList!=null && contextList.size()>0)
                hc=(HttpContext)contextList.get(contextList.size()-1);
            
        }
        if (hc==null)
            hc=addContext(host,contextPath);

        return hc;
    }
    
    
 
    /* ------------------------------------------------------------ */
    /** 
     * @return Collection of all handler from all contexts
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
                    HttpContext context = (HttpContext) contexts.next();
                    set.addAll(context.getHttpHandlers());
                }
            }
        }
        return set;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return Collection of all handler.
     */
    public synchronized Set getHttpContexts()
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
    /** 
     * @deprecated use getRequestLogSink()
     */
    public synchronized LogSink getLogSink()
    {
        return getRequestLogSink();
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @deprecated use setRequestLogSink()
     */
    public synchronized void setLogSink(LogSink logSink)
    {
        setRequestLogSink(logSink);
    }
    
    /* ------------------------------------------------------------ */
    public synchronized LogSink getRequestLogSink()
    {
        return _requestLogSink;
    }
    
    /* ------------------------------------------------------------ */
    /** Set the request log.
     * Set the LogSink to be used for the request log.
     * @param logSink 
     */
    public synchronized void setRequestLogSink(LogSink logSink)
    {
        if (isStarted())
            throw new IllegalStateException("Started");
        if (_requestLogSink!=null)
            remove(_requestLogSink);
        _requestLogSink=logSink;
        if (_requestLogSink!=null)
            add(_requestLogSink);
    }

    /* ------------------------------------------------------------ */
    public RequestLogFormat getRequestLogFormat()
    {
        return _requestLogFormat;
    }
    
    /* ------------------------------------------------------------ */
    /** Set the requestLogFormat.
     * Set the format instance to be used for formatting the request
     * log. The default requestLogFormat is an extended
     * NCSARequestLogFormat using the date format of the request
     * LogSink in the GMT timezone.
     * @param format 
     */
    public void setRequestLogFormat(RequestLogFormat format)
    {
        _requestLogFormat=format;
    }

    /* ------------------------------------------------------------ */
    /** Log a request to the request log
     * @param request The request.
     * @param response The response generated.
     * @param length The length of the body.
     */
    void log(HttpRequest request,
             HttpResponse response,
             int length)
    {
        if (_requestLogSink!=null &&
            _requestLogFormat!=null &&
            request!=null &&
            response!=null)
            _requestLogSink.log(_requestLogFormat.format(request,response,length));
    }
    
    /* ------------------------------------------------------------ */
    /** Service a request.
     * Handle the request by passing it to the HttpHandler contained in
     * the mapped HttpContexts.
     * The requests host and path are used to select a list of
     * HttpContexts. Each HttpHandler in these context is offered
     * the request in turn, until the request is handled.
     *
     * If no handler handles the request, 404 Not Found is returned.
     *
     * @param request 
     * @param response
     * @return The HttpContext that completed handling of the request or null.
     * @exception IOException 
     * @exception HttpException 
     */
    public HttpContext service(HttpRequest request,HttpResponse response)
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
                            HttpContext context=
                                (HttpContext)contextList.get(j);
                            
                            if (Code.debug())
                                Code.debug("Try ",context,",",new Integer(j));

                            if (context.handle(request,response))
                                return context;
                        }
                    }   
                }
            }
            
            // try no host
            if (host==null)
                break;
            host=null;
        }	

        synchronized(this)
        {
            if (_notFoundContext==null)
            {
                _notFoundContext=new HttpContext(this,"/");
                _notFoundContext.addHttpHandler(new NotFoundHandler());
                try{_notFoundContext.start();}catch(Exception e){Code.warning(e);}
            }
            if (!_notFoundContext.handle(request,response))
                response.sendError(response.__404_Not_Found);
            return _notFoundContext;
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
                            HttpContext context=
                                (HttpContext)contextList.get(j);
                            
                            HttpHandler handler =
                                context.getHttpHandler(handlerClass);

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
    /* ------------------------------------------------------------ */
    private boolean _statsOn=false;
    private int _connections;
    private int _connectionsOpen;
    private int _connectionsOpenMax;
    private long _connectionsDurationAve;
    private long _connectionsDurationMax;
    private int _connectionsRequestsAve;
    private int _connectionsRequestsMax;

    private int _errors;
    private int _requests;
    private int _requestsActive;
    private int _requestsActiveMax;
    private long _requestsDurationAve;
    private long _requestsDurationMax;    

    /* ------------------------------------------------------------ */
    /** Reset statistics.
     */
    public void statsReset()
    {
        _connections=0;
        _connectionsOpen=0;
        _connectionsOpenMax=0;
        _connectionsDurationAve=0;
        _connectionsDurationMax=0;
        _connectionsRequestsAve=0;
        _connectionsRequestsMax=0;
        
        _errors=0;
        _requests=0;
        _requestsActive=0;
        _requestsActiveMax=0;
        _requestsDurationAve=0;
        _requestsDurationMax=0;
    }
    
    /* ------------------------------------------------------------ */
    public void setStatsOn(boolean on)
    {
        Log.event("Statistics on = "+on+" for "+this);
        _statsOn=on;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return True if statistics collection is turned on.
     */
    public boolean getStatsOn()
    {
        return _statsOn;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return Number of connections accepted by the server since
     * statsReset() called. Undefined if setStatsOn(false).
     */
    public int getConnections() {return _connections;}

    /* ------------------------------------------------------------ */
    /** 
     * @return Number of connections currently open that were opened
     * since statsReset() called. Undefined if setStatsOn(false).
     */
    public int getConnectionsOpen() {return _connectionsOpen;}

    /* ------------------------------------------------------------ */
    /** 
     * @return Maximum number of connections opened simultaneously
     * since statsReset() called. Undefined if setStatsOn(false).
     */
    public int getConnectionsOpenMax() {return _connectionsOpenMax;}

    /* ------------------------------------------------------------ */
    /** 
     * @return Sliding average duration in milliseconds of open connections
     * since statsReset() called. Undefined if setStatsOn(false).
     */
    public long getConnectionsDurationAve() {return _connectionsDurationAve/128;}

    /* ------------------------------------------------------------ */
    /** 
     * @return Maximum duration in milliseconds of an open connection
     * since statsReset() called. Undefined if setStatsOn(false).
     */
    public long getConnectionsDurationMax() {return _connectionsDurationMax;}

    /* ------------------------------------------------------------ */
    /** 
     * @return Sliding average number of requests per connection
     * since statsReset() called. Undefined if setStatsOn(false).
     */
    public int getConnectionsRequestsAve() {return _connectionsRequestsAve/16;}

    /* ------------------------------------------------------------ */
    /** 
     * @return Maximum number of requests per connection
     * since statsReset() called. Undefined if setStatsOn(false).
     */
    public int getConnectionsRequestsMax() {return _connectionsRequestsMax;}


    /* ------------------------------------------------------------ */
    /** 
     * @return Number of errors generated while handling requests.
     * since statsReset() called. Undefined if setStatsOn(false).
     */
    public int getErrors() {return _errors;}

    /* ------------------------------------------------------------ */
    /** 
     * @return Number of requests
     * since statsReset() called. Undefined if setStatsOn(false).
     */
    public int getRequests() {return _requests;}

    /* ------------------------------------------------------------ */
    /** 
     * @return Number of requests currently active.
     * Undefined if setStatsOn(false).
     */
    public int getRequestsActive() {return _requestsActive;}

    /* ------------------------------------------------------------ */
    /** 
     * @return Maximum number of active requests
     * since statsReset() called. Undefined if setStatsOn(false).
     */
    public int getRequestsActiveMax() {return _requestsActiveMax;}

    /* ------------------------------------------------------------ */
    /** 
     * @return Average duration of request handling in milliseconds 
     * since statsReset() called. Undefined if setStatsOn(false).
     */
    public long getRequestsDurationAve() {return _requestsDurationAve/128;}

    /* ------------------------------------------------------------ */
    /** 
     * @return Get maximum duration in milliseconds of request handling
     * since statsReset() called. Undefined if setStatsOn(false).
     */
    public long getRequestsDurationMax() {return _requestsDurationMax;}
    
    /* ------------------------------------------------------------ */
    synchronized void statsOpenConnection()
    {
        if (++_connectionsOpen > _connectionsOpenMax)
            _connectionsOpenMax=_connectionsOpen;
    }
    
    /* ------------------------------------------------------------ */
    synchronized void statsGotRequest()
    {
        if (++_requestsActive > _requestsActiveMax)
            _requestsActiveMax=_requestsActive;
    }
    
    /* ------------------------------------------------------------ */
    synchronized void statsEndRequest(long duration,boolean ok)
    {
        _requests++;
        if (!ok)
            _errors++;
        _requestsActive--;
        if (_requestsActive<0)
            _requestsActive=0;
        if (duration>_requestsDurationMax)
            _requestsDurationMax=duration;
        if (_requestsDurationAve==0)
            _requestsDurationAve=duration*128;
        _requestsDurationAve=_requestsDurationAve-_requestsDurationAve/128+duration;
    }
    
    /* ------------------------------------------------------------ */
    synchronized void statsCloseConnection(long duration,int requests)
    {
        _connections++;
        _connectionsOpen--;
        if (_connectionsOpen<0)
            _connectionsOpen=0;
        if (duration>_connectionsDurationMax)
            _connectionsDurationMax=duration;
        if (_connectionsDurationAve==0)
            _connectionsDurationAve=128*duration;
        _connectionsDurationAve=_connectionsDurationAve-_connectionsDurationAve/128+duration;
        if (requests>_connectionsRequestsMax)
            _connectionsRequestsMax=requests;
        if (_connectionsRequestsAve==0)
            _connectionsRequestsAve=16;
        _connectionsRequestsAve=_connectionsRequestsAve-_connectionsRequestsAve/16+requests;
    }
    
    /* ------------------------------------------------------------ */
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
                ("\nUsage - java org.mortbay.http.HttpServer [[<addr>:]<port> .. ]");
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
            HttpContext context = server.getContext(host,"/");
            context.setResourceBase("docroot/");
            context.setServingResources(true);
            context.addHttpHandler(new DumpHandler());
            
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


    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public class ComponentEvent extends EventObject
    {
        private Object component;
        private ComponentEvent(Object component)
        {
            super(HttpServer.this);
            this.component=component;
        }
        public Object getComponent()
        {
            return component;
        }
    }
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    public interface ComponentEventListener extends EventListener
    {
        public void addComponent(ComponentEvent event);
        public void removeComponent(ComponentEvent event);
    }
    
    /* ------------------------------------------------------------ */
    private void add(Object o)
    {
        Code.debug("add component: ",o);
        if (_components==null)
            _components=new ArrayList();
        _components.add(o);

        if (_eventListeners!=null)
        {
            ComponentEvent event = new ComponentEvent(o);
            for(int i=0;i<_eventListeners.size();i++)
            {
                EventListener listener =
                    (EventListener)_eventListeners.get(i);
                if (listener instanceof ComponentEventListener)
                    ((ComponentEventListener)listener).addComponent(event);
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    private void remove(Object o)
    {
        Code.debug("remove component: ",o);
        _components.remove(o);
        if (_eventListeners!=null)
        {
            ComponentEvent event = new ComponentEvent(o);
            for(int i=0;i<_eventListeners.size();i++)
            {
                EventListener listener =
                    (EventListener)_eventListeners.get(i);
                if (listener instanceof ComponentEventListener)
                    ((ComponentEventListener)listener).removeComponent(event);
            }
        }
    }

    /* ------------------------------------------------------------ */
    public void addEventListener(EventListener listener)
    {
        Code.debug("addEventListener: ",listener);
        if (_eventListeners==null)
            _eventListeners=new ArrayList();
        _eventListeners.add(listener);
    }
    
    /* ------------------------------------------------------------ */
    public void removeEventListener(EventListener listener)
    {
        Code.debug("removeEventListener: ",listener);
        _eventListeners.remove(listener);
    }

    /* ------------------------------------------------------------ */
    /** Destroy a stopped server.
     * Remove all components and send notifications to all event listeners.
     */
    public void destroy()
    {
        if (isStarted())
            throw new IllegalStateException("Started");
        if (_listeners!=null)
            _listeners.clear();
        _listeners=null;
        if (_hostMap!=null)
            _hostMap.clear();
        _hostMap=null;


        if (_components!=null && _eventListeners!=null)
        {
            for (int c=0;c<_components.size();c++)
            {
                Object o=_components.get(c);
                
                if (_eventListeners!=null)
                {
                    ComponentEvent event = new ComponentEvent(o);
                    for(int i=0;i<_eventListeners.size();i++)
                    {
                        EventListener listener =
                            (EventListener)_eventListeners.get(i);
                        if (listener instanceof ComponentEventListener)
                            ((ComponentEventListener)listener).removeComponent(event);
                    }
                }
            }
        }
        if (_components!=null)
            _components.clear();
        _components=null;
        
        if (_eventListeners!=null)
        {
            ComponentEvent event = new ComponentEvent(this);
            for(int i=0;i<_eventListeners.size();i++)
            {
                EventListener listener =
                    (EventListener)_eventListeners.get(i);
                if (listener instanceof ComponentEventListener)
                    ((ComponentEventListener)listener).removeComponent(event);
            }
        }
        if (_eventListeners!=null)
            _eventListeners.clear();
        _eventListeners=null;
    }
}
