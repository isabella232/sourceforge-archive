// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.Jetty.Servlet;

import com.mortbay.HTTP.HttpFields;
import com.mortbay.HTTP.HttpHandler;
import com.mortbay.HTTP.HandlerContext;
import com.mortbay.HTTP.Handler.ResourceHandler;
import com.mortbay.HTTP.HttpRequest;
import com.mortbay.HTTP.HttpResponse;
import com.mortbay.HTTP.PathMap;
import com.mortbay.Util.Code;
import com.mortbay.Util.MultiMap;
import com.mortbay.Util.Resource;
import com.mortbay.Util.UrlEncoded;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;


/* ------------------------------------------------------------ */
/** Servlet RequestDispatcher.
 * 
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class Dispatcher implements RequestDispatcher
{
    public final static String __REQUEST_URI= "javax.servlet.include.request_uri";
    public final static String __SERVLET_PATH= "javax.servlet.include.servlet_path";
    public final static String __CONTEXT_PATH= "javax.servlet.include.context_path";
    public final static String __QUERY_STRING= "javax.servlet.include.query_string";
    public final static String __PATH_INFO= "javax.servlet.include.path_info";
    
    Context _context;
    HandlerContext _handlerContext;
    ServletHolder _holder=null;
    String _pathSpec;
    String _path;
    String _query;
    Resource _resource;
    ResourceHandler _resourceHandler;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param server 
     * @param URL 
     */
    Dispatcher(Context context, String pathInContext, String query)
        throws IllegalStateException
    {
        Code.debug("Dispatcher for ",context,",",pathInContext,",",query);
        
        _path = Resource.canonicalPath(pathInContext);
        _query=query;
        
        _context = context;
        _handlerContext = _context.getHandler().getHandlerContext();

        for(int i=_handlerContext.getHandlerSize();i-->0;)
        {
            HttpHandler handler = _handlerContext.getHandler(i);
            
            if (handler instanceof ServletHandler)
            {
                // Look for path in servlet handlers
                ServletHandler shandler=(ServletHandler)handler;                
                if (!shandler.isStarted())
                    continue;

                Map.Entry entry=shandler.getHolderEntry(_path);
                if(entry!=null)
                {
                    _pathSpec=(String)entry.getKey();
                    _holder = (ServletHolder)entry.getValue();
                    break;
                }
            }
            else if (handler instanceof ResourceHandler &&
                     _resourceHandler==null)
            {
                // remember resourceHandler as we may need it for a
                // resource forward.
                _resourceHandler=(ResourceHandler)handler;
            }
        }

        // If no servlet found
        if (_holder==null && _resourceHandler!=null)
        {
            // Look for a static resource
            try{
                Resource resource= context.getServletHandler()
                    .getHandlerContext().getBaseResource();
                if (resource!=null)
                    resource = resource.addPath(_path);
                if (resource.exists() && !resource.isDirectory())
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
    Dispatcher(Context context, String name)
        throws IllegalStateException
    {
        _context = context;
        _handlerContext = _context.getHandler().getHandlerContext();

        for(int i=_handlerContext.getHandlerSize();i-->0;)
        {
            if (_handlerContext.getHandler(i) instanceof ServletHandler)
            {
                ServletHandler handler=(ServletHandler)
                    _handlerContext.getHandler(i);
                if (!handler.isStarted())
                    continue;
                _holder=handler.getServletHolder(name);
                break;
            }
        }
        
        if (_holder==null)
            throw new IllegalStateException("No named servlet handler in context");
    }

    
    /* ------------------------------------------------------------ */
    /** 
     * @param request 
     * @param response 
     * @exception ServletException 
     * @exception IOException 
     */
    public void forward(javax.servlet.ServletRequest request,
                        javax.servlet.ServletResponse response)
        throws ServletException,IOException
    {
        ServletRequest servletRequest=(ServletRequest)request;
        HttpRequest httpRequest=servletRequest.getHttpRequest();
        ServletResponse servletResponse=(ServletResponse)response;
        HttpResponse httpResponse=servletResponse.getHttpResponse();
            
        if (servletRequest.getHttpRequest().isCommitted())
            throw new IllegalStateException("Request is committed");
        servletResponse.resetBuffer();
        servletResponse.setOutputState(-1);
        
        // Remove any evidence of previous include
        httpRequest.removeAttribute(__REQUEST_URI);
        httpRequest.removeAttribute(__SERVLET_PATH);
        httpRequest.removeAttribute(__CONTEXT_PATH);
        httpRequest.removeAttribute(__QUERY_STRING);
        httpRequest.removeAttribute(__PATH_INFO);
        httpRequest.removeAttribute(ServletHandler.__SERVLET_PATH);
        
        // merge query params
        if (_query!=null && _query.length()>0)
        {
            MultiMap parameters=new MultiMap();
            UrlEncoded.decodeTo(_query,parameters);
            servletRequest.pushParameters(parameters);
            
            String oldQ=servletRequest.getQueryString();
            if (oldQ!=null && oldQ.length()>0)
            {
                UrlEncoded encoded = new UrlEncoded(oldQ);
                Iterator iter = parameters.entrySet().iterator();
                while(iter.hasNext())
                {
                    Map.Entry entry = (Map.Entry)iter.next();
                    encoded.put(entry.getKey(),entry.getValue());
                }
                _query=encoded.encode(false);
            }
        }
        
        if (_path==null)
        {
            // go direct to named servlet
            _holder.handle(servletRequest,servletResponse);
        }
        else
        {
            // The path of the new request is the forward path
            // context must be the same, info is recalculate.
            if (_pathSpec!=null)
            {
                Code.debug("Forward request to ",_holder,
                           " at ",_pathSpec);
                servletRequest.setForwardPaths(_context,
                                               PathMap.pathMatch(_pathSpec,_path),
                                               PathMap.pathInfo(_pathSpec,_path),
                                               _query);
            }
            
            // Forward request
            //httpRequest.setAttribute(ServletHandler.__SERVLET_REQUEST,request);
            //httpRequest.setAttribute(ServletHandler.__SERVLET_RESPONSE,response);
            httpRequest.setAttribute(ServletHandler.__SERVLET_HOLDER,_holder);
            _context.getHandlerContext().handle(_path,httpRequest,httpResponse);
        }
    }
        
        
    /* ------------------------------------------------------------ */
    /** 
     * @param request 
     * @param response 
     * @exception ServletException 
     * @exception IOException 
     */
    public void include(javax.servlet.ServletRequest request,
                        javax.servlet.ServletResponse response)
        throws ServletException, IOException     
    {
        ServletRequest servletRequest=(ServletRequest)request;
        HttpRequest httpRequest=servletRequest.getHttpRequest();
        ServletResponse servletResponse=(ServletResponse)response;
        HttpResponse httpResponse=servletResponse.getHttpResponse();
            
        // Need to ensure that there is no change to the
        // response other than write
        boolean old_locked = servletResponse.getLocked();
        servletResponse.setLocked(true);
        int old_output_state = servletResponse.getOutputState();
        servletResponse.setOutputState(0);

        // handle static resource
        if (_resource!=null)
        {
            Code.debug("Include resource ",_resource);
            // just call it with existing request/response
            InputStream in = _resource.getInputStream();
            try
            {
                int len = (int)_resource.length();
                httpResponse.getOutputStream().write(in,len);
                return;
            }
            finally
            {
                try{in.close();}catch(IOException e){Code.ignore(e);}
                servletResponse.setLocked(old_locked);
                servletResponse.setOutputState(old_output_state);
            }
        }
        
        // handle named servlet
        if (_pathSpec==null)
        {
            Code.debug("Include named ",_holder);
            // just call it with existing request/response
            try
            {
                _holder.handle(servletRequest,servletResponse);
                return;
            }
            finally
            {
                servletResponse.setLocked(old_locked);
                servletResponse.setOutputState(old_output_state);
            }
        }
        
        // merge query string
        if (_query!=null && _query.length()>0)
        {
            MultiMap parameters=new MultiMap();
            UrlEncoded.decodeTo(_query,parameters);
            servletRequest.pushParameters(parameters);
        }
        
        // Request has all original path and info etc.
        // New path is in attributes - whose values are
        // saved to handle chains of includes.
        
        // javax.servlet.include.request_uri
        Object old_request_uri =
            request.getAttribute(__REQUEST_URI);
        httpRequest.setAttribute(__REQUEST_URI,
                                 servletRequest.getRequestURI());
        
        // javax.servlet.include.context_path
        Object old_context_path =
            request.getAttribute(__CONTEXT_PATH);
        httpRequest.setAttribute(__CONTEXT_PATH,
                                 servletRequest.getContextPath());
        
        // javax.servlet.include.query_string
        Object old_query_string =
            request.getAttribute(__QUERY_STRING);
        httpRequest.setAttribute(__QUERY_STRING,
                                 _query);
        
        // javax.servlet.include.servlet_path
        Object old_servlet_path =
            request.getAttribute(__SERVLET_PATH);
        
        // javax.servlet.include.path_info
        Object old_path_info =
            request.getAttribute(__PATH_INFO);

        // Try each holder until handled.
        try
        {
            // The path of the new request is the forward path
            // context must be the same, info is recalculate.
            Code.debug("Include request to ",_holder,
                       " at ",_pathSpec);
            httpRequest.setAttribute(__SERVLET_PATH,
                                 PathMap.pathMatch(_pathSpec,_path));
            httpRequest.setAttribute(__PATH_INFO,
                                 PathMap.pathInfo(_pathSpec,_path));
                
            // try service request
            _holder.handle(servletRequest,servletResponse);
        }
        finally
        {
            // revert request back to it's old self.
            servletResponse.setLocked(old_locked);
            servletResponse.setOutputState(old_output_state);
            if (_query!=null && _query.length()>0)
                servletRequest.popParameters();
            httpRequest.setAttribute(__REQUEST_URI,old_request_uri);
            httpRequest.setAttribute(__CONTEXT_PATH,old_context_path);
            httpRequest.setAttribute(__QUERY_STRING,old_query_string);
            httpRequest.setAttribute(__SERVLET_PATH,old_servlet_path);
            httpRequest.setAttribute(__PATH_INFO,old_path_info);
        }
    }
};



