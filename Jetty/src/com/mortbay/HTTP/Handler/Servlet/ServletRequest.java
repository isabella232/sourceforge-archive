// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================


package com.mortbay.HTTP.Handler.Servlet;

import com.sun.java.util.collections.*;
import com.mortbay.HTTP.*;
import com.mortbay.Util.*;
import java.io.*;
import java.net.InetAddress;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.*;
import java.security.Principal;


/* ------------------------------------------------------------ */
/** Servlet Request Wrapper
 * This class wraps a Jetty HTTP request as a 2.1 Servlet
 * request.
 * <p>
 *
 * <p><h4>Notes</h4>
 * <p>
 *
 * <p><h4>Usage</h4>
 * <pre>
 */
/*
 * </pre>
 *
 * @see
 * @version 1.0 Thu Jan 27 2000
 * @author Greg Wilkins (gregw)
 */
class ServletRequest
    implements HttpServletRequest
{
    /* -------------------------------------------------------------- */
    /** For decoding session ids etc. */
    public static final String
        __SESSIONID_NOT_CHECKED = "not checked",
        __SESSIONID_URL = "url",
        __SESSIONID_COOKIE = "cookie",
        __SESSIONID_NONE = "none";
    public static final String
        __SessionId = "JettySessionId",
        __SessionUrlPrefix = "_s_",
        __SessionUrlSuffix = "_S_";

    private HttpRequest _request;
    private String _contextPath=null;
    private String _servletPath=null;
    private String _pathInfo=null;
    private String _pathTranslated=null;
    private String _sessionId=null;
    private HttpSession _session=null;
    private String _sessionIdState=__SESSIONID_NOT_CHECKED;
    private ServletIn _in =null;
    private BufferedReader _reader=null;
    private int _inputState=0;
    private Context _context;

    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param request 
     */
    ServletRequest(String contextPath,
		   String servletPath,
		   String pathInfo,
		   HttpRequest request,
		   Context context)
    {
	_contextPath=contextPath;
        _servletPath=servletPath;
	_pathInfo=pathInfo;
        _request=request;
	_context=context;
    }
    
    
    /* ------------------------------------------------------------ */
    /**
     *
     * Returns the preferred <code>Locale</code> that the client will 
     * accept content in, based on the Accept-Language header.
     * If the client request doesn't provide an Accept-Language header,
     * this method returns the default locale for the server.
     *
     */
    public Locale getLocale()
    {
	Code.notImplemented();
	return null;
    }
    
    /* ------------------------------------------------------------ */
    /**
     *
     * Returns an <code>Enumeration</code> of <code>Locale</code> objects
     * indicating, in decreasing order starting with the preferred locale, the
     * locales that are acceptable to the client based on the Accept-Language
     * header.
     * If the client request doesn't provide an Accept-Language header,
     * this method returns an <code>Enumeration</code> containing one 
     * <code>Locale</code>, the default locale for the server.
     *
     *
     * @return		an <code>Enumeration</code> of preferred 
     *                  <code>Locale</code> objects for the client
     *
     */
    public Enumeration getLocales()
    {
	Code.notImplemented();
	return null;
    }
    
    /* ------------------------------------------------------------ */
    public String getAuthType()
    {
        Object o=_request.getAttribute(HttpRequest.__AuthType);
        if (o!=null)
            return o.toString();
        return null;
    }

    /* ------------------------------------------------------------ */
    /**
     *
     * Returns a boolean indicating whether this request was made using a
     * secure channel, such as HTTPS.
     *
     *
     * @return		a boolean indicating if the request was made using a
     *                  secure channel
     *
     */
    public boolean isSecure()
    {
	Code.notImplemented();
	return false;
    }
    
    /* ------------------------------------------------------------ */
    public Cookie[] getCookies()
    {
        Map cookies=_request.getCookies();
        if (cookies==null|| cookies.size()==0)
            return null; // XXX or empty set?

        // XXX save this for next call?
        Cookie[] ca = new Cookie[cookies.size()];
        int c=0;
        Iterator i=cookies.keySet().iterator();
        while(i.hasNext())
        {
            String cookie=i.next().toString();
            ca[c++] = new Cookie(cookie,cookies.get(cookie).toString());
        }
        return ca;
    }
    
    /* ------------------------------------------------------------ */
    public long getDateHeader(String name)
    {
        return _request.getDateField(name);
    }
    
    /* ------------------------------------------------------------ */
    public Enumeration getHeaderNames()
    {
        return Collections.enumeration(_request.getFieldNames());
    }
    
    /* ------------------------------------------------------------ */
    public String getHeader(String name)
    {
        return _request.getField(name);
    }
    
    /* ------------------------------------------------------------ */
    /**
     *
     * Returns all the values of the specified request header
     * as an <code>Enumeration</code> of <code>String</code> objects.
     *
     * <p>Some headers, such as <code>Accept-Language</code> can be sent
     * by clients as several headers each with a different value rather than
     * sending the header as a comma separated list.
     *
     * <p>If the request did not include any headers
     * of the specified name, this method returns an empty
     * <code>Enumeration</code>.
     * The header name is case insensitive. You can use
     * this method with any request header.
     *
     * @param name		a <code>String</code> specifying the
     *				header name
     *
     * @return			a <code>Enumeration</code> containing the
     *				values of the requested
     *				header, or <code>null</code>
     *				if the request does not
     *				have any headers of that name
     *
     */			
    public Enumeration getHeaders(String s)
    {
	Code.notImplemented();
	return null;
    }
    
    /* ------------------------------------------------------------ */
    public int getIntHeader(String name)
    {
        return _request.getIntField(name);
    }
    
    /* ------------------------------------------------------------ */
    public String getMethod()
    {
        return _request.getMethod();
    }
    
    /* ------------------------------------------------------------ */
    /**
     *
     * Returns the portion of the request URI that indicates the context
     * of the request.  The context path always comes first in a request
     * URI.  The path starts with a "/" character but does not end with a "/"
     * character.  For servlets in the default (root) context, this method
     * returns "".
     *
     *
     * @return		a <code>String</code> specifying the
     *			portion of the request URI that indicates the context
     *			of the request
     *
     *
     */
    public String getContextPath()
    {
	return _contextPath;
    }
    
    /* ------------------------------------------------------------ */
    public String getPathInfo()
    {
        return _pathInfo;
    }
    
    /* ------------------------------------------------------------ */
    public String getPathTranslated()
    {
        if (_pathInfo==null || _pathInfo.length()==0)
            return null;
        if (_pathTranslated==null)
            _pathTranslated=_context.getRealPathInfo(_pathInfo);
        return _pathTranslated;
    }
    
    /* ------------------------------------------------------------ */
    public String getQueryString()
    {
        return _request.getQuery();
    }
    
    /* ------------------------------------------------------------ */
    public String getRemoteUser()
    {
        Object o=_request.getAttribute(HttpRequest.__AuthUser);
        if (o!=null)
            return o.toString();
        return null;
    }

    /* ------------------------------------------------------------ */
    /**
     *
     * Returns a boolean indicating whether the authenticated user is included
     * in the specified logical "role".  Roles and role membership can be
     * defined using deployment descriptors.  If the user has not been
     * authenticated, the method returns <code>false</code>.
     *
     * @param role		a <code>String</code> specifying the name
     *				of the role
     *
     * @return		a <code>boolean</code> indicating whether
     *			the user making this request belongs to a given role;
     *			<code>false</code> if the user has not been 
     *			authenticated
     *
     */
    public boolean isUserInRole(String role)
    {
	Code.notImplemented();
	return false;
    }

    /* ------------------------------------------------------------ */
    /**
     *
     * Returns a <code>java.security.Principal</code> object containing
     * the name of the current authenticated user. If the user has not been
     * authenticated, the method returns <code>null</code>.
     *
     * @return		a <code>java.security.Principal</code> containing
     *			the name of the user making this request;
     *			<code>null</code> if the user has not been 
     *			authenticated
     *
     */
    public Principal getUserPrincipal()
    {
	Code.notImplemented();
	return null;
    }
    
    
    /* ------------------------------------------------------------ */
    public String getRequestedSessionId()
    {
        if (_sessionIdState == __SESSIONID_NOT_CHECKED)
        {          
            // Then try cookies
            if (_sessionId == null)
            {
                Map cookies=_request.getCookies();
                if (cookies!=null && cookies.size()>0)
                {
                    _sessionId=(String)cookies.get(__SessionId);
                    if (_sessionId!=null && _sessionId.length()>0)
                    {
                        _sessionIdState = __SESSIONID_COOKIE;
                        Code.debug("Got Session ",_sessionId," from cookie");
                    }
                }
            }
            
            // check if there is a url encoded session param.
            String path = _servletPath;
            int prefix=path.indexOf(__SessionUrlPrefix);
            if (prefix!=-1)
            {
                int suffix=path.indexOf(__SessionUrlSuffix);
                if (suffix!=-1 && prefix<suffix)
                {
                    // definitely a session id in there!
                    String id =
                        path.substring(prefix+
                                       __SessionUrlPrefix.length(),
                                       suffix);
                    
                    Code.debug("Got Session ",id," from URL");
                    
                    try
                    {
                        Long.parseLong(id,36);
                        if (_sessionIdState==__SESSIONID_NOT_CHECKED)
                        {
                            _sessionId=id;
                            _sessionIdState = __SESSIONID_URL;
                        }
                        else if (!id.equals(_sessionId))
                            Code.warning("Mismatched session IDs");
                        
                        // translate our path to drop the prefix off.
                        if (suffix+__SessionUrlSuffix.length()<path.length())
                            _servletPath =
                                path.substring(0,prefix)+
                                path.substring(suffix+
                                               __SessionUrlSuffix.length());
                        else
                            _servletPath = path.substring(0,prefix);
                        
                        Code.debug("Translated servlet path="+_servletPath);
                    }
                    catch(NumberFormatException e)
                    {
                        Code.ignore(e);
                    }
                }
            }
            
            if (_sessionId == null)
                _sessionIdState = __SESSIONID_NONE;
        }
        
        return _sessionId;
    }
    
    /* ------------------------------------------------------------ */
    public String getRequestURI()
    {
        return _request.getPath();
    }
    
    /* ------------------------------------------------------------ */
    public String getServletPath()
    {
        return _servletPath;
    }
    
    /* ------------------------------------------------------------ */
    public HttpSession getSession(boolean create)
    {
        return null; // XXX
    }
    
    /* ------------------------------------------------------------ */
    public HttpSession getSession()
    {
        return null; // XXX
    }
    
    /* ------------------------------------------------------------ */
    public boolean isRequestedSessionIdValid()
    {
        return _sessionId != null && getSession(false) != null;
    }
    
    /* -------------------------------------------------------------- */
    public boolean isRequestedSessionIdFromCookie()
    {
        return _sessionIdState == __SESSIONID_COOKIE;
    }
    
    /* -------------------------------------------------------------- */
    public boolean isRequestedSessionIdFromURL()
    {
        return _sessionIdState == __SESSIONID_URL;
    }
    
    /* -------------------------------------------------------------- */
    /**
     * @deprecated
     */
    public boolean isRequestedSessionIdFromUrl()
    {
        return isRequestedSessionIdFromURL();
    }
    
    /* -------------------------------------------------------------- */
    public Object getAttribute(String name)
    {
        return _request.getAttribute(name);
    }
    
    /* -------------------------------------------------------------- */
    public void removeAttribute(String name)
    {
	Code.notImplemented();
    }
    
    /* -------------------------------------------------------------- */
    public Enumeration getAttributeNames()
    {
        return Collections.enumeration(_request.getAttributeNames());
    }
    
    /* -------------------------------------------------------------- */
    public String getCharacterEncoding()
    {
        return _request.getCharacterEncoding();
    }
    
    /* -------------------------------------------------------------- */
    public int getContentLength()
    {
        return _request.getIntField(HttpFields.__ContentLength);
    }
    
    /* -------------------------------------------------------------- */
    public String getContentType()
    {
        return _request.getField(HttpFields.__ContentType);
    }
    
    /* -------------------------------------------------------------- */
    public synchronized ServletInputStream getInputStream()
    {
        if (_inputState!=0 && _inputState!=1)
            throw new IllegalStateException();
        if (_in==null)
            _in = new ServletIn(_request.getInputStream());  
        _inputState=1;
        return _in;
    }
    
    /* -------------------------------------------------------------- */
    public String getParameter(String name)
    {
        return _request.getParameter(name);
    }
    
    /* -------------------------------------------------------------- */
    public Enumeration getParameterNames()
    {
        return Collections.enumeration(_request.getParameterNames());
    }
    
    /* -------------------------------------------------------------- */
    public String[] getParameterValues(String name)
    {
        List v = _request.getParameterValues(name);
        if (v==null)
            return null;
	String[]a=new String[v.size()];
        return (String[])v.toArray(a);
    }
    
    /* -------------------------------------------------------------- */
    public String getProtocol()
    {
        return _request.getVersion();
    }
    
    /* -------------------------------------------------------------- */
    public String getScheme()
    {
        return _request.getScheme();
    }
    
    /* -------------------------------------------------------------- */
    public String getServerName()
    {
        return _request.getHost();
    }
    
    /* -------------------------------------------------------------- */
    public int getServerPort()
    {
        return _request.getPort();
    }
    
    /* -------------------------------------------------------------- */
    public synchronized BufferedReader getReader()
    {
        if (_inputState!=0 && _inputState!=2)
            throw new IllegalStateException();
        if (_reader==null)
        {
            try
            {
                _reader=new BufferedReader(new InputStreamReader(getInputStream(),"ISO-8859-1"));
            }
            catch(UnsupportedEncodingException e)
            {
                Code.ignore(e);
                _reader=new BufferedReader(new InputStreamReader(getInputStream()));
            }
            _inputState=2;
        }
        return _reader;
    }
    
    /* -------------------------------------------------------------- */
    public String getRemoteAddr()
    {
        HttpConnection connection = _request.getConnection();
        if (connection!=null)
        {
            InetAddress addr = connection.getRemoteAddr();
            if (addr!=null)
                return addr.getHostAddress();
        }
        return "127.0.0.1";
    }
    
    /* -------------------------------------------------------------- */
    public String getRemoteHost()
    {
        String remoteHost=null;
        HttpConnection connection = _request.getConnection();
        if (connection!=null)
        {
            InetAddress addr = connection.getRemoteAddr();
            if (addr!=null)
                remoteHost = addr.getHostName();
        }
        return remoteHost;
    }

    /* -------------------------------------------------------------- */
    public void setAttribute(String name, Object value)
    {
        _request.setAttribute(name,value);
    }
    
    /* -------------------------------------------------------------- */
    public String getRealPath(String path)
    {
	_context.getRealPath(path);
        return null;
    }

    /* ------------------------------------------------------------ */
    /**
     *
     * Returns a {@link RequestDispatcher} object that acts as a wrapper for
     * the resource located at the given path.  
     * A <code>RequestDispatcher</code> object can be used to forward
     * a request to the resource or to include the resource in a response.
     * The resource can be dynamic or static.
     *
     * <p>The pathname specified may be relative, although it cannot extend
     * outside the current servlet context.  If the path begins with 
     * a "/" it is interpreted as relative to the current context root.  
     * This method returns <code>null</code> if the servlet container
     * cannot return a <code>RequestDispatcher</code>.
     *
     * <p>The difference between this method and {@link
     * ServletContext#getRequestDispatcher} is that this method can take a
     * relative path.
     *
     * @param path      a <code>String</code> specifying the pathname
     *                  to the resource
     *
     * @return          a <code>RequestDispatcher</code> object
     *                  that acts as a wrapper for the resource
     *                  at the specified path
     *
     * @see             RequestDispatcher
     * @see             ServletContext#getRequestDispatcher
     *
     */
    public RequestDispatcher getRequestDispatcher(String url)
    {
	Code.notImplemented();
	return null;
    }
    
}




