// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP.Handler;

import com.mortbay.HTTP.ChunkableOutputStream;
import com.mortbay.HTTP.HttpException;
import com.mortbay.HTTP.HttpFields;
import com.mortbay.HTTP.HttpRequest;
import com.mortbay.HTTP.HttpResponse;
import com.mortbay.Util.Code;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

/* ------------------------------------------------------------ */
/** Handler for resources that were not found.
 * Implements OPTIONS and TRACE methods for the server.
 * @see
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class NotFoundHandler extends NullHandler
{
    /* ------------------------------------------------------------ */
    public void handle(String contextPath,
                       String pathInContext,
                       HttpRequest request,
                       HttpResponse response)
        throws HttpException, IOException
    {
        if (!isStarted())
            return;

        Code.debug("Not Found");

        // Not found GET request
        String method=request.getMethod();
        if (method.equals(HttpRequest.__GET))
        {
            response.sendError(response.__404_Not_Found,
                               "Could not find resource for "+
                               request.getPath());
        }

        // Not found special requests.
        else if (method.equals(HttpRequest.__HEAD)   ||
                 method.equals(HttpRequest.__POST)   ||
                 method.equals(HttpRequest.__PUT)    ||
                 method.equals(HttpRequest.__DELETE) ||
                 method.equals(HttpRequest.__MOVE)   )
        {
            response.sendError(response.__404_Not_Found);
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
                response.sendError(response.__200_OK);
            }
            else
                response.sendError(response.__404_Not_Found);
        }

        
        else if (method.equals(HttpRequest.__TRACE))
        {
            // 9.8
            // Handle TRACE by returning request header
            response.setField(HttpFields.__ContentType,
                              HttpFields.__MessageHttp);
            ChunkableOutputStream out = response.getOutputStream();
            ByteArrayOutputStream buf = new ByteArrayOutputStream(2048);
            Writer writer = new OutputStreamWriter(buf,"ISO-8859-1");
            writer.write(request.toString());
            writer.flush();
            response.setIntField(HttpFields.__ContentLength,buf.size());
            buf.writeTo(out);
            request.setHandled(true);
        }

        
        else
        {
            // Unknown METHOD
            response.setField(HttpFields.__Allow,
                              "GET, HEAD, POST, PUT, DELETE, MOVE, OPTIONS, TRACE");
            response.sendError(response.__200_OK);
            response.sendError(response.__405_Method_Not_Allowed);
        }
    }
}
