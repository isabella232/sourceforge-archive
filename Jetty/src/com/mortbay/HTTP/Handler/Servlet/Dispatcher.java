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
    String _path;
    String _query;
    
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param server 
     * @param URL 
     */
    Dispatcher(Context context, String path)
    {
	_context = context;
	_path = path;
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
	    
	Code.notImplemented();

	// The path of the new request is the forward path
	// context must be the same, info is recalculate.
	    
	// merge query string

	// pass the request to the new servlet.

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
	    
	Code.notImplemented();

	// Need to ensure that there is no change to the
	// response other than write

	// Request has all original path and info etc.
	// New path is in attributes - whose values are
	// saved to handle chains of includes.

	// javax.servlet.include.request_uri
	// javax.servlet.include.context_path
	// javax.servlet.include.servlet_path
	// javax.servlet.include.path_info
	// javax.servlet.include.query_string

	// merge in query params (but must be reversable)

	// pass request to the servlet -we will have to locate it
	// with new path or name????

	// revert request back to it's old self.
    }
};
