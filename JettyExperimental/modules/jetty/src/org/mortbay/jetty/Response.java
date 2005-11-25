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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.mortbay.io.IO;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.ErrorHandler;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.log.Log;
import org.mortbay.util.ByteArrayISO8859Writer;
import org.mortbay.util.QuotedStringTokenizer;
import org.mortbay.util.StringUtil;
import org.mortbay.util.URIUtil;

/* ------------------------------------------------------------ */
/** Response.
 * @author gregw
 *
 */
public class Response implements HttpServletResponse
{   
    public static final int
        DISABLED=-1,
        NONE=0,
        STREAM=1,
        WRITER=2;
    
    private static PrintWriter __nullPrintWriter;
    private static ServletOutputStream __nullServletOut;

    static
    {
        try{
            __nullPrintWriter = new PrintWriter(IO.getNullWriter());
            __nullServletOut = new NullOutput();
        }
        catch (Exception e)
        {
            Log.warn(e);
        }
    }
    
    private HttpConnection _connection;
    private int _status=200;
    private String _reason;
    private Locale _locale;
    private String _mimeType;
    private String _characterEncoding;
    private boolean _explicitEncoding;
    private String _contentType;    
    private int _outputState;
    private PrintWriter _writer;



    /* ------------------------------------------------------------ */
    /**
     * 
     */
    Response(HttpConnection connection)
    {
        _connection=connection;
    }


    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#reset()
     */
    void recycle()
    {
        _status=200;
        _reason=null;
        _locale=null;
        _mimeType=null;
        _characterEncoding=null;
        _explicitEncoding=false;
        _contentType=null;
        _outputState=NONE;  
        _writer=null;
    }
    
    
    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#addCookie(javax.servlet.http.Cookie)
     */
    public void addCookie(Cookie cookie)
    {
        _connection.getResponseFields().addSetCookie(cookie);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#containsHeader(java.lang.String)
     */
    public boolean containsHeader(String name)
    {
        return _connection.getResponseFields().containsKey(name);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#encodeURL(java.lang.String)
     */
    public String encodeURL(String url)
    {
        Request request=_connection.getRequest();
        
        // should not encode if cookies in evidence
        if (url==null || request==null || request.isRequestedSessionIdFromCookie())
            return url;        
        
        // get session;
        HttpSession session=request.getSession(false);
        
        // no session 
        if (session == null)
            return url;
        
        // invalid session
        String id = session.getId();
        if (id == null)
            return url;
        
        // TODO Check host and port are for this server
        
        // Already encoded
        int prefix=url.indexOf(SessionManager.__SessionUrlPrefix);
        if (prefix!=-1)
        {
            int suffix=url.indexOf("?",prefix);
            if (suffix<0)
                suffix=url.indexOf("#",prefix);

            if (suffix<=prefix)
                return url.substring(0,prefix+SessionManager.__SessionUrlPrefix.length())+id;
            return url.substring(0,prefix+SessionManager.__SessionUrlPrefix.length())+id+
                url.substring(suffix);
        }        
        
        // edit the session
        int suffix=url.indexOf('?');
        if (suffix<0)
            suffix=url.indexOf('#');
        if (suffix<0)
            return url+SessionManager.__SessionUrlPrefix+id;
        return url.substring(0,suffix)+
            SessionManager.__SessionUrlPrefix+id+url.substring(suffix);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#encodeRedirectURL(java.lang.String)
     */
    public String encodeRedirectURL(String url)
    {
        return encodeURL(url);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#encodeUrl(java.lang.String)
     */
    public String encodeUrl(String url)
    {
        return encodeURL(url);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#encodeRedirectUrl(java.lang.String)
     */
    public String encodeRedirectUrl(String url)
    {
        return encodeURL(url);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#sendError(int, java.lang.String)
     */
    public void sendError(int code, String message) throws IOException
    {
        if (isCommitted())
            Log.warn("Committed before "+code+" "+message);
        
        reset();
        message=message==null?HttpGenerator.getReason(code):message; 
        setStatus(code,message);
        
        // If we are allowed to have a body 
        if (code!=SC_NO_CONTENT &&
            code!=SC_NOT_MODIFIED &&
            code!=SC_PARTIAL_CONTENT &&
            code>=SC_OK)
        {
            Request request = _connection.getRequest();
            // TODO - probably should reset these after the request?
            request.setAttribute(ServletHandler.__J_S_ERROR_STATUS_CODE,new Integer(code));
            request.setAttribute(ServletHandler.__J_S_ERROR_MESSAGE, message);
            request.setAttribute(ServletHandler.__J_S_ERROR_REQUEST_URI, request.getRequestURI());
            request.setAttribute(ServletHandler.__J_S_ERROR_SERVLET_NAME,"UNKNOWN"); // TODO!!!
            
            ErrorHandler error_handler = null;
            ContextHandler.Context context = request.getContext();
            if (context!=null)
                error_handler=context.getContextHandler().getErrorHandler();
            if (error_handler!=null)
                error_handler.handle(null,_connection.getRequest(),this, Handler.ERROR);
            else
            {
                setContentType(MimeTypes.TEXT_HTML);
                ByteArrayISO8859Writer writer= new ByteArrayISO8859Writer(2048);
                HttpConnection connection = HttpConnection.getCurrentConnection();
                ErrorHandler.writeErrorPage(request, writer, connection.getResponse().getStatus(), connection.getResponse().getReason());
                writer.flush();
                setContentLength(writer.size());
                writer.writeTo(getOutputStream());
                writer.destroy();
            }
        }
        else if (code!=SC_PARTIAL_CONTENT) 
        {
            _connection.getRequestFields().remove(HttpHeaders.CONTENT_TYPE_BUFFER);
            _connection.getRequestFields().remove(HttpHeaders.CONTENT_LENGTH_BUFFER);
            _characterEncoding=null;
            _mimeType=null;
        }
        
        complete();

        
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#sendError(int)
     */
    public void sendError(int sc) throws IOException
    {
        sendError(sc,null);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#sendRedirect(java.lang.String)
     */
    public void sendRedirect(String location) throws IOException
    {
        if (location==null)
            throw new IllegalArgumentException();
        
        if (!URIUtil.hasScheme(location))
        {
            StringBuffer buf = _connection.getRequest().getRootURL();
            if (location.startsWith("/"))
                buf.append(URIUtil.canonicalPath(location));
            else
            {
                String path=_connection.getRequest().getRequestURI();
                String parent=(path.endsWith("/"))?path:URIUtil.parentPath(path);
                location=URIUtil.canonicalPath(URIUtil.addPaths(parent,location));
                if (!location.startsWith("/"))
                    buf.append('/');
                buf.append(location);
            }
            
            location=buf.toString();
        }
        resetBuffer();
        
        setHeader(HttpHeaders.LOCATION,location);
        setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#setDateHeader(java.lang.String, long)
     */
    public void setDateHeader(String name, long date)
    {
        _connection.getResponseFields().putDateField(name, date);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#addDateHeader(java.lang.String, long)
     */
    public void addDateHeader(String name, long date)
    {
        _connection.getResponseFields().addDateField(name, date);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#setHeader(java.lang.String, java.lang.String)
     */
    public void setHeader(String name, String value)
    {
        _connection.getResponseFields().put(name, value);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#addHeader(java.lang.String, java.lang.String)
     */
    public void addHeader(String name, String value)
    {
        _connection.getResponseFields().put(name, value);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#setIntHeader(java.lang.String, int)
     */
    public void setIntHeader(String name, int value)
    {
        _connection.getResponseFields().putLongField(name, value);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#addIntHeader(java.lang.String, int)
     */
    public void addIntHeader(String name, int value)
    {
        _connection.getResponseFields().addLongField(name, value);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#setStatus(int)
     */
    public void setStatus(int sc)
    {
        setStatus(sc,null);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#setStatus(int, java.lang.String)
     */
    public void setStatus(int sc, String sm)
    {
        _status=sc;
        _reason=sm==null?HttpGenerator.getReason(sc):sm; 
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#getCharacterEncoding()
     */
    public String getCharacterEncoding()
    {
        if (_characterEncoding==null)
            _characterEncoding=StringUtil.__ISO_8859_1;
        return _characterEncoding;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#getContentType()
     */
    public String getContentType()
    {
        return _contentType;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#getOutputStream()
     */
    public ServletOutputStream getOutputStream() throws IOException
    {
        if (_outputState==DISABLED)
            return __nullServletOut;
        
        if (_outputState!=NONE && _outputState!=STREAM)
            throw new IllegalStateException("WRITER");
       
        _outputState=STREAM;
        return _connection.getOutputStream();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#getWriter()
     */
    public PrintWriter getWriter() throws IOException
    {
        if (_outputState==DISABLED)
            return __nullPrintWriter;
                                   
        if (_outputState!=NONE && _outputState!=WRITER)
            throw new IllegalStateException("STREAM");
        
        /* if there is no writer yet */
        if (_writer==null)
        {
            /* get encoding from Content-Type header */
            String encoding = _characterEncoding;
            
            if (encoding==null)
            {
                /* implementation of educated defaults */
                if(_mimeType!=null)
                    encoding = null; // TODO getHttpContext().getEncodingByMimeType(_mimeType);
                
                if (encoding==null)
                    encoding = StringUtil.__ISO_8859_1;
                
                setCharacterEncoding(encoding);
            }
            
            /* construct Writer using correct encoding */
            _writer = _connection.getPrintWriter(encoding);
        }                    
        _outputState=WRITER;
        return _writer;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#setCharacterEncoding(java.lang.String)
     */
    public void setCharacterEncoding(String encoding)
    {
        if (this._outputState==0 && !isCommitted())
        {
            _explicitEncoding=true;
                 
            if (encoding==null)
            {
                // Clear any encoding.
                if (_characterEncoding!=null)
                {
                    _characterEncoding=null;
                    _connection.getResponseFields().put(HttpHeaders.CONTENT_TYPE_BUFFER,_mimeType);
                }
            }
            else
            {
                // No, so just add this one to the mimetype
                _characterEncoding=encoding;
                if (_mimeType!=null)
                {
                    _connection.getResponseFields().put(HttpHeaders.CONTENT_TYPE_BUFFER,_mimeType+";charset="+
                        	QuotedStringTokenizer.quote(_characterEncoding,";= "));
                }
            }
        }
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#setContentLength(int)
     */
    public void setContentLength(int len)
    {
        // Protect from setting after committed as default handling
        // of a servlet HEAD request ALWAYS sets _content length, even
        // if the getHandling committed the response!
        if (!isCommitted())
            _connection.getResponseFields().putLongField(HttpHeaders.CONTENT_LENGTH, len);
    }
    
    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#setContentLength(int)
     */
    public void setLongContentLength(long len)
    {
        // Protect from setting after committed as default handling
        // of a servlet HEAD request ALWAYS sets _content length, even
        // if the getHandling committed the response!
        if (!isCommitted())
            _connection.getResponseFields().putLongField(HttpHeaders.CONTENT_LENGTH, len);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#setContentType(java.lang.String)
     */
    public void setContentType(String typeAndMime)
    {
        if (isCommitted())
            return;
        
        if (typeAndMime==null)
        {
            if (_locale==null)
                _characterEncoding=null;
            _mimeType=null;
            _contentType=null;
            _connection.getResponseFields().remove(HttpHeaders.CONTENT_TYPE_BUFFER);
        }
        else
        {
            // Look for encoding in contentType
            int i0=typeAndMime.indexOf(';');
            
            if (i0>0)
            {
                // Strip params off mimetype
                _mimeType=typeAndMime.substring(0,i0).trim();

                // Look for charset
                int i1=typeAndMime.indexOf("charset=",i0);
                if (i1>=0)
                {
                    i1+=8;
                    int i2 = typeAndMime.indexOf(' ',i1);
                    _characterEncoding = (0<i2)
                        ? typeAndMime.substring(i1,i2)
                        : typeAndMime.substring(i1);
                    _characterEncoding = QuotedStringTokenizer.unquote(_characterEncoding);
                    _contentType=typeAndMime;
                }
                else // No encoding in the params.
                {
                     if (_characterEncoding!=null)
                         // Add any previously set encoding.
                         _contentType=typeAndMime+";charset="+QuotedStringTokenizer.quote(_characterEncoding,";= ");
                }
            }
            else // No encoding and no other params
            {
                _mimeType=typeAndMime;
               
                // Add any previously set encoding.
                if (_characterEncoding!=null)
                    _contentType=typeAndMime+";charset="+QuotedStringTokenizer.quote(_characterEncoding,";= ");
                else
                    _contentType=_mimeType;
            }
            
            _connection.getResponseFields().put(HttpHeaders.CONTENT_TYPE_BUFFER,MimeTypes.CACHE.lookup(_contentType)); 
        }
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#setBufferSize(int)
     */
    public void setBufferSize(int size)
    {
        _connection.getGenerator().increaseContentBufferSize(size);
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#getBufferSize()
     */
    public int getBufferSize()
    {
        return _connection.getGenerator().getContentBufferSize();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#flushBuffer()
     */
    public void flushBuffer() throws IOException
    {
        _connection.flushResponse();
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#reset()
     */
    public void reset()
    {
        resetBuffer();
        
        _status=200;
        _reason=null;
        _mimeType=_contentType=_characterEncoding=null;
        _explicitEncoding=false;
        _locale=null;
        _outputState=NONE;
        _writer=null; 
    }
    
    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#resetBuffer()
     */
    public void resetBuffer()
    {
        if (isCommitted())
            throw new IllegalStateException("Committed");
        // TODO implement
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#isCommitted()
     */
    public boolean isCommitted()
    {
        return _connection.isResponseCommitted();
    }


    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#setLocale(java.util.Locale)
     */
    public void setLocale(Locale locale)
    {
        if (locale == null || isCommitted())
            return; 

        _locale = locale;
        _connection.getResponseFields().put(HttpHeaders.CONTENT_TYPE_BUFFER,locale.toString().replace('_','-'));
                  
        if (this._outputState!=0 )
            return; 
        
        /* get current MIME type from Content-Type header */                  
        String type=getContentType();
        if (type==null)
        {
            // servlet did not set Content-Type yet
            // so lets assume default one
            type="application/octet-stream";
        }
        
        String charset = _connection.getRequest().getContext().getContextHandler().getLocaleEncoding(locale);
        if (charset != null && charset.length()>0)
        {
            int semi=type.indexOf(';');
            if (semi<0)
                type += "; charset="+charset;
            else if (!_explicitEncoding)
                type = type.substring(0,semi)+"; charset="+charset;
            
            _connection.getResponseFields().put(HttpHeaders.CONTENT_TYPE_BUFFER,type);
        }
        
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#getLocale()
     */
    public Locale getLocale()
    {
        if (_locale==null)
            return Locale.getDefault();
        return _locale;
    }

    /* ------------------------------------------------------------ */
    /**
     * @return
     */
    public int getStatus()
    {
        return _status;
    }
    
    /* ------------------------------------------------------------ */
    /**
     * @return
     */
    public String getReason()
    {
        return _reason;
    }
    

    
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    /* ------------------------------------------------------------ */
    private static class NullOutput extends ServletOutputStream
    {
        public void write(int b) throws IOException
        {
        }
    }



    /* ------------------------------------------------------------ */
    /**
     * 
     */
    public void complete()
    	throws IOException
    {	
        _connection.completeResponse();
    }



}
