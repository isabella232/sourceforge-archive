// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP;


// =======================================================================
public interface ExceptionHandler 
{
    /* ----------------------------------------------------------------- */
    /** Abstract handler of handler Exceptions. 
     * @param request The HTTP request that was handled
     * @param response The HTTP response. The headersWritten() method needs
     * to be checked to determine if headers were sent before the exception.
     * @param exception Any exception thrown by another HttpHandler.
     */
    abstract public void handle(HttpRequest request,
				HttpResponse response,
				Exception exception)
	throws Exception;
}
	    




