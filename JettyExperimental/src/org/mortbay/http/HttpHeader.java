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
        CONNECTION_INDEX=1,
        DATE_INDEX=2,
        PRAGMA_INDEX=3,
        TRAILER_INDEX=4,
        TRANSFER_ENCODING_INDEX=5,
        UPGRADE_INDEX=6,
        VIA_INDEX=7,
        WARNING_INDEX=8,
        ALLOW_INDEX=9,
        CONTENT_ENCODING_INDEX=10,
        CONTENT_LANGUAGE_INDEX=11,
        CONTENT_LENGTH_INDEX=12,
        CONTENT_LOCATION_INDEX=13,
        CONTENT_MD5_INDEX=14,
        CONTENT_RANGE_INDEX=15,
        CONTENT_TYPE_INDEX=16,
        EXPIRES_INDEX=17,
        LAST_MODIFIED_INDEX=18,
        ACCEPT_INDEX=19,
        ACCEPT_CHARSET_INDEX=20,
        ACCEPT_ENCODING_INDEX=21,
        ACCEPT_LANGUAGE_INDEX=22,
        AUTHORIZATION_INDEX=23,
        EXPECT_INDEX=24,
        FORWARDED_INDEX=25,
        FROM_INDEX=26,
        HOST_INDEX=27,
        IF_MATCH_INDEX=28,
        IF_MODIFIED_SINCE_INDEX=29,
        IF_NONE_MATCH_INDEX=30,
        IF_RANGE_INDEX=31,
        IF_UNMODIFIED_SINCE_INDEX=32,
        KEEP_ALIVE_INDEX=33,
        MAX_FORWARDS_INDEX=34,
        PROXY_AUTHORIZATION_INDEX=35,
        RANGE_INDEX=36,
        REQUEST_RANGE_INDEX=37,
        REFERER_INDEX=38,
        TE_INDEX=39,
        USER_AGENT_INDEX=40,
        X_FORWARDED_FOR_INDEX=41,
        ACCEPT_RANGES_INDEX=42,
        AGE_INDEX=43,
        ETAG_INDEX=44,
        LOCATION_INDEX=45,
        PROXY_AUTHENTICATE_INDEX=46,
        RETRY_AFTER_INDEX=47,
        SERVER_INDEX=48,
        SERVLET_ENGINE_INDEX=49,
        VARY_INDEX=50,
        WWW_AUTHENTICATE_INDEX=51,
        COOKIE_INDEX=52,
        SET_COOKIE_INDEX=53,
        SET_COOKIE2_INDEX=54,
        MIME_VERSION_INDEX=55,
        IDENTITY_INDEX=56;

    public final static HttpHeader CACHE=new HttpHeader();

    private HttpHeader()
    {
        add(CONTENT_LENGTH, CONTENT_LENGTH_INDEX);
        add(CONNECTION,CONNECTION_INDEX);
        add(DATE,DATE_INDEX);
        add(PRAGMA,PRAGMA_INDEX);
        add(TRAILER,TRAILER_INDEX);
        add(TRANSFER_ENCODING,TRANSFER_ENCODING_INDEX);
        add(UPGRADE,UPGRADE_INDEX);
        add(VIA,VIA_INDEX);
        add(WARNING,WARNING_INDEX);
        add(ALLOW,ALLOW_INDEX);
        add(CONTENT_ENCODING,CONTENT_ENCODING_INDEX);
        add(CONTENT_LANGUAGE,CONTENT_LANGUAGE_INDEX);
        add(CONTENT_LENGTH,CONTENT_LENGTH_INDEX);
        add(CONTENT_LOCATION,CONTENT_LOCATION_INDEX);
        add(CONTENT_MD5,CONTENT_MD5_INDEX);
        add(CONTENT_RANGE,CONTENT_RANGE_INDEX);
        add(CONTENT_TYPE,CONTENT_TYPE_INDEX);
        add(EXPIRES,EXPIRES_INDEX);
        add(LAST_MODIFIED,LAST_MODIFIED_INDEX);
        add(ACCEPT,ACCEPT_INDEX);
        add(ACCEPT_CHARSET,ACCEPT_CHARSET_INDEX);
        add(ACCEPT_ENCODING,ACCEPT_ENCODING_INDEX);
        add(ACCEPT_LANGUAGE,ACCEPT_LANGUAGE_INDEX);
        add(AUTHORIZATION,AUTHORIZATION_INDEX);
        add(EXPECT,EXPECT_INDEX);
        add(FORWARDED,FORWARDED_INDEX);
        add(FROM,FROM_INDEX);
        add(HOST,HOST_INDEX);
        add(IF_MATCH,IF_MATCH_INDEX);
        add(IF_MODIFIED_SINCE,IF_MODIFIED_SINCE_INDEX);
        add(IF_NONE_MATCH,IF_NONE_MATCH_INDEX);
        add(IF_RANGE,IF_RANGE_INDEX);
        add(IF_UNMODIFIED_SINCE,IF_UNMODIFIED_SINCE_INDEX);
        add(KEEP_ALIVE,KEEP_ALIVE_INDEX);
        add(MAX_FORWARDS,MAX_FORWARDS_INDEX);
        add(PROXY_AUTHORIZATION,PROXY_AUTHORIZATION_INDEX);
        add(RANGE,RANGE_INDEX);
        add(REQUEST_RANGE,REQUEST_RANGE_INDEX);
        add(REFERER,REFERER_INDEX);
        add(TE,TE_INDEX);
        add(USER_AGENT,USER_AGENT_INDEX);
        add(X_FORWARDED_FOR,X_FORWARDED_FOR_INDEX);
        add(ACCEPT_RANGES,ACCEPT_RANGES_INDEX);
        add(AGE,AGE_INDEX);
        add(ETAG,ETAG_INDEX);
        add(LOCATION,LOCATION_INDEX);
        add(PROXY_AUTHENTICATE,PROXY_AUTHENTICATE_INDEX);
        add(RETRY_AFTER,RETRY_AFTER_INDEX);
        add(SERVER,SERVER_INDEX);
        add(SERVLET_ENGINE,SERVLET_ENGINE_INDEX);
        add(VARY,VARY_INDEX);
        add(WWW_AUTHENTICATE,WWW_AUTHENTICATE_INDEX);
        add(COOKIE,COOKIE_INDEX);
        add(SET_COOKIE,SET_COOKIE_INDEX);
        add(SET_COOKIE2,SET_COOKIE2_INDEX);
        add(MIME_VERSION,MIME_VERSION_INDEX);
        add(IDENTITY,IDENTITY_INDEX);
    }

}
