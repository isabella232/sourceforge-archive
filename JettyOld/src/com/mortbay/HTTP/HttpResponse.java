// ========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ------------------------------------------------------------------------

package com.mortbay.HTTP;

import com.mortbay.Base.*;
import com.mortbay.Util.*;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import java.net.URL;
import java.net.MalformedURLException;

/* ------------------------------------------------------------------ */
/** Handling of a HTTP response.
 * Implements and extends the javax.servlet.http.HttpServletResponse
 * interface.
 * The extensions are for HttpHandler instances that need to modify
 * the response or have better access to the IO.
 *
 * <p><h4>Note</h4>
 * By default, responses will only use chunking if requested by the
 * by setting the transfer encoding header.  However, if
 * the chunkByDefault is set, then chunking is
 * used if no content length is set.
 *
 * @see com.mortbay.HTTP.HttpServer
 * @version $Id$
 * @author Greg Wilkins
*/
public class HttpResponse extends HttpHeader implements HttpServletResponse
{    
    /* -------------------------------------------------------------- */
    public final static String MIME_Version ="MIME-Version"   ;
    public final static String Server ="Server"   ;
    public final static String Expires ="Expires"   ;
    public final static String Location ="Location"   ;
    public final static String Allow = "Allow"   ;
    
    /* -------------------------------------------------------------- */
    public final static Hashtable __errorCodeMap = new Hashtable();
    static
    {
        // Build error code map using reflection
        try
        {
            Field[] fields = javax.servlet.http.HttpServletResponse.class
                .getDeclaredFields();
            for (int f=fields.length; f-->0 ;)
            {
                int m = fields[f].getModifiers();
                if (!Modifier.isFinal(m) || !Modifier.isStatic(m))
                    continue;

                if (!fields[f].getType().equals(Integer.TYPE))
                    continue;

                if (fields[f].getName().startsWith("SC_"))
                {
                    String error = fields[f].getName().substring(3);
                    error = error.replace('_',' ');
                    __errorCodeMap.put(fields[f].get(null),error);
                }
            }
        }
        catch (Exception e)
        {
            Code.warning(e);
        }
    }
    
    /* -------------------------------------------------------------- */
    private String version;
    private String status;
    private String reason;
    private HttpOutputStream httpOut;
    private OutputStream out;
    private PrintWriter writer;
    private boolean headersWritten=false;   
    private Cookies cookies = null;
    private Vector filters=null;
    private HttpRequest request=null;
    private Observable observable=null;
    private boolean chunkByDefault=false;
    private boolean doNotClose=false;
    private int outputState=0;
    private boolean handled=false;
    private HttpSession session=null;
    private boolean noSession=false;
    
    /* -------------------------------------------------------------- */
    /** Construct a response
     * @param out The output stream that the response will be written to.
     * @param request The HttpRequest that this response is to.
     */
    public HttpResponse(OutputStream out, HttpRequest request)
    {
        this.out=out;
        this.httpOut=new HttpOutputStream(out,this);
        this.request=request;
        if (request!=null)
            request.setHttpResponse(this);
        version = HttpHeader.HTTP_1_0;
        status = Integer.toString(SC_OK);
        reason = "OK";
        setHeader(ContentType,"text/html");
        setHeader(MIME_Version,"1.0");
        
        // XXX - need to automate setting this with getServerInfo
        setHeader(Server,Version.__jetty);
        setDateHeader(Date,System.currentTimeMillis());

        if (request!=null &&
            HttpHeader.Close.equals(request.getHeader(HttpHeader.Connection)))
            setHeader(HttpHeader.Connection,HttpHeader.Close);
    }
    
    /* -------------------------------------------------------------- */
    public String getVersion()
    {
        return version;
    }
    
    /* -------------------------------------------------------------- */
    public void setVersion(String version)
    {
        this.version=version;
    }
    
    /* -------------------------------------------------------------- */
    public String getStatus()
    {
        return status;
    }
    
    /* -------------------------------------------------------------- */
    public String getReason()
    {
        return reason;
    }
    
    /* -------------------------------------------------------------- */
    /** Get the HttpRequest for this response
     */
    public HttpRequest getRequest()
    {
        return request;
    }
    
    /* -------------------------------------------------------------- */
    public String getResponseLine()
    {
        return version + " "+status + " " + reason;
    }

    /* -------------------------------------------------------------- */
    public void setChunkByDefault(boolean chunk)
    {
        chunkByDefault=chunk;
    }
    
    /* -------------------------------------------------------------- */
    /** Add an observer to the response.
     * Observers are notified when the HTTP headers are complete
     * and before any output has been written.  Thus observers may
     * examine or modify the headers and activate filters.
     * Notify is called with the request as the argument object
     */
    public void addObserver(Observer o)
    {
        if (observable==null)
            observable=new Observed();
        Code.debug("Added Observer "+o);
        observable.addObserver(o);
    }
    
    /* -------------------------------------------------------------- */
    public void deleteObserver(Observer o)
    {
        if (observable!=null)
            observable.deleteObserver(o);
    }

    /* -------------------------------------------------------------- */
    /** Complete the response.
     * Will conditionally call close to indicate the end
     * of the response
     */
    public void complete()
        throws IOException
    {
        if (!doNotClose)
            httpOut.close();
        doNotClose=false;
    }
    
    /* -------------------------------------------------------------- */
    /** Return true if the headers have already been written for this
     * response
     */
    public boolean headersWritten()
    {
        return headersWritten;
    }
    
    /* -------------------------------------------------------------- */
    /** Return true if the headers have already been written for this
     * response
     */
    public boolean requestHandled()
    {
        return handled;
    }
    
    /* -------------------------------------------------------------- */
    /** If the headers have not already been written, write them.
     * If any HttpFilters have been added activate them before writing.
     */
    public void writeHeaders() 
        throws IOException
    {
        if (headersWritten)
            return;
        Code.debug("Write Headers");
        headersWritten=true;
        
        // Add Date if not already there
        if (getHeader(HttpHeader.Date)==null)
        {
            int s = Integer.parseInt(status);
            if (s<100 || s>199)
                setDateHeader(HttpHeader.Date,new Date().getTime());
        }

            
        // Tell anybody who wants to not headers are complete
        // (e.g.. to activate filters by content-type)
        if (observable!=null)
        {
            Code.debug("notify Observers");
            observable.notifyObservers(this);
        }
            
        // Should we chunk or close
        if (HttpHeader.HTTP_1_1.equals(version))
        {
            String encoding = getHeader(HttpHeader.TransferEncoding);
            String connection =getHeader(Connection);
            String length = getHeader(HttpHeader.ContentLength);
                    
            // chunk if we are told to
            if (encoding!=null && encoding.equals(HttpHeader.Chunked))
            {
                httpOut.setChunking(true);
            }
            // if we have no content length then ...
            else if (length==null)
            {
                // if not closing and chunk by default
                if (!(HttpHeader.Close.equals(connection))
                    && chunkByDefault)
                {
                    // need to chunk
                    setHeader(HttpHeader.TransferEncoding,
                              HttpHeader.Chunked);
                    httpOut.setChunking(true);
                }
                else
                {
                    // have to close to mark the end
                    setHeader(Connection,HttpHeader.Close);
                }
            }
            else
            {
                // We have a content length, so we will not be
                // chunking, but we can be persistent, so we must
                // hide the next close.
                doNotClose=true;
            }
        }
        else if (HttpHeader.HTTP_1_0.equals(version))
        {
            String connection=getHeader(Connection);
            if (connection==null)
            {
                // Assume we close unless otherwise
                setHeader(Connection,HttpHeader.Close);
            
                String length = getHeader(HttpHeader.ContentLength);
                if (length!=null && length.length()>0 && request!=null)
                {
                    if (request.getHttpServer().http1_0_KeepAlive)
                    {
                        // We have a length, so Consider a keep-alive
                        connection=request.getHeader(Connection);
                        if (connection!=null &&
                            "keep-alive".equals(StringUtil.asciiToLowerCase(connection)))
                        {
                            // Lets Keep the connection alive
                            setHeader(Connection,"Keep-Alive");
                            doNotClose=true;
                        }
                    }
                }
            }
        }
        else
        {
            setHeader(Connection,HttpHeader.Close);
        }
            
            
        // Write the headers
        handled=true;
        OutputStreamWriter writer = new OutputStreamWriter(out,"ISO-8859-1");
        synchronized(writer)
        {
            writer.write(version);
            writer.write(" ");
            writer.write(status);
            writer.write(" ");
            writer.write(reason);
            writer.write(CRLF);
            
            if (cookies!=null)
                super.write(writer,cookies.toString());
            else
                super.write(writer);
            writer.flush();
        }
        
        // Handle HEAD
        if (request!=null && request.getMethod().equals("HEAD"))
            // Fake a break in the HttpOutputStream
            throw HeadException.instance;
    }

    
    /* ------------------------------------------------------------- */
    /** Copy all data from an input stream to the HttpResponse.
     * This method assumes that the input stream does not include HTTP
     * headers.
     * @param stream the InputStream to read
     * @param length If greater than 0, this is the number of bytes
     *        to read and write
     */
    public void writeInputStream(InputStream stream,long length)
        throws IOException
    {
        writeInputStream(stream,length,false);
    }
    
    /* ------------------------------------------------------------- */
    /** Copy all data from an input stream to the HttpResponse.
     * @param stream the InputStream to read
     * @param length If greater than 0, this is the number of bytes
     *        to read and write
     * @param streamIncludesHeaders True when the input stream includes
     *        HTTP headers which replace those set in this HttpResponse.
     */
    public void writeInputStream(InputStream stream,
                                 long length,
                                 boolean streamIncludesHeaders)
        throws IOException
    {
        Code.debug("writeInputStream:"+length);
        if (streamIncludesHeaders)
        {
            Code.assert(!headersWritten(),"Headers already written");
            headersWritten=true;
            handled=true;
            if (observable!=null)
                observable.notifyObservers(this);
        }
        
        if (stream!=null)
            IO.copy(stream,getOutputStream(),length);
    }
    
    
    /* -------------------------------------------------------------- */
    /** Return the Cookies instance
     * These are the "Set-Cookies" that will be sent with this response.
     * Cookies may be added, modified or deleted from the Cookies instance.
     * @deprecated Use addCookie()
     */
    public Cookies getCookies()
    {
        if (cookies==null)
            cookies=new Cookies();
        return cookies;
    }
    

    /* ------------------------------------------------------------- */
    /** Get the HttpOutputStream of the response.
     */
    synchronized HttpOutputStream getHttpOutputStream()
    {
        return httpOut;
    }

    /* ------------------------------------------------------------- */
    void flush()
        throws IOException
    {
        if (outputState==2)
            writer.flush();
        else
            httpOut.flush();
    }

    
    /* -------------------------------------------------------------- */
    /* ServletResponse methods -------------------------------------- */
    /* -------------------------------------------------------------- */

    /* ------------------------------------------------------------- */
    /** Set the content length of the response
     */
    public void setContentLength(int len)
    {
        setHeader(ContentLength,Integer.toString(len));
        handled=true;
    }

    /* ------------------------------------------------------------- */
    /** Set the content type of the response
     */
    public void setContentType(String type)
    {
        setHeader(ContentType,type);
        handled=true;
    }


    /* ------------------------------------------------------------- */
    /** Get the OutputStream of the response.
     * The first write to this stream will trigger writing of the
     * HTTP filters and potential activation of any HttpFilters.
     */
    public synchronized ServletOutputStream getOutputStream()
    {
        handled=true;
        if (outputState!=0 && outputState!=1)
            throw new IllegalStateException();
        outputState=1;
        return httpOut;
    }
    
    /* -------------------------------------------------------------- */
    /* HttpServletResponse methods ---------------------------------- */
    /* -------------------------------------------------------------- */
    
    /* ------------------------------------------------------------- */
    /**
     * Sets the status code and message for this response.
     * @param code the status code
     * @param msg the status message
     * @deprecated
     */
    public void setStatus(int code,String msg)
    {
        handled=true;
        status=Integer.toString(code);
        reason=(String)__errorCodeMap.get(new Integer(code));
        if (reason==null)
            reason=msg;
    }

    /* ------------------------------------------------------------- */
    /**
     * Sets the status code and a default message for this response.
     * @param code the status code
     */
    public void setStatus(int code)
    {
        handled=true;
        status=Integer.toString(code);
        String msg = (String)__errorCodeMap.get(new Integer(code));
        reason=(msg!=null)?msg:status;
    }
      
    /* ------------------------------------------------------------- */
    /**
     * Sends an error response to the client using the specified status
     * code and detail message.
     * @param code the status code
     * @param msg the detail message
     * @exception IOException If an I/O error has occurred.
     */
    public void sendError(int code,String msg) 
        throws IOException
    {
        setContentType("text/html");
        setStatus(code,msg);
        writeHeaders();

        if (writer!=null)
            writer.flush();
        outputState=0;
        PrintWriter out = getWriter();
        out.println("<HTML><HEAD><TITLE>Error "+code+"</TITLE>");
        out.println("<BODY><H2>HTTP ERROR: "+
                    code +
                    " " + msg + "</H2>");       
        out.println("</BODY>\n</HTML>");
        out.flush();
    }
      
    /* ------------------------------------------------------------- */
    /**
     * Sends an error response to the client using the specified status
     * code and no default message.
     * @param code the status code
     * @exception IOException If an I/O error has occurred.
     */
    public void sendError(int code) 
        throws IOException
    {
        String msg = (String)__errorCodeMap.get(new Integer(code));
        if (msg==null)
            sendError(code,"UNKNOWN ERROR CODE");
        else
            sendError(code,msg);
    }
    
    /* ------------------------------------------------------------- */
    /**
     * Sends a redirect response to the client using the specified redirect
     * location URL.
     * @param location the redirect location URL
     * @exception IOException If an I/O error has occurred.
     */
    public void sendRedirect(String location)
        throws IOException
    {
        setHeader(Location,location);
        setStatus(SC_MOVED_TEMPORARILY);
        writeHeaders();
    }
    
    /* -------------------------------------------------------------- */
    public boolean containsHeader(String headerKey)
    {
        return getHeader(headerKey)!=null;
    }

    /* -------------------------------------------------------------- */
    public void addCookie(javax.servlet.http.Cookie cookie)
    {
        if (cookies==null)
            cookies=new Cookies();
        cookies.setCookie(cookie);
    }

    /* -------------------------------------------------------------- */
    /**
     * @deprecated
     */
    public java.lang.String encodeRedirectUrl(java.lang.String url)
    {
        return encodeRedirectURL(url);
    }
    
    /* -------------------------------------------------------------- */
    /**
     * @deprecated
     */
    public java.lang.String encodeUrl(java.lang.String url)
    {
        return encodeURL(url);
    }
    
    /* -------------------------------------------------------------- */
    public java.lang.String encodeRedirectURL(java.lang.String url)
    {
        //XXX - Don't know what to do different here?
        return encodeURL(url);
    }
    
    /* ------------------------------------------------------------ */
    /** Encode session ID in URL if required.
     * The session ID is encoded in the file part of the URL between
     * character guards configured in SessionContext.
     * If '/' is in the URL, the encoded session is placed after the last
     * '/', otherwise is is placed at the start of the URL. This encoding
     * should not disturb any browser interpretation of relative paths,
     * file suffixes or URL references.
     * @param url The URL to encode
     * @return Encoded URL or the original URL
     */
    public java.lang.String encodeURL(java.lang.String url)
    {
        // should not encode if cookies in evidence
        if (request==null || request.isRequestedSessionIdFromCookie())
            return url;
        
        // get session;
        if (session==null && !noSession)
        {
            session=request.getSession(false);
            noSession=(session==null);
        }

        // no session or no url
        if (session == null || url==null)
            return url;

        // invalid session
        String id = session.getId();
        if (id == null)
            return url;

        // insert the id
        int from= url.length()-1;
        if (from<0)
            return SessionContext.SessionUrlPrefix + id +
                SessionContext.SessionUrlSuffix;            
        
        int hook = url.indexOf('?');
        if (hook>=0 && hook<from)
            from=hook;
        int hash = url.indexOf('#');
        if (hash>=0 && hash<from)
            from=hash;
            
        int slash = url.lastIndexOf('/',from);
        if (slash==-1)
        {
            url =
                SessionContext.SessionUrlPrefix + id +
                SessionContext.SessionUrlSuffix + url;    
        }
        else
        {
            url =
                url.substring(0,slash+1) +
                SessionContext.SessionUrlPrefix + id +
                SessionContext.SessionUrlSuffix + 
                url.substring(slash+1); 
        }
        
        
        // Must be a partial url...
        return url;
    }
    
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
    public synchronized java.io.PrintWriter getWriter()
    {
        if (outputState!=0 && outputState!=2)
            throw new IllegalStateException();
        
        if (writer==null)
        {
            try
            {
                writer=new PrintWriter(new OutputStreamWriter(getOutputStream(),getCharacterEncoding()));
            }
            catch(UnsupportedEncodingException e)
            {
                Code.warning(e);
                writer=new PrintWriter(new OutputStreamWriter(getOutputStream()));
            }
        }
        outputState=2;
        return writer;
    }    

    /* ------------------------------------------------------------ */
    /** Destroy the header.
     * Help the garbage collector by null everything that we can.
     */
    public void destroy()
    {
        version=null;
        status=null;
        reason=null;
        httpOut=null;
        out=null;
        writer=null;
        cookies=null;
        session=null;
        if (filters!=null)
        {
            filters.removeAllElements();
            filters=null;
        }
        request=null;
        observable=null;
        super.destroy();
    }
    
    /* ------------------------------------------------------------ */
    boolean preDispatchHandled=false;
    void preDispatch()
    {
	preDispatchHandled=handled;
	handled=false;
	if (writer!=null)
	    writer.flush();
	outputState=0;
    }
    
    /* ------------------------------------------------------------ */
    void postDispatch()
    {
	handled=preDispatchHandled;
	if (writer!=null)
	    outputState=2;
    }
}
