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

package org.mortbay.http.bio;

import org.mortbay.http.HttpHeader;
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
