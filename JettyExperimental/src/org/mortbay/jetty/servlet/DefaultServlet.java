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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;
import org.slf4j.ULogger;
import org.mortbay.io.Buffer;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.IO;
import org.mortbay.io.WriterOutputStream;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.HttpFields;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.HttpMethods;
import org.mortbay.jetty.InclusiveByteRange;
import org.mortbay.jetty.MultiPartResponse;
import org.mortbay.jetty.ResourceCache;
import org.mortbay.jetty.Response;
import org.mortbay.jetty.ResourceCache.Entry;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.resource.Resource;
import org.mortbay.resource.ResourceFactory;
import org.mortbay.util.LogSupport;
import org.mortbay.util.URIUtil;



/* ------------------------------------------------------------ */
/** The default servlet.                                                 
 * This servlet, normally mapped to /, provides the handling for static 
 * content, OPTION and TRACE methods for the context.                   
 * The following initParameters are supported:                          
 * <PRE>                                                                      
 *   acceptRanges     If true, range requests and responses are         
 *                    supported                                         
 *                                                                      
 *   dirAllowed       If true, directory listings are returned if no    
 *                    welcome file is found. Else 403 Forbidden.        
 *
 *   redirectWelcome  If true, welcome files are redirected rather than
 *                    forwarded to.
 *
 *   minGzipLength    If set to a positive integer, then static content
 *                    larger than this will be served as gzip content encoded
 *                    if a matching resource is found ending with ".gz"
 *
 *  resourceBase      Set to replace the context resource base
 *
 *  relativeResourceBase    
 *                    Set with a pathname relative to the base of the
 *                    servlet context root. Useful for only serving static content out
 *                    of only specific subdirectories.
 * </PRE>
 *                                                               
 * The MOVE method is allowed if PUT and DELETE are allowed             
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class DefaultServlet extends HttpServlet implements ResourceFactory
{
    private static ULogger log = LoggerFactory.getLogger(DefaultServlet.class);
    
    private ContextHandler.Context _context;
    private ServletHandler _servletHandler;
    private String _AllowString="GET, POST, HEAD, OPTIONS, TRACE";
    
    private long _maxSizeBytes=1024;
    private long _maxSizeBuffer=256*1024;
        
    private boolean _acceptRanges=true;
    private boolean _dirAllowed;
    private boolean _redirectWelcomeFiles;
    private int _minGzipLength=-1;
    private Resource _resourceBase;
    private ResourceCache _cache;
    
    
    /* ------------------------------------------------------------ */
    public void init()
    throws UnavailableException
    {
        ServletContext config=getServletContext();
        _context = (ContextHandler.Context)config;
        
        _acceptRanges=getInitBoolean("acceptRanges");
        _dirAllowed=getInitBoolean("dirAllowed");
        _redirectWelcomeFiles=getInitBoolean("redirectWelcome");
        _minGzipLength=getInitInt("minGzipLength");
        
        String rrb = getInitParameter("relativeResourceBase");
        if (rrb!=null)
        {
            try
            {
                _resourceBase=Resource.newResource(_context.getResource("/")).addPath(rrb);
            }
            catch (Exception e) 
            {
                log.warn(LogSupport.EXCEPTION,e);
                throw new UnavailableException(e.toString()); 
            }
        }
        
        String rb=getInitParameter("resourceBase");
        if (rrb != null && rb != null)
            throw new  UnavailableException("resourceBase & relativeResourceBase");    
        
        if (rb!=null)
        {
            try{_resourceBase=Resource.newResource(rb);}
            catch (Exception e) {
                log.warn(LogSupport.EXCEPTION,e);
                throw new UnavailableException(e.toString()); 
            }
        }
        
        try
        {
            if (_resourceBase==null)
                _resourceBase=Resource.newResource(_context.getResource("/"));
        }
        catch (Exception e) 
        {
            log.warn(LogSupport.EXCEPTION,e);
            throw new UnavailableException(e.toString()); 
        }
        
        if (log.isDebugEnabled()) log.debug("resource base = "+_resourceBase);
    }
    
    /* ------------------------------------------------------------ */
    private boolean getInitBoolean(String name)
    {
        String value=getInitParameter(name);
        return value!=null && value.length()>0 &&
        (value.startsWith("t")||
                value.startsWith("T")||
                value.startsWith("y")||
                value.startsWith("Y")||
                value.startsWith("1"));
    }
    
    /* ------------------------------------------------------------ */
    private int getInitInt(String name)
    {
        String value=getInitParameter(name);
        if (value!=null && value.length()>0)
            return Integer.parseInt(value);
        return -1;
    }
    
    /* ------------------------------------------------------------ */
    /** get Resource to serve.
     * Map a path to a resource. The default implementation calls
     * HttpContext.getResource but derived servlets may provide
     * their own mapping.
     * @param pathInContext The path to find a resource for.
     * @return The resource to serve.
     */
    public Resource getResource(String pathInContext)
    {
        Resource r=null;
        try
        {
            r = _resourceBase.addPath(pathInContext);
            if (log.isDebugEnabled()) log.debug("RESOURCE="+r);
        }
        catch (IOException e)
        {
            LogSupport.ignore(log, e);
        }
        return r;
    }
    
    /* ------------------------------------------------------------ */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    	throws ServletException, IOException
    {
        boolean include=false;
        String servletPath=(String)request.getAttribute(Dispatcher.__INCLUDE_SERVLET_PATH);
        String pathInfo=null;
        if (servletPath==null)
        {
            servletPath=request.getServletPath();
            pathInfo=request.getPathInfo();
        }
        else
        {
            include=true;
            pathInfo=(String)request.getAttribute(Dispatcher.__INCLUDE_PATH_INFO);
        }
        
        String pathInContext=URIUtil.addPaths(servletPath,pathInfo);
        boolean endsWithSlash=false; 
        
        Resource resource=null;
        ResourceCache.Entry cache=null;
        MetaData metaData=null;
        
        // Find the resource and metadata
        try
        {   
            if (_cache==null)
                resource=getResource(pathInContext);
            else
            {
                cache=_cache.lookup(pathInContext,this);
              
                if (metaData!=null)
                {
                    resource=cache.getResource();
                    metaData=(MetaData)cache.getValue();
                }
                else
                    resource=getResource(pathInContext);
            }
            
            if (resource==null || !resource.exists())
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            else if (resource.isDirectory())
            {
                endsWithSlash=pathInContext.endsWith("/");
                String welcome=null;
                
                if (!endsWithSlash && !pathInContext.equals("/"))
                {
                    String q=request.getQueryString();
                    StringBuffer buf=request.getRequestURL();
                    if (q!=null&&q.length()!=0)
                    {
                        buf.append('?');
                        buf.append(q);
                    }
                    response.setContentLength(0);
                    response.sendRedirect(response.encodeRedirectURL(URIUtil.addPaths(buf.toString(),"/")));
                }
                // else look for a welcome file
                else if (null!=(welcome=getWelcomeFile(resource)))
                {
                    String ipath=URIUtil.addPaths(pathInContext,welcome);
                    if (_redirectWelcomeFiles)
                    {
                        // Redirect to the index
                        response.setContentLength(0);
                        response.sendRedirect(URIUtil.addPaths( _context.getContextPath(),ipath));
                    }
                    else
                    {
                        // Forward to the index
                        RequestDispatcher dispatcher=_servletHandler.getRequestDispatcher(ipath);
                        if (dispatcher!=null)
                            dispatcher.forward(request,response);
                    }
                }
                else if (passConditionalHeaders(request,response,resource,metaData=checkMetaData(metaData,pathInContext,resource)))
                    // If we got here, no forward to index took place
                    sendDirectory(request,response,resource,pathInContext.length()>1);
            }
            else if (passConditionalHeaders(request,response,resource,metaData))    
                // just send it
                sendData(request,response,pathInContext,include,resource,metaData=checkMetaData(metaData,pathInContext,resource));
            
        }
        catch(IllegalArgumentException e)
        {
            LogSupport.ignore(log,e);
        }
        finally
        {
            if (cache!=null && metaData!=null)
            {
                synchronized(metaData)
                {
                    if (!metaData.isValid())
                        cache.setValue(metaData);
                }
            }
            if (metaData!=null)
                metaData.release();
            else if (resource!=null)
                resource.release();
        }
        
    }

    /* ------------------------------------------------------------ */
    private MetaData checkMetaData(MetaData metaData,String pathInContext, Resource resource)
    {
        if (metaData==null)
        {
            metaData=new MetaData(resource);
            String mime_type=_context.getMimeType(pathInContext);
            if (mime_type!=null) metaData.setMimeType(new ByteArrayBuffer(mime_type));
            

          
            ResourceCache.Entry gzcached=_cache.lookup(pathInContext+".gz",null);
     
        }
        return metaData;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param resource
     * @return
     */
    private String getWelcomeFile(Resource resource)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* ------------------------------------------------------------ */
    /* Check modification date headers.
     */
    protected boolean passConditionalHeaders(HttpServletRequest request,HttpServletResponse response,Resource resource,MetaData metaData)
    throws IOException
    {
        if (!request.getMethod().equals(HttpMethods.HEAD) && request.getAttribute(Dispatcher.__INCLUDE_REQUEST_URI)==null)
        {
            if (metaData!=null)
            {
                String ifms=request.getHeader(HttpHeaders.IF_MODIFIED_SINCE);
                String mdlm=metaData.getLastModified().toString();
                if (ifms!=null && mdlm!=null && ifms.equals(mdlm))
                {
                    response.reset();
                    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    response.flushBuffer();
                    return false;
                }
            }

            // Parse the if[un]modified dates and compare to resource
            long date=0;
            
            if ((date=request.getDateHeader(HttpHeaders.IF_UNMODIFIED_SINCE))>0)
            {
                if (resource.lastModified()/1000 > date/1000)
                {
                    response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
                    return false;
                }
            }
            
            if ((date=request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE))>0)
            {
                if (resource.lastModified()/1000 <= date/1000)
                {
                    response.reset();
                    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    response.flushBuffer();
                    return false;
                }
            }
        }
        return true;
    }
    
    
    /* ------------------------------------------------------------------- */
    protected void sendDirectory(HttpServletRequest request,
            HttpServletResponse response,
            Resource resource,
            boolean parent)
    throws IOException
    {
        if (!_dirAllowed)
        {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        
        byte[] data=null;
        String base = URIUtil.addPaths(request.getRequestURI(),"/");
        String dir = resource.getListHTML(base,parent);
        if (dir==null)
        {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
            "No directory");
            return;
        }
        data=dir.getBytes("UTF-8");
        response.setContentType("text/html; charset=UTF-8");
        response.setContentLength(data.length);
        
        if (!request.getMethod().equals(HttpMethods.HEAD))
            // TODO - maybe a better way?
            response.getOutputStream().write(data);
    }
    
    
    /* ------------------------------------------------------------ */
    protected void sendData(HttpServletRequest request,
            				HttpServletResponse response,
            				String pathInContext,
            	            boolean include,
            				Resource resource,
            				MetaData metaData)
    throws IOException
    {
        Resource content=resource;
        long content_length=resource.length();
        Buffer cached;
        
        // Get the output stream (or writer)
        OutputStream out =null;
        try{out = response.getOutputStream();}
        catch(IllegalStateException e) {out = new WriterOutputStream(response.getWriter());}
        
  
        // see if there are any range headers
        Enumeration reqRanges = include?null:request.getHeaders(HttpHeaders.RANGE);
        
        if ( reqRanges == null || !reqRanges.hasMoreElements())
        {
            //  if there were no ranges, send entire entity
            Resource data=resource;
            if (include)
            {
                data.writeTo(out,0,content_length);
            }
            else
            {
                // look for a gziped content.
                if (_minGzipLength>0)
                {
                    String accept=request.getHeader(HttpHeaders.ACCEPT_ENCODING);
                    if (accept!=null && accept.indexOf("gzip")>=0 &&
                        !include && content_length>_minGzipLength )
                    {
                        /* 
                            response.setHeader(HttpHeaders.CONTENT_ENCODING,"gzip");
                         */
                    }
                }
                
                
                // See if a short direct method can be used?
                if (!(out instanceof HttpConnection.Output))
                {
                    ((HttpConnection.Output)out).sendContent(metaData);
                }
                else if (cached !=null)
                {
                    writeHeaders(response,metaData,content_length);
                    cached.writeTo(out);
                }
                else
                {
                    // Write content normally
                    writeHeaders(response,metaData,content_length);
                    data.writeTo(out,0,content_length);
                }
            }
        }
        else
        {
            
            // Parse the satisfiable ranges
            List ranges =InclusiveByteRange.satisfiableRanges(reqRanges,content_length);
            
            //  if there are no satisfiable ranges, send 416 response
            if (ranges==null || ranges.size()==0)
            {
                writeHeaders(response, resource,cached, content_length);
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                response.setHeader(HttpHeaders.CONTENT_RANGE, 
                        InclusiveByteRange.to416HeaderRangeString(content_length));
                resource.writeTo(out,0,content_length);
                return;
            }
            
            
            //  if there is only a single valid range (must be satisfiable 
            //  since were here now), send that range with a 216 response
            if ( ranges.size()== 1)
            {
                InclusiveByteRange singleSatisfiableRange =
                    (InclusiveByteRange)ranges.get(0);
                long singleLength = singleSatisfiableRange.getSize(content_length);
                writeHeaders(response,resource,cached,singleLength);
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                response.setHeader(HttpHeaders.CONTENT_RANGE, 
                        singleSatisfiableRange.toHeaderRangeString(content_length));
                resource.writeTo(out,singleSatisfiableRange.getFirst(content_length),singleLength);
                return;
            }
            
            
            //  multiple non-overlapping valid ranges cause a multipart
            //  216 response which does not require an overall 
            //  content-length header
            //
            writeHeaders(response,resource,cached,-1);
            String mimetype = cached!=null?cached.getMimeType().toString():_context.getMimeType(pathInContext);
            MultiPartResponse multi = new MultiPartResponse(response.getOutputStream());
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            
            // If the request has a "Request-Range" header then we need to
            // send an old style multipart/x-byteranges Content-Type. This
            // keeps Netscape and acrobat happy. This is what Apache does.
            String ctp;
            if (request.getHeader(HttpHeaders.REQUEST_RANGE)!=null)
                ctp = "multipart/x-byteranges; boundary=";
            else
                ctp = "multipart/byteranges; boundary=";
            response.setContentType(ctp+multi.getBoundary());
            
            InputStream in=resource.getInputStream();
            long pos=0;
            
            for (int i=0;i<ranges.size();i++)
            {
                InclusiveByteRange ibr = (InclusiveByteRange) ranges.get(i);
                String header=HttpHeaders.CONTENT_RANGE+": "+
                ibr.toHeaderRangeString(content_length);
                multi.startPart(mimetype,new String[]{header});
                
                long start=ibr.getFirst(content_length);
                long size=ibr.getSize(content_length);
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
                    (resource).writeTo(out,start,size);
                
            }
            if (in!=null)
                in.close();
            multi.close();
        }
        return;
    }
    
    /* ------------------------------------------------------------ */
    protected void writeHeaders(HttpServletResponse response,MetaData metaData,long count)
    throws IOException
    {
        response.setContentType(metaData.getMimeType().toString());
        response.setHeader(HttpHeaders.LAST_MODIFIED,metaData.getLastModified().toString());
       

        if (count != -1)
        {
            if (response instanceof Response)
                ((Response)response).setLongContentLength(count);
            else if (count<Integer.MAX_VALUE)
                response.setContentLength((int)count);
            else 
                response.setHeader(HttpHeaders.CONTENT_LENGTH,""+count);
        }
        
        if (_acceptRanges)
            response.setHeader(HttpHeaders.ACCEPT_RANGES,"bytes");
    }
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /** MetaData associated with a context Resource.
     */
    public class MetaData implements ResourceCache.Value
    {
        Resource _resource;
        String _name;
        long _lastModified;
        Buffer _lastModifiedBytes;
        Buffer _mimeType;
        boolean _valid;
        
        MetaData(Resource resource)
        {
            _resource=resource;
            _name=_resource.toString();
            _lastModified=resource.lastModified();
            _lastModifiedBytes=new ByteArrayBuffer(HttpFields.formatDate(_resource.lastModified(),false));
        }
        
        public Resource getResource()
        {
            return _resource;
        }
        
        public Buffer getLastModified()
        {
            return _lastModifiedBytes;
        }

        public Buffer getMimeType()
        {
            return _mimeType;
        }
        
        public void setMimeType(Buffer type)
        {
            _mimeType=type;
        }

        public void validate()
        {
            synchronized(this)
            {
                _valid=true;
            }
        }
        
        public void invalidate()
        {
            synchronized(this)
            {
                _valid=false;
            }
        }
        
        public void release()
        {
            synchronized(this)
            {
                if (!_valid)
                {
                    _resource.release();
                }
            }
        }
        
        public boolean isValid()
        {
            synchronized(this)
            {
                return _valid;
            }
        }
    }
}
