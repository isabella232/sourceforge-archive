// ===========================================================================
// Copyright (c) 1996 Mort Bay Consulting Pty. Ltd. All rights reserved.
// $Id$
// ---------------------------------------------------------------------------

package com.mortbay.HTTP;
import com.mortbay.Util.*;
import java.io.*;
import java.util.*;
import java.text.*;


// ====================================================================
public class HttpMessage 
{
    public final static String ContentType = "Content-Type";
    public final static String TransferEncoding="Transfer-Encoding";  
    public final static String Chunked = "chunked"; 
    public final static String ContentLength = "Content-Length";
    public final static String WwwFormUrlEncode = "application/x-www-form-urlencoded";
    public final static String WwwAuthenticate = "WWW-Authenticate"; 
    public final static String Authorization = "Authorization"; 
    public final static String Host = "Host";  
    public final static String Date = "Date"; 
    public final static String Cookie = "Cookie";
    public final static String SetCookie = "Set-Cookie";
    public final static String Connection = "Connection";
    public final static String Close = "close";
    public final static String Referer="Referer";
    public final static String Expires="Expires";
    public final static String UserAgent="User-Agent";
    public final static String IfModifiedSince="If-Modified-Since";
    public final static String IfUnmodifiedSince="If-Unmodified-Since";
    public final static String LastModified="Last-Modified";

    public final static String HTTP_1_0 ="HTTP/1.0"   ;
    public final static String HTTP_1_1 ="HTTP/1.1"   ;
    
    /* ------------------------------------------------------------ */
    /** Destroy the header.
     * Help the garbage collector by null everything that we can.
     */
    public void destroy()
    {
    }
}














