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
        CONTENT_LENGTH="Content-Length",
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
    
    private static int __index=1
    public final static int
        CONTENT_LENGTH_INDEX=__index++,
        CONNECTION_INDEX=__index++,
        DATE_INDEX=__index++,
        PRAGMA_INDEX=__index++,
        TRAILER_INDEX=__index++,
        TRANSFER_ENCODING_INDEX=__index++,
        UPGRADE_INDEX=__index++,
        VIA_INDEX=__index++,
        WARNING_INDEX=__index++,
        ALLOW_INDEX=__index++,
        CONTENT_ENCODING_INDEX=__index++,
        CONTENT_LANGUAGE_INDEX=__index++,
        CONTENT_LENGTH_INDEX=__index++,
        CONTENT_LOCATION_INDEX=__index++,
        CONTENT_MD5_INDEX=__index++,
        CONTENT_RANGE_INDEX=__index++,
        CONTENT_TYPE_INDEX=__index++,
        EXPIRES_INDEX=__index++,
        LAST_MODIFIED_INDEX=__index++,
        ACCEPT_INDEX=__index++,
        ACCEPT_CHARSET_INDEX=__index++,
        ACCEPT_ENCODING_INDEX=__index++,
        ACCEPT_LANGUAGE_INDEX=__index++,
        AUTHORIZATION_INDEX=__index++,
        EXPECT_INDEX=__index++,
        FORWARDED_INDEX=__index++,
        FROM_INDEX=__index++,
        HOST_INDEX=__index++,
        IF_MATCH_INDEX=__index++,
        IF_MODIFIED_SINCE_INDEX=__index++,
        IF_NONE_MATCH_INDEX=__index++,
        IF_RANGE_INDEX=__index++,
        IF_UNMODIFIED_SINCE_INDEX=__index++,
        KEEP_ALIVE_INDEX=__index++,
        MAX_FORWARDS_INDEX=__index++,
        PROXY_AUTHORIZATION_INDEX=__index++,
        RANGE_INDEX=__index++,
        REQUEST_RANGE_INDEX=__index++,
        REFERER_INDEX=__index++,
        TE_INDEX=__index++,
        USER_AGENT_INDEX=__index++,
        X_FORWARDED_FOR_INDEX=__index++,
        ACCEPT_RANGES_INDEX=__index++,
        AGE_INDEX=__index++,
        ETAG_INDEX=__index++,
        LOCATION_INDEX=__index++,
        PROXY_AUTHENTICATE_INDEX=__index++,
        RETRY_AFTER_INDEX=__index++,
        SERVER_INDEX=__index++,
        SERVLET_ENGINE_INDEX=__index++,
        VARY_INDEX=__index++,
        WWW_AUTHENTICATE_INDEX=__index++,
        COOKIE_INDEX=__index++,
        SET_COOKIE_INDEX=__index++,
        SET_COOKIE2_INDEX=__index++,
        MIME_VERSION_INDEX=__index++,
        IDENTITY_INDEX=__index++;

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
