
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
import com.mortbay.Util.Resource;
import com.mortbay.Util.StringUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
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
public class HandlerContext
{
    private HttpServer _httpServer;
    private List _handlers=new ArrayList(3);
    private ServletHandler _servletHandler;
    private String _classPath;
    private Resource _resourceBase;
    private Map _attributes = new HashMap(11);
    private Map _mimeMap;
    private String _name;
    private boolean _redirectNullPath=true;

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param httpServer 
     */
    HandlerContext(HttpServer httpServer)
    {
        _httpServer=httpServer;

    }
    
    /* ------------------------------------------------------------ */
    void addName(String name)
    {
      if (_name==null)
        _name=name;
      else if (_name.indexOf(":")>=0)
          _name=_name+";"+name;
      else
          _name=_name+","+name;
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
     * Also sets the com.mortbay.HTTP.classPath context attribute.
     * A class path is only required for a context if it uses classes
     * that are not in the system class path, or if class reloading is
     * to be performed.
     * @param fileBase 
     */
    public void setClassPath(String classPath)
    {
        _classPath = classPath;
        _attributes.put("com.mortbay.HTTP.classPath",classPath);
    }

    /* ------------------------------------------------------------ */
    public Resource getResourceBase()
    {
        return _resourceBase;
    }

    
    /* ------------------------------------------------------------ */
    public void setResourceBase(Resource resourceBase)
    {
        _resourceBase=resourceBase;
        _attributes.put("com.mortbay.HTTP.resourceBase",
                        _resourceBase.toString());
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
            _attributes.put("com.mortbay.HTTP.resourceBase",
                            _resourceBase.toString());
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
        _attributes.put("com.mortbay.HTTP.mimeMap",_mimeMap);
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
    /** Set a handler.
     * @param i 
     * @param handler 
     */
    public void setHandler(int i,HttpHandler handler)
    {
        _handlers.set(i,handler);
    }
    
    /* ------------------------------------------------------------ */
    /** Add a HttpHandler to the context.
     * @param handler 
     */
    public void addHandler(HttpHandler handler)
    {
        _handlers.add(handler);
        try
        {
            handler.initialize(this);
        }
        catch(InterruptedException e)
        {
            Code.warning(e);
        }
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
        if (_servletHandler==null)
        {
            _servletHandler=getServletHandler();
            if (_servletHandler==null)
            {
                _servletHandler=new ServletHandler();
                addHandler(_servletHandler);
            }
        }
        return _servletHandler.addServlet(name,pathSpec,className);
    }

    /* ------------------------------------------------------------ */
    public boolean isServingServlets()
    {
        return getServletHandler()!=null;
    }
    
    /* ------------------------------------------------------------ */
    public ServletHandler getServletHandler()
    {
        return (ServletHandler)
            getHandler(com.mortbay.HTTP.Handler.Servlet.ServletHandler.class);
    }
    
    /* ------------------------------------------------------------ */
    /** Setup context for serving dynamic servlets.
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
                addHandler(new DynamicHandler());
        }
        else if (handler!=null)
            _handlers.remove(handler);
    }

    /* ------------------------------------------------------------ */
    public boolean isServingDynamicServlets()
    {
        return getDynamicHandler()!=null;
    }
    
    /* ------------------------------------------------------------ */
    public DynamicHandler getDynamicHandler()
    {
        return (DynamicHandler)
            getHandler(com.mortbay.HTTP.Handler.Servlet.DynamicHandler.class);
    }
    
    /* ------------------------------------------------------------ */
    /** Setup context for serving Resources as files.
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
                addHandler(new ResourceHandler());
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
        return getResourceHandler()!=null;
    }
    
    /* ------------------------------------------------------------ */
    public ResourceHandler getResourceHandler()
    {
        return (ResourceHandler)
            getHandler(com.mortbay.HTTP.Handler.ResourceHandler.class);
    }
    
    /* ------------------------------------------------------------ */
    public SecurityHandler getSecurityHandler()
    {
        return (SecurityHandler)
            getHandler(com.mortbay.HTTP.Handler.SecurityHandler.class);
    }

    /* ------------------------------------------------------------ */
    public void addSecurityConstraint(String pathSpec,
                                      SecurityConstraint sc)
    {
        SecurityHandler sh=getSecurityHandler();
        if (sh==null)
        {
            sh=new SecurityHandler();
            _handlers.add(0,sh);
        }
        sh.addSecurityConstraint(pathSpec,sc);
    }


    /* ------------------------------------------------------------ */
    /** 
     * @param username 
     * @param password 
     */
    public  void addUser(String username, String password)
    {
        SecurityHandler sh=getSecurityHandler();
        if (sh==null)
        {
            sh=new SecurityHandler();
            _handlers.add(0,sh);
        }
        sh.addUser(username,password);
    }
    
    
    /* ------------------------------------------------------------ */
    /** 
     * @param name 
     * @return 
     */
    public Object getAttribute(String name)
    {
        return _attributes.get(name);
    }

    /* ------------------------------------------------------------ */
    /** 
     * @return 
     */
    public Enumeration getAttributeNames()
    {
        return Collections.enumeration(_attributes.keySet());
    }

    /* ------------------------------------------------------------ */
    /** 
     * @param name 
     * @param value 
     */
    public synchronized void setAttribute(String name, Object value)
    {
        _attributes.put(name,value);
        if ("com.mortbay.HTTP.resourceBase".equals(name))
            setResourceBase(value.toString());
        else if ("com.mortbay.HTTP.classPath".equals(name))
            _classPath=value.toString();
        else if ("com.mortbay.HTTP.mimeMap".equals(name))
            _mimeMap=(Map)value;
    }

    
    /* ------------------------------------------------------------ */
    /** 
     * @param name 
     */
    public synchronized void removeAttribute(String name)
    {
        _attributes.remove(name);
        if ("com.mortbay.HTTP.resourceBase".equals(name))
            _resourceBase=null;
        else if ("com.mortbay.HTTP.classPath".equals(name))
            _classPath=null;
        else if ("com.mortbay.HTTP.mimeMap".equals(name))
            _mimeMap=null;
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
            ext="default";
        else
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
        if (type==null)
            type = (String)_mimeMap.get("default");

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
     * @param contextPathSpec 
     * @param request 
     * @param response 
     * @exception HttpException 
     * @exception IOException 
     */
    public boolean handle(String contextPathSpec,
                          HttpRequest request,
                          HttpResponse response)
        throws HttpException, IOException
    {
        String pathInContext = request.getPath();
        String contextPath=null;
        if (!"/".equals(contextPathSpec))
        {
            contextPath=PathMap.pathMatch(contextPathSpec,pathInContext);
            pathInContext=PathMap.pathInfo(contextPathSpec,pathInContext);
        }
        
        if (_redirectNullPath && pathInContext==null)
        {
            // XXX optional and avoid //////
            StringBuffer buf=request.getRequestURL();
            buf.append("/");
            String q=request.getQuery();
            if (q!=null&&q.length()!=0)
                buf.append("?"+q);
            response.setField(HttpFields.__Location,
                              buf.toString());
            if (Code.debug())
                Code.warning("Context "+contextPathSpec+
                             " consumed all of path "+
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
                                
            handler.handle(contextPath,
                           pathInContext,
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
        return "Context["+_name+"]"; 
    }
}










