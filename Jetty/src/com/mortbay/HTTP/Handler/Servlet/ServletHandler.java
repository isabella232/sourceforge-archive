// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler.Servlet;

import com.mortbay.HTTP.Handler.NullHandler;
import com.mortbay.HTTP.HandlerContext;
import com.mortbay.HTTP.HttpException;
import com.mortbay.HTTP.HttpRequest;
import com.mortbay.HTTP.HttpResponse;
import com.mortbay.HTTP.PathMap;
import com.mortbay.Util.Code;
import com.mortbay.Util.Log;
import com.mortbay.Util.Resource;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;


/* --------------------------------------------------------------------- */
/** ServletHandler<p>
 * This handler maps requests to servlets that implement the
 * javax.servlet.http.HttpServlet API.
 * It is configured with a PathMap of paths to ServletHolder instances.
 *
 * @version $Id$
 * @author Greg Wilkins
 */
public class ServletHandler extends NullHandler 
{
    /* ----------------------------------------------------------------- */
    private PathMap _servletMap=new PathMap();
    private Map _nameMap=new HashMap();
    private Context _context;
    private boolean _autoReload;
    private ServletLoader _loader;

    /* ----------------------------------------------------------------- */
    /** Construct basic auth handler.
     */
    public ServletHandler()
    {
        _context = new Context(this);
    }
    
    /* ------------------------------------------------------------ */
    public Context getContext()
    {
        return _context;
    }

    /* ------------------------------------------------------------ */
    public PathMap getServletMap()
    {
        return _servletMap;
    }
    
    /* ------------------------------------------------------------ */
    public boolean isAutoReload()
    {
        return _autoReload;
    }
    /* ------------------------------------------------------------ */
    public void setAutoReload(boolean autoReload)
    {
        _autoReload = autoReload;
    }
    
    /* ------------------------------------------------------------ */
    public ServletLoader getServletLoader()
    {
        return _loader;
    }
    
    /* ------------------------------------------------------------ */
    private synchronized void initializeLoader()
    {
        try
        {
            String classPath=getHandlerContext().getClassPath();	    
            if (classPath!=null && classPath.length()>0)
            {
                _loader=new ServletLoader(classPath,false);
                Code.debug("servlet classpath=",classPath);
            }
            else
                _loader=new ServletLoader(null,false);    
        }
        catch(Throwable e)
        {
            Code.fail(e);
        }
    }
    
    /* ----------------------------------------------------------------- */
    public void start()
    {
        // Initialize classloader
        initializeLoader();
        
        // Initialize servlets
        Iterator i = _servletMap.values().iterator();
        while (i.hasNext())
        {
            ServletHolder holder = (ServletHolder)i.next();
            if (holder.isInitOnStartup())
                holder.initialize();
        }
        
        Log.event("ServletHandler started: "+this);
        super.start();
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
        _servletMap.put(pathSpec,holder);
    }
    
    /* ----------------------------------------------------------------- */
    /**
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
            // Handle reload 
            if (isAutoReload())
            {
                synchronized(this)
                {
                    if (_loader.isModified())
                    {
                        Log.event("RELOAD "+this);
                        // XXX Should wait for requests to goto 0;
                        
                        // destroy old servlets
                        destroy();
                        // New loader
                        initializeLoader();
                    }
                }
            }
            
            
            // Build servlet request and response
            ServletRequest request =
                new ServletRequest(_context,httpRequest);
            ServletResponse response =
                new ServletResponse(request,httpResponse);

            handle(pathInContext,request,response);
        }
        catch(Exception e)
        {
            Code.warning(e);
            System.err.println(httpRequest);
            throw new HttpException();
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
        return _servletMap.getMatch(pathInContext);
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

        Map.Entry entry=getHolderEntry(pathInContext);
        
        if (entry!=null)
        {
            String servletPathSpec=(String)entry.getKey();
            ServletHolder holder = (ServletHolder)entry.getValue();
        
            Code.debug("Pass request to servlet at ",entry);
            request.setPaths(PathMap.pathMatch(servletPathSpec,
                                               pathInContext),
                             PathMap.pathInfo(servletPathSpec,
                                              pathInContext));
            
            // Check session stuff
            HttpSession session=null;
            if ((session=request.getSession(false))!=null)
                Context.access(session);
            
            // service request
            try
            {
                holder.handle(request,response);
            }
            finally
            {
                response.setOutputState(0);
                Code.debug("Handled by ",entry);
                if (!httpResponse.isCommitted())
                    httpResponse.commit();
            }
        }
    }
        
    /* ------------------------------------------------------------ */
    /** 
     * @param servletName 
     * @return 
     */
    public ServletHolder newServletHolder(String servletClass)
        throws javax.servlet.UnavailableException,
               ClassNotFoundException
    {
        return new ServletHolder(this,servletClass);
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
}
