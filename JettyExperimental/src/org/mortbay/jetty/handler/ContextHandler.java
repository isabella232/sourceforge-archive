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

package org.mortbay.jetty.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.HttpRequest;

/* ------------------------------------------------------------ */
/** ContextHandler.
 * 
 * This handler wraps a call to handle by setting the context and
 * servlet path, plus setting the context classloader.
 * 
 * @author gregw
 *
 */
public class ContextHandler extends AbstractHandlerCollection
{
    private String _contextPath;
    private ClassLoader _classLoader;

    /* ------------------------------------------------------------ */
    /**
     * 
     */
    public ContextHandler()
    {
        super();
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the _contextPath.
     */
    public String getContextPath()
    {
        return _contextPath;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param _contextPath The _contextPath to set.
     */
    public void setContextPath(String contextPath)
    {
        _contextPath = contextPath;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the classLoader.
     */
    public ClassLoader getClassLoader()
    {
        return _classLoader;
    }
    /* ------------------------------------------------------------ */
    /**
     * @param classLoader The classLoader to set.
     */
    public void setClassLoader(ClassLoader classLoader)
    {
        _classLoader = classLoader;
    }
    
    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.jetty.Handler#handle(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public boolean handle(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException
    {
        boolean handled=false;
        HttpRequest http_request=null;
        String old_context_path=null;
        String old_servlet_path=null;
        ClassLoader old_classloader=null;
        Thread current_thread=null;
        
        try
        {
            // Update context path and servlet path
            if (_contextPath!=null)
            {
                http_request=(request instanceof HttpRequest)?(HttpRequest)request:HttpConnection.getCurrentConnection().getHttpRequest();
                old_context_path=http_request.getContextPath();
                old_servlet_path=http_request.getServletPath();
                http_request.setContextPath(_contextPath);
                if (old_servlet_path.startsWith(_contextPath))
                    http_request.setServletPath(old_servlet_path.substring(_contextPath.length()));
            }
            
            // Set the classloader
            if (_classLoader!=null)
            {
                current_thread=Thread.currentThread();
                old_classloader=current_thread.getContextClassLoader();
                current_thread.setContextClassLoader(_classLoader);
            }
            
            handled = super.handle(request, response);
            
        }
        finally
        {
            // reset the classloader
            if (_classLoader!=null)
            {
                current_thread.setContextClassLoader(old_classloader);
            }
            
            // reset the context and servlet path.
            if (_contextPath!=null)
            {
                http_request.setContextPath(old_context_path);
                http_request.setServletPath(old_servlet_path);              
            }
        }
        return handled;
    }
}
