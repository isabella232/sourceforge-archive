/**
 *
 * Copyright 2003-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

//
// This source code implements specifications defined by the Java
// Community Process. In order to remain compliant with the specification
// DO NOT add / change / or delete method signatures!
//

package javax.servlet.jsp;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import javax.servlet.http.HttpSession;

import javax.servlet.jsp.tagext.BodyContent;

/**
 * <p>
 * PageContext extends JspContext to provide useful _context information for
 * when JSP technology is used in a Servlet environment.
 * <p>
 * A PageContext instance provides access to all the namespaces associated
 * with a JSP page, provides access to several page attributes, as well as
 * a layer above the implementation details.  Implicit objects are added
 * to the pageContext automatically.
 *
 * <p> The <code> PageContext </code> class is an abstract class, designed to be
 * extended to provide implementation dependent implementations thereof, by
 * conformant JSP engine runtime environments. A PageContext instance is 
 * obtained by a JSP implementation class by calling the
 * JspFactory.getPageContext() method, and is released by calling
 * JspFactory.releasePageContext().
 *
 * <p> An example of how PageContext, JspFactory, and other classes can be
 * used  within a JSP Page Implementation object is given elsewhere.
 *
 * <p>
 * The PageContext provides a number of facilities to the page/component 
 * author and page implementor, including:
 * <ul>
 * <li>a single API to manage the various scoped namespaces
 * <li>a number of convenience API's to access various public objects
 * <li>a mechanism to obtain the JspWriter for output
 * <li>a mechanism to manage session usage by the page
 * <li>a mechanism to expose page directive attributes to the scripting 
 *     environment
 * <li>mechanisms to forward or include the current request to other active 
 *     components in the application
 * <li>a mechanism to handle errorpage exception processing
 * </ul>
 *
 * <p><B>Methods Intended for Container Generated Code</B>
 * <p>Some methods are intended to be used by the code generated by the
 * container, not by code written by JSP page authors, or JSP tag library 
 * authors.
 * <p>The methods supporting <B>lifecycle</B> are <code>initialize()</code>
 * and <code>release()</code>
 *
 * <p>
 * The following methods enable the <B>management of nested</B> JspWriter 
 * streams to implement Tag Extensions: <code>pushBody()</code>
 *
 * <p><B>Methods Intended for JSP authors</B>
 * <p>
 * The following methods provide <B>convenient access</B> to implicit objects:
 * <code>getException()</code>,  <code>getPage()</code>
 * <code>getRequest()</code>,  <code>getResponse()</code>,
 * <code>getSession()</code>,  <code>getServletConfig()</code>
 * and <code>getServletContext()</code>.
 *
 * <p>
 * The following methods provide support for <B>forwarding, inclusion
 * and error handling</B>:
 * <code>forward()</code>,  <code>include()</code>,
 * and  <code>handlePageException()</code>.
 */

abstract public class PageContext 
    extends JspContext
{
    
    /**
     * Sole constructor. (For invocation by subclass constructors, 
     * typically implicit.)
     */
    public PageContext() {
    }
    
    /**
     * Page scope: (this is the default) the named reference remains available
     * in this PageContext until the return from the current Servlet.service()
     * invocation.
     */

    public static final int PAGE_SCOPE		= 1;

    /**
     * Request scope: the named reference remains available from the 
     * ServletRequest associated with the Servlet until the current request 
     * is completed.
     */

    public static final int REQUEST_SCOPE	= 2;

    /**
     * Session scope (only valid if this page participates in a session):
     * the named reference remains available from the HttpSession (if any)
     * associated with the Servlet until the HttpSession is invalidated.
     */

    public static final int SESSION_SCOPE	= 3;

    /**
     * Application scope: named reference remains available in the 
     * ServletContext until it is reclaimed.
     */

    public static final int APPLICATION_SCOPE	= 4;

    /**
     * Name used to store the Servlet in this PageContext's nametables.
     */

    public static final String PAGE = "javax.servlet.jsp.jspPage";

    /**
     * Name used to store this PageContext in it's own name table.
     */

    public static final String PAGECONTEXT = "javax.servlet.jsp.jspPageContext";

    /**
     * Name used to store ServletRequest in PageContext name table.
     */

    public static final String REQUEST = "javax.servlet.jsp.jspRequest";

    /**
     * Name used to store ServletResponse in PageContext name table.
     */

    public static final String RESPONSE = "javax.servlet.jsp.jspResponse";

    /**
     * Name used to store ServletConfig in PageContext name table.
     */

    public static final String CONFIG = "javax.servlet.jsp.jspConfig";

    /**
     * Name used to store HttpSession in PageContext name table.
     */

    public static final String SESSION = "javax.servlet.jsp.jspSession";
    /**
     * Name used to store current JspWriter in PageContext name table.
     */

    public static final String OUT = "javax.servlet.jsp.jspOut";

    /**
     * Name used to store ServletContext in PageContext name table.
     */

    public static final String APPLICATION = "javax.servlet.jsp.jspApplication";

    /**
     * Name used to store uncaught exception in ServletRequest attribute 
     * list and PageContext name table.
     */

    public static final String EXCEPTION = "javax.servlet.jsp.jspException";

    /**
     * <p>
     * The initialize method is called to initialize an uninitialized PageContext
     * so that it may be used by a JSP Implementation class to service an
     * incoming request and response within it's _jspService() method.
     *
     * <p>
     * This method is typically called from JspFactory.getPageContext() in
     * order to initialize state.
     *
     * <p>
     * This method is required to create an initial JspWriter, and associate
     * the "out" name in page scope with this newly created object.
     *
     * <p>
     * This method should not be used by page  or tag library authors.
     *
     * @param servlet The Servlet that is associated with this PageContext
     * @param request The currently pending request for this Servlet
     * @param response The currently pending response for this Servlet
     * @param errorPageURL The value of the errorpage attribute from the page 
     *     directive or null
     * @param needsSession The value of the session attribute from the 
     *     page directive
     * @param bufferSize The value of the buffer attribute from the page 
     *     directive
     * @param autoFlush The value of the autoflush attribute from the page 
     *     directive
     *
     * @throws IOException during creation of JspWriter
     * @throws IllegalStateException if out not correctly initialized
     * @throws IllegalArgumentException If one of the given parameters
     *     is invalid
     */
 
    abstract public void initialize(Servlet servlet, ServletRequest request, 
        ServletResponse response, String errorPageURL, boolean needsSession, 
        int bufferSize, boolean autoFlush)  
        throws IOException, IllegalStateException, IllegalArgumentException;

    /**
     * <p>
     * This method shall "reset" the internal state of a PageContext, releasing
     * all internal references, and preparing the PageContext for potential
     * reuse by a later invocation of initialize(). This method is typically
     * called from JspFactory.releasePageContext().
     *
     * <p>
     * Subclasses shall envelope this method.
     *
     * <p>
     * This method should not be used by page  or tag library authors.
     *
     */

    abstract public void release();

    /**
     * The current value of the session object (an HttpSession).
     *
     * @return the HttpSession for this PageContext or null
     */

    abstract public HttpSession getSession();

    /**
     * The current value of the page object (In a Servlet environment, 
     * this is an instance of javax.servlet.Servlet).
     *
     * @return the Page implementation class instance associated 
     *     with this PageContext
     */

    abstract public Object getPage();


    /**
     * The current value of the request object (a ServletRequest).
     *
     * @return The ServletRequest for this PageContext
     */

    abstract public ServletRequest getRequest();

    /**
     * The current value of the response object (a ServletResponse).
     *
     * @return the ServletResponse for this PageContext
     */

    abstract public ServletResponse getResponse();

    /**
     * The current value of the exception object (an Exception).
     *
     * @return any exception passed to this as an errorpage
     */

    abstract public Exception getException();

    /**
     * The ServletConfig instance.
     *
     * @return the ServletConfig for this PageContext
     */

    abstract public ServletConfig getServletConfig();

    /**
     * The ServletContext instance.
     * 
     * @return the ServletContext for this PageContext
     */

    abstract public ServletContext getServletContext();

    /**
     * <p>
     * This method is used to re-direct, or "forward" the current 
     * ServletRequest and ServletResponse to another active component in 
     * the application.
     * </p>
     * <p>
     * If the <I> relativeUrlPath </I> begins with a "/" then the URL specified
     * is calculated relative to the DOCROOT of the <code> ServletContext </code>
     * for this JSP. If the path does not begin with a "/" then the URL 
     * specified is calculated relative to the URL of the request that was
     * mapped to the calling JSP.
     * </p>
     * <p>
     * It is only valid to call this method from a <code> Thread </code>
     * executing within a <code> _jspService(...) </code> method of a JSP.
     * </p>
     * <p>
     * Once this method has been called successfully, it is illegal for the
     * calling <code> Thread </code> to attempt to modify the <code>
     * ServletResponse </code> object.  Any such attempt to do so, shall result
     * in undefined behavior. Typically, callers immediately return from 
     * <code> _jspService(...) </code> after calling this method.
     * </p>
     *
     * @param relativeUrlPath specifies the relative URL path to the target 
     *     resource as described above
     *
     * @throws IllegalStateException if <code> ServletResponse </code> is not 
     *     in a state where a forward can be performed
     * @throws ServletException if the page that was forwarded to throws
     *     a ServletException
     * @throws IOException if an I/O error occurred while forwarding
     */

    abstract public void forward(String relativeUrlPath) 
        throws ServletException, IOException;

    /**
     * <p>
     * Causes the resource specified to be processed as part of the current
     * ServletRequest and ServletResponse being processed by the calling Thread.
     * The output of the target resources processing of the request is written
     * directly to the ServletResponse output stream.
     * </p>
     * <p>
     * The current JspWriter "out" for this JSP is flushed as a side-effect
     * of this call, prior to processing the include.
     * </p>
     * <p>
     * If the <I> relativeUrlPath </I> begins with a "/" then the URL specified
     * is calculated relative to the DOCROOT of the <code>ServletContext</code>
     * for this JSP. If the path does not begin with a "/" then the URL 
     * specified is calculated relative to the URL of the request that was
     * mapped to the calling JSP.
     * </p>
     * <p>
     * It is only valid to call this method from a <code> Thread </code>
     * executing within a <code> _jspService(...) </code> method of a JSP.
     * </p>
     *
     * @param relativeUrlPath specifies the relative URL path to the target 
     *     resource to be included
     *
     * @throws ServletException if the page that was forwarded to throws
     *     a ServletException
     * @throws IOException if an I/O error occurred while forwarding
     */
    abstract public void include(String relativeUrlPath) 
        throws ServletException, IOException;

    /**
     * <p>
     * Causes the resource specified to be processed as part of the current
     * ServletRequest and ServletResponse being processed by the calling Thread.
     * The output of the target resources processing of the request is written
     * directly to the current JspWriter returned by a call to getOut().
     * </p>
     * <p>
     * If flush is true, The current JspWriter "out" for this JSP 
     * is flushed as a side-effect of this call, prior to processing 
     * the include.  Otherwise, the JspWriter "out" is not flushed.
     * </p>
     * <p>
     * If the <i>relativeUrlPath</i> begins with a "/" then the URL specified
     * is calculated relative to the DOCROOT of the <code>ServletContext</code>
     * for this JSP. If the path does not begin with a "/" then the URL 
     * specified is calculated relative to the URL of the request that was
     * mapped to the calling JSP.
     * </p>
     * <p>
     * It is only valid to call this method from a <code> Thread </code>
     * executing within a <code> _jspService(...) </code> method of a JSP.
     * </p>
     *
     * @param relativeUrlPath specifies the relative URL path to the 
     *     target resource to be included
     * @param flush True if the JspWriter is to be flushed before the include,
     *     or false if not.
     *
     * @throws ServletException if the page that was forwarded to throws
     *     a ServletException
     * @throws IOException if an I/O error occurred while forwarding
     * @since 2.0
     */
    abstract public void include(String relativeUrlPath, boolean flush) 
	throws ServletException, IOException;

    /**
     * <p>
     * This method is intended to process an unhandled 'page' level
     * exception by forwarding the exception to the specified
     * error page for this JSP.  If forwarding is not possible (for
     * example because the response has already been committed), an
     * implementation dependent mechanism should be used to invoke
     * the error page (e.g. "including" the error page instead).
     *
     * <p>
     * If no error page is defined in the page, the exception should
     * be rethrown so that the standard servlet error handling
     * takes over.
     *
     * <p>
     * A JSP implementation class shall typically clean up any local state
     * prior to invoking this and will return immediately thereafter. It is
     * illegal to generate any output to the client, or to modify any 
     * ServletResponse state after invoking this call.
     *
     * <p>
     * This method is kept for backwards compatiblity reasons.  Newly
     * generated code should use PageContext.handlePageException(Throwable).
     *
     * @param e the exception to be handled
     *
     * @throws ServletException if an error occurs while invoking the error page
     * @throws IOException if an I/O error occurred while invoking the error
     *     page
     * @throws NullPointerException if the exception is null
     *
     * @see #handlePageException(Throwable)
     */

    abstract public void handlePageException(Exception e) 
        throws ServletException, IOException;

    /**
     * <p>
     * This method is intended to process an unhandled 'page' level
     * exception by forwarding the exception to the specified
     * error page for this JSP.  If forwarding is not possible (for
     * example because the response has already been committed), an
     * implementation dependent mechanism should be used to invoke
     * the error page (e.g. "including" the error page instead).
     *
     * <p>
     * If no error page is defined in the page, the exception should
     * be rethrown so that the standard servlet error handling
     * takes over.
     *
     * <p>
     * This method is intended to process an unhandled "page" level exception
     * by redirecting the exception to either the specified error page for this
     * JSP, or if none was specified, to perform some implementation dependent
     * action.
     *
     * <p>
     * A JSP implementation class shall typically clean up any local state
     * prior to invoking this and will return immediately thereafter. It is
     * illegal to generate any output to the client, or to modify any 
     * ServletResponse state after invoking this call.
     *
     * @param t the throwable to be handled
     *
     * @throws ServletException if an error occurs while invoking the error page
     * @throws IOException if an I/O error occurred while invoking the error
     *     page
     * @throws NullPointerException if the exception is null
     *
     * @see #handlePageException(Exception)
     */

    abstract public void handlePageException(Throwable t) 
        throws ServletException, IOException;

    /**
     * Return a new BodyContent object, save the current "out" JspWriter,
     * and update the value of the "out" attribute in the page scope
     * attribute namespace of the PageContext.
     *
     * @return the new BodyContent
     */

    public BodyContent pushBody() {
        return null; // XXX to implement
    }
         

    /**
     * Provides convenient access to error information.
     *
     * @return an ErrorData instance containing information about the 
     * error, as obtained from the request attributes, as per the 
     * Servlet specification.  If this is not an error page (that is,
     * if the isErrorPage attribute of the page directive is not set
     * to "true"), the information is meaningless.
     *
     * @since 2.0
     */
    public ErrorData getErrorData() {
	return new ErrorData( 
	    (Throwable)getRequest().getAttribute( "javax.servlet.error.exception" ),
	    ((Integer)getRequest().getAttribute( 
		"javax.servlet.error.status_code" )).intValue(),
	    (String)getRequest().getAttribute( "javax.servlet.error.request_uri" ),
	    (String)getRequest().getAttribute( "javax.servlet.error.servlet_name" ) );
    }
    
}
