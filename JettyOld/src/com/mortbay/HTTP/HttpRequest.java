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
/** A received HTTP request.
 * Implements and extends the javax.servlet.http.HttpServletRequest
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
    public static final String HEAD="HEAD";
    public static final String DELETE="DELETE";
    public static final String MOVE="MOVE";
    public static final String OPTIONS="OPTIONS";
    public static final byte[] Continue=
        "HTTP/1.1 100 Continue\015\012\015\012".getBytes();

    private static final SessionContext sessions =
        new SessionContext();
    private static final Enumeration __NoAttributes = new Vector().elements();

    /* -------------------------------------------------------------- */
    /** For decoding session ids etc. */
    public static final String SESSIONID_NOT_CHECKED = "not checked";
    public static final String SESSIONID_URL = "url";
    public static final String SESSIONID_COOKIE = "cookie";
    public static final String SESSIONID_NONE = "none";
    
    /* ------------------------------------------------------------ */

    private String method=null;
    private URI uri=null;
    private String version=null;

    /* -------------------------------------------------------------- */

    private boolean localRequest=false;
    private HttpServer httpServer = null;
    private Socket connection;
    private HttpInputStream in;
    private InetAddrPort address;

    private UrlEncoded formParameters=null;
    private Hashtable cookieParameters=null;
    private Hashtable attributes=null;
    private Cookie[] cookies=null;
    private String sessionId=null;
    private HttpSession session=null;
    private String sessionIdState=SESSIONID_NOT_CHECKED;

    private String requestLine=null;
    private String protocolHostPort=null;
    private String resourcePath=null;
    private String servletPath=null;
    private String pathInfo=null;
    private String remoteUser=null;
    private String authType=null;
    
    private byte[] byteContent = null;

    private String pathTranslated = null;
    private String serverName = null;
    private int serverPort = 0;
    private HttpResponse response=null;
    private BufferedReader reader=null;
    private int inputState=0;

    private Dictionary redirectParams = null;
    
    /* -------------------------------------------------------------- */
    /** Construct received request.
     * @param httpServer The server for this request.
     * @param connection The socket the request was received over.
     * @param address the IP address that was listened on for the request.
     * @exception IOException Problem reading the request header
     */
    public HttpRequest(HttpServer httpServer,
                       Socket connection,
                       InetAddrPort address)
        throws IOException
    {
        this.httpServer=httpServer;
        this.connection=connection;
        this.in=new HttpInputStream(connection.getInputStream());
        this.address=address;

        // Get and Decode request header
        com.mortbay.HTTP.HttpInputStream$CharBuffer cb = null;
        do
        {
            cb = in.readCharBufferLine();
        }
        while(cb!=null && cb.size==0);
        
        if (cb==null)
            throw new IOException("EOF");
        decodeRequestLine(cb.chars,cb.size);
        
        // Build URI
        pathInfo=uri.getPath();

        // Handle version
        if (HTTP_1_1.equals(version))
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
        else 
        {
            int content_length=getContentLength();
            if (content_length>=0)
                in.setContentLength(content_length);
        }
    }

    /* -------------------------------------------------------------- */
    /** Construct request to send
     * @param httpServer The server for this request.
     * @param method The method for this request
     * @param uri The uri for this request.
     */
    public HttpRequest(HttpServer server, String method, String uri)
    {
        this.httpServer=server;
        this.method  = method;
        this.uri = new URI(uri);
        pathInfo=this.uri.getPath();
        version=HttpHeader.HTTP_1_0;
        protocolHostPort="";
        localRequest=true;
    }
    
    /* -------------------------------------------------------------- */
    /** Construct request to send
     * @param httpServer The server for this request.
     * @param method The method for this request
     * @param uri The uri for this request.
     */
    public HttpRequest(HttpServer server, String method, URI uri)
    {
        this.httpServer=server;
        this.method  = method;
        this.uri = uri;
        pathInfo=this.uri.getPath();
        version=HttpHeader.HTTP_1_0;
        protocolHostPort="";
        localRequest=true;
    }

    /* ------------------------------------------------------------ */
    /** Get local request status
     * @return true if this is a synthetic local request
     */
    public boolean isLocalRequest()
    {
        return localRequest;
    }
    
    /* ------------------------------------------------------------ */
    /** Set associated response 
     */
    public void setHttpResponse(HttpResponse response)
    {
        this.response=response;
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
    public String getRequestPath()
    {
        if (uri==null)
            return null;
        return uri.getPath();
    }
    
    
    /* -------------------------------------------------------------- */
    /** Set the URI path 
     */
    public void setRequestPath(String path)
    {
        if (uri!=null)
            uri.setPath(path);
        servletPath=null;
        pathInfo=path;
    }

    /* -------------------------------------------------------------- */
    /** Set the URI path and redirect params
     */
    public void setRequestPath(String path, Dictionary params)
    {
        if (uri!=null && path!=null)
            uri.setPath(path);
        servletPath=null;
        pathInfo=path;

	redirectParams=params;
    }
    
    /* ------------------------------------------------------------ */
    /** Get the resource path.
     * If set, the resource path is the path used by Jetty Handlers
     * to locate the resource that will handle the request. If not
     * set, the RequestPath is returned.  A resource path is required
     * to implement the RequestDispatcher.include method, which leaves
     * the requestPath unmodified when calling a new resource for content.
     * HttpHandlers should use the resource path to determine which
     * entity should handle the request. The RequestPath should be used
     * for any actually handling.  Note that this just shows how much
     * the RequestDispatcher API sux and is not really generic.
     * @return the resource path or the request path.
     */
    public String getResourcePath()
    {
        if (resourcePath != null)
            return resourcePath;
        return getRequestPath();
    }
    
    /* ------------------------------------------------------------ */
    /** Set the resource path.
     * The resource path is the path used by Jetty Handlers
     * to locate the resource that will handle the request.
     * A resource path is required
     * to implement the RequestDispatcher.include method, which leaves
     * the requestPath unmodified when calling a new resource for content.
     * @param path 
     */
    public void setResourcePath(String path, Dictionary params)
    {
        resourcePath=path;
	redirectParams=params;
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
        String path=getResourcePath();
        
        switch (servletPath.charAt(servletPath.length()-1))
        {
          case '|':
          case '%':
          case '$':
          case '*':
              servletPath=servletPath.substring(0,servletPath.length()-1);
              break;
          default:
              int s = servletPath.indexOf("*");
              if (s==0)
                  servletPath="";
              else if (s>=0)
                  servletPath=servletPath.substring(0,s);
        }
        
        Code.debug("SetServletPath '"+servletPath+
                   "' in " + uri );
                                        
        this.servletPath=servletPath;

        if (!path.startsWith(servletPath))
            throw new MalformedURLException("Bad servletPath '"+
                                            servletPath+"' for "+uri);

        pathInfo=path.substring(servletPath.length());

//      System.err.println("uri="+uri.getPath());
//      System.err.println("PI="+pathInfo);
//      if (uri.getPath().endsWith(pathInfo))
//      {
//          this.servletPath=uri.getPath().substring(0,
//                                                   uri.getPath().length()-
//                                                   pathInfo.length());
//          System.err.println("SP="+this.servletPath);
//      }
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
     * @param translateURI If true, the URI of the request is translated,
     * otherwise only the resource path is affected.
     */
    public void translateAddress(String pathSpec,
                                 String newPath,
                                 boolean translateURI)
    {
        String path=getResourcePath();
        path=PathMap.translate(path,pathSpec,newPath);
        
        servletPath=null;
        pathInfo=path;
        setResourcePath(path,null);
        if (translateURI)
            uri.setPath(getResourcePath());
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
     * After this call, form parameters may be fetch via the
     * getParameter() method.
     */
    public void decodeFormParameters()
         throws IOException
    {
         if (formParameters!=null)
             return;
 
         String contentType = getContentType();
         if (contentType!=null &&
             contentType.equals(HttpHeader.WwwFormUrlEncode))
         {
             int contentLength = getContentLength();
             if (contentLength<0)
                 Code.warning("No contentLength for "+
                              HttpHeader.WwwFormUrlEncode);
             else {
                 // Read all the post data
                 InputStream in = getInputStream();
                 byte[] postBytes = new byte[contentLength];
                 int n = 0;
                 while (n < contentLength) {
                     int count = in.read(postBytes, n, contentLength - n);
                     if (count < 0)
                         throw new EOFException();
                     n += count;
                 }
 
                 // Convert it to a hash table
                 String content = new String(postBytes,"ISO-8859-1");
                 formParameters = new UrlEncoded(content);
             }
         }
    }
    
    /* -------------------------------------------------------------- */
    /** Decode Cookies
     * If includeAsParameters is true,  cookies may be fetch via the
     * getParameter() method
     */
    public void cookiesAsParameters()
         throws IOException
    {
        getCookies();
        cookieParameters=new Hashtable(11);
        for (int i=0;cookies!=null && i<cookies.length; i++)
            cookieParameters.put(cookies[i].getName(),
                                 cookies[i].getValue());
    }
    
    
    /* -------------------------------------------------------------- */
    /** Write the request header to an output stream
     */ 
    public void write(OutputStream out)
        throws IOException
    {
        out.write(method.getBytes());
        out.write(' ');
        
        if (resourcePath!=null && resourcePath.length()>0)
        {
            out.write(resourcePath.getBytes());
            String qs = getQueryString();
            if (qs!=null && qs.length()>0)
                out.write(("?"+qs).getBytes());
        }
        else
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
    
    /* ------------------------------------------------------------- */
    /** Get the HttpServer of the request.
     */
    public HttpServer getHttpServer()
    {
        return httpServer;
    }
    
    /* ------------------------------------------------------------- */
    /** Get the HttpInputStream of the request.
     * It is dangerous to use this unless you know what you are doing.
     */
    public HttpInputStream getHttpInputStream()
    {
        return in;
    }

    /* ------------------------------------------------------------ */
    private static ThreadPool localThreadPool=null;

    /* ------------------------------------------------------------ */
    /** Handle a request locally.
     * This methods dispatches this request to the local server.
     * It is used by the ServletContext.getResourceAsStream method.
     * @return InputStream containing content.
     * @exception IOException 
     */
    public InputStream handleRequestLocally()
        throws IOException
    {
        Code.assert(httpServer!=null,"Not constructed with HttpServer");
        if (localThreadPool==null)
            localThreadPool=new ThreadPool(0,"LocalRequest");

        Code.debug("HandleRequestLocally()");
        final PipedInputStream in = new PipedInputStream();
        final PipedOutputStream out = new PipedOutputStream(in);
        final HttpResponse response = new HttpResponse(out,this);
        
        // run new request in own thread
        try{
            localThreadPool
                .run(new Runnable()
                     {
                         public void run()
                             {
                                 try{
                                     httpServer.handle(HttpRequest.this,response);
                                     out.close();
                                     response.destroy();
                                 }
                                 catch(IOException e)
                                 {
                                     Code.warning(e);
                                 }
                             }
                     });
        }
        catch(InterruptedException e)
        {
            Code.warning(e);
            return null;
        }
        
        // Get response line
        HttpInputStream replyStream = new HttpInputStream(in);
        String replyLine=replyStream.readLine();
        Code.debug("Resource response: ",replyLine);
        
        // Skip header of reply
        HttpHeader replyHeader = new HttpHeader();
        replyHeader.read(replyStream);
        
        // Return content
        return replyStream;
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
            else if (address!=null && address.getInetAddress()!=null)
                serverName = address.getInetAddress().getHostName();
            
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

        if (address!=null && serverPort==0)
            serverPort=address.getPort();
        
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
        if (connection!=null)
            return connection.getInetAddress().getHostAddress();
        return "localhost";
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
     * @deprecated
     */  
    public String getRealPath(String path)
    {
        return httpServer.getRealPath(path);
    }

    /* -------------------------------------------------------------- */
    /** Get request input stream
     * @return an input stream for reading the request body.
     */
    public synchronized ServletInputStream getInputStream()
    {
        if (inputState!=0 && inputState!=1)
            throw new IllegalStateException();
        inputState=1;
        return in;
    }

    /* -------------------------------------------------------------- */
    /**
     * Returns the value of the specified parameter for the request. For
     * example, in an HTTP servlet this would return the value of the
     * specified query string parameter.
     * @param name the parameter name
     * @return For the given example, getParameter("aaa") would
     * return "123".
     * @deprecated use getParameterValues
     */
    public String getParameter(String name)
    {
        Object value=null;
	if (redirectParams!=null)
	    value = redirectParams.get(name);
	if (value==null)
	    value = uri.get(name);
        if (value==null && formParameters!=null)
            value = formParameters.get(name);
        if (value==null && cookieParameters!=null)
            value = cookieParameters.get(name);
        if (value==null)
            return null;
        return value.toString();
    }
    
    /* -------------------------------------------------------------- */
    /**
     * Returns the multi-values of the specified parameter for the request.
     */
    public String[] getParameterValues(String name)
    {
        Object values=uri.getValues(name);
        if (values==null && formParameters!=null)
            values = formParameters.getValues(name);
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
            return uri.getParameterNames();

        Vector names = new Vector();
        Enumeration e = uri.getParameters().keys();
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
    /** Returns the scheme of the URL used in this request, for
     * example "http", "https"
     */
    public String getScheme()
    {
        return "http";
    }
    
    /* -------------------------------------------------------------- */
    /**
     * @param name the attribute name
     * @return the value of the attribute, or null if not defined
     */
    public Object getAttribute(String name)
    {
        if (attributes == null)
            return null;
        
        return attributes.get( name );
    }
    
    /* -------------------------------------------------------------- */
    public Enumeration getAttributeNames()
    {
        if (attributes == null)
            return __NoAttributes;
        
        return attributes.keys();
    }
    
    /* -------------------------------------------------------------- */
    public void setAttribute(String name, Object o)
    {   
        if (attributes == null)
            attributes = new Hashtable(11);
        attributes.put(name, o);
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
        String s = getHeader(ContentType);
        try {
            int i1 = s.indexOf("charset=",s.indexOf(';')) + 8;
            int i2 = s.indexOf(' ',i1);
            return (0 < i2) ? s.substring(i1,i2) : s.substring(i1);
        }
        catch (Exception e)
        {
            return "ISO-8859-1";
        }
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
     * /Servlet/Path/Foo/Bar
     * </PRE>
     */
    public  String getRequestURI()
    {
        return uri.getPath();
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
        if (pathInfo.length()==0)
            return null;
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
        if (pathInfo.length()==0)
            return null;
        if (pathTranslated==null)
            pathTranslated=getRealPath(pathInfo);
        return pathTranslated;
    }

    
    /* -------------------------------------------------------------- */
    /** Get the query string of the request
     * @return For the given example, this would return <PRE>
     * aaa=123&bbb=456
     * </PRE>
     */
    public  String getQueryString()
    {
        return uri.getQuery();
    }

    /* -------------------------------------------------------------- */
    /** Get the remote user name decoded from authentication headers
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
        if (cookies==null)
        {
            String c = getHeader(HttpHeader.Cookie);
            if (c!=null)
                cookies=Cookies.decode(c);
            else
                cookies=new javax.servlet.http.Cookie[0];
        }
        return cookies;
    }
    
    /* -------------------------------------------------------------- */
    public String getRequestedSessionId()
    {
        if (sessionIdState == SESSIONID_NOT_CHECKED)
        {          
            // Then try cookies
            if (sessionId == null)
            {
                getCookies();
                for (int i=0; cookies!=null && i<cookies.length; i++)
                {
                    if (cookies[i].getName().equals(SessionContext.SessionId))
                    {
                        sessionId=cookies[i].getValue();
                        sessionIdState = SESSIONID_COOKIE;
                        Code.debug("Got Session ",sessionId," from cookie");
                    }
                }
            }
            
            // check if there is a url encoded session param.
            String path = getRequestPath();
            int prefix=path.indexOf(SessionContext.SessionUrlPrefix);
            if (prefix!=-1)
            {
                int suffix=path.indexOf(SessionContext.SessionUrlSuffix);
                if (suffix!=-1 && prefix<suffix)
                {
                    // definitely a session id in there!
                    String id =
                        path.substring(prefix+
                                       SessionContext.SessionUrlPrefix.length(),
                                       suffix);
                    
                    Code.debug("Got Session ",id," from URL");
                    
                    try
                    {
                        Long.parseLong(id,36);
                        if (sessionIdState==SESSIONID_NOT_CHECKED)
                        {
                            sessionId=id;
                            sessionIdState = SESSIONID_URL;
                        }
                        else if (!id.equals(sessionId))
                            Code.warning("Mismatched session IDs");
                        
                        // translate our path to drop the prefix off.
                        if (suffix+SessionContext.SessionUrlSuffix.length()
                            <path.length())
                            setRequestPath(path.substring(0,prefix)+
                                           path.substring(suffix+
                                                          SessionContext.SessionUrlSuffix.length()));
                        else
                            setRequestPath(path.substring(0,prefix));
                        
                        Code.debug(getRequestPath());
                    }
                    catch(NumberFormatException e)
                    {
                        Code.ignore(e);
                    }
                }
            }
            
            if (sessionId == null)
                sessionIdState = SESSIONID_NONE;
        }
        
        return sessionId;
    }
    
    /* ------------------------------------------------------------ */
    public HttpSession getSession()
    {
        HttpSession session = getSession(false);
        return (session == null) ? getSession(true) : session;
    }
    
    /* -------------------------------------------------------------- */
    public HttpSession getSession(boolean create)
    {
        Code.debug("getSession("+create+")");
        
        if (session != null && SessionContext.isValid(session))
            return session;
        
        String id = getRequestedSessionId();
        
        if (id != null)
        {
            session=sessions.getSession(id);
            if (session == null && !create)
                return null;
        }

        if (session == null && create)
        {
            session = sessions.newSession();
            Cookie cookie =
                new Cookie(SessionContext.SessionId,session.getId());
            cookie.setPath("/");
            response.addCookie(cookie); 
        }

        return session;
    }
    
    /* -------------------------------------------------------------- */
    public boolean isRequestedSessionIdFromCookie()
    {
        return sessionIdState == SESSIONID_COOKIE;
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
    public boolean isRequestedSessionIdFromURL()
    {
        return sessionIdState == SESSIONID_URL;
    }
    
    /* -------------------------------------------------------------- */
    public boolean isRequestedSessionIdValid()
    {
        return sessionId != null && getSession(false) != null;
    }
    
    /* -------------------------------------------------------------- */
    public synchronized BufferedReader getReader()
    {
        if (inputState!=0 && inputState!=2)
            throw new IllegalStateException();
        if (reader==null)
        {
            try
            {
                reader=new BufferedReader(new InputStreamReader(getInputStream(),"ISO-8859-1"));
            }
            catch(UnsupportedEncodingException e)
            {
                Code.ignore(e);
                reader=new BufferedReader(new InputStreamReader(getInputStream()));
            }
            inputState=2;
        }
        return reader;
    }

    /* -------------------------------------------------------------- */
    void decodeRequestLine(char[] buf,int len)
        throws IOException
    {
        // Search for first space separated chunk
        int s1=-1,s2=-1,s3=-1;
        int state=0;
    startloop:
        for (int i=0;i<len;i++)
        {
            char c=buf[i];
            switch(state)
            {
              case 0: // leading white
                  if (c==' ')
                      continue;
                  state=1;
                  s1=i;
                  
              case 1: // reading method
                  if (c==' ')
                      state=2;
                  else
                      s2=i;
                  continue;
                  
              case 2: // skip whitespace after method
                  s3=i;
                  if (c!=' ')
                      break startloop;
            }
        }

        // Search for first space separated chunk
        int e1=-1,e2=-1,e3=-1;
        state=0;
    endloop:
        for (int i=len;i-->0;)
        {
            char c=buf[i];
            switch(state)
            {
              case 0: // leading white
                  if (c==' ')
                      continue;
                  state=1;
                  e1=i;
                  
              case 1: // reading method
                  if (c==' ')
                      state=2;
                  else
                      e2=i;
                  continue;
                  
              case 2: // skip whitespace after method
                  e3=i;
                  if (c!=' ')
                      break endloop;
            }
        }
        
        // Check sufficient params
        if (s3<0 || e1<0 || e3<s2 )
            throw new IOException("Bad requestline");

        // get method
        method=new String(buf,s1,s2-s1+1);
        
        // get version
        if (s2!=e3 || s3!=e2)
        {
            for (int i=e1;i<=e2;i++)
                if (buf[i]>'a'&&buf[i]<'z')
                    buf[i]=(char)(buf[i]-'a'+'A');
            version=new String(buf,e2,e1-e2+1);
        }
        else
        {
            // missing version
            version=HttpHeader.HTTP_1_0;
            e3=e1;
        }

        // rebuild requestline
        StringBuffer rl = new StringBuffer(len);
        rl.append(buf,s1,s2-s1+2);
        rl.append(buf,s3,e3-s3+1);
        rl.append(" ");
        rl.append(version);
        requestLine=rl.toString();

        // handle URI
        String uris=null;
        if (buf[s3]!='/')
        {
            // look for //
            for (int i=s3;i<e3;i++)
            {
                if (buf[i]=='/')
                {
                    if (buf[i+1]!='/')
                        break;

                    // look for next /
                    for (int j=i+2;j<=e3;j++)
                    {
                        if (buf[j]=='/')
                        {
                            protocolHostPort=new String(buf,s3,j-s3+1);
                            uris=new String(buf,j,e3-j+1);
                            break;
                        }
                    }
                    break;
                }
            }
        }
        if (uris==null)
        {
            protocolHostPort="";
            uris = new String(buf,s3,e3-s3+1);
        }
        
        uri = new URI(uris);
        Code.debug(requestLine);
    }


    /* ------------------------------------------------------------ */
    /** Destroy the request.
     * Help the garbage collector by null everything that we can.
     */
    public void destroy()
    {
        method=null;
        uri=null;
        version=null;
        httpServer=null;
        connection=null;
        in=null;
        address=null;
        if (formParameters!=null)
        {
            formParameters.clear();
            formParameters=null;
        }
        if (cookieParameters!=null)
        {
            cookieParameters.clear();
            cookieParameters=null;
        }
        if (attributes!=null)
        {
            attributes.clear();
            attributes=null;
        }
        cookies=null;
        sessionId=null;
        session=null;
        sessionIdState=null;
        requestLine=null;
        protocolHostPort=null;
        resourcePath=null;
        servletPath=null;
        pathInfo=null;
        remoteUser=null;
        authType=null;
        byteContent=null;
        pathTranslated=null;
        serverName=null;
        response=null;
        reader=null;
        super.destroy();
    }
    
    
    /* -------------------------------------------------------------- */
    /** Set the default session timeout.
     *  @param  default The default timeout in seconds
     */
    public static void setSessionMaxInactiveInterval(int defaultTime)
    {
        sessions.setMaxInactiveInterval(defaultTime);
    }
}

           

