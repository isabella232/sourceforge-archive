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



/* ================================================================ */
/** Handle a multipart MIME response
 * <p><h4>Usage</h4>
 * <pre>
 * public class MultiPartCount extends Servlet
 * {
 *     public void init(){}
 *     
 *     public void service(ServletRequest req, ServletResponse res) 
 *        throws Exception 
 *     {
 * 	  MultiPartResponse multi=new MultiPartResponse(res);
 * 	  multi.startNextPart("text/plain");
 * 	  multi.out.write("One\n");
 * 	  multi.endPart();
 * 	  Thread.sleep(2000);
 * 	  multi.startNextPart("text/plain");
 * 	  multi.out.write("Two\n");
 * 	  multi.endPart();
 * 	  Thread.sleep(2000);
 * 	  multi.startNextPart("text/plain");
 * 	  multi.out.write("Three\n");
 * 	  multi.endLastPart();
 *     }
 * }
 *
 * </pre>
 *
 * @version $Id$
 * @author Greg Wilkins
*/
public class MultiPartResponse
{
    /* ------------------------------------------------------------ */
    private String boundary =
    "com.mortbay.HTTP.MultiPartResponse.boundary.";
    
    /* ------------------------------------------------------------ */
    ServletResponse response=null;
    InputStream in=null;
    OutputStream outputStream = null;

    /* ------------------------------------------------------------ */    
    /** PrintWriter to write content too
     */
    public Writer out = null; 

    /* ------------------------------------------------------------ */
    /** MultiPartResponse constructor
     * @param response The ServletResponse to which this multipart
     *                 response will be sent.
     */
    public MultiPartResponse(HttpServletRequest request,
			     HttpServletResponse response)
	 throws IOException
    {
	this(request,response,true);
    }

    /* ------------------------------------------------------------ */
    /** MultiPartResponse constructor
     * @param response The ServletResponse to which this multipart
     *                 response will be sent.
     */
    public MultiPartResponse(HttpServletRequest request,
			     HttpServletResponse response,
			     boolean alwaysExpire)
	 throws IOException
    {
	this.response=response;
	in = request.getInputStream();
	outputStream=response.getOutputStream();
	out=new OutputStreamWriter(response.getOutputStream());

	String ua = request.getHeader(HttpHeader.UserAgent);
	if (ua!=null && ua.indexOf("MSIE")>0)
	    boundary="MSIE.CANNOT.HANDLE.MULTI.PART.MIME.";
	
	boundary+=Long.toString(System.currentTimeMillis(),36);
	response.setContentType("multipart/mixed;boundary="+boundary);
	if (alwaysExpire)
	    response.setHeader("Expires","1 Jan 1971");

	out.write("--"+boundary+HttpHeader.CRLF);
	out.flush();

	if (HttpHeader.HTTP_1_1.equals(request.getProtocol()))
	    response.setHeader(HttpHeader.Connection,HttpHeader.Close);

    }
    

    /* ------------------------------------------------------------ */
    /** Start creation of the next Content
     */
    public void startNextPart(String contentType)
	 throws IOException
    {
	out.write("Content-type: "+contentType+
		  HttpHeader.CRLF+HttpHeader.CRLF);
    }
    
    /* ------------------------------------------------------------ */
    /** End the current part
     * @exception IOException IOException
     */
    public void endPart()
	 throws IOException
    {
	endPart(false);
    }
    
    /* ------------------------------------------------------------ */
    /** End the current part and the whole response.
     * @exception IOException IOException
     */
    public void endLastPart()
	 throws IOException
    {
	endPart(true);
    }
    
    /* ------------------------------------------------------------ */
    /** End the current part
     * @param lastPart True if this is the last part
     * @exception IOException IOException
     */
    public void endPart(boolean lastPart)
	 throws IOException
    {
	out.write(HttpHeader.CRLF+"--"+
		  boundary+(lastPart?"--":"")+
		  HttpHeader.CRLF);
	out.flush();
    }
    
};




