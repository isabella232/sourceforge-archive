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

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.util.StringMap;
import org.slf4j.LoggerFactory;
import org.slf4j.ULogger;

/* ------------------------------------------------------------ */
/** Servlet RequestDispatcher.
 * 
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class Dispatcher implements RequestDispatcher
{
    private static ULogger log = LoggerFactory.getLogger(DefaultServlet.class);
    
    /** Dispatch include attribute names */
    public final static String __INCLUDE_REQUEST_URI= "javax.servlet.include.request_uri";
    public final static String __INCLUDE_CONTEXT_PATH= "javax.servlet.include.context_path";
    public final static String __INCLUDE_SERVLET_PATH= "javax.servlet.include.servlet_path";
    public final static String __INCLUDE_PATH_INFO= "javax.servlet.include.path_info";
    public final static String __INCLUDE_QUERY_STRING= "javax.servlet.include.query_string";

    /** Dispatch include attribute names */
    public final static String __FORWARD_REQUEST_URI= "javax.servlet.forward.request_uri";
    public final static String __FORWARD_CONTEXT_PATH= "javax.servlet.forward.context_path";
    public final static String __FORWARD_SERVLET_PATH= "javax.servlet.forward.servlet_path";
    public final static String __FORWARD_PATH_INFO= "javax.servlet.forward.path_info";
    public final static String __FORWARD_QUERY_STRING= "javax.servlet.forward.query_string";

    
    public final static StringMap __managedAttributes = new StringMap();
    static
    {
        __managedAttributes.put(__INCLUDE_REQUEST_URI,__INCLUDE_REQUEST_URI);
        __managedAttributes.put(__INCLUDE_CONTEXT_PATH,__INCLUDE_CONTEXT_PATH);
        __managedAttributes.put(__INCLUDE_SERVLET_PATH,__INCLUDE_SERVLET_PATH);
        __managedAttributes.put(__INCLUDE_PATH_INFO,__INCLUDE_PATH_INFO);
        __managedAttributes.put(__INCLUDE_QUERY_STRING,__INCLUDE_QUERY_STRING);
        
        __managedAttributes.put(__FORWARD_REQUEST_URI,__FORWARD_REQUEST_URI);
        __managedAttributes.put(__FORWARD_CONTEXT_PATH,__FORWARD_CONTEXT_PATH);
        __managedAttributes.put(__FORWARD_SERVLET_PATH,__FORWARD_SERVLET_PATH);
        __managedAttributes.put(__FORWARD_PATH_INFO,__FORWARD_PATH_INFO);
        __managedAttributes.put(__FORWARD_QUERY_STRING,__FORWARD_QUERY_STRING);
    }

    /* ------------------------------------------------------------ */
    /** Dispatch type from name
     */
    public static int type(String type)
    {
        if ("request".equalsIgnoreCase(type))
            return Handler.REQUEST;
        if ("forward".equalsIgnoreCase(type))
            return Handler.FORWARD;
        if ("include".equalsIgnoreCase(type))
            return Handler.INCLUDE;
        if ("error".equalsIgnoreCase(type))
            return Handler.ERROR;
        throw new IllegalArgumentException(type);
    }


    /* ------------------------------------------------------------ */
    private ContextHandler _contextHandler;
    private String _uri;
    private String _path;
    private String _query;
    
    /* ------------------------------------------------------------ */
    /**
     * @param contextHandler
     * @param uriInContext
     * @param pathInContext
     * @param query
     */
    public Dispatcher(ContextHandler contextHandler, String uri, String pathInContext, String query)
    {
        _contextHandler=contextHandler;
        _uri=uri;
        _path=pathInContext;
        _query=query;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.RequestDispatcher#forward(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
     */
    public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException
    {
        Request base_request=(request instanceof Request)?((Request)request):HttpConnection.getCurrentConnection().getRequest();
        
        String old_uri=base_request.getRequestURI();
        try
        {
            base_request.setRequestURI(_uri);
            
            _contextHandler.handle(_path, (HttpServletRequest)request, (HttpServletResponse)response, Handler.FORWARD);
        }
        finally
        {
            base_request.setRequestURI(old_uri);
        }
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.RequestDispatcher#include(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
     */
    public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException
    {
        // TODO Auto-generated method stub
        
    }


};
