// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP.Handler;

import com.mortbay.HTTP.HttpException;
import com.mortbay.HTTP.HttpFields;
import com.mortbay.HTTP.HttpRequest;
import com.mortbay.HTTP.HttpResponse;
import java.io.IOException;

/* ------------------------------------------------------------ */
/** Handler to test TE transfer encoding.
 * If 'gzip' or 'deflate' is in the query string, the
 * response is given that transfer encoding
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class TestTEHandler extends NullHandler
{
    /* ------------------------------------------------------------ */
    public void handle(String pathInContext,
                       HttpRequest request,
                       HttpResponse response)
        throws HttpException, IOException
    {
        // For testing set transfer encodings
        if (request.getQuery()!=null)
        {
            if (request.getQuery().indexOf("gzip")>=0)
            {
                response.setField(HttpFields.__TransferEncoding,"gzip");
                response.addField(HttpFields.__TransferEncoding,"chunked");
            }
            if (request.getQuery().indexOf("deflate")>=0)
            {
                response.setField(HttpFields.__TransferEncoding,"deflate");
                response.addField(HttpFields.__TransferEncoding,"chunked");
            }
        }
    }
}
