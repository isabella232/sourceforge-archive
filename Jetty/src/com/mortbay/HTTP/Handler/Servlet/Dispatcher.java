// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler.Servlet;
//import com.sun.java.util.collections.*; XXX-JDK1.1

import com.mortbay.HTTP.Handler.NullHandler;
import com.mortbay.HTTP.*;
import com.mortbay.Util.*;
import java.io.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.*;
import java.net.*;


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
    List _holders=new ArrayList(8);
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

		List matches=handler.holderMatches(_path);
		_holders.addAll(matches);
	    }
	}
	
	if (_holders.size()==0)
	    throw new IllegalStateException("No servlet handlers in context");
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

	// Try each holder in turn until request is handled.
	for (int i=0;i<_holders.size();i++)
	{
	    Map.Entry entry =
		(Map.Entry)_holders.get(i);
	    
	    // The path of the new request is the forward path
	    // context must be the same, info is recalculate.
	    String servletPathSpec=(String)entry.getKey();
	    ServletHolder holder = (ServletHolder)entry.getValue();
	    
	    Code.debug("Try forward request to ",entry);
	    servletRequest.setPaths(PathMap.pathMatch(servletPathSpec,
						      _path),
				    PathMap.pathInfo(servletPathSpec,
						     _path),
				    _query);
	    
	    // try service request
	    holder.handle(servletRequest,servletResponse);
	    
	    // Break if the response has been updated
	    if (servletResponse.isDirty())
	    {
		Code.debug("Forwarded to ",entry);
		break;
	    }
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
	    for (int i=0;i<_holders.size();i++)
	    {
		Map.Entry entry =
		    (Map.Entry)_holders.get(i);
		
		// The path of the new request is the forward path
		// context must be the same, info is recalculate.
		String servletPathSpec=(String)entry.getKey();
		ServletHolder holder = (ServletHolder)entry.getValue();
	    
		Code.debug("Try forward request to ",entry);
		request.setAttribute("javax.servlet.include.servlet_path",
				     PathMap.pathMatch(servletPathSpec,
						       _path));
		request.setAttribute("javax.servlet.include.path_info",
				     PathMap.pathInfo(servletPathSpec,
						      _path));
		
		// try service request
		holder.handle(servletRequest,servletResponse);
	    
		// Break if the response has been updated
		if (servletResponse.isDirty())
		{
		    Code.debug("Included from ",entry);
		    break;
		}
	    }
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


