/*
 * $Id$
 * 
 * Copyright (c) 1995-1999 Sun Microsystems, Inc. All Rights Reserved.
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
 * A servlet engine generated object that gives servlets information
 * about their environment.
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
     * Returns a <tt>ServletContext</tt> object for a particular
     * URL path. This allows servlets to potentially gain access
     * to the resources and to obtain <tt>RequestDispatcher</tt>
     * objects from the target context.
     *
     * <p>In security concious environments, the servlet engine
     * may always return null for any given URL path.
     *       
     * @param uripath
     */

    public ServletContext getContext(String uripath);

    /**
     * Returns the major version of the servlet API that this
     * servlet engine supports. All 2.1 compliant implementations
     * must return the integer 2 from this method.
     *
     * @return 2
     */
    
    public int getMajorVersion();

    /**
     * Returns the mime type of the specified file, or null if not
     * known. The MIME type is determined according to the
     * configuration of the servlet engine.
     *
     * @param file name of the file whose mime type is required
     */

    public String getMimeType(String file);

    /**
     * Returns the minor version of the servlet API that this
     * servlet engine supports. All 2.1 compliant implementations
     * must return the integer 1 from this method.
     *
     * @return 1
     */

    public int getMinorVersion();

    /**
     * Returns a URL object of a resource that is mapped to a
     * corresponding URL path. The URL path must be of the form
     * <tt>/dir/dir/file.ext</tt>. This method allows a servlet
     * to access content to be served from the servlet engines
     * document space in a system independent manner. Resources
     * could be located on the local file system, a remote
     * file system, a database, or a remote network site.
     *
     * <p>This method may return null if there is no resource
     * mapped to the given URL path.
     * 
     * <p>The servlet engine must implement whatever URL handlers
     * and <tt>URLConnection</tt> objects are necessary to access
     * the given content.
     *
     * <p>This method does not fill the same purpose as the
     * <tt>getResource</tt> method of <tt>java.lang.Class</tt>.
     * The method in <tt>java.lang.Class</tt> looks up resources
     * based on class loader. This method allows servlet engines
     * to make resources avaialble to a servlet from any source
     * without regards to class loaders, location, etc.
     *
     * @param path Path of the content resource
     * @exception MalformedURLException if the resource path is
     * not properly formed.
     */
    
    public URL getResource(String path) throws MalformedURLException;


    /**
     * Returns an <tt>InputStream</tt> object allowing access to
     * a resource that is mapped to a corresponding URL path. The
     * URL path must be of the form <tt>/dir/dir/file.ext</tt>.
     *
     * <p>Note that meta-information such as content length and
     * content type that are available when using the
     * <tt>getResource</tt> method of this class are lost when
     * using this method.
     *
     * <p>This method may return null if there is no resource
     * mapped to the given URL path.
     * <p>The servlet engine must implement whatever URL handlers
     * and <tt>URLConnection</tt> objects are necessary to access
     * the given content.
     *
     * <p>This method does not fill the same purpose as the
     * <tt>getResourceAsStream</tt> method of <tt>java.lang.Class</tt>.
     * The method in <tt>java.lang.Class</tt> looks up resources
     * based on class loader. This method allows servlet engines
     * to make resources avaialble to a servlet from any source
     * without regards to class loaders, location, etc.     
     *
     * @param name
     */

    public InputStream getResourceAsStream(String path);

    /**
     * Returns a <tt>RequestDispatcher</tt> object for the specified
     * URL path if the context knows of an active source (such as
     * a servlet, JSP page, CGI script, etc) of content for the
     * particular path. This format of the URL path must be of the
     * form <tt>/dir/dir/file.ext</tt>. The servlet engine is responsible
     * for implementing whatever functionality is required to
     * wrap the target source with an implementation of the
     * <tt>RequestDispatcher</tt> interface.
     *
     * <p>This method will return null if the context cannot provide
     * a dispatcher for the path provided.
     *
     * @param urlpath Path to use to look up the target server resource
     * @see RequestDispatcher
     */

    public RequestDispatcher getRequestDispatcher(String urlpath);
    
    /**
     * Originally defined to return a servlet from the context
     * with the specified name. This method has been deprecated and
     * only remains to preserve binary compatibility.
     * This method will always return null.
     *
     * @deprecated This method has been deprecated for
     * servlet lifecycle reasons. This method will be permanently
     * removed in a future version of the Servlet API.
     */

    public Servlet getServlet(String name) throws ServletException;

    /**
     * Originally defined to return an <tt>Enumeration</tt> of
     * <tt>Servlet</tt> objects containing all the servlets
     * known to this context.
     * This method has been deprecated and only remains to preserve
     * binary compatibility. This method must always return an empty
     * enumeration.
     *
     * @deprecated This method has been deprecated for
     * servlet lifecycle reasons. This method will be permanently
     * removed in a future version of the Servlet API.
     */

    public Enumeration getServlets();

    /**
     * Originally defined to return an <tt>Enumeration</tt> of
     * <tt>String</tt> objects containing all the servlet names
     * known to this context.       
     * This method has been deprecated and only remains to preserve
     * binary compatibility. This methd must always return an
     * empty enumeration.
     *
     * @deprecated This method has been deprecated for
     * servlet lifecycle reasons. This method will be permanently
     * removed in a future version of the Servlet API.
     */

    public Enumeration getServletNames();
    
    /**
     * Logs the specified message to the context's log. The
     * name and type of the servlet log is servlet engine specific,
     * but is normally an event log.
     *
     * @param msg the message to be written
     */

    public void log(String msg);

    /**
     * Logs the specified message and a stack trace of the given
     * exception to the context's log. The
     * name and type of the servlet log is servlet engine specific,
     * but is normally an event log.
     *
     * @param exception the exception to be written
     * @param msg the message to be written
     *
     * @deprecated Use log(String message, Throwable t) instead
     */

    public void log(Exception exception, String msg);

    /**
     * Logs the specified message and a stack trace of the given
     * <tt>Throwable</tt> object to the context's log. The
     * name and type of the servlet log is servlet engine specific,
     * but is normally an event log.
     *
     * @param msg the message to be written
     * @param throwable the exception to be written
     */
    
    public void log(String message, Throwable throwable);
    
    /**
     * Applies alias rules to the specified virtual path in URL path
     * format, that is, <tt>/dir/dir/file.ext</tt>. Returns a
     * String representing the corresponding real path in the
     * format that is appropriate for the operating system the
     * servlet engine is running under (including the proper path
     * separators).
     *
     * <p>This method returns null if the translation could not
     * be performed for any reason.
     *
     * @param path the virtual path to be translated into a real path
     */

    public String getRealPath(String path);

    /**
     * Returns the name and version of the network service under which
     * the servlet is running. The form of this string must begin with
     * <tt>&lt;servername&gt;/&lt;versionnumber&gt;</tt>. For example
     * the Java Web Server could return a string of the form
     * <tt>Java Web Server/1.1.3</tt>. Other optional information
     * can be returned in parenthesis after the primary string. For
     * example, <tt>Java Web Server/1.1.3 (JDK 1.1.6; Windows NT 4.0 x86)
     * </tt>.
     */

    public String getServerInfo();

    /**
     * Returns an object that is known to the context by a given name,
     * or null if there is no such object associated with the name.
     * This method allwos access to additional information about the
     * servlet engine not already provided by other methods in this
     * interface.
     *
     * Attribute names should follow the same convention as package names.
     * Names matching java.*, javax.*, and sun.* are reserved for
     * definition by this specification or by the reference implementation.
     *
     * @param name the name of the attribute whose value is required
     * @return the value of the attribute, or null if the attribute
     * does not exist.
     */

    public Object getAttribute(String name);

    /**
     * Returns an enumeration of the attribute names present in this
     * context.
     */

    public Enumeration getAttributeNames();
    
    /**
     * Binds an object to a given name in this context. If an object
     * is allready bound into the context with the given name,
     * it will be replaced.
     *
     * Attribute names should follow the same convention as package names.
     * Names matching java.*, javax.*, and sun.* are reserved for
     * definition by this specification or by the reference implementation.
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
