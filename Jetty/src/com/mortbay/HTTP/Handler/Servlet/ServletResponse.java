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

/* ------------------------------------------------------------ */
/** 
 *
 * @see
 * @version 1.0 Sun Apr  9 2000
 * @author Greg Wilkins (gregw)
 */
public class ServletResponse implements HttpServletResponse
{
    private HttpResponse _response;
    private HttpRequest _request;
    private int _outputState=0;
    private ServletOut _out =null;
    private PrintWriter _writer=null;


    /* ------------------------------------------------------------ */
    ServletResponse(HttpResponse response, HttpRequest request)
    {
        _response=response;
        _request=request;
    }

    /* ------------------------------------------------------------ */
    void commit()
	throws IOException
    {
	_response.commit();
    }

    /* ------------------------------------------------------------ */
    public boolean isCommitted()
    {
	return _response.isCommitted();
    }

    /* ------------------------------------------------------------ */
    public void setBufferSize(int size)
    {
	ChunkableOutputStream out = _response.getOutputStream();
	if (out.isWritten())
	    throw new IllegalStateException("Output written");
	
	out.setBufferCapacity(size);
    }
    
    /* ------------------------------------------------------------ */
    public int getBufferSize()
    {
	return _response.getOutputStream().getBufferCapacity();
    }
    
    /* ------------------------------------------------------------ */
    public void flush()
	throws IOException
    {
	Code.debug("FLUSH");
	if (_out!=null)
	    _out.flush();
	else if (_writer!=null)
	    _writer.flush();
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
	    _response.getOutputStream().flush();

	if (!_response.isCommitted())
	    _response.commit();
    }
    
    /* ------------------------------------------------------------ */
    public void reset()
    {
	_response.reset();
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
        _response.addSetCookie(cookie.getName(),
                               cookie.getValue(),
                               cookie.getDomain(),
                               cookie.getPath(),
                               cookie.getMaxAge(),
                               cookie.getSecure());
    }

    /* ------------------------------------------------------------ */
    public boolean containsHeader(String name) 
    {
        return _response.containsField(name);
    }

    /* ------------------------------------------------------------ */
    /**
     * Encodes the specified URL by including the session ID in it,
     * or, if encoding is not needed, returns the URL unchanged.
     * The implementation of this method includes the logic to
     * determine whether the session ID needs to be encoded in the URL.
     * For example, if the browser supports cookies, or session
     * tracking is turned off, URL encoding is unnecessary.
     * 
     * <p>For robust session tracking, all URLs emitted by a servlet 
     * should be run through this
     * method.  Otherwise, URL rewriting cannot be used with browsers
     * which do not support cookies.
     *
     * @param	url	the url to be encoded.
     * @return		the encoded URL if encoding is needed;
     * 			the unchanged URL otherwise.
     */
    public String encodeURL(String url) 
    {
	Code.notImplemented();
	return null;
    }

    /* ------------------------------------------------------------ */
    /**
     * Encodes the specified URL for use in the
     * <code>sendRedirect</code> method or, if encoding is not needed,
     * returns the URL unchanged.  The implementation of this method
     * includes the logic to determine whether the session ID
     * needs to be encoded in the URL.  Because the rules for making
     * this determination can differ from those used to decide whether to
     * encode a normal link, this method is seperate from the
     * <code>encodeURL</code> method.
     * 
     * <p>All URLs sent to the <code>HttpServletResponse.sendRedirect</code>
     * method should be run through this method.  Otherwise, URL
     * rewriting cannot be used with browsers which do not support
     * cookies.
     *
     * @param	url	the url to be encoded.
     * @return		the encoded URL if encoding is needed;
     * 			the unchanged URL otherwise.
     *
     * @see #sendRedirect
     * @see #encodeUrl
     */
    public String encodeRedirectURL(String url) 
    {
	Code.notImplemented();
	return null;
    }

    /* ------------------------------------------------------------ */
    /**
     * @deprecated	As of version 2.1, use encodeURL(String url) instead
     *
     * @param	url	the url to be encoded.
     * @return		the encoded URL if encoding is needed; 
     * 			the unchanged URL otherwise.
     */
    public String encodeUrl(String url) 
    {
	Code.notImplemented();
	return null;
    }

    /* ------------------------------------------------------------ */
    /**
     * @deprecated	As of version 2.1, use 
     *			encodeRedirectURL(String url) instead
     *
     * @param	url	the url to be encoded.
     * @return		the encoded URL if encoding is needed; 
     * 			the unchanged URL otherwise.
     */
    public String encodeRedirectUrl(String url) 
    {
	Code.notImplemented();
	return null;
    }

    /* ------------------------------------------------------------ */
    public void sendError(int status, String message)
	throws IOException
    {
	_response.sendError(status,message);
    }

    /* ------------------------------------------------------------ */
    public void sendError(int status) 
	throws IOException
    {
	_response.sendError(status);
    }

    /* ------------------------------------------------------------ */
    public void sendRedirect(String url) 
	throws IOException
    {
	_response.sendRedirect(url);
    }

    /* ------------------------------------------------------------ */
    public void setDateHeader(String name, long value) 
    {
	_response.setDateField(name,value);
    }

    /* ------------------------------------------------------------ */
    public void setHeader(String name, String value) 
    {
	_response.setField(name,value);
    }

    /* ------------------------------------------------------------ */
    public void setIntHeader(String name, int value) 
    {
	_response.setIntField(name,value);
    }
    
    /* ------------------------------------------------------------ */
    public void addDateHeader(String name, long value) 
    {
	_response.addDateField(name,new Date(value));
    }

    /* ------------------------------------------------------------ */
    public void addHeader(String name, String value) 
    {
	_response.addField(name,value);
    }

    /* ------------------------------------------------------------ */
    public void addIntHeader(String name, int value) 
    {
	_response.addIntField(name,value);
    }

    /* ------------------------------------------------------------ */
    public void setStatus(int status) 
    {
	_response.setStatus(status);
    }

    /* ------------------------------------------------------------ */
    public void setStatus(int status, String message) 
    {
	_response.setStatus(status);
	_response.setReason(message);
    }

    /* ------------------------------------------------------------ */
    public String getCharacterEncoding() 
    {
	return _response.getCharacterEncoding();
    }

    /* ------------------------------------------------------------ */
    public synchronized ServletOutputStream getOutputStream() 
    {
        if (_outputState!=0 && _outputState!=1)
            throw new IllegalStateException();
        if (_out==null)
            _out = new ServletOut(_request.getOutputStream());  
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
			(_request.getOutputStream()));
        _outputState=2;
        return _writer;
    }
    
    /* ------------------------------------------------------------ */
    public void setContentLength(int len) 
    {
	_response.setIntField(HttpFields.__ContentLength,len);
    }
    
    /* ------------------------------------------------------------ */
    public void setContentType(String contentType) 
    {
	_response.setField(HttpFields.__ContentType,contentType);
    }
}





