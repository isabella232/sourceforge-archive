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

package org.mortbay.http;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/* ------------------------------------------------------------ */
/** HttpResponse.
 * @author gregw
 *
 */
public class HttpResponse implements HttpServletResponse
{

    /* ------------------------------------------------------------ */
    /**
     * 
     */
    public HttpResponse()
    {
        super();
        // TODO Auto-generated constructor stub
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#addCookie(javax.servlet.http.Cookie)
     */
    public void addCookie(Cookie cookie)
    {
        // TODO Auto-generated method stub

    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#containsHeader(java.lang.String)
     */
    public boolean containsHeader(String name)
    {
        // TODO Auto-generated method stub
        return false;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#encodeURL(java.lang.String)
     */
    public String encodeURL(String url)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#encodeRedirectURL(java.lang.String)
     */
    public String encodeRedirectURL(String url)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#encodeUrl(java.lang.String)
     */
    public String encodeUrl(String url)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#encodeRedirectUrl(java.lang.String)
     */
    public String encodeRedirectUrl(String url)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#sendError(int, java.lang.String)
     */
    public void sendError(int sc, String msg) throws IOException
    {
        // TODO Auto-generated method stub

    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#sendError(int)
     */
    public void sendError(int sc) throws IOException
    {
        // TODO Auto-generated method stub

    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#sendRedirect(java.lang.String)
     */
    public void sendRedirect(String location) throws IOException
    {
        // TODO Auto-generated method stub

    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#setDateHeader(java.lang.String, long)
     */
    public void setDateHeader(String name, long date)
    {
        // TODO Auto-generated method stub

    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#addDateHeader(java.lang.String, long)
     */
    public void addDateHeader(String name, long date)
    {
        // TODO Auto-generated method stub

    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#setHeader(java.lang.String, java.lang.String)
     */
    public void setHeader(String name, String value)
    {
        // TODO Auto-generated method stub

    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#addHeader(java.lang.String, java.lang.String)
     */
    public void addHeader(String name, String value)
    {
        // TODO Auto-generated method stub

    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#setIntHeader(java.lang.String, int)
     */
    public void setIntHeader(String name, int value)
    {
        // TODO Auto-generated method stub

    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#addIntHeader(java.lang.String, int)
     */
    public void addIntHeader(String name, int value)
    {
        // TODO Auto-generated method stub

    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#setStatus(int)
     */
    public void setStatus(int sc)
    {
        // TODO Auto-generated method stub

    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.http.HttpServletResponse#setStatus(int, java.lang.String)
     */
    public void setStatus(int sc, String sm)
    {
        // TODO Auto-generated method stub

    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#getCharacterEncoding()
     */
    public String getCharacterEncoding()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#getContentType()
     */
    public String getContentType()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#getOutputStream()
     */
    public ServletOutputStream getOutputStream() throws IOException
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#getWriter()
     */
    public PrintWriter getWriter() throws IOException
    {
        // TODO Auto-generated method stub
        return null;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#setCharacterEncoding(java.lang.String)
     */
    public void setCharacterEncoding(String charset)
    {
        // TODO Auto-generated method stub

    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#setContentLength(int)
     */
    public void setContentLength(int len)
    {
        // TODO Auto-generated method stub

    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#setContentType(java.lang.String)
     */
    public void setContentType(String type)
    {
        // TODO Auto-generated method stub

    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#setBufferSize(int)
     */
    public void setBufferSize(int size)
    {
        // TODO Auto-generated method stub

    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#getBufferSize()
     */
    public int getBufferSize()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#flushBuffer()
     */
    public void flushBuffer() throws IOException
    {
        // TODO Auto-generated method stub

    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#resetBuffer()
     */
    public void resetBuffer()
    {
        // TODO Auto-generated method stub

    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#isCommitted()
     */
    public boolean isCommitted()
    {
        // TODO Auto-generated method stub
        return false;
    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#reset()
     */
    public void reset()
    {
        // TODO Auto-generated method stub

    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#setLocale(java.util.Locale)
     */
    public void setLocale(Locale loc)
    {
        // TODO Auto-generated method stub

    }

    /* ------------------------------------------------------------ */
    /* 
     * @see javax.servlet.ServletResponse#getLocale()
     */
    public Locale getLocale()
    {
        // TODO Auto-generated method stub
        return null;
    }


    /* ------------------------------------------------------------ */
    public void sendContent(Object content)
    {
        // TODO
    }
}
