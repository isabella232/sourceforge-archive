
// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;

import com.mortbay.HTTP.Handler.ResourceHandler;
import com.mortbay.HTTP.Handler.SecurityHandler;
import com.mortbay.HTTP.Handler.Servlet.DynamicHandler;
import com.mortbay.HTTP.Handler.Servlet.ServletHandler;
import com.mortbay.HTTP.Handler.Servlet.ServletHolder;
import com.mortbay.Util.Code;
import com.mortbay.Util.IO;
import com.mortbay.Util.Resource;
import com.mortbay.Util.StringUtil;
import com.mortbay.Util.LifeCycle;
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
 * Convenience methods are provided for adding file and servlet
 * handlers.
 *
 * @see HttpServer
 * @see HttpHandler
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class HandlerContext implements LifeCycle
{
    public final static String
        __ResourceBase="com.mortbay.HTTP.HandlerContext.resourceBase",
        __ClassPath="com.mortbay.HTTP.HandlerContext.classPath",
        __ClassLoader="com.mortbay.HTTP.HandlerContext.classLoader",
        __MimeMap="com.mortbay.HTTP.HandlerContext.mimeMap";
    
    private HttpServer _httpServer;
    private List _handlers=new ArrayList(3);
    private String _classPath;
    private ClassLoader _parent;
    private ClassLoader _loader;
    private Resource _resourceBase;
    private boolean _started;

    private Map _attributes = new HashMap(11);
    private Map _initParams = new HashMap(11);
    private Map _mimeMap;
    
    private List _hosts=new ArrayList(2);
    private String _contextPath;
    private String _name;
    private boolean _redirectNullPath=true;

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param httpServer 
     * @param contextPathSpec 
     */
    HandlerContext(HttpServer httpServer,String contextPathSpec)
    {
        // check context path
        if (contextPathSpec==null ||
            contextPathSpec.indexOf(",")>=0 ||
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
        _name=_contextPath;
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
     * @return attribute value or null
     */
    public Object getAttribute(String name)
    {
        if (__ClassLoader.equals(name))
            return getClassLoader();
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
     * @param value attribute value
     */
    public synchronized void setAttribute(String name, Object value)
    {
        if (__ResourceBase.equals(name))
            setResourceBase(value.toString());
        else if (__ClassPath.equals(name))
            setClassPath(value.toString());
        else if (__ClassLoader.equals(name))
            setClassLoader((ClassLoader)value);
        else if (__MimeMap.equals(name))
        {
            _mimeMap=(Map)value;
            _attributes.put(name,value);
        }
        else
            _attributes.put(name,value);
    }

    
    /* ------------------------------------------------------------ */
    /** 
     * @param name attribute name
     */
    public synchronized void removeAttribute(String name)
    {
        if (__ResourceBase.equals(name))
            setResourceBase((String)null);
        else if (__ClassPath.equals(name))
            setClassPath(null);
        else if (__ClassLoader.equals(name))
            setClassLoader(null);
        else if (__MimeMap.equals(name))
        {
            _mimeMap=null;
            _attributes.remove(name);
        }
        else
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

        _name = ((host!=null ||_hosts.size()>1)
                 ?(_hosts.toString()+":")
                 :"")+
            _contextPath;
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
     * Also sets the com.mortbay.HTTP.HandlerContext.classPath attribute.
     * A class path is only required for a context if it uses classes
     * that are not in the system class path, or if class reloading is
     * to be performed.
     * @param fileBase 
     */
    public void setClassPath(String classPath)
    {
        _classPath=classPath;
        _attributes.put(__ClassPath,classPath);
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
        _attributes.put(__ClassLoader,loader);
    }
   
    /* ------------------------------------------------------------ */
    public Resource getResourceBase()
    {
        return _resourceBase;
    }
    
    /* ------------------------------------------------------------ */
    public void setResourceBase(Resource resourceBase)
    {
        Code.debug("resourceBase=",resourceBase," for ", this);
        _resourceBase=resourceBase;
        _attributes.put(__ResourceBase,_resourceBase.toString());
    }
    
    /* ------------------------------------------------------------ */
    /**
     * If a relative file is passed, it is converted to a file
     * URL based on the current working directory.
     * Also sets the com.mortbay.HTTP.resouceBase context attribute
     * @param resourceBase A URL prefix or directory name.
     */
    public void setResourceBase(String resourceBase)
    {
        try{
            _resourceBase=Resource.newResource(resourceBase);
            _attributes.put(__ResourceBase,
                            _resourceBase.toString());
            Code.debug("resourceBase=",_resourceBase," for ", this);
        }
        catch(IOException e)
        {
            Code.debug(e);
            throw new IllegalArgumentException(resourceBase+":"+e.toString());
        }
    }
    
    /* ------------------------------------------------------------ */
    public Map getMimeMap()
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
        _attributes.put(__MimeMap,_mimeMap);
    }
    
    /* ------------------------------------------------------------ */
    /** Get all handlers
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
    /** Add a servlet to the context.
     * Conveniance method.
     * If no ServletHandler is found in the context, a new one is added.
     * @param name The name of the servlet.
     * @param pathSpec The pathspec within the context
     * @param className The classname of the servlet.
     * @return The ServletHolder.
     * @exception ClassNotFoundException 
     * @exception InstantiationException 
     * @exception IllegalAccessException 
     */
    public synchronized ServletHolder addServlet(String pathSpec,
                                                 String className)
        throws ClassNotFoundException,
               InstantiationException,
               IllegalAccessException
    {
        return addServlet(className,pathSpec,className);
    }
    
    /* ------------------------------------------------------------ */
    /** Add a servlet to the context.
     * If no ServletHandler is found in the context, a new one is added.
     * @param name The name of the servlet.
     * @param pathSpec The pathspec within the context
     * @param className The classname of the servlet.
     * @return The ServletHolder.
     * @exception ClassNotFoundException 
     * @exception InstantiationException 
     * @exception IllegalAccessException 
     */
    public synchronized ServletHolder addServlet(String name,
                                                 String pathSpec,
                                                 String className)
        throws ClassNotFoundException,
               InstantiationException,
               IllegalAccessException
    {
        return getServletHandler().addServlet(name,pathSpec,className);
    }

    /* ------------------------------------------------------------ */
    /** Get the context ServletHandler.
     * Conveniance method. If no ServletHandler exists, a new one is added to
     * the context.
     * @return ServletHandler
     */
    public synchronized ServletHandler getServletHandler()
    {
        ServletHandler servletHandler= (ServletHandler)
            getHandler(com.mortbay.HTTP.Handler.Servlet.ServletHandler.class);
        if (servletHandler==null)
        {
            servletHandler=new ServletHandler();
            addHandler(servletHandler);
        }
        return servletHandler;
    }
    
    /* ------------------------------------------------------------ */
    /** Get the context Dynamic Servlet Handler.
     * Conveniance method. If no dynamicHandler exists, a new one is added to
     * the context.
     * @return DynamicHandler
     */
    public DynamicHandler getDynamicHandler()
    {
        DynamicHandler dynamicHandler= (DynamicHandler)
            getHandler(com.mortbay.HTTP.Handler.Servlet.DynamicHandler.class);
        if (dynamicHandler==null)
        {
            dynamicHandler=new DynamicHandler();
            addHandler(dynamicHandler);
        }
        return dynamicHandler;
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
     * the context.
     * @return SecurityHandler
     */
    public SecurityHandler getSecurityHandler()
    {
        SecurityHandler securityHandler= (SecurityHandler)
            getHandler(com.mortbay.HTTP.Handler.SecurityHandler.class);
        if (securityHandler==null)
        {
            securityHandler=new SecurityHandler();
            addHandler(securityHandler);
        }
        return securityHandler;
    }
    
    /* ------------------------------------------------------------ */
    /** Setup context for serving dynamic servlets.
     * Conveniance method.  A Dynamic servlet is one which is mapped from a
     * URL containing the class name of the servlet - which is dynamcially
     * loaded when the first request is received.
     * @param serve If true and there is no DynamicHandler instance in the
     * context, a dynamicHandler is added. If false, all DynamicHandler
     * instances are removed from the context.
     */
    public synchronized void setServingDynamicServlets(boolean serve)
    {
        HttpHandler handler = (DynamicHandler)
            getHandler(com.mortbay.HTTP.Handler.Servlet.DynamicHandler.class);
        if (serve)
        {
            if (handler==null)
                getDynamicHandler();
        }
        else if (handler!=null)
            _handlers.remove(handler);
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
    /** Set the SecurityHandler realm.
     * Conveniance method.
     * If a SecurityHandler is not in the context, one is created
     * as the 0th handler.
     * @param realmName 
     */
    public void setRealm(String realmName)
    {
        SecurityHandler sh=getSecurityHandler();
        sh.setRealm(realmName);
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
    /** 
     * @param filename 
     * @return 
     */
    public String getMimeByExtension(String filename)
    {
        int i=filename.lastIndexOf(".");
        String ext;

        if (i<0 || i>=filename.length())
            return null;
        
        ext=StringUtil.asciiToLowerCase(filename.substring(i+1));
        
        if (_mimeMap==null)
        {
            _mimeMap = new HashMap();
            _mimeMap.put("default","application/octet-stream");
            _mimeMap.put("ai","application/postscript");
            _mimeMap.put("aif","audio/x-aiff");
            _mimeMap.put("aifc","audio/x-aiff");
            _mimeMap.put("aiff","audio/x-aiff");
            _mimeMap.put("asc","text/plain");
            _mimeMap.put("au","audio/basic");
            _mimeMap.put("avi","video/x-msvideo");
            _mimeMap.put("bcpio","application/x-bcpio");
            _mimeMap.put("bin","application/octet-stream");
            _mimeMap.put("cdf","application/x-netcdf");
            _mimeMap.put("class","application/octet-stream");
            _mimeMap.put("cpio","application/x-cpio");
            _mimeMap.put("cpt","application/mac-compactpro");
            _mimeMap.put("csh","application/x-csh");
            _mimeMap.put("css","text/css");
            _mimeMap.put("dcr","application/x-director");
            _mimeMap.put("dir","application/x-director");
            _mimeMap.put("dms","application/octet-stream");
            _mimeMap.put("doc","application/msword");
            _mimeMap.put("dvi","application/x-dvi");
            _mimeMap.put("dxr","application/x-director");
            _mimeMap.put("eps","application/postscript");
            _mimeMap.put("etx","text/x-setext");
            _mimeMap.put("exe","application/octet-stream");
            _mimeMap.put("ez","application/andrew-inset");
            _mimeMap.put("gif","image/gif");
            _mimeMap.put("gtar","application/x-gtar");
            _mimeMap.put("hdf","application/x-hdf");
            _mimeMap.put("hqx","application/mac-binhex40");
            _mimeMap.put("html","text/html");
            _mimeMap.put("htm","text/html");
            _mimeMap.put("ice","x-conference/x-cooltalk");
            _mimeMap.put("ief","image/ief");
            _mimeMap.put("iges","model/iges");
            _mimeMap.put("igs","model/iges");
            _mimeMap.put("java","text/plain");
            _mimeMap.put("jpeg","image/jpeg");
            _mimeMap.put("jpe","image/jpeg");
            _mimeMap.put("jpg","image/jpeg");
            _mimeMap.put("js","application/x-javascript");
            _mimeMap.put("jsp","text/plain");
            _mimeMap.put("kar","audio/midi");
            _mimeMap.put("latex","application/x-latex");
            _mimeMap.put("lha","application/octet-stream");
            _mimeMap.put("lzh","application/octet-stream");
            _mimeMap.put("man","application/x-troff-man");
            _mimeMap.put("me","application/x-troff-me");
            _mimeMap.put("mesh","model/mesh");
            _mimeMap.put("mid","audio/midi");
            _mimeMap.put("midi","audio/midi");
            _mimeMap.put("mif","application/vnd.mif");
            _mimeMap.put("movie","video/x-sgi-movie");
            _mimeMap.put("mov","video/quicktime");
            _mimeMap.put("mp2","audio/mpeg");
            _mimeMap.put("mp3","audio/mpeg");
            _mimeMap.put("mpeg","video/mpeg");
            _mimeMap.put("mpe","video/mpeg");
            _mimeMap.put("mpga","audio/mpeg");
            _mimeMap.put("mpg","video/mpeg");
            _mimeMap.put("ms","application/x-troff-ms");
            _mimeMap.put("msh","model/mesh");
            _mimeMap.put("nc","application/x-netcdf");
            _mimeMap.put("oda","application/oda");
            _mimeMap.put("pbm","image/x-portable-bitmap");
            _mimeMap.put("pdb","chemical/x-pdb");
            _mimeMap.put("pdf","application/pdf");
            _mimeMap.put("pgm","image/x-portable-graymap");
            _mimeMap.put("pgn","application/x-chess-pgn");
            _mimeMap.put("png","image/png");
            _mimeMap.put("pnm","image/x-portable-anymap");
            _mimeMap.put("ppm","image/x-portable-pixmap");
            _mimeMap.put("ppt","application/vnd.ms-powerpoint");
            _mimeMap.put("ps","application/postscript");
            _mimeMap.put("qt","video/quicktime");
            _mimeMap.put("ra","audio/x-pn-realaudio");
            _mimeMap.put("ra","audio/x-realaudio");
            _mimeMap.put("ram","audio/x-pn-realaudio");
            _mimeMap.put("ras","image/x-cmu-raster");
            _mimeMap.put("rgb","image/x-rgb");
            _mimeMap.put("rm","audio/x-pn-realaudio");
            _mimeMap.put("roff","application/x-troff");
            _mimeMap.put("rpm","application/x-rpm");
            _mimeMap.put("rpm","audio/x-pn-realaudio");
            _mimeMap.put("rtf","application/rtf");
            _mimeMap.put("rtf","text/rtf");
            _mimeMap.put("rtx","text/richtext");
            _mimeMap.put("sgml","text/sgml");
            _mimeMap.put("sgm","text/sgml");
            _mimeMap.put("sh","application/x-sh");
            _mimeMap.put("shar","application/x-shar");
            _mimeMap.put("silo","model/mesh");
            _mimeMap.put("sit","application/x-stuffit");
            _mimeMap.put("skd","application/x-koan");
            _mimeMap.put("skm","application/x-koan");
            _mimeMap.put("skp","application/x-koan");
            _mimeMap.put("skt","application/x-koan");
            _mimeMap.put("smi","application/smil");
            _mimeMap.put("smil","application/smil");
            _mimeMap.put("snd","audio/basic");
            _mimeMap.put("spl","application/x-futuresplash");
            _mimeMap.put("src","application/x-wais-source");
            _mimeMap.put("sv4cpio","application/x-sv4cpio");
            _mimeMap.put("sv4crc","application/x-sv4crc");
            _mimeMap.put("swf","application/x-shockwave-flash");
            _mimeMap.put("t","application/x-troff");
            _mimeMap.put("tar","application/x-tar");
            _mimeMap.put("tcl","application/x-tcl");
            _mimeMap.put("tex","application/x-tex");
            _mimeMap.put("texi","application/x-texinfo");
            _mimeMap.put("texinfo","application/x-texinfo");
            _mimeMap.put("tiff","image/tiff");
            _mimeMap.put("tif","image/tiff");
            _mimeMap.put("tr","application/x-troff");
            _mimeMap.put("tsv","text/tab-separated-values");
            _mimeMap.put("txt","text/plain");
            _mimeMap.put("ustar","application/x-ustar");
            _mimeMap.put("vcd","application/x-cdlink");
            _mimeMap.put("vrml","model/vrml");
            _mimeMap.put("wav","audio/x-wav");
            _mimeMap.put("wbmp","image/vnd.wap.wbmp");
            _mimeMap.put("wml","text/vnd.wap.wml");
            _mimeMap.put("wmlc","application/vnd.wap.wmlc");
            _mimeMap.put("wmls","text/vnd.wap.wmlscript");
            _mimeMap.put("wmlsc","application/vnd.wap.wmlscriptc");
            _mimeMap.put("wtls-ca-certificate","application/vnd.wap.wtls-ca-certificate");
            _mimeMap.put("wrl","model/vrml");
            _mimeMap.put("xbm","image/x-xbitmap");
            _mimeMap.put("xml","text/xml");
            _mimeMap.put("xpm","image/x-xpixmap");
            _mimeMap.put("xwd","image/x-xwindowdump");
            _mimeMap.put("xyz","chemical/x-pdb");
            _mimeMap.put("zip","application/zip");
        }
        
        String type = (String)_mimeMap.get(ext);
        return type;
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param extension 
     * @param type 
     */
    public void setMimeMapping(String extension,String type)
    {
        getMimeByExtension("default");
        _mimeMap.put(extension,type);
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
    
    
    /* ------------------------------------------------------------ */
    public String toString()
    {
        return "HandlerContext["+_name+"]"; 
    }
    
    /* ------------------------------------------------------------ */
    public String toString(boolean detail)
    {
        return "HandlerContext["+_name+"]" +
            (detail?("="+_handlers):""); 
    }
    
    /* ------------------------------------------------------------ */
    /** Start all handlers then listeners.
     */
    public synchronized void start()
    {
        if (_httpServer==null)
            throw new IllegalStateException("No server for "+this);
        
        _started=true;
        
        // setup the context loader
        _loader=null;
        if (_parent!=null || _classPath!=null ||  this.getClass().getClassLoader()!=null)
        {
            // If no parent, then try this classes loader as parent
            if (_parent==null)
                _parent=this.getClass().getClassLoader();
            
            Code.debug("Init classloader from "+_classPath+
                       ", "+_parent+" for "+this);
            
            if (_classPath==null || _classPath.length()==0)
                _loader=_parent;
            else
                _loader=new ContextLoader(_classPath,_parent);
            _attributes.put(__ClassLoader,_loader);
        }
        
        // Start the handlers
        Iterator handlers = _handlers.iterator();
        while(handlers.hasNext())
        {
            HttpHandler handler=(HttpHandler)handlers.next();
            if (!handler.isStarted())
                handler.start();
        }
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
        _loader=null;
    }
    
    /* ------------------------------------------------------------ */
    /** Stop all listeners then handlers.
     * All the handlers are unmapped and the listeners removed.
     */
    public synchronized void destroy()
    {
        _started=false;
        Iterator handlers = _handlers.iterator();
        while(handlers.hasNext())
        {
            HttpHandler handler=(HttpHandler)handlers.next();
            {
                try{handler.destroy();}
                catch(Exception e){Code.warning(e);}
            }
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
        _mimeMap=null;
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
