// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;

import com.mortbay.HTTP.Handler.ResourceHandler;
import com.mortbay.HTTP.Handler.SecurityHandler;
import com.mortbay.HTTP.Handler.ForwardHandler;
import com.mortbay.Util.Code;
import com.mortbay.Util.IO;
import com.mortbay.Util.Resource;
import com.mortbay.Util.StringUtil;
import com.mortbay.Util.LifeCycle;
import com.mortbay.Util.MultiException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.List;
import java.util.Map;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Permission;

/* ------------------------------------------------------------ */
/** Context for a collection of HttpHandlers.
 * Handler Context provides an ordered container for HttpHandlers
 * that share the same path prefix, filebase, resourcebase and/or
 * classpath.
 * <p>
 * A HandlerContext is analageous to a ServletContext in the
 * Servlet API, except that it may contain other types of handler
 * other than servlets.
 * <p>
 * Convenience methods are provided for adding common handlers. See
 * com.mortbay.Jetty.JettyContext for conveniance methods for
 * servlets.
 *
 * <B>Note. that order is important when configuring a HandlerContext.
 * For example, if resource serving is enabled before servlets, then resources
 * take priority.</B>
 *
 * @see HttpServer
 * @see HttpHandler
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class HandlerContext implements LifeCycle
{
    /* ------------------------------------------------------------ */
    final static String[] __mimeType =
    {
        "default","application/octet-stream",
        "ai","application/postscript",
        "aif","audio/x-aiff",
        "aifc","audio/x-aiff",
        "aiff","audio/x-aiff",
        "asc","text/plain",
        "au","audio/basic",
        "avi","video/x-msvideo",
        "bcpio","application/x-bcpio",
        "bin","application/octet-stream",
        "cdf","application/x-netcdf",
        "class","application/java-vm",
        "cpio","application/x-cpio",
        "cpt","application/mac-compactpro",
        "csh","application/x-csh",
        "css","text/css",
        "dcr","application/x-director",
        "dir","application/x-director",
        "dms","application/octet-stream",
        "doc","application/msword",
        "dvi","application/x-dvi",
        "dxr","application/x-director",
        "eps","application/postscript",
        "etx","text/x-setext",
        "exe","application/octet-stream",
        "ez","application/andrew-inset",
        "gif","image/gif",
        "gtar","application/x-gtar",
        "gz","application/gzip",
        "gzip","application/gzip",
        "hdf","application/x-hdf",
        "hqx","application/mac-binhex40",
        "html","text/html",
        "htm","text/html",
        "ice","x-conference/x-cooltalk",
        "ief","image/ief",
        "iges","model/iges",
        "igs","model/iges",
        "java","text/plain",
        "jpeg","image/jpeg",
        "jpe","image/jpeg",
        "jpg","image/jpeg",
        "js","application/x-javascript",
        "jsp","text/plain",
        "kar","audio/midi",
        "latex","application/x-latex",
        "lha","application/octet-stream",
        "lzh","application/octet-stream",
        "man","application/x-troff-man",
        "me","application/x-troff-me",
        "mesh","model/mesh",
        "mid","audio/midi",
        "midi","audio/midi",
        "mif","application/vnd.mif",
        "movie","video/x-sgi-movie",
        "mov","video/quicktime",
        "mp2","audio/mpeg",
        "mp3","audio/mpeg",
        "mpeg","video/mpeg",
        "mpe","video/mpeg",
        "mpga","audio/mpeg",
        "mpg","video/mpeg",
        "ms","application/x-troff-ms",
        "msh","model/mesh",
        "nc","application/x-netcdf",
        "oda","application/oda",
        "pbm","image/x-portable-bitmap",
        "pdb","chemical/x-pdb",
        "pdf","application/pdf",
        "pgm","image/x-portable-graymap",
        "pgn","application/x-chess-pgn",
        "png","image/png",
        "pnm","image/x-portable-anymap",
        "ppm","image/x-portable-pixmap",
        "ppt","application/vnd.ms-powerpoint",
        "ps","application/postscript",
        "qt","video/quicktime",
        "ra","audio/x-pn-realaudio",
        "ra","audio/x-realaudio",
        "ram","audio/x-pn-realaudio",
        "ras","image/x-cmu-raster",
        "rgb","image/x-rgb",
        "rm","audio/x-pn-realaudio",
        "roff","application/x-troff",
        "rpm","application/x-rpm",
        "rpm","audio/x-pn-realaudio",
        "rtf","application/rtf",
        "rtf","text/rtf",
        "rtx","text/richtext",
        "sgml","text/sgml",
        "sgm","text/sgml",
        "sh","application/x-sh",
        "shar","application/x-shar",
        "silo","model/mesh",
        "sit","application/x-stuffit",
        "skd","application/x-koan",
        "skm","application/x-koan",
        "skp","application/x-koan",
        "skt","application/x-koan",
        "smi","application/smil",
        "smil","application/smil",
        "snd","audio/basic",
        "spl","application/x-futuresplash",
        "src","application/x-wais-source",
        "sv4cpio","application/x-sv4cpio",
        "sv4crc","application/x-sv4crc",
        "swf","application/x-shockwave-flash",
        "t","application/x-troff",
        "tar","application/x-tar",
        "tar.gz","application/x-gtar",
        "tcl","application/x-tcl",
        "tex","application/x-tex",
        "texi","application/x-texinfo",
        "texinfo","application/x-texinfo",
        "tgz","application/x-gtar",
        "tiff","image/tiff",
        "tif","image/tiff",
        "tr","application/x-troff",
        "tsv","text/tab-separated-values",
        "txt","text/plain",
        "ustar","application/x-ustar",
        "vcd","application/x-cdlink",
        "vrml","model/vrml",
        "wav","audio/x-wav",
        "wbmp","image/vnd.wap.wbmp",
        "wml","text/vnd.wap.wml",
        "wmlc","application/vnd.wap.wmlc",
        "wmls","text/vnd.wap.wmlscript",
        "wmlsc","application/vnd.wap.wmlscriptc",
        "wtls-ca-certificate","application/vnd.wap.wtls-ca-certificate",
        "wrl","model/vrml",
        "xbm","image/x-xbitmap",
        "xml","text/xml",
        "xpm","image/x-xpixmap",
        "xwd","image/x-xwindowdump",
        "xyz","chemical/x-pdb",
        "zip","application/zip",
        "z","application/compress"
    };
    
    private final static Map __dftMimeMap = new HashMap();
    static
    {        
        for (int i=0; i<__mimeType.length; i+=2)
            __dftMimeMap.put(__mimeType[i],__mimeType[i+1]);
    }
    
    final static String[] __encodings =
    {
        "text/html",   StringUtil.__ISO_8859_1,
        "text/plain",  "US-ASCII",
        "text/xml",    "UTF-8",
    };

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
    protected HandlerContext(HttpServer httpServer,String contextPathSpec)
    {
        // check context path
        if (contextPathSpec==null ||
            contextPathSpec.indexOf(',')>=0 ||
            contextPathSpec.startsWith("*") ||
            !contextPathSpec.startsWith("/"))
            new IllegalArgumentException
                ("Illegal context spec:"+contextPathSpec);

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

            Code.warning("Unsuitable contextPathSpec "+contextPathSpec+
                         ", Assuming: "+_contextPath+"/*");
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
    public String getClassPath()
    {
        return _classPath;
    }
     
    /* ------------------------------------------------------------ */
    /** Sets the class path for the context.
     * A class path is only required for a context if it uses classes
     * that are not in the system class path.
     * @param fileBase 
     */
    public void setClassPath(String classPath)
    {
        _classPath=classPath;
        if (isStarted())
            Code.warning("classpath set while started");
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
    /** Set the class loader.
     * If a classpath is also set, this classloader is treated as
     * a parent loader to a URLClassLoader initialized with the
     * classpath.
     * Also sets the com.mortbay.HTTP.HandlerContext.classLoader
     * attribute.
     * @param loader 
     */
    public void setClassLoader(ClassLoader loader)
    {
        _parent=loader;
        if (isStarted())
            Code.warning("classpath set while started");
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
     * com.mortbay.HTTP.Handler.Servlet.Context.
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
    /** get error page URI.
     * @param error A string representing an error code or a
     * exception classname
     * @return URI within context
     */
    public Resource getErrorPageResource(String error)
    {
        if (_errorPages==null || _resourceBase==null)
            return null;
        
        String page = (String) _errorPages.get(error);
        if (page==null)
            return null;
        
        try{
            Resource resource = _resourceBase.addPath(page);
            if (resource.exists())
                return resource;
        }
        catch(IOException e)
        {
            Code.ignore(e);
        }
        return null;
    }
    
    /* ------------------------------------------------------------ */
    public String removeErrorPage(String error)
    {
        if (_errorPages==null)
            return null;
       return (String) _errorPages.remove(error);
    }
    
    /* ------------------------------------------------------------ */
    /** Set HttpServer Access.
     * If true then the HttpServer instance is available as a
     * context attribute "com.mortbay.HTTP.HttpServer".
     * This should only been done for trusted contexts.
     * @param access 
     */
    public void setHttpServerAccess(boolean access)
    {
        _httpServerAccess=access;
    }

    /* ------------------------------------------------------------ */
    /** Get HttpServer Access.
     * If true then the HttpServer instance is available as a
     * context attribute "com.mortbay.HTTP.HttpServer".
     * This should only been done for trusted contexts.
     * @return 
     */
    public boolean getHttpServerAccess()
    {
        return _httpServerAccess;
    }
    
    
    /* ------------------------------------------------------------ */
    /** Get all handlers.
     * @return 
     */
    public List getHandlers()
    {
        return _handlers;
    }

    /* ------------------------------------------------------------ */
    /** Gent the number of handlers.
     * @return 
     */
    public int getHandlerSize()
    {
        return _handlers.size();
    }
    
    
    /* ------------------------------------------------------------ */
    /** Add a handler.
     * @param i The position in the handler list
     * @param handler The handler.
     */
    public synchronized void addHandler(int i,HttpHandler handler)
    {
        _handlers.add(i,handler);
        handler.initialize(this);
    }
    
    /* ------------------------------------------------------------ */
    /** Add a HttpHandler to the context.
     * @param handler 
     */
    public synchronized void addHandler(HttpHandler handler)
    {
        addHandler(_handlers.size(),handler);
    }

    /* ------------------------------------------------------------ */
    /** Get handler by index.
     * @param i 
     * @return 
     */
    public HttpHandler getHandler(int i)
    {
        return (HttpHandler)_handlers.get(i);
    }
    
    /* ------------------------------------------------------------ */
    /** Get a handler by class.
     * @param handlerClass 
     * @return The first handler that is an instance of the handlerClass
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
    /** Remove a handler.
     * The handler must be stopped before being removed.
     * @param i 
     */
    public synchronized HttpHandler removeHandler(int i)
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
    public synchronized void removeHandler(HttpHandler handler)
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
            getHandler(com.mortbay.HTTP.Handler.ResourceHandler.class);
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
            getHandler(com.mortbay.HTTP.Handler.SecurityHandler.class);
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
            getHandler(com.mortbay.HTTP.Handler.ResourceHandler.class);
        if (serve)
        {
            if (handler==null)
                getResourceHandler();
        }
        else while (handler!=null)
        {
            _handlers.remove(handler);
            handler = (ResourceHandler)
                getHandler(com.mortbay.HTTP.Handler.ResourceHandler.class);
        }
    }

    /* ------------------------------------------------------------ */
    public boolean isServingResources()
    {
        return null!=getHandler(com.mortbay.HTTP.Handler.ResourceHandler.class);
    }
    
    /* ------------------------------------------------------------ */
    /** Set the SecurityHandler realm.
     * Conveniance method.
     * If a SecurityHandler is not in the context, one is created
     * as the 0th handler.
     * @param realmName 
     */
    public void setRealm(String realmName)
    {
        SecurityHandler sh=getSecurityHandler();
        sh.setRealmName(realmName);
    }
    
    /* ------------------------------------------------------------ */
    public String getRealm()
    {
        SecurityHandler handler = (SecurityHandler)
            getHandler(com.mortbay.HTTP.Handler.SecurityHandler.class);
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
     * Also sets the com.mortbay.HTTP.mimeMap context attribute
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
        {
            _encodingMap = new HashMap((__encodings.length*3)/4);
            for (int i=0; i<__encodings.length; i+=2)
                _encodingMap.put(__encodings[i],__encodings[i+1]);
        }
        return _encodingMap;
    }
    
    /* ------------------------------------------------------------ */
    /** Set the map of mime type to char encoding.
     * Also sets the com.mortbay.HTTP.encodingMap context attribute
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
    /** 
     * @param request 
     * @param response 
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

        // Save the thread context loader
        Thread thread = Thread.currentThread();
        ClassLoader lastContextLoader=thread.getContextClassLoader();
        HandlerContext lastHandlerContext=response.getHandlerContext();
        try
        {
            if (_loader!=null)
                thread.setContextClassLoader(_loader);
            response.setHandlerContext(this);
            
            List handlers=getHandlers();
            for (int k=0;k<handlers.size();k++)
            {
                HttpHandler handler =
                    (HttpHandler)handlers.get(k);
                
                if (!handler.isStarted())
                {
                    Code.debug(handler," not started in ",this);
                    continue;
                }
                
                if (Code.debug())
                    Code.debug("Try handler ",handler);
                
                handler.handle(pathInContext,
                               request,
                               response);
                
                if (request.isHandled())
                {
                    if (Code.debug())
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
            response.setHandlerContext(lastHandlerContext);
        }
    }
    
    /* ------------------------------------------------------------ */
    protected String getHandlerContextName()
    {
        if (_name==null)
            _name = (_hosts.size()>1?(_hosts.toString()+":"):"")+_contextPath;
        return _name;
    }
    
    /* ------------------------------------------------------------ */
    public String toString()
    {
        return "HandlerContext["+getHandlerContextName()+"]"; 
    }
    
    /* ------------------------------------------------------------ */
    public String toString(boolean detail)
    {
        return "HandlerContext["+getHandlerContextName()+"]" +
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

        MultiException mx = new MultiException();
        
        _started=true;

        getMimeMap();
        getEncodingMap();

        if (_httpServerAccess)
            setAttribute("com.mortbay.HTTP.HttpServer",_httpServer);
        else
            removeAttribute("com.mortbay.HTTP.HttpServer");
        
        // setup the context loader
        _loader=null;
        if (_parent!=null || _classPath!=null ||  this.getClass().getClassLoader()!=null)
        {
            // If no parent, then try this threads classes loader as parent
            if (_parent==null)
                _parent=Thread.currentThread().getContextClassLoader();
            
            // If no parent, then try this classes loader as parent
            if (_parent==null)
                _parent=this.getClass().getClassLoader();

            Code.debug("Init classloader from ",_classPath,
                       ", ",_parent," for ",this);
            
            if (_classPath==null || _classPath.length()==0)
                _loader=_parent;
            else
                _loader=new ContextLoader(_classPath,_parent,_permissions);
        }
        
        // Start the handlers
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
                if (!handler.isStarted())
                    try{handler.start();}catch(Exception e){mx.add(e);}
            }
        }
        finally
        {
            thread.setContextClassLoader(lastContextLoader);
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
    /** Stop all listeners then handlers.
     * All the handlers are unmapped and the listeners removed.
     */
    public synchronized void destroy()
    {
        _started=false;
        if (_httpServer!=null)
            _httpServer.remove(this);
        
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
                {
                    try{handler.destroy();}
                    catch(Exception e){Code.warning(e);}
                }
            }
        }
        finally
        {
            thread.setContextClassLoader(lastContextLoader);
        }
        
        _httpServer=null;
        _handlers.clear();
        _handlers=null;
        _classPath=null;
        _parent=null;
        _loader=null;
        _resourceBase=null;
        _attributes.clear();
        _attributes=null;
        _initParams.clear();
        _initParams=null;
        if (_mimeMap!=null)
            _mimeMap.clear();
        _mimeMap=null;
        if (_encodingMap!=null)
            _encodingMap.clear();
        _encodingMap=null;
        _hosts.clear();
        _hosts=null;
        _contextPath=null;
        _name=null;
        _redirectNullPath=false;
    }

    /* ------------------------------------------------------------ */
    public synchronized boolean isDestroyed()
    {
        return _handlers==null;
    }    
}
