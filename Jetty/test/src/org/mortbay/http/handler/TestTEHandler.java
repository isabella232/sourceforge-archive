// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.http.handler;

import java.io.IOException;

import org.mortbay.http.HttpException;
import org.mortbay.http.HttpFields;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;

/* ------------------------------------------------------------ */
/** Handler to test TE transfer encoding.
 * If 'gzip' or 'deflate' is in the query string, the
 * response is given that transfer encoding
 *
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class TestTEHandler extends AbstractHttpHandler
{
    /* ------------------------------------------------------------ */
    public void handle(String pathInContext,
                       String pathParams,
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
