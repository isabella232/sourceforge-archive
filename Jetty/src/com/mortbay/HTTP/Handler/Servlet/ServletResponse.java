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

/* ------------------------------------------------------------ */
/** Wrapper of Jetty request for Servlet API.
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class ServletResponse implements HttpServletResponse
{
    private HttpResponse _httpResponse;
    private ServletRequest _servletRequest;
    private int _outputState=0;
    private ServletOut _out =null;
    private PrintWriter _writer=null;
    private HttpSession _session=null;
    private boolean _noSession=false;
    private boolean _locked=false;
    
    private static String __lockedMsg = "Response locked by inclusion";
    
    /* ------------------------------------------------------------ */
    ServletResponse(ServletRequest request,HttpResponse response)
    {
        _servletRequest=request;
	_servletRequest.setServletResponse(this);
        _httpResponse=response;
    }

    /* ------------------------------------------------------------ */
    boolean getLocked()
    {
	return _locked;
    }
    
    /* ------------------------------------------------------------ */
    void setLocked(boolean locked)
    {
	_locked=locked;
    }

    /* ------------------------------------------------------------ */
    int getOutputState()
    {
	return _outputState;
    }
    
    /* ------------------------------------------------------------ */
    void setOutputState(int s)
	throws IOException
    {
	_outputState=s;
	if (_out!=null)
	    _out.flush();
	if (_writer!=null)
	    _writer.flush();
    }
    
    
    /* ------------------------------------------------------------ */
    HttpResponse getHttpResponse()
    {
	return _httpResponse;
    }
    
    /* ------------------------------------------------------------ */
    void commit()
	throws IOException
    {
	_httpResponse.commit();
    }

    /* ------------------------------------------------------------ */
    public boolean isCommitted()
    {
	return _httpResponse.isCommitted();
    }
    
    /* ------------------------------------------------------------ */
    boolean isDirty()
    {
	return _httpResponse.isDirty();
    }

    /* ------------------------------------------------------------ */
    public void setBufferSize(int size)
    {
	ChunkableOutputStream out = _httpResponse.getOutputStream();
	if (out.isWritten())
	    throw new IllegalStateException("Output written");
	
	out.setBufferCapacity(size);
    }
    
    /* ------------------------------------------------------------ */
    public int getBufferSize()
    {
	return _httpResponse.getOutputStream().getBufferCapacity();
    }
    
    
    /* ------------------------------------------------------------ */
    public void flushBuffer()
	throws IOException
    {
	if (_out!=null)
	    _out.flush();
	else if (_writer!=null)
	    _writer.flush();
	else
	    _httpResponse.getOutputStream().flush();
    }
    
    /* ------------------------------------------------------------ */
    public void reset()
    {
	_httpResponse.reset();
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Sets the locale of the response, setting the headers (including the
     * Content-Type's charset) as appropriate.  This method should be called
     * before a call to {@link #getWriter}.  By default, the response locale
     * is the default locale for the server.
     * 
     * @param loc  the locale of the response
     *
     * @see 		#getLocale
     *
     */

    public void setLocale(Locale locale)
    {
	Code.notImplemented();
    }
    
    /* ------------------------------------------------------------ */
    /**
     * Returns the locale assigned to the response.
     * 
     * 
     * @see 		#setLocale
     *
     */
    public Locale getLocale()
    {
	Code.notImplemented();
	return null;
    }
    
    
    /* ------------------------------------------------------------ */
    public void addCookie(Cookie cookie) 
    {
        _httpResponse.addSetCookie(cookie.getName(),
				   cookie.getValue(),
				   cookie.getDomain(),
				   cookie.getPath(),
				   cookie.getMaxAge(),
				   cookie.getSecure());
    }

    /* ------------------------------------------------------------ */
    public boolean containsHeader(String name) 
    {
        return _httpResponse.containsField(name);
    }

    /* ------------------------------------------------------------ */
    public String encodeURL(String url) 
    {
        // should not encode if cookies in evidence
        if (_servletRequest==null || _servletRequest.isRequestedSessionIdFromCookie())
            return url;
        
        // get session;
        if (_session==null && !_noSession)
        {
            _session=_servletRequest.getSession(false);
            _noSession=(_session==null);
        }

        // no session or no url
        if (_session == null || url==null)
            return url;

        // invalid session
        String id = _session.getId();
        if (id == null)
            return url;

	// Check host and port are for this server
	// XXX not implemented
	
	// Already encoded
	int prefix=url.indexOf(Context.__SessionUrlPrefix);
	if (prefix!=-1)
	{
	    int suffix=url.indexOf(Context.__SessionUrlSuffix,prefix);
	    if (suffix!=-1 && prefix<suffix)
		// Update ID
		return
		    url.substring(0,prefix+Context.__SessionUrlPrefix.length())+
		    id+
		    url.substring(suffix);
	}
	
	// edit the session
	int end=url.indexOf('?');
	if (end<0)
	    end=url.indexOf('#');
	if (end<0)
	    return url+";jsessionid="+id+"_";
	return url.substring(0,end)+
	    ";jsessionid="+id+"_"+
	    url.substring(end);
    }

    /* ------------------------------------------------------------ */
    public String encodeRedirectURL(String url) 
    {
	return encodeURL(url);
    }

    /* ------------------------------------------------------------ */
    /**
     * @deprecated	As of version 2.1, use encodeURL(String url) instead
     */
    public String encodeUrl(String url) 
    {
        return encodeURL(url);
    }

    /* ------------------------------------------------------------ */
    /**
     * @deprecated	As of version 2.1, use 
     *			encodeRedirectURL(String url) instead
     *
     */
    public String encodeRedirectUrl(String url) 
    {
        return encodeRedirectURL(url);
    }

    /* ------------------------------------------------------------ */
    public void sendError(int status, String message)
	throws IOException
    {
	_httpResponse.sendError(status,message);
    }

    /* ------------------------------------------------------------ */
    public void sendError(int status) 
	throws IOException
    {
	_httpResponse.sendError(status);
    }

    /* ------------------------------------------------------------ */
    public void sendRedirect(String url) 
	throws IOException
    {
	_httpResponse.sendRedirect(url);
    }

    /* ------------------------------------------------------------ */
    public void setDateHeader(String name, long value) 
    {
	if (_locked)
	{
	    if (_httpResponse.getDateField(name)==value)
		return;
	    throw new IllegalStateException(__lockedMsg);
	}
	
	_httpResponse.setDateField(name,value);
    }

    /* ------------------------------------------------------------ */
    public void setHeader(String name, String value) 
    {
	if (_locked)
	{
	    if (value==null)
	    {
		if (_httpResponse.getField(name)==null)
		    return;
	    }
	    else if (value.equals(_httpResponse.getField(name)))
		return;
	    throw new IllegalStateException(__lockedMsg);
	}
	_httpResponse.setField(name,value);
    }

    /* ------------------------------------------------------------ */
    public void setIntHeader(String name, int value) 
    {
	if (_locked)
	{
	    if (_httpResponse.getIntField(name)==value)
		return;
	    throw new IllegalStateException(__lockedMsg);
	}
	_httpResponse.setIntField(name,value);
    }
    
    /* ------------------------------------------------------------ */
    public void addDateHeader(String name, long value) 
    {
	if (_locked)
	    throw new IllegalStateException(__lockedMsg);
	
	_httpResponse.addDateField(name,new Date(value));
    }

    /* ------------------------------------------------------------ */
    public void addHeader(String name, String value) 
    {
	if (_locked)
	    throw new IllegalStateException(__lockedMsg);
	
	_httpResponse.addField(name,value);
    }

    /* ------------------------------------------------------------ */
    public void addIntHeader(String name, int value) 
    {
	if (_locked)
	    throw new IllegalStateException(__lockedMsg);
	
	_httpResponse.addIntField(name,value);
    }

    /* ------------------------------------------------------------ */
    public void setStatus(int status) 
    {
	_httpResponse.setStatus(status);
    }

    /* ------------------------------------------------------------ */
    public void setStatus(int status, String message) 
    {
	setStatus(status);
	_httpResponse.setReason(message);
    }

    /* ------------------------------------------------------------ */
    public String getCharacterEncoding() 
    {
	return _httpResponse.getCharacterEncoding();
    }

    /* ------------------------------------------------------------ */
    public synchronized ServletOutputStream getOutputStream() 
    {
        if (_outputState!=0 && _outputState!=1)
            throw new IllegalStateException();
        if (_out==null)
            _out = new ServletOut(_servletRequest.getHttpRequest()
				  .getOutputStream());  
        _outputState=1;
        return _out;
    }

    /* ------------------------------------------------------------ */
    public synchronized PrintWriter getWriter() 
    {
        if (_outputState!=0 && _outputState!=2)
            throw new IllegalStateException();
        if (_writer==null)
            _writer =
		new PrintWriter
		    (new OutputStreamWriter
			(_servletRequest.getHttpRequest()
			 .getOutputStream()));
        _outputState=2;
        return _writer;
    }
    
    /* ------------------------------------------------------------ */
    public void setContentLength(int len) 
    {
	// Protect from setting after committed as default handling
	// of a servlet HEAD request ALWAYS sets content length, even
	// if the getHandling committed the response!
	if (!isCommitted())
	    setIntHeader(HttpFields.__ContentLength,len);
    }
    
    /* ------------------------------------------------------------ */
    public void setContentType(String contentType) 
    {
	setHeader(HttpFields.__ContentType,contentType);
    }
}





