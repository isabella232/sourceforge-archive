// ========================================================================
// $Id$
// Copyright 1996-2004 Mort Bay Consulting Pty. Ltd.
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

package com.acme;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Request;
import org.mortbay.util.ajax.Continuation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* ------------------------------------------------------------ */
/** Dump Servlet Request.
 * 
 */
public class TestRetry extends HttpServlet
{
    private static final long serialVersionUID = -1293398142223438970L;

    private static Logger log = LoggerFactory.getLogger(TestRetry.class);

    /* ------------------------------------------------------------ */
    String pageType;

    /* ------------------------------------------------------------ */
    public void init(ServletConfig config) throws ServletException
    {
    	super.init(config);
    }

    /* ------------------------------------------------------------ */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        doGet(request, response);
    }

    /* ------------------------------------------------------------ */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        
        System.err.println("\ndoGet");
        Continuation continuation = Request.getRequest(request).getContinuation(true);
        System.err.println("continuation="+continuation+" isNew"+continuation.isNew());
        
        Object o = continuation.getEvent(5000L);
        if (o==null)
        {
            // timeout
            PrintWriter pout= response.getWriter();
            pout.write("<h1>Retry Servlet - timeout</h1>\n");
            
        }
        else
        {
            PrintWriter pout= response.getWriter();
            pout.write("<h1>Retry Servlet - "+o+"</h1>\n");
        }
          
    }

    /* ------------------------------------------------------------ */
    public String getServletInfo()
    {
        return "Retry Servlet";
    }

    /* ------------------------------------------------------------ */
    public synchronized void destroy()
    {
        log.debug("Destroyed");
    }

    /* ------------------------------------------------------------ */
    private String getURI(HttpServletRequest request)
    {
        String uri= (String)request.getAttribute("javax.servlet.forward.request_uri");
        if (uri == null)
            uri= request.getRequestURI();
        return uri;
    }

    /* ------------------------------------------------------------ */
    private static String toString(Object o)
    {
        if (o == null)
            return null;

        if (o.getClass().isArray())
        {
            StringBuffer sb= new StringBuffer();
            Object[] array= (Object[])o;
            for (int i= 0; i < array.length; i++)
            {
                if (i > 0)
                    sb.append("\n");
                sb.append(array.getClass().getComponentType().getName());
                sb.append("[");
                sb.append(i);
                sb.append("]=");
                sb.append(toString(array[i]));
            }
            return sb.toString();
        }
        else
            return o.toString();
    }

}
