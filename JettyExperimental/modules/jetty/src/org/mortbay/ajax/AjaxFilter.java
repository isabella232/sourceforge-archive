//========================================================================
//$Id$
//Copyright 2004 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.mortbay.ajax;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.util.StringUtil;

public class AjaxFilter implements Filter
{
    ServletContext context;

    public void init(FilterConfig filterConfig) throws ServletException
    {
        context=filterConfig.getServletContext();
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the context.
     */
    public ServletContext getContext()
    {
        return context;
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        String[] method=request.getParameterValues("ajax");
        
        if (method!=null && method.length>0)
        {
            HttpServletRequest srequest = (HttpServletRequest)request;
            HttpServletResponse sresponse = (HttpServletResponse) response;
            
            StringWriter sout = new StringWriter();
            PrintWriter out = new PrintWriter(sout);
            
            out.println("<ajax-response>");
            AjaxResponse aResponse =new AjaxResponse(srequest,out);
            
            for (int i=0;i<method.length;i++)
                handle(method[i],srequest,aResponse);

            out.println("</ajax-response>");
            byte[] ajax = sout.toString().getBytes("UTF-8");
            sresponse.setContentType("text/xml; charset=UTF-8");
            sresponse.setContentLength(ajax.length);
            sresponse.getOutputStream().write(ajax);
            sresponse.flushBuffer();
            
        }
        else
            chain.doFilter(request, response);
    }

    public void handle(String method,HttpServletRequest request,AjaxResponse response)
    {    
        response.elementResponse(null, "<span class=\"error\">No implementation for "+method+" "+request.getParameter("member")+"</span>");
    }
    
    public void destroy()
    {
        context=null;
    }

    public static String encodeText(String s)
    {
        // TODO - much better implementation of this needed
        s=StringUtil.replace(s, "<", "&lt;");
        s=StringUtil.replace(s, ">", "&gt;");
        s=StringUtil.replace(s, "&", "&amp;");
        return s;
    }
    
    public static class AjaxResponse
    {
        private HttpServletRequest request;
        private PrintWriter out;
        private AjaxResponse(HttpServletRequest request,PrintWriter out)
        {this.out=out; this.request=request;}
        
        public void elementResponse(String id,String element)
        {
            if (id==null)
                id = request.getParameter("id");
            if (id==null)
                id="unknown";
            out.println("<response type=\"element\" id=\""+id+"\">"+element+"</response>");
        }
        
        public void objectResponse(String id,String element)
        {
            if (id==null)
                id = request.getParameter("id");
            if (id==null)
                id="unknown";
            
            out.println("<response type=\"object\" id=\""+id+"\">"+element+"</response>");
        }
    }
}
