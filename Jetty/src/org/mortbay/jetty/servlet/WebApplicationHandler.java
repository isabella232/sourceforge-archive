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
    private static String __AllowString="GET, HEAD, POST, OPTION, TRACE";
    
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
            
            // Look for the servlet and/or resource
            Map.Entry servlet=getHolderEntry(pathInContext);
            Resource resource=null;
            if (servlet==null)
                resource=getHttpContext().getResource(pathInContext);
            
            Code.debug("servlet=",servlet,"  resource=",resource);
            
            
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
                Chain chain=new Chain(pathInContext,filters,servlet,resource);
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
                
                // Do resource
                else if (resource!=null)
                    serve(pathInContext,resource,request,response);
                // Not found
                else
                    notFound(request,response);
            }
        }
        catch(Exception e)
        {
            System.err.println("Exception "+e);
            
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
            System.err.println("Error "+e);
            
            Code.warning("Servlet Error for "+httpRequest.getURI(),e);
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
    public void serve(String pathInContext,
                      Resource resource,
                      ServletRequest request,
                      ServletResponse response)
        throws ServletException,IOException
    {
        if (request instanceof HttpServletRequest)
        {
            HttpServletRequest httpServletRequest=(HttpServletRequest)request;
            HttpServletResponse httpServletResponse=(HttpServletResponse)response;
            serve(pathInContext,resource,httpServletRequest,httpServletResponse);
        }
        else
        {
            // Handle non HTTP request
            Code.notImplemented();
        }
    }
    
    /* ------------------------------------------------------------ */
    private void setAllowHeader(HttpServletResponse response)
    {
        if (response!=null)
            response.setHeader(HttpFields.__Allow, __AllowString);
    }
    
    /* ------------------------------------------------------------ */
    public void serve(String pathInContext,
                      Resource resource,
                      HttpServletRequest request,
                      HttpServletResponse response)
        throws ServletException,IOException
    {
        Code.debug("Server ",resource);
        
        boolean endsWithSlash= pathInContext.endsWith("/");
        String method=request.getMethod();
        
        // Is the method allowed?        
        if (__AllowString.indexOf(method)<0)
        {
            Code.debug("Method not allowed: ",method);
            if (resource.exists())
            {
                setAllowHeader(response);
                response.sendError(HttpResponse.__405_Method_Not_Allowed);
            }
            return;
        }

        // Handle the request
        try
        {
            Code.debug("PATH=",pathInContext,
                       " RESOURCE=",resource);
            
            // check filename
            if (method.equals(HttpRequest.__GET) ||
                method.equals(HttpRequest.__POST) ||
                method.equals(HttpRequest.__HEAD))
                handleGet(pathInContext,resource,request, response,endsWithSlash);  
            else if (method.equals(HttpRequest.__OPTIONS))
                // handleOptions(response,pathInContext);
                Code.notImplemented();
            else if (method.equals(HttpRequest.__TRACE))
                // handleTrace(request, response);
                Code.notImplemented();
            else
            {
                Code.debug("Unknown action:"+method);
                // anything else...
                try{
                    if (resource.exists())
                        response.sendError(HttpResponse.__501_Not_Implemented);
                    else
                        notFound(request,response);
                }
                catch(Exception e) {Code.ignore(e);}
            }
        }
        catch(IllegalArgumentException e)
        {
            Code.ignore(e);
        }
        finally
        {
            if (resource!=null && !(resource instanceof CachedResource))
                resource.release();
        }
    }
    
    /* ------------------------------------------------------------ */
    /* Check modification date headers.
     */
    private boolean passConditionalHeaders(HttpServletRequest request,
                                           HttpServletResponse response,
                                           Resource resource)
        throws IOException
    {
        if (!request.getMethod().equals(HttpRequest.__HEAD))
        {
            // check any modified headers.
            long date=0;
            
            if ((date=request.getDateHeader(HttpFields.__IfUnmodifiedSince))>0)
            {
                if (resource.lastModified() > date)
                {
                    response.sendError(HttpResponse.__412_Precondition_Failed);
                    return false;
                }
            }
            
            if ((date=request.getDateHeader(HttpFields.__IfModifiedSince))>0)
            {
                if (resource.lastModified() <= date)
                {
                    response.sendError(HttpResponse.__304_Not_Modified);
                    return false;
                }
            }
        }
        return true;
    }
 
    /* ------------------------------------------------------------------- */
    public void handleGet(String pathInContext,
                          Resource resource,
                          HttpServletRequest request,
                          HttpServletResponse response,
                          boolean endsWithSlash)
        throws ServletException,IOException
    {
        Code.debug("handleGet ",resource);
 
        if (!resource.exists())
            notFound(request,response);
        else
        {
            // Check modified dates
            if (!passConditionalHeaders(request,response,resource))
                return;            
     
            // check if directory
            if (resource.isDirectory())
            {
                if (!endsWithSlash && !pathInContext.equals("/"))
                {
                    Code.debug("Redirect to directory/");
                    
                    String q=request.getQueryString();
                    StringBuffer buf=request.getRequestURL();
                    if (q!=null&&q.length()!=0)
                    {
                        buf.append('?');
                        buf.append(q);
                    }
                    response.setHeader(HttpFields.__Location, URI.addPaths(buf.toString(),"/"));
                    response.sendError(302);
                    return;
                }
  
                // See if index file exists
                String welcome=getHttpContext().getWelcomeFile(resource);
                if (welcome!=null)
                {     
                    // Forward to the index
                    String ipath=URI.addPaths(pathInContext,welcome);
                    RequestDispatcher dispatcher=getRequestDispatcher(ipath);
                    dispatcher.forward(request,response);
                    return;
                }

                // If we got here, no forward to index took place
                sendDirectory(request,response,resource,pathInContext.length()>1);
            }
            // check if it is a file
            else if (resource.exists())
                sendData(request,response,resource,true);
            else
                // don't know what it is
                Code.warning("Unknown file type");
        }
    }

    /* ------------------------------------------------------------ */
    void sendData(HttpServletRequest request,
                  HttpServletResponse response,
                  Resource resource,
                  boolean writeHeaders)
        throws IOException
    {
        long resLength=resource.length();
        
        //  see if there are any range headers
        Enumeration reqRanges = request.getHeaders(HttpFields.__Range);
        
        if (!writeHeaders || reqRanges == null || !reqRanges.hasMoreElements())
        {
            //  if there were no ranges, send entire entity
            if (writeHeaders)
                writeHeaders(response,resource,resLength);
            OutputStream out = response.getOutputStream();
            resource.writeTo(out,0,resLength);            
            return;
        }
            
        // Parse the satisfiable ranges
        List ranges =InclusiveByteRange.satisfiableRanges(reqRanges,resLength);
        if (Code.debug())
            Code.debug("ranges: " + reqRanges + " == " + ranges);
        
        //  if there are no satisfiable ranges, send 416 response
        if (ranges==null || ranges.size()==0)
        {
            Code.debug("no satisfiable ranges");
            writeHeaders(response, resource, resLength);
            response.setStatus(HttpResponse.__416_Requested_Range_Not_Satisfiable,
                               "Requested Range Not Satisfiable");
            response.setHeader(HttpFields.__ContentRange, 
                               InclusiveByteRange.to416HeaderRangeString(resLength));
            
            OutputStream out = response.getOutputStream();
            resource.writeTo(out,0,resLength);
            return;
        }

        
        //  if there is only a single valid range (must be satisfiable 
        //  since were here now), send that range with a 216 response
        if ( ranges.size()== 1)
        {
            InclusiveByteRange singleSatisfiableRange =
                (InclusiveByteRange)ranges.get(0);
            Code.debug("single satisfiable range: " + singleSatisfiableRange);
            long singleLength = singleSatisfiableRange.getSize(resLength);
            writeHeaders(response,resource,singleLength);
            response.setStatus(HttpResponse.__206_Partial_Content,"Partial Content");
            response.setHeader(HttpFields.__ContentRange, 
                               singleSatisfiableRange.toHeaderRangeString(resLength));
            OutputStream out = response.getOutputStream();
            resource.writeTo(out,singleSatisfiableRange.getFirst(resLength),singleLength);
            return;
        }
        
        
        //  multiple non-overlapping valid ranges cause a multipart
        //  216 response which does not require an overall 
        //  content-length header
        //
        HttpContext.ResourceMetaData metaData =
            (HttpContext.ResourceMetaData)resource.getAssociate();
        String encoding = metaData.getEncoding();
        MultiPartResponse multi = new MultiPartResponse(response.getOutputStream());
        response.setStatus(HttpResponse.__206_Partial_Content,"Partial Content");

	// If the request has a "Request-Range" header then we need to
	// send an old style multipart/x-byteranges Content-Type. This
	// keeps Netscape and acrobat happy. This is what Apache does.
	String ctp;
	if (request.getHeader(HttpFields.__RequestRange)!=null)
	    ctp = "multipart/x-byteranges; boundary=";
	else
	    ctp = "multipart/byteranges; boundary=";
	response.setContentType(ctp+multi.getBoundary());

        InputStream in=(resource instanceof CachedResource)
            ?null:resource.getInputStream();
        OutputStream out = response.getOutputStream();
        long pos=0;
            
        for (int i=0;i<ranges.size();i++)
        {
            InclusiveByteRange ibr = (InclusiveByteRange) ranges.get(i);
            String header=HttpFields.__ContentRange+": "+
                ibr.toHeaderRangeString(resLength);
            Code.debug("multi range: ",encoding," ",header);
            multi.startPart(encoding,new String[]{header});

            long start=ibr.getFirst(resLength);
            long size=ibr.getSize(resLength);
            if (in!=null)
            {
                // Handle non cached resource
                if (start<pos)
                {
                    in.close();
                    in=resource.getInputStream();
                    pos=0;
                }
                if (pos<start)
                {
                    in.skip(start-pos);
                    pos=start;
                }
                IO.copy(in,out,size);
                pos+=size;
            }
            else
                // Handle cached resource
                ((CachedResource)resource).writeTo(out,start,size);
            
        }
        if (in!=null)
            in.close();
        multi.close();
        
        return;
    }

    /* ------------------------------------------------------------ */
    void writeHeaders(HttpServletResponse response,
                      Resource resource,
                      long count)
        throws IOException
    {
        HttpContext.ResourceMetaData metaData =
            (HttpContext.ResourceMetaData)resource.getAssociate();

        response.setContentType(metaData.getEncoding());
        if (count != -1)
        {
            if (count==resource.length())
                response.setHeader(HttpFields.__ContentLength,metaData.getLength());
            else
                response.setContentLength((int)count);
        }

        response.setHeader(HttpFields.__LastModified,metaData.getLastModified());
        
        if (_acceptRanges)
            response.setHeader(HttpFields.__AcceptRanges,"bytes");
    }

    /* ------------------------------------------------------------------- */
    void sendDirectory(HttpServletRequest request,
                       HttpServletResponse response,
                       Resource resource,
                       boolean parent)
        throws IOException
    {
        Code.debug("sendDirectory: "+resource);
        String base = URI.addPaths(request.getRequestURI(),"/");
        ByteArrayISO8859Writer dir = getHttpContext()
            .getDirectoryListing(resource,base,parent);
        if (dir==null)
        {
            response.sendError(HttpResponse.__403_Forbidden,
                               "No directory");
            return;
        }
        
        response.setContentType("text/html");
        response.setContentLength(dir.length());
        
        if (!request.getMethod().equals(HttpRequest.__HEAD))
            dir.writeTo(response.getOutputStream());
    }

    
    /* ------------------------------------------------------------ */
    public void notFound(ServletRequest request,
                         ServletResponse response)
        throws IOException
    {
        if (request instanceof HttpServletRequest)
        {
            HttpServletRequest httpServletRequest=(HttpServletRequest)request;
            HttpServletResponse httpServletResponse=(HttpServletResponse)response;
            notFound(httpServletRequest,httpServletResponse);
        }
    }
    
    /* ------------------------------------------------------------ */
    public void notFound(HttpServletRequest request,
                         HttpServletResponse response)
        throws IOException
    {
        Code.debug("Not Found ",request.getRequestURI());
        String method=request.getMethod();
            
        // Not found special requests.
        if (method.equals(HttpRequest.__GET)    ||
            method.equals(HttpRequest.__HEAD)   ||
            method.equals(HttpRequest.__POST))
        {
            response.sendError(HttpResponse.__404_Not_Found,request.getRequestURI()+" Not Found");
        }
        
        else if (method.equals(HttpRequest.__OPTIONS))
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
        else
        {
            // Unknown METHOD
            response.setHeader(HttpFields.__Allow,__AllowString);
            response.sendError(HttpResponse.__405_Method_Not_Allowed);
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
        Resource _resource;

        /* ------------------------------------------------------------ */
        Chain(String pathInContext,
              LazyList filters,
              Map.Entry servlet,
              Resource resource)
        {
            _pathInContext=pathInContext;
            _filters=filters;
            _servlet=servlet;
            _resource=resource;
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
            
            // Do resource
            else if (_resource!=null)
                serve(_pathInContext,_resource,request,response);

            // Not found
            else
                notFound(request,response);
        }
    }
}

