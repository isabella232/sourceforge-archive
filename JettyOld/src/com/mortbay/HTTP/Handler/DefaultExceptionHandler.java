// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP.Handler;
import com.mortbay.Base.*;
import com.mortbay.HTTP.*;
import java.io.*;


// =======================================================================
/** Default Exception handler
 * This specialization of exception handler adds the exception and
 * its stack trace to the reply for view by the user.
 * It is intended as the last resort exception handler
 */
public class DefaultExceptionHandler implements ExceptionHandler
{
    /* ----------------------------------------------------------------- */
    /** Base handler of handler Exceptions. 
     * @param request The HTTP request that was handled
     * @param response The HTTP response. The headersWritten() method needs
     * to be checked to determine if headers were sent before the exception.
     * @param exception Any exception thrown by another HttpHandler.
     */
    public void handle(HttpRequest request,
		       HttpResponse response,
		       Exception exception)
	 throws Exception
    {
	try{
	    throw exception;
	}
	catch (Exception e)
	{
	    // Send exception response
	    try
	    {
		if (!response.headersWritten())
		    response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
		OutputStream out = response.getOutputStream();
		out.write("<HTML><HEAD><TITLE>Exception</TITLE>".getBytes());
		out.write("<BODY><H2>".getBytes());
		out.write(e.toString().getBytes());
		out.write("</H2><br><PRE>".getBytes());
		e.printStackTrace(new PrintWriter(out));
		out.write("</PRE></BODY></HTML>".getBytes());
		out.flush();
	    }
	    catch (Exception e2)
	    {
		Code.debug("Exception creating error page",e);
	    }
	}
    }    
}
	
