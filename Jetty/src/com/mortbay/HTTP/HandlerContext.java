// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;

import com.mortbay.Util.*;
import com.sun.java.util.collections.*;
import java.io.*;
import java.net.*;
import java.util.*;
import com.mortbay.HTTP.Handler.*;
import com.mortbay.HTTP.Handler.Servlet.*;
import java.lang.reflect.*;

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
 * @version 1.0 Sat Jun 17 2000
 * @author Greg Wilkins (gregw)
 */
public class HandlerContext
{
    private HttpServer _httpServer;
    private List _handlers=new ArrayList(3);
    private ServletHandler _servletHandler;
    private String _fileBase;
    private String _classPath;
    private String _resourceBase;
    private Map _attributes = new HashMap(11);
    private Map _mimeMap ;

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param httpServer 
     */
    HandlerContext(HttpServer httpServer)
    {
	_httpServer=httpServer;
    }

    
    /* ------------------------------------------------------------ */
    public HttpServer getHttpServer()
    {
	return _httpServer;
    }

    /* ------------------------------------------------------------ */
    public String getFileBase()
    {
	return _fileBase;
    }

    /* ------------------------------------------------------------ */
    /** Sets the file base for the context.
     * Also sets the com.mortbay.HTTP.fileBase context attribute
     * @param fileBase A directory name.
     */
    public void setFileBase(String fileBase)
    {
	_fileBase = fileBase;
	_attributes.put("com.mortbay.HTTP.fileBase",fileBase);
    }

    /* ------------------------------------------------------------ */
    public String getClassPath()
    {
	return _classPath;
    }
    
    /* ------------------------------------------------------------ */
    /** Sets the class path for the context.
     * Also sets the com.mortbay.HTTP.classPath context attribute
     * @param fileBase 
     */
    public void setClassPath(String classPath)
    {
	_classPath = classPath;
	_attributes.put("com.mortbay.HTTP.classPath",classPath);
    }

    /* ------------------------------------------------------------ */
    public String getResourceBase()
    {
	return _resourceBase;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * If a relative file is passed, it is converted to a file
     * URL based on the current working directory.
     * Also sets the com.mortbay.HTTP.resouceBase context attribute
     * @param resourceBase A URL prefix or directory name.
     */
    public void setResourceBase(String resourceBaseUrl)
    {
	if (resourceBaseUrl!=null)
	{
	    if( resourceBaseUrl.startsWith("./"))
		_resourceBase =
		    "file:"+
		    System.getProperty("user.dir")+
		    resourceBaseUrl.substring(1);
	    else if (resourceBaseUrl.startsWith("/"))
		_resourceBase = "file:"+resourceBaseUrl;
	    else
		_resourceBase = resourceBaseUrl;
	}
	else
	    _resourceBase = resourceBaseUrl;
	_attributes.put("com.mortbay.HTTP.resourceBase",_resourceBase);
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
    /** 
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
     * @param pathSpec 
     * @param className 
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
	if (_servletHandler==null)
	{
	    _servletHandler=getServletHandler();
	    if (_servletHandler==null)
	    {
		_servletHandler=new ServletHandler();
		addHandler(_servletHandler);
	    }
	}
	return _servletHandler.addServlet(pathSpec,className);
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
    /** Setup context for serving files.
     * @param serve If true and there is no FileHandler instance in the
     * context, a FileHandler is added. If false, all FileHandler instances
     * are removed from the context.
     */
    public synchronized void setServingFiles(boolean serve)
    {
	FileHandler handler = (FileHandler)
	    getHandler(com.mortbay.HTTP.Handler.FileHandler.class);
	if (serve)
	{
	    if (handler==null)
		addHandler(new FileHandler());
	}
	else while (handler!=null)
	{
	    _handlers.remove(handler);
	    handler = (FileHandler)
	    getHandler(com.mortbay.HTTP.Handler.FileHandler.class);
	}
    }
    

    /* ------------------------------------------------------------ */
    public boolean isServingFiles()
    {
	return getFileHandler()!=null;
    }

    /* ------------------------------------------------------------ */
    public FileHandler getFileHandler()
    {
	return (FileHandler)
	    getHandler(com.mortbay.HTTP.Handler.FileHandler.class);
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
	    _resourceBase=value.toString();
	else if ("com.mortbay.HTTP.classPath".equals(name))
	    _classPath=value.toString();
	else if ("com.mortbay.HTTP.fileBase".equals(name))
	    _fileBase=value.toString();
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
	else if ("com.mortbay.HTTP.fileBase".equals(name))
	    _fileBase=null;
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
        int i=filename.indexOf(".");
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

    
}










