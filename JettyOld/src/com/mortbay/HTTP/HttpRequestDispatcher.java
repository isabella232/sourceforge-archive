// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;

import com.mortbay.Base.Code;
import com.mortbay.Util.URI;
import java.io.*;
import javax.servlet.*;

/* ------------------------------------------------------------ */
/** Dispatch requests to other resources.
 * IT IS NOT RECOMMENDED THAT THIS API IS USED.
 * The resources that can be referenced by the RequestDispatcher
 * API, are very restricted and cannot be written as generic servlet.
 *
 * @see HttpServer
 * @version 1.0 Sat Feb 13 1999
 * @author Greg Wilkins (gregw)
 */
public class HttpRequestDispatcher implements javax.servlet.RequestDispatcher
{

    HttpServer _server;
    URI _uri;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param server 
     * @param URL 
     */
    HttpRequestDispatcher(HttpServer server, String uri)
    {
        _server = server;
	_uri = new URI(uri);
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
	req.getHttpResponse().preDispatch();
        req.setResourcePath(_uri);
        _server.handle(req,res);
        req.getHttpResponse().postDispatchForward();  
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
	Code.debug("Include ",_uri.getPath());
	req.getHttpResponse().preDispatch();
        req.setResourcePath(_uri);
        _server.handle(req,res);
	req.getHttpResponse().postDispatchInclude();
        req.setResourcePath((URI)null);
    }
};





