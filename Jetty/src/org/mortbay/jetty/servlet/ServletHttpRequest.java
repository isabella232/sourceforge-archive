// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.security.Principal;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUtils;
import javax.servlet.http.HttpServletRequestWrapper;
import org.mortbay.http.ChunkableInputStream;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpConnection;
import org.mortbay.http.HttpFields;
import org.mortbay.http.HttpMessage;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.handler.NullHandler;
import org.mortbay.util.Code;
import org.mortbay.util.LazyList;
import org.mortbay.util.MultiMap;
import org.mortbay.util.Resource;
import org.mortbay.util.StringUtil;
import org.mortbay.util.URI;


/* ------------------------------------------------------------ */
/** Servlet Request Wrapper.
 * This class wraps a Jetty HTTP request as a 2.2 Servlet
 * request.
 *
 * Note that this wrapper is not synchronized and if a request is to
 * be operated on by multiple threads, then higher level
 * synchronizations may be required.
 * 
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class ServletHttpRequest
    implements HttpServletRequest
{
    /* -------------------------------------------------------------- */
    public static final String
        __SESSIONID_NOT_CHECKED = "not checked",
        __SESSIONID_URL = "url",
        __SESSIONID_COOKIE = "cookie",
        __SESSIONID_NONE = "none";

    private static final Enumeration __emptyEnum =  
        Collections.enumeration(Collections.EMPTY_LIST);
    private static final Collection __defaultLocale =
        Collections.singleton(Locale.getDefault());

    private ServletHandler _servletHandler;    
    private HttpRequest _httpRequest;
    private ServletHttpResponse _servletHttpResponse;

    private String _contextPath=null;
    private String _servletPath=null;
    private String _pathInfo=null;
    private String _query=null;
    private String _pathTranslated=null;
    private String _sessionId=null;
    private HttpSession _session=null;
    private String _sessionIdState=__SESSIONID_NOT_CHECKED;
    private ServletIn _in =null;
    private BufferedReader _reader=null;
    private int _inputState=0;
    private ServletHolder _servletHolder;
    private String _pathInContext;
    private ServletRequest _wrapper;
    private HttpMessage _facade = new Facade();
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     */
    ServletHttpRequest(ServletHandler servletHandler,
                       String pathInContext,
                       HttpRequest request)
    {
        _servletHandler=servletHandler;
        _pathInContext=pathInContext;
        _contextPath=_servletHandler.getHttpContext().getContextPath();
        if (_contextPath.length()<=1)
            _contextPath="";

        _httpRequest=request;
    }

    /* ------------------------------------------------------------ */
    HttpMessage getFacade()
    {
        return _facade;
    }
    
    /* ------------------------------------------------------------ */
    ServletHandler getServletHandler()
    {
        return _servletHandler;
    }

    /* ------------------------------------------------------------ */
    void setServletHandler(ServletHandler servletHandler)
    {
        _servletHandler=servletHandler;
    }

    /* ------------------------------------------------------------ */
    /** Set a ServletRequest Wrapper.
     * This call is used by the Dispatcher and the FilterHandler to
     * store a user generated wrapper for this request. The wrapper is
     * recovered by getWrapper for use by the next filter and/or
     * servlet.
     *
     * Note that the ServletHttpRequest is always the facade object of
     * the HttpRequest, even if a ServletRequest object is set here.
     * @param wrapper 
     */
    void setWrapper(ServletRequest wrapper)
    {
        if (wrapper == this)
            _wrapper=null;
        else
            _wrapper=wrapper;
    }
    
    /* ------------------------------------------------------------ */
    /** 
     * @return The top most wrapper of the request or this request if
     * there are no wrappers. 
     */
    ServletRequest getWrapper()
    {
        if (_wrapper==null)
            return this;
        return _wrapper;
    }
    
    /* ------------------------------------------------------------ */
    /** Set servletpath and pathInfo.
     * Called by the Handler before passing a request to a particular
     * holder to split the context path into a servlet path and path info.
     * @param servletPath 
     * @param pathInfo 
     */
    void setServletPaths(String servletPath,
                         String pathInfo,
                         ServletHolder holder)
    {
        _servletPath=servletPath;
        _pathInfo=pathInfo;
        _servletHolder=holder;
    }
    
    /* ------------------------------------------------------------ */
    ServletHolder getServletHolder()
    {
        return _servletHolder;
    }
    
    /* ------------------------------------------------------------ */
    String getPathInContext()
    {
        return _pathInContext;
    }
    
    /* ------------------------------------------------------------ */
    HttpRequest getHttpRequest()
    {
        return _httpRequest;
    }
    
    /* ------------------------------------------------------------ */
    public ServletHttpResponse getServletHttpResponse()
    {
        return _servletHttpResponse;
    }
    
    /* ------------------------------------------------------------ */
    void setServletHttpResponse(ServletHttpResponse response)
    {
        _servletHttpResponse = response;
    }

    /* ------------------------------------------------------------ */
    public Locale getLocale()
    {
        Enumeration enum = _httpRequest.getFieldValues(HttpFields.__AcceptLanguage,
                                                       HttpFields.__separators);

        // handle no locale
        if (enum == null || !enum.hasMoreElements())
            return Locale.getDefault();
        
        // sort the list in quality order
        List acceptLanguage = HttpFields.qualityList(enum);
        if (acceptLanguage.size()==0)
            return  Locale.getDefault();

        LazyList langs = null;
        int size=acceptLanguage.size();
        
        // convert to locals
        for (int i=0; i<size; i++)
        {
            String language = (String)acceptLanguage.get(i);
            language=HttpFields.valueParameters(language,null);
            String country = "";
            int dash = language.indexOf('-');
            if (dash > -1)
            {
                country = language.substring(dash + 1).trim();
                language = language.substring(0,dash).trim();
            }
            return new Locale(language,country);
        }

        return  Locale.getDefault();
    }
    
    /* ------------------------------------------------------------ */
    public Enumeration getLocales()
    {
        Enumeration enum = _httpRequest.getFieldValues(HttpFields.__AcceptLanguage,
                                                       HttpFields.__separators);

        // handle no locale
        if (enum == null || !enum.hasMoreElements())
            return Collections.enumeration(__defaultLocale);
        
        // sort the list in quality order
        List acceptLanguage = HttpFields.qualityList(enum);
        
        if (acceptLanguage.size()==0)
            return
                Collections.enumeration(__defaultLocale);

        LazyList langs = null;
        int size=acceptLanguage.size();
        
        // convert to locals
        for (int i=0; i<size; i++)
        {
            String language = (String)acceptLanguage.get(i);
            language=HttpFields.valueParameters(language,null);
            String country = "";
            int dash = language.indexOf('-');
            if (dash > -1)
            {
                country = language.substring(dash + 1).trim();
                language = language.substring(0,dash).trim();
            }
            langs=LazyList.add(langs,size,
                               new Locale(language,country));
        }

        if (LazyList.size(langs)==0)
            return Collections.enumeration(__defaultLocale);
            
        return Collections.enumeration(LazyList.getList(langs));
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
        return "https".equalsIgnoreCase(_httpRequest.getScheme());
    }
    
    /* ------------------------------------------------------------ */
    public Cookie[] getCookies()
    {
        Cookie[] cookies = _httpRequest.getCookies();
        if (cookies.length==0)
            return null;
        return cookies;
    }
    
    /* ------------------------------------------------------------ */
    public long getDateHeader(String name)
    {
        return _httpRequest.getDateField(name);
    }
    
    /* ------------------------------------------------------------ */
    public Enumeration getHeaderNames()
    {
        return _httpRequest.getFieldNames();
    }
    
    /* ------------------------------------------------------------ */
    public String getHeader(String name)
    {
        return _httpRequest.getField(name);
    }
    
    /* ------------------------------------------------------------ */
    public Enumeration getHeaders(String s)
    {
        Enumeration enum=_httpRequest.getFieldValues(s,",");
        if (enum==null)
            return __emptyEnum;
        return enum;
    }
    
    /* ------------------------------------------------------------ */
    public int getIntHeader(String name)
        throws NumberFormatException
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
        if (_servletPath==null)
            return null; 
        return _pathInfo;
    }
    
    /* ------------------------------------------------------------ */
    public String getPathTranslated()
    {
        if (_pathInfo==null || _pathInfo.length()==0)
            return null;
        if (_pathTranslated==null)
        {
            Resource resource =
                _servletHandler.getHttpContext().getBaseResource();

            if (resource==null)
                return null;

            try
            {
                resource=resource.addPath(_pathInfo);
                File file = resource.getFile();
                if (file==null)
                    return null;
                _pathTranslated=file.getAbsolutePath();
            }
            catch(Exception e)
            {
                Code.debug(e);
            }
        }
        
        return _pathTranslated;
    }
    
    /* ------------------------------------------------------------ */
    public String getQueryString()
    {
        if (_query==null)
            _query =_httpRequest.getQuery();
        return _query;
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
    public boolean isUserInRole(String role)
    {
        return _httpRequest.isUserInRole(role);
    }

    /* ------------------------------------------------------------ */
    public Principal getUserPrincipal()
    {
        return _httpRequest.getUserPrincipal();
    }
    
    /* ------------------------------------------------------------ */
    void setSessionId(String pathParams)
    {
        _sessionId=null;
        
        // try cookies first
        if (_servletHandler.isUsingCookies())
        {
            Cookie[] cookies=_httpRequest.getCookies();
            if (cookies!=null && cookies.length>0)
            {
                for (int i=0;i<cookies.length;i++)
                {
                    if (SessionManager.__SessionId.equals(cookies[i].getName()))
                    {
                        _sessionId=cookies[i].getValue();
                        _sessionIdState = __SESSIONID_COOKIE;
                        Code.debug("Got Session ",_sessionId," from cookie");
                        break;
                    }
                }
            }
        }
            
        // check if there is a url encoded session param.
        if (pathParams!=null && pathParams.startsWith(SessionManager.__SessionId))
        {
            String id =
                pathParams.substring(SessionManager.__SessionId.length()+1);
            Code.debug("Got Session ",id," from URL");
            
            if (_sessionId==null)
            {
                _sessionId=id;
                _sessionIdState = __SESSIONID_URL;
            }
            else if (!id.equals(_sessionId))
                Code.warning("Mismatched session IDs");
        }
        
        if (_sessionId == null)
            _sessionIdState = __SESSIONID_NONE;        
    }
    
    /* ------------------------------------------------------------ */
    public String getRequestedSessionId()
    {
        return _sessionId;
    }
    
    /* ------------------------------------------------------------ */
    public String getRequestURI()
    {
        return _httpRequest.getEncodedPath();
    }
    
    /* ------------------------------------------------------------ */
    public StringBuffer getRequestURL()
    {
        StringBuffer buf = _httpRequest.getRootURL();
        buf.append(getRequestURI());
        return buf;
    }
    
    /* ------------------------------------------------------------ */
    public String getServletPath()
    {
        if (_servletPath==null)
            return _pathInContext;
        return _servletPath;
    }
    
    /* ------------------------------------------------------------ */
    public HttpSession getSession(boolean create)
    {        
        if (_session != null && ((SessionManager.Session)_session).isValid())
            return _session;
        
        String id = getRequestedSessionId();
        
        if (id != null)
        {
            _session=_servletHandler.getHttpSession(id);
            if (_session == null && !create)
                return null;
        }

        if (_session == null && create)
        {
            _session = _servletHandler.newHttpSession();
            if (_servletHandler.isUsingCookies())
            {
                Cookie cookie =
                    new Cookie(SessionManager.__SessionId,_session.getId());
                String path=getContextPath();
                if (path==null || path.length()==0)
                    path="/";
                cookie.setPath(path);
                _servletHttpResponse.getHttpResponse().addSetCookie(cookie,false);
                cookie.setVersion(1);
                _servletHttpResponse.getHttpResponse().addSetCookie(cookie,true); 
            }
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
    public Enumeration getAttributeNames()
    {
        return _httpRequest.getAttributeNames();
    }
    
    /* -------------------------------------------------------------- */
    public Object getAttribute(String name)
    {
        return _httpRequest.getAttribute(name);
    }
    
    /* -------------------------------------------------------------- */
    public void setAttribute(String name, Object value)
    {
        if (name.startsWith("org.mortbay.http"))
        {
            Code.warning("Servlet attempted update of "+name);
            return;
        }
        _httpRequest.setAttribute(name,value);
    }
    
    /* -------------------------------------------------------------- */
    public void removeAttribute(String name)
    {
        if (name.startsWith("org.mortbay.http"))
        {
            Code.warning("Servlet attempted update of "+name);
            return;
        }
        _httpRequest.removeAttribute(name);
    }
    
    /* -------------------------------------------------------------- */
    public void setCharacterEncoding(String encoding)
        throws UnsupportedEncodingException
    {
        if (_inputState!=0)
            throw new IllegalStateException("getReader() or getInputStream() called");
        _httpRequest.setCharacterEncoding(encoding);
    }
    
    /* -------------------------------------------------------------- */
    public String getCharacterEncoding()
    {
        return _httpRequest.getCharacterEncoding();
    }
    
    /* -------------------------------------------------------------- */
    public int getContentLength()
    {
        return _httpRequest.getContentLength();
    }
    
    /* -------------------------------------------------------------- */
    public String getContentType()
    {
        return _httpRequest.getContentType();
    }
    
    /* -------------------------------------------------------------- */
    public ServletInputStream getInputStream()
    {
        if (_inputState!=0 && _inputState!=1)
            throw new IllegalStateException();
        if (_in==null)
            _in = new ServletIn((ChunkableInputStream)_httpRequest.getInputStream());  
        _inputState=1;
        _reader=null;
        return _in;
    }
    
    /* -------------------------------------------------------------- */
    /**
     * This method is not recommended as it forces the generation of a
     * non-optimal data structure.
     */
    public Map getParameterMap()
    {
        return Collections.unmodifiableMap(_httpRequest.getParameterStringArrayMap());
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
        List v=_httpRequest.getParameterValues(name);
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
        if (port==0)
        {
            if (getScheme().equalsIgnoreCase("https"))
                return 443;
            return 80;
        }
        return port;
    }
    
    /* -------------------------------------------------------------- */
    public BufferedReader getReader()
    {
        if (_inputState!=0 && _inputState!=2)
            throw new IllegalStateException();
        if (_reader==null)
        {
            try
            {
                String encoding=getCharacterEncoding();
                if (encoding==null)
                    encoding=StringUtil.__ISO_8859_1;
                _reader=new BufferedReader(new InputStreamReader(getInputStream(),encoding));
            }
            catch(UnsupportedEncodingException e)
            {
                Code.warning(e);
                _reader=new BufferedReader(new InputStreamReader(getInputStream()));
            }
        }
        _inputState=2;
        return _reader;
    }
    
    /* -------------------------------------------------------------- */
    public String getRemoteAddr()
    {
        return _httpRequest.getRemoteAddr();
    }
    
    /* -------------------------------------------------------------- */
    public String getRemoteHost()
    {
        if (_httpRequest.getHttpConnection()==null)
            return null;
        return _httpRequest.getRemoteHost();
    }

    /* -------------------------------------------------------------- */
    public String getRealPath(String path)
    {
        return _servletHandler.getServletContext().getRealPath(path);
    }

    /* ------------------------------------------------------------ */
    public RequestDispatcher getRequestDispatcher(String url)
    {
        if (url == null)
            return null;

        if (!url.startsWith("/"))
        {
            String relTo=URI.addPaths(_servletPath,_pathInfo);
            int slash=relTo.lastIndexOf("/");
            if (slash>1)
                relTo=relTo.substring(0,slash+1);
            else
                relTo="/";
            url=URI.addPaths(relTo,url);
        }
    
        return _servletHandler.getServletContext().getRequestDispatcher(url);
    }

    /* ------------------------------------------------------------ */
    public String toString()
    {
        return
            getContextPath()+"+"+getServletPath()+"+"+getPathInfo()+"\n"+
            _httpRequest.toString();
    }


    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /** HttpMessage Facade.
     * This facade allows the ServletHttpRequest to be treated as a
     * HttpMessage by HttpHandlers.
     */
    public class Facade implements HttpMessage.Request
    {
        public ServletHttpRequest getServletHttpRequest()
        {return ServletHttpRequest.this;}
        
        public InputStream getInputStream() throws IOException
        {return getWrapper().getInputStream();}
        public OutputStream getOutputStream()
        {throw new UnsupportedOperationException();}
        
        public boolean containsField(String name)
        {return getHeader(name)!=null;}
        public Enumeration getFieldNames()
        {return ((HttpServletRequest)getWrapper()).getHeaderNames();}
        public Enumeration getFieldValues(String name)
        {return ((HttpServletRequest)getWrapper()).getHeaders(name);}
        public Enumeration getFieldValues(String name, String separators)
        {throw new UnsupportedOperationException();}
        
        public String getField(String name)
        {return ((HttpServletRequest)getWrapper()).getHeader(name);}
        public int getIntField(String name)
        {return ((HttpServletRequest)getWrapper()).getIntHeader(name);}
        public long getDateField(String name)
        {return ((HttpServletRequest)getWrapper()).getDateHeader(name);}
        
        public String setField(String name, String value){throw new UnsupportedOperationException();}
        public void setField(String name, List values){throw new UnsupportedOperationException();}
        public void setIntField(String name, int value){throw new UnsupportedOperationException();}
        public void setDateField(String name, long date){throw new UnsupportedOperationException();}
        
        public void addField(String name, String value){throw new UnsupportedOperationException();}
        public void addIntField(String name, int value){throw new UnsupportedOperationException();}
        public void addDateField(String name, long date){throw new UnsupportedOperationException();}
        
        public String removeField(String name){throw new UnsupportedOperationException();}
        
        public String getContentType(){return getWrapper().getContentType();}
        public void setContentType(String type){throw new UnsupportedOperationException();}
        public int getContentLength(){return getWrapper().getContentLength();}
        public void setContentLength(int len){throw new UnsupportedOperationException();}
        public String getCharacterEncoding(){return getWrapper().getCharacterEncoding();}
        public void setCharacterEncoding(String encoding){throw new UnsupportedOperationException();}
        
        public Object getAttribute(String name){return getWrapper().getAttribute(name);}
        public Object setAttribute(String name, Object attribute)
        {Object old=getAttribute(name);getWrapper().setAttribute(name,attribute);return old;}
        public Enumeration getAttributeNames(){return getWrapper().getAttributeNames();}
        public void removeAttribute(String name){getWrapper().removeAttribute(name);}

        public String toString(){ return "Facade:"+ServletHttpRequest.this.toString();}
    }

    
    /* ------------------------------------------------------------ */
    /** Unwrap a ServletRequest.
     *
     * @see javax.servlet.ServletRequestWrapper
     * @see javax.servlet.http.HttpServletRequestWrapper
     * @param request 
     * @return The core ServletHttpRequest which must be the
     * underlying request object 
     */
    public static ServletHttpRequest unwrap(ServletRequest request)
    {
        while (!(request instanceof ServletHttpRequest))
        {
            if (request instanceof ServletRequestWrapper)
            {
                ServletRequestWrapper wrapper =
                    (ServletRequestWrapper)request;
                request=wrapper.getRequest();
            }
            else
                throw new IllegalArgumentException("Does not wrap ServletHttpRequest");
        }

        return (ServletHttpRequest)request;
    }

}






