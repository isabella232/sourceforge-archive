/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 18/02/2004
 * $Id$
 * ============================================== */
 
package org.mortbay.http.server;

import org.mortbay.http.HttpHeader;
import org.mortbay.http.HttpOutputStream;
import org.mortbay.io.Portable;

/* ------------------------------------------------------------------------------- */
/** 
 * 
 * @version $Revision$
 * @author gregw
 */
public class HttpResponse extends HttpHeader
{
    HttpOutputStream _out;
    HttpRequest _request;

    /* ------------------------------------------------------------------------------- */
    /** Constructor.
     */
    public HttpResponse(HttpRequest request)
    {
        _request=request;
        _request._response=this;
    }

    /* ------------------------------------------------------------------------------- */
    public void destroy()
    {
        super.destroy();
        if (_request!=null)
            _request.destroy();
        _request=null;
        if (_out!=null)
            _out.destroy();
        _out=null;
    }

    /* ------------------------------------------------------------------------------- */
    /** getOutputStream.
     * @return
     */
    public HttpOutputStream getHttpOutputStream()
    {
        return _out;
    }

    /* ------------------------------------------------------------------------------- */
    public HttpRequest getHttpRequest()
    {
        return _request;
    }

    /* ------------------------------------------------------------------------------- */
    /** set_out.
     * @param stream
     */
    void setHttpOutputStream(HttpOutputStream out)
    {
        if (out.getHttpHeader()!=this)
            Portable.throwIllegalArgument("Not stream for this response");
        _out= out;
    }
}
