/*
 * $Id$
 * 
 * Copyright (c) 1995-1998 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the confidential and proprietary information of Sun
 * Microsystems, Inc. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Sun.
 * 
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE
 * SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 * 
 * CopyrightVersion 1.0
 */

package javax.servlet;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Enumeration;

/**
 * The ServletContext interface gives servlets access to information about
 * their environment, and allows them to log significant events.  Servlet
 * writers decide what data to log.  The interface is implemented by
 * services, and used by servlets.
 *
 * <p>In a server that supports the concept of multiple hosts (and even
 * virtual hosts), the context must be at least as unique as the host.
 * Servlet engines may also provide context objects that are unique to
 * a group of servlets and which is tied to a specific portion of the
 * URL path namespace of the host. This grouping may be administratively
 * assigned or defined by deployment information.
 *
 * <p>Servlets get the ServletContext object via the getServletContext
 * method of ServletConfig.  The ServletConfig object is provided to
 * the servlet at initialization, and is accessible via the servlet's
 * getServletConfig method.
 *
 * @see Servlet#getServletConfig
 * @see ServletConfig#getServletContext
 */

public interface ServletContext {

    /**
     * Returns the ServletContext object for a particular uripath,
     * or null if there is not a ServletContext for the particular
     * path available or visible to this servlet.  This allows one
     * content to get RequestDispatchers and resources from another
     * context if so permitted. In more restricted environments, this
     * call may return null.
     *
     * @param uripath
     */

    public ServletContext getContext(String uripath);

    /**
     * Returns the major version of the servlet API that this
     * context supports.
     *
     * @return 2
     */
    
    public int getMajorVersion();

    /**
     * Returns the mime type of the specified file, or null if not
     * known.
     *
     * @param file name of the file whose mime type is required
     */

    public String getMimeType(String file);

    /**
     * Returns the minor version of the servlet API that this context
     * supports.
     *
     * @return 1
     */

    public int getMinorVersion();

    /**
     * Returns a URL object allowing access to any content resource
     * requested. This method allows a servlet to access items in the
     * host servlet engine's document space in a system independent
     * way whether they reside on the local file system, a remote
     * file system, a database, or a remote network site without
     * knowing the specific details of how to obtain the resources.
     * The servlet engine must implement all of the specifics about
     * how to access the resource.
     *
     * <p>Only content resources known to this context object can
     * be accessed via this call.
     *
     * <p>The path parameter of this call must be a URL path.  To
     * access the file index.html in the server's document root, the
     * call to this method would have the parameter "/index.html".
     *
     * @param path Path of the content resource
     * @exception MalformedURLException if the resource path is
     * not properly formed.
     */
    
    public URL getResource(String path) throws MalformedURLException;


    /**
     * Returns an input stream to the given content resource path, or
     * null if not found.
     *
     * @param name */

    public InputStream getResourceAsStream(String path);

    /**
     * Returns a request dispatcher to a server component accessible
     * via a URL path. This method is typically used to obtain a
     * reference to another servlet. After obtaining a RequestDispatcher,
     * the servlet programmer forward a request to the target
     * component or include content from it.
     *
     * <p>This method will return null if the context cannot provide
     * a dispatcher for the path provided.
     *
     * @param urlpath Path to use to look up the target server resource
     * @see RequestDispatcher
     */

    public RequestDispatcher getRequestDispatcher(String urlpath);
    
    /**
     * This method has been deprecated and only remains to preserve
     * binary compatibility. This method will always return null.
     *
     * @deprecated This method has been deprecated for security and
     * servlet lifecycle reasons. This method will be permanently
     * removed in a future version of the Servlet API.
     */

    public Servlet getServlet(String name) throws ServletException;

    /**
     * This method has been deprecated and only remains to preserve
     * binary compatibility. This method must always return an empty
     * enumeration.
     *
     * @deprecated This method has been deprecated for security and
     * servlet lifecycle reasons. This method will be permanently
     * removed in a future version of the Servlet API.
     */

    public Enumeration getServlets();

    /**
     * This method has been deprecated and only remains to preserve
     * binary compatibility. This methd must always return an
     * empty enumeration.
     *
     * @deprecated This method has been deprecated for security and
     * servlet lifecycle reasons. This method will be permanently
     * removed in a future version of the Servlet API.
     */

    public Enumeration getServletNames();
    
    /**
     * Writes the given message string to the servlet log file.
     * The name and type of the servlet log file is server specific; it
     * is normally an event log.
     *
     * @param msg the message to be written
     */

    public void log(String msg);

    /**
     * Write the stacktrace and the given message string to the 
     * servlet log file. The name of the servlet log file is 
     * server specific; it is normally an event log.
     *
     * @param exception the exception to be written
     * @param msg the message to be written
     *
     * @deprecated Use log(String message, Throwable t) instead
     */

    public void log(Exception exception, String msg);

    /**
     * Write the given message and stacktrace to the servlet log. The
     * name of the log is network service specific; it is typically
     * the event log.
     *
     * @param msg the message to be written
     * @param throwable the exception to be written
     */
    
    public void log(String message, Throwable throwable);
    
    /**
     * Applies alias rules to the specified virtual path and returns the
     * corresponding real path.  For example, in an HTTP servlet,
     * this method would resolve the path against the HTTP service's
     * docroot.  Returns null if virtual paths are not supported, or if the
     * translation could not be performed for any reason.
     *
     * @param path the virtual path to be translated into a real path
     */

    public String getRealPath(String path);

    /**
     * Returns the name and version of the network service under which
     * the servlet is running. For example, if the network service was
     * an HTTP service, then this would be the same as the CGI
     * variable SERVER_SOFTWARE.
     */

    public String getServerInfo();

    /**
     * Returns the value of the named attribute of the network service,
     * or null if the attribute does not exist.  This method allows
     * access to additional information about the service, not already
     * provided by the other methods in this interface. Attribute names
     * should follow the same convention as package names.  The package
     * names java.* and javax.* are reserved for use by Javasoft, and
     * com.sun.* is reserved for use by Sun Microsystems.
     *
     * @param name the name of the attribute whose value is required
     * @return the value of the attribute, or null if the attribute
     * does not exist.
     */

    public Object getAttribute(String name);

    /**
     * Returns an enumeration of the attribute names present in the
     * network service.
     */

    public Enumeration getAttributeNames();
    
    /**
     * This method stores an attribute in the servlet context.
     *
     * @param name the name of the attribute to store
     * @param value the value of the attribute
     */
    
    public void setAttribute(String name, Object object);


    /**
     * Removes the attribute from the context that is bound to a particular
     * name.
     *
     * @param name the name of the attribute to remove from the context
     */

    public void removeAttribute(String name);
    
}
