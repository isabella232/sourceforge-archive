// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionListener;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpConnection;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpMessage;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.HttpServer;
import org.mortbay.http.PathMap;
import org.mortbay.http.UserPrincipal;
import org.mortbay.http.UserRealm;
import org.mortbay.http.Version;
import org.mortbay.http.handler.NullHandler;
import org.mortbay.http.handler.SecurityHandler;
import org.mortbay.util.Code;
import org.mortbay.util.Frame;
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
 *
 * @version $Id$
 * @author Greg Wilkins
 */
public class ServletHandler
    extends NullHandler
    implements SecurityHandler.FormAuthenticator
{
    /* ------------------------------------------------------------ */
    private static final boolean __Slosh2Slash=File.separatorChar=='\\';

    /* ------------------------------------------------------------ */
    public final static String __J_URI="org.mortbay.jetty.URI";
    public final static String __J_AUTHENTICATED="org.mortbay.jetty.Auth";
    
    /* ------------------------------------------------------------ */
    private PathMap _servletMap=new PathMap();
    private Map _nameMap=new HashMap();
    private HttpContext _httpContext;
    private Context _context;
    private ClassLoader _loader;
    private String _dynamicServletPathSpec;
    private Map _dynamicInitParams ;
    private boolean _serveDynamicSystemServlets=false;
    private boolean _usingCookies=true;
    private LogSink _logSink;
    private SessionManager _sessionManager;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public ServletHandler()
    {
        _context=new Context();
        _sessionManager = new HashSessionManager(this);
    }
    
    /* ------------------------------------------------------------ */
    public void initialize(HttpContext context)
    {
        super.initialize(context);
        if (context instanceof ServletHttpContext)
        {
            ServletHttpContext servletHttpContext= (ServletHttpContext)context;
            servletHttpContext.setServletHandler(this);
            servletHttpContext.setServletContext(_context);
        }
    }

    /* ------------------------------------------------------------ */
    public void setSessionManager(SessionManager sm)
    {
        _sessionManager=sm;
    }
    
    /* ------------------------------------------------------------ */
    public SessionManager getSessionManager()
    {
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
            _sessionManager.addEventListener(listener);
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
            _sessionManager.removeEventListener(listener);
    }
    
    /* ------------------------------------------------------------ */
    public synchronized boolean isStarted()
    {
        return super.isStarted();
    }
    
    /* ----------------------------------------------------------------- */
    public synchronized void start()
        throws Exception
    {
        _httpContext=getHttpContext();
        
        _sessionManager.start();
        
        // Initialize classloader
        _loader=getHttpContext().getClassLoader();

        // start the handler - protected by synchronization until
        // end of the call.
        super.start();

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
        // Stop servlets
        Iterator i = _servletMap.values().iterator();
        while (i.hasNext())
        {
            ServletHolder holder = (ServletHolder)i.next();
            holder.stop();
        }      
        _sessionManager.stop();
        _loader=null;
        _context=null;
        _httpContext=null;
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
        HttpServletRequest httpServletRequest = (HttpServletRequest)
            httpRequest.getFacade();
        
        if (httpServletRequest==null)
            httpServletRequest=newFacades(pathInContext,
                                          pathParams,
                                          httpRequest,
                                          httpResponse,
                                          getHolderEntry(pathInContext));
        return httpServletRequest;
    }

    /* ------------------------------------------------------------ */
    /** Get Servlet Response.
     * GetHttpServletRequest must have been called first.
     * @param httpResponse 
     * @return HttpServletRespone wrapping the passed HttpResponse.
     */
    public HttpServletResponse getHttpServletResponse(HttpResponse httpResponse)
    {
        // Look for a previously built servlet request.
        return (HttpServletResponse)httpResponse.getFacade();
    }
    
    /* ------------------------------------------------------------ */
    private ServletHttpRequest newFacades(String pathInContext,
                                          String pathParams,
                                          HttpRequest httpRequest,
                                          HttpResponse httpResponse,
                                          Map.Entry entry)
    {
        // Build the request and response.
        ServletHttpRequest servletHttpRequest  = new ServletHttpRequest(this,httpRequest);
        httpRequest.setFacade(servletHttpRequest);
        ServletHttpResponse servletHttpResponse =
            new ServletHttpResponse(servletHttpRequest,httpResponse);
        httpResponse.setFacade(servletHttpResponse);
        
        // Handle the session ID
        servletHttpRequest.setSessionId(pathParams);
        HttpSession session=servletHttpRequest.getSession(false);
        if (session!=null)
            ((SessionManager.Session)session).access();
        
        // Look for a servlet
        if (entry!=null)
        {
            String servletPathSpec=(String)entry.getKey();            
            servletHttpRequest.setPaths(PathMap.pathMatch(servletPathSpec,
                                                          pathInContext),
                                        PathMap.pathInfo(servletPathSpec,
                                                         pathInContext));
            servletHttpRequest.setServletHolder((ServletHolder)entry.getValue());
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
    void setSessionTimeout(int timeoutMinutes)
    {
        _sessionManager.setSessionTimeout(timeoutMinutes);
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
        if (!isStarted() || _servletMap.size()==0)
            return;
        
        try
        {
            ServletHttpRequest request=null;
            ServletHttpResponse response=null;
            
            // handle
            Code.debug("ServletHandler: ",pathInContext);

            // Do we already have facades?
            if (httpRequest.getFacade() instanceof ServletHttpRequest)
                request= (ServletHttpRequest) httpRequest.getFacade();
            else
            {
                // Return if no servlet match
                Map.Entry entry=getHolderEntry(pathInContext);
                if (entry==null)
                    return;

                // create the facade
                request=newFacades(pathInContext,pathParams,httpRequest,httpResponse,entry);
            }

            // Get the rest of the stuff
            response = request.getServletHttpResponse();
            ServletHolder holder = request.getServletHolder();

            // service request
            if (holder!=null)
            {
                // service request
                holder.handle(request,response);
                response.setOutputState(0);
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
            if (th instanceof IOException)
                throw (IOException)th;
            
            Code.warning("Servlet Exception for "+httpRequest.getURI(),th);
            Code.debug(httpRequest);
            
            httpResponse.getHttpConnection().forceClose();
            if (!httpResponse.isCommitted())
                httpResponse.sendError(503,th);
            else
                Code.debug("Response already committed for handling ",th);
        }
        catch(Error e)
        {
            Code.warning("Servlet Error for "+httpRequest.getURI(),e);
            Code.debug(httpRequest);
            httpResponse.getHttpConnection().forceClose();
            if (!httpResponse.isCommitted())
                httpResponse.sendError(503,e);
            else
                Code.debug("Response already committed for handling ",e);
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
                    Code.ignore(e);
                }
            }
        }
        
        return entry;
    }
    

    /* ------------------------------------------------------------ */
    /** Perform form authentication.
     * Called from SecurityHandler.
     * @return true if authenticated.
     */
    public boolean formAuthenticated(SecurityHandler shandler,
                                     String pathInContext,
                                     String pathParams,
                                     HttpRequest httpRequest,
                                     HttpResponse httpResponse)
        throws IOException
    {
        HttpServletRequest request = getHttpServletRequest(pathInContext,
                                                           pathParams,
                                                           httpRequest,
                                                           httpResponse);
        HttpServletResponse response = getHttpServletResponse(httpResponse);

        // Handle paths
        String uri = pathInContext;
        
        // Setup session 
        HttpSession session=request.getSession(true);             
        
        // Handle a request for authentication.
        if ( uri.substring(uri.lastIndexOf("/")+1).startsWith(__J_SECURITY_CHECK) )
        {
            // Check the session object for login info. 
            String username = request.getParameter(__J_USERNAME);
            String password = request.getParameter(__J_PASSWORD);
            
            UserPrincipal user =
                shandler.getUserRealm().getUser(username);
            if (user!=null && user.authenticate(password,httpRequest))
            {
                Code.debug("Form authentication OK for ",username);
                httpRequest.setAttribute(HttpRequest.__AuthType,"FORM");
                httpRequest.setAttribute(HttpRequest.__AuthUser,username);
                httpRequest.setAttribute(UserPrincipal.__ATTR,user);
                session.setAttribute(__J_AUTHENTICATED,username);
                String nuri=(String)session.getAttribute(__J_URI);
                if (nuri==null)
                    response.sendRedirect(URI.addPaths(request.getContextPath(),
                                                       shandler.getErrorPage()));
                else
                    response.sendRedirect(nuri);
            }
            else
            {
                Code.debug("Form authentication FAILED for ",username);
                response.sendRedirect(URI.addPaths(request.getContextPath(),
                                                   shandler.getErrorPage()));
            }
            
            // Security check is always false, only true after final redirection.
            return false;
        }

        // Check if the session is already authenticated.
        if (session.getAttribute(__J_AUTHENTICATED) != null)
        {
            String username=(String)session.getAttribute(__J_AUTHENTICATED);
            UserPrincipal user =
                shandler.getUserRealm().getUser(username);
            Code.debug("FORM Authenticated for ",username);
            httpRequest.setAttribute(HttpRequest.__AuthType,"FORM");
            httpRequest.setAttribute(HttpRequest.__AuthUser,username);
            httpRequest.setAttribute(UserPrincipal.__ATTR,user);
            return true;
        }
        
        // redirect to login page
        if (httpRequest.getQuery()!=null)
            uri+="?"+httpRequest.getQuery();
        session.setAttribute(__J_URI, URI.addPaths(request.getContextPath(),uri));
        response.sendRedirect(URI.addPaths(request.getContextPath(),
                                           shandler.getLoginPage()));
        return false;
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
                _httpContext.getHttpServer()
                .findHandler(org.mortbay.jetty.servlet.ServletHandler.class,
                             uri,
                             _httpContext.getHosts());
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
            return _httpContext.getMimeByExtension(file);
        }

        /* ------------------------------------------------------------ */
        public Set getResourcePaths(String uriInContext)
        {
            Resource baseResource=_httpContext.getBaseResource();
            uriInContext=Resource.canonicalPath(uriInContext);
            if (baseResource==null || uriInContext==null)
                return Collections.EMPTY_SET;

            try
            {
                Resource resource = baseResource.addPath(uriInContext);
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
            Resource baseResource=_httpContext.getBaseResource();
            uriInContext=Resource.canonicalPath(uriInContext);
            if (baseResource==null || uriInContext==null)
                return null;
        
            try{
                Resource resource = baseResource.addPath(uriInContext);
                if (resource.exists())
                    return resource.getURL();

                String aliasedUri=_httpContext.getResourceAlias(uriInContext);
                if (aliasedUri!=null)
                    return getResource(aliasedUri);
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
                URL url = getResource(uriInContext);
                if (url!=null)
                    return url.openStream();
            }
            catch(MalformedURLException e) {Code.ignore(e);}
            catch(IOException e) {Code.ignore(e);}
            return null;
        }


        /* ------------------------------------------------------------ */
        public RequestDispatcher getRequestDispatcher(String uriInContext)
        {
        
            if (uriInContext == null || !uriInContext.startsWith("/"))
                return null;

            try
            {
                String pathInContext=uriInContext;
                String query=null;
                int q=0;
                if ((q=pathInContext.indexOf('?'))>0)
                {
                    pathInContext=uriInContext.substring(0,q);
                    query=uriInContext.substring(q+1);
                }

                return new Dispatcher(ServletHandler.this,pathInContext,query);
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
        public String getRealPath(String path)
        {
            if(Code.debug())
                Code.debug("getRealPath of ",path," in ",this);

            if (__Slosh2Slash)
                path=path.replace('\\','/');
        
            Resource baseResource=_httpContext.getBaseResource();
            if (baseResource==null )
                return null;

            try{
                Resource resource = baseResource.addPath(path);
                File file = resource.getFile();

                return (file==null)
                    ?"null"
                    :(file.getAbsolutePath());
            }
            catch(IOException e)
            {
                Code.warning(e);
                return null;
            }
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
            return _httpContext.getInitParameter(param);
        }

        /* ------------------------------------------------------------ */
        /** Get context init parameter names.
         * Delegated to HttpContext.
         * @return Enumeration of names
         */
        public Enumeration getInitParameterNames()
        {
            return _httpContext.getInitParameterNames();
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
                File tempDir=(File)_httpContext
                    .getAttribute("javax.servlet.context.tempdir");
                if (tempDir==null)
                {
                    try{
                        tempDir=File.createTempFile("JettyContext",null);
                        if (tempDir.exists())
                            tempDir.delete();
                        tempDir.mkdir();
                        tempDir.deleteOnExit();
                        _httpContext
                            .setAttribute("javax.servlet.context.tempdir",
                                          tempDir);
                    }
                    catch(Exception e)
                    {
                        Code.warning(e);
                    }
                }
                Code.debug("TempDir=",tempDir);
            }

            return _httpContext.getAttribute(name);
        }

        /* ------------------------------------------------------------ */
        /** Get context attribute names.
         * Delegated to HttpContext.
         */
        public Enumeration getAttributeNames()
        {
            return _httpContext.getAttributeNames();
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
            _httpContext.setAttribute(name,value);
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
            _httpContext.removeAttribute(name);
        }
    
        /* ------------------------------------------------------------ */
        public String getServletContextName()
        {
            if (_httpContext instanceof WebApplicationContext)
                return ((WebApplicationContext)_httpContext).getDisplayName();
            return null;
        }
    }


    
}
