/*
  GNUJSP - a free JSP1.0 implementation
  Copyright (C) 1999, Yaroslav Faybishenko <yaroslav@cs.berkeley.edu>

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

package org.gjt.jsp;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.NoSuchMethodError;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;

import org.gjt.jsp.jsdk20.HttpServletRequestWrapper;
import org.gjt.jsp.jsdk20.ServletContextWrapper;

class PageContextImpl extends PageContext 
    implements JspMsg
{
    // Should make that settable via config parameter.
    /**
     * Should unhandled exception be considered private/public?
     * private: print to error log only (for application environment)
     * public:  print copy to html page (good for development)
     */
    private boolean exceptionsArePublic = true;
    private Servlet servlet;
    private ServletRequest request;
    private ServletResponse response;
    private HttpSession session;
    private String errorPageURL;
    private JspWriterImpl out;
    private Hashtable pageAttributes;
    static boolean JSDK20;

    static class StringArrayEnumeration implements Enumeration {
	private String[] array;
	private int index;
	StringArrayEnumeration(String[] a) {
	    array = a;
	}
	public boolean hasMoreElements() {
	    return (index < array.length);
	}
	public Object nextElement() {
	    return (array[index++]);
	}
    }

    static {
	JSDK20 = false;
	try {
	    // exception raised when run in jsdk 2.0
	    // environment
	    (ServletContext.class).getMethod("getMajorVersion", null);
	} catch (Exception e) {
	    JSDK20 = true;
	}
	System.err.println("GNUJSP: Detected JSDK version 2." + 
			   (JSDK20 ? "0" : "1"));
    }

    protected PageContextImpl () {
    }

    public void initialize(Servlet servlet, ServletRequest request,
			   ServletResponse response, String errorPageURL,
			   boolean needsSession, int bufferSize,
			   boolean autoFlush) {
	this.servlet = servlet;
	this.request = request;
	this.response = response;
	this.errorPageURL = errorPageURL;
	this.out = new JspWriterImpl(response,bufferSize,autoFlush);
	pageAttributes = new Hashtable();
	session = ((HttpServletRequest) request).getSession(needsSession);

	setAttribute(PAGE, servlet, PAGE_SCOPE);
	setAttribute(PAGECONTEXT, this, PAGE_SCOPE);
	setAttribute(REQUEST, getRequest(), PAGE_SCOPE);
	setAttribute(RESPONSE, getResponse(), PAGE_SCOPE);
	setAttribute(CONFIG, getServletConfig(), PAGE_SCOPE);
	if (getSession() != null) setAttribute(SESSION, getSession(), PAGE_SCOPE);
	setAttribute(OUT, getOut(), PAGE_SCOPE);
	setAttribute(APPLICATION, getServletContext(), PAGE_SCOPE);
	if (getException() != null) setAttribute(EXCEPTION, getException(), PAGE_SCOPE);
    }

    public void release() {
	servlet = null;
	request = null;
	response = null;
	errorPageURL = null;
	
	// FIXME
    }

    // Get attribute with page scope
    public Object getAttribute(String name) {
	return pageAttributes.get(name);
    }

    // Set attribute with page scope
    public void setAttribute(String name, Object attribute) {
	pageAttributes.put(name,attribute);
    }

    // Get attribute with specified scope
    public Object getAttribute(String name, int scope) {
	Object attribute = null;
	try
	{
		switch (scope) {
			case PAGE_SCOPE:
				attribute = getAttribute(name);
				break;
			case REQUEST_SCOPE:
				attribute = request.getAttribute(name);
				break;
			case SESSION_SCOPE:
				attribute = session.getValue(name);
				break;
			case APPLICATION_SCOPE:
				attribute = getServletContext().getAttribute(name);
				break;
			default:
				throw new IllegalArgumentException
				    (JspConfig.getLocalizedMsg
				     (ERR_sp10_A_4_illegal_scope));
		}
	}
	finally
	{
		return attribute;
	}
    }

   // Set attribute with specified scope
    public void setAttribute(String name, Object attribute, int scope) {
	switch (scope) {
	case PAGE_SCOPE:
	    setAttribute(name, attribute);
	    break;
	case REQUEST_SCOPE:
	    if (JSDK20) {
		((HttpServletRequestWrapper) request).setAttribute(name,attribute);
	    } else {
		request.setAttribute(name, attribute);
	    }
	    break;
	case SESSION_SCOPE:
	    session.putValue(name, attribute);
	    break;
	case APPLICATION_SCOPE:
	    if (JSDK20) {
		((ServletContextWrapper) getServletContext())
		    .setAttribute(name, attribute);
	    } else {
		getServletContext().setAttribute(name, attribute);
	    }
	    break;
	default:
	    throw new IllegalArgumentException(
		  JspConfig.getLocalizedMsg(ERR_sp10_A_4_illegal_scope));
	}
    }

    // Search page, request, session, and application scopes in order
    public Object findAttribute(String name) {
	Object obj;
	if ((obj = getAttribute(name, PAGE_SCOPE)) == null) {
	    if ((obj = getAttribute(name, REQUEST_SCOPE)) == null) {
		if ((obj = getAttribute(name, SESSION_SCOPE)) == null) {
		    obj = getAttribute(name, APPLICATION_SCOPE);
		}
	    }
	}
	return obj;
    }

    // page scope
    public void removeAttribute(String name) {
	pageAttributes.remove(name);
    }

    public void removeAttribute(String name, int scope) {
	switch (scope) {
	case PAGE_SCOPE:
	    removeAttribute(name);
	    break;
	case REQUEST_SCOPE:
	    if (JSDK20) {
		((HttpServletRequestWrapper) request).setAttribute(name,null);
	    } else {
		request.setAttribute(name, null);
	    }
	    break;
	case SESSION_SCOPE:
	    session.removeValue(name);
	    break;
	case APPLICATION_SCOPE:
	    if (JSDK20) {
		((ServletContextWrapper) getServletContext())
		    .removeAttribute(name);
	    } else {
		getServletContext().removeAttribute(name);
	    }
	    break;
	default:
	    throw new IllegalArgumentException(
		  JspConfig.getLocalizedMsg(ERR_sp10_A_4_illegal_scope));
	}
    }

    public int getAttributesScope(String name) {
	if (getAttribute(name, PAGE_SCOPE) != null) return PAGE_SCOPE;
	if (getAttribute(name, REQUEST_SCOPE) != null) return REQUEST_SCOPE;
	if (getAttribute(name, SESSION_SCOPE) != null) return SESSION_SCOPE;
	if (getAttribute(name, APPLICATION_SCOPE) != null) return APPLICATION_SCOPE;
	return 0;
    }

    public Enumeration getAttributeNamesInScope(int scope) {
	switch (scope) {
	case PAGE_SCOPE:
	    return pageAttributes.keys();
	case REQUEST_SCOPE:
	    return JSDK20 ? ((HttpServletRequestWrapper) request).getAttributeNames() : request.getAttributeNames();
	case SESSION_SCOPE:
	    return new StringArrayEnumeration(session.getValueNames());
	case APPLICATION_SCOPE:
	    return JSDK20 ?
		((ServletContextWrapper) getServletContext()).getAttributeNames()
		: getServletContext().getAttributeNames();
	default:
	    throw new IllegalArgumentException(
		  JspConfig.getLocalizedMsg(ERR_sp10_A_4_illegal_scope));
	}
    }

    public Exception getException() {
	if (JSDK20) {
	    return (Exception) 
		((HttpServletRequestWrapper) request).getAttribute(EXCEPTION);
	} else {
	    return (Exception) 
		request.getAttribute(EXCEPTION);
	}
    }
    public JspWriter getOut() {
	return out;
    }

    public Object getPage() {
	return servlet;
    }

    public ServletRequest getRequest() {
	return request;
    }

    public ServletResponse getResponse() {
	return response;
    }

    public ServletConfig getServletConfig() {
	return servlet.getServletConfig();
    }

    public ServletContext getServletContext() {
	return servlet.getServletConfig().getServletContext();
    }

    public HttpSession getSession() {
       return session;
    }

    /**
     * Returns the URI of a document with a URI given relative to the
     * current JSP page.  Because different servlet engines implement
     * the current page differently, we check things in the following
     * order:
     * 1. getPathInfo() -- This works for Apache JServ and Jigsaw
     * 2. if null or empty, getServletPath()
     *
     * Note:  If you're writing a servlet engine, do it right!  Please 
     * check JSP 1.0 chapter B.8.12 Extension Mapping.
     */
    private String findURI(String relativeURI) {

	if (relativeURI.startsWith("/")) return relativeURI; // not so relative

	// pathInfo attribute is honored by JServ 1.0, Jigsaw 2.0.3
	String currentURI = ((HttpServletRequest) request).getPathInfo();

	// WebApp jo! for example
	if ((currentURI == null) || ("".equals(currentURI))) {
	    currentURI = ((HttpServletRequest) request).getServletPath();
	}

	int pos = currentURI.lastIndexOf('/');
	if (pos == -1) 
	    throw new IllegalArgumentException(
       	       JspConfig.getLocalizedMsg(ERR_gnujsp_cannot_interpret_pathinfo));
	return (currentURI.substring(0, pos + 1) + relativeURI);
    }

    private RequestDispatcher getRequestDispatcher(String path) throws IOException {
	RequestDispatcher dispatcher = JSDK20 
	    ? ((ServletContextWrapper) getServletContext()).getRequestDispatcher (path, out) 
	    : getServletContext().getRequestDispatcher(path);
	if (dispatcher == null) 
	    throw new IOException(JspConfig.getLocalizedMsg(ERR_sp10_B_9_cannot_find_request_dispatcher));
	return dispatcher;
    }
	
    public void forward (String relativeUrlPath) 
	throws ServletException, IOException
    {
	out.clear ();
	getRequestDispatcher(findURI(relativeUrlPath)).forward (request, response);
    }
    
    public void include (String relativeUrlPath)
	throws ServletException, IOException
    {
	RequestDispatcher dispatcher = null;
	out.flush ();
	getRequestDispatcher (findURI(relativeUrlPath)).include(request,response);
    }

    private String encodeAllHTML(String s) {
	  StringBuffer sb = new StringBuffer(s.length() + 100);
	  int i = 0;
	  char c = 0;
	  while(i < s.length()) {
	      c = s.charAt(i);
	      if(c == '<') {
	          sb.append("&lt;");
	      } else if(c == '>') {
	          sb.append("&gt;");
	      } else if(c == '&') {
	          sb.append("&amp;");
	      } else {
	          sb.append(c);
	      }
	      i++;
	  }
	  return sb.toString();
    }

    private String getStackTrace(Exception e) {
	StringWriter sw = new StringWriter();
	PrintWriter pw = new PrintWriter(sw);
	e.printStackTrace(pw);
	return sw.toString();
    }

    public void handlePageException (Exception e) {
	try {
	    boolean exceptionWhileForwarding = true;

	    if (JSDK20) {
		((HttpServletRequestWrapper) request).setAttribute(EXCEPTION,e);
	    } else {
		request.setAttribute(EXCEPTION,e);
	    }
	    
	    if (errorPageURL != null) {
		try {
		    forward (errorPageURL);
		    exceptionWhileForwarding = false;
		} catch (ServletException ex) {
		} catch (IOException ex) {
		} catch (IllegalStateException ex) {
		} 
	    }
	    
	    if (exceptionWhileForwarding || errorPageURL == null) {
		try {
		    // Allow control over whether exceptions 
		    // are shown to the user-agent
		    boolean exceptionsArePublic = 
			shouldShowExceptions();
		    
		    if(exceptionsArePublic) {
			if (errorPageURL != null)
			    out.println (JspConfig.getLocalizedMsg(ERR_sp10_2_7_1_cannot_forward_to_error_page)
					 + ": " + errorPageURL); 
			
			out.println("<PRE>Exception: "
				    + encodeAllHTML(e.toString()));
			out.println(encodeAllHTML(getStackTrace(e)));
			if (errorPageURL == null)
			    out.println(JspConfig.getLocalizedMsg(ERR_gnujsp_caught_exception_tell_admin_no_errorpage));
			out.println("</PRE>");
		    } else {
			// if we are not to show exceptions, 
			// simply report that an error occured 
			// and was logged
			
			if (errorPageURL != null)
			    out.println (JspConfig.getLocalizedMsg(ERR_sp10_2_7_1_cannot_forward_to_error_page)); 
			
			out.println (JspConfig.getLocalizedMsg(ERR_gnujsp_caught_exception_tell_admin_see_log));
		    }
		} catch(IOException ee) {}
	    }
	} finally {
	    // Log the error
	    log (JspConfig.getLocalizedMsg(ERR_gnujsp_error_processing_jsp_page)
	     + " " + ((HttpServletRequest) request).getPathInfo());

	    if (errorPageURL != null)
		log (JspConfig.getLocalizedMsg(ERR_sp10_2_7_1_cannot_forward_to_error_page)); 
	    else
		log (JspConfig.getLocalizedMsg(ERR_gnujsp_consider_an_errorpage));
	    log ("Exception: "+e);
	    log (getStackTrace(e));
	}
    }
    private void log(String s) {
	getServletContext().log(s);
    }
    /**
     * should exceptions be shown to the user?
     */
    private boolean shouldShowExceptions() {
	boolean exceptionsArePublic = true;
	try {
	    Boolean showExceptions = 
		(Boolean) getAttribute("org.gjt.jsp.showExceptions", 
				       SESSION_SCOPE);
	    if (showExceptions == null)	{
		exceptionsArePublic = !"false".equals(getServletConfig().getInitParameter("showExceptions"));
	    } else if (showExceptions.equals(Boolean.FALSE)) 
		exceptionsArePublic = false;
	} catch (Exception ex) { }
	
	return exceptionsArePublic;
    }
}
