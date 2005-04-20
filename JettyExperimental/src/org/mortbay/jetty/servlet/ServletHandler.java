// ========================================================================
// $Id$
// Copyright 199-2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.mortbay.jetty.servlet;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
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

import org.apache.ugli.LoggerFactory;
import org.apache.ugli.ULogger;
import org.mortbay.io.Portable;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.HttpHeaderValues;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.HttpMethods;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.WrappedHandler;
import org.mortbay.resource.MimeTypes;
import org.mortbay.util.Container;
import org.mortbay.util.LogSupport;
import org.mortbay.util.MultiException;
import org.omg.CORBA._PolicyStub;


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
public class ServletHandler extends WrappedHandler
{
    private static ULogger log = LoggerFactory.getLogger(ServletHolder.class);

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
    protected ServletContext _context;
    protected ServletHolder[] _servlets;
    protected PathMap _servletPathMap=new PathMap();
    protected Map _servletNameMap=new HashMap();

    protected transient ULogger _contextLog;

    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    public ServletHandler()
    {
    }
    
    /* ------------------------------------------------------------ */
    public ServletHolder getServletHolder(String name)
    {
        return (ServletHolder)_servletNameMap.get(name);
    }    
    
    /* ------------------------------------------------------------ */
    /**
     * Register an existing ServletHolder with this handler.
     * @param holder the ServletHolder to register.
     */
    public void addServletHolder(ServletHolder holder)
    {
        ServletHolder existing = (ServletHolder)
        _servletNameMap.get(holder.getName());
        if (existing==null)
            _servletNameMap.put(holder.getName(),holder);
        else if (existing!=holder)
            throw new IllegalArgumentException("Holder already exists for name: "+holder.getName());
    }

    /* ------------------------------------------------------------ */
    public ServletContext getServletContext()
    {
        return _context;
    }
    
    /* ----------------------------------------------------------------- */
    protected synchronized void doStart()
        throws Exception
    {
        if (isStarted())
            return;
        _context=ContextHandler.getCurrentContext();
        _contextLog = LoggerFactory.getLogger(_context.getServletContextName());
        if (_contextLog==null)
            _contextLog=log;

        initializeServlets();
    }   
    
    /* ------------------------------------------------------------ */
    /** Get Servlets.
     * @return Array of defined servlets
     */
    public ServletHolder[] getServlets()
    {
        return _servlets;
    }
    
    /* ------------------------------------------------------------ */
    /** Get Servlets.
     * @return Array of defined servlets
     */
    public synchronized void setServlets(ServletHolder[] holders)
    {
        _servlets=(ServletHolder[])holders.clone();
        try
        {
            mapServlets();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /* ------------------------------------------------------------ */
    synchronized void mapServlets()
    	throws Exception
    {
        // handle null holders
        if (_servlets==null)
        {
            _servletNameMap=null;
            _servletPathMap=null;
            return;
        }
        
        // create new maps
        PathMap pm = new PathMap();
        HashMap nm = new HashMap();
        
        // update the maps
        for (int i=0;i<_servlets.length;i++)
        {
            nm.put(_servlets[i].getName(),_servlets[i]);
            String[] paths = _servlets[i].getPaths();
            if (paths!=null)
            {
                for (int j=0;j<paths.length;j++)
                    pm.put(paths[j],_servlets[i]);
            }
        }
        
        _servletPathMap=pm;
        _servletNameMap=nm;
        
        System.err.println("servletNameMap="+nm);
        System.err.println("servletPathMap="+pm);
        
        if (isStarted())
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
        
        if (_servlets!=null)
        {
            // Sort and Initialize servlets
            ServletHolder[] servlets = (ServletHolder[])_servlets.clone();
            Arrays.sort(servlets);
            for (int i=0; i<servlets.length; i++)
            {
                try
                {
                    servlets[i].setServletHandler(this);
                    servlets[i].start();
                }
                catch(Exception e)
                {
                    log.debug(LogSupport.EXCEPTION,e);
                    mx.add(e);
                }
            } 
            mx.ifExceptionThrow();  
        }
    }
    
    /* ----------------------------------------------------------------- */
    protected synchronized void doStop()
        throws Exception
    {
        // Sort and Initialize servlets
        ServletHolder[] holders = getServlets();
        
        // Stop servlets
        for (int i=holders.length; i-->0;)
        {
            try
            {
                holders[i].stop();
            }
            catch(Exception e){log.warn(LogSupport.EXCEPTION,e);}
        }
        
    }


    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.jetty.Handler#handle(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, int)
     */
    public boolean handle(HttpServletRequest request, HttpServletResponse response,int type)
         throws IOException
    {
        if (!isStarted())
            return false;

        // Get the base requests
        Request base_request=(request instanceof Request)?((Request)request):HttpConnection.getCurrentConnection().getRequest();
        
        // find the servlet
        ServletHolder servletHolder=null;
        if (type == REQUEST)
        {
            // Look for the servlet
            String pathInContext=request.getPathInfo(); // TODO WRONG???

            Map.Entry map=getHolderEntry(pathInContext);
            if (map!=null)
            {
                servletHolder=(ServletHolder)map.getValue();
                if(log.isDebugEnabled())log.debug("servlet="+servletHolder);
          
                String servletPathSpec=(String)map.getKey(); 
                String servletPath=PathMap.pathMatch(servletPathSpec,pathInContext);
                String pathInfo=PathMap.pathInfo(servletPathSpec,pathInContext);
                
                base_request.setServletPath(servletPath);
                base_request.setPathInfo(pathInfo);
                // TODO - should these be reset afterwards?
            }      
        }
        else
        {
            // servlet must already be determined
            Portable.throwNotSupported();
        }
        
        try
        {
            
            
            // Do that funky filter thang 
            // TODO
            
            // servlet thang!
            if (servletHolder!=null)
            {

                servletHolder.handle(request,response);
                
                /* TODO
                dispatch(pathInContext,request,response,servletHolder, Dispatcher.__REQUEST);
                */
            }
        }
        catch(Exception e)
        {
            log.debug(LogSupport.EXCEPTION,e);
            
            Throwable th=e;
            while (th instanceof ServletException)
            {
                Throwable cause=((ServletException)th).getRootCause();
                if (cause==th || cause==null)
                    break;
                th=cause;
            }
            
            /* TODO
            if (th instanceof HttpException)
                throw (HttpException)th;
            if (th instanceof EOFException)
                throw (IOException)th;
            else */ if (log.isDebugEnabled() || !( th instanceof java.io.IOException))
            {
                _contextLog.warn(request.getRequestURI()+": ",th);
                if(log.isDebugEnabled())
                {
                    log.warn(request.getRequestURI()+": ",th);
                    log.debug(request);
                }
            }
            
            // TODO httpResponse.getHttpConnection().forceClose();
            if (!response.isCommitted())
            {
                request.setAttribute(ServletHandler.__J_S_ERROR_EXCEPTION_TYPE,th.getClass());
                request.setAttribute(ServletHandler.__J_S_ERROR_EXCEPTION,th);
                if (th instanceof UnavailableException)
                {
                    UnavailableException ue = (UnavailableException)th;
                    if (ue.isPermanent())
                        response.sendError(HttpServletResponse.SC_NOT_FOUND,e.getMessage());
                    else
                        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,e.getMessage());
                }
                else
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,e.getMessage());
            }
            else
                if(log.isDebugEnabled())log.debug("Response already committed for handling "+th);
        }
        catch(Error e)
        {   
            log.warn("Error for "+request.getRequestURI(),e);
            if(log.isDebugEnabled())log.debug(request);
            
            // TODO httpResponse.getHttpConnection().forceClose();
            if (!response.isCommitted())
            {
                request.setAttribute(ServletHandler.__J_S_ERROR_EXCEPTION_TYPE,e.getClass());
                request.setAttribute(ServletHandler.__J_S_ERROR_EXCEPTION,e);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,e.getMessage());
            }
            else
                if(log.isDebugEnabled())log.debug("Response already committed for handling ",e);
        }
        finally
        {
            if (servletHolder!=null && response!=null)
            {
                // TODO ??? response.complete();
            }
        }
        return true;
    }
    
    /* ------------------------------------------------------------ */
    /** ServletHolder matching path.
     * @param pathInContext Path within _context.
     * @return PathMap Entries pathspec to ServletHolder
     */
    private Map.Entry getHolderEntry(String pathInContext)
    {
        return _servletPathMap.getMatch(pathInContext);
    }
    
    

    /* ------------------------------------------------------------ */
    /**
     * @param ipath
     * @return
     */
    public RequestDispatcher getRequestDispatcher(String ipath)
    {
        // TODO Auto-generated method stub
        return null;
    }


        
}
