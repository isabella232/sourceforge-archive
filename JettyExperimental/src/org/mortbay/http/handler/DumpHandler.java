// ========================================================================
// $Id$
// Copyright 2004 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================

package org.mortbay.http.handler;

import java.io.IOException;

import org.mortbay.http.HttpHeaderValues;
import org.mortbay.http.HttpHeaders;
import org.mortbay.http.bio.HttpHandler;
import org.mortbay.http.bio.HttpInputStream;
import org.mortbay.http.bio.HttpOutputStream;
import org.mortbay.http.bio.HttpRequest;
import org.mortbay.http.bio.HttpResponse;


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
