/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 18/02/2004
 * $Id$
 * ============================================== */
 
package org.mortbay.http.bio;

import java.io.IOException;


/** A temporary interface for extending the HttpConnection handling */
public interface HttpHandler
{
    public void handle(HttpRequest request, HttpResponse response)
        throws IOException;
}