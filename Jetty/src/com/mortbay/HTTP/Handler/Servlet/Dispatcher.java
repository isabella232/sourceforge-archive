// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler.Servlet;

import com.mortbay.HTTP.HttpFields;
import com.mortbay.HTTP.HandlerContext;
import com.mortbay.HTTP.HttpRequest;
import com.mortbay.HTTP.PathMap;
import com.mortbay.Util.Code;
import com.mortbay.Util.MultiMap;
import com.mortbay.Util.UrlEncoded;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;


/* ------------------------------------------------------------ */
/**
 * 
 * @version 1.0 Mon Aug 21 2000
 * @author Greg Wilkins (gregw)
 */
public class Dispatcher implements RequestDispatcher
{
    Context _context;
    HandlerContext _handlerContext;
    ServletHolder _holder=null;
    String _pathSpec;
    String _path;
    String _query;
    
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param server 
     * @param URL 
     */
    Dispatcher(Context context, String pathInContext, String query)
        throws IllegalStateException
    {
        _path = pathInContext;
        _query=query;
        
        _context = context;
        _handlerContext = _context.getHandler().getHandlerContext();

        for(int i=_handlerContext.getHandlerSize();i-->0;)
        {
            if (_handlerContext.getHandler(i) instanceof ServletHandler)
            {
                ServletHandler handler=(ServletHandler)
                    _handlerContext.getHandler(i);

                Map.Entry entry=handler.getHolderEntry(_path);
                if(entry!=null)
                {
                    _pathSpec=(String)entry.getKey();
                    _holder = (ServletHolder)entry.getValue();
                    break;
                }
            }
        }
        
        if (_holder==null)
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
        ServletResponse servletResponse=(ServletResponse)response;
            
        if (servletRequest.getHttpRequest().isCommitted())
            throw new IllegalStateException("Request is committed");
        servletResponse.reset();

        // Remove any evidence of previous include
        request.removeAttribute( "javax.servlet.include.request_uri");
        request.removeAttribute( "javax.servlet.include.servlet_path");
        request.removeAttribute( "javax.servlet.include.context_path");
        request.removeAttribute( "javax.servlet.include.query_string");
        request.removeAttribute( "javax.servlet.include.path_info");


        // handle named servlet
        if (_pathSpec==null)
        {
            // just call it with existing request/response
            _holder.handle(servletRequest,servletResponse);
            return;
        }
        
        // merge query string
        if (_query!=null && _query.length()>0)
        {
            MultiMap parameters=new MultiMap(servletRequest.getParameters());
            UrlEncoded.decodeTo(_query,parameters);
            servletRequest.setParameters(parameters);

            String oldQ=servletRequest.getQueryString();
            if (oldQ!=null && oldQ.length()>0)
                _query=oldQ+'&'+_query;
        }

        // The path of the new request is the forward path
        // context must be the same, info is recalculate.
        Code.debug("Forward request to ",_holder,
                   " at ",_pathSpec);
        servletRequest.setForwardPaths(_context,
                                       PathMap.pathMatch(_pathSpec,_path),
                                       PathMap.pathInfo(_pathSpec,_path),
                                       _query);
            
        // try service request
        _holder.handle(servletRequest,servletResponse);
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
        ServletResponse servletResponse=(ServletResponse)response;
            
        // Request has all original path and info etc.
        // New path is in attributes - whose values are
        // saved to handle chains of includes.
        
        // Need to ensure that there is no change to the
        // response other than write
        boolean old_locked = servletResponse.getLocked();
        servletResponse.setLocked(true);
        int old_output_state = servletResponse.getOutputState();
        servletResponse.setOutputState(0);

        // handle named servlet
        if (_pathSpec==null)
        {
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
        MultiMap old_parameters=null;
        if (_query!=null && _query.length()>0)
        {
            old_parameters = servletRequest.getParameters();
            MultiMap parameters=new MultiMap(old_parameters);
            UrlEncoded.decodeTo(_query,parameters);
            servletRequest.setParameters(parameters);
        }
        
        // javax.servlet.include.request_uri
        Object old_request_uri =
            request.getAttribute("javax.servlet.include.request_uri");
        request.setAttribute("javax.servlet.include.request_uri",
                             servletRequest.getRequestURI());
        
        // javax.servlet.include.context_path
        Object old_context_path =
            request.getAttribute("javax.servlet.include.context_path");
        request.setAttribute("javax.servlet.include.context_path",
                             servletRequest.getContextPath());
        
        // javax.servlet.include.query_string
        Object old_query_string =
            request.getAttribute("javax.servlet.include.query_string");
        request.setAttribute("javax.servlet.include.query_string",
                             _query);
        
        // javax.servlet.include.servlet_path
        Object old_servlet_path =
            request.getAttribute("javax.servlet.include.servlet_path");
        
        // javax.servlet.include.path_info
        Object old_path_info =
            request.getAttribute("javax.servlet.include.path_info");

        // Try each holder until handled.
        try
        {
            // The path of the new request is the forward path
            // context must be the same, info is recalculate.
            Code.debug("Include request to ",_holder,
                       " at ",_pathSpec);
            request.setAttribute("javax.servlet.include.servlet_path",
                                 PathMap.pathMatch(_pathSpec,_path));
            request.setAttribute("javax.servlet.include.path_info",
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
                servletRequest.setParameters(old_parameters);
            request.setAttribute("javax.servlet.include.request_uri",
                                 old_request_uri);
            request.setAttribute("javax.servlet.include.context_path",
                                 old_context_path);
            request.setAttribute("javax.servlet.include.query_string",
                                 old_query_string);
            request.setAttribute("javax.servlet.include.servlet_path",
                                 old_servlet_path);
            request.setAttribute("javax.servlet.include.path_info",
                                 old_path_info);
        }
    }
};



