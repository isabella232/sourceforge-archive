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
public class MultiPartResponse implements Runnable
{
    /* ------------------------------------------------------------ */
    private static final String boundary =
    "com.mortbay.HTTP.MutliPartResponse.boundary";
    
    /* ------------------------------------------------------------ */
    ServletResponse response=null;
    Thread servlet=null;
    InputStream in=null;
    OutputStream outputStream = null;

    /* ------------------------------------------------------------ */    
    /** PrintWriter to write content too
     */
    public Writer out = null; 

    /* ------------------------------------------------------------ */
    /** MultiPartResponse contructor
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
    /** MultiPartResponse contructor
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
	response.setContentType("multipart/mixed;boundary="+boundary);
	if (alwaysExpire)
	    response.setHeader("Expires","1 Jan 1971");

	out.write("--"+boundary+HttpHeader.CRLF);
	out.flush();

	servlet = Thread.currentThread();

	if (HttpHeader.HTTP_1_0.equals(request.getProtocol()))
	    new Thread(this).start();
	else
	    Code.warning("Don't know how to terminate HTTP/1.1 requests");
    }
    
    /* ------------------------------------------------------------ */
    public void run()
    {
	try{
	    while (in.read()!=-1);
	}
	catch(Exception e){
	    Code.debug("MultiPartResponse monitor input got ",e);
	}
	finally{
	    if (servlet!=null && servlet.isAlive())
	    {
		Code.debug("Stop the servlet");
		servlet.stop();
	    }
	}
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
    public void endPart()
	 throws IOException
    {
	endPart(false);
    }
    
    /* ------------------------------------------------------------ */
    public void endLastPart()
	 throws IOException
    {
	endPart(true);
    }
    
    /* ------------------------------------------------------------ */
    public void endPart(boolean lastPart)
	 throws IOException
    {
	out.write(HttpHeader.CRLF+"--"+
		  boundary+(lastPart?"--":"")+
		  HttpHeader.CRLF);
	out.flush();
    }
    
};




