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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.ugli.LoggerFactory;
import org.apache.ugli.ULogger;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.resource.Resource;
import org.mortbay.util.LogSupport;
import org.mortbay.util.URIUtil;


/* ------------------------------------------------------------ */
/** ContextHandler.
 * 
 * This handler wraps a call to handle by setting the context and
 * servlet path, plus setting the context classloader.
 * 
 * @author gregw
 *
 */
public class ContextHandler extends WrappedHandler
{
    private static ULogger log = LoggerFactory.getLogger(HttpConnection.class);
    private static ThreadLocal __context=new ThreadLocal();
    
    /* ------------------------------------------------------------ */
    /** Get the current ServletContext implementation.
     * This call is only valid during a call to doStart and is available to
     * nested handlers to access the context.
     * 
     * @return ServletContext implementation
     */
    public static Context getCurrentContext()
    {
        Context context = (Context)__context.get();
        if (context==null)
            throw new IllegalStateException("Only valid during call to doStart()");
        return context;
    }
    
    private HashMap _attributes;
    private ClassLoader _classLoader;
    private Context _context;
    private String _contextPath;
    private HashMap _initParams;
    private String _servletContextName;
    private Resource _resourceBase;  
    private String _docRoot;
    
    /* ------------------------------------------------------------ */
    /**
     * 
     */
    public ContextHandler()
    {
        super();
        _context=new Context();
        _attributes=new HashMap();
        _initParams=new HashMap();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletContext#getAttribute(java.lang.String)
     */
    public Object getAttribute(String name)
    {
        return _attributes.get(name);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletContext#getAttributeNames()
     */
    public Enumeration getAttributeNames()
    {
        return Collections.enumeration(_attributes.keySet());
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the attributes.
     */
    public HashMap getAttributes()
    {
        return _attributes;
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
     * @return Returns the _contextPath.
     */
    public String getContextPath()
    {
        return _contextPath;
    }
   
    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletContext#getInitParameter(java.lang.String)
     */
    public String getInitParameter(String name)
    {
        return (String)_initParams.get(name);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletContext#getInitParameterNames()
     */
    public Enumeration getInitParameterNames()
    {
        return Collections.enumeration(_initParams.keySet());
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the initParams.
     */
    public HashMap getInitParams()
    {
        return _initParams;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletContext#getServletContextName()
     */
    public String getServletContextName()
    {
        return _servletContextName;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.thread.AbstractLifeCycle#doStart()
     */
    protected void doStart() throws Exception
    {
        ClassLoader old_classloader=null;
        Thread current_thread=null;
        Object old_context=null;
        
        try
        {
            // Set the classloader
            if (_classLoader!=null)
            {
                current_thread=Thread.currentThread();
                old_classloader=current_thread.getContextClassLoader();
                current_thread.setContextClassLoader(_classLoader);
            }
            
            old_context=__context.get();
            __context.set(_context);
            
            super.doStart();
            
        }
        finally
        {
            __context.set(old_context);
            
            // reset the classloader
            if (_classLoader!=null)
            {
                current_thread.setContextClassLoader(old_classloader);
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.thread.AbstractLifeCycle#doStop()
     */
    protected void doStop() throws Exception
    {
        ClassLoader old_classloader=null;
        Thread current_thread=null;
        
        try
        {
            // Set the classloader
            if (_classLoader!=null)
            {
                current_thread=Thread.currentThread();
                old_classloader=current_thread.getContextClassLoader();
                current_thread.setContextClassLoader(_classLoader);
            }
            
            super.doStop();
            
        }
        finally
        {
            // reset the classloader
            if (_classLoader!=null)
            {
                current_thread.setContextClassLoader(old_classloader);
            }
        }
    }
    
    /* ------------------------------------------------------------ */
    /* 
     * @see org.mortbay.jetty.Handler#handle(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public boolean handle(HttpServletRequest request, HttpServletResponse response, int dispatch)
            throws IOException, ServletException
    {
        boolean handled=false;
        Request http_request=null;
        Context old_context=null;
        String old_path_info=null;
        ClassLoader old_classloader=null;
        Thread current_thread=null;
        
        try
        {
            http_request=(request instanceof Request)?(Request)request:HttpConnection.getCurrentConnection().getRequest();
            old_context=http_request.getContext();
            old_path_info=http_request.getPathInfo();
            
            http_request.setContext(_context);
            
            // TODO - maybe only for REQUEST type ?
            if (old_path_info.startsWith(_contextPath))
                http_request.setPathInfo(old_path_info.substring(_contextPath.length()));
            
            // Set the classloader
            if (_classLoader!=null)
            {
                current_thread=Thread.currentThread();
                old_classloader=current_thread.getContextClassLoader();
                current_thread.setContextClassLoader(_classLoader);
            }
            
            handled = super.handle(request, response, dispatch);
            
        }
        finally
        {
            // reset the classloader
            if (_classLoader!=null)
            {
                current_thread.setContextClassLoader(old_classloader);
            }
            
            // reset the context and servlet path.
            http_request.setContext(old_context);
            http_request.setPathInfo(old_path_info); 
        }
        return handled;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletContext#removeAttribute(java.lang.String)
     */
    public void removeAttribute(String name)
    {
        _attributes.remove(name);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletContext#setAttribute(java.lang.String, java.lang.Object)
     */
    public void setAttribute(String name, Object object)
    {
        _attributes.put(name,object);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param attributes The attributes to set.
     */
    public void setAttributes(HashMap attributes)
    {
        _attributes = attributes;
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
    /**
     * @param _contextPath The _contextPath to set.
     */
    public void setContextPath(String contextPath)
    {
        _contextPath = contextPath;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param initParams The initParams to set.
     */
    public void setInitParams(HashMap initParams)
    {
        _initParams = initParams;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param servletContextName The servletContextName to set.
     */
    public void setServletContextName(String servletContextName)
    {
        _servletContextName = servletContextName;
    }
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the resourceBase.
     */
    public String getResourceBase()
    {
        if (_resourceBase==null)
            return null;
        return _resourceBase.toString();
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param resourceBase The resourceBase to set.
     */
    public void setResourceBase(String resourceBase) 
    {
        try
        {
            _docRoot=null;
            _resourceBase = Resource.newResource(resourceBase);
            File file=_resourceBase.getFile();
            if (file!=null)
            {
                _docRoot=file.getCanonicalPath();
                if (_docRoot.endsWith(File.pathSeparator))
                    _docRoot=_docRoot.substring(0,_docRoot.length()-1);
            }
        }
        catch (Exception e)
        {
            log.warn(e);
            throw new IllegalArgumentException(resourceBase);
        }
    }
    
    /* ------------------------------------------------------------ */
    /** Context.
     * @author gregw
     *
     */
    public class Context implements ServletContext
    {

        /* ------------------------------------------------------------ */
        private Context()
        {}

        /* ------------------------------------------------------------ */
        /* 
         * @see javax.servlet.ServletContext#getContext(java.lang.String)
         */
        public ServletContext getContext(String uripath)
        {
            // TODO Auto-generated method stub
            return null;
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see javax.servlet.ServletContext#getMajorVersion()
         */
        public int getMajorVersion()
        {
            return 2;
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see javax.servlet.ServletContext#getMimeType(java.lang.String)
         */
        public String getMimeType(String file)
        {
            // TODO Auto-generated method stub
            return null;
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see javax.servlet.ServletContext#getMinorVersion()
         */
        public int getMinorVersion()
        {
            return 4;
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see javax.servlet.ServletContext#getNamedDispatcher(java.lang.String)
         */
        public RequestDispatcher getNamedDispatcher(String name)
        {
            // TODO Auto-generated method stub
            return null;
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see javax.servlet.ServletContext#getRealPath(java.lang.String)
         */
        public String getRealPath(String path)
        {
            if (_docRoot==null)
                return null;
            
            if (path==null)
                return null;
            path=URIUtil.canonicalPath(path);
            
            if (!path.startsWith("/"))
                path="/"+path;
            path.replace('/', File.pathSeparatorChar);
            
            return _docRoot+path;
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see javax.servlet.ServletContext#getRequestDispatcher(java.lang.String)
         */
        public RequestDispatcher getRequestDispatcher(String path)
        {
            // TODO Auto-generated method stub
            return null;
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see javax.servlet.ServletContext#getResource(java.lang.String)
         */
        public URL getResource(String path) throws MalformedURLException
        {
            if (path==null || !path.startsWith("/"))
                throw new MalformedURLException(path);
            
            if (_resourceBase==null)
                return null;
            
            try
            {
                Resource resource=_resourceBase.addPath(URIUtil.canonicalPath(path));
                return resource.getURL();
            }
            catch(Exception e)
            {
                LogSupport.ignore(log,e);
                return null;
            }
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see javax.servlet.ServletContext#getResourceAsStream(java.lang.String)
         */
        public InputStream getResourceAsStream(String path)
        {
            try
            {
                URL url=getResource(path);
                if (url==null)
                    return null;
                return url.openStream();
            }
            catch(Exception e)
            {
                LogSupport.ignore(log,e);
                return null;
            }
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see javax.servlet.ServletContext#getResourcePaths(java.lang.String)
         */
        public Set getResourcePaths(String path)
        {
            if (path==null || !path.startsWith("/"))
                return Collections.EMPTY_SET;
            
            if (_resourceBase==null)
                return null;
            
            try
            {
                Resource resource=_resourceBase.addPath(URIUtil.canonicalPath(path));
                String[] l=resource.list();
                if (l==null)
                {
                    HashSet set = new HashSet();
                    for(int i=0;i<l.length;i++)
                        set.add(l[i]);
                    return set;
                }   
            }
            catch(Exception e)
            {
                LogSupport.ignore(log,e);
            }
            return Collections.EMPTY_SET;
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see javax.servlet.ServletContext#getServerInfo()
         */
        public String getServerInfo()
        {
            // TODO Auto-generated method stub
            return "JettyE";
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see javax.servlet.ServletContext#getServlet(java.lang.String)
         */
        public Servlet getServlet(String name) throws ServletException
        {
            return null;
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see javax.servlet.ServletContext#getServletNames()
         */
        public Enumeration getServletNames()
        {
            return Collections.enumeration(Collections.EMPTY_LIST);
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see javax.servlet.ServletContext#getServlets()
         */
        public Enumeration getServlets()
        {
            return Collections.enumeration(Collections.EMPTY_LIST);
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see javax.servlet.ServletContext#log(java.lang.Exception, java.lang.String)
         */
        public void log(Exception exception, String msg)
        {
            // TODO better logging
            log.info(msg,exception);
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see javax.servlet.ServletContext#log(java.lang.String)
         */
        public void log(String msg)
        {
            log.info(msg);
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see javax.servlet.ServletContext#log(java.lang.String, java.lang.Throwable)
         */
        public void log(String message, Throwable throwable)
        {
            log.info(message,throwable);
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see javax.servlet.ServletContext#getInitParameter(java.lang.String)
         */
        public String getInitParameter(String name)
        {
            return ContextHandler.this.getInitParameter(name);
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see javax.servlet.ServletContext#getInitParameterNames()
         */
        public Enumeration getInitParameterNames()
        {
            return ContextHandler.this.getInitParameterNames();
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see javax.servlet.ServletContext#getAttribute(java.lang.String)
         */
        public Object getAttribute(String name)
        {
            return ContextHandler.this.getAttribute(name);
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see javax.servlet.ServletContext#getAttributeNames()
         */
        public Enumeration getAttributeNames()
        {
            return ContextHandler.this.getAttributeNames();
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see javax.servlet.ServletContext#setAttribute(java.lang.String, java.lang.Object)
         */
        public void setAttribute(String name, Object object)
        {
            ContextHandler.this.setAttribute(name,object);
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see javax.servlet.ServletContext#removeAttribute(java.lang.String)
         */
        public void removeAttribute(String name)
        {
            ContextHandler.this.removeAttribute(name);
        }

        /* ------------------------------------------------------------ */
        /* 
         * @see javax.servlet.ServletContext#getServletContextName()
         */
        public String getServletContextName()
        {
            return ContextHandler.this.getServletContextName();
        }

        /* ------------------------------------------------------------ */
        /**
         * @return Returns the _contextPath.
         */
        public String getContextPath()
        {
            return _contextPath;
        }

    }

}
