/*
  GNUJSP - a free JSP1.0 implementation

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either version 2
  of the License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
*/

package org.gjt.jsp.jsdk20;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * <b>HttpServletRequestWrapper</b> class is used internally
 * for redirecting/including requests to another JSP pages.
 *
 * The idea of wrapping is the following.
 * Suppose we need a request to look like:
 * <u>http://server/server-page.jsp/path-info?query-string</u>,
 * not something like http://server-name/servlet/gnujsp/server-page.jsp/page-path-info?query-string.
 * So need to replace getQueryString(), getPathInfo() and all those methods
 * with new ones. There is also some support for keeping attributes
 * in request (it could be set only via JSP engine !).
 *
 * @author Serge P. Nekoval
 */
public class HttpServletRequestWrapper implements HttpServletRequest {

    private HttpServletRequest req;
    private Hashtable attrs = new Hashtable();

    /**
     * these attributes are used when
     * simulating include on jsdk 2.0 
     */
    private String requestAttr = null; // the relative url
    private String servletAttr = null; // the servlet path
    private String pathAttr = null; // the pathinfo
    private String queryAttr = null; // the querystring
    // indicate that we wrapped ourself, so
    // we may use getAttribute recursive
    // FIXME: TODO
    private boolean wrappedWrapper = false;

    /**
     * Create response wrapper object
     *@param req ServletRequest object to wrap
     *@param servletName String containing JSP page name (relative)
     *@param extra String containing path info & query string
     */
    public HttpServletRequestWrapper(HttpServletRequest req) {
      this.req = req;
    }

    /**
     * Return request URI in the form like:
     * 
     * @return this request's URI where a page name is
     *              passed to JSP servlet via the path info
     * @see javax.servlet.http.HttpUtils#getRequestURL
     */
    public String getRequestURI() {
      return (requestAttr != null) ? requestAttr : req.getRequestURI();
    }

    /**
     * Note: this returns path unchanged, because it will be
     * something like /servlet/gnujsp
     *
     * @return the servlet being invoked, as contained in this
     * request's URI
     */
    public String getServletPath() {
      return (servletAttr != null) ? servletAttr : req.getServletPath();
    }

    /**
     * Gets any optional extra path information following the servlet
     * path of this request's URI, but immediately preceding its query
     * string. Same as the CGI variable PATH_INFO.
     *
     * @return the optional path information following the servlet
     * path, but before the query string, in this request's URI; null
     * if this request's URI contains no extra path information
     */
    public String getPathInfo() {
	return (pathAttr != null) ? pathAttr : req.getPathInfo();
    }

    /**
     * Gets any optional extra path information following the servlet
     * path of this request's URI, but immediately preceding its query
     * string, and translates it to a real path.  Similar to the CGI
     * variable PATH_TRANSLATED
     *
     * @return extra path information translated to a real path or null
     * if no extra path information is in the request's URI
     */
    public String getPathTranslated() {
	String s = req.getPathTranslated();
	if(pathAttr != null && s != null) {
	    // try to construct new path 
	    String oldPathInfo = req.getPathInfo();
	    if(!s.endsWith(oldPathInfo)) {
		// giving up
		return null;
	    }
	    // get prefix and replace path info
	    return s.substring(0, s.length()-oldPathInfo.length()) + pathAttr;
	} else {
	    return s;
	}
	// Any reason for returning "null"? We need either
	// a functioning ServletContect.getRealPath()
	// or we need this to find the jsp file for include. (alph)
	// return null;
    }

    /**
     * Gets any query string that is part of the HTTP request URI.
     * Same as the CGI variable QUERY_STRING.
     * Query string goes after '?' in path info
     * @return query string that is part of this request's URI, or null
     * if it contains no query string
     */
    public String getQueryString() {
      return req.getQueryString();
    }

    /**
     *
     * Stores an attribute in the context of this request.
     * Attributes are reset between requests.
     *
     * <p>Attribute names should follow the same conventions as
     * package names. Names beginning with <code>java.*</code>,
     * <code>javax.*</code>, and <code>com.sun.*</code>, are
     * reserved for use by Sun Microsystems.
     *
     * Note: setAttribute is included in here in order to support
     *       Java Servlet 2.1 specification. The only way it could be
     *       used is an internal call by JSP Engine (possibly
     *       with classcast like '(HttpServletRequestWrapper)request').
     *       This means that pages are normally not able to use this
     *       method (I mean, under JSDK 2.0)
     *
     * @param key     a <code>String</code> specifying 
     *          the name of the attribute
     *
     * @param o       an <code>Object</code> containing 
     *          the context of the request
     *
     * @exception IllegalStateException if the specified attribute already has a value
     *
     */

    public void setAttribute(String key, Object o) {
	if(key.startsWith("javax.servlet.forward.")) {
	    // no need to store these in hash
	    if(key.equals("javax.servlet.forward.request")) {
		requestAttr = (String) o;
	    } else if(key.equals("javax.servlet.forward.servlet")) {
		servletAttr = (String) o;
	    } else if(key.equals("javax.servlet.forward.path")) {
		pathAttr = (String) o;
	    } else if(key.equals("javax.servlet.forward.query")) {
		queryAttr = (String) o;
	    }
	    return;
	}
	attrs.put(key, o);
	if(key.startsWith("javax.servlet.include.")) {
	    if(key.equals("javax.servlet.include.request")) {
		requestAttr = (String) o;
	    } else if(key.equals("javax.servlet.include.servlet")) {
		servletAttr = (String) o;
	    } else if(key.equals("javax.servlet.include.path")) {
		pathAttr = (String) o;
	    } else if(key.equals("javax.servlet.include.query")) {
		queryAttr = (String) o;
	    }
	}
    }
    public Enumeration getAttributeNames() {
	return attrs.keys();
    }

    /**
     * Returns the value of the named attribute of the request, or
     * null if the attribute does not exist.  This method allows
     * access to request information not already provided by the other
     * methods in this interface.  Attribute names should follow the
     * same convention as package names.
     *
     * Note: request attribute are joined with JSP-supplied attributes.
     *       Priority is given to JSP-supplied attributes
     *
     * @param name the name of the attribute whose value is required
     */
    public Object getAttribute(String name) {
      Object currentAttr = attrs.get(name);
      return (currentAttr == null) ? req.getAttribute(name) : currentAttr;
    }

    /**
     ****************************************************************
     *    These methods are redirected to the previous request      *
     ****************************************************************
     */

    /**
     * Returns the size of the request entity data, or -1 if not known.
     * Same as the CGI variable CONTENT_LENGTH.
     */
    public int getContentLength() {
      return req.getContentLength();
    }

    /**
     * Returns the Internet Media Type of the request entity data, or
     * null if not known. Same as the CGI variable CONTENT_TYPE.
     */
    public String getContentType() {
      return req.getContentType();
    }

    /**
     * Returns the protocol and version of the request as a string of
     * the form <code>&lt;protocol&gt;/&lt;major version&gt;.&lt;minor
     * version&gt</code>.  Same as the CGI variable SERVER_PROTOCOL.
     */
    public String getProtocol() {
      return req.getProtocol();
    }

    /**
     * Returns the scheme of the URL used in this request, for example
     * "http", "https", or "ftp".  Different schemes have different
     * rules for constructing URLs, as noted in RFC 1738.  The URL used
     * to create a request may be reconstructed using this scheme, the
     * server name and port, and additional information such as URIs.
     */
    public String getScheme() {
      return req.getScheme();
    }

    /**
     * Returns the host name of the server that received the request.
     * Same as the CGI variable SERVER_NAME.
     */
    public String getServerName() {
      return req.getServerName();
    }

    /**
     * Returns the port number on which this request was received.
     * Same as the CGI variable SERVER_PORT.
     */
    public int getServerPort() {
      return req.getServerPort();
    }

    /**
     * Returns the IP address of the agent that sent the request.
     * Same as the CGI variable REMOTE_ADDR.
     */
    public String getRemoteAddr() {
      return req.getRemoteAddr();
    }

    /**
     * Returns the fully qualified host name of the agent that sent the
     * request. Same as the CGI variable REMOTE_HOST.
     */
    public String getRemoteHost() {
      return req.getRemoteHost();
    }

    /**
     * Applies alias rules to the specified virtual path and returns
     * the corresponding real path, or null if the translation can not
     * be performed for any reason.  For example, an HTTP servlet would
     * resolve the path using the virtual docroot, if virtual hosting
     * is enabled, and with the default docroot otherwise.  Calling
     * this method with the string "/" as an argument returns the
     * document root.
     *
     * @param path the virtual path to be translated to a real path
     */
    public String getRealPath(String path) {
	return req.getRealPath(path); // We know this is deprecated.
    }

    /**
     * Throws an exception because binary streams are not supported
     *
     * @see getReader
     * @exception IOException Indicates that binary streams are not supported
     */
    public ServletInputStream getInputStream() throws IOException {
      return req.getInputStream();
    }

    /**
     * Returns a string containing the lone value of the specified
     * parameter, or null if the parameter does not exist. For example,
     * in an HTTP servlet this method would return the value of the
     * specified query string parameter. Servlet writers should use
     * this method only when they are sure that there is only one value
     * for the parameter.  If the parameter has (or could have)
     * multiple values, servlet writers should use
     * getParameterValues. If a multiple valued parameter name is
     * passed as an argument, the return value is implementation
     * dependent.
     *
     * @see #getParameterValues
     *
     * @param name the name of the parameter whose value is required.
     */
    public String getParameter(String name) {
      return req.getParameter(name);
    }

    /**
     * Returns the values of the specified parameter for the request as
     * an array of strings, or null if the named parameter does not
     * exist. For example, in an HTTP servlet this method would return
     * the values of the specified query string or posted form as an
     * array of strings.
     *
     * @param name the name of the parameter whose value is required.
     * @see javax.servlet.ServletRequest#getParameter
     */
    public String[] getParameterValues(String name) {
      return req.getParameterValues(name);
    }

    /**
     * Returns the parameter names for this request as an enumeration
     * of strings, or an empty enumeration if there are no parameters
     * or the input stream is empty.  The input stream would be empty
     * if all the data had been read from the stream returned by the
     * method getInputStream.
     */
    public Enumeration getParameterNames() {
      return req.getParameterNames();
    }

    /**
     * Returns a buffered reader for reading text in the request body.
     * This translates character set encodings as appropriate. 
     *
     * @see getInputStream
     *
     * @exception UnsupportedEncodingException if the character set encoding
     *  is unsupported, so the text can't be correctly decoded.
     * @exception IllegalStateException if getInputStream has been
     *  called on this same request.
     * @exception IOException on other I/O related errors.
     */
    public BufferedReader getReader () throws IOException {
      return req.getReader();
    }

    /**
     * Returns the character set encoding for the input of this request.
     */
    public String getCharacterEncoding () {
      return req.getCharacterEncoding();
    }

    /**
     * Gets the array of cookies found in this request.
     *
     * @return the array of cookies found in this request
     */
    public Cookie[] getCookies() {
      return req.getCookies();
    }

    /**
     * Gets the HTTP method (for example, GET, POST, PUT) with which
     * this request was made. Same as the CGI variable REQUEST_METHOD.
     *
     * @return the HTTP method with which this request was made
     */
    public String getMethod() {
      return req.getMethod();
    }

    /**
     * Gets the name of the user making this request.  The user name is
     * set with HTTP authentication.  Whether the user name will
     * continue to be sent with each subsequent communication is
     * browser-dependent.  Same as the CGI variable REMOTE_USER.
     *
     * @return the name of the user making this request, or null if not
     * known.
     */
    public String getRemoteUser() {
      return req.getRemoteUser();
    }

    /**
     * Gets the authentication scheme of this request.  Same as the CGI
     * variable AUTH_TYPE.
     *
     * @return this request's authentication scheme, or null if none.
     */
    public String getAuthType() {
      return req.getAuthType();
    }

    /**
     * Gets the value of the requested header field of this request.
     * The case of the header field name is ignored.
     * 
     * @param name the String containing the name of the requested
     * header field
     * @return the value of the requested header field, or null if not
     * known.
     */
    public String getHeader(String name) {
      return req.getHeader(name);
    }

    /**
     * Gets the value of the specified integer header field of this
     * request.  The case of the header field name is ignored.  If the
     * header can't be converted to an integer, the method throws a
     * NumberFormatException.
     * 
     * @param name the String containing the name of the requested
     * header field
     * @return the value of the requested header field, or -1 if not
     * found.
     */
    public int getIntHeader(String name) {
      return req.getIntHeader(name);
    }

    /**
     * Gets the value of the requested date header field of this
     * request.  If the header can't be converted to a date, the method
     * throws an IllegalArgumentException.  The case of the header
     * field name is ignored.
     * 
     * @param name the String containing the name of the requested
     * header field
     * @return the value the requested date header field, or -1 if not
     * found.
     */
    public long getDateHeader(String name) {
      return req.getDateHeader(name);
    }

    /**
     * Gets the header names for this request.
     *
     * @return an enumeration of strings representing the header names
     * for this request. Some server implementations do not allow
     * headers to be accessed in this way, in which case this method
     * will return null.
     */
    public Enumeration getHeaderNames() {
      return req.getHeaderNames();
    }

    /**
     * Gets the current valid session associated with this request, if
     * create is false or, if necessary, creates a new session for the
     * request, if create is true.
     *
     * <p><b>Note</b>: to ensure the session is properly maintained,
     * the servlet developer must call this method (at least once)
     * before any output is written to the response.
     *
     * <p>Additionally, application-writers need to be aware that newly
     * created sessions (that is, sessions for which
     * <code>HttpSession.isNew</code> returns true) do not have any
     * application-specific state.
     *
     * @return the session associated with this request or null if
     * create was false and no valid session is associated
     * with this request.
     */
    public HttpSession getSession (boolean create) {
      return req.getSession(create);
    }
   
    /**
     * Gets the session id specified with this request.  This may
     * differ from the actual session id.  For example, if the request
     * specified an id for an invalid session, then this will get a new
     * session with a new id.
     *
     * @return the session id specified by this request, or null if the
     * request did not specify a session id
     * 
     * @see #isRequestedSessionIdValid */
    public String getRequestedSessionId () {
      return req.getRequestedSessionId();
    }

    /**
     * Checks whether this request is associated with a session that
     * is valid in the current session context.  If it is not valid,
     * the requested session will never be returned from the
     * <code>getSession</code> method.
     * 
     * @return true if this request is assocated with a session that is
     * valid in the current session context.
     *
     * @see #getRequestedSessionId
     * @see javax.servlet.http.HttpSessionContext
     * @see #getSession
     */
    public boolean isRequestedSessionIdValid () {
      return req.isRequestedSessionIdValid();
    }

    /**
     * Checks whether the session id specified by this request came in
     * as a cookie.  (The requested session may not be one returned by
     * the <code>getSession</code> method.)
     * 
     * @return true if the session id specified by this request came in
     * as a cookie; false otherwise
     *
     * @see #getSession
     */
    public boolean isRequestedSessionIdFromCookie () {
      return req.isRequestedSessionIdFromCookie();
    }

    /**
     * Checks whether the session id specified by this request came in
     * as part of the URL.  (The requested session may not be the one
     * returned by the <code>getSession</code> method.)
     * 
     * @return true if the session id specified by the request for this
     * session came in as part of the URL; false otherwise
     *
     * @see #getSession
     */
    public boolean isRequestedSessionIdFromUrl () {
	return req.isRequestedSessionIdFromUrl(); // We know it's deprecated.
    }

    /** New in 2.1, just here for compiler happiness. */
    public boolean isRequestedSessionIdFromURL () {
	return req.isRequestedSessionIdFromUrl(); // We know it's deprecated.
    }

    /** New in 2.1, just here for compiler happiness. */
    public HttpSession getSession() {
	return req.getSession(true); 
    }

}
