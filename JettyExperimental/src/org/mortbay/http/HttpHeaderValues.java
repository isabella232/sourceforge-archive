/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 17-Apr-2003
 * $Id$
 * ============================================== */

package org.mortbay.http;

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
        TEXT_HTML="text/html",
        
        CLIENT0="ISO-8859-1,utf-8;q=0.7, *;q=0.7",
        CLIENT1="video/x-mng,image/png,image/jpeg,image/gif;q=0.2,*/*;q=0.1",
        CLIENT2="300";

    private static int index=1;
    public final static int
        __CLOSE=index++,
        __CHUNKED=index++,
        __FORM_ENCODED=index++,
        __GZIP=index++,
        __IDENTITY=index++,
        __KEEP_ALIVE=index++,
        __TEXT_HTML=index++;

    public final static HttpHeaderValues CACHE= new HttpHeaderValues();

    private HttpHeaderValues()
    {
        add(CLOSE, __CLOSE);
        add(CHUNKED, __CHUNKED);
        add(FORM_ENCODED,__FORM_ENCODED);
        add(GZIP,__GZIP);
        add(IDENTITY, __IDENTITY);
        add(KEEP_ALIVE,__KEEP_ALIVE);
        add(TEXT_HTML,__TEXT_HTML);
        
        index=100;
        add(CLIENT0,index++);
        add(CLIENT1,index++);
        add(CLIENT2,index++);
    }
}
