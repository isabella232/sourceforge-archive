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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.io.Buffer;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.MimeTypes;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.servlet.Dispatcher;
import org.mortbay.resource.Resource;
import org.mortbay.util.Loader;
import org.mortbay.util.LogSupport;
import org.mortbay.util.URIUtil;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


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
    private static Logger log = LoggerFactory.getLogger(ContextHandler.class);
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
    private String _docRoot;
    private Resource _baseResource;  
    private MimeTypes _mimeTypes;
    
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
            

            if (_mimeTypes==null)
                _mimeTypes=new MimeTypes();
            
            old_context=__context.get();
            __context.set(_context);
            
            startContext();
            
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
    protected void startContext()
    	throws Exception
    {
        super.doStart();
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
    public boolean handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch)
            throws IOException, ServletException
    {
        boolean handled=false;
        Request base_request=null;
        Context old_context=null;
        String old_context_path=null;
        String old_servlet_path=null;
        String old_path_info=null;
        ClassLoader old_classloader=null;
        Thread current_thread=null;
        
        
        try
        {
            base_request=(request instanceof Request)?(Request)request:HttpConnection.getCurrentConnection().getRequest();
            old_context=base_request.getContext();
            old_context_path=base_request.getContextPath();
            old_servlet_path=base_request.getServletPath();
            old_path_info=base_request.getPathInfo();

            // Are we already in this context?
            if (old_context!=_context)
            {
                
                // Nope - so check the target.
                if (dispatch==REQUEST)
                {
                    if (target.startsWith(_contextPath))
                        target=target.substring(_contextPath.length());
                    else 
                    {
                        // Not for this context!
                        old_context=_context;
                        return false;
                    }
                }
                
                // Update the paths
                base_request.setContext(_context);
                if (dispatch!=INCLUDE && target.startsWith("/"))
                {
                    base_request.setContextPath(_context.getContextPath());
                    base_request.setServletPath(null);
                    base_request.setPathInfo(target);
                }
                
                // Set the classloader
                if (_classLoader!=null)
                {
                    current_thread=Thread.currentThread();
                    old_classloader=current_thread.getContextClassLoader();
                    current_thread.setContextClassLoader(_classLoader);
                }
            }
            
            handled = super.handle(target, request, response, dispatch);
            
        }
        finally
        {
            if (old_context!=_context)
            {
                // reset the classloader
                if (_classLoader!=null)
                {
                    current_thread.setContextClassLoader(old_classloader);
                }
                
                // reset the context and servlet path.
                base_request.setContext(old_context);
                base_request.setContextPath(old_context_path);
                base_request.setServletPath(old_servlet_path);
                base_request.setPathInfo(old_path_info); 
            }
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
    public Resource getBaseResource()
    {
        if (_baseResource==null)
            return null;
        return _baseResource;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the base resource as a string.
     */
    public String getResourceBase()
    {
        if (_baseResource==null)
            return null;
        return _baseResource.toString();
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param resourceBase The resourceBase to set.
     */
    public void setBaseResource(Resource base) 
    {
        _baseResource=base;
        _docRoot=null;
        
        try
        {
            File file=_baseResource.getFile();
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
            throw new IllegalArgumentException(base.toString());
        }
    }

    /* ------------------------------------------------------------ */
    /**
     * @param resourceBase The base resource as a string.
     */
    public void setResourceBase(String resourceBase) 
    {
        try
        {
            setBaseResource(Resource.newResource(resourceBase));
        }
        catch (Exception e)
        {
            log.warn(e);
            throw new IllegalArgumentException(resourceBase);
        }
    }

    /* ------------------------------------------------------------ */
    /**
     * @return Returns the mimeTypes.
     */
    public MimeTypes getMimeTypes()
    {
        return _mimeTypes;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param mimeTypes The mimeTypes to set.
     */
    public void setMimeTypes(MimeTypes mimeTypes)
    {
        _mimeTypes = mimeTypes;
    }

    /* ------------------------------------------------------------ */
    public String toString()
    {
        return "ContextHandler@"+Integer.toHexString(hashCode())+"{"+getContextPath()+","+getBaseResource()+"}";
    }

    /* ------------------------------------------------------------ */
    public synchronized Class loadClass(String className)
        throws ClassNotFoundException
    {
        if (className==null)
            return null;
        
        if (_classLoader==null)
            return Loader.loadClass(this.getClass(), className);

        return _classLoader.loadClass(className);
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
        public ContextHandler getContextHandler()
        {
            // TODO reduce visibility of this method
            return ContextHandler.this;
        }

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
            if (_mimeTypes==null)
                return null;
            Buffer mime = _mimeTypes.getMimeByExtension(file);
            if (mime!=null)
                return mime.toString();
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
        public RequestDispatcher getRequestDispatcher(String uriInContext)
        {
            if (uriInContext == null)
                return null;

            if (!uriInContext.startsWith("/"))
                return null;
            
            try
            {
                String query=null;
                int q=0;
                if ((q=uriInContext.indexOf('?'))>0)
                {
                    query=uriInContext.substring(q+1);
                    uriInContext=uriInContext.substring(0,q);
                }
                if ((q=uriInContext.indexOf(';'))>0)
                    uriInContext=uriInContext.substring(0,q);

                String pathInContext=URIUtil.canonicalPath(URIUtil.decodePath(uriInContext));
                String uri=URIUtil.addPaths(getContextPath(), uriInContext);
                return new Dispatcher(ContextHandler.this, uri, pathInContext, query);
            }
            catch(Exception e)
            {
                LogSupport.ignore(log,e);
            }
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
            
            if (_baseResource==null)
                return null;
            
            try
            {
                Resource resource=_baseResource.addPath(URIUtil.canonicalPath(path));
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
            
            if (_baseResource==null)
                return null;
            
            try
            {
                Resource resource=_baseResource.addPath(URIUtil.canonicalPath(path));
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


        /* ------------------------------------------------------------ */
        public String toString()
        {
            return "ServletContext@"+Integer.toHexString(hashCode())+"{"+getContextPath()+","+getBaseResource()+"}";
        }

    }

}
