
// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.jetty.servlet;

import org.mortbay.http.HandlerContext;
import org.mortbay.http.Version;
import org.mortbay.util.Code;
import org.mortbay.util.Frame;
import org.mortbay.util.Log;
import org.mortbay.util.LogSink;
import org.mortbay.util.URI;
import org.mortbay.util.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.ConcurrentModificationException;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

/* --------------------------------------------------------------------- */
/** Jetty Servlet Context.
 *
 * @version $Id$
 * @author Greg Wilkins
 */
public class Context implements ServletContext
{
    /* ------------------------------------------------------------ */
    private static final String
        CONTEXT_LOG="org.mortbay.jetty.servlet.Context.LogSink";

    /* ------------------------------------------------------------ */
    private static final boolean __Slosh2Slash=File.separatorChar=='\\';
 
    /* ------------------------------------------------------------ */
    private ServletHandler _handler;
    private HandlerContext _handlerContext;
    private LogSink _logSink;
    private SessionManager _sessionManager;

    /* ------------------------------------------------------------ */
    /** Constructor.
     * @param handler
     */
    Context(ServletHandler handler)
    {
        _handler=handler;
        _handlerContext=_handler.getHandlerContext();
        if(_handlerContext!=null)
            _logSink=(LogSink)_handlerContext.getAttribute(CONTEXT_LOG);
        _sessionManager = new HashSessionManager(this);
    }
    
    /* ------------------------------------------------------------ */
    ServletHandler getHandler(){return _handler;}
    HandlerContext getHandlerContext(){return _handlerContext;}
    void setHandlerContext(HandlerContext hc)
    {
        _handlerContext=hc;
        if(_handlerContext!=null)
            _logSink=(LogSink)_handlerContext.getAttribute(CONTEXT_LOG);
    }

    /* ------------------------------------------------------------ */
    ServletHandler getServletHandler()
    {
        return _handler;
    }
    
    /* ------------------------------------------------------------ */
    public String getServletContextName()
    {
        if (_handlerContext instanceof WebApplicationContext)
            return ((WebApplicationContext)_handlerContext).getDisplayName();
        return null;
    }
    
    
    /* ------------------------------------------------------------ */
    public String getContextPath()
    {
        return _handlerContext.getContextPath();
    }

    /* ------------------------------------------------------------ */
    public ServletContext getContext(String uri)
    {        
        HandlerContext context=
            _handlerContext;

        ServletHandler handler= (ServletHandler)
            context.getHttpServer()
            .findHandler(org.mortbay.jetty.servlet.ServletHandler.class,
                         uri,context.getHosts());
        
        if (handler!=null)
            return handler.getContext();
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
        return _handlerContext.getMimeByExtension(file);
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
        Resource baseResource=_handlerContext.getBaseResource();
        uriInContext=Resource.canonicalPath(uriInContext);
        if (baseResource==null || uriInContext==null)
            return null;
        
        try{
            Resource resource = baseResource.addPath(uriInContext);
            if (resource.exists())
                return resource.getURL();

            String aliasedUri=_handlerContext.getResourceAlias(uriInContext);
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
    public Set getResourcePaths(String uriInContext)
    {
        Resource baseResource=_handlerContext.getBaseResource();
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
    public InputStream getResourceAsStream(String uriInContext)
    {
        try
        {
            URL url = getResource(uriInContext);
            if (url!=null)
                return url.openStream();
        }
        catch(MalformedURLException e)
        {
            Code.ignore(e);
        }
        catch(IOException e)
        {
            Code.ignore(e);
        }
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

            return new Dispatcher(this,pathInContext,query);
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

        try
        {
            return new Dispatcher(this,name);
        }
        catch(Exception e)
        {
            Code.ignore(e);
            return null;
        }
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
    }

    /* ------------------------------------------------------------ */
    public void log(String msg, Throwable th)
    {
        Code.warning(msg,th);
    }

    /* ------------------------------------------------------------ */
    public String getRealPath(String path)
    {
        if(Code.debug())
            Code.debug("getRealPath of ",path," in ",this);

        if (__Slosh2Slash)
            path=path.replace('\\','/');
        
        Resource baseResource=_handlerContext.getBaseResource();
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
     * Delegated to HandlerContext.
     * Init Parameters differ from attributes as they can only
     * have string values, servlets cannot set them and they do
     * not have a package scoped name space.
     * @param param param name
     * @return param value or null
     */
    public String getInitParameter(String param)
    {
        return _handlerContext.getInitParameter(param);
    }

    /* ------------------------------------------------------------ */
    /** Get context init parameter names.
     * Delegated to HandlerContext.
     * @return Enumeration of names
     */
    public Enumeration getInitParameterNames()
    {
        return _handlerContext.getInitParameterNames();
    }

    /* ------------------------------------------------------------ */
    /** Get context attribute.
     * Delegated to HandlerContext.
     * @param name attribute name.
     * @return attribute
     */
    public Object getAttribute(String name)
    {
        if ("javax.servlet.context.tempdir".equals(name))
        {
            // Initialize temporary directory
            File tempDir=(File)_handlerContext
                .getAttribute("javax.servlet.context.tempdir");
            if (tempDir==null)
            {
                try{
                    tempDir=File.createTempFile("JettyContext",null);
                    if (tempDir.exists())
                        tempDir.delete();
                    tempDir.mkdir();
                    tempDir.deleteOnExit();
                    _handlerContext
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

        return _handlerContext.getAttribute(name);
    }

    /* ------------------------------------------------------------ */
    /** Get context attribute names.
     * Delegated to HandlerContext.
     */
    public Enumeration getAttributeNames()
    {
        return _handlerContext.getAttributeNames();
    }

    /* ------------------------------------------------------------ */
    /** Set context attribute names.
     * Delegated to HandlerContext.
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
        _handlerContext.setAttribute(name,value);
    }

    /* ------------------------------------------------------------ */
    /** Remove context attribute.
     * Delegated to HandlerContext.
     * @param name attribute name.
     */
    public void removeAttribute(String name)
    {
        if (name.startsWith("org.mortbay.http"))
        {
            Code.warning("Servlet attempted update of "+name);
            return;
        }
        _handlerContext.removeAttribute(name);
    }


    /* ------------------------------------------------------------ */
    boolean isValid(HttpSession session)
    {
        return _sessionManager.isValid(session);
    }

    /* ------------------------------------------------------------ */
    HttpSession getHttpSession(String id)
    {
        return _sessionManager.getHttpSession(id);
    }
    
    /* ------------------------------------------------------------ */
    HttpSession newSession()
    {
        return _sessionManager.newSession();
    }

    /* ------------------------------------------------------------ */
    void stop()
    {
        _sessionManager.stop();
    }

    /* ------------------------------------------------------------ */
    void access(HttpSession session)
    {
        _sessionManager.access(session);
    }
    
    /* ------------------------------------------------------------ */
    void setSessionTimeout(int timeoutMinutes)
    {
        _sessionManager.setSessionTimeout(timeoutMinutes);
    }
    

}
