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
        add(CONTENT_LENGTH,__CONTENT_LENGTH);
        add(CONNECTION,__CONNECTION);
        add(DATE,__DATE);
        add(PRAGMA,__PRAGMA);
        add(TRAILER,__TRAILER);
        add(TRANSFER_ENCODING,__TRANSFER_ENCODING);
        add(UPGRADE,__UPGRADE);
        add(VIA,__VIA);
        add(WARNING,__WARNING);
        add(ALLOW,__ALLOW);
        add(CONTENT_ENCODING,__CONTENT_ENCODING);
        add(CONTENT_LANGUAGE,__CONTENT_LANGUAGE);
        add(CONTENT_LENGTH,__CONTENT_LENGTH);
        add(CONTENT_LOCATION,__CONTENT_LOCATION);
        add(CONTENT_MD5,__CONTENT_MD5);
        add(CONTENT_RANGE,__CONTENT_RANGE);
        add(CONTENT_TYPE,__CONTENT_TYPE);
        add(EXPIRES,__EXPIRES);
        add(LAST_MODIFIED,__LAST_MODIFIED);
        add(ACCEPT,__ACCEPT);
        add(ACCEPT_CHARSET,__ACCEPT_CHARSET);
        add(ACCEPT_ENCODING,__ACCEPT_ENCODING);
        add(ACCEPT_LANGUAGE,__ACCEPT_LANGUAGE);
        add(AUTHORIZATION,__AUTHORIZATION);
        add(EXPECT,__EXPECT);
        add(FORWARDED,__FORWARDED);
        add(FROM,__FROM);
        add(HOST,__HOST);
        add(IF_MATCH,__IF_MATCH);
        add(IF_MODIFIED_SINCE,__IF_MODIFIED_SINCE);
        add(IF_NONE_MATCH,__IF_NONE_MATCH);
        add(IF_RANGE,__IF_RANGE);
        add(IF_UNMODIFIED_SINCE,__IF_UNMODIFIED_SINCE);
        add(KEEP_ALIVE,__KEEP_ALIVE);
        add(MAX_FORWARDS,__MAX_FORWARDS);
        add(PROXY_AUTHORIZATION,__PROXY_AUTHORIZATION);
        add(RANGE,__RANGE);
        add(REQUEST_RANGE,__REQUEST_RANGE);
        add(REFERER,__REFERER);
        add(TE,__TE);
        add(USER_AGENT,__USER_AGENT);
        add(X_FORWARDED_FOR,__X_FORWARDED_FOR);
        add(ACCEPT_RANGES,__ACCEPT_RANGES);
        add(AGE,__AGE);
        add(ETAG,__ETAG);
        add(LOCATION,__LOCATION);
        add(PROXY_AUTHENTICATE,__PROXY_AUTHENTICATE);
        add(RETRY_AFTER,__RETRY_AFTER);
        add(SERVER,__SERVER);
        add(SERVLET_ENGINE,__SERVLET_ENGINE);
        add(VARY,__VARY);
        add(WWW_AUTHENTICATE,__WWW_AUTHENTICATE);
        add(COOKIE,__COOKIE);
        add(SET_COOKIE,__SET_COOKIE);
        add(SET_COOKIE2,__SET_COOKIE2);
        add(MIME_VERSION,__MIME_VERSION);
        add(IDENTITY,__IDENTITY);
    }

}
