// ===========================================================================
// Copyright (c) 1996-2002 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.jetty.servlet;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionListener;
import org.mortbay.http.HttpConnection;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpListener;
import org.mortbay.http.HttpMessage;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.HttpServer;
import org.mortbay.http.PathMap;
import org.mortbay.http.SecurityConstraint.Authenticator;
import org.mortbay.http.UserPrincipal;
import org.mortbay.http.UserRealm;
import org.mortbay.http.Version;
import org.mortbay.http.handler.NullHandler;
import org.mortbay.http.handler.SecurityHandler;
import org.mortbay.http.handler.ResourceHandler;
import org.mortbay.util.Code;
import org.mortbay.util.Frame;
import org.mortbay.util.InetAddrPort;
import org.mortbay.util.LifeCycle;
import org.mortbay.util.Log;
import org.mortbay.util.LogSink;
import org.mortbay.util.MultiException;
import org.mortbay.util.Resource;
import org.mortbay.util.URI;


/* --------------------------------------------------------------------- */
/** Servlet HttpHandler.
 * This handler maps requests to servlets that implement the
 * javax.servlet.http.HttpServlet API.
 * <P>
 * This handler does not implement the full J2EE features and is intended to
 * be used when a full web application is not required.  Specifically filters
 * and request wrapping are not supported.
 * <P>
 * If a SessionManager is not added to the handler before it is
 * initialized, then a HashSessionManager with a standard
 * java.util.Random generator is created.
 * <P>
 * @see org.mortbay.jetty.servlet.WebApplicationHandler
 * @version $Id$
 * @author Greg Wilkins
 */
public class ServletHandler
    extends NullHandler
{
    /* ------------------------------------------------------------ */
    private static final boolean __Slosh2Slash=File.separatorChar=='\\';

    /* ------------------------------------------------------------ */
    private PathMap _servletMap=new PathMap();
    private Map _nameMap=new HashMap();
    private Context _context;
    private ClassLoader _loader;
    private String _dynamicServletPathSpec;
    private Map _dynamicInitParams ;
    private boolean _serveDynamicSystemServlets=false;
    private boolean _usingCookies=true;
    private LogSink _logSink;
    private SessionManager _sessionManager;
    private boolean _autoInitializeServlets=true;
    private String _formLoginPage;
    private String _formErrorPage;
    private ResourceHandler _resourceHandler;
    private List _sessionListeners=new ArrayList();

    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public ServletHandler()
    {
        _context=new Context();
    }
    
    /* ------------------------------------------------------------ */
    public void initialize(HttpContext context)
    {
        SessionManager sessionManager=getSessionManager();
        super.initialize(context);
        if (context instanceof ServletHttpContext)
        {
            ServletHttpContext servletHttpContext= (ServletHttpContext)context;
            servletHttpContext.setServletHandler(this);
            servletHttpContext.setServletContext(_context);
        }
        sessionManager.initialize(this);
    }

    /* ------------------------------------------------------------ */
    public void formAuthInit(String formLoginPage,
                             String formErrorPage)
    {
        _formLoginPage=formLoginPage;
        _formErrorPage=formErrorPage;
    }
    
    
    /* ------------------------------------------------------------ */
    public void setSessionManager(SessionManager sm)
    {
        if (isStarted())
            throw new IllegalStateException("Started");

        if (getHttpContext()!=null && _sessionManager!=null)
	{
	  _sessionManager.initialize(null);
	  for (Iterator i=_sessionListeners.iterator();i.hasNext();)
	  {
	    EventListener listener=(EventListener)i.next();
	    _sessionManager.removeEventListener(listener);
	  }
	}

        _sessionManager=sm;

        if (getHttpContext()!=null)
	{
	  for (Iterator i=_sessionListeners.iterator();i.hasNext();)
	    {
	      EventListener listener=(EventListener)i.next();
	      _sessionManager.addEventListener(listener);
	    }
	  _sessionManager.initialize(this);
	}
    }
    
    /* ------------------------------------------------------------ */
    public SessionManager getSessionManager()
    {
        if (_sessionManager==null)
            _sessionManager = new HashSessionManager();
        return _sessionManager;
    }
    
    /* ------------------------------------------------------------ */
    public ServletContext getServletContext() { return _context; }

    /* ------------------------------------------------------------ */
    public PathMap getServletMap() { return _servletMap; }
    
    /* ------------------------------------------------------------ */
    public boolean isAutoReload() { return false; }
    
    /* ------------------------------------------------------------ */
    public String getDynamicServletPathSpec() { return _dynamicServletPathSpec; }

    /* ------------------------------------------------------------ */
    public Map getDynamicInitParams() { return _dynamicInitParams; }

    /* ------------------------------------------------------------ */
    public boolean isUsingCookies() { return _usingCookies; }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return True if dynamic servlets can be on the non-context classpath
     */
    public boolean getServeDynamicSystemServlets()
    { return _serveDynamicSystemServlets; }
    
    /* ------------------------------------------------------------ */
    /** Set the dynamic servlet path.
     * If set, the ServletHandler will dynamically load servlet
     * classes that have their class names as the path info after the
     * set path sepcification.
     * @param dynamicServletPathSpec The path within the context at which
     * dynamic servlets are launched. eg /servlet/*
     */
    public void setDynamicServletPathSpec(String dynamicServletPathSpec)
    {
        if (dynamicServletPathSpec!=null &&
            !dynamicServletPathSpec.equals("/") &&
            !dynamicServletPathSpec.endsWith("/*"))
            throw new IllegalArgumentException("dynamicServletPathSpec must end with /*");
            
        _dynamicServletPathSpec=dynamicServletPathSpec;
    }
    
    /* ------------------------------------------------------------ */
    /** Set dynamic servlet initial parameters.
     * @param initParams Map passed as initParams to newly created
     * dynamic servlets.
     */
    public void setDynamicInitParams(Map initParams)
    {
        _dynamicInitParams = initParams;
    }

    /* ------------------------------------------------------------ */
    /** Set serving dynamic system servlets.
     * This is a security option so that you can control what servlets
     * can be loaded with dynamic discovery.
     * @param b If set to false, the dynamic servlets must be loaded
     * by the context classloader.  
     */
    public void setServeDynamicSystemServlets(boolean b)
    {
        _serveDynamicSystemServlets=b;
    }
    
    /* ------------------------------------------------------------ */
    public ClassLoader getClassLoader()
    {
        return _loader;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param sc If true, cookies are used for sessions
     */
    public void setUsingCookies(boolean uc)
    {
        _usingCookies=uc;
    }

    /* ------------------------------------------------------------ */
    public void setLogSink(LogSink logSink)
    {
        _logSink=logSink;
    }
    
    /* ------------------------------------------------------------ */
    public LogSink getLogSink()
    {
        return _logSink;
    }

    /* ------------------------------------------------------------ */
    public ServletHolder newServletHolder(String name,
                                          String servletClass,
                                          String forcedPath)
    {
        if (_nameMap.containsKey(name))
            throw new IllegalArgumentException("Named servlet already exists: "+name);
        
        ServletHolder holder = new ServletHolder(this,name,servletClass,forcedPath);
        _nameMap.put(holder.getName(),holder);
        return holder;
    }

    /* ------------------------------------------------------------ */
    public ServletHolder newServletHolder(String name,
                                          String servletClass)
    {
        return newServletHolder(name,servletClass,null);
    }    
    
    /* ------------------------------------------------------------ */
    public ServletHolder getServletHolder(String name)
    {
        return (ServletHolder)_nameMap.get(name);
    }    

    /* ------------------------------------------------------------ */
    public ServletHolder mapPathToServlet(String pathSpec,
                                          String servletName)
    {
        ServletHolder holder =(ServletHolder)_nameMap.get(servletName);

        if (!pathSpec.startsWith("/") && !pathSpec.startsWith("*"))
        {
            Code.warning("pathSpec must start with '/' or '*' : "+pathSpec);
            pathSpec="/"+pathSpec;
        }
        
        if (holder==null)
            throw new IllegalArgumentException("Unknown servlet: "+servletName);
        _servletMap.put(pathSpec,holder);
        return holder;
    }
    
    /* ------------------------------------------------------------ */
    public ServletHolder addServlet(String name,
                                    String pathSpec,
                                    String servletClass,
                                    String forcedPath)
    {
        ServletHolder holder = getServletHolder(name);
        if (holder==null)
            holder = newServletHolder(name,servletClass,forcedPath);
        mapPathToServlet(pathSpec,name);
        return holder;
    }
    
    /* ------------------------------------------------------------ */
    public ServletHolder addServlet(String name,
                                    String pathSpec,
                                    String servletClass)
    {
        return addServlet(name,pathSpec,servletClass,null);
    }

    
    /* ------------------------------------------------------------ */
    public ServletHolder addServlet(String pathSpec,
                                    String servletClass)
    {
        return addServlet(servletClass,pathSpec,servletClass,null);
    }

    /* ------------------------------------------------------------ */
    void addServletHolder(String pathSpec, ServletHolder holder)
    {
        try
        {
            ServletHolder existing = (ServletHolder)
                _nameMap.get(holder.getName());
            if (existing==null)
                _nameMap.put(holder.getName(),holder);
            else if (existing!=holder)
                throw new IllegalArgumentException("Holder already exists for name: "+holder.getName());
            
            if (isStarted() && !holder.isStarted())
                holder.start();
            _servletMap.put(pathSpec,holder);
        }
        catch(Exception e)
        {
            Code.warning(e);
        }
    }
    
    /* ------------------------------------------------------------ */
    public synchronized void addEventListener(EventListener listener)
        throws IllegalArgumentException
    {
        if ((listener instanceof HttpSessionActivationListener) ||
            (listener instanceof HttpSessionAttributeListener) ||
            (listener instanceof HttpSessionBindingListener) ||
            (listener instanceof HttpSessionListener))
	{
	  _sessionManager.addEventListener(listener);
	  _sessionListeners.add(listener);
	}
        else 
            throw new IllegalArgumentException(listener.toString());
    }
    
    /* ------------------------------------------------------------ */
    public synchronized void removeEventListener(EventListener listener)
    {
        if ((listener instanceof HttpSessionActivationListener) ||
            (listener instanceof HttpSessionAttributeListener) ||
            (listener instanceof HttpSessionBindingListener) ||
            (listener instanceof HttpSessionListener))
          {
	    _sessionManager.removeEventListener(listener);
	    _sessionListeners.remove(_sessionListeners.indexOf(listener));
	  }
    }
    
    /* ------------------------------------------------------------ */
    public synchronized boolean isStarted()
    {
        return super.isStarted();
    }

    /* ------------------------------------------------------------ */
    public boolean isAutoInitializeServlets()
    {
        return _autoInitializeServlets;
    }

    /* ------------------------------------------------------------ */
    public void setAutoInitializeServlets(boolean b)
    {
        _autoInitializeServlets=b;
    }
    
    /* ----------------------------------------------------------------- */
    public synchronized void start()
        throws Exception
    {
        if (isStarted())
            return;
        
        _resourceHandler=(ResourceHandler)getHttpContext().getHandler(ResourceHandler.class);
        if (_sessionManager!=null)
            _sessionManager.start();
        
        // Initialize classloader
        _loader=getHttpContext().getClassLoader();

        // start the handler - protected by synchronization until
        // end of the call.
        super.start();

        if (_autoInitializeServlets)
            initializeServlets();
    }   
    
    /* ------------------------------------------------------------ */
    /** Initialize load-on-startup servlets.
     * Called automatically from start if autoInitializeServlet is true.
     */
    public void initializeServlets()
        throws Exception
    {
        MultiException mx = new MultiException();
        // Sort and Initialize servlets
        ServletHolder holders [] = (ServletHolder [])
            (new HashSet(_servletMap.values ())).toArray(new ServletHolder [0]);
        java.util.Arrays.sort (holders);        
        for (int i=0; i<holders.length; i++)
        {
            ServletHolder holder = holders [i];
            
            try{holder.start();}
            catch(Exception e)
            {
                Code.debug(e);
                mx.add(e);
            }
        } 
        mx.ifExceptionThrow();       
    }
    
    /* ----------------------------------------------------------------- */
    public synchronized void stop()
        throws InterruptedException
    {
        // Stop the session manager
        _sessionManager.stop();
        
        // Stop servlets
        Iterator i = _servletMap.values().iterator();
        while (i.hasNext())
        {
            ServletHolder holder = (ServletHolder)i.next();
            holder.stop();
        }      
        _loader=null;
        _context=null;
        super.stop();
    }

    /* ------------------------------------------------------------ */
    /** Get or create a ServletRequest.
     * Create a new or retrieve a previously created servlet request
     * that wraps the http request.
     * @param httpRequest 
     * @return HttpServletRequest wrapping the passed HttpRequest.
     */
    public HttpServletRequest getHttpServletRequest(String pathInContext,
                                                    String pathParams,
                                                    HttpRequest httpRequest,
                                                    HttpResponse httpResponse)
    {
        // Look for a previously built servlet request.
        ServletHttpRequest servletHttpRequest =
            (ServletHttpRequest)httpRequest.getWrapper();
        
        if (servletHttpRequest==null)
            servletHttpRequest=newWrappers(pathInContext,
                                           pathParams,
                                           httpRequest,
                                           httpResponse,
                                           getHolderEntry(pathInContext));
        
        return servletHttpRequest; 
    }

    /* ------------------------------------------------------------ */
    /** Get Servlet Response.
     * GetHttpServletRequest must have been called first.
     * @param httpResponse 
     * @return HttpServletRespone wrapping the passed HttpResponse.
     */
    public HttpServletResponse getHttpServletResponse(HttpResponse httpResponse)
    {
        return (HttpServletResponse) httpResponse.getWrapper();
    }
    
    /* ------------------------------------------------------------ */
    private ServletHttpRequest newWrappers(String pathInContext,
                                           String pathParams,
                                           HttpRequest httpRequest,
                                           HttpResponse httpResponse,
                                           Map.Entry entry)
    {
        // Build the request and response.
        ServletHttpRequest servletHttpRequest =
            new ServletHttpRequest(this,pathInContext,httpRequest);
        httpRequest.setWrapper(servletHttpRequest);
        ServletHttpResponse servletHttpResponse =
            new ServletHttpResponse(servletHttpRequest,httpResponse);
        httpResponse.setWrapper(servletHttpResponse);
        
        // Handle the session ID
        servletHttpRequest.setSessionId(pathParams);
        HttpSession session=servletHttpRequest.getSession(false);
        if (session!=null)
            ((SessionManager.Session)session).access();
        
        // Look for a servlet
        if (entry!=null)
        {
            String servletPathSpec=(String)entry.getKey();            
            servletHttpRequest.setServletPaths(PathMap.pathMatch(servletPathSpec,
                                                                 pathInContext),
                                               PathMap.pathInfo(servletPathSpec,
                                                                pathInContext),
                                               (ServletHolder)entry.getValue());
        }
        Code.debug("Servlet request for ",entry);
        return servletHttpRequest;
    }

    /* ------------------------------------------------------------ */
    HttpSession getHttpSession(String id)
    {
        return _sessionManager.getHttpSession(id);
    }
    
    /* ------------------------------------------------------------ */
    HttpSession newHttpSession()
    {
        return _sessionManager.newHttpSession();
    }

    /* ------------------------------------------------------------ */
    void setSessionInactiveInterval(int seconds)
    {
        _sessionManager.setMaxInactiveInterval(seconds);
    }

    /* ----------------------------------------------------------------- */
    /** Handle request.
     * @param contextPath 
     * @param pathInContext 
     * @param httpRequest 
     * @param httpResponse 
     * @exception IOException 
     */
    public void handle(String pathInContext,
                       String pathParams,
                       HttpRequest httpRequest,
                       HttpResponse httpResponse)
         throws IOException
    {
        if (!isStarted())
            return;
        
        try
        {
            ServletHttpRequest request=null;
            ServletHttpResponse response=null;
            ServletHolder holder=null;
            
            // handling
            Code.debug("ServletHandler: ",pathInContext);

            Map.Entry entry=getHolderEntry(pathInContext);
            if (entry==null)
                return;
                
            request=newWrappers(pathInContext,
                                pathParams,
                                httpRequest,
                                httpResponse,
                                entry);
            holder = request.getServletHolder();
            response = request.getServletHttpResponse();

            // service request
            if (holder!=null)
            {
                //service request
                holder.handle(request,response);
                response.flushBuffer();
                
                // reset output
                response.setOutputState(ServletHttpResponse.NO_OUT);
                Code.debug("Handled by ",holder);
                if (!httpResponse.isCommitted())
                    httpResponse.commit();
            }
        }
        catch(Exception e)
        {
            Code.debug(e);
            
            Throwable th=e;
            if (e instanceof ServletException)
            {
                if (((ServletException)e).getRootCause()!=null)
                {
                    Code.debug("Extracting root cause from ",e);
                    th=((ServletException)e).getRootCause();
                }
            }
            
            if (th instanceof HttpException)
                throw (HttpException)th;
            if (th.getClass().equals(IOException.class))
                throw (IOException)th;
            
            Code.warning("Servlet Exception for "+httpRequest.getURI(),th);
            Code.debug(httpRequest);
            
            httpResponse.getHttpConnection().forceClose();
            if (!httpResponse.isCommitted())
            {
                httpRequest.setAttribute("javax.servlet.error.exception_type",th.getClass());
                httpRequest.setAttribute("javax.servlet.error.exception",th);
                httpResponse.sendError(th instanceof UnavailableException
                                       ?HttpResponse.__503_Service_Unavailable
                                       :HttpResponse.__500_Internal_Server_Error);
            }
            else
                Code.debug("Response already committed for handling ",th);
        }
        catch(Error e)
        {
            Code.warning("Servlet Error for "+httpRequest.getURI(),e);
            Code.debug(httpRequest);
            httpResponse.getHttpConnection().forceClose();
            if (!httpResponse.isCommitted())
            {
                httpRequest.setAttribute("javax.servlet.error.exception_type",e.getClass());
                httpRequest.setAttribute("javax.servlet.error.exception",e);
                httpResponse.sendError(HttpResponse.__500_Internal_Server_Error);
            }
            else
                Code.debug("Response already committed for handling ",e);
        }
        finally
        {
            httpRequest.setWrapper(null);
            httpResponse.setWrapper(null);
        }
    }

    
    /* ------------------------------------------------------------ */
    /** ServletHolder matching path.
     * @param pathInContext Path within context.
     * @return PathMap Entries pathspec to ServletHolder
     */
    public Map.Entry getHolderEntry(String pathInContext)
    {
        Map.Entry entry =_servletMap.getMatch(pathInContext);

        String servletClass=null;
        if (_dynamicServletPathSpec!=null)
            servletClass=PathMap.pathInfo(_dynamicServletPathSpec,pathInContext);
        
        // Do we have a match and no chance of a new
        // dynamci servlet
        if (entry!=null && servletClass==null)
            return entry;

        // If it could be a dynamic servlet
        synchronized(this)
        {
            // sychronize and try again.
            entry =_servletMap.getMatch(pathInContext);
            if (entry!=null && servletClass==null)
                return entry;
            
            if (servletClass!=null && servletClass.length()>2 &&
                (entry==null||!PathMap.match(_dynamicServletPathSpec,(String)entry.getKey())))
            {
                try
                {
                    // OK lets look for a dynamic servlet.
                    String path=pathInContext;
                    Code.debug("looking for ",servletClass," in ",
                               getHttpContext().getClassPath());
                
                    // remove prefix
                    servletClass=servletClass.substring(1);
                
                    // remove suffix
                    int slash=servletClass.indexOf('/');
                    if (slash>=0)
                        servletClass=servletClass.substring(0,slash);            
                    if (servletClass.endsWith(".class"))
                        servletClass=servletClass.substring(0,servletClass.length()-6);
                
                    // work out the actual servlet path
                    if ("/".equals(_dynamicServletPathSpec))
                        path='/'+servletClass;
                    else
                        path=PathMap.pathMatch(_dynamicServletPathSpec,path)+'/'+servletClass;
                
                    Code.debug("Dynamic path=",path);

                    if (servletClass==null || servletClass.length()==0)
                        return null;
                    
                    // make a holder
                    ServletHolder holder=new ServletHolder(this,servletClass,servletClass);
                    
                    // Set params
                    Map params=getDynamicInitParams();
                    if (params!=null)
                        holder.putAll(params);
                    holder.start();
                    Object servlet=holder.getServlet();

                    // Check that the class was intended as a dynamic
                    // servlet
                    if (!_serveDynamicSystemServlets &&
                        _loader!=null &&
                        _loader!=this.getClass().getClassLoader())
                    {
                        // This context has a specific class loader.
                        if (servlet.getClass().getClassLoader()!=_loader)
                        {
                            holder.stop();
                            String msg = "Dynamic servlet "+
                                servletClass+
                                " is not loaded from context: "+
                                getHttpContext().getContextPath();
                        
                            Code.warning(msg);
                            throw new UnavailableException(msg);
                        }
                    }
                
                    Log.event("Dynamic load '"+servletClass+"' at "+path);
                    addServletHolder(path+"/*",holder);
                    addServletHolder(path+".class/*",holder);
                    
                    entry=_servletMap.getMatch(pathInContext);
                }
                catch(Exception e)
                {
                    Code.warning(e);
                }
            }
        }
        
        return entry;
    }
    

    /* ------------------------------------------------------------ */
    public Set getResourcePaths(String uriInContext)
    {
        try
        {
            uriInContext=URI.canonicalPath(uriInContext);
            if (uriInContext==null)
                return Collections.EMPTY_SET;
            Resource resource=getHttpContext().getResource(uriInContext);
            if (resource==null || !resource.isDirectory())
                return Collections.EMPTY_SET;
            String[] contents=resource.list();
            if (contents==null || contents.length==0)
                return Collections.EMPTY_SET;
            HashSet set = new HashSet(contents.length*2);
            for (int i=0;i<contents.length;i++)
                set.add(URI.addPaths(uriInContext,contents[i]));
            return set;
        }
        catch(Exception e)
        {
            Code.ignore(e);
        }
        
        return Collections.EMPTY_SET;
    }
    

    /* ------------------------------------------------------------ */
    /** Get a Resource.
     * If no resource is found, resource aliases are tried.
     * @param uriInContext 
     * @return 
     * @exception MalformedURLException 
     */
    public URL getResource(String uriInContext)
        throws MalformedURLException
    {        
        try{
            Resource resource = getHttpContext().getResource(uriInContext);
            if (resource!=null && resource.exists())
                return resource.getURL();
        }
        catch(IllegalArgumentException e)
        {
            Code.ignore(e);
        }
        catch(MalformedURLException e)
        {
            throw e;
        }
        catch(IOException e)
        {
            Code.warning(e);
        }
        return null;
    }

    /* ------------------------------------------------------------ */
    public InputStream getResourceAsStream(String uriInContext)
    {
        try
        {
            uriInContext=URI.canonicalPath(uriInContext);
            URL url = getResource(uriInContext);
            if (url!=null)
                return url.openStream();
        }
        catch(MalformedURLException e) {Code.ignore(e);}
        catch(IOException e) {Code.ignore(e);}
        return null;
    }

    /* ------------------------------------------------------------ */
    public String getRealPath(String path)
    {
        if(Code.debug())
            Code.debug("getRealPath of ",path," in ",this);

        if (__Slosh2Slash)
            path=path.replace('\\','/');
        
        Resource baseResource=getHttpContext().getBaseResource();
        if (baseResource==null )
            return null;

        try{
            Resource resource = baseResource.addPath(path);
            File file = resource.getFile();

            return (file==null)?null:(file.getAbsolutePath());
        }
        catch(IOException e)
        {
            Code.warning(e);
            return null;
        }
    }

    /* ------------------------------------------------------------ */
    public RequestDispatcher getRequestDispatcher(String uriInContext)
    {
        if (uriInContext == null)
            return null;

        if (!uriInContext.startsWith("/"))
            uriInContext="/"+uriInContext;
        
        try
        {
            String query=null;
            int q=0;
            if ((q=uriInContext.indexOf('?'))>0)
            {
                query=uriInContext.substring(q+1);
                uriInContext=uriInContext.substring(0,q);
            }

            return new Dispatcher(ServletHandler.this,
                                  _resourceHandler,
                                  uriInContext,query);
        }
        catch(Exception e)
        {
            Code.ignore(e);
            return null;
        }
    }

    /* ------------------------------------------------------------ */
    public RequestDispatcher getNamedDispatcher(String name)
    {
        if (name == null || name.length()==0)
            return null;

        try { return new Dispatcher(ServletHandler.this,name); }
        catch(Exception e) {Code.ignore(e);}
        
        return null;
    }
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class Context implements ServletContext
    {
        /* ------------------------------------------------------------ */
        public ServletContext getContext(String uri)
        {        
            ServletHandler handler= (ServletHandler)
                getHttpContext().getHttpServer()
                .findHandler(org.mortbay.jetty.servlet.ServletHandler.class,
                             uri,
                             getHttpContext().getVirtualHosts());
            if (handler!=null)
                return handler.getServletContext();
            return null;
        }

        /* ------------------------------------------------------------ */
        public int getMajorVersion()
        {
            return 2;
        }

        /* ------------------------------------------------------------ */
        public int getMinorVersion()
        {
            return 3;
        }

        /* ------------------------------------------------------------ */
        public String getMimeType(String file)
        {
            return getHttpContext().getMimeByExtension(file);
        }

        /* ------------------------------------------------------------ */
        public Set getResourcePaths(String uriInContext)
        {
            return ServletHandler.this.getResourcePaths(uriInContext);
        }

        /* ------------------------------------------------------------ */
        public URL getResource(String uriInContext)
            throws MalformedURLException
        {
            return ServletHandler.this.getResource(uriInContext);
        }

        /* ------------------------------------------------------------ */
        public InputStream getResourceAsStream(String uriInContext)
        {
            return ServletHandler.this.getResourceAsStream(uriInContext);
        }

        /* ------------------------------------------------------------ */
        public String getRealPath(String path)
        {
            return ServletHandler.this.getRealPath(path);
        }

        /* ------------------------------------------------------------ */
        public RequestDispatcher getRequestDispatcher(String uriInContext)
        {
            return ServletHandler.this.getRequestDispatcher(uriInContext);
        }

        /* ------------------------------------------------------------ */
        public RequestDispatcher getNamedDispatcher(String name)
        {
            return ServletHandler.this.getNamedDispatcher(name);
        }
    
        /* ------------------------------------------------------------ */
        /**
         * @deprecated 
         */
        public Servlet getServlet(String name)
        {
            return null;
        }

        /* ------------------------------------------------------------ */
        /**
         * @deprecated 
         */
        public Enumeration getServlets()
        {
            return Collections.enumeration(Collections.EMPTY_LIST);
        }

        /* ------------------------------------------------------------ */
        /**
         * @deprecated 
         */
        public Enumeration getServletNames()
        {
            return Collections.enumeration(Collections.EMPTY_LIST);
        }


    
        /* ------------------------------------------------------------ */
        /** Servlet Log.
         * Log message to servlet log. Use either the system log or a
         * LogSinkset via the context attribute
         * org.mortbay.jetty.servlet.Context.LogSink
         * @param msg 
         */
        public void log(String msg)
        {
            if (_logSink!=null)
                _logSink.log(Log.EVENT,msg,new
                             Frame(2),System.currentTimeMillis());
            else
                Log.message(Log.EVENT,msg,new Frame(2));
        }

        /* ------------------------------------------------------------ */
        public void log(Exception e, String msg)
        {
            Code.warning(msg,e);
            log(msg+": "+e.toString());
        }

        /* ------------------------------------------------------------ */
        public void log(String msg, Throwable th)
        {
            Code.warning(msg,th);
            log(msg+": "+th.toString());
        }

        /* ------------------------------------------------------------ */
        public String getServerInfo()
        {
            return Version.__Version;
        }


        /* ------------------------------------------------------------ */
        /** Get context init parameter.
         * Delegated to HttpContext.
         * @param param param name
         * @return param value or null
         */
        public String getInitParameter(String param)
        {
            return getHttpContext().getInitParameter(param);
        }

        /* ------------------------------------------------------------ */
        /** Get context init parameter names.
         * Delegated to HttpContext.
         * @return Enumeration of names
         */
        public Enumeration getInitParameterNames()
        {
            return getHttpContext().getInitParameterNames();
        }

    
        /* ------------------------------------------------------------ */
        /** Get context attribute.
         * Delegated to HttpContext.
         * @param name attribute name.
         * @return attribute
         */
        public Object getAttribute(String name)
        {
            if ("javax.servlet.context.tempdir".equals(name))
            {
                // Initialize temporary directory
                Object t = getHttpContext().getAttribute("javax.servlet.context.tempdir");

                if (t instanceof File)
                    return (File)t;
                
                return getHttpContext().getTempDirectory();
            }

            return getHttpContext().getAttribute(name);
        }

        /* ------------------------------------------------------------ */
        /** Get context attribute names.
         * Delegated to HttpContext.
         */
        public Enumeration getAttributeNames()
        {
            return getHttpContext().getAttributeNames();
        }

        /* ------------------------------------------------------------ */
        /** Set context attribute names.
         * Delegated to HttpContext.
         * @param name attribute name.
         * @param value attribute value
         */
        public void setAttribute(String name, Object value)
        {
            if (name.startsWith("org.mortbay.http"))
            {
                Code.warning("Servlet attempted update of "+name);
                return;
            }
            getHttpContext().setAttribute(name,value);
        }

        /* ------------------------------------------------------------ */
        /** Remove context attribute.
         * Delegated to HttpContext.
         * @param name attribute name.
         */
        public void removeAttribute(String name)
        {
            if (name.startsWith("org.mortbay.http"))
            {
                Code.warning("Servlet attempted update of "+name);
                return;
            }
            getHttpContext().removeAttribute(name);
        }
    
        /* ------------------------------------------------------------ */
        public String getServletContextName()
        {
            if (getHttpContext() instanceof WebApplicationContext)
                return ((WebApplicationContext)getHttpContext()).getDisplayName();
            return null;
        }
    }
    
}
