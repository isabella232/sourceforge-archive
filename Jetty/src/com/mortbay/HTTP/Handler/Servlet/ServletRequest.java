// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================


package com.mortbay.HTTP.Handler.Servlet;
//import com.sun.java.util.collections.*; XXX-JDK1.1

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

    private HttpRequest _httpRequest;
    private ServletResponse _servletResponse;
    
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
		   HttpRequest request,
		   Context context)
    {
	_contextPath=contextPath;
        _httpRequest=request;
	_context=context;
    }

    /* ------------------------------------------------------------ */
    void setServletPath(String servletPath,String pathInfo)
    {
        _servletPath=servletPath;
	_pathInfo=pathInfo;
    }
    
    
    /* ------------------------------------------------------------ */
    HttpRequest getHttpRequest()
    {
	return _httpRequest;
    }
    
    /* ------------------------------------------------------------ */
    ServletResponse getServletResponse()
    {
	return _servletResponse;
    }
    
    /* ------------------------------------------------------------ */
    void setServletResponse(ServletResponse response)
    {
	_servletResponse = response;
    }
    
    /* ------------------------------------------------------------ */
    public Locale getLocale()
    {
	return (Locale)getLocales().nextElement();
    }
    
    /* ------------------------------------------------------------ */
    public Enumeration getLocales()
    {
	List acceptLanguage =
	    _httpRequest.getHeader().getValues("Accept-Language");

	// handle no locale
        if (acceptLanguage == null || acceptLanguage.size()==0)
	    return
		Collections.enumeration(Collections.singleton(Locale.getDefault()));
	
	
	// sort the list in quality order
	acceptLanguage = HttpFields.qualityList(acceptLanguage);
	
	if (acceptLanguage.size()==0)
	    return
		Collections.enumeration(Collections.singleton(Locale.getDefault()));

	// convert to locals
	for (int i=0; i<acceptLanguage.size(); i++)
	{
	    String language = (String)acceptLanguage.get(i);
	    String country = "";
	    int dash = language.indexOf("-");
	    if (dash > -1)
	    {
		country = language.substring(dash + 1).trim();
		language = language.substring(0,dash).trim();
	    }
	    
	    acceptLanguage.set(i,new Locale(language, country));
	}
	
	return Collections.enumeration(acceptLanguage);
    }

    
    /* ------------------------------------------------------------ */
    public String getAuthType()
    {
        Object o=_httpRequest.getAttribute(HttpRequest.__AuthType);
        if (o!=null)
            return o.toString();
        return null;
    }

    /* ------------------------------------------------------------ */
    public boolean isSecure()
    {
	return "HTTPS".equals(_httpRequest.getScheme()) ||
	    "https".equals(_httpRequest.getScheme());
    }
    
    /* ------------------------------------------------------------ */
    public Cookie[] getCookies()
    {
        Map cookies=_httpRequest.getCookies();
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
        return _httpRequest.getDateField(name);
    }
    
    /* ------------------------------------------------------------ */
    public Enumeration getHeaderNames()
    {
        return Collections.enumeration(_httpRequest.getFieldNames());
    }
    
    /* ------------------------------------------------------------ */
    public String getHeader(String name)
    {
        return _httpRequest.getField(name);
    }
    
    /* ------------------------------------------------------------ */
    public Enumeration getHeaders(String s)
    {
	List list=_httpRequest.getFieldValues(s);
	if (list==null)
	    return null;
	return Collections.enumeration(list);
    }
    
    /* ------------------------------------------------------------ */
    public int getIntHeader(String name)
    {
        return _httpRequest.getIntField(name);
    }
    
    /* ------------------------------------------------------------ */
    public String getMethod()
    {
        return _httpRequest.getMethod();
    }
    
    /* ------------------------------------------------------------ */
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
        return _httpRequest.getQuery();
    }
    
    /* ------------------------------------------------------------ */
    public String getRemoteUser()
    {
        Object o=_httpRequest.getAttribute(HttpRequest.__AuthUser);
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
                Map cookies=_httpRequest.getCookies();
                if (cookies!=null && cookies.size()>0)
                {
                    _sessionId=(String)cookies.get(Context.__SessionId);
                    if (_sessionId!=null && _sessionId.length()>0)
                    {
                        _sessionIdState = __SESSIONID_COOKIE;
                        Code.debug("Got Session ",_sessionId," from cookie");
                    }
                }
            }
            
            // check if there is a url encoded session param.
            String path = _servletPath;
            int prefix=path.indexOf(Context.__SessionUrlPrefix);
            if (prefix!=-1)
            {
                int suffix=path.indexOf(Context.__SessionUrlSuffix,prefix);
                if (suffix!=-1 && prefix<suffix)
                {
                    // definitely a session id in there!
                    String id =
                        path.substring(prefix+
                                       Context.__SessionUrlPrefix.length(),
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
                        if (suffix+Context.__SessionUrlSuffix.length()<path.length())
                            _servletPath =
                                path.substring(0,prefix)+
                                path.substring(suffix+
                                               Context.__SessionUrlSuffix.length());
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
	String path=_httpRequest.getPath();

	// remove any session stuff
	if (isRequestedSessionIdFromURL())
	{
            int prefix=path.indexOf(Context.__SessionUrlPrefix);
            if (prefix!=-1)
            {
                int suffix=path.indexOf(Context.__SessionUrlSuffix,prefix);
                if (suffix!=-1 && prefix<suffix)
                {    
		    // translate our path to drop the prefix off.
		    if (suffix+Context.__SessionUrlSuffix.length()<path.length())
			path =
			    path.substring(0,prefix)+
			    path.substring(suffix+
					   Context.__SessionUrlSuffix.length());
		    else
			path = path.substring(0,prefix);
		}
	    }    
	}
	return path;
    }
    
    /* ------------------------------------------------------------ */
    public String getServletPath()
    {
        return _servletPath;
    }
    
    /* ------------------------------------------------------------ */
    public HttpSession getSession(boolean create)
    {
        Code.debug("getSession(",new Boolean(create),")");
        
        if (_session != null && _context.isValid(_session))
            return _session;
        
        String id = getRequestedSessionId();
        
        if (id != null)
        {
            _session=_context.getSession(id);
            if (_session == null && !create)
                return null;
        }

        if (_session == null && create)
        {
            _session = _context.newSession();
            Cookie cookie =
                new Cookie(_context.__SessionId,_session.getId());
            cookie.setPath("/");
            getServletResponse().addCookie(cookie); 
        }

        return _session;
    }
    
    /* ------------------------------------------------------------ */
    public HttpSession getSession()
    {
        HttpSession session = getSession(false);
        return (session == null) ? getSession(true) : session;
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
        return _httpRequest.getAttribute(name);
    }
    
    /* -------------------------------------------------------------- */
    public void removeAttribute(String name)
    {
	_httpRequest.removeAttribute(name);
    }
    
    /* -------------------------------------------------------------- */
    public Enumeration getAttributeNames()
    {
        return Collections.enumeration(_httpRequest.getAttributeNames());
    }
    
    /* -------------------------------------------------------------- */
    public void setAttribute(String name, Object value)
    {
        _httpRequest.setAttribute(name,value);
    }
    
    /* -------------------------------------------------------------- */
    public String getCharacterEncoding()
    {
        return _httpRequest.getCharacterEncoding();
    }
    
    /* -------------------------------------------------------------- */
    public int getContentLength()
    {
        return _httpRequest.getIntField(HttpFields.__ContentLength);
    }
    
    /* -------------------------------------------------------------- */
    public String getContentType()
    {
        return _httpRequest.getField(HttpFields.__ContentType);
    }
    
    /* -------------------------------------------------------------- */
    public synchronized ServletInputStream getInputStream()
    {
        if (_inputState!=0 && _inputState!=1)
            throw new IllegalStateException();
        if (_in==null)
            _in = new ServletIn(_httpRequest.getInputStream());  
        _inputState=1;
        return _in;
    }
    
    /* -------------------------------------------------------------- */
    public String getParameter(String name)
    {
        return _httpRequest.getParameter(name);
    }
    
    /* -------------------------------------------------------------- */
    public Enumeration getParameterNames()
    {
        return Collections.enumeration(_httpRequest.getParameterNames());
    }
    
    /* -------------------------------------------------------------- */
    public String[] getParameterValues(String name)
    {
        List v = _httpRequest.getParameterValues(name);
        if (v==null)
            return null;
	String[]a=new String[v.size()];
        return (String[])v.toArray(a);
    }
    
    /* -------------------------------------------------------------- */
    public String getProtocol()
    {
        return _httpRequest.getVersion();
    }
    
    /* -------------------------------------------------------------- */
    public String getScheme()
    {
        return _httpRequest.getScheme();
    }
    
    /* -------------------------------------------------------------- */
    public String getServerName()
    {
        return _httpRequest.getHost();
    }
    
    /* -------------------------------------------------------------- */
    public int getServerPort()
    {
	int port = _httpRequest.getPort();
        return port==0?80:port;
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
        HttpConnection connection = _httpRequest.getConnection();
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
        HttpConnection connection = _httpRequest.getConnection();
        if (connection!=null)
        {
            InetAddress addr = connection.getRemoteAddr();
            if (addr!=null)
                remoteHost = addr.getHostName();
        }
        return remoteHost;
    }

    /* -------------------------------------------------------------- */
    public String getRealPath(String path)
    {
	_context.getRealPath(path);
        return null;
    }

    /* ------------------------------------------------------------ */
    public RequestDispatcher getRequestDispatcher(String url)
    {
	if (url == null)
            return null;

        if (!url.startsWith("/"))
	{
	    String relTo=_servletPath+_pathInfo;
	    
	    int slash=relTo.lastIndexOf("/");
	    relTo=relTo.substring(0,slash);
	    
	    while(url.startsWith("../"))
	    {
		if (relTo.length()==0)
		    return null;
		url=url.substring(3);
		slash=relTo.lastIndexOf("/");
		relTo=relTo.substring(0,slash);
	    }

	    url=relTo+url;
	    
	}
    
	return _context.getRequestDispatcher(url);
    }

}






