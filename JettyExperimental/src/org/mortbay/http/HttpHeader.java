/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 17-Apr-2003
 * $Id$
 * ============================================== */

package org.mortbay.http;

import org.mortbay.util.io.BufferCache;

/* ------------------------------------------------------------------------------- */
/** 
 * 
 * @version $Revision$
 * @author gregw
 */
public class HttpHeader extends BufferCache
{
    /* ------------------------------------------------------------ */
    /** General Fields.
     */
    public final static String
        CONNECTION="Connection",
        DATE="Date",
        PRAGMA="Pragma",
        TRAILER="Trailer",
        TRANSFER_ENCODING="Transfer-Encoding",
        UPGRADE="Upgrade",
        VIA="Via",
        WARNING="Warning";

    /* ------------------------------------------------------------ */
    /** Entity Fields.
     */
    public final static String 
        ALLOW="Allow",
        CONTENT_ENCODING="Content-Encoding",
        CONTENT_LANGUAGE="Content-Language",
        CONTENT_LENGTH="Content-Length",
        CONTENT_LOCATION="Content-Location",
        CONTENT_MD5="Content-MD5",
        CONTENT_RANGE="Content-Range",
        CONTENT_TYPE="Content-Type",
        EXPIRES="Expires",
        LAST_MODIFIED="Last-Modified";

    /* ------------------------------------------------------------ */
    /** Request Fields.
     */
    public final static String
        ACCEPT="Accept",
        ACCEPT_CHARSET="Accept-Charset",
        ACCEPT_ENCODING="Accept-Encoding",
        ACCEPT_LANGUAGE="Accept-Language",
        AUTHORIZATION="Authorization",
        EXPECT="Expect",
        FORWARDED="Forwarded",
        FROM="From",
        HOST="Host",
        IF_MATCH="If-Match",
        IF_MODIFIED_SINCE="If-Modified-Since",
        IF_NONE_MATCH="If-None-Match",
        IF_RANGE="If-Range",
        IF_UNMODIFIED_SINCE="If-Unmodified-Since",
        KEEP_ALIVE="keep-alive",
        MAX_FORWARDS="Max-Forwards",
        PROXY_AUTHORIZATION="Proxy-Authorization",
        RANGE="Range",
        REQUEST_RANGE="Request-Range",
        REFERER="Referer",
        TE="TE",
        USER_AGENT="User-Agent",
        X_FORWARDED_FOR="X-Forwarded-For";

    /* ------------------------------------------------------------ */
    /** Response Fields.
     */
    public final static String
        ACCEPT_RANGES="Accept-Ranges",
        AGE="Age",
        ETAG="ETag",
        LOCATION="Location",
        PROXY_AUTHENTICATE="Proxy-Authenticate",
        RETRY_AFTER="Retry-After",
        SERVER="Server",
        SERVLET_ENGINE="Servlet-Engine",
        VARY="Vary",
        WWW_AUTHENTICATE="WWW-Authenticate";

    /* ------------------------------------------------------------ */
    /** Other Fields.
     */
    public final static String
        COOKIE="Cookie",
        SET_COOKIE="Set-Cookie",
        SET_COOKIE2="Set-Cookie2",
        MIME_VERSION="MIME-Version",
        IDENTITY="identity";
    
    public final static int
        __CONNECTION=1,
        __DATE=2,
        __PRAGMA=3,
        __TRAILER=4,
        __TRANSFER_ENCODING=5,
        __UPGRADE=6,
        __VIA=7,
        __WARNING=8,
        __ALLOW=9,
        __CONTENT_ENCODING=10,
        __CONTENT_LANGUAGE=11,
        __CONTENT_LENGTH=12,
        __CONTENT_LOCATION=13,
        __CONTENT_MD5=14,
        __CONTENT_RANGE=15,
        __CONTENT_TYPE=16,
        __EXPIRES=17,
        __LAST_MODIFIED=18,
        __ACCEPT=19,
        __ACCEPT_CHARSET=20,
        __ACCEPT_ENCODING=21,
        __ACCEPT_LANGUAGE=22,
        __AUTHORIZATION=23,
        __EXPECT=24,
        __FORWARDED=25,
        __FROM=26,
        __HOST=27,
        __IF_MATCH=28,
        __IF_MODIFIED_SINCE=29,
        __IF_NONE_MATCH=30,
        __IF_RANGE=31,
        __IF_UNMODIFIED_SINCE=32,
        __KEEP_ALIVE=33,
        __MAX_FORWARDS=34,
        __PROXY_AUTHORIZATION=35,
        __RANGE=36,
        __REQUEST_RANGE=37,
        __REFERER=38,
        __TE=39,
        __USER_AGENT=40,
        __X_FORWARDED_FOR=41,
        __ACCEPT_RANGES=42,
        __AGE=43,
        __ETAG=44,
        __LOCATION=45,
        __PROXY_AUTHENTICATE=46,
        __RETRY_AFTER=47,
        __SERVER=48,
        __SERVLET_ENGINE=49,
        __VARY=50,
        __WWW_AUTHENTICATE=51,
        __COOKIE=52,
        __SET_COOKIE=53,
        __SET_COOKIE2=54,
        __MIME_VERSION=55,
        __IDENTITY=56;

    public final static HttpHeader CACHE=new HttpHeader();

    private HttpHeader()
    {
        add(__CONTENT_LENGTH, CONTENT_LENGTH);
        add(__CONNECTION,CONNECTION);
        add(__DATE,DATE);
        add(__PRAGMA,PRAGMA);
        add(__TRAILER,TRAILER);
        add(__TRANSFER_ENCODING,TRANSFER_ENCODING);
        add(__UPGRADE,UPGRADE);
        add(__VIA,VIA);
        add(__WARNING,WARNING);
        add(__ALLOW,ALLOW);
        add(__CONTENT_ENCODING,CONTENT_ENCODING);
        add(__CONTENT_LANGUAGE,CONTENT_LANGUAGE);
        add(__CONTENT_LENGTH,CONTENT_LENGTH);
        add(__CONTENT_LOCATION,CONTENT_LOCATION);
        add(__CONTENT_MD5,CONTENT_MD5);
        add(__CONTENT_RANGE,CONTENT_RANGE);
        add(__CONTENT_TYPE,CONTENT_TYPE);
        add(__EXPIRES,EXPIRES);
        add(__LAST_MODIFIED,LAST_MODIFIED);
        add(__ACCEPT,ACCEPT);
        add(__ACCEPT_CHARSET,ACCEPT_CHARSET);
        add(__ACCEPT_ENCODING,ACCEPT_ENCODING);
        add(__ACCEPT_LANGUAGE,ACCEPT_LANGUAGE);
        add(__AUTHORIZATION,AUTHORIZATION);
        add(__EXPECT,EXPECT);
        add(__FORWARDED,FORWARDED);
        add(__FROM,FROM);
        add(__HOST,HOST);
        add(__IF_MATCH,IF_MATCH);
        add(__IF_MODIFIED_SINCE,IF_MODIFIED_SINCE);
        add(__IF_NONE_MATCH,IF_NONE_MATCH);
        add(__IF_RANGE,IF_RANGE);
        add(__IF_UNMODIFIED_SINCE,IF_UNMODIFIED_SINCE);
        add(__KEEP_ALIVE,KEEP_ALIVE);
        add(__MAX_FORWARDS,MAX_FORWARDS);
        add(__PROXY_AUTHORIZATION,PROXY_AUTHORIZATION);
        add(__RANGE,RANGE);
        add(__REQUEST_RANGE,REQUEST_RANGE);
        add(__REFERER,REFERER);
        add(__TE,TE);
        add(__USER_AGENT,USER_AGENT);
        add(__X_FORWARDED_FOR,X_FORWARDED_FOR);
        add(__ACCEPT_RANGES,ACCEPT_RANGES);
        add(__AGE,AGE);
        add(__ETAG,ETAG);
        add(__LOCATION,LOCATION);
        add(__PROXY_AUTHENTICATE,PROXY_AUTHENTICATE);
        add(__RETRY_AFTER,RETRY_AFTER);
        add(__SERVER,SERVER);
        add(__SERVLET_ENGINE,SERVLET_ENGINE);
        add(__VARY,VARY);
        add(__WWW_AUTHENTICATE,WWW_AUTHENTICATE);
        add(__COOKIE,COOKIE);
        add(__SET_COOKIE,SET_COOKIE);
        add(__SET_COOKIE2,SET_COOKIE2);
        add(__MIME_VERSION,MIME_VERSION);
        add(__IDENTITY,IDENTITY);
    }

}
