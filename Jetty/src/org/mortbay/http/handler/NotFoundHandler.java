// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http.handler;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpFields;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;

/* ------------------------------------------------------------ */
/** Handler for resources that were not found.
 * Implements OPTIONS and TRACE methods for the server.
 * 
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class NotFoundHandler extends AbstractHttpHandler
{
    private static Log log = LogFactory.getLog(NotFoundHandler.class);

    /* ------------------------------------------------------------ */
    public void handle(String pathInContext,
                       String pathParams,
                       HttpRequest request,
                       HttpResponse response)
        throws HttpException, IOException
    {
        log.debug("Not Found");
        String method=request.getMethod();
        
        // Not found  requests.
        if (method.equals(HttpRequest.__GET)    ||
            method.equals(HttpRequest.__HEAD)   ||
            method.equals(HttpRequest.__POST)   ||
            method.equals(HttpRequest.__PUT)    ||
            method.equals(HttpRequest.__DELETE) ||
            method.equals(HttpRequest.__MOVE)   )
        {
            response.sendError(HttpResponse.__404_Not_Found,
                               request.getPath()+" Not Found");
        }
        
        else if (method.equals(HttpRequest.__OPTIONS))
        {
            // Handle OPTIONS request for entire server
            if ("*".equals(request.getPath()))
            {
                // 9.2
                response.setIntField(HttpFields.__ContentLength,0);
                response.setField(HttpFields.__Allow,
                                  "GET, HEAD, POST, PUT, DELETE, MOVE, OPTIONS, TRACE");
                response.commit();
            }
            else
                response.sendError(HttpResponse.__404_Not_Found);
        }
        else if (method.equals(HttpRequest.__TRACE))
        {
            handleTrace(request,response);
        }
        else
        {
            // Unknown METHOD
            response.setField(HttpFields.__Allow,
                              "GET, HEAD, POST, PUT, DELETE, MOVE, OPTIONS, TRACE");
            response.sendError(HttpResponse.__405_Method_Not_Allowed);
        }
    }
}
