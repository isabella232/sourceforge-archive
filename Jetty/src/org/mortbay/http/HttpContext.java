// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import org.mortbay.http.handler.ResourceHandler;
import org.mortbay.http.handler.SecurityHandler;
import org.mortbay.util.Code;
import org.mortbay.util.InetAddrPort;
import org.mortbay.util.LifeCycle;
import org.mortbay.util.Log;
import org.mortbay.util.LogSink;
import org.mortbay.util.MultiException;
import org.mortbay.util.Resource;
import org.mortbay.util.StringUtil;

/* ------------------------------------------------------------ */
/** Context for a collection of HttpHandlers.
 * Handler Context provides an ordered container for HttpHandlers
 * that share the same path prefix, filebase, resourcebase and/or
 * classpath.
 * <p>
 * A HttpContext is analageous to a ServletContext in the
 * Servlet API, except that it may contain other types of handler
 * other than servlets.
 * <p>
 * A ClassLoader is created for the context and it uses 
 * Thread.currentThread().getContextClassLoader(); as it's parent loader.
 * The class loader is initialized during start(), when a derived
 * context calls initClassLoader() or on the first call to loadClass()
 * <p>
 *
 * <B>Note. that order is important when configuring a HttpContext.
 * For example, if resource serving is enabled before servlets, then resources
 * take priority.</B>
 *
 * @see HttpServer
 * @see HttpHandler
 * @see org.mortbay.jetty.servlet.ServletHttpContext
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class HttpContext implements LifeCycle
{
    private final static Map __dftMimeMap = new HashMap();
    private final static Map __encodings = new HashMap();
    static
    {
        ResourceBundle mime = ResourceBundle.getBundle("org/mortbay/http/mime");
        Enumeration i = mime.getKeys();
        while(i.hasMoreElements())
        {
            String ext = (String)i.nextElement();
            __dftMimeMap.put(ext,mime.getString(ext));
        }

        ResourceBundle encoding = ResourceBundle.getBundle("org/mortbay/http/encoding");
        i = mime.getKeys();
        while(i.hasMoreElements())
        {
            String type = (String)i.nextElement();
            __encodings.put(type,mime.getString(type));
        }
    }

    /* ------------------------------------------------------------ */
    private HttpServer _httpServer;
    private List _handlers=new ArrayList(3);
    private String _classPath;
    private ClassLoader _parent;
    private ClassLoader _loader;
    private Resource _resourceBase;
    private boolean _started;

    private Map _attributes = new HashMap(11);
    private Map _initParams = new HashMap(11);
    
    private List _hosts=new ArrayList(2);
    private String _contextPath;
    private String _name;
    private boolean _redirectNullPath=true;
    private boolean _httpServerAccess=false;
    
    private File _tmpDir;
    
    private Map _mimeMap;
    private Map _encodingMap;
    private Map _resourceAliases;
    private Map _errorPages;
    

    private PermissionCollection _permissions;    
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param httpServer 
     * @param contextPathSpec 
     */
    protected HttpContext(HttpServer httpServer,String contextPathSpec)
    {
        // check context path
        if (contextPathSpec==null ||
            contextPathSpec.indexOf(',')>=0 ||
            contextPathSpec.startsWith("*"))
            new IllegalArgumentException
                ("Illegal context spec:"+contextPathSpec);
        if(!contextPathSpec.startsWith("/"))
	    contextPathSpec='/'+contextPathSpec;

        if (contextPathSpec.endsWith("/*"))
            _contextPath=contextPathSpec
                .substring(0,contextPathSpec.length()-2);
        else if (contextPathSpec.length()>1)
        {
            if (contextPathSpec.endsWith("/"))
                _contextPath=contextPathSpec
                    .substring(0,contextPathSpec.length()-1);
            else
                _contextPath=contextPathSpec;

            Code.debug("Fixing contextPathSpec ",contextPathSpec,
                       ", Assuming: ",_contextPath+"/*");
        }
        else
            _contextPath="/";

        _httpServer=httpServer;
        _name=null;
    }
    
    /* ------------------------------------------------------------ */
    /** Set context init parameter.
     * Init Parameters differ from attributes as they can only
     * have string values, servlets cannot set them and they do
     * not have a package scoped name space.
     * @param param param name
     * @param value param value or null
     */
    public void setInitParameter(String param, String value)
    {
        _initParams.put(param,value);
    }
    
    /* ------------------------------------------------------------ */
    /** Get context init parameter.
     * @param param param name
     * @return param value or null
     */
    public String getInitParameter(String param)
    {
        return (String)_initParams.get(param);
    }
    
    /* ------------------------------------------------------------ */
    /** Get context init parameter.
     * @return Enumeration of names
     */
    public Enumeration getInitParameterNames()
    {
        return Collections.enumeration(_initParams.keySet());
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param name attribute name
     * @param value attribute value
     */
    public synchronized void setAttribute(String name, Object value)
    {
        _attributes.put(name,value);
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param name attribute name
     * @return attribute value or null
     */
    public Object getAttribute(String name)
    {
        return _attributes.get(name);
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return enumaration of names.
     */
    public Enumeration getAttributeNames()
    {
        return Collections.enumeration(_attributes.keySet());
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @param name attribute name
     */
    public synchronized void removeAttribute(String name)
    {
        _attributes.remove(name);
    }
    
    /* ------------------------------------------------------------ */
    /** Register a host mapping for this context.
     * This method is a shorthand for
     * <Code>context.getHttpServer()..addContext(hostname,context)</Code>.
     * The call does not alter the original registration of the
     * context, nor can it change the context path.
     * @param hostname 
     */
    public void registerHost(String hostname)
    {
        _httpServer.addContext(hostname,this);
    }
    
    /* ------------------------------------------------------------ */
    /** Add a virtual host alias to this context.
     * @param host 
     */
    void addHost(String host)
    {
        // Note that null hosts are also added.
        _hosts.add(host);
        _name=null;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return List of virtual hosts that this context is registered in.
     */
    public List getHosts()
    {
        return _hosts;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return The context prefix
     */
    public String getContextPath()
    {
        return _contextPath;
    }
    
    /* ------------------------------------------------------------ */
    public HttpServer getHttpServer()
    {
        return _httpServer;
    }

    /* ------------------------------------------------------------ */
    /** Get the context classpath.
     * This method only returns the paths that have been set for this
     * context and does not include any paths from a parent or the
     * system classloader.
     * Note that this may not be a legal javac classpath.  A file only
     * classpath can be obtained from ContextLoader.getFileClassPath().
     * @return a coma or ';' separated list of class
     * resources. These may be jar files, directories or URLs to jars
     * or directories.
     * @see org.mortbay.jetty.servlet.WebApplicationContext.getFileClassPath();
     */
    public String getClassPath()
    {
        return _classPath;
    }
     
    /* ------------------------------------------------------------ */
    /** Sets the class path for the context.
     * A class path is only required for a context if it uses classes
     * that are not in the system class path.
     * @param filePath a coma or ';' separated list of class
     * resources. These may be jar files, directories or URLs to jars
     * or directories.
     */
    public void setClassPath(String classPath)
    {
        _classPath=classPath;
        if (isStarted())
            Code.warning("classpath set while started");
    }

    /* ------------------------------------------------------------ */
    /** Sets the class path for the context from the jar and zip files found
     *  in the specified resource.
     * @param lib the resource that contains the jar and/or zip files.
     * @param append true if the classpath entries are to be appended to any
     * existing classpath, or false if they replace the existing classpath.
     * @see setClassPath
     */
    public void setClassPaths(Resource lib, boolean append)
    {
        if (_loader!=null)
            throw new IllegalStateException("ClassLoader already initialized");
        
        if (lib.exists() && lib.isDirectory())
        {
            StringBuffer classPath=new StringBuffer();
            String[] files=lib.list();

            for (int f=0;files!=null && f<files.length;f++)
            {
                try {
                    Resource fn=lib.addPath(files[f]);
                    String fnlc=fn.getName().toLowerCase();
                    if (fnlc.endsWith(".jar") || fnlc.endsWith(".zip"))
                    {
                        classPath.append(classPath.length()>0?",":"");
                        classPath.append(fn.toString());
                    }
                }
                catch (Exception ex)
                {
                    Code.warning(ex);
                }
            }

            if (classPath.length()>0)
            {
                if (append && this.getClassPath()!=null)
                       classPath.append(",").append(_classPath);
                _classPath=classPath.toString();
            }
        }
    }

    /* ------------------------------------------------------------ */
    /** Sets the class path for the context from the jar and zip files found
     *  in the specified resource.
     * @param lib the resource that contains the jar and/or zip files.
     * @param append true if the classpath entries are to be appended to any
     * existing classpath, or false if they are to be prepended.
     * @see setClassPaths
     * @exception IOException
     */
    public void setClassPaths(String lib, boolean append) throws IOException
    {
        if (_loader!=null)
            throw new IllegalStateException("ClassLoader already initialized");
        this.setClassPaths(Resource.newResource(lib), append);
    }

    /* ------------------------------------------------------------ */
    public File getTempDirectory()
    {
        if (_tmpDir!=null)
            return _tmpDir;
        
        // Initialize temporary directory
        //
        // I'm afraid that this is very much black magic.
        // but if you can think of better....
        Object t = getAttribute("javax.servlet.context.tempdir");
        
        if (t!=null && (t instanceof File))
        {
            _tmpDir=(File)t;
            if (_tmpDir.isDirectory() && _tmpDir.canWrite())
                return _tmpDir;
        }
                
        if (t!=null && (t instanceof String))
        {
            try
            {
                _tmpDir=new File((String)t);
                
                if (_tmpDir.isDirectory() && _tmpDir.canWrite())
                {
                    Log.event("Converted to File "+_tmpDir+" for "+this);
                    setAttribute("javax.servlet.context.tempdir",_tmpDir);
                    return _tmpDir;
                }
            }
            catch(Exception e)
            {
                Code.warning(e);
            }
        }

        // No tempdir set so make one!
        try
        {
            HttpListener httpListener=(HttpListener)
                _httpServer.getListeners().iterator().next();
            
            String vhost = null;
            for (int h=0;vhost==null && _hosts!=null && h<_hosts.size();h++)
                vhost=(String)_hosts.get(h);
            if (InetAddrPort.__0_0_0_0.equals(vhost))
                vhost=null;
            
            String host=httpListener.getHost();
            if (InetAddrPort.__0_0_0_0.equals(host))
                host=null;
            
            String temp="Jetty_"+
                (host==null?"":host)+
                "_"+
                httpListener.getPort()+
                "_"+
                (vhost==null?"":vhost)+
                "_"+
                getContextPath();
            
            temp=temp.replace('/','_');
            temp=temp.replace('.','_');
            temp=temp.replace('\\','_');
            
            _tmpDir=new File(System.getProperty("java.io.tmpdir"),
                             temp);
            if (!_tmpDir.exists())
            {
                _tmpDir.mkdir();
                _tmpDir.deleteOnExit();
                Log.event("Created temp dir "+_tmpDir+" for "+this);
            }
            else if (!_tmpDir.isDirectory() || !_tmpDir.canWrite())
                _tmpDir=null;
            else
            {
                _tmpDir.deleteOnExit();
                Log.event("Reused temp dir "+_tmpDir+" for "+this);
            }
        }
        catch(Exception e)
        {
            _tmpDir=null;
            Code.warning(e);
        }    
        
        if (_tmpDir==null)
        {
            try{
                // that didn't work, so try something simpler (ish)
                _tmpDir=File.createTempFile("JettyContext",null);
                if (_tmpDir.exists())
                    _tmpDir.delete();
                _tmpDir.mkdir();
                _tmpDir.deleteOnExit();
                Log.event("Created temp dir "+_tmpDir+" for "+this);
            }
            catch(IOException e)
            {
                Code.fail(e);
            }
        }
        
        setAttribute("javax.servlet.context.tempdir",_tmpDir);
        return _tmpDir;
    }
    
    /* ------------------------------------------------------------ */
    public void setTempDirectory(File file)
    {
        _tmpDir=file;
        setAttribute("javax.servlet.context.tempdir",_tmpDir);
    }
    
    /* ------------------------------------------------------------ */
    /** Get the classloader.
     * If no classloader has been set and the context has been loaded
     * normally, then null is returned.
     * If no classloader has been set and the context was loaded from
     * a classloader, that loader is returned.
     * If a classloader has been set and no classpath has been set then
     * the set classloader is returned.
     * If a classloader and a classpath has been set, then a new
     * URLClassloader initialized on the classpath with the set loader as a
     * partent is return.
     * @return Classloader or null.
     */
    public synchronized ClassLoader getClassLoader()
    {
        return _loader;
    }

    /* ------------------------------------------------------------ */
    /** Initialize the context classloader.
     * Initialize the context classloader with the current parameters.
     * Any attempts to change the classpath after this call will
     * result in a IllegalStateException
     */
    protected void initClassLoader()
        throws MalformedURLException, IOException
    {
        if (_loader==null)
        {
            // If no parent, then try this threads classes loader as parent
            if (_parent==null)
                _parent=Thread.currentThread().getContextClassLoader();
            
            // If no parent, then try this classes loader as parent
            if (_parent==null)
                _parent=this.getClass().getClassLoader();
            
            Code.debug("Init classloader from ",_classPath,
                       ", ",_parent," for ",this);

            if (_classPath!=null || _permissions!=null)
                _loader=new ContextLoader(this,_classPath,_parent,_permissions);
            else
                _loader=_parent;
        }
    }
    
    /* ------------------------------------------------------------ */
    public synchronized Class loadClass(String className)
        throws ClassNotFoundException
    {
        if (_loader==null)
        {
            try{initClassLoader();}
            catch(Exception e)
            {
                Code.warning(e);
                return null;
            }
        }
        
        if (className==null)
            return null;
        
        return _loader.loadClass(className);
    }
    
    /* ------------------------------------------------------------ */
    /** Set the Resource Base.
     * The base resource is the Resource to use as a relative base
     * for all context resources. The ResourceBase attribute is a
     * string version of the baseResource.
     * If a relative file is passed, it is converted to a file
     * URL based on the current working directory.
     * @return The file or URL to use as the base for all resources
     * within the context.
     */
    public String getResourceBase()
    {
        if (_resourceBase==null)
            return null;
        return _resourceBase.toString();
    }
    
    /* ------------------------------------------------------------ */
    /** Set the Resource Base.
     * The base resource is the Resource to use as a relative base
     * for all context resources. The ResourceBase attribute is a
     * string version of the baseResource.
     * If a relative file is passed, it is converted to a file
     * URL based on the current working directory.
     * @param resourceBase A URL prefix or directory name.
     */
    public void setResourceBase(String resourceBase)
    {
        try{
            _resourceBase=Resource.newResource(resourceBase);
            Code.debug("resourceBase=",_resourceBase," for ", this);
        }
        catch(IOException e)
        {
            Code.debug(e);
            throw new IllegalArgumentException(resourceBase+":"+e.toString());
        }
    }

    /* ------------------------------------------------------------ */
    /** Get a Resource from the context.
     * The resource base must be set
     * @param path A path to be applied to the resource base
     * @return A Resource or null if no resourceBasse is set.
     */
    public Resource getResource(String path)
        throws IOException
    {
        if (_resourceBase==null)
            return null;
        if (path==null)
            return _resourceBase;
        return _resourceBase.addPath(path);
    }
    
    /* ------------------------------------------------------------ */
    /** Get the base resource.
     * The base resource is the Resource to use as a relative base
     * for all context resources. The ResourceBase attribute is a
     * string version of the baseResource.
     * @return The resourceBase as a Resource instance 
     */
    public Resource getBaseResource()
    {
        return _resourceBase;
    }
    
    /* ------------------------------------------------------------ */
    /** Set the base resource.
     * The base resource is the Resource to use as a relative base
     * for all context resources. The ResourceBase attribute is a
     * string version of the baseResource.
     * @param base The resourceBase as a Resource instance
     */
    public void setBaseResource(Resource base)
    {
        _resourceBase=base;
    }
    
    /* ------------------------------------------------------------ */
    /** Set Resource Alias.
     * Resource aliases map resource uri's within a context.
     * They may optionally be used by a handler when looking for
     * a resource.  The only known user is
     * org.mortbay.http.handler.Servlet.Context.
     * @param alias 
     * @param uri 
     */
    public void setResourceAlias(String alias,String uri)
    {
        if (_resourceAliases==null)
            _resourceAliases=new HashMap(5);
        _resourceAliases.put(alias,uri);
    }
    
    /* ------------------------------------------------------------ */
    public String getResourceAlias(String alias)
    {
        if (_resourceAliases==null)
            return null;
       return (String) _resourceAliases.get(alias);
    }
    
    /* ------------------------------------------------------------ */
    public String removeResourceAlias(String alias)
    {
        if (_resourceAliases==null)
            return null;
       return (String) _resourceAliases.remove(alias);
    }
    
    /* ------------------------------------------------------------ */
    /** set error page URI.
     * @param error A string representing an error code or a
     * exception classname
     * @param uriInContext 
     */
    public void setErrorPage(String error,String uriInContext)
    {
        if (_errorPages==null)
            _errorPages=new HashMap(5);
        _errorPages.put(error,uriInContext);
    }
    
    /* ------------------------------------------------------------ */
    /** get error page URI.
     * @param error A string representing an error code or a
     * exception classname
     * @return URI within context
     */
    public String getErrorPage(String error)
    {
        if (_errorPages==null)
            return null;
       return (String) _errorPages.get(error);
    }
    
    
    /* ------------------------------------------------------------ */
    public String removeErrorPage(String error)
    {
        if (_errorPages==null)
            return null;
       return (String) _errorPages.remove(error);
    }
    
    /* ------------------------------------------------------------ */
    /** Get all handlers.
     * @return 
     */
    public List getHttpHandlers()
    {
        return _handlers;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @deprecated 
     */
    public List getHandlers()
    {
        return _handlers;
    }

    /* ------------------------------------------------------------ */
    /**
     * @deprecated
     */
    public synchronized void addHandler(int i,HttpHandler handler)
    {
        _handlers.add(i,handler);
        handler.initialize(this);
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @deprecated
     */
    public synchronized void addHandler(HttpHandler handler)
    {
        addHandler(_handlers.size(),handler);
    }


    /* ------------------------------------------------------------ */
    /** Add a handler.
     * @param i The position in the handler list
     * @param handler The handler.
     */
    public synchronized void addHttpHandler(int i,HttpHandler handler)
    {
        _handlers.add(i,handler);
        handler.initialize(this);
    }
    
    /* ------------------------------------------------------------ */
    /** Add a HttpHandler to the context.
     * @param handler 
     */
    public synchronized void addHttpHandler(HttpHandler handler)
    {
        addHandler(_handlers.size(),handler);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @deprecated
     */
    public HttpHandler getHandler(int i)
    {
        return (HttpHandler)_handlers.get(i);
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @deprecated
     */
    public int getHandlerIndex(HttpHandler handler)
    {
        for (int h=0;h<_handlers.size();h++)
        {
            if ( handler == _handlers.get(h))
                return h;
        }
        return -1;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @deprecated
     */
    public synchronized HttpHandler getHandler(Class handlerClass)
    {
        for (int h=0;h<_handlers.size();h++)
        {
            HttpHandler handler = (HttpHandler)_handlers.get(h);
            if (handlerClass.isInstance(handler))
                return handler;
        }
        return null;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @deprecated
     */
    public synchronized HttpHandler removeHandler(int i)
    {
        HttpHandler handler = getHandler(i);
        if (handler.isStarted())
            throw new IllegalStateException("Handler is started");
        return (HttpHandler)_handlers.remove(i);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @deprecated
     */
    public synchronized void removeHandler(HttpHandler handler)
    {
        if (handler.isStarted())
            throw new IllegalStateException("Handler is started");
        _handlers.remove(handler);
    }
    
    /* ------------------------------------------------------------ */
    /** Get handler by index.
     */
    public HttpHandler getHttpHandler(int i)
    {
        return (HttpHandler)_handlers.get(i);
    }
    
    /* ------------------------------------------------------------ */
    /** Get handler index.
     * @param Handler instance 
     * @return Index of handler in context or -1 if not found.
     */
    public int getHttpHandlerIndex(HttpHandler handler)
    {
        for (int h=0;h<_handlers.size();h++)
        {
            if ( handler == _handlers.get(h))
                return h;
        }
        return -1;
    }
    
    /* ------------------------------------------------------------ */
    /** Get a handler by class.
     * @param handlerClass 
     * @return The first handler that is an instance of the handlerClass
     */
    public synchronized HttpHandler getHttpHandler(Class handlerClass)
    {
        for (int h=0;h<_handlers.size();h++)
        {
            HttpHandler handler = (HttpHandler)_handlers.get(h);
            if (handlerClass.isInstance(handler))
                return handler;
        }
        return null;
    }
    
    /* ------------------------------------------------------------ */
    /** Remove a handler.
     * The handler must be stopped before being removed.
     * @param i 
     */
    public synchronized HttpHandler removeHttpHandler(int i)
    {
        HttpHandler handler = getHandler(i);
        if (handler.isStarted())
            throw new IllegalStateException("Handler is started");
        return (HttpHandler)_handlers.remove(i);
    }
    
    /* ------------------------------------------------------------ */
    /** Remove a handler.
     * The handler must be stopped before being removed.
     */
    public synchronized void removeHttpHandler(HttpHandler handler)
    {
        if (handler.isStarted())
            throw new IllegalStateException("Handler is started");
        _handlers.remove(handler);
    }
    

    /* ------------------------------------------------------------ */
    /** Get the context ResourceHandler.
     * Conveniance method. If no ResourceHandler exists, a new one is added to
     * the context.
     * @return ResourceHandler
     */
    public ResourceHandler getResourceHandler()
    {
        ResourceHandler resourceHandler= (ResourceHandler)
            getHandler(org.mortbay.http.handler.ResourceHandler.class);
        if (resourceHandler==null)
        {
            resourceHandler=new ResourceHandler();
            addHandler(resourceHandler);
        }
        return resourceHandler;
    }
    
    /* ------------------------------------------------------------ */
    /** Get the context SecurityHandler.
     * Conveniance method. If no SecurityHandler exists, a new one is added to
     * the context at position 0.
     * @return SecurityHandler
     */
    public SecurityHandler getSecurityHandler()
    {
        SecurityHandler securityHandler= (SecurityHandler)
            getHandler(org.mortbay.http.handler.SecurityHandler.class);
        if (securityHandler==null)
        {
            securityHandler=new SecurityHandler();
            addHandler(0,securityHandler);
        }
        return securityHandler;
    }
    
    /* ------------------------------------------------------------ */
    /** Setup context for serving Resources as files.
     * Conveniance method.
     * @param serve If true and there is no ResourceHandler instance in the
     * context, a ResourceHandler is added. If false, all ResourceHandler
     * instances are removed from the context.
     */
    public synchronized void setServingResources(boolean serve)
    {
        ResourceHandler handler = (ResourceHandler)
            getHandler(org.mortbay.http.handler.ResourceHandler.class);
        if (serve)
        {
            if (handler==null)
                getResourceHandler();
        }
        else while (handler!=null)
        {
            _handlers.remove(handler);
            handler = (ResourceHandler)
                getHandler(org.mortbay.http.handler.ResourceHandler.class);
        }
    }

    /* ------------------------------------------------------------ */
    public boolean isServingResources()
    {
        return null!=getHandler(org.mortbay.http.handler.ResourceHandler.class);
    }
    
    /* ------------------------------------------------------------ */
    /** Set the SecurityHandler realm name.
     * Conveniance method.
     * If a SecurityHandler is not in the context, one is created
     * as the 0th handler.
     * @param realmName The name to use to retrieve the actual realm
     * from the HttpServer
     */
    public void setRealm(String realmName)
    {
        SecurityHandler sh=getSecurityHandler();
        sh.setRealmName(realmName);
    }

    /* ------------------------------------------------------------ */
    /** Set the SecurityHandler realm.
     * Conveniance method.
     * If a SecurityHandler is not in the context, one is created
     * as the 0th handler.
     * @param realmName The name of the realm
     * @param realm The realm instance to use, instead of one
     * retrieved from the HttpServer.
     */
    public void setRealm(String realmName, UserRealm realm)
    {
        SecurityHandler sh=getSecurityHandler();
        sh.setRealm(realmName,realm);
    }

    
    /* ------------------------------------------------------------ */
    public String getRealm()
    {
        SecurityHandler handler = (SecurityHandler)
            getHandler(org.mortbay.http.handler.SecurityHandler.class);
        if (handler!=null)
            return handler.getRealmName();
        return null;
    }
    
    /* ------------------------------------------------------------ */
    /** Add a security constraint.
     * Conveniance method.
     * If a SecurityHandler is not in the context, one is created
     * as the 0th handler.
     * @param pathSpec 
     * @param sc 
     */
    public void addSecurityConstraint(String pathSpec,
                                      SecurityConstraint sc)
    {
        SecurityHandler sh=getSecurityHandler();
        sh.addSecurityConstraint(pathSpec,sc);
    }
    
    /* ------------------------------------------------------------ */
    /** Add an auth security constraint.
     * Conveniance method.
     * @param pathSpec 
     * @param Auth role
     */
    public void addAuthConstraint(String pathSpec,
                                  String role)
    {
        SecurityHandler sh=getSecurityHandler();
        sh.addSecurityConstraint(pathSpec,new SecurityConstraint(role,role));
    }

    /* ------------------------------------------------------------ */
    public synchronized Map getMimeMap()
    {
        return _mimeMap;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * Also sets the org.mortbay.http.mimeMap context attribute
     * @param mimeMap 
     */
    public void setMimeMap(Map mimeMap)
    {
        _mimeMap = mimeMap;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param filename 
     * @return 
     */
    public String getMimeByExtension(String filename)
    {
        String type=null;
        
        if (filename!=null)
        {
            int i=-1;
            while(type==null)
            {
                i=filename.indexOf(".",i+1);
                
                if (i<0 || i>=filename.length())
                    break;
                
                String ext=StringUtil.asciiToLowerCase(filename.substring(i+1));
                if (_mimeMap!=null)
                    type = (String)_mimeMap.get(ext);
                if (type==null)
                    type=(String)__dftMimeMap.get(ext);
            }
        }

        return type;
    }

    /* ------------------------------------------------------------ */
    /** Set a mime mapping
     * @param extension 
     * @param type 
     */
    public void setMimeMapping(String extension,String type)
    {
        if (_mimeMap==null)
            _mimeMap=new HashMap();
        _mimeMap.put(extension,type);
    }

    
    /* ------------------------------------------------------------ */
    /** Get the map of mime type to char encoding.
     * @return 
     */
    public synchronized Map getEncodingMap()
    {
        if (_encodingMap==null)
            _encodingMap=Collections.unmodifiableMap(__encodings);
        return _encodingMap;
    }
    
    /* ------------------------------------------------------------ */
    /** Set the map of mime type to char encoding.
     * Also sets the org.mortbay.http.encodingMap context attribute
     * @param encodingMap 
     */
    public void setEncodingMap(Map encodingMap)
    {
        _encodingMap = encodingMap;
    }

    /* ------------------------------------------------------------ */
    /** Get char encoding by mime type.
     * @param type 
     * @return 
     */
    public String getEncodingByMimeType(String type)
    {
        String encoding =null;
        
        if (type!=null)
            encoding=(String)_encodingMap.get(type);

        return encoding;
    }

    /* ------------------------------------------------------------ */
    /** Set the encoding that should be used for a mimeType.
     * @param mimeType
     * @param encoding
     */
    public void setTypeEncoding(String mimeType,String encoding)
    {
        getEncodingMap().put(mimeType,encoding);
    }

    /* ------------------------------------------------------------ */
    /** Set null path redirection.
     * @param b if true a /context request will be redirected to
     * /context/ if there is not path in the context.
     */
    public void setRedirectNullPath(boolean b)
    {
        _redirectNullPath=b;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return True if a /context request is redirected to /context/ if
     * there is not path in the context.
     */
    public boolean isRedirectNullPath()
    {
        return _redirectNullPath;
    }



    /* ------------------------------------------------------------ */
    /** Set the permissions to be used for this context.
     * The collection of permissions set here are used for all classes
     * loaded by this context.  This is simpler that creating a
     * security policy file, as not all code sources may be statically
     * known.
     * @param permissions 
     */
    public void setPermissions(PermissionCollection permissions)
    {
        _permissions=permissions;
    }
    
    /* ------------------------------------------------------------ */
    /** Get the permissions to be used for this context.
     */
    public PermissionCollection getPermissions()
    {
        return _permissions;
    }

    /* ------------------------------------------------------------ */
    /** Add a permission to this context.
     * The collection of permissions set here are used for all classes
     * loaded by this context.  This is simpler that creating a
     * security policy file, as not all code sources may be statically
     * known.
     * @param permission 
     */
    public void addPermission(Permission permission)
    {
        if (_permissions==null)
            _permissions=new Permissions();
        _permissions.add(permission);
    }

    /* ------------------------------------------------------------ */
    /** Handler request.
     * Determine the path within the context and then call
     * handle(pathInContext,request,response).
     * @param request 
     * @param response 
     * @return True if the request has been handled.
     * @exception HttpException 
     * @exception IOException 
     */
    public boolean handle(HttpRequest request,
                          HttpResponse response)
        throws HttpException, IOException
    {
        if (!_started)
            return false;
        
        String pathInContext = request.getPath();
        
        String contextPath=null;
        if (_contextPath.length()>1)
        {
            contextPath=_contextPath;
            pathInContext=pathInContext.substring(_contextPath.length());
        }
        
        if (_redirectNullPath && (pathInContext==null ||
                                  pathInContext.length()==0))
        {
            StringBuffer buf=request.getRequestURL();
            buf.append("/");
            String q=request.getQuery();
            if (q!=null&&q.length()!=0)
                buf.append("?"+q);
            response.setField(HttpFields.__Location,
                              buf.toString());
            if (Code.debug())
                Code.warning(this+" consumed all of path "+
                             request.getPath()+
                             ", redirect to "+buf.toString());
            response.sendError(302);
            return true;
        }
        
        String pathParams=null;
        int semi = pathInContext.indexOf(';');
        if (semi>=0)
        {
            pathParams=pathInContext.substring(semi+1);
            pathInContext=pathInContext.substring(0,semi);
        }

        return handle(0,pathInContext,pathParams,request,response);
    }

    /* ------------------------------------------------------------ */
    /** Handler request.
     * Call each HttpHandler until request is handled.
     * @param firstHandler The index of the first handler to call.
     * @param pathInContext Path in context
     * @param pathParams Path parameters such as encoded Session ID
     * @param request 
     * @param response 
     * @return True if the request has been handled.
     * @exception HttpException 
     * @exception IOException 
     */
    public boolean handle(int firstHandler,
                          String pathInContext,
                          String pathParams,
                          HttpRequest request,
                          HttpResponse response)
        throws HttpException, IOException
    {
        // Save the thread context loader
        Thread thread = Thread.currentThread();
        ClassLoader lastContextLoader=thread.getContextClassLoader();
        HttpContext lastHttpContext=response.getHttpContext();
        try
        {
            if (_loader!=null)
                thread.setContextClassLoader(_loader);
            response.setHttpContext(this);
            
            List handlers=getHandlers();
            for (int k=firstHandler;k<handlers.size();k++)
            {
                HttpHandler handler =
                    (HttpHandler)handlers.get(k);
                
                if (!handler.isStarted())
                {
                    Code.debug(handler," not started in ",this);
                    continue;
                }
                
                Code.debug("Handler ",handler);
                
                handler.handle(pathInContext,
                               pathParams,
                               request,
                               response);
                
                if (request.isHandled())
                {
                    Code.debug("Handled by ",handler);
                    response.complete();
                    return true;
                }
            }
            return false;
        }
        finally
        {
            thread.setContextClassLoader(lastContextLoader);
            response.setHttpContext(lastHttpContext);
        }
    }
    
    /* ------------------------------------------------------------ */
    protected String getHttpContextName()
    {
        if (_name==null)
            _name = (_hosts.size()>1?(_hosts.toString()+":"):"")+_contextPath;
        return _name;
    }
    
    /* ------------------------------------------------------------ */
    public String toString()
    {
        return "HttpContext["+getHttpContextName()+"]"; 
    }
    
    /* ------------------------------------------------------------ */
    public String toString(boolean detail)
    {
        return "HttpContext["+getHttpContextName()+"]" +
            (detail?("="+_handlers):""); 
    }
    
    /* ------------------------------------------------------------ */
    /** Start all handlers then listeners.
     */
    public synchronized void start()
        throws Exception
    {
        if (_httpServer==null)
            throw new IllegalStateException("No server for "+this);

        // start the context itself
        _started=true;
        getMimeMap();
        getEncodingMap();

        
        // setup the context loader
        initClassLoader();
        
        // Start the handlers
        Thread thread = Thread.currentThread();
        ClassLoader lastContextLoader=thread.getContextClassLoader();
        try
        {
            if (_loader!=null)
                thread.setContextClassLoader(_loader);

            startHandlers();
            
        }
        finally
        {
            thread.setContextClassLoader(lastContextLoader);
        }

    }

    /* ------------------------------------------------------------ */
    /** Start the handlers.
     * This is called by start after the classloader has been
     * initialized and set as the thread context loader.
     * It may be specialized to provide custom handling
     * before any handlers are started.
     * @exception Exception 
     */
    protected void startHandlers()
        throws Exception
    {   
        // Prepare a multi exception
        MultiException mx = new MultiException();

        Iterator handlers = _handlers.iterator();
        while(handlers.hasNext())
        {
            HttpHandler handler=(HttpHandler)handlers.next();
            if (!handler.isStarted())
                try{handler.start();}catch(Exception e){mx.add(e);}
        }
        mx.ifExceptionThrow();
    }
    
    /* ------------------------------------------------------------ */
    /** Start all handlers then listeners.
     */
    public synchronized boolean isStarted()
    {
        return _started;
    }
    
    /* ------------------------------------------------------------ */
    /** Stop all listeners then handlers.
     * @exception InterruptedException If interrupted, stop may not have
     * been called on everything.
     */
    public synchronized void stop()
        throws InterruptedException
    {
        _started=false;
        Thread thread = Thread.currentThread();
        ClassLoader lastContextLoader=thread.getContextClassLoader();
        try
        {
            if (_loader!=null)
                thread.setContextClassLoader(_loader);
            Iterator handlers = _handlers.iterator();
            while(handlers.hasNext())
            {
                HttpHandler handler=(HttpHandler)handlers.next();
                if (handler.isStarted())
                {
                    try{handler.stop();}
                    catch(Exception e){Code.warning(e);}
                }
            }
        }
        finally
        {
            thread.setContextClassLoader(lastContextLoader);
        }
        _loader=null;
        
    }
    

    /* ------------------------------------------------------------ */
    private boolean _statsOn=false;
    int _requests;
    int _responses1xx; // Informal
    int _responses2xx; // Success
    int _responses3xx; // Redirection
    int _responses4xx; // Client Error
    int _responses5xx; // Server Error

    /* ------------------------------------------------------------ */
    /** True set statistics recording on for this context.
     * @param on If true, statistics will be recorded for this context.
     */
    public void setStatsOn(boolean on)
    {
        Log.event("setStatsOn "+on+" for "+this);
        _statsOn=on;
    }

    /* ------------------------------------------------------------ */
    public boolean getStatsOn() {return _statsOn;}
    
    /* ------------------------------------------------------------ */
    public synchronized void statsReset()
    {
         _requests=0;
         _responses1xx=0;
         _responses2xx=0;
         _responses3xx=0;
         _responses4xx=0;
         _responses5xx=0;
    }
    
    
    /* ------------------------------------------------------------ */
    /**
     * @return Get the number of requests handled by this context
     * since last call of statsReset(). If setStatsOn(false) then this
     * is undefined. 
     */
    public int getRequests() {return _requests;}

    /* ------------------------------------------------------------ */
    /** 
     * @return Get the number of responses with a 2xx status returned
     * by this context since last call of statsReset(). Undefined if
     * if setStatsOn(false). 
     */
    public int getResponses1xx() {return _responses1xx;}

    /* ------------------------------------------------------------ */
    /** 
     * @return Get the number of responses with a 100 status returned
     * by this context since last call of statsReset(). Undefined if
     * if setStatsOn(false). 
     */
    public int getResponses2xx() {return _responses2xx;}

    /* ------------------------------------------------------------ */
    /** 
     * @return Get the number of responses with a 3xx status returned
     * by this context since last call of statsReset(). Undefined if
     * if setStatsOn(false). 
     */
    public int getResponses3xx() {return _responses3xx;}

    /* ------------------------------------------------------------ */
    /** 
     * @return Get the number of responses with a 4xx status returned
     * by this context since last call of statsReset(). Undefined if
     * if setStatsOn(false). 
     */
    public int getResponses4xx() {return _responses4xx;}

    /* ------------------------------------------------------------ */
    /** 
     * @return Get the number of responses with a 5xx status returned
     * by this context since last call of statsReset(). Undefined if
     * if setStatsOn(false). 
     */
    public int getResponses5xx() {return _responses5xx;}

    
    /* ------------------------------------------------------------ */
    /** 
     * @deprecated use HttpServer.getRequestLogSink() 
     */
    public synchronized LogSink getLogSink()
    {
        return null;
    }
    
    
    /* ------------------------------------------------------------ */
    /**
     * @deprecated use HttpServersetRequestLogSink()
     */
    public synchronized void setLogSink(LogSink logSink)
    {
        Log.warning("HttpContext.setLogSink not supported. Use HttpServer.setLogSink");
    }
    
        
    /* ------------------------------------------------------------ */
    /** Log a request and response.
     * @param request 
     * @param response 
     */
    void log(HttpRequest request,
             HttpResponse response,
             int length)
    {
        if (_statsOn)
        {
            synchronized(this)
            {
                _requests++;
                if (response!=null)
                {
                    switch(response.getStatus()/100)
                    {
                      case 1: _responses1xx++;break;
                      case 2: _responses2xx++;break;
                      case 3: _responses3xx++;break;
                      case 4: _responses4xx++;break;
                      case 5: _responses5xx++;break;
                    }
                }
            }
        }

        if (_httpServer!=null)
            _httpServer.log(request,response,length);
    }
}
