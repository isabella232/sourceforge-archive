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
    public final static String CONTENT_LENGTH= "Content-Length",
        CONNECTION= "Connection",
        DATE= "Date",
        Pragma= "Pragma",
        Trailer= "Trailer",
        TransferEncoding= "Transfer-Encoding",
        Upgrade= "Upgrade",
        Via= "Via",
        Warning= "Warning";

    /* ------------------------------------------------------------ */
    /** Entity Fields.
     */
    public final static String 
        __Allow= "Allow",
        __ContentEncoding= "Content-Encoding",
        __ContentLanguage= "Content-Language",
        __ContentLength= "Content-Length",
        __ContentLocation= "Content-Location",
        __ContentMD5= "Content-MD5",
        __ContentRange= "Content-Range",
        __ContentType= "Content-Type",
        __Expires= "Expires",
        __LastModified= "Last-Modified";

    /* ------------------------------------------------------------ */
    /** Request Fields.
     */
    public final static String __Accept= "Accept",
        __AcceptCharset= "Accept-Charset",
        __AcceptEncoding= "Accept-Encoding",
        __AcceptLanguage= "Accept-Language",
        __Authorization= "Authorization",
        __Expect= "Expect",
        __Forwarded= "Forwarded",
        __From= "From",
        __Host= "Host",
        __IfMatch= "If-Match",
        __IfModifiedSince= "If-Modified-Since",
        __IfNoneMatch= "If-None-Match",
        __IfRange= "If-Range",
        __IfUnmodifiedSince= "If-Unmodified-Since",
        __KeepAlive= "keep-alive",
        __MaxForwards= "Max-Forwards",
        __ProxyAuthorization= "Proxy-Authorization",
        __Range= "Range",
        __RequestRange= "Request-Range",
        __Referer= "Referer",
        __TE= "TE",
        __UserAgent= "User-Agent",
        __XForwardedFor= "X-Forwarded-For";

    /* ------------------------------------------------------------ */
    /** Response Fields.
     */
    public final static String __AcceptRanges= "Accept-Ranges",
        __Age= "Age",
        __ETag= "ETag",
        __Location= "Location",
        __ProxyAuthenticate= "Proxy-Authenticate",
        __RetryAfter= "Retry-After",
        __Server= "Server",
        __ServletEngine= "Servlet-Engine",
        __Vary= "Vary",
        __WwwAuthenticate= "WWW-Authenticate";

    /* ------------------------------------------------------------ */
    /** Other Fields.
     */
    public final static String __Cookie= "Cookie",
        __SetCookie= "Set-Cookie",
        __SetCookie2= "Set-Cookie2",
        __MimeVersion= "MIME-Version",
        __Identity= "identity";
    ;

    public final static int CONTENT_LENGTH_INDEX= 1;

    public final static HttpHeader CACHE= new HttpHeader();

    private HttpHeader()
    {
        add(CONTENT_LENGTH, CONTENT_LENGTH_INDEX);
    }

}
