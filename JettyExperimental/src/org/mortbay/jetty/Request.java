//========================================================================
//$Id$
//Copyright 2004 Mort Bay Consulting Pty. Ltd.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.mortbay.jetty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URI;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.LoggerFactory;
import org.slf4j.ULogger;
import org.mortbay.io.Buffer;
import org.mortbay.io.BufferUtil;
import org.mortbay.io.EndPoint;
import org.mortbay.io.IO;
import org.mortbay.io.Portable;
import org.mortbay.resource.MimeTypes;
import org.mortbay.util.ByteArrayOutputStream2;
import org.mortbay.util.LazyList;
import org.mortbay.util.LogSupport;
import org.mortbay.util.MultiMap;
import org.mortbay.util.StringUtil;
import org.mortbay.util.URIUtil;
import org.mortbay.util.UrlEncoded;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.ContextHandler.Context;

/* ------------------------------------------------------------ */
/** Request.
 * @author gregw
 *
 */
public class Request implements HttpServletRequest
{
    private static final Collection __defaultLocale = Collections.singleton(Locale.getDefault());
    private static ULogger log = LoggerFactory.getLogger(HttpConnection.class);
    private static final int NONE=0, STREAM=1, READER=2;
    
    private HttpConnection _connection;
    private EndPoint _endp;
    
    private Map _attributes;
    private String _authType;
    private String _characterEncoding;
    private Cookie[] _cookies;
    private String _serverName;
    private String _method;
    private String _pathInfo;
    private int _port;
    private String _protocol=HttpVersions.HTTP_1_1;
    private String _queryString;
    private String _requestedSessionId;
    private String _requestURI;
    private String _scheme=URIUtil.HTTP;
    private String _servletPath;
    private URI _uri;
    private Principal _userPrincipal;
    private MultiMap _parameters;
    private boolean _paramsExtracted;
    private int _inputState;
    private BufferedReader _reader;
    private boolean _dns=false;
    private ContextHandler.Context _context;
    
    /* ------------------------------------------------------------ */
    /**
     * 
     */
    Request(HttpConnection connection)
    {
        _connection=connection;
        _endp=connection.getEndPoint();
        _dns=_connection.useDNS();
    }

    void recycle()
    {
        if(_attributes!=null)
            _attributes.clear();
        _authType=null;
        _characterEncoding=null;
        _context=null;
        _cookies=null;
        _serverName=null;
        _method=null;
        _pathInfo=null;
        _port=0;
        _protocol=HttpVersions.HTTP_1_1;
        _queryString=null;
        _requestedSessionId=null;
        _requestURI=null;
        _scheme=URIUtil.HTTP;
        _servletPath=null;
        _uri=null;
        _userPrincipal=null;
        if (_parameters!=null)
            _parameters.clear();
        _paramsExtracted=false;;
        _inputState=NONE;
        _reader=null; 
    }
    
    
    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#getAttribute(java.lang.String)
     */
    public Object getAttribute(String name)
    {
        if (_attributes==null)
            return null;
        return _attributes.get(name);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#getAttributeNames()
     */
    public Enumeration getAttributeNames()
    {
        if (_attributes==null)
            return Collections.enumeration(Collections.EMPTY_LIST);
        return Collections.enumeration(_attributes.keySet());
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletRequest#getAuthType()
     */
    public String getAuthType()
    {
        return _authType;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#getCharacterEncoding()
     */
    public String getCharacterEncoding()
    {
        return _characterEncoding;
    }
    

    /* ------------------------------------------------------------ */
    public Object getContent()
        throws IOException
    {
        // TODO 
        return null;
    }

    /* ------------------------------------------------------------ */
    public Object getContentAs(Object type)
    	throws IOException
    {
        // TODO 
        return null;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#getContentLength()
     */
    public int getContentLength()
    {
        return (int)_connection.getRequestFields().getLongField(HttpHeaders.CONTENT_LENGTH_BUFFER);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#getContentType()
     */
    public String getContentType()
    {
        return _connection.getRequestFields().getStringField(HttpHeaders.CONTENT_TYPE_BUFFER);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletRequest#getContextPath()
     */
    public String getContextPath()
    {
        if (_context==null)
            return "";
        return _context.getContextPath();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletRequest#getCookies()
     */
    public Cookie[] getCookies()
    {
        return _cookies;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletRequest#getDateHeader(java.lang.String)
     */
    public long getDateHeader(String name)
    {
        return _connection.getRequestFields().getDateField(name);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletRequest#getHeader(java.lang.String)
     */
    public String getHeader(String name)
    {
        return _connection.getRequestFields().getStringField(name);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletRequest#getHeaderNames()
     */
    public Enumeration getHeaderNames()
    {
        return _connection.getRequestFields().getFieldNames();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletRequest#getHeaders(java.lang.String)
     */
    public Enumeration getHeaders(String name)
    {
        return _connection.getRequestFields().getValues(name);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#getInputStream()
     */
    public ServletInputStream getInputStream() throws IOException
    {
        if (_inputState!=NONE && _inputState!=STREAM)
            throw new IllegalStateException("READER");
        _inputState=STREAM;
        return _connection.getInputStream();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletRequest#getIntHeader(java.lang.String)
     */
    public int getIntHeader(String name)
    {
        return (int)_connection.getRequestFields().getLongField(name);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#getLocalAddr()
     */
    public String getLocalAddr()
    {
        return _endp==null?null:_endp.getLocalAddr();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#getLocale()
     */
    public Locale getLocale()
    {
        Enumeration enm = _connection.getRequestFields().getValues(HttpHeaders.ACCEPT_LANGUAGE, HttpFields.__separators);
        
        // handle no locale
        if (enm == null || !enm.hasMoreElements())
            return Locale.getDefault();
        
        // sort the list in quality order
        List acceptLanguage = HttpFields.qualityList(enm);
        if (acceptLanguage.size()==0)
            return  Locale.getDefault();
        
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
    /* 
     * @see javax.servlet.ServletRequest#getLocales()
     */
    public Enumeration getLocales()
    {

        Enumeration enm = _connection.getRequestFields().getValues(HttpHeaders.ACCEPT_LANGUAGE, HttpFields.__separators);
        
        // handle no locale
        if (enm == null || !enm.hasMoreElements())
            return Collections.enumeration(__defaultLocale);
        
        // sort the list in quality order
        List acceptLanguage = HttpFields.qualityList(enm);
        
        if (acceptLanguage.size()==0)
            return
            Collections.enumeration(__defaultLocale);
        
        Object langs = null;
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
            langs=LazyList.ensureSize(langs,size);
            langs=LazyList.add(langs,new Locale(language,country));
        }
        
        if (LazyList.size(langs)==0)
            return Collections.enumeration(__defaultLocale);
        
        return Collections.enumeration(LazyList.getList(langs));
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#getLocalName()
     */
    public String getLocalName()
    {
        if (_dns)
            return _endp==null?null:_endp.getLocalHost();
        return _endp==null?null:_endp.getLocalAddr();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#getLocalPort()
     */
    public int getLocalPort()
    {
        return _endp==null?0:_endp.getLocalPort();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletRequest#getMethod()
     */
    public String getMethod()
    {
        return _method;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#getParameter(java.lang.String)
     */
    public String getParameter(String name)
    {
        if (!_paramsExtracted) extractParameters();
        return (String) _parameters.getValue(name, 0);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#getParameterMap()
     */
    public Map getParameterMap()
    {
        if (!_paramsExtracted) extractParameters();
        return _parameters.toStringArrayMap();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#getParameterNames()
     */
    public Enumeration getParameterNames()
    {
        if (!_paramsExtracted) extractParameters();
        return Collections.enumeration(_parameters.keySet());
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#getParameterValues(java.lang.String)
     */
    public String[] getParameterValues(String name)
    {
        if (!_paramsExtracted) extractParameters();
        List vals = _parameters.getValues(name);
        if (vals==null)
            return new String[0];
        return (String[])vals.toArray(new String[vals.size()]);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletRequest#getPathInfo()
     */
    public String getPathInfo()
    {
        return _pathInfo;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletRequest#getPathTranslated()
     */
    public String getPathTranslated()
    {
        if (_pathInfo==null || _context==null)
            return null;
        return _context.getRealPath(_pathInfo);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#getProtocol()
     */
    public String getProtocol()
    {
        return _protocol;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletRequest#getQueryString()
     */
    public String getQueryString()
    {
        return _queryString;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#getReader()
     */
    public BufferedReader getReader() throws IOException
    {
        if (_inputState!=NONE && _inputState!=READER)
            throw new IllegalStateException("STREAMED");
        if (_reader==null)
        {
            String encoding=getCharacterEncoding();
            if (encoding==null)
                encoding=StringUtil.__ISO_8859_1;
            _reader=new BufferedReader(new InputStreamReader(getInputStream(),encoding));
            
        }
        _inputState=READER;
        return _reader;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#getRealPath(java.lang.String)
     */
    public String getRealPath(String path)
    {
        if (_context==null)
            return null;
        return _context.getRealPath(path);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#getRemoteAddr()
     */
    public String getRemoteAddr()
    {
        return _endp==null?null:_endp.getRemoteAddr();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#getRemoteHost()
     */
    public String getRemoteHost()
    {
        if (_dns)
            return _endp==null?null:_endp.getRemoteHost();
        return _endp==null?null:_endp.getRemoteAddr();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#getRemotePort()
     */
    public int getRemotePort()
    {
        return _endp==null?0:_endp.getRemotePort();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletRequest#getRemoteUser()
     */
    public String getRemoteUser()
    {
        return _userPrincipal.toString();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#getRequestDispatcher(java.lang.String)
     */
    public RequestDispatcher getRequestDispatcher(String path)
    {
        if (path == null || _context==null)
            return null;

        // handle relative path
        if (!path.startsWith("/"))
        {
            String relTo=URIUtil.addPaths(_servletPath,_pathInfo);
            int slash=relTo.lastIndexOf("/");
            if (slash>1)
                relTo=relTo.substring(0,slash+1);
            else
                relTo="/";
            path=URIUtil.addPaths(relTo,path);
        }
    
        return _context.getRequestDispatcher(path);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletRequest#getRequestedSessionId()
     */
    public String getRequestedSessionId()
    {
        return _requestedSessionId;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletRequest#getRequestURI()
     */
    public String getRequestURI()
    {
        if (_requestURI==null)
            _requestURI=_uri.getRawPath();
        return _requestURI;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletRequest#getRequestURL()
     */
    public StringBuffer getRequestURL()
    {
        StringBuffer url = new StringBuffer(48);
        synchronized (url)
        {
            String scheme = getScheme();
            int port = getServerPort();

            url.append(scheme);
            url.append("://");
            url.append(getServerName());
            if (_port>0 && 
                ((scheme.equalsIgnoreCase(URIUtil.HTTP) && port != 80) || 
                 (scheme.equalsIgnoreCase(URIUtil.HTTPS) && port != 443)))
            {
                url.append(':');
                url.append(_port);
            }
            
            url.append(getRequestURI());
            return url;
        }
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#getScheme()
     */
    public String getScheme()
    {
        return _scheme;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#getServerName()
     */
    public String getServerName()
    {        // Return already determined host
        if (_serverName != null) return _serverName;

        // Return host from absolute URI
        _serverName = _uri.getHost();
        _port = _uri.getPort();
        if (_serverName != null) return _serverName;

        // Return host from header field
        Buffer hostPort = _connection.getRequestFields().get(HttpHeaders.HOST_BUFFER);
        if (hostPort!=null)
        {
            for (int i=hostPort.length();i-->0;)   
            {
                if (hostPort.peek(hostPort.getIndex()+i)==':')
                {
                    _serverName=hostPort.peek(hostPort.getIndex(), i).toString();
                    _port=BufferUtil.toInt(hostPort.peek(i+1, hostPort.length()-i-1));
                }
            }
            if (_serverName==null || _port<0)
            {
                _serverName = hostPort.toString();
                _port = 0;
            }
            
            return _serverName;
        }

        // Return host from connection
        if (_connection != null)
        {
            _serverName = getLocalName();
            _port = getLocalPort();
            if (_serverName != null && !Portable.ALL_INTERFACES.equals(_serverName)) 
                return _serverName;
        }

        // Return the local host
        try
        {
            _serverName = InetAddress.getLocalHost().getHostAddress();
        }
        catch (java.net.UnknownHostException e)
        {
            LogSupport.ignore(log, e);
        }
        return _serverName;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#getServerPort()
     */
    public int getServerPort()
    {
        if (_port<=0)
        {
            if (_serverName==null)
                getServerName();
        
            if (_port<=0 && _serverName!=null && _uri!=null && _uri.isAbsolute())
                _port = _uri.getPort();
            else 
                _port = _endp==null?0:_endp.getLocalPort();
        }
        
        if (_port<=0)
        {
            if (getScheme().equalsIgnoreCase(URIUtil.HTTPS))
                return 443;
            return 80;
        }
        return _port;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletRequest#getServletPath()
     */
    public String getServletPath()
    {
        if (_servletPath==null)
            _servletPath="";
        return _servletPath;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletRequest#getSession()
     */
    public HttpSession getSession()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletRequest#getSession(boolean)
     */
    public HttpSession getSession(boolean create)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletRequest#getUserPrincipal()
     */
    public Principal getUserPrincipal()
    {
        return _userPrincipal;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromCookie()
     */
    public boolean isRequestedSessionIdFromCookie()
    {
        // TODO Auto-generated method stub
        return false;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromUrl()
     */
    public boolean isRequestedSessionIdFromUrl()
    {
        // TODO Auto-generated method stub
        return false;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromURL()
     */
    public boolean isRequestedSessionIdFromURL()
    {
        // TODO Auto-generated method stub
        return false;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdValid()
     */
    public boolean isRequestedSessionIdValid()
    {
        // TODO Auto-generated method stub
        return false;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#isSecure()
     */
    public boolean isSecure()
    {
        return _connection.isConfidential();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletRequest#isUserInRole(java.lang.String)
     */
    public boolean isUserInRole(String role)
    {
        // TODO lookup user role link in context!!!
        
        // TODO Auto-generated method stub
        return false;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#removeAttribute(java.lang.String)
     */
    public void removeAttribute(String name)
    {
        if (_attributes!=null)
            _attributes.remove(name);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#setAttribute(java.lang.String, java.lang.Object)
     */
    public void setAttribute(String name, Object attribute)
    {
        if (_attributes==null)
            _attributes=new HashMap(11);
        _attributes.put(name,attribute);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletRequest#setCharacterEncoding(java.lang.String)
     */
    public void setCharacterEncoding(String encoding) throws UnsupportedEncodingException
    {
        if (_inputState!=NONE) 
            return;
        _characterEncoding=encoding;
    }
    

    /* ------------------------------------------------------------ */
    /*
     * Extract Paramters from query string and/or form _content.
     */
    private void extractParameters()
    {
        if (_paramsExtracted) return;
        _paramsExtracted = true;

        if (_parameters == null) _parameters = new MultiMap(16);

        // Handle query string
        String encoding = getCharacterEncoding();
        if (encoding == null)
        {
            // No encoding, so use the existing characters.
            encoding = StringUtil.__ISO_8859_1;
            if (_uri!=null && _uri.getQuery()!=null)
                UrlEncoded.decodeTo(_uri.getQuery(), _parameters);
        }
        else if (_uri!=null && _uri.getRawQuery()!=null)
        {
            UrlEncoded.decodeTo(_uri.getRawQuery(), _parameters, encoding);
        }
        

        // handle any _content.
        String content_type = getContentType();
        if (content_type != null && content_type.length() > 0)
        {
            content_type = HttpFields.valueParameters(content_type, null);
            
            if (MimeTypes.FORM_ENCODED.equalsIgnoreCase(content_type) && HttpMethods.POST.equals(getMethod()))
            {
                int content_length = getContentLength();
                if (content_length <= 0)
                    log.debug("No form _content");
                else
                {
                    try
                    {
                        // TODO limit size
                        // TODO use getContentAs API.
                        
                        // Read the _content
                        ByteArrayOutputStream2 bout = new ByteArrayOutputStream2(content_length);
                        InputStream in = getInputStream();
                        
                        // Copy to a byte array.
                        // TODO - this is very inefficient and we could
                        // save lots of memory by streaming this!!!!
                        IO.copy(in, bout, content_length);
                        
                        
                        // Add form params to query params
                        UrlEncoded.decodeTo(bout.getBuf(), 0, bout.getCount(), _parameters,
                                encoding);
                    }
                    catch (IOException e)
                    {
                        if (log.isDebugEnabled())
                            log.warn(LogSupport.EXCEPTION, e);
                        else
                            log.warn(e.toString());
                    }
                }
            }
        }
    }
    
    
    /* ------------------------------------------------------------ */
    /**
     * @param host The host to set.
     */
    public void setServerName(String host)
    {
        _serverName = host;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the uri.
     */
    public URI getUri()
    {
        return _uri;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param uri The uri to set.
     */
    public void setUri(URI uri)
    {
        _uri = uri;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the connection.
     */
    public HttpConnection getConnection()
    {
        return _connection;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return Returns the inputState.
     */
    public int getInputState()
    {
        return _inputState;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param authType The authType to set.
     */
    public void setAuthType(String authType)
    {
        _authType = authType;
    }
    
    
    /* ------------------------------------------------------------ */
    /**
     * @param cookies The cookies to set.
     */
    public void setCookies(Cookie[] cookies)
    {
        _cookies = cookies;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param method The method to set.
     */
    public void setMethod(String method)
    {
        _method = method;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param pathInfo The pathInfo to set.
     */
    public void setPathInfo(String pathInfo)
    {
        _pathInfo = pathInfo;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param protocol The protocol to set.
     */
    public void setProtocol(String protocol)
    {
        _protocol = protocol;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param requestedSessionId The requestedSessionId to set.
     */
    public void setRequestedSessionId(String requestedSessionId)
    {
        _requestedSessionId = requestedSessionId;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param scheme The scheme to set.
     */
    public void setScheme(String scheme)
    {
        _scheme = scheme;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @param queryString The queryString to set.
     */
    public void setQueryString(String queryString)
    {
        _queryString = queryString;
    }
    /* ------------------------------------------------------------ */
    /**
     * @param requestURI The requestURI to set.
     */
    public void setRequestURI(String requestURI)
    {
        _requestURI = requestURI;
    }
    /* ------------------------------------------------------------ */
    /**
     * @param servletPath The servletPath to set.
     */
    public void setServletPath(String servletPath)
    {
        _servletPath = servletPath;
    }
    /* ------------------------------------------------------------ */
    /**
     * @param userPrincipal The userPrincipal to set.
     */
    public void setUserPrincipal(Principal userPrincipal)
    {
        _userPrincipal = userPrincipal;
    }

    /* ------------------------------------------------------------ */
    /**
     * @param context
     */
    public void setContext(Context context)
    {
        _context=context;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return
     */
    public Context getContext()
    {
        return _context;
    }
}

