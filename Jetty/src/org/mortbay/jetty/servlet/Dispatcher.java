// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package org.mortbay.jetty.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.HashSet;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import org.mortbay.http.ChunkableOutputStream;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpFields;
import org.mortbay.http.HttpMessage;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.PathMap;
import org.mortbay.http.handler.NullHandler;
import org.mortbay.http.handler.ResourceHandler;
import org.mortbay.util.Code;
import org.mortbay.util.Log;
import org.mortbay.util.MultiMap;
import org.mortbay.util.Resource;
import org.mortbay.util.UrlEncoded;
import org.mortbay.util.URI;
import org.mortbay.util.WriterOutputStream;


/* ------------------------------------------------------------ */
/** Servlet RequestDispatcher.
 * 
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class Dispatcher implements RequestDispatcher
{
    public final static String __REQUEST_URI= "javax.servlet.include.request_uri";
    public final static String __CONTEXT_PATH= "javax.servlet.include.context_path";
    public final static String __SERVLET_PATH= "javax.servlet.include.servlet_path";
    public final static String __PATH_INFO= "javax.servlet.include.path_info";
    public final static String __QUERY_STRING= "javax.servlet.include.query_string";
    
    ServletHandler _servletHandler;
    ServletHolder _holder=null;
    String _pathSpec;
    String _path;
    String _query;
    Resource _resource;
    ResourceHandler _resourceHandler;
    WebApplicationHandler _webAppHandler;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param server 
     * @param URL 
     */
    Dispatcher(ServletHandler servletHandler,
               ResourceHandler resourceHandler,
               String pathInContext,
               String query)
        throws IllegalStateException
    {
        Code.debug("Dispatcher for ",servletHandler,",",pathInContext,",",query);
        
        _servletHandler=servletHandler;
        _resourceHandler=resourceHandler;
        if (_servletHandler instanceof WebApplicationHandler)
        {
            _webAppHandler=(WebApplicationHandler)_servletHandler;
            _resourceHandler=null;
        }
        
        _path=URI.canonicalPath(pathInContext);
        _query=query;

        Map.Entry entry=_servletHandler.getHolderEntry(_path);
        if(entry!=null)
        {
            _pathSpec=(String)entry.getKey();
            _holder = (ServletHolder)entry.getValue();
        }
        else if (_resourceHandler!=null)
        {            
            // Look for a static resource
            try{
                Resource resource=
                    _resourceHandler.getResourceBase().getResource(pathInContext);
                if (resource.exists())
                {
                    _resource=resource;
                    Code.debug("Dispatcher for resource ",_resource);
                }
            }
            catch(IOException e){Code.ignore(e);}
        }
        else if (_webAppHandler!=null)
        {            
            // Look for a static resource
            try{
                Resource resource=
                    _webAppHandler.getResourceBase().getResource(pathInContext);
                if (resource.exists())
                {
                    _resource=resource;
                    Code.debug("Dispatcher for resource ",_resource);
                }
            }
            catch(IOException e){Code.ignore(e);}
        }

        // if no servlet and no resource
        if (_holder==null && _resource==null)
            throw new IllegalStateException("No servlet handlers in context");
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param server 
     * @param URL 
     */
    Dispatcher(ServletHandler servletHandler,String name)
        throws IllegalStateException
    {
        _servletHandler=servletHandler;
        _holder=_servletHandler.getServletHolder(name);
        if (_holder==null)
            throw new IllegalStateException("No named servlet handler in context");
    }

    /* ------------------------------------------------------------ */
    public boolean isNamed()
    {
        return _path==null;
    }

    /* ------------------------------------------------------------ */
    public boolean isResource()
    {
        return _resource!=null;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param request 
     * @param response 
     * @exception ServletException 
     * @exception IOException 
     */
    public void forward(ServletRequest servletRequest,
                        ServletResponse servletResponse)
        throws ServletException,IOException
    {
        dispatch(servletRequest,servletResponse,true);
    }
    
    
    /* ------------------------------------------------------------ */
    /** 
     * @param request 
     * @param response 
     * @exception ServletException 
     * @exception IOException 
     */
    public void include(ServletRequest servletRequest,
                        ServletResponse servletResponse)
        throws ServletException, IOException     
    {
        dispatch(servletRequest,servletResponse,false);
    }
    
    /* ------------------------------------------------------------ */
    private void dispatch(ServletRequest servletRequest,
                          ServletResponse servletResponse,
                          boolean forward)
        throws ServletException,IOException
    {
        HttpServletRequest httpServletRequest=(HttpServletRequest)servletRequest;
        HttpServletResponse httpServletResponse=(HttpServletResponse)servletResponse;
        ServletHttpRequest servletHttpRequest=ServletHttpRequest.unwrap(servletRequest);
        ServletHttpResponse servletHttpResponse=servletHttpRequest.getServletHttpResponse();

        try
        {
            // wrap the request and response
            DispatcherRequest request = new DispatcherRequest(httpServletRequest);
            DispatcherResponse response = new DispatcherResponse(httpServletResponse);

            if (forward)
            {
                // Reset any output done so far.
                servletResponse.resetBuffer();
                servletHttpResponse.resetBuffer();
            }
            else
            {
                response.setLocked(true);
            }
            

            // Merge parameters
            String query=_query;
            MultiMap parameters=null;
            if (query!=null)
            {
                // Add the parameters
                parameters=new MultiMap();
                UrlEncoded.decodeTo(query,parameters);
                request.addParameters(parameters);
            }
            
            if (isNamed())
            {
                // No further modifications required.
                _holder.handle(request,response);
            }
            else if (isResource())
            {
                if (forward)
                {
                    if (_webAppHandler!=null)
                        _webAppHandler.handleGet(_path,_resource,request,response,true);
                    else
                    {
                        HttpRequest httpRequest=servletHttpRequest.getHttpRequest();
                        HttpResponse httpResponse=httpRequest.getHttpResponse();
                        _resourceHandler.handle(_path,null,httpRequest,httpResponse);
                    }
                }
                else
                {
                    OutputStream out=response.getOutputStream();
                    _resource.writeTo(out,0,-1);
                }
            }
            else
            {
                // path based dispatcher
                request.setForwarded(forward);
                request.setIncluded(!forward);
                
                // merge query string
                String oldQ=httpServletRequest.getQueryString();
                if (oldQ!=null && oldQ.length()>0 && parameters!=null)
                {
                    UrlEncoded encoded = new UrlEncoded(oldQ);
                    encoded.putAll(parameters);
                    query=encoded.encode();
                }
                else
                    query=oldQ;
                
                // Adjust servlet paths
                servletHttpRequest.setServletHandler(_servletHandler);
                request.setContextPath(_servletHandler.getHttpContext().getContextPath());
                request.setServletPath(PathMap.pathMatch(_pathSpec,_path));
                request.setPathInfo(PathMap.pathInfo(_pathSpec,_path));
                request.setQueryString(query);
                _holder.handle(request,response);
                
                if (forward)
                {
                    response.flushBuffer();
                    response.close();
                    servletHttpResponse.setOutputState(ServletHttpResponse.DISABLED);
                }
                else if (response.isFlushNeeded())
                    response.flushBuffer();
            }
        }
        finally
        {
        }
    }
        
        

    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class DispatcherRequest extends HttpServletRequestWrapper
    {
        boolean _included;
        boolean _forwarded;
        String _contextPath;
        String _servletPath;
        String _pathInfo;
        String _query;
        MultiMap _parameters;
        
        /* ------------------------------------------------------------ */
        DispatcherRequest(HttpServletRequest request)
        {
            super(request);
        }

        /* ------------------------------------------------------------ */
        void setIncluded(boolean b)
        {
            _included=b;
        }

        /* ------------------------------------------------------------ */
        boolean isIncluded()
        {
            return _included;
        }
        
        /* ------------------------------------------------------------ */
        void setForwarded(boolean b)
        {
            _forwarded=b;
        }

        /* ------------------------------------------------------------ */
        boolean isForwarded()
        {
            return _forwarded;
        }
        
        /* ------------------------------------------------------------ */
        void setContextPath(String s)
        {
            _contextPath=s;
        }
        
        /* ------------------------------------------------------------ */
        public String getContextPath()
        {
            return(_forwarded)?_contextPath:super.getContextPath();
        }
        
        /* ------------------------------------------------------------ */
        void setServletPath(String s)
        {
            _servletPath=s;
        }
        
        /* ------------------------------------------------------------ */
        public String getServletPath()
        {
            return(_forwarded)?_servletPath:super.getServletPath();
        }
        
        /* ------------------------------------------------------------ */
        void setPathInfo(String s)
        {
            _pathInfo=s;
        }
        
        /* ------------------------------------------------------------ */
        public String getPathInfo()
        {
            return(_forwarded)?_pathInfo:super.getPathInfo();
        }
        
        /* ------------------------------------------------------------ */
        void setQueryString(String s)
        {
            _query=s;
        }
        
        /* ------------------------------------------------------------ */
        public String getQueryString()
        {
            return(_forwarded)?_query:super.getQueryString();
        }
        

        /* ------------------------------------------------------------ */
        void addParameters(MultiMap parameters)
        {
            _parameters=parameters;
        }
        
        /* -------------------------------------------------------------- */
        public Enumeration getParameterNames()
        {
            if (_parameters==null)
                return super.getParameterNames();
            
            HashSet set = new HashSet(_parameters.keySet());
            Enumeration e = super.getParameterNames();
            while (e.hasMoreElements())
                set.add(e.nextElement());

            return Collections.enumeration(set);
        }
        
        /* -------------------------------------------------------------- */
        public String getParameter(String name)
        {
            if (_parameters==null)
                return super.getParameter(name);
            String value=_parameters.getString(name);
            if (value!=null)
                return value;
            return super.getParameter(name);
        }
        
        /* -------------------------------------------------------------- */
        public String[] getParameterValues(String name)
        {
            if (_parameters==null)
                return super.getParameterValues(name);
            List values=_parameters.getValues(name);
            if (values!=null)
            {
                String[]a=new String[values.size()];
                return (String[])values.toArray(a);
            }
            return super.getParameterValues(name);
        }
        
        /* -------------------------------------------------------------- */
        public Map getParameterMap()
        {
            if (_parameters==null)
                return super.getParameterMap();
            Map m0 = _parameters.toStringArrayMap();
            Map m1 = super.getParameterMap();
            
            Iterator i = m1.entrySet().iterator();
            while(i.hasNext())
            {
                Map.Entry entry = (Map.Entry)i.next();
                if (!m0.containsKey(entry.getKey()))
                    m0.put(entry.getKey(),entry.getValue());
            }
            return m0;
        }

        /* ------------------------------------------------------------ */
        public void setAttribute(String name, Object o)
        {
            if (_included)
            {
                if (name.equals(__PATH_INFO))
                    _pathInfo=o.toString();
                else if (name.equals(__SERVLET_PATH))
                    _servletPath=o.toString();
                else if (name.equals(__CONTEXT_PATH))
                    _contextPath=o.toString();
                else if (name.equals(__QUERY_STRING))
                    _query=o.toString();
                else
                    super.setAttribute(name,o);
            }
            else
                super.setAttribute(name,o);
        }
        
        /* ------------------------------------------------------------ */
        public Object getAttribute(String name)
        {
            if (name.equals(__PATH_INFO))
                return _included?_pathInfo:null;
            if (name.equals(__REQUEST_URI))
                return _included?URI.addPaths(_contextPath,URI.addPaths(_servletPath,_pathInfo)):null;
            if (name.equals(__SERVLET_PATH))
                return _included?_servletPath:null;
            if (name.equals(__CONTEXT_PATH))
                return _included?_contextPath:null;
            if (name.equals(__QUERY_STRING))
                return _included?_query:null;
            
            return super.getAttribute(name);
        }
        
        /* ------------------------------------------------------------ */
        public Enumeration getAttributeNames()
        {
            HashSet set=new HashSet();
            Enumeration e=super.getAttributeNames();
            while (e.hasMoreElements())
                set.add(e.nextElement());

            if (_included)
            {
                set.add(__PATH_INFO);
                set.add(__REQUEST_URI);
                set.add(__SERVLET_PATH);
                set.add(__CONTEXT_PATH);
                set.add(__QUERY_STRING);
            }
            else
            {
                set.remove(__PATH_INFO);
                set.remove(__REQUEST_URI);
                set.remove(__SERVLET_PATH);
                set.remove(__CONTEXT_PATH);
                set.remove(__QUERY_STRING);
            }
            
            return Collections.enumeration(set);
        }
    }
    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private class DispatcherResponse extends HttpServletResponseWrapper
    {
        private boolean _locked;
        private ServletOutputStream _out=null;
        private PrintWriter _writer=null;
        private boolean _flushNeeded=false;
        
        /* ------------------------------------------------------------ */
        DispatcherResponse(HttpServletResponse response)
        {
            super(response);
        }
        
        /* ------------------------------------------------------------ */
        void setLocked(boolean locked)
        {
            _locked=locked;
        }
        
        /* ------------------------------------------------------------ */
        boolean getLocked()
        {
            return _locked;
        }

        /* ------------------------------------------------------------ */
        public ServletOutputStream getOutputStream()
            throws IOException
        {
            if (_writer!=null)
                throw new IllegalStateException("getWriter called");

            if (_out==null)
            {
                try {_out=super.getOutputStream();}
                catch(IllegalStateException e)
                {
                    Code.ignore(e);
                    _flushNeeded=true;
                    _out=new ServletOut(new WriterOutputStream(super.getWriter()));
                }
            }
            
            return _out;
        }  
      
        /* ------------------------------------------------------------ */
        public PrintWriter getWriter()
            throws IOException
        {
            if (_out!=null)
                throw new IllegalStateException("getOutputStream called");

            if (_writer==null)
            {
                try{_writer=super.getWriter();}
                catch(IllegalStateException e)
                {
                    if (Code.debug()) Code.warning(e);
                    _flushNeeded=true;
                    _writer = new ServletWriter(super.getOutputStream(),
                                                getCharacterEncoding());
                }
            }
            return _writer;
        }

        /* ------------------------------------------------------------ */
        boolean isFlushNeeded()
        {
            return _flushNeeded;
        }
        
        /* ------------------------------------------------------------ */
        public void flushBuffer()
            throws IOException
        {
            if (_writer!=null)
                _writer.flush();
            if (_out!=null)
                _out.flush();
            super.flushBuffer();
        }
        
        /* ------------------------------------------------------------ */
        public void close()
            throws IOException
        {
            if (_writer!=null)
                _writer.close();
            if (_out!=null)
                _out.close();
        }
        
        /* ------------------------------------------------------------ */
        public void setLocale(Locale locale)
        {
            if (!_locked) super.setLocale(locale);
        }
        
        /* ------------------------------------------------------------ */
        public void sendError(int status, String message)
            throws IOException
        {
            if (!_locked) super.sendError(status,message);
        }
        
        /* ------------------------------------------------------------ */
        public void sendError(int status)
            throws IOException
        {
            if (!_locked) super.sendError(status);
        }
        
        /* ------------------------------------------------------------ */
        public void sendRedirect(String url)
            throws IOException
        {
            if (!_locked) super.sendRedirect(url);
        }
        
        /* ------------------------------------------------------------ */
        public void setDateHeader(String name, long value)
        {
            if (!_locked) super.setDateHeader(name,value);
        }
        
        /* ------------------------------------------------------------ */
        public void setHeader(String name, String value)
        {
            if (!_locked) super.setHeader(name,value);
        }
        
        /* ------------------------------------------------------------ */
        public void setIntHeader(String name, int value)
        {
            if (!_locked) super.setIntHeader(name,value);
        }
        
        /* ------------------------------------------------------------ */
        public void addHeader(String name, String value)
        {
            if (!_locked) super.addHeader(name,value);
        }
        
        /* ------------------------------------------------------------ */
        public void addDateHeader(String name, long value)
        {
            if (!_locked) super.addDateHeader(name,value);
        }
        
        /* ------------------------------------------------------------ */
        public void addIntHeader(String name, int value)
        {
            if (!_locked) super.addIntHeader(name,value);
        }
        
        /* ------------------------------------------------------------ */
        public void setStatus(int status)
        {
            if (!_locked) super.setStatus(status);
        }
        
        /* ------------------------------------------------------------ */
        public void setStatus(int status, String message)
        {
            if (!_locked) super.setStatus(status,message);
        }
        
        /* ------------------------------------------------------------ */
        public void setContentLength(int len)
        {
            if (!_locked) super.setContentLength(len);
        }
        
        /* ------------------------------------------------------------ */
        public void setContentType(String contentType)
        {
            if (!_locked) super.setContentType(contentType);
        }
    }
};



