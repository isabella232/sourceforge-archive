// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Jetty.Servlet;

import com.mortbay.HTTP.ContextLoader;
import com.mortbay.HTTP.Handler.NullHandler;
import com.mortbay.HTTP.HandlerContext;
import com.mortbay.HTTP.HttpException;
import com.mortbay.HTTP.HttpRequest;
import com.mortbay.HTTP.HttpResponse;
import com.mortbay.HTTP.PathMap;
import com.mortbay.HTTP.Handler.SecurityHandler;
import com.mortbay.HTTP.UserRealm;
import com.mortbay.HTTP.UserPrincipal;
import com.mortbay.Util.Code;
import com.mortbay.Util.IO;
import com.mortbay.Util.Log;
import com.mortbay.Util.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import java.security.Principal;


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
    public final static String __JSP_SERVLET="org.apache.jasper.servlet.JspServlet";
    public final static String __SERVLET_REQUEST="com.mortbay.J.S.Request";
    public final static String __SERVLET_RESPONSE="com.mortbay.J.S.Response";
    public final static String __SERVLET_HOLDER="com.mortbay.J.S.Holder";
    public final static String __SERVLET_PATH="com.mortbay.J.S.Path";
    public final static String __J_SECURITY_CHECK="j_security_check";
    public final static String __J_USERNAME="j_username";
    public final static String __J_PASSWORD="j_password";
    public final static String __J_URI="com.mortbay.J.S.URI";
    public final static String __J_AUTHENTICATED="com.mortbay.J.S.Auth";
    
    /* ------------------------------------------------------------ */
    private PathMap _servletMap=new PathMap();
    
    private Map _nameMap=new HashMap();
    private Context _context;
    private ClassLoader _loader;
    private String _dynamicServletPathSpec;
    private Map _dynamicInitParams ;
    private boolean _serveDynamicSystemServlets=false;
    private boolean _usingCookies=true;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public ServletHandler()
    {
        _context = new Context(this);
    }
    
    /* ------------------------------------------------------------ */
    public Context getContext() { return _context; }

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
    /** Not Supported. 
     * @param autoReload 
     */
    public void setAutoReload(boolean autoReload)
    {
        if (autoReload==true)
            Code.warning("AutoReload is no longer supported!\n"+
                         "It may be resurrected once the URL libraries fully support lastModified");
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
    
    /* ----------------------------------------------------------------- */
    public synchronized void start()
        throws Exception
    {        
        _context.setHandlerContext(getHandlerContext());
        
        // Initialize classloader
        _loader=getHandlerContext().getClassLoader();
        
        // Sort and Initialize servlets
        ServletHolder holders [] = (ServletHolder [])
            (new HashSet(_servletMap.values ())).toArray(new ServletHolder [0]);
        java.util.Arrays.sort (holders);        
        for (int i=0; i<holders.length; i++)
        {
            ServletHolder holder = holders [i];
            
            if (holder.isInitOnStartup())
                holder.initialize();
            else
            {
                try
                {
                    holder.initializeClass();
                }
                catch(UnavailableException e)
                {
                    Code.warning(e);
                }
            }
        }
        
        super.start();
    }   
    
    /* ----------------------------------------------------------------- */
    public synchronized void stop()
    {
        _loader=null;
        // Stop servlets
        Iterator i = _servletMap.values().iterator();
        while (i.hasNext())
        {
            ServletHolder holder = (ServletHolder)i.next();
            holder.destroy();
        }
        if (_context!=null)
            _context.stop();
        super.stop();
    }
    
    
    /* ------------------------------------------------------------ */
    /** 
     * @param path 
     * @param servletClass 
     */
    public ServletHolder addServlet(String name,
                                    String pathSpec,
                                    String servletClass)
    {
        ServletHolder holder = addServlet(pathSpec,servletClass);
        holder.setServletName(name);
        return holder;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param path 
     * @param servletClass 
     */
    public ServletHolder addServlet(String pathSpec,
                                    String servletClass)
    {
        try
        {
            ServletHolder holder =
                newServletHolder(servletClass);
            addHolder(pathSpec,holder);
            
            return holder;
        }
        catch(Exception e)
        {
            Code.warning(e);
            throw new IllegalArgumentException(e.toString());
        }
    }

    /* ------------------------------------------------------------ */
    /** Get servlet by name.
     * @param name 
     * @return 
     */
    public ServletHolder getServletHolder(String name)
    {
        return (ServletHolder)_nameMap.get(name);
    }
    
    /* ------------------------------------------------------------ */
    public void addHolder(String pathSpec, ServletHolder holder)
    {
        try
        {
            if (isStarted())
                holder.initializeClass();
            _servletMap.put(pathSpec,holder);
        }
        catch(UnavailableException e)
        {
            Code.warning(e);
        }
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
                       HttpRequest httpRequest,
                       HttpResponse httpResponse)
         throws IOException
    {
        try
        {
            // Look for a previously built servlet request.
            ServletRequest request = (ServletRequest)
                httpRequest.getAttribute(ServletHandler.__SERVLET_REQUEST);
            ServletResponse response;
            if (request==null)
            {
                // Build servlet request and response
                request  = new ServletRequest(_context,httpRequest);
                response = new ServletResponse(request,httpResponse);
                // Check session stuff
                pathInContext=request.setSessionId(pathInContext);
                HttpSession session=null;
                if ((session=request.getSession(false))!=null)
                    Context.access(session);
            }
            else
            {
                response=(ServletResponse)request.getAttribute(ServletHandler.__SERVLET_RESPONSE);
                String pic = (String)request.getAttribute(ServletHandler.__SERVLET_PATH);
                if (pic!=null)
                    pathInContext=pic;
            }

            // handle
            handle(pathInContext,request,response);
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
        }
        catch(Error e)
        {
            Code.warning("Servlet Error for "+httpRequest.getURI(),e);
            Code.debug(httpRequest);
            httpResponse.getHttpConnection().forceClose();
            if (!httpResponse.isCommitted())
                httpResponse.sendError(503,e);
        }
    }

    
    /* ------------------------------------------------------------ */
    /** ServletHolder matching path.
     * In a separate method so that dynamic servlet loading can be
     * implemented by derived handlers.
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
        if (servletClass!=null && servletClass.length()>2 &&
            (entry==null||!PathMap.match(_dynamicServletPathSpec,(String)entry.getKey())))
        {
            try
            {
                // OK lets look for a dynamic servlet.
                String path=pathInContext;
                Code.debug("looking for ",servletClass," in ",
                           getHandlerContext().getClassPath());
                
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
                ServletHolder holder=newServletHolder(servletClass);
                
                // Set params
                Map params=getDynamicInitParams();
                if (params!=null)
                    holder.putAll(params);
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
                        holder.destroy();
                        String msg = "Dynamic servlet "+
                            servletClass+
                            " is not in context: "+
                            getContext().getHandlerContext().getContextPath();
                        
                        Code.warning(msg);
                        throw new UnavailableException(msg);
                    }
                }
                
                Log.event("Dynamic load '"+servletClass+"' at "+path);
                addHolder(path+"/*",holder);
                addHolder(path+".class/*",holder);

                entry=_servletMap.getMatch(pathInContext);
            }
            catch(Exception e)
            {
                Code.ignore(e);
            }
        }
        
        return entry;
    }
    

    /* ------------------------------------------------------------ */
    /** 
     * @param pathInContext 
     * @param request 
     * @param response 
     * @exception IOException 
     * @exception ServletException 
     * @exception UnavailableException 
     */
    void handle(String pathInContext,
                ServletRequest request,
                ServletResponse response)
        throws IOException, ServletException, UnavailableException
    {
        Code.debug("Looking for servlet at ",pathInContext);
        HttpResponse httpResponse=response.getHttpResponse();

        ServletHolder holder =(ServletHolder)
            request.getAttribute(ServletHandler.__SERVLET_HOLDER);
        if (holder==null)
        {
            Map.Entry entry=getHolderEntry(pathInContext);
            if (entry!=null)
            {
                String servletPathSpec=(String)entry.getKey();
                holder = (ServletHolder)entry.getValue();
        
                Code.debug("Pass request to servlet at ",entry);
                request.setPaths(PathMap.pathMatch(servletPathSpec,
                                                   pathInContext),
                                 PathMap.pathInfo(servletPathSpec,
                                                  pathInContext));
            }
        }

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
        
    /* ------------------------------------------------------------ */
    public ServletHolder newServletHolder(String servletClass)
        throws javax.servlet.UnavailableException,
               ClassNotFoundException
    {
        return new ServletHolder(this,servletClass);
    }

    /* ------------------------------------------------------------ */
    public ServletHolder newServletHolder(String servletClass, String path)
        throws javax.servlet.UnavailableException,
               ClassNotFoundException
    {
        return new ServletHolder(this,servletClass,path);
    }
    
    /* ------------------------------------------------------------ */
    /** Destroy Handler.
     * Destroy all servlets
     */
    public synchronized void destroy()
    {
        Iterator i = _servletMap.values().iterator();
        while (i.hasNext())
        {
            ServletHolder holder = (ServletHolder)i.next();
            holder.destroy();
        }
    }


    /* ------------------------------------------------------------ */
    void mapHolder(String name,ServletHolder holder, String oldName)
    {
        synchronized(_nameMap)
        {
            if (oldName!=null)
                _nameMap.remove(oldName);
            _nameMap.put(name,holder);
        }
    }

    /* ------------------------------------------------------------ */
    public String getJSPClassName()
    {
        return __JSP_SERVLET;
    }
    
    /* ------------------------------------------------------------ */
    /** Perform form authentication.
     * Called from SecurityHandler.
     * @return true if authenticated.
     */
    public boolean formAuthenticated(SecurityHandler shandler,
                                     String pathInContext,
                                     HttpRequest httpRequest,
                                     HttpResponse httpResponse)
        throws IOException
    {
        // Look for a previously built servlet request.
        ServletRequest request = (ServletRequest)
            httpRequest.getAttribute(ServletHandler.__SERVLET_REQUEST);
        ServletResponse response;
        if (request==null)
        {
            // Build servlet request and response
            request  = new ServletRequest(_context,httpRequest);
            response = new ServletResponse(request,httpResponse);
            request.setAttribute(ServletHandler.__SERVLET_REQUEST,request);
            request.setAttribute(ServletHandler.__SERVLET_RESPONSE,response);
        }
        else
        {
            response=(ServletResponse)request.getAttribute(ServletHandler.__SERVLET_RESPONSE);
        }

        // Handle paths
        pathInContext = request.setSessionId(pathInContext); 
        request.setAttribute(ServletHandler.__SERVLET_PATH,pathInContext);
        String uri = request.setSessionId(httpRequest.getURI().toString());
        
        // Setup session 
        HttpSession session=request.getSession(true);             
        
        // Handle a request for authentication.
        if ( uri.substring(uri.lastIndexOf("/")+1).startsWith(__J_SECURITY_CHECK) )
        {
            // Check the session object for login info. 
            String username = request.getParameter(__J_USERNAME);
            String password = request.getParameter(__J_PASSWORD);
            
            UserPrincipal user =
                shandler.getUserRealm().getUser(username,request.getHttpRequest());
            if (user!=null && user.authenticate(password))
            {
                Code.debug("Form authentication OK for ",username);
                request.setAttribute(HttpRequest.__AuthType,"FORM");
                request.setAttribute(HttpRequest.__AuthUser,username);
                request.setAttribute(UserPrincipal.__ATTR,user);
                session.setAttribute(__J_AUTHENTICATED,__J_AUTHENTICATED);
                String nuri=(String)session.getValue(__J_URI);
                response.sendRedirect(nuri==null?shandler.getErrorPage():nuri);
            }
            else
            {
                Code.debug("Form authentication FAILED for ",username);
                response.sendRedirect(shandler.getErrorPage());
            }
            
            // Security check is always false, only true after final redirection.
            return false;
        }

        // Check if the session is already authenticated.
        if (session.getAttribute(__J_AUTHENTICATED).equals(__J_AUTHENTICATED))
            return true;

        // redirect to login page
        response.sendRedirect(shandler.getLoginPage());
        return false;
    }
}
