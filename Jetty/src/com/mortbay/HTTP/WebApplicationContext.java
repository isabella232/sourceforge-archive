// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;

import com.mortbay.Util.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.util.*;
import com.mortbay.HTTP.Handler.*;
import com.mortbay.HTTP.Handler.Servlet.*;
import java.lang.reflect.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import javax.servlet.*;

/* ------------------------------------------------------------ */
/** 
 * <p>
 *
 *
 * @see
 * @version 1.0 Tue Jul 18 2000
 * @author Greg Wilkins (gregw)
 */
public class WebApplicationContext extends HandlerContext
{
    /* ------------------------------------------------------------ */
    /** XXX - needs to be configured
     */
    public static final File __dtd = new File("./etc/web.dtd");
    
	
    /* ------------------------------------------------------------ */
    private String _name;
    private File _directory;
    private String _directoryName;
    private Document _web;
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
	    // XXX need to add jar files to classpath

	    // Add servlet Handler
	    addHandler(new ServletHandler());
	    _servletHandler = getServletHandler();

	    // FileBase and ResourcePath
	    setFileBase(_directoryName);
	    setResourceBase("file:"+_directoryName);
	    setServingFiles(true);
	    
	    // parse the web.xml file
	    DocumentBuilder docBuilder =
		DocumentBuilderFactory.newInstance().newDocumentBuilder();
	    docBuilder.setEntityResolver(new EntityResolver()
		{
		    public InputSource resolveEntity (String pid, String sid)
			throws IOException
		    {
			if (__dtd!=null && __dtd.exists() &&
			    sid.equals("http://java.sun.com/j2ee/dtds/web-app_2_2.dtd"))
			    return new InputSource(new FileReader(__dtd));
			return null;
		    }
		});
	    _web = docBuilder.parse (web);
	    _web.getDocumentElement ().normalize();
	    
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
	NodeList children=_web.getDocumentElement().getChildNodes();
	for (int c=0;children!=null &&c<children.getLength();c++)
	{
	    Node child=children.item(c);
	    if (child.getNodeType()!=Node.ELEMENT_NODE)
		continue;

	    String name=child.getNodeName();

	    if ("display-name".equals(name))
		initDisplayName(child);
	    else if ("description".equals(name))
	    {
		Code.warning("Not implemented: "+name);
		dump("",child);
	    }
	    else if ("distributable".equals(name))
	    {
		Code.warning("Not implemented: "+name);
		dump("",child);
	    }
	    else if ("context-param".equals(name))
		initContextParam(child);
	    else if ("servlet".equals(name))
		initServlet(child);
	    else if ("servlet-mapping".equals(name))
		initServletMapping(child);
	    else if ("session-config".equals(name))
	    {
		Code.warning("Not implemented: "+name);
		dump("",child);
	    }
	    else if ("mime-mapping".equals(name))
	    {
		Code.warning("Not implemented: "+name);
		dump("",child);
	    }
	    else if ("welcome-file-list".equals(name))
		initWelcomeFileList(child);
	    else if ("error-page".equals(name))
	    {
		Code.warning("Not implemented: "+name);
		dump("",child);
	    }
	    else if ("taglib".equals(name))
	    {
		Code.warning("Not implemented: "+name);
		dump("",child);
	    }
	    else if ("resource-ref".equals(name))
	    {
		Code.warning("Not implemented: "+name);
		dump("",child);
	    }
	    else if ("security-constraint".equals(name))
	    {
		Code.warning("Not implemented: "+name);
		dump("",child);
	    }
	    else if ("login-config".equals(name))
	    {
		Code.warning("Not implemented: "+name);
		dump("",child);
	    }
	    else if ("security-role".equals(name))
	    {
		Code.warning("Not implemented: "+name);
		dump("",child);
	    }
	    else if ("env-entry".equals(name))
	    {
		Code.warning("Not implemented: "+name);
		dump("",child);
	    }
	    else if ("ejb-ref".equals(name))
	    {
		Code.warning("Not implemented: "+name);
		dump("",child);
	    }
	    else
	    {
		Code.warning("UNKNOWN TAG: "+name);
		dump("",child);
	    }
	}
    }

    /* ------------------------------------------------------------ */
    private void initDisplayName(Node node)
    {
	_name=text(node.getFirstChild(),false);
    }
    
    /* ------------------------------------------------------------ */
    private void initContextParam(Node node)
    {
	NodeList list = node.getChildNodes();
	String name=value(list,"param-name");
	String value=value(list,"param-value");
	Code.debug("ContextParam: ",name,"=",value);
	setAttribute(name,value); 
    }

    /* ------------------------------------------------------------ */
    private void initServlet(Node node)
	throws ClassNotFoundException, UnavailableException
    {
	NodeList list = node.getChildNodes();
	String name=value(list,"servlet-name");
	String className=value(list,"servlet-class");

	Map params=null;
	Iterator iter= new NodeIterator(list,"init-param");
	while(iter.hasNext())
	{
	    if (params==null)
		params=new HashMap(7);
	    Node paramNode=(Node)iter.next();
	    NodeList plist = node.getChildNodes();
	    String pname=value(list,"param-name");
	    String pvalue=value(list,"param-value");
	    params.put(pname,pvalue);
	}
	Code.debug(name,"=",className,": ",params);

	ServletHolder holder = _servletHandler.newServletHolder(className);
	holder.setServletName(name);
	if(params!=null)
	    holder.setProperties(params);
    }

    /* ------------------------------------------------------------ */
    private void initServletMapping(Node node)
    {
	NodeList list = node.getChildNodes();
	String name=value(list,"servlet-name");
	String pathSpec=value(list,"url-pattern");

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
    private void initWelcomeFileList(Node node)
    {
	FileHandler fh = getFileHandler();
	fh.setIndexFiles(null);
	
	Iterator iter= new NodeIterator(node.getChildNodes(),
					"welcome-file");
	while(iter.hasNext())
	{
	    Node indexNode=(Node)iter.next();
	    String index=indexNode.getFirstChild().getNodeValue();
	    Code.debug("Index: ",index);
	    fh.addIndexFile(index);
	}
    }
    
    
    /* ------------------------------------------------------------ */
    private String text(Node node,boolean deep)
    {
	StringBuffer buf = new StringBuffer();
	synchronized(buf)
	{
	    text(buf,node,deep);
	    return buf.toString();
	}
    }
    
    /* ------------------------------------------------------------ */
    private void text(StringBuffer buf, Node node,boolean deep)
    {
	int t = node.getNodeType();
	if (t==Node.TEXT_NODE)
	    buf.append(node.getNodeValue());

	if (deep)
	{
	    NodeList children=node.getChildNodes();
	    for (int c=0;children!=null &&c<children.getLength();c++)
		text(buf,children.item(c),deep);
	}
    }

    /* ------------------------------------------------------------ */
    private String value(NodeList list,String element)
    {
	for (int c=0;list!=null &&c<list.getLength();c++)
	{
	    Node child = list.item(c);   
	    int t = child.getNodeType();
	    if (t==Node.ELEMENT_NODE && element.equals(child.getNodeName()))
		return text(child.getFirstChild(),false);
	}
	return null;
    }
	
    /* ------------------------------------------------------------ */
    private void dump(String indent,Node node)
    {
	int t = node.getNodeType();
	String n=node.getNodeName();
	String v=node.getNodeValue();

	if (t==Node.ELEMENT_NODE)
	{   
	    System.out.println (indent+n);
	}
	else if (t==Node.TEXT_NODE)
	{
	    System.out.println (indent+v);
	}
	
	NodeList children=node.getChildNodes();
	for (int c=0;children!=null &&c<children.getLength();c++)
	    dump(indent+"  ",children.item(c));
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
    
    /* ------------------------------------------------------------ */
    private static class NodeIterator implements Iterator
    {
	int c=0;
	NodeList _list;
	String _element;
	Node _child=null;
	
	/* ------------------------------------------------------------ */
	NodeIterator(NodeList list,String element)
	{
	    _list=list;
	    _element=element;
	}

	/* ------------------------------------------------------------ */
	public boolean hasNext()
	{
	    if (_child!=null)
		return true;
	    while (c<_list.getLength())
	    {
		Node child = _list.item(c);   
		int t = child.getNodeType();
		if (t==Node.ELEMENT_NODE && _element.equals(child.getNodeName()))
		{
		    _child=child;
		    return true;
		}
		c++;
	    }
	    return false;
	}
	
	/* ------------------------------------------------------------ */
	public Object next()
	{
	    try
	    {
		if (hasNext())
		    return _child;
		throw new NoSuchElementException();
	    }
	    finally
	    {
		_child=null;
		c++;
	    }
	}

	/* ------------------------------------------------------------ */
	public void remove()
	{
	    throw new UnsupportedOperationException("Not supported");
	}
	
    }
    
}

    
    





