// ===========================================================================
// Copyright (c) 1996-2003 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.jetty.servlet;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.http.EOFException;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpFields;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.PathMap;
import org.mortbay.http.Version;
import org.mortbay.http.handler.AbstractHttpHandler;
import org.mortbay.util.ByteArrayISO8859Writer;
import org.mortbay.util.LogSupport;
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
public class ServletHandler extends AbstractHttpHandler
{
    private static Log log = LogFactory.getLog(ServletHandler.class);

    /* ------------------------------------------------------------ */
    public static final String __DEFAULT_SERVLET="default";
    public static final String __J_S_CONTEXT_TEMPDIR="javax.servlet.context.tempdir";
    public static final String __J_S_ERROR_EXCEPTION="javax.servlet.error.exception";
    public static final String __J_S_ERROR_EXCEPTION_TYPE="javax.servlet.error.exception_type";
    public static final String __J_S_ERROR_MESSAGE="javax.servlet.error.message";
    public static final String __J_S_ERROR_REQUEST_URI="javax.servlet.error.request_uri";
    public static final String __J_S_ERROR_SERVLET_NAME="javax.servlet.error.servlet_name";
    public static final String __J_S_ERROR_STATUS_CODE="javax.servlet.error.status_code";
    
    /* ------------------------------------------------------------ */
    private static final boolean __Slosh2Slash=File.separatorChar=='\\';
    private static String __AllowString="GET, HEAD, POST, OPTIONS, TRACE";

    
    /* ------------------------------------------------------------ */
    private boolean _usingCookies=true;
    private boolean _autoInitializeServlets=true;
    
    /* ------------------------------------------------------------ */
    protected PathMap _servletMap=new PathMap();
    protected Map _nameMap=new HashMap();
    protected String _formLoginPage;
    protected String _formErrorPage;
    protected SessionManager _sessionManager;

    protected transient Context _context;
    protected transient ClassLoader _loader;

    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public ServletHandler()
    {}
    
    /* ------------------------------------------------------------ */
    public void initialize(HttpContext context)
    {
        SessionManager sessionManager=getSessionManager();
        super.initialize(context);
        _context=new Context();
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

        _sessionManager=sm;
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
    public boolean isUsingCookies() { return _usingCookies; }
    
    /* ------------------------------------------------------------ */
    /** Set the dynamic servlet path.
     * @deprecated Use org.mortbay.jetty.servlet.Invoker
     */
    public void setDynamicServletPathSpec(String dynamicServletPathSpec)
    {
        log.warn("setDynamicServletPathSpec is Deprecated.");
    }
    
    /* ------------------------------------------------------------ */
    /** Set dynamic servlet initial parameters.
     * @deprecated Use org.mortbay.jetty.servlet.Invoker
     */
    public void setDynamicInitParams(Map initParams)
    {
        log.warn("setDynamicInitParams is Deprecated.");
    }

    /* ------------------------------------------------------------ */
    /** Set serving dynamic system servlets.
     * @deprecated Use org.mortbay.jetty.servlet.Invoker
     */
    public void setServeDynamicSystemServlets(boolean b)
    {
        log.warn("setServeDynamicSystemServlets is Deprecated.");
    }
    
    /* ------------------------------------------------------------ */
    public ClassLoader getClassLoader()
    {
        return _loader;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param uc If true, cookies are used for sessions
     */
    public void setUsingCookies(boolean uc)
    {
        _usingCookies=uc;
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
            log.warn("pathSpec should start with '/' or '*' : "+pathSpec);
            pathSpec="/"+pathSpec;
        }
        
        if (holder==null)
            throw new IllegalArgumentException("Unknown servlet: "+servletName);
        _servletMap.put(pathSpec,holder);
        return holder;
    }
    
    /* ------------------------------------------------------------ */
    /** Add a servlet.
     * @param name The servlet name.
     * @param pathSpec A path specification to map this servlet to.
     * @param servletClass The class name of the servlet.
     * @param forcedPath If non null, the request attribute
     * javax.servlet.include.servlet_path will be set to this path before
     * service is called.
     * @return The ServletHolder for the servlet.
     */
    public ServletHolder addServlet(String name,
                                    String pathSpec,
                                    String servletClass,
                                    String forcedPath)
    {
        ServletHolder holder = getServletHolder(name);
        if (holder==null)
            holder = newServletHolder(name,servletClass,forcedPath);
        mapPathToServlet(pathSpec,name);
        if (isStarted() && !holder.isStarted())
        {
            try{holder.start();}
            catch(Exception e){log.warn(LogSupport.EXCEPTION,e);}
        }
        return holder;
    }
    
    /* ------------------------------------------------------------ */
    /** Add a servlet.
     * @param name The servlet name.
     * @param pathSpec A path specification to map this servlet to.
     * @param servletClass The class name of the servlet.
     * @return The ServletHolder for the servlet.
     */
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
            log.warn(LogSupport.EXCEPTION,e);
        }
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
    /** Get Servlets.
     * @return Array of defined servlets
     */
    public ServletHolder[] getServlets()
    {
        // Sort and Initialize servlets
        HashSet holder_set = new HashSet(_nameMap.size());
        holder_set.addAll(_nameMap.values());
        ServletHolder holders [] = (ServletHolder [])
            holder_set.toArray(new ServletHolder [holder_set.size()]);
        java.util.Arrays.sort (holders);
        return holders;
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
        ServletHolder[] holders = getServlets();
        for (int i=0; i<holders.length; i++)
        {
            try{holders[i].start();}
            catch(Exception e)
            {
                log.debug(LogSupport.EXCEPTION,e);
                mx.add(e);
            }
        } 
        mx.ifExceptionThrow();       
    }
    
    /* ----------------------------------------------------------------- */
    public synchronized void stop()
        throws InterruptedException
    {
        // Sort and Initialize servlets
        ServletHolder[] holders = getServlets();
        
        super.stop();
        
        // Stop servlets
        for (int i=holders.length; i-->0;)
        {
            try
            {
                if (holders[i].isStarted())
                    holders[i].stop();
            }
            catch(Exception e){log.warn(LogSupport.EXCEPTION,e);}
        }
        
        // Stop the session manager
        _sessionManager.stop();
        
        _loader=null;
    }

    /* ------------------------------------------------------------ */
    HttpSession getHttpSession(String id)
    {
        return _sessionManager.getHttpSession(id);
    }
    
    /* ------------------------------------------------------------ */
    HttpSession newHttpSession(HttpServletRequest request)
    {
        return _sessionManager.newHttpSession(request);
    }

    /* ------------------------------------------------------------ */
    void setSessionInactiveInterval(int seconds)
    {
        _sessionManager.setMaxInactiveInterval(seconds);
    }

    /* ----------------------------------------------------------------- */
    /** Handle request.
     * @param pathInContext
     * @param pathParams
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
        if (!isStarted() && _context==null)
            return;
        
        // Handle TRACE
        if (HttpRequest.__TRACE.equals(httpRequest.getMethod()))
        {
            handleTrace(httpRequest,httpResponse);
            return;
        }

        // Look for existing request/response objects
        ServletHttpRequest request = (ServletHttpRequest) httpRequest.getWrapper();
        ServletHttpResponse response = (ServletHttpResponse) httpResponse.getWrapper();
        if (request==null)
        {
            // Build the request and response.
            request = new ServletHttpRequest(this,pathInContext,httpRequest);
            response = new ServletHttpResponse(request,httpResponse);
            httpRequest.setWrapper(request);
            httpResponse.setWrapper(response);
        }
        else
        {
            // Recycled request
            request.recycle(this,pathInContext);
            response.recycle();
        }
        
        
        // Look for the servlet
        Map.Entry servlet=getHolderEntry(pathInContext);
        ServletHolder servletHolder=servlet==null?null:(ServletHolder)servlet.getValue();
        if(log.isDebugEnabled())log.debug("servlet="+servlet);
            
        try
        {
            // Adjust request paths
            if (servlet!=null)
            {
                String servletPathSpec=(String)servlet.getKey(); 
                request.setServletPaths(PathMap.pathMatch(servletPathSpec,pathInContext),
                                        PathMap.pathInfo(servletPathSpec,pathInContext),
                                        servletHolder);
            }
            
            // Handle the session ID
            request.setSessionId(pathParams);
            HttpSession session=request.getSession(false);
            if (session!=null)
                ((SessionManager.Session)session).access();
            if(log.isDebugEnabled())log.debug("session="+session);
            
            // Do that funky filter and servlet thang!
            if (servletHolder!=null)
                dispatch(pathInContext,request,response,servletHolder);
        }
        catch(Exception e)
        {
            log.debug(LogSupport.EXCEPTION,e);
            
            Throwable th=e;
            if (e instanceof ServletException)
            {
                Throwable root=((ServletException)e).getRootCause();
                while (root instanceof ServletException)
                    root=((ServletException)e).getRootCause();
                if (root instanceof HttpException ||
                    root instanceof EOFException)
                {
                    if(log.isDebugEnabled())log.debug("Extracting root cause from ",e);
                    th=root;
                }
            }
            
            if (th instanceof HttpException)
                throw (HttpException)th;
            if (th instanceof EOFException)
                throw (IOException)th;
            else if (!log.isDebugEnabled() && th instanceof java.io.IOException)
                log.warn("Exception for "+httpRequest.getURI()+": "+th);
            else
            {
                log.warn("Exception for "+httpRequest.getURI(),th);
                if(log.isDebugEnabled())log.debug(httpRequest);
            }
            
            httpResponse.getHttpConnection().forceClose();
            if (!httpResponse.isCommitted())
            {
                request.setAttribute(ServletHandler.__J_S_ERROR_EXCEPTION_TYPE,th.getClass());
                request.setAttribute(ServletHandler.__J_S_ERROR_EXCEPTION,th);
                response.sendError(th instanceof UnavailableException
                                   ?HttpResponse.__503_Service_Unavailable
                                   :HttpResponse.__500_Internal_Server_Error,
                                   e.getMessage());
            }
            else
                if(log.isDebugEnabled())log.debug("Response already committed for handling "+th);
        }
        catch(Error e)
        {   
            log.warn("Error for "+httpRequest.getURI(),e);
            if(log.isDebugEnabled())log.debug(httpRequest);
            
            httpResponse.getHttpConnection().forceClose();
            if (!httpResponse.isCommitted())
            {
                request.setAttribute(ServletHandler.__J_S_ERROR_EXCEPTION_TYPE,e.getClass());
                request.setAttribute(ServletHandler.__J_S_ERROR_EXCEPTION,e);
                response.sendError(HttpResponse.__500_Internal_Server_Error,
                                   e.getMessage());
            }
            else
                if(log.isDebugEnabled())log.debug("Response already committed for handling ",e);
        }
        finally
        {
            if (servletHolder!=null)
            {
                response.flushBuffer();
                if (!httpRequest.isHandled())
                    new Throwable().printStackTrace();
            }
        }
    }

    /* ------------------------------------------------------------ */
    /** Dispatch to a servletHolder.
     * This method may be specialized to insert extra handling in the
     * dispatch of a request to a specific servlet. This is used by
     * WebApplicatonHandler to implement dispatched filters.
     * The default implementation simply calls
     * ServletHolder.handle(request,response)
     * @param pathInContext The path used to select the servlet holder.
     * @param request 
     * @param response 
     * @param servletHolder 
     * @exception ServletException 
     * @exception UnavailableException 
     * @exception IOException 
     */
    protected void dispatch(String pathInContext,
                            HttpServletRequest request,
                            HttpServletResponse response,
                            ServletHolder servletHolder)
        throws ServletException,
               UnavailableException,
               IOException
    {
        servletHolder.handle(request,response);
    }
    
    
    /* ------------------------------------------------------------ */
    /** ServletHolder matching path.
     * @param pathInContext Path within context.
     * @return PathMap Entries pathspec to ServletHolder
     */
    public Map.Entry getHolderEntry(String pathInContext)
    {
        return _servletMap.getMatch(pathInContext);
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
            log.trace(LogSupport.IGNORED,e);
        }
        
        return Collections.EMPTY_SET;
    }
    

    /* ------------------------------------------------------------ */
    /** Get a Resource.
     * If no resource is found, resource aliases are tried.
     * @param uriInContext 
     * @return URL of the resource.
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
            log.trace(LogSupport.IGNORED,e);
        }
        catch(MalformedURLException e)
        {
            throw e;
        }
        catch(IOException e)
        {
            log.warn(LogSupport.EXCEPTION,e);
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
        catch(MalformedURLException e) {log.trace(LogSupport.IGNORED,e);}
        catch(IOException e) {log.trace(LogSupport.IGNORED,e);}
        return null;
    }

    /* ------------------------------------------------------------ */
    public String getRealPath(String path)
    {
        if(log.isDebugEnabled())
            if(log.isDebugEnabled())log.debug("getRealPath of "+path+" in "+this);

        if (__Slosh2Slash)
            path=path.replace('\\','/');
        path=URI.canonicalPath(path);
        if (path==null)
            return null;

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
            log.warn(LogSupport.EXCEPTION,e);
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
            if ((q=uriInContext.indexOf(';'))>0)
                uriInContext=uriInContext.substring(0,q);

            String pathInContext=URI.canonicalPath(URI.decodePath(uriInContext));
            Map.Entry entry=getHolderEntry(pathInContext);
            if (entry!=null)
                return new Dispatcher(ServletHandler.this,
                                      uriInContext,
                                      pathInContext,
                                      query,
                                      entry);
        }
        catch(Exception e)
        {
            log.trace(LogSupport.IGNORED,e);
        }
        return null;
    }

    /* ------------------------------------------------------------ */
    /** Get Named dispatcher.
     * @param name The name of the servlet. If null or empty string, the
     * containers default servlet is returned.
     * @return Request dispatcher for the named servlet.
     */
    public RequestDispatcher getNamedDispatcher(String name)
    {
        if (name == null || name.length()==0)
            name=__DEFAULT_SERVLET;

        try { return new Dispatcher(ServletHandler.this,name); }
        catch(Exception e) {log.trace(LogSupport.IGNORED,e);}
        
        return null;
    }


    
    /* ------------------------------------------------------------ */
    void notFound(HttpServletRequest request,
                  HttpServletResponse response)
        throws IOException
    {
        if(log.isDebugEnabled())log.debug("Not Found "+request.getRequestURI());
        String method=request.getMethod();
            
        // Not found special requests.
        if (method.equals(HttpRequest.__GET)    ||
            method.equals(HttpRequest.__HEAD)   ||
            method.equals(HttpRequest.__POST))
        {
            response.sendError(HttpResponse.__404_Not_Found,request.getRequestURI()+" Not Found");
        }
        else if (method.equals(HttpRequest.__TRACE))
            handleTrace(request,response);
        else if (method.equals(HttpRequest.__OPTIONS))
            handleOptions(request,response);
        else
        {
            // Unknown METHOD
            response.setHeader(HttpFields.__Allow,__AllowString);
            response.sendError(HttpResponse.__405_Method_Not_Allowed);
        }
    }
    
    /* ------------------------------------------------------------ */
    void handleTrace(HttpServletRequest request,
                            HttpServletResponse response)
        throws IOException
    {
        response.setHeader(HttpFields.__ContentType,
                           HttpFields.__MessageHttp);
        OutputStream out = response.getOutputStream();
        ByteArrayISO8859Writer writer = new ByteArrayISO8859Writer();
        writer.write(request.toString());
        writer.flush();
        response.setIntHeader(HttpFields.__ContentLength,writer.size());
        writer.writeTo(out);
        out.flush();
    }
    
    /* ------------------------------------------------------------ */
    void handleOptions(HttpServletRequest request,
                              HttpServletResponse response)
        throws IOException
    {
        // Handle OPTIONS request for entire server
        if ("*".equals(request.getRequestURI()))
        {
            // 9.2
            response.setIntHeader(HttpFields.__ContentLength,0);
            response.setHeader(HttpFields.__Allow,__AllowString);                
            response.flushBuffer();
        }
        else
            response.sendError(HttpResponse.__404_Not_Found);
    }

    /* ------------------------------------------------------------ */
    String getErrorPage(int status,ServletHttpRequest request)
    {
        return null;
    }
    
    
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    class Context implements ServletContext
    {
        /* -------------------------------------------------------- */
        ServletHandler getServletHandler()
        {
            return ServletHandler.this;
        }
        
        /* -------------------------------------------------------- */
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
            log.info(msg);
        }

        /* ------------------------------------------------------------ */
        /**
         * @deprecated As of Java Servlet API 2.1, use
         * 			{@link #log(String message, Throwable throwable)} 
         *			instead.
         */
        public void log(Exception e, String msg)
        {
            log.warn(msg,e);
        }

        /* ------------------------------------------------------------ */
        public void log(String msg, Throwable th)
        {
            log.warn(msg,th);
        }

        /* ------------------------------------------------------------ */
        public String getServerInfo()
        {
            return Version.__VersionImpl;
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
            if (ServletHandler.__J_S_CONTEXT_TEMPDIR.equals(name))
            {
                // Initialize temporary directory
                Object t = getHttpContext().getAttribute(ServletHandler.__J_S_CONTEXT_TEMPDIR);

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
            getHttpContext().setAttribute(name,value);
        }

        /* ------------------------------------------------------------ */
        /** Remove context attribute.
         * Delegated to HttpContext.
         * @param name attribute name.
         */
        public void removeAttribute(String name)
        {
            getHttpContext().removeAttribute(name);
        }
    
        /* ------------------------------------------------------------ */
        public String getServletContextName()
        {
            if (getHttpContext() instanceof WebApplicationContext)
                return ((WebApplicationContext)getHttpContext()).getDisplayName();
            return null;
        }

        /* ------------------------------------------------------------ */
        public String toString()
        {
            return "ServletContext["+getHttpContext()+"]";
        }
    }    
}
