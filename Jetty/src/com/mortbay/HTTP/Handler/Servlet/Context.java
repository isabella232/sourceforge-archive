// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler.Servlet;

import com.sun.java.util.collections.*;
import com.mortbay.HTTP.Handler.NullHandler;
import com.mortbay.HTTP.*;
import com.mortbay.Util.*;
import java.io.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.*;
import java.net.*;


/* --------------------------------------------------------------------- */
/** ServletHandler<p>
 * This handler maps requests to servlets that implement the
 * javax.servlet.http.HttpServlet API.
 * It is configured with a PathMap of paths to ServletHolder instances.
 *
 * @see Interface.HttpHandler
 * @version $Id$
 * @author Greg Wilkins
 */
public class Context implements ServletContext
{
    /* ------------------------------------------------------------ */
    private ServletHandler _handler;
    ServletHandler getHandler(){return _handler;}

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param handler 
     */
    public Context(ServletHandler handler)
    {
	_handler=handler;
    }


    /* ------------------------------------------------------------ */
    /**
     * Returns a <code>String</code> containing the value of the named
     * context-wide initialization parameter, or <code>null</code> if the 
     * parameter does not exist.
     *
     * <p>This method can make available configuration information useful
     * to an entire "web application".  For example, it can provide a 
     * webmaster's email address or the name of a system that holds 
     * critical data.
     *
     * @param	name	a <code>String</code> containing the name of the
     *                  parameter whose value is requested
     * 
     * @return 		a <code>String</code> containing at least the 
     *			servlet container name and version number
     *
     * @see ServletConfig#getInitParameter
     */
    public String getInitParameter(String param)
    {
	Code.notImplemented();
	return null;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Returns the names of the context's initialization parameters as an
     * <code>Enumeration</code> of <code>String</code> objects, or an
     * empty <code>Enumeration</code> if the context has no initialization
     * parameters.
     *
     * @return 		an <code>Enumeration</code> of <code>String</code> 
     *                  objects containing the names of the context's
     *                  initialization parameters
     *
     * @see ServletConfig#getInitParameter
     */
    public Enumeration getInitParameterNames()
    {
	Code.notImplemented();
	return null;
    }
    
    
    /* ------------------------------------------------------------ */
    String getRealPathInfo(String pathInfo)
    {
	if (_handler.getFileBase()==null)
	    return null;
	
	return
	    _handler.getFileBase()+
	    pathInfo.replace('/',File.separatorChar);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Returns a <code>ServletContext</code> object that 
     * corresponds to a specified URL on the server.
     *
     * <p>This method allows servlets to gain
     * access to the context for various parts of the server, and as
     * needed obtain {@link RequestDispatcher} objects from the context.
     * The given path must be absolute (beginning with "/") and is 
     * interpreted based on the server's document root. 
     * 
     * <p>In a security conscious environment, the servlet container may
     * return <code>null</code> for a given URL.
     *       
     * @param uripath 	a <code>String</code> specifying the absolute URL of 
     *			a resource on the server
     *
     * @return		the <code>ServletContext</code> object that
     *			corresponds to the named URL
     *
     * @see 		RequestDispatcher
     *
     */
    public ServletContext getContext(String uri)
    {
	Code.notImplemented();
	return null;
    }
    
    /* ------------------------------------------------------------ */
    public int getMajorVersion()
    {
	return 2;
    }
    
    /* ------------------------------------------------------------ */
    public int getMinorVersion()
    {
	return 1;
    }

    /* ------------------------------------------------------------ */
    /**
     * Returns the MIME type of the specified file, or <code>null</code> if 
     * the MIME type is not known. The MIME type is determined
     * by the configuration of the servlet container, and may be specified
     * in a web application deployment descriptor. Common MIME
     * types are <code>"text/html"</code> and <code>"image/gif"</code>.
     *
     *
     * @param   file    a <code>String</code> specifying the name
     *			of a file
     *
     * @return 		a <code>String</code> specifying the file's MIME type
     *
     */
    public String getMimeType(String file)
    {
	Code.notImplemented();
	return null;
    }
    
    /* ------------------------------------------------------------ */
    public URL getResource(String uri)
	throws MalformedURLException
    {
	String resourceBase=_handler.getResourceBase();
	if (resourceBase==null)
	    return null;
	
	return new URL(resourceBase+uri);
    }
    
    /* ------------------------------------------------------------ */
    public InputStream getResourceAsStream(String uri)
    {
	try
	{
	    URL url = getResource(uri);
	    if (url!=null)
		return url.openStream();
	}
	catch(MalformedURLException e)
	{
	    Code.ignore(e);
	}
	catch(IOException e)
	{
	    Code.ignore(e);
	}
	return null;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * 
     * Returns a {@link RequestDispatcher} object that acts
     * as a wrapper for the resource located at the given path.
     * A <code>RequestDispatcher</code> object can be used to forward 
     * a request to the resource or to include the resource in a response.
     * The resource can be dynamic or static.
     *
     * <p>The pathname must begin with a "/" and is interpreted as relative
     * to the current context root.  Use <code>getContext</code> to obtain
     * a <code>RequestDispatcher</code> for resources in foreign contexts.
     * This method returns <code>null</code> if the <code>ServletContext</code>
     * cannot return a <code>RequestDispatcher</code>.
     *
     * @param path 	a <code>String</code> specifying the pathname
     *			to the resource
     *
     * @return 		a <code>RequestDispatcher</code> object
     *			that acts as a wrapper for the resource
     *			at the specified path
     *
     * @see 		RequestDispatcher
     * @see 		ServletContext#getContext
     *
     */
    public RequestDispatcher getRequestDispatcher(String uri)
    {
	Code.notImplemented();
	return null;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Returns a {@link RequestDispatcher} object that acts
     * as a wrapper for the named servlet.
     *
     * <p>Servlets (and JSP pages also) may be given names via server 
     * administration or via a web application deployment descriptor.
     * A servlet instance can determine its name using 
     * {@link ServletConfig#getServletName}.
     *
     * <p>This method returns <code>null</code> if the 
     * <code>ServletContext</code>
     * cannot return a <code>RequestDispatcher</code> for any reason.
     *
     * @param name 	a <code>String</code> specifying the name
     *			of a servlet to wrap
     *
     * @return 		a <code>RequestDispatcher</code> object
     *			that acts as a wrapper for the named servlet
     *
     * @see 		RequestDispatcher
     * @see 		ServletContext#getContext
     * @see 		ServletConfig#getServletName
     *
     */
    public RequestDispatcher getNamedDispatcher(String name)
    {
	Code.notImplemented();
	return null;
    }


    /* ------------------------------------------------------------ */
    public Servlet getServlet(String name)
    {
	Code.warning("No longer supported");
	return null;
    }
    
    /* ------------------------------------------------------------ */
    public Enumeration getServlets()
    {
	Code.warning("No longer supported");
	return null;
    }
    
    /* ------------------------------------------------------------ */
    public Enumeration getServletNames()
    {
	Code.warning("No longer supported");
	return null;
    }
    
    /* ------------------------------------------------------------ */
    public void log(String msg)
    {
	Log.event(msg);
    }
    
    /* ------------------------------------------------------------ */
    public void log(Exception e, String msg)
    {
	Code.warning(msg,e);
    }
    
    /* ------------------------------------------------------------ */
    public void log(String msg, Throwable th)
    {
	Code.warning(msg,th);
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Returns a <code>String</code> containing the real path 
     * for a given virtual path. For example, the virtual path "/index.html"
     * has a real path of whatever file on the server's filesystem would be
     * served by a request for "/index.html".
     *
     * <p>The real path returned will be in a form
     * appropriate to the computer and operating system on
     * which the servlet container is running, including the
     * proper path separators. This method returns <code>null</code>
     * if the servlet container cannot translate the virtual path
     * to a real path for any reason (such as when the content is
     * being made available from a <code>.war</code> archive).
     *
     *
     * @param path 	a <code>String</code> specifying a virtual path
     *
     *
     * @return 		a <code>String</code> specifying the real path,
     *                  or null if the translation cannot be performed
     *			
     *
     */
    public String getRealPath(String path)
    {
	Code.notImplemented();
	return null;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Returns the name and version of the servlet container on which
     * the servlet is running. 
     *
     * <p>The form of the returned string is 
     * <i>servername</i>/<i>versionnumber</i>.
     * For example, the JavaServer Web Development Kit may return the string
     * <code>JavaServer Web Dev Kit/1.0</code>.
     *
     * <p>The servlet container may return other optional information 
     * after the primary string in parentheses, for example,
     * <code>JavaServer Web Dev Kit/1.0 (JDK 1.1.6; Windows NT 4.0 x86)</code>.
     *
     *
     * @return 		a <code>String</code> containing at least the 
     *			servlet container name and version number
     *
     */
    public String getServerInfo()
    {
	Code.notImplemented();
	return null;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Returns the servlet container attribute with the given name, 
     * or <code>null</code> if there is no attribute by that name.
     * An attribute allows a servlet container to give the
     * servlet additional information not
     * already provided by this interface. See your
     * server documentation for information about its attributes.
     * A list of supported attributes can be retrieved using
     * <code>getAttributeNames</code>.
     *
     * <p>The attribute is returned as a <code>java.lang.Object</code>
     * or some subclass.
     * Attribute names should follow the same convention as package
     * names. The Java Servlet API specification reserves names
     * matching <code>java.*</code>, <code>javax.*</code>,
     * and <code>sun.*</code>.
     *
     *
     * @param name 	a <code>String</code> specifying the name 
     *			of the attribute
     *
     * @return 		an <code>Object</code> containing the value 
     *			of the attribute, or <code>null</code>
     *			if no attribute exists matching the given
     *			name
     *
     * @see 		ServletContext#getAttributeNames
     *
     */
    public Object getAttribute(String name)
    {
	Code.notImplemented();
	return null;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Returns an <code>Enumeration</code> containing the 
     * attribute names available
     * within this servlet context. Use the
     * {@link #getAttribute} method with an attribute name
     * to get the value of an attribute.
     *
     * @return 		an <code>Enumeration</code> of attribute 
     *			names
     *
     * @see		#getAttribute
     *
     */
    public Enumeration getAttributeNames()
    {
	Code.notImplemented();
	return null;
    }
    
    /* ------------------------------------------------------------ */
    /**
     *
     * Binds an object to a given attribute name in this servlet context. If
     * the name specified is already used for an attribute, this
     * method will remove the old attribute and bind the name
     * to the new attribute.
     *
     * <p>Attribute names should follow the same convention as package
     * names. The Java Servlet API specification reserves names
     * matching <code>java.*</code>, <code>javax.*</code>, and
     * <code>sun.*</code>.
     *
     *
     * @param name 	a <code>String</code> specifying the name 
     *			of the attribute
     *
     * @param object 	an <code>Object</code> representing the
     *			attribute to be bound
     *
     *
     *
     */
    public void setAttribute(String name, Object value)
    {
	Code.notImplemented();
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Removes the attribute with the given name from 
     * the servlet context. After removal, subsequent calls to
     * {@link #getAttribute} to retrieve the attribute's value
     * will return <code>null</code>.
     *
     *
     * @param name	a <code>String</code> specifying the name 
     * 			of the attribute to be removed
     *
     */
    public void removeAttribute(String name)
    {
	Code.notImplemented();
    }					       
}


