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
import java.util.Arrays;
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
import java.util.WeakHashMap;
import org.mortbay.http.handler.DumpHandler;
import org.mortbay.http.handler.ResourceHandler;
import org.mortbay.http.handler.NotFoundHandler;
import org.mortbay.util.Code;
import org.mortbay.util.InetAddrPort;
import org.mortbay.util.LifeCycle;
import org.mortbay.util.Loader;
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
    private static WeakHashMap __servers = new WeakHashMap();
    private static Collection __roServers =
        Collections.unmodifiableCollection(__servers.keySet());
    private static String[] __noVirtualHost=new String[1];
    
    /* ------------------------------------------------------------ */
    /** Get HttpServer Collection.
     * Get a collection of all known HttpServers.  Servers can be
     * removed from this list with the setAnonymous call.
     * @return  Collection of all servers.
     */
    public static Collection getHttpServers()
    {
        return __roServers;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @deprecated User getHttpServers()
     */
    public static List getHttpServerList()
    {
        return new ArrayList(__roServers);
    }
    
    /* ------------------------------------------------------------ */
    private List _listeners = new ArrayList(3);
    private HttpEncoding _httpEncoding ;
    private HashMap _realmMap = new HashMap(3);    
    private StringMap _virtualHostMap = new StringMap();
    
    private HttpContext _notFoundContext=null;
    private boolean _chunkingForced=false;
    
    private RequestLog _requestLog;
    private List _eventListeners;
    private List _components;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public HttpServer()
    {
        this(false);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param anonymous If true, the server is not included in the
     * static server lists and stopAll methods.
     */
    public HttpServer(boolean anonymous)
    {
        setAnonymous(anonymous);
        _virtualHostMap.setIgnoreCase(true);
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param anonymous If true, the server is not included in the
     * static server lists and stopAll methods.
     */
    public void setAnonymous(boolean anonymous)
    {
        if (anonymous)
            __servers.remove(this);
        else
            __servers.put(this,__servers);
    }



    /* ------------------------------------------------------------ */
    /** 
     * @param listeners Array of HttpListeners.
     */
    public void setListeners(HttpListener[] listeners)
    {
        List old = new ArrayList(_listeners);
        
        for (int i=0;i<listeners.length;i++)
        {
            boolean existing=old.remove(listeners[i]);
            if (!existing)
                addListener(listeners[i]);
        }

        for (int i=0;i<old.size();i++)
        {
            HttpListener listener=(HttpListener)old.get(i);
            removeListener(listener);
        }
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return Array of HttpListeners.
     */
    public HttpListener[] getListeners()
    {
        if (_listeners==null)
            return new HttpListener[0];
        HttpListener[] listeners=new HttpListener[_listeners.size()];
        return (HttpListener[])_listeners.toArray(listeners);
    }
    
    
    /* ------------------------------------------------------------ */
    /** Create and add a SocketListener.
     * Conveniance method.
     * @param address
     * @return the HttpListener.
     * @exception IOException 
     */
    public HttpListener addListener(String address)
        throws IOException
    {
        return addListener(new InetAddrPort(address));
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
        HttpListener listener = new SocketListener(address);
        listener.setHttpServer(this);
        _listeners.add(listener);
        addComponent(listener);
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
        _listeners.add(listener);
        addComponent(listener);
    }
    
    /* ------------------------------------------------------------ */
    /** Remove a HTTP Listener.
     * @param listener 
     */
    public void removeListener(HttpListener listener)
    {
        if (listener==null)
            return;
        
        for (int l=0;l<_listeners.size();l++)
        {
            if (listener.equals(_listeners.get(l)))
            {
                _listeners.remove(l);
                removeComponent(listener);
                if (listener.isStarted())
                    try{listener.stop();}catch(InterruptedException e){Code.warning(e);}
                listener.setHttpServer(null);
            }
        }
    }

    
    /* ------------------------------------------------------------ */
    public synchronized void setContexts(HttpContext[] contexts)
    {
        List old = Arrays.asList(getContexts());
        
        for (int i=0;i<contexts.length;i++)
        {
            boolean existing=old.remove(contexts[i]);
            if (!existing)
                addContext(contexts[i]);
        }

        for (int i=0;i<old.size();i++)
            removeContext((HttpContext)old.get(i));
    }

    
    /* ------------------------------------------------------------ */
    public synchronized HttpContext[] getContexts()
    {
        if (_virtualHostMap==null)
            return new HttpContext[0];
        
        ArrayList contexts = new ArrayList(33);
        Iterator maps=_virtualHostMap.values().iterator();
        while (maps.hasNext())
        {
            PathMap pm=(PathMap)maps.next();
            Iterator lists=pm.values().iterator();
            while(lists.hasNext())
            {
                List list=(List)lists.next();
                for (int i=0;i<list.size();i++)
                {
                    HttpContext context=(HttpContext)list.get(i);
                    if (!contexts.contains(context))
                        contexts.add(context);
                }
            }
        }
        return (HttpContext[])contexts.toArray(new HttpContext[contexts.size()]);
    }



    /* ------------------------------------------------------------ */
    /** Add a context.
     * @param context 
     */
    public void addContext(HttpContext context)
    {
        if (context.getContextPath()==null ||
            context.getContextPath().length()==0)
            throw new IllegalArgumentException("No Context Path Set");
        boolean existing=removeMappings(context);
        if (!existing)
        {
            context.setHttpServer(this);
            addComponent(context);
        }
        addMappings(context);
    }


    /* ------------------------------------------------------------ */
    /** Remove a context or Web application.
     * @exception IllegalStateException if context not stopped
     */
    public boolean removeContext(HttpContext context)
        throws IllegalStateException
    {
        if (removeMappings(context))
        {
            removeComponent(context);
            if (context.isStarted())
                try{context.stop();} catch (InterruptedException e){Code.warning(e);}
            context.setHttpServer(null);
            return true;
        }
        return false;
    }
    

    /* ------------------------------------------------------------ */
    /** Add a context.
     * As contexts cannot be publicly created, this may be used to
     * alias an existing context.
     * @param virtualHost The virtual host or null for all hosts.
     * @param context 
     */
    public void addContext(String virtualHost,
                           HttpContext context)
    {
        if (virtualHost!=null)
            context.addVirtualHost(virtualHost);
        addContext(context);
    }


    /* ------------------------------------------------------------ */
    /** Create and add a new context.
     * Note that multiple contexts can be created for the same
     * virtualHost and contextPath. Requests are offered to multiple
     * contexts in the order they where added to the HttpServer.
     * @param contextPath
     * @return A HttpContext instance created by a call to newHttpContext.
     */
    public HttpContext addContext(String contextPath)
    {
        HttpContext hc = newHttpContext();
        hc.setContextPath(contextPath);
        addContext(hc);
        return hc;
    }
    
    /* ------------------------------------------------------------ */
    /** Create and add a new context.
     * Note that multiple contexts can be created for the same
     * virtualHost and contextPath. Requests are offered to multiple
     * contexts in the order they where added to the HttpServer.
     * @param virtualHost Virtual hostname or null for all hosts.
     * @param contextPathSpec Path specification relative to the context path.
     * @return A HttpContext instance created by a call to newHttpContext.
     */
    public HttpContext addContext(String virtualHost, String contextPathSpec)
    {
        if (virtualHost!=null && virtualHost.length()==0)
            virtualHost=null;
        HttpContext hc = newHttpContext();
        hc.setContextPath(contextPathSpec);
        if (virtualHost!=null)
            hc.addVirtualHost(virtualHost);
        addContext(hc);
        return hc;
    }
    
    
    /* ------------------------------------------------------------ */
    /** Get specific context. 
     * @param virtualHost The virtual host or null for all hosts.
     * @param contextPathSpec Path specification relative to the context path.
     * @param i Index among contexts of same virtualHost and pathSpec.
     * @return The HttpContext or null.
     */
    public HttpContext getContext(String virtualHost, String contextPathSpec, int i)
    {
        HttpContext hc=null;

        PathMap contextMap=(PathMap)_virtualHostMap.get(virtualHost);
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
     * @param virtualHost The virtual host or null for all hosts.
     * @param contextPath 
     * @return HttpContext. If multiple contexts exist for the same
     * virtualHost and pathSpec, the most recently added context is returned.
     * If no context exists, a new context is created by a call to newHttpContext.
     */
    public HttpContext getContext(String virtualHost, String contextPath)
    { 
        HttpContext hc=null;

        PathMap contextMap=(PathMap)_virtualHostMap.get(virtualHost);
        if (contextMap!=null)
        {
            List contextList = (List)contextMap.get(contextPath);
            if (contextList!=null && contextList.size()>0)
                hc=(HttpContext)contextList.get(contextList.size()-1);
            
        }
        if (hc==null)
            hc=addContext(virtualHost,contextPath);

        return hc;
    }
    
    /* ------------------------------------------------------------ */
    /** Get or create context. 
     * @param contextPathSpec Path specification relative to the context path.
     * @return The HttpContext  If multiple contexts exist for the same
     * pathSpec, the most recently added context is returned.
     * If no context exists, a new context is created by a call to newHttpContext.
     */
    public HttpContext getContext(String contextPathSpec)
    {
	return getContext(null,contextPathSpec);
    }    
 
    /* ------------------------------------------------------------ */
    /** Create a new HttpContext.
     * Specialized HttpServer classes may specialize this method to
     * return subclasses of HttpContext.
     * @param contextPathSpec Path specification relative to the context path. 
     * @return A new instance of HttpContext or a subclass of HttpContext
     */
    protected HttpContext newHttpContext()
    {
        return  new HttpContext();
    }

    /* ------------------------------------------------------------ */    
    void addMapping(String virtualHost, HttpContext context)
    {
        // Get the map of contexts
        PathMap contextMap=(PathMap)_virtualHostMap.get(virtualHost);
        if (contextMap==null)
        {
            contextMap=new PathMap(7);
            _virtualHostMap.put(virtualHost,contextMap);
        }
        
        // Generalize contextPath
        String contextPathSpec=context.getContextPath();
        if (contextPathSpec.length()>1)
            contextPathSpec+="/*";
        
        // Get the list of contexts at this path
        List contextList = (List)contextMap.get(contextPathSpec);
        if (contextList==null)
        {
            contextList=new ArrayList(1);
            contextMap.put(contextPathSpec,contextList);
        }
        
        // Add the context to the list
        contextList.add(context);
        
        Code.debug("Added ",context," for host ",(virtualHost==null?"*":virtualHost));
    }
    

    /* ------------------------------------------------------------ */
    void addMappings(HttpContext context)
    {
        String[] hosts=context.getVirtualHosts();
        if (hosts==null || hosts.length==0)
            hosts = __noVirtualHost;

        // For each host name
        for (int h=0;h<hosts.length;h++)
        {
            String virtualHost=hosts[h];
            addMapping(virtualHost,context);
        }
    }


    /* ------------------------------------------------------------ */
    boolean removeMapping(String virtualHost, HttpContext context)
    {
        boolean existing=false;
        if (_virtualHostMap!=null)
        {
            PathMap contextMap=(PathMap)_virtualHostMap.get(virtualHost);
            
            Iterator i2=contextMap.values().iterator();
            while(i2.hasNext())
            {
                List contextList = (List)i2.next();
                if (contextList.remove(context))
                    existing=true;                
                if (contextList.size()==0)
                    i2.remove();
            }
        }
        return existing;
    }
    
    /* ------------------------------------------------------------ */
    boolean removeMappings(HttpContext context)
    {
        boolean existing=false;
        
        if (_virtualHostMap!=null)
        {
            Iterator i1 = _virtualHostMap.keySet().iterator();
            while(i1.hasNext())
            {
                String virtualHost=(String)i1.next();
                if (removeMapping(virtualHost,context))
                    existing=true;
            }
        }
        return existing;
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

        statsReset();
        
        if (Code.verbose(99))
        {
            Code.debug("LISTENERS: ",_listeners);
            Code.debug("HANDLER: ",_virtualHostMap);
        }   

        if (_requestLog!=null && !_requestLog.isStarted())
        {
            try{
                _requestLog.start();
                Log.event("Started "+_requestLog);
            }
            catch(Exception e){mex.add(e);}
        }
        
        HttpContext[] contexts = getContexts();
        for (int i=0;i<contexts.length;i++)
        {
            HttpContext context=contexts[i];
            try{context.start();}catch(Exception e){mex.add(e);}
        }
        
        for (int l=0;l<_listeners.size();l++)
        {
            HttpListener listener =(HttpListener)_listeners.get(l);
            listener.setHttpServer(this);
            if (!listener.isStarted())
                try{listener.start();}catch(Exception e){mex.add(e);}
        }
        
        mex.ifExceptionThrowMulti();
        Log.event("Started "+this);
    }
    
    /* ------------------------------------------------------------ */
    public synchronized boolean isStarted()
    {
        for (int l=0;l<_listeners.size();l++)
        {
            HttpListener listener =(HttpListener)_listeners.get(l);
            if (listener.isStarted())
                return true;
        }
        
        return false;
    }
    
    /* ------------------------------------------------------------ */
    /** Stop all listeners then handlers.
     * Equivalent to stop(false);
     * @exception InterruptedException If interrupted, stop may not have
     * been called on everything.
     */
    public synchronized void stop()
        throws InterruptedException
    {
        stop(false);
    }
    
    /* ------------------------------------------------------------ */
    /** Stop all listeners then handlers.
     * @param graceful If true and statistics are on, then this method will wait
     * for requestsActive to go to zero before calling stop()
     */
    public synchronized void stop(boolean graceful)
        throws InterruptedException
    { 
        for (int l=0;l<_listeners.size();l++)
        {
            HttpListener listener =(HttpListener)_listeners.get(l); 
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
        
        HttpContext[] contexts = getContexts();
        for (int i=0;i<contexts.length;i++)
        {
            HttpContext context=contexts[i];
            context.stop(graceful);
        }

        if (_notFoundContext!=null)
        {
            _notFoundContext.stop();
            removeComponent(_notFoundContext);
        }
        _notFoundContext=null;
        
        if (_requestLog!=null && _requestLog.isStarted())
        {
            _requestLog.stop();
            Log.event("Stopped "+_requestLog);
        }
        
        Log.event("Stopped "+this);
    }
    
    /* ------------------------------------------------------------ */
    /** Define a virtual host alias.
     * All requests to the alias are handled the same as request for
     * the virtualHost.
     * @deprecated Use HttpContext.addVirtualHost
     * @param virtualHost Host name or IP
     * @param alias Alias hostname or IP
     */
    public void addHostAlias(String virtualHost, String alias)
    {
        Code.warning("addHostAlias is deprecated. Use HttpContext.addVirtualHost");
        Object contextMap=_virtualHostMap.get(virtualHost);
        if (contextMap==null)
            throw new IllegalArgumentException("No Such Host: "+virtualHost);
        _virtualHostMap.put(alias,contextMap);
    }

    /* ------------------------------------------------------------ */
    /** Set the request log.
     * @param logSink 
     */
    public synchronized void setRequestLog(RequestLog log)
    {
        if (isStarted())
            throw new IllegalStateException("Started");
        if (_requestLog!=null)
            removeComponent(_requestLog);
        _requestLog=log;
        if (_requestLog!=null)
            addComponent(_requestLog);
    }

    
    /* ------------------------------------------------------------ */
    public RequestLog getRequestLog()
    {
        return _requestLog;
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
        if (_requestLog!=null &&
            request!=null &&
            response!=null)
            _requestLog.log(request,response,length);
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
            PathMap contextMap=(PathMap)_virtualHostMap.get(host);
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
                                Code.debug("Try ",context,","+j);

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
                _notFoundContext.addHandler(new NotFoundHandler());
                addComponent(_notFoundContext);
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
     * @param vhosts null or a list of virtual hosts names to search
     * @return HttpHandler
     */
    public HttpHandler findHandler(Class handlerClass,
                                   String uri,
                                   String[] vhosts)
    {
        System.err.println("handlerClass="+handlerClass);
        System.err.println("uri="+uri);
        uri = URI.stripPath(uri);
        System.err.println("stripped="+uri);
        System.err.println("vhosts.length="+vhosts.length);

        if (vhosts==null || vhosts.length==0)
            vhosts=__noVirtualHost;
        
        for (int h=0; h<vhosts.length ; h++)
        {
            String host = vhosts[h];
            
            System.err.println("host="+host);
            
            PathMap contextMap=(PathMap)_virtualHostMap.get(host);
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
                        System.err.println("entry="+entry);
                        
                        String contextPath=(String)entry.getKey();
                        List contextList = (List)entry.getValue();
                
                        for (int j=0;j<contextList.size();j++)
                        {
                            HttpContext context=
                                (HttpContext)contextList.get(j);
                            System.err.println("context="+context);
                            
                            HttpHandler handler = context.getHandler(handlerClass);

                            System.err.println("handler="+handler);
                            
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
    public Map getHostMap()
    {
        return _virtualHostMap;
    }


    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private boolean _statsOn=false;
    private long _statsStartedAt=0;
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
        _statsStartedAt=System.currentTimeMillis();
        
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
     * @return Timestamp stats were started at.
     */
    public long getStatsOnMs()
    {
        return _statsOn?(System.currentTimeMillis()-_statsStartedAt):0;
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
        if (--_requestsActive<0)
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
            context.addHandler(new ResourceHandler());
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
    private void addComponent(Object o)
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
    private void removeComponent(Object o)
    {
        Code.debug("remove component: ",o);
        if (_components.remove(o) && _eventListeners!=null)
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
     * Remove all components and send notifications to all event
     * listeners. The HttpServer must be stopped before it can be destroyed.
     */
    public void destroy()
    {
        if (isStarted())
            throw new IllegalStateException("Started");
        if (_listeners!=null)
            _listeners.clear();
        _listeners=null;
        if (_virtualHostMap!=null)
            _virtualHostMap.clear();
        _virtualHostMap=null;
        _notFoundContext=null;

        if (_components!=null && _eventListeners!=null)
        {
            for (int c=0;c<_components.size();c++)
            {
                Object o=_components.get(c);
                if (o instanceof HttpContext )
                    ((HttpContext)o).destroy();
                
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
