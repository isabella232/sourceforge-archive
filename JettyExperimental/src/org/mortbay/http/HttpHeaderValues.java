/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 17-Apr-2003
 * $Id$
 * ============================================== */

package org.mortbay.http;

import org.mortbay.io.Buffer;
import org.mortbay.io.BufferCache;

/**
 * 
 * @version $Revision$
 * @author gregw
 */
public class HttpHeaderValues extends BufferCache
{
    public final static String
        CLOSE="close",
        CHUNKED="chunked",
        FORM_ENCODED="application/x-www-form-urlencoded",
        GZIP="gzip",
        IDENTITY="identity",
        KEEP_ALIVE="keep-alive",
        MULTIPART_BYTERANGES="multipart/byteranges",
        TEXT_HTML="text/html",
        CONTINUE="100-continue",
        
        CLIENT0="ISO-8859-1,utf-8;q=0.7, *;q=0.7",
        CLIENT1="video/x-mng,image/png,image/jpeg,image/gif;q=0.2,*/*;q=0.1",
        CLIENT2="300";

    private static int index=1;
    public final static int
        CLOSE_ORDINAL=index++,
        CHUNKED_ORDINAL=index++,
        FORM_ENCODED_ORDINAL=index++,
        GZIP_ORDINAL=index++,
        IDENTITY_ORDINAL=index++,
        KEEP_ALIVE_ORDINAL=index++,
        MULTIPART_BYTERANGES_ORDINAL=index++,
        TEXT_HTML_ORDINAL=index++,
        CONTINUE_ORDINAL=index++;

    public final static HttpHeaderValues CACHE= new HttpHeaderValues();

    public final static Buffer 
        CLOSE_BUFFER=CACHE.add(CLOSE,CLOSE_ORDINAL),
        CHUNKED_BUFFER=CACHE.add(CHUNKED,CHUNKED_ORDINAL),
        FORM_ENCODED_BUFFER=CACHE.add(FORM_ENCODED,FORM_ENCODED_ORDINAL),
        GZIP_BUFFER=CACHE.add(GZIP,GZIP_ORDINAL),
        IDENTITY_BUFFER=CACHE.add(IDENTITY,IDENTITY_ORDINAL),
        KEEP_ALIVE_BUFFER=CACHE.add(KEEP_ALIVE,KEEP_ALIVE_ORDINAL),
        TEXT_HTML_BUFFER=CACHE.add(TEXT_HTML,TEXT_HTML_ORDINAL),
        CONTINUE_BUFFER=CACHE.add(CONTINUE, CONTINUE_ORDINAL),
        MULTIPART_BYTERANGES_BUFFER=CACHE.add(MULTIPART_BYTERANGES,MULTIPART_BYTERANGES_ORDINAL);
        
    static
    {  
        index=100;
        CACHE.add(CLIENT0,index++);
        CACHE.add(CLIENT1,index++);
        CACHE.add(CLIENT2,index++);
    }
}
