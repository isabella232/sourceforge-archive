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
	// Send exception response
	try
	{
	    Code.warning(exception);
	    if (!response.headersWritten())
		response.setStatus(HttpResponse.SC_INTERNAL_SERVER_ERROR);
	    OutputStream out = response.getOutputStream();
	    PrintWriter pout=new PrintWriter(out);
	    pout.println("<HTML><HEAD><TITLE>Exception</TITLE>");
	    pout.println("<BODY><H2>");
	    pout.println(exception.toString());
	    pout.println("</H2><br><PRE>");
	    exception.printStackTrace(pout);
	    pout.println("</PRE></BODY></HTML>");
	    pout.flush();
	}
	catch (Exception e2)
	{
	    Code.debug("Exception creating error page",exception);
	}
    }    
}
	
