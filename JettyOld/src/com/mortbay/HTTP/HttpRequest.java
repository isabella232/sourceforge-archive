// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP;
import com.mortbay.Base.*;
import com.mortbay.Util.*;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.net.*;
import java.util.*;


/* ------------------------------------------------------------------ */
/** Contains a received HTTP request
 * <p> Implements and extends the javax.servlet.http.HttpServletRequest
 * interface.
 * The extensions are for HttpHandler instances that need to modify
 * the request or have better access to the IO.
 *
 * <p><h4>Usage</h4>
 * The method comments present examples based on a request to
 * the URL:
 * <pre>
 * http://localhost:1234/Servlet/Path/Foo/Bar?aaa=123&bbb=456
 * </pre>
 *
 * @see com.mortbay.HTTP.HttpServer
 * @version $Id$
 * @author Greg Wilkins
*/
public class HttpRequest extends HttpHeader
    implements HttpServletRequest
{
    /* -------------------------------------------------------------- */
    public static final String GET="GET";
    public static final String POST="POST";
    public static final String PUT="PUT";
    public static final byte[] Continue=
	"HTTP/1.1 100 Continue\015\012\015\012".getBytes();

    private static final SessionContext sessions =
	new SessionContext();
    
    /* -------------------------------------------------------------- */
    public String method=null;
    public URI uri=null;
    public String version=null;

    /* -------------------------------------------------------------- */
    private Socket connection;
    private HttpInputStream in;
    private InetAddrPort address;

    private Hashtable formParameters=null;
    private Hashtable cookieParameters=null;
    private Cookie[] cookies=null;
    
    private String requestLine=null;
    private String requestURI=null;
    private String protocolHostPort=null;
    private String servletPath=null;
    private String pathInfo=null;
    private String remoteUser=null;
    private String authType=null;
    
    private byte[] byteContent = null;

    private String pathTranslated = null;
    private String serverName = null;
    private int serverPort = 0;
    HttpResponse response=null;
    
    /* -------------------------------------------------------------- */
    /** Construct received request
     * @param connection The socket the request was received over.
     * @param address the IP address that was listened on for the reqest.
     */
    public HttpRequest(Socket connection,
		       InetAddrPort address)
	throws IOException
    {
	this.connection=connection;
	this.in=new HttpInputStream(connection.getInputStream());
	this.address=address;

	// Decode request header
	requestLine=in.readLine();
	if (requestLine==null)
	    throw new IOException("EOF");
	requestLine.trim();
	try{
	    int s1=requestLine.indexOf(' ',0);
	    method = requestLine.substring(0,s1);
	    requestURI = requestLine.substring(s1+1).trim();
	
	    int s2=requestURI.indexOf(' ');
	    if (s2>0)
	    {
		version = requestURI.substring(s2+1).toUpperCase().trim();
		requestURI = requestURI.substring(0,s2);
	    }
	}
	catch(Exception e)
	{
	    Code.ignore(e);
	    if (requestURI==null)
		requestURI="/";
	}

	// handle full URL
	if (!requestURI.startsWith("/"))
	{
	    int slash = requestURI.indexOf("//");
	    if (slash<0)
		slash=0;
	    else
		slash+=2;
	    if (slash<requestURI.length())
	    {
		slash=requestURI.indexOf("/",slash);
		if (slash>0)
		{
		    protocolHostPort=requestURI.substring(0,slash);
		    requestURI=requestURI.substring(slash);
		}
	    }
	}
	if (protocolHostPort==null)
	    protocolHostPort="";

	// Build URI
	uri = new URI(requestURI);
	pathInfo=uri.path();

	// Handle version
	if (version==null || !version.startsWith("HTTP/"))
	    // fix missing or corrupt version
	    version=HttpHeader.HTTP_1_0;
	else if (HTTP_1_1.equals(version))
	{
	    // reset HTTP/1.1 version for faster matching
	    version=HTTP_1_1;
	    // send continue
	    OutputStream out=connection.getOutputStream();
	    out.write(Continue);
	    out.flush();
	}
	
	// Read headers
	super.read(in);

	if (Chunked.equals(getHeader(TransferEncoding)))
	{
	    setHeader(ContentLength,null);
	    this.in.chunking(true);
	}
	
	decodeCookieParameters();

    }

    /* -------------------------------------------------------------- */
    /** Construct request to send
     * @param method The method for this request
     * @param uri The uri for this request.
     */
    public HttpRequest(String method, String uri)
    {
	this.method  = method;
	this.uri = new URI(uri);
	this.requestURI=uri;
	pathInfo=this.uri.path();
	version=HttpHeader.HTTP_1_0;
    }
    
    /* -------------------------------------------------------------- */
    /** Construct request to send
     * @param method The method for this request
     * @param uri The uri for this request.
     */
    public HttpRequest(String method, URI uri)
    {
	this.method  = method;
	this.uri = uri;
	this.requestURI=uri.toString();
	pathInfo=this.uri.path();
	version=HttpHeader.HTTP_1_0;
    }

    /* ------------------------------------------------------------ */
    /** Get associated response 
     * @return response
     */
    public HttpResponse getHttpResponse()
    {
	return response;
    }
    
    /* -------------------------------------------------------------- */
    /** Get the URI path minus any query strings with translations
     * applied.
     * @return For the given example, this would return <PRE>
     * /Servlet/Path/Foo/Bar
     * </PRE>
     */
    public  String getRequestPath()
    {
	if (uri==null)
	    return null;
	return uri.path();
    }
    
    /* -------------------------------------------------------------- */
    /** Return the HTTP request line as it was received
     */
    public  String getRequestLine()
    {
	if (requestLine==null)
	    requestLine=method+" "+protocolHostPort+uri+" "+version;
	return requestLine;
    }

    /* ------------------------------------------------------------ */
    /** Get the protocol version
     * @return return the version.
     */
    public String getVersion()
    {
	return version;
    }
    

    /* ------------------------------------------------------------ */
    /** Set the translated path
     * @param pathTranslated Translated Path.
     */
    public void setPathTranslated(String pathTranslated)
    {
	this.pathTranslated=pathTranslated;
    }
    
    /* -------------------------------------------------------------- */
    /** Set the servlet path
     * getServletPath and getPathInfo are not valid until this
     * has been called.
     */
    public void setServletPath(String servletPath)
	 throws MalformedURLException
    {
	switch (servletPath.charAt(servletPath.length()-1))
	{
	  case '|':
	  case '%':
	  case '$':
	      servletPath=servletPath.substring(0,servletPath.length()-1);
	      break;
	  default:
	      break;
	}
	
	Code.debug("SetServletPath '"+servletPath+
		   "' in " + uri );
					
	this.servletPath=servletPath;
	String path=uri.path();

	if (!path.startsWith(servletPath))
	    throw new MalformedURLException("Bad servletPath '"+
					    servletPath+"' for "+uri);

	pathInfo=path.substring(servletPath.length());
    }

    /* ------------------------------------------------------------ */
    /** Set the request version 
     * @param version 
     */
    public void setVersion(String version)
    {
	this.version=version;
    }
    
    /* -------------------------------------------------------------- */
    /** Translate the URI
     * Apply any translation rules to the URI.
     * If setServletPath has been called, it's results are nulled
     * @param pathSpec The path segment to be translated. This may end
     * with the special characters defined in PathMap.
     * @param newPath The path segment to replace path with.
     */
    public void translateAddress(String pathSpec,String newPath)
    {
	String old = uri.path();
	String match=PathMap.match(pathSpec,old);
	Code.assert(match!=null,"translate non matching address");

	if (pathSpec.endsWith("%") && match.endsWith("/") &&
	    ! newPath.endsWith("/"))
	    newPath += "/";
	
	if (match.length()==old.length())    
	    uri.path(newPath);
	else
	    uri.path(newPath+old.substring(match.length()));
	
	Code.debug("Translated '"+match+
		   "' part of '" + old +
		   "' to '"+newPath+
		   "' resulted with "+uri.path());
	
	servletPath=null;
	pathInfo=uri.path();
    }

    /* -------------------------------------------------------------- */
    /** Set the remoteUser from authentication headers
     */
    public void setRemoteUser(String authType, String name)
    {
	this.authType=authType;
	remoteUser=name;
    }
    

    /* -------------------------------------------------------------- */
    /** decode Form Parameters
     * After this call, form paramters may be fetch via the
     * getParameter() method.
     */
    public void decodeFormParameters()
	 throws IOException
    {
	String contentType = getContentType();
	if (contentType!=null &&
	    contentType.equals(HttpHeader.WwwFormUrlEncode))
	{
	    int contentLength = getContentLength();
	    if (contentLength<0)
		Code.warning("No contentLength for "+
			     HttpHeader.WwwFormUrlEncode);
	    else
		formParameters =
		    javax.servlet.http.HttpUtils
		    .parsePostData(contentLength,getInputStream());
	}
    }
    
    /* -------------------------------------------------------------- */
    /** decode Form Parameters
     * After this call, form paramters may be fetch via the
     * getParameter() method.
     */
    public void decodeCookieParameters()
	 throws IOException
    {
	if (cookieParameters==null)
	{
	    cookieParameters=new Hashtable();
	    String c = getHeader(HttpHeader.Cookie);
	    if (c!=null)
		cookies=Cookies.decode(c,cookieParameters);
	}
    }
    
    
    /* -------------------------------------------------------------- */
    /** Write the request header to an output stream
     */ 
    public void write(OutputStream out)
	throws IOException
    {
	out.write(method.getBytes());
	out.write(' ');
	out.write(uri.toString().getBytes());
	out.write(' ');
	out.write(version.getBytes());
	out.write(__CRLF);
	super.write(out);
	out.flush();
    }

    /* -------------------------------------------------------------- */
    /** Put a parameter into the request.
     * Placed in uri query parameters for forwarded requests
     * @param deprecated Use putParameterValues
     */
    public void putParameter(String name, String value)
    {
	uri.put(name,value);
    }
    
    /* -------------------------------------------------------------- */
    /** Put a multi-valued parameter into the request.
     * Placed in uri query parameters for forwarded requests
     */
    public void putParameterValues(String name, String[] values)
    {
	uri.put(name,values);
    }
    
    /* -------------------------------------------------------------- */
    /* - SERVLET METHODS -------------------------------------------- */
    /* -------------------------------------------------------------- */

    /* -------------------------------------------------------------- */
    /** Get the length of any content in this request or -1 if not known.
     * @return For the given example, this would return <PRE>
     * -1
     * </PRE>
     */
    public  int getContentLength()
    {
	return getIntHeader(ContentLength);
    }

    /* -------------------------------------------------------------- */
    /** Return the MIME encoding type of any (form) content in this request
     * or null if not known.
     * @return For the given example, this would return <PRE>
     * null
     * </PRE>
     */
    public String getContentType()
    {
	return getHeaderNoParams(ContentType);
    }

   
    /* -------------------------------------------------------------- */
    /** Get the actual protocol and version.
     * Get protocol and version used in the request as a string of the
     * form &lt;protocol&gt;/&lt;major version&gt;.&lt;minor version&gt.
     * @return For the given example, this would return <PRE>
     * HTTP/1.0
     * </PRE>
     */
    public  String getProtocol()
    {
	return version;
    }
    
    /* -------------------------------------------------------------- */
    /** Get the host name of the server that received the request.
     * @return For the given example, this would return <PRE>
     * localhost
     * </PRE>
     */
    public String getServerName()
    {	
	if (serverName==null)
	{
	    serverName=getHeader(Host);
	    if (serverName!=null && serverName.length()>0)
	    {
		int colon=serverName.indexOf(':');
		if (colon>=0)
		{
		    if (colon<serverName.length())
		    {
			try{
			    serverPort=Integer
				.parseInt(serverName.substring(colon+1));
			}
			catch(Exception e)
			{
			    Code.ignore(e);
			    serverPort=80;
			}
		    }
		    serverName=serverName.substring(0,colon);
		}
		else
		    serverPort=80;
	    }
	    else if (address.inetAddress!=null)
		serverName = address.inetAddress.getHostName();
	    
	    if (serverName==null)
	    {
		try {serverName=InetAddress.getLocalHost().getHostName();}
		catch(java.net.UnknownHostException ignore){
		}
	    }
	    
	    int slash = serverName.indexOf("/");
	    if (slash>=0)
		serverName=serverName.substring(slash+1);
	}
	
	return serverName;
    }
    
    /* -------------------------------------------------------------- */
    /** Get the port number used in the request
     * @return For the given example, this would return <PRE>
     * 1234
     * </PRE>
     */
    public int getServerPort()
    {
	if (serverName==null)
	    getServerName();

	if (serverPort==0)
	    serverPort=address.port;
	
	return serverPort;
    }
    
    /* -------------------------------------------------------------- */
    /** Get the remote IP address of the system that sent the request.
     * @return For the given example, this would return <PRE>
     * 127.0.0.1
     * </PRE>
     */
    public  String getRemoteAddr()
    {
	String remoteAddr=connection.getInetAddress().toString();
	int slash = remoteAddr.indexOf("/");
	if (slash>=0)
	    remoteAddr=remoteAddr.substring(slash+1);
	return remoteAddr;
    }
    
    /* -------------------------------------------------------------- */
    /** Get the hostname of the system that sent the request.
     * @return For the given example, this would return <PRE>
     * localhost
     * </PRE>
     */
    public  String getRemoteHost()
    {
	String remoteHost=null;

	if (connection!=null)
	    remoteHost = connection.getInetAddress().getHostName();
	
	return remoteHost;
    }

    /** -------------------------------------------------------------- */
    /** Applies alias rules to the specified virtual path and returns the
     * corresponding real path. getRealPath("/") is the document root.
     * It returns null if the translation cannot be performed.
     * @param path the path to be translated
     */  
    public String getRealPath(String path)
    {
	// XXX
	return null;
    }

    /* -------------------------------------------------------------- */
    /** Get request input stream
     * @return an input stream for reading the request body.
     */
    public ServletInputStream getInputStream()
    {
	return in;
    }


    /* -------------------------------------------------------------- */
    /**
     * Returns the value of the specified parameter for the request. For
     * example, in an HTTP servlet this would return the value of the
     * specified query string parameter.
     * @param name the parameter name
     * @return For the given example, getParamter("aaa") would
     * return "123".
     * @deprecated use getParameterValues
     */
    public String getParameter(String name)
    {
	Object value = uri.get(name);
	
	if (value==null && formParameters!=null)
	    value = formParameters.get(name);
	if (value==null && cookieParameters!=null)
	    value = cookieParameters.get(name);

	if (value!=null)
	{
	    if (value instanceof String[])
	    {
		String[] a = (String[]) value;
		switch(a.length)
		{
		  case 0:
		      return null;
		  case 1:
		      return a[0];
		  default:
		      {
			  StringBuffer buf = new StringBuffer();
			  for (int i=0;i<a.length;i++)
			  {
			      if (i!=0)
				  buf.append(',');
			      buf.append(a[i]);
			  }
			  return buf.toString();
		      }
		}
	    }
	    
	    return value.toString();
	}
	return null;
    }
    
    /* -------------------------------------------------------------- */
    /**
     * Returns the multi-values of the specified parameter for the request.
     */
    public String[] getParameterValues(String name)
    {
	Object values=uri.getValues(name);
	if (values==null && formParameters!=null)
	    values = formParameters.get(name);
	if (values==null && cookieParameters!=null)
	    values = cookieParameters.get(name);
	
	if (values!=null && !(values instanceof String[]))
	{
	    String[] a = new String[1];
	    a[0]=values.toString();
	    return a;
	}
	
	return (String[])values;
    }

    /* -------------------------------------------------------------- */
    /**
     * Returns an enumeration of strings representing the parameter names
     * for this request.
     */
    public Enumeration getParameterNames()
    {
	if (formParameters==null && cookieParameters==null)
	    return uri.parameters().keys();

	Vector names = new Vector();
	Enumeration e = uri.parameters().keys();
	while (e.hasMoreElements())
	    names.addElement(e.nextElement());

	if (formParameters!=null)
	{
	    e = formParameters.keys();
	    while (e.hasMoreElements())
		names.addElement(e.nextElement());
	}
	
	if (cookieParameters!=null)
	{
	    e = cookieParameters.keys();
	    while (e.hasMoreElements())
		names.addElement(e.nextElement());
	}
	
	return names.elements();
    }
    
    /* -------------------------------------------------------------- */
    /**
     * Returns an attribute of the request given the specified key name.
     * This allows access to request information not already provided by
     * the other methods in this interface. Key names beginning with
     * 'COM.sun.*' are reserved.
     * @param name the attribute name
     * @return the value of the attribute, or null if not defined
     */
    public Object getAttribute(String name)
    {
	return null;
    }
    
    /* -------------------------------------------------------------- */
    /** Returns the scheme of the URL used in this request, for
     * example "http", "https"
     */
    public String getScheme()
    {
	return "http";
    }


    
    /* -------------------------------------------------------------- */
    /* - HTTPSERVLET METHODS ---------------------------------------- */
    /* -------------------------------------------------------------- */

    
    /* -------------------------------------------------------------- */
    /** Returns the character set encoding for the input of this request.
     * Checks the Content-Type header for a charset parameter and return its
     * value if found or ISO-8859-1 otherwise.
     * @return Character Encoding.
     */
    public String getCharacterEncoding ()
    {
	String encoding = getHeader(ContentType);
	if (encoding==null || encoding.length()==0)
	    return "ISO-8859-1";
	
	int i=encoding.indexOf(';');
	if (i<0)
	    return "ISO-8859-1";
	
	i=encoding.indexOf("charset=",i);
	if (i<0 || i+8>=encoding.length())
	    return "ISO-8859-1";
	    
	encoding=encoding.substring(i+8);
	i=encoding.indexOf(' ');
	if (i>0)
	    encoding=encoding.substring(0,i);
	    
	return encoding;
    }
    
    /* -------------------------------------------------------------- */
    /** Get the HTTP method for this request.
     * Returns the method with which the request was made. The returned
     * value can be "GET", "HEAD", "POST", or an extension method. Same
     * as the CGI variable REQUEST_METHOD.
     * @return For the given example, this would return <PRE>
     * GET
     * </PRE>
     */
    public  String getMethod()
    {
	return method;
    }
    
    /* -------------------------------------------------------------- */
    /** Get the full URI.
     * @return For the given example, this would return <PRE>
     * /Servlet/Path/Foo/Bar?aaa=123&bbb=456
     * </PRE>
     */
    public  String getRequestURI()
    {
	return requestURI;
    }


    /* -------------------------------------------------------------- */
    /** Get the URI sub path that matched to the servlet.
     * @return For the given example, this would return <PRE>
     * /Servlet/Path
     * </PRE>
     */
    public  String getServletPath()
    {
	return servletPath;
    }
    

    /* -------------------------------------------------------------- */
    /**
     * Get the URI segment that is more than the servlet path
     * @return For the given example, this would return <PRE>
     * /Foo/Bar
     * </PRE>
     */
    public  String getPathInfo()
    {
	return pathInfo;
    }

    /* -------------------------------------------------------------- */
    /** Return the actual URI after translation.
     * Not implemented
     * @return For the given example, this would return <PRE>
     * null
     * </PRE>
     */
    public  String getPathTranslated()
    {
	if (pathTranslated==null)
	    return uri.path();
	return pathTranslated;
    }

    /* -------------------------------------------------------------- */
    /** Get the quesry string of the request
     * @return For the given example, this would return <PRE>
     * aaa=123&bbb=456
     * </PRE>
     */
    public  String getQueryString()
    {
	return uri.query();
    }

    /* -------------------------------------------------------------- */
    /** Get the remote user name decoded from authenticatin headers
     * @return For the given example, this would return <PRE>
     * null
     * </PRE>
     */
    public  String getRemoteUser()
    {
	return remoteUser;
    }

    /* -------------------------------------------------------------- */
    /** Get the authentication method (normally BASIC)
     * @return For the given example, this would return <PRE>
     * null
     * </PRE>
     */
    public  String getAuthType()
    {
	return authType;
    }   


    /* -------------------------------------------------------------- */
    public javax.servlet.http.Cookie getCookies()[]
    {
	return cookies;
    }
    
    /* -------------------------------------------------------------- */
    public String getRequestedSessionId()
    {
	return (String)cookieParameters.get(SessionContext.SessionId);
    }
    
    /* -------------------------------------------------------------- */
    public HttpSession getSession(boolean create)
    {
	HttpSession session=null;
	String id = getRequestedSessionId();
	boolean old=false;
	
	String browserId =
	    (String)cookieParameters.get(SessionContext.BrowserId);
	
	if (id!=null)
	{
	    session = sessions.getSession(id);
	    if(session==null && create && browserId!=null)
		session=sessions.oldSession(id,browserId);
	}
	
	if (session==null && create)
	{
	    session=sessions.newSession();
	    Cookie cookie =
		new Cookie(SessionContext.SessionId,(String)
			   session.getValue(SessionContext.SessionId));
	    cookie.setPath("/");
	    response.addCookie(cookie);
	
	    if (browserId==null)
	    {
		cookie =
		    new Cookie(SessionContext.BrowserId,(String)
			       session.getValue(SessionContext.BrowserId));
		cookie.setPath("/");
		cookie.setMaxAge(SessionContext.distantFuture);
		response.addCookie(cookie);
	    }
	    else
		session.putValue(SessionContext.BrowserId,browserId);
	}

	return session;
    }
    
    /* -------------------------------------------------------------- */
    public boolean isRequestedSessionIdFromCookie()
    {
	return getRequestedSessionId()!=null;
    }
    
    /* -------------------------------------------------------------- */
    public boolean isRequestedSessionIdFromUrl()
    {
	return false;
    }
    
    /* -------------------------------------------------------------- */
    public boolean isRequestedSessionIdValid()
    {
	return getSession(false)!=null;
    }
    
    /* -------------------------------------------------------------- */
    public BufferedReader getReader()
    {
	Code.notImplemented();
	return null;
    }
}

	   

