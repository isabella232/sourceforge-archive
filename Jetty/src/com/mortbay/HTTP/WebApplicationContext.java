// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;
//import com.sun.java.util.collections.*; XXX-JDK1.1
import java.util.*; //XXX-JDK1.2

import com.mortbay.Util.*;
import java.util.NoSuchElementException;
import java.io.*;
import java.net.*;
import com.mortbay.HTTP.Handler.*;
import com.mortbay.HTTP.Handler.Servlet.*;
import java.lang.reflect.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import javax.servlet.*;

/* ------------------------------------------------------------ */
/** 
 * <p>
 *
 *
 * @see
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class WebApplicationContext extends HandlerContext
{
    static private XmlParser __xmlParser=null;    
	
    /* ------------------------------------------------------------ */
    private String _name;
    private File _directory;
    private String _directoryName;
    private XmlParser.Node _web;
    private ServletHandler _servletHandler;
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param httpServer 
     * @param directory 
     */
    WebApplicationContext(HttpServer httpServer, String directory)
	throws IOException
    {
	super(httpServer);

	if (__xmlParser==null)
	{
	    __xmlParser=new XmlParser();
	    // XXX - need to configure this better
	    __xmlParser.addLocalDTD("web-app_2_2.dtd",
				    new File(System.getProperty("user.dir")+
					     "/etc/web.dtd"));
	}

	File _directory = new File(directory);
	if (!_directory.exists() || !_directory.isDirectory())
	    throw new IllegalArgumentException("No such directory: "+directory);
	_directoryName=_directory.getCanonicalPath();

	// Check web.xml file
	File web = new File(_directoryName+
			    File.separatorChar+
			    "WEB-INF"+
			    File.separatorChar+
			    "web.xml");
	if (!web.exists())
	    throw new IllegalArgumentException("No web file: "+web);

	try
	{
	    // Set the classpath
	    File classes = new File(_directoryName+
				    File.separatorChar+
				    "WEB-INF"+
				    File.separatorChar+
				    "classes");
	    if (classes.exists())
		setClassPath(classes.getCanonicalPath());
	    // XXX - need to add jar files to classpath

	    
	    // Add servlet Handler
	    addHandler(new ServletHandler());
	    _servletHandler = getServletHandler();

	    // FileBase and ResourcePath
	    setResourceBase(_directoryName);
	    setServingResources(true);
	    
	    _web = __xmlParser.parse(web);	    
	    initialize();
	}
	catch(IOException e)
	{
	    Code.warning("Parse error on "+_directoryName,e);
	    throw e;
	}	
	catch(Exception e)
	{
	    Code.warning("Configuration error "+_directoryName,e);
	    throw new IOException("Parse error on "+_directoryName+
				  ": "+e.toString());
	}
    }


    /* ------------------------------------------------------------ */
    private void initialize()
	throws ClassNotFoundException,UnavailableException
    {
	Iterator iter=_web.iterator();
	while (iter.hasNext())
	{
	    XmlParser.Node node=(XmlParser.Node)iter.next();
	    String name=node.getTag();

	    if ("display-name".equals(name))
		initDisplayName(node);
	    else if ("description".equals(name))
	    {
		Code.warning("Not implemented: "+name);
		System.err.println(node);
	    }
	    else if ("distributable".equals(name))
	    {
		Code.warning("Not implemented: "+name);
		System.err.println(node);
	    }
	    else if ("context-param".equals(name))
		initContextParam(node);
	    else if ("servlet".equals(name))
		initServlet(node);
	    else if ("servlet-mapping".equals(name))
		initServletMapping(node);
	    else if ("session-config".equals(name))
	    {
		Code.warning("Not implemented: "+name);
		System.err.println(node);
	    }
	    else if ("mime-mapping".equals(name))
	    {
		Code.warning("Not implemented: "+name);
		System.err.println(node);
	    }
	    else if ("welcome-file-list".equals(name))
		initWelcomeFileList(node);
	    else if ("error-page".equals(name))
	    {
		Code.warning("Not implemented: "+name);
		System.err.println(node);
	    }
	    else if ("taglib".equals(name))
	    {
		Code.warning("Not implemented: "+name);
		System.err.println(node);
	    }
	    else if ("resource-ref".equals(name))
	    {
		Code.warning("Not implemented: "+name);
		System.err.println(node);
	    }
	    else if ("security-constraint".equals(name))
	    {
		Code.warning("Not implemented: "+name);
		System.err.println(node);
	    }
	    else if ("login-config".equals(name))
	    {
		Code.warning("Not implemented: "+name);
		System.err.println(node);
	    }
	    else if ("security-role".equals(name))
	    {
		Code.warning("Not implemented: "+name);
		System.err.println(node);
	    }
	    else if ("env-entry".equals(name))
	    {
		Code.warning("Not implemented: "+name);
		System.err.println(node);
	    }
	    else if ("ejb-ref".equals(name))
	    {
		Code.warning("Not implemented: "+name);
		System.err.println(node);
	    }
	    else
	    {
		Code.warning("UNKNOWN TAG: "+name);
		System.err.println(node);
	    }
	}
    }

    /* ------------------------------------------------------------ */
    private void initDisplayName(XmlParser.Node node)
    {
	_name=node.toString(false);
    }
    
    /* ------------------------------------------------------------ */
    private void initContextParam(XmlParser.Node node)
    {
	String name=node.get("param-name").toString(false);
	String value=node.get("param-value").toString(false);
	Code.debug("ContextParam: ",name,"=",value);
	setAttribute(name,value); 
    }

    /* ------------------------------------------------------------ */
    private void initServlet(XmlParser.Node node)
	throws ClassNotFoundException, UnavailableException
    {
	String name=node.get("servlet-name").toString(false);
	String className=node.get("servlet-class").toString(false);

	Map params=null;
	Iterator iter= node.iterator("init-param");
	while(iter.hasNext())
	{
	    if (params==null)
		params=new HashMap(7);
	    XmlParser.Node paramNode=(XmlParser.Node)iter.next();
	    String pname=paramNode.get("param-name").toString(false);
	    String pvalue=paramNode.get("param-value").toString(false);
	    params.put(pname,pvalue);
	}
	Code.debug(name,"=",className,": ",params);

	ServletHolder holder = _servletHandler.newServletHolder(className);
	holder.setServletName(name);
	if(params!=null)
	    holder.setProperties(params);
    }

    /* ------------------------------------------------------------ */
    private void initServletMapping(XmlParser.Node node)
    {
	String name=node.get("servlet-name").toString(false);
	String pathSpec=node.get("url-pattern").toString(false);

	ServletHolder holder = _servletHandler.getServletHolder(name);
	if (holder==null)
	    Code.warning("No such servlet: "+name);
	else
	{
	    Code.debug("ServletMapping: ",name,"=",pathSpec);
	    _servletHandler.addHolder(pathSpec,holder);
	}
    }
    
    /* ------------------------------------------------------------ */
    private void initWelcomeFileList(XmlParser.Node node)
    {
	FileHandler fh = getFileHandler();
	fh.setIndexFiles(null);
	
	Iterator iter= node.iterator("welcome-file");
	while(iter.hasNext())
	{
	    XmlParser.Node indexNode=(XmlParser.Node)iter.next();
	    String index=indexNode.toString(false);
	    Code.debug("Index: ",index);
	    fh.addIndexFile(index);
	}
    }


    /* ------------------------------------------------------------ */
    /** 
     * @return 
     */
    public String toString()
    {
	if (_name!=null)
	    return "'"+_name+"' @ "+_directoryName;
	return _directoryName;
    }
}

    
    





