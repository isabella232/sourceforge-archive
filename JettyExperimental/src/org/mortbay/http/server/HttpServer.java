/* ==============================================
* Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
* Distributed under the artistic license.
* Created on 17-Apr-2003
* $Id$
* ============================================== */

package org.mortbay.http.server;

import java.io.IOException;

import org.mortbay.http.server.HttpHandler;
import org.mortbay.http.server.HttpRequest;
import org.mortbay.http.server.HttpResponse;
import org.mortbay.http.server.SocketListener;

/**
 * Temporary Servler class to get things running.
 */
public class HttpServer  implements HttpHandler
{
    public SocketListener listener;
    
    public HttpServer()
    {
        listener = new SocketListener();
        listener.setHttpServer(this);
    }

    public static void main(String[] args) throws Exception
    {
        HttpServer server = new HttpServer();
        server.listener.setPort(8080);
        server.listener.start();
        server.listener.join();
    }

    public void handle(HttpRequest request, HttpResponse response) throws IOException
    {
    }


}
