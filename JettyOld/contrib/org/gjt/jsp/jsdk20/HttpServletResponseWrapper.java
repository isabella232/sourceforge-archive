/*
  GNUJSP - a free JSP1.0 implementation

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  as published by the Free Software Foundation; either version 2
  of the License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
*/

package org.gjt.jsp.jsdk20;

import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.gjt.jsp.JspConfig;
import org.gjt.jsp.JspMsg;

/**
 *
 * Defines a Wrapper to an HTTP servlet response for
 * realization of include on jsdk 2.0 platforms.
 *
 */

public class HttpServletResponseWrapper 
    implements HttpServletResponse, JspMsg {

    private PrintWriter writer = null;
    // you may use Writer or ServletOutputStream.
    private int outputType = 0;
    private static int OUT_STREAM = 1;
    private static int OUT_WRITER = 2;
    private ServletOutputStream streamOut = null;

    // FIXME: need to check which attributes may be set and which not

    private HttpServletResponse res = null;

    public HttpServletResponseWrapper(HttpServletResponse res) {
	this.res = res;
    }

    /**
     * Returns the name of the character set encoding used for
     * the MIME body sent by this response.
     *
     * <p>The character encoding is either the one specified in
     * the content itself or another one the client understands.
     * If no character encoding has been assigned, it is implicitly
     * set to <i>text/plain</i>.
     *
     * <p>See RFC 2047 (http://ds.internic.net/rfc/rfc2045.txt)
     * for more information about character encoding and MIME.
     *
     * @return		a <code>String</code> specifying the
     *			name of the character set encoding, for
     *			example, <i>text/plain</i>
     *
     */
  
    public String getCharacterEncoding () {
	return res.getCharacterEncoding();
    }
    

    /**
     * Returns a {@link ServletOutputStream} suitable for writing binary 
     * data in the response. The servlet engine does not encode the
     * binary data.
     *
     * @return				a {@link ServletOutputStream} for writing binary data	
     *
     * @exception IllegalStateException if you have already called the <code>getWriter</code> method
     * 					for the same response
     *
     * @exception IOException 		if an input or output exception occurred
     *
     * @see 				#getWriter
     *
     */

    public ServletOutputStream getOutputStream() throws IOException {
	if(outputType == 0) {
	    outputType = OUT_STREAM;
	    streamOut = 
		new ServletOutputStreamImpl((writer != null) 
					? writer
					: res.getWriter());
	} else if(outputType != OUT_STREAM) {
	    throw new IllegalStateException
		(JspConfig.getLocalizedMsg(ERR_sp10_A_3_already_called_getwriter));
	    
	}
	return streamOut;
    }
    

    /**
     * Returns a <code>PrintWriter</code> object that you
     * can use to send character text to the client. 
     * The character encoding used is the one specified 
     * in the <code>charset=</code> property of the
     * {@link #setContentType} method, which you must call
     * <i>before</i> you call this method. 
     *
     * <p>If necessary, the MIME type of the response is 
     * modified to reflect the character encoding used.
     *
     * <p> You cannot use this method if you have already
     * called {@link #getOutputStream} for this 
     * <code>ServletResponse</code> object.
     *
     * 
     * @return 					a <code>PrintWriter</code> object that 
     *						can return text to the client 
     *
     * @exception UnsupportedEncodingException  if the character encoding specified in
     *						<code>setContentType</code> cannot be
     *						used
     *
     * @exception IllegalStateException    	if the <code>getOutputStream</code>
     * 						method has already been called for this 
     *						response object; in that case, you can't
     *						use this method
     *
     * @exception IOException   		if an input or output exception occurred
     *
     * @see 					#getOutputStream
     * @see 					#setContentType
     *
     */

    public PrintWriter getWriter() throws IOException {
	if(outputType == 0) {
	    outputType = OUT_WRITER;
	} else if(outputType != OUT_WRITER) {
	    throw new IllegalStateException
		(JspConfig.getLocalizedMsg
		 (ERR_sp10_A_3_already_called_getoutputstream));
	}
	if(writer != null) {
	    return writer;
	} else {
	    return res.getWriter();
	}
    }

    /**
     * Sets the length of the content the server returns
     * to the client. In HTTP servlets, this method sets the
     * HTTP Content-Length header.
     *
     *
     * @param len 	an integer specifying the length of the 
     * 			content being returned to the client; sets
     *			the Content-Length header
     *
     */

    public void setContentLength(int len) {
	res.setContentLength(len);
    }
    
    /**
     * Sets the content type of the response the server sends to
     * the client. The content type may include the type of character
     * encoding used, for example, <code>text/html; charset=ISO-8859-4</code>.
     *
     * <p>You can only use this method once, and you should call it
     * before you obtain a <code>PrintWriter</code> or 
     * {@link ServletOutputStream} object to return a response.
     *
     *
     * @param type 	a <code>String</code> specifying the MIME 
     *			type of the content
     *
     * @see 		#getOutputStream
     * @see 		#getWriter
     *
     */

    public void setContentType(String type) {
	res.setContentType(type);
    }

    /**
     * Adds the specified cookie to the response.  It can be called
     * multiple times to set more than one cookie.
     *
     * @param cookie the Cookie to return to the client
     *
     */

    public void addCookie(Cookie cookie) {
	// FIXME: not allowed I think
	throw new IllegalStateException
	    (JspConfig.getLocalizedMsg(ERR_method_addcookie_not_allowed));
    }
    
    /**
     * Checks whether the response message header has a field with
     * the specified name.
     * 
     * @param name the header field name
     * @return true if the response message header has a field with
     * the specified name; false otherwise
     */

	public boolean containsHeader(String name) {
	    return res.containsHeader(name);
	}

    /**
     * Encodes the specified URL by including the session ID in it,
     * or, if encoding is not needed, returns the URL unchanged.
     * The implementation of this method should include the logic to
     * determine whether the session ID needs to be encoded in the URL.
     * For example, if the browser supports cookies, or session
     * tracking is turned off, URL encoding is unnecessary.
     * 
     * <p>All URLs emitted by a Servlet should be run through this
     * method.  Otherwise, URL rewriting cannot be used with browsers
     * which do not support cookies.
     *
     * @param url the url to be encoded.
     * @return the encoded URL if encoding is needed; the unchanged URL
     * otherwise.
     */

	public String encodeURL (String url) {
	    return res.encodeUrl(url);  // FIXME: jsdk2.1 , differente urls
	}

    /**
     * Encodes the specified URL for use in the
     * <code>sendRedirect</code> method or, if encoding is not needed,
     * returns the URL unchanged.  The implementation of this method
     * should include the logic to determine whether the session ID
     * needs to be encoded in the URL.  Because the rules for making
     * this determination differ from those used to decide whether to
     * encode a normal link, this method is seperate from the
     * <code>encodeUrl</code> method.
     * 
     * <p>All URLs sent to the HttpServletResponse.sendRedirect
     * method should be run through this method.  Otherwise, URL
     * rewriting canont be used with browsers which do not support
     * cookies.
     *
     * <p>After this method is called, the response should be considered
     * to be committed and should not be written to.
     *
     * @param url the url to be encoded.
     * @return the encoded URL if encoding is needed; the unchanged URL
     * otherwise.
     *
     * @see #sendRedirect
     * @see #encodeUrl
     */

	public String encodeRedirectURL (String url) {
	    // FIXME: jsdk 2.1, differente urls
	    return res.encodeRedirectUrl(url);
	}

    /**
     * Encodes the specified URL by including the session ID in it,
     * or, if encoding is not needed, returns the URL unchanged.
     * The implementation of this method should include the logic to
     * determine whether the session ID needs to be encoded in the URL.
     * For example, if the browser supports cookies, or session
     * tracking is turned off, URL encoding is unnecessary.
     * 
     * <p>All URLs emitted by a Servlet should be run through this
     * method.  Otherwise, URL rewriting cannot be used with browsers
     * which do not support cookies.
     *
     * @param url the url to be encoded.
     * @return the encoded URL if encoding is needed; the unchanged URL
     * otherwise.
     * @deprecated Use encodeURL(String url)
     */

	public String encodeUrl(String url) {
	    return res.encodeUrl(url); // FIXME: is different url?
	}
    
    /**
     * Encodes the specified URL for use in the
     * <code>sendRedirect</code> method or, if encoding is not needed,
     * returns the URL unchanged.  The implementation of this method
     * should include the logic to determine whether the session ID
     * needs to be encoded in the URL.  Because the rules for making
     * this determination differ from those used to decide whether to
     * encode a normal link, this method is seperate from the
     * <code>encodeUrl</code> method.
     * 
     * <p>All URLs sent to the HttpServletResponse.sendRedirect
     * method should be run through this method.  Otherwise, URL
     * rewriting canont be used with browsers which do not support
     * cookies.
     *
     * @param url the url to be encoded.
     * @return the encoded URL if encoding is needed; the unchanged URL
     * otherwise.       
     * @deprecated Use encodeRedirectURL(String url)
     */

	public String encodeRedirectUrl(String url) {
	    return res.encodeRedirectUrl(url); // FIXME: is different url?
	}

    /**
     * Sends an error response to the client using the specified status
     * code and descriptive message.  If setStatus has previously been
     * called, it is reset to the error status code.  The message is
     * sent as the body of an HTML page, which is returned to the user
     * to describe the problem.  The page is sent with a default HTML
     * header; the message is enclosed in simple body tags
     * (&lt;body&gt;&lt;/body&gt;).
     *
     * <p>After using this method, the response should be considered
     * to be committed and should not be written to.
     *
     * @param sc the status code
     * @param msg the detail message
     * @exception IOException If an I/O error has occurred.  */

    public void sendError(int sc, String msg) throws IOException {
	res.sendError(sc, msg); // FIXME: ok?
    }

    /**
     * Sends an error response to the client using the specified
     * status code and a default message.
     * @param sc the status code
     * @exception IOException If an I/O error has occurred.
     */

    public void sendError(int sc) throws IOException {
	res.sendError(sc); // FIXME: ok?
    }

    /**
     * Sends a temporary redirect response to the client using the
     * specified redirect location URL.  The URL must be absolute (for
     * example, <code><em>https://hostname/path/file.html</em></code>).
     * Relative URLs are not permitted here.
     *
     * @param location the redirect location URL
     * @exception IOException If an I/O error has occurred.
     */

    public void sendRedirect(String location) throws IOException {
	res.sendRedirect(location); // FIXME: ok?
    }
    
    /**
     * 
     * Adds a field to the response header with the given name and
     * date-valued field.  The date is specified in terms of
     * milliseconds since the epoch.  If the date field had already
     * been set, the new value overwrites the previous one.  The
     * <code>containsHeader</code> method can be used to test for the
     * presence of a header before setting its value.
     * 
     * @param name the name of the header field
     * @param value the header field's date value
     * 
     * @see #containsHeader
     */

    public void setDateHeader(String name, long date) {
	res.setDateHeader(name, date); // FIXME: ok?
    }
    
    /**
     *
     * Adds a field to the response header with the given name and value.
     * If the field had already been set, the new value overwrites the
     * previous one.  The <code>containsHeader</code> method can be
     * used to test for the presence of a header before setting its
     * value.
     * 
     * @param name the name of the header field
     * @param value the header field's value
     *
     * @see #containsHeader
     */

    public void setHeader(String name, String value) {
	res.setHeader(name, value); // FIXME: ok?
    }

    /**
     * Adds a field to the response header with the given name and
     * integer value.  If the field had already been set, the new value
     * overwrites the previous one.  The <code>containsHeader</code>
     * method can be used to test for the presence of a header before
     * setting its value.
     *
     * @param name the name of the header field
     * @param value the header field's integer value
     *
     * @see #containsHeader
     */

    public void setIntHeader(String name, int value) {
	res.setIntHeader(name, value); // ok?
    }

    /**
     * Sets the status code for this response.  This method is used to
     * set the return status code when there is no error (for example,
     * for the status codes SC_OK or SC_MOVED_TEMPORARILY).  If there
     * is an error, the <code>sendError</code> method should be used
     * instead.
     *
     * @param sc the status code
     *
     * @see #sendError
     */

    public void setStatus(int sc) {
	res.setStatus(sc); // FIXME: ok?
    }
  
    /**
     * Sets the status code and message for this response.  If the
     * field had already been set, the new value overwrites the
     * previous one.  The message is sent as the body of an HTML
     * page, which is returned to the user to describe the problem.
     * The page is sent with a default HTML header; the message
     * is enclosed in simple body tags (&lt;body&gt;&lt;/body&gt;).
     * 
     * @param sc the status code
     * @param sm the status message
     * @deprecated ambiguous meaning. To send an error with a description
     * page, use sendError(int sc, String msg);
     */

    public void setStatus(int sc, String sm) {
	res.setStatus(sc, sm); // FIXME: ok?
    }
    
    /** this jsdk 2.1 compat code for jsdk 2.0 */
    public void setWriter(PrintWriter writer) {
	this.writer = writer;
    }
}


