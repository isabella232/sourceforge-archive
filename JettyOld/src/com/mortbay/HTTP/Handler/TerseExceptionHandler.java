package com.mortbay.HTTP.Handler;

import java.io.*;
import com.mortbay.Base.*;
import com.mortbay.HTTP.*;

/**
 * Handle an Exception from a HttpHandler in a more terse fashion
 * than sending a stack trace to the client.
 *
 * Why? Becuase I don't want to send stack traces to clients.
 * 
 * @see DefaultExceptionHandler
 * 
 * @author Brett Sealey
 * @version $Id$
 */
public class TerseExceptionHandler implements ExceptionHandler
{
    /**
     * The function called to handle an exception thrown while handling
     * a request.
     * 
     * @param request - what was being served.
     * @param response - where any response should go
     * @param exception - what went wrong
     * 
     * @throws Exception when it can't send an error back to the client
     * 
     * @see HttpServer
     */
    public void handle(HttpRequest request, HttpResponse response,
                       Exception exception) throws Exception
    {
	if (exception instanceof java.io.IOException)
	    Code.debug(exception);
	else
	    Code.warning(exception);
	
        response.sendError(HttpResponse.SC_INTERNAL_SERVER_ERROR,
                           "Server Error");
    }
}
