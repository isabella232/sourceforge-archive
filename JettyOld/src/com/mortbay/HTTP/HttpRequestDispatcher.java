// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;

import com.mortbay.Base.Code;
import java.io.*;
import javax.servlet.*;

public class HttpRequestDispatcher implements javax.servlet.RequestDispatcher
{

    HttpServer _server;
    String _path;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param server 
     * @param URL 
     */
    HttpRequestDispatcher(HttpServer server, String path)
    {
	_server = server;
	_path = path;
    }

    
    /* ------------------------------------------------------------ */
    /** 
     * @param request 
     * @param response 
     * @exception ServletException 
     * @exception IOException 
     */
    public void forward(ServletRequest request,
			ServletResponse response)
	throws ServletException,IOException
    {
	HttpRequest req = (HttpRequest) request;
	HttpResponse res = (HttpResponse) response;
	req.setRequestPath(_path);
	_server.handle(req,res);
    }
    

    /* ------------------------------------------------------------ */
    /** 
     * @param request 
     * @param response 
     * @exception ServletException 
     * @exception IOException 
     */
    public void include(ServletRequest request,
			ServletResponse response)
	throws ServletException, IOException     
    {
	HttpRequest req = (HttpRequest) request;
	HttpResponse res = (HttpResponse) response;
	req.setResourcePath(_path);
	_server.handle(req,res);
    }
    
};

