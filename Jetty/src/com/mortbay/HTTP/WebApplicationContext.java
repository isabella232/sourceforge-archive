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
import org.xml.sax.*;
import javax.servlet.*;

/* ------------------------------------------------------------ */
/**
 *
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
    Context _context;
    
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
	    __xmlParser.redirectEntity
		("web-app_2_2.dtd",
		 Resource.newSystemResource("com/mortbay/HTTP/web.dtd"));
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
	    _context=_servletHandler.getContext();
	    
	    // FileBase and ResourcePath
	    setResourceBase(_directoryName);
	    setServingResources(true);
	    ResourceHandler rh = getResourceHandler();
	    rh.setDirAllowed(true);
	    rh.setPutAllowed(true);
	    rh.setDelAllowed(true);
	    
	    _web = __xmlParser.parse(web);

	    // Standard constraint
	    SecurityConstraint sc=new SecurityConstraint();
	    sc.setName("WEB-INF");
	    sc.addRole("com.mortbay.jetty.WebApplicationContext");
	    addSecurityConstraint("/WEB-INF/*",sc);
	    addSecurityConstraint("/WEB-INF",sc);
	    
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
		initSessionConfig(node);
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
		initSecurityConstraint(node);
	    else if ("login-config".equals(name))
		initLoginConfig(node);
	    else if ("security-role".equals(name))
		Code.warning("Not implemented: "+node);
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
	
	ServletHolder holder = _servletHandler.newServletHolder(className);
	holder.setServletName(name);
	
	Iterator iter= node.iterator("init-param");
	while(iter.hasNext())
	{
	    XmlParser.Node paramNode=(XmlParser.Node)iter.next();
	    String pname=paramNode.get("param-name").toString(false);
	    String pvalue=paramNode.get("param-value").toString(false);
	    holder.put(pname,pvalue);
	}

	XmlParser.Node startup = node.get("load-on-startup");
	if (startup!=null &&
	    startup.toString(false).trim().toLowerCase().startsWith("t"))
	    holder.setInitOnStartup(true);
	
	XmlParser.Node securityRef = node.get("security-role-ref");
	if (securityRef!=null)
	{
	    // XXX - If you know what to do with this, please tell me
	    Code.warning("Not Implemented: "+securityRef+" in servlet "+name);
	}
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
    private void initSessionConfig(XmlParser.Node node)
    {
	XmlParser.Node tNode=node.get("session-timeout");
	if(tNode!=null)
	{
	    int timeout = Integer.parseInt(tNode.toString(false));
	    _context.setSessionTimeout(timeout);
	}
    }
    
    /* ------------------------------------------------------------ */
    private void initWelcomeFileList(XmlParser.Node node)
    {
	ResourceHandler rh = getResourceHandler();
	rh.setIndexFiles(null);
	
	Iterator iter= node.iterator("welcome-file");
	while(iter.hasNext())
	{
	    XmlParser.Node indexNode=(XmlParser.Node)iter.next();
	    String index=indexNode.toString(false);
	    Code.debug("Index: ",index);
	    rh.addIndexFile(index);
	}
    }

    /* ------------------------------------------------------------ */
    private void initSecurityConstraint(XmlParser.Node node)
    {
	SecurityConstraint scBase = new SecurityConstraint();
	
	XmlParser.Node auths=node.get("auth-constraint");
	Iterator iter= auths.iterator("role-name");
	while(iter.hasNext())
	{
	    XmlParser.Node role=(XmlParser.Node)iter.next();
	    scBase.addRole(role.toString(false));
	}
	XmlParser.Node data=node.get("user-data-constraint");
	if (data!=null)
	{
	    String guarantee = data.toString(false).trim().toUpperCase();
	    if (guarantee==null || guarantee.length()==0 ||
		"NONE".equals(guarantee))
		scBase.setDataConstraint(scBase.DC_NONE);
	    else if ("INTEGRAL".equals(guarantee))
		scBase.setDataConstraint(scBase.DC_INTEGRAL);
	    else if ("CONFIDENTIAL".equals(guarantee))
		scBase.setDataConstraint(scBase.DC_CONFIDENTIAL);
	    else
	    {
		Code.warning("Unknown user-data-constraint:"+guarantee);
		scBase.setDataConstraint(scBase.DC_CONFIDENTIAL);
	    }
	}

	iter= node.iterator("web-resource-collection");
	while(iter.hasNext())
	{
	    XmlParser.Node collection=(XmlParser.Node)iter.next();
	    String name=collection.get("web-resource-name").toString(false);
	    SecurityConstraint sc = (SecurityConstraint)scBase.clone();
	    sc.setName(name);
	    
	    Iterator iter2= collection.iterator("http-method");
	    while(iter2.hasNext())
		sc.addMethod(((XmlParser.Node)iter2.next()).toString(false));

	    iter2= collection.iterator("url-pattern");
	    while(iter2.hasNext())
	    {
		String url=
		    ((XmlParser.Node)iter2.next()).toString(false).trim();
		addSecurityConstraint(url,sc);
	    }
	}
    }
    
    /* ------------------------------------------------------------ */
    private void initLoginConfig(XmlParser.Node node)
    {
	SecurityHandler sh = getSecurityHandler();
	if (sh==null)
	    return;

	XmlParser.Node method=node.get("auth-method");
	if (method!=null)
	    sh.setAuthMethod(method.toString(false));
	XmlParser.Node name=node.get("realm-name");
	if (name!=null)
	    sh.setAuthRealm(name.toString(false));
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
