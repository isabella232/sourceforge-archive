// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.jetty.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpFields;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.InclusiveByteRange;
import org.mortbay.http.MultiPartResponse;
import org.mortbay.http.PathMap;
import org.mortbay.util.ByteArrayISO8859Writer;
import org.mortbay.util.CachedResource;
import org.mortbay.util.Code;
import org.mortbay.util.IO;
import org.mortbay.util.LazyList;
import org.mortbay.util.Log;
import org.mortbay.util.MultiException;
import org.mortbay.util.MultiException;
import org.mortbay.util.MultiMap;
import org.mortbay.util.Resource;
import org.mortbay.util.StringUtil;
import org.mortbay.util.URI;

/* --------------------------------------------------------------------- */
/** WebApp HttpHandler.
 * This handler extends the ServletHandler with security, filter and resource
 * capabilities to provide full J2EE web container support.
 * <p>
 * @since Jetty 4.1
 * @see org.mortbay.jetty.servlet.WebApplicationContext
 * @version $Id$
 * @author Greg Wilkins
 */
public class WebApplicationHandler extends ServletHandler
{
    private Map _filterMap=new HashMap();
    private List _pathFilters=new ArrayList();
    private MultiMap _servletFilterMap=new MultiMap();
    private boolean _acceptRanges=true;
    private boolean _started=false;
    
    /* ------------------------------------------------------------ */
    public boolean isAcceptRanges()
    {
        return _acceptRanges;
    }
    
    /* ------------------------------------------------------------ */
    /** Set if the handler accepts range requests.
     * Default is false;
     * @param ar True if the handler should accept ranges
     */
    public void setAcceptRanges(boolean ar)
    {
        _acceptRanges=ar;
    }
    
    /* ------------------------------------------------------------ */
    public FilterHolder defineFilter(String name, String className)
    {
        FilterHolder holder = new FilterHolder(this,name,className);
        _filterMap.put(holder.getName(),holder);
        return holder;
    }
    
    /* ------------------------------------------------------------ */
    public FilterHolder getFilter(String name)
    {
        return (FilterHolder)_filterMap.get(name);
    }

    /* ------------------------------------------------------------ */
    public FilterHolder mapServletToFilter(String servletName,
                                           String filterName)
    {
        FilterHolder holder =(FilterHolder)_filterMap.get(filterName);
        if (holder==null)
            throw new IllegalArgumentException("Unknown filter :"+filterName);
        Code.debug("Filter servlet ",servletName," --> ",filterName);
        _servletFilterMap.add(servletName,holder);
        return holder;
    }
    
    /* ------------------------------------------------------------ */
    public FilterHolder mapPathToFilter(String pathSpec,
                                        String filterName)
    {
        FilterHolder holder =(FilterHolder)_filterMap.get(filterName);
        if (holder==null)
            throw new IllegalArgumentException("Unknown filter :"+filterName);
        
        Code.debug("Filter path ",pathSpec," --> ",filterName);

        if (!holder.isMappedToPath())
            _pathFilters.add(holder);
        holder.addPathSpec(pathSpec);
        
        return holder;
    }

    /* ------------------------------------------------------------ */
    public boolean isStarted()
    {
        return _started&&super.isStarted();
    }
    
    /* ----------------------------------------------------------------- */
    public synchronized void start()
        throws Exception
    {
        // Start filters
        MultiException mex = new MultiException();
        
        try {super.start();}
        catch (Exception e){mex.add(e);}
        
        Iterator iter = _filterMap.values().iterator();
        while (iter.hasNext())
        {
            FilterHolder holder = (FilterHolder)iter.next();
            try{holder.start();}
            catch(Exception e) {mex.add(e);}
        }
        
        Code.debug("Path Filters: "+_pathFilters);
        Code.debug("Servlet Filters: "+_servletFilterMap);

        _started=true;
        
        mex.ifExceptionThrow();
    }
    
    /* ------------------------------------------------------------ */
    public synchronized void stop()
        throws  InterruptedException
    {
        // Stop filters
        try
        {
            Iterator iter = _filterMap.values().iterator();
            while (iter.hasNext())
            {
                FilterHolder holder = (FilterHolder)iter.next();
                holder.stop();
            }
            super.stop();
        }
        finally
        {
            _started=false;
        }
    }
    
    
    /* ------------------------------------------------------------ */
    public void handle(String pathInContext,
                       String pathParams,
                       HttpRequest httpRequest,
                       HttpResponse httpResponse)
         throws IOException
    {
        if (!_started)
            return;
        
        // Handle TRACE
        if (HttpRequest.__TRACE.equals(httpRequest.getMethod()))
        {
            handleTrace(httpRequest,httpResponse);
            return;
        }
        
        // Extract and check filename
        pathInContext=URI.canonicalPath(pathInContext);
        if (pathInContext==null)
            throw new HttpException(HttpResponse.__403_Forbidden);
        
        Code.debug("handle ",httpRequest);

        // Check if this is re-entrant
        ServletHttpRequest request = (ServletHttpRequest) httpRequest.getWrapper();
        ServletHttpResponse response = (ServletHttpResponse) httpResponse.getWrapper();
        boolean reentrant=true;
        if (request==null)
        {
            reentrant=false;
            // Build the request and response.
            request = new ServletHttpRequest(this,pathInContext,httpRequest);
            response = new ServletHttpResponse(request,httpResponse);
            httpRequest.setWrapper(request);
            httpResponse.setWrapper(response);
        }
        else if (request.getPathInContext()==null)
        {
            // Recycled request
            reentrant=false;
            request.recycle(this,pathInContext);
        }

        try
        {
            // protect web-inf and meta-inf
            String pathParamsLC=URI.canonicalPath(StringUtil.asciiToLowerCase(pathInContext));
            if(pathParamsLC.startsWith("/web-inf") || pathParamsLC.startsWith("/meta-inf" ))
            {
                response.sendError(HttpResponse.__403_Forbidden);
                return;
            }
            
            // Look for the servlet
            Map.Entry servlet=getHolderEntry(pathInContext);
            Code.debug("servlet=",servlet);
            
            // Build list of filters
            LazyList filters = null;
            
            // Do the first time through stuff.
            if (!reentrant)
            {
                // Adjust request paths
                if (servlet!=null)
                {
                    String servletPathSpec=(String)servlet.getKey(); 
                    request.setServletPaths(PathMap.pathMatch(servletPathSpec,pathInContext),
                                            PathMap.pathInfo(servletPathSpec,pathInContext),
                                            (ServletHolder)servlet.getValue());
                }
                
                // Handle the session ID
                request.setSessionId(pathParams);
                HttpSession session=request.getSession(false);
                if (session!=null)
                    ((SessionManager.Session)session).access();
                
                Code.debug("session=",session);
                
                // Security Check
                if (!getHttpContext().checkSecurityContstraints
                    (pathInContext,httpRequest,httpResponse))
                return;
                
                // Path filters
                for (int i=0;i<_pathFilters.size();i++)
                {
                    FilterHolder holder=(FilterHolder)_pathFilters.get(i);
                    if (holder.appliesTo(pathInContext))
                        filters=LazyList.add(filters,holder);
                }
                
                // Servlet filters
                if (servlet!=null && _servletFilterMap.size()>0)
                {
                    Object o=_servletFilterMap
                        .get(((ServletHolder)servlet.getValue()).getName());
                    if (o!=null)
                    {
                        if (o instanceof List)
                        filters=LazyList.add(filters,(List)o);
                        else
                            filters=LazyList.add(filters,o);
                    }    
                }
                Code.debug("filters=",filters);
            }
            
            // Do the handling thang
            if (LazyList.size(filters)>0)
            {
                Chain chain=new Chain(pathInContext,filters,servlet);
                chain.doFilter(request,response);
            }
            else
            {
                // Call servlet
                if (servlet!=null)
                {
                    ServletHolder holder = (ServletHolder)servlet.getValue();
                    if (Code.verbose()) Code.debug("call servlet ",holder);
                    holder.handle(request,response);
                }
                else // Not found
                    notFound(request,response);
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

            if (!Code.debug() && th instanceof java.io.IOException)
                Code.warning("Exception for "+httpRequest.getURI()+": "+th);
            else
            {
                Code.warning("Exception for "+httpRequest.getURI(),th);
                Code.debug(httpRequest);
            }
            
            httpResponse.getHttpConnection().forceClose();
            if (!httpResponse.isCommitted())
            {
                request.setAttribute("javax.servlet.error.exception_type",th.getClass());
                request.setAttribute("javax.servlet.error.exception",th);
                response.sendError(th instanceof UnavailableException
                                   ?HttpResponse.__503_Service_Unavailable
                                   :HttpResponse.__500_Internal_Server_Error,
                                   e.getMessage());
            }
            else
                Code.debug("Response already committed for handling ",th);
        }
        catch(Error e)
        {   
            Code.warning("Error for "+httpRequest.getURI(),e);
            Code.debug(httpRequest);
            
            httpResponse.getHttpConnection().forceClose();
            if (!httpResponse.isCommitted())
            {
                request.setAttribute("javax.servlet.error.exception_type",e.getClass());
                request.setAttribute("javax.servlet.error.exception",e);
                response.sendError(HttpResponse.__500_Internal_Server_Error,
                                   e.getMessage());
            }
            else
                Code.debug("Response already committed for handling ",e);
        }
        finally
        {
            httpRequest.setHandled(true);
            response.flushBuffer();
            response.setOutputState(ServletHttpResponse.NO_OUT);
            if (!httpResponse.isCommitted())
                httpResponse.commit();
            request.recycle(null,null);
            response.recycle();
        }
    }
    
    

    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class Chain implements FilterChain
    {
        String _pathInContext;
        int _filter=0;
        LazyList _filters;
        Map.Entry _servlet;

        /* ------------------------------------------------------------ */
        Chain(String pathInContext,
              LazyList filters,
              Map.Entry servlet)
        {
            _pathInContext=pathInContext;
            _filters=filters;
            _servlet=servlet;
        }
        
        /* ------------------------------------------------------------ */
        public void doFilter(ServletRequest request, ServletResponse response)
            throws IOException,
                   ServletException
        {
            if (Code.verbose()) Code.debug("doFilter ",_filter);
            
            // pass to next filter
            if (_filter<LazyList.size(_filters))
            {
                FilterHolder holder = (FilterHolder)LazyList.get(_filters,_filter++);
                if (Code.verbose()) Code.debug("call filter ",holder);
                Filter filter = holder.getFilter();
                filter.doFilter(request,response,this);
                return;
            }

            // Call servlet
            if (_servlet!=null)
            {
                ServletHolder holder = (ServletHolder)_servlet.getValue();
                if (Code.verbose()) Code.debug("call servlet ",holder);
                holder.handle(request,response);
            }
            else // Not found
                notFound((HttpServletRequest)request,
                         (HttpServletResponse)response);
        }
    }
}

