/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 18/02/2004
 * $Id$
 * ============================================== */
 
package org.mortbay.http.handler;

import java.io.IOException;

import org.mortbay.http.HttpHeaderValues;
import org.mortbay.http.HttpHeaders;
import org.mortbay.http.HttpInputStream;
import org.mortbay.http.HttpOutputStream;
import org.mortbay.http.server.HttpHandler;
import org.mortbay.http.server.HttpRequest;
import org.mortbay.http.server.HttpResponse;


public class DumpHandler implements HttpHandler

{
    public void handle(HttpRequest request, HttpResponse response)
        throws IOException
    {
        StringBuffer content= new StringBuffer();
        byte data[]= new byte[4096];
        int length= 0;
        int len;
        HttpInputStream in = request.getHttpInputStream();
        while ((len= in.read(data, 0, 4096)) > 0)
        {
            length += len;
            if (len > 0)
                content.append(new String(data, 0, len));
        }

        response.setStatus(200);
        response.put(HttpHeaders.CONTENT_TYPE_BUFFER, HttpHeaderValues.TEXT_HTML_BUFFER);
        response.put(HttpHeaders.CONNECTION_BUFFER, request.get(HttpHeaders.CONNECTION_BUFFER));
        HttpOutputStream out=response.getHttpOutputStream();
        out.write("<html><h1>Test Server</h1>".getBytes());
        out.write(("<p>Request content read = " + length + "</p>").getBytes());
        out.write("<h3>Request:</h3><pre>".getBytes());
        out.write(request.toString().getBytes());
        out.write("</pre>".getBytes());
        out.write(("<form method=\"POST\" action=\"" + request.getUri() + "\">").getBytes());
        out.write("<textarea name=\"text\">Test input</textarea>".getBytes());
        out.write("<br/><input type=\"Submit\"></form>\n".getBytes());

        out.write("<h3>Content:</h3><pre>".getBytes());
        out.write(content.toString().getBytes());
        out.write("</pre></html>\n".getBytes());

    }
}