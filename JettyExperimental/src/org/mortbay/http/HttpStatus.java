/* ==============================================
 * Copyright 2003 Mort Bay Consulting Pty Ltd. All rights reserved.
 * Distributed under the artistic license.
 * Created on 17-Apr-2003
 * $Id$
 * ============================================== */

package org.mortbay.http;

import org.mortbay.io.BufferCache;

/* ------------------------------------------------------------------------------- */
/**
 * 
 */
public class HttpStatus extends BufferCache
{
    public final static String Continue= "Continue",
        Switching_Protocols= "Switching Protocols",
        Processing= "Processing",
        OK= "OK",
        Created= "Created",
        Accepted= "Accepted",
        Non_Authoritative_Information= "Non Authoritative Information",
        No_Content= "No Content",
        Reset_Content= "Reset Content",
        Partial_Content= "Partial Content",
        Multi_Status= "Multi Status",
        Multiple_Choices= "Multiple Choices",
        Moved_Permanently= "Moved Permanently",
        Moved_Temporarily= "Moved Temporarily",
        Found= "Found",
        See_Other= "See Other",
        Not_Modified= "Not Modified",
        Use_Proxy= "Use Proxy",
        Bad_Request= "Bad Request",
        Unauthorized= "Unauthorized",
        Payment_Required= "Payment Required",
        Forbidden= "Forbidden",
        Not_Found= "Not Found",
        Method_Not_Allowed= "Method Not Allowed",
        Not_Acceptable= "Not Acceptable",
        Proxy_Authentication_Required= "Proxy Authentication Required",
        Request_Timeout= "Request Timeout",
        Conflict= "Conflict",
        Gone= "Gone",
        Length_Required= "Length Required",
        Precondition_Failed= "Precondition Failed",
        Request_Entity_Too_Large= "Request Entity Too Large",
        Request_URI_Too_Large= "Request URI Too Large",
        Unsupported_Media_Type= "Unsupported Media Type",
        Requested_Range_Not_Satisfiable= "Requested Range Not Satisfiable",
        Expectation_Failed= "Expectation Failed",
        Unprocessable_Entity= "Unprocessable Entity",
        Locked= "Locked",
        Failed_Dependency= "Failed Dependency",
        Internal_Server_Error= "Internal Server Error",
        Not_Implemented= "Not Implemented",
        Bad_Gateway= "Bad Gateway",
        Service_Unavailable= "Service Unavailable",
        Gateway_Timeout= "Gateway Timeout",
        HTTP_Version_Not_Supported= "HTTP Version Not Supported",
        Insufficient_Storage= "Insufficient Storage",
        Unknown="Unknown";

    public final static int __100_Continue= 100,
        __101_Switching_Protocols= 101,
        __102_Processing= 102,
        __200_OK= 200,
        __201_Created= 201,
        __202_Accepted= 202,
        __203_Non_Authoritative_Information= 203,
        __204_No_Content= 204,
        __205_Reset_Content= 205,
        __206_Partial_Content= 206,
        __207_Multi_Status= 207,
        __300_Multiple_Choices= 300,
        __301_Moved_Permanently= 301,
        __302_Moved_Temporarily= 302,
        __302_Found= 302,
        __303_See_Other= 303,
        __304_Not_Modified= 304,
        __305_Use_Proxy= 305,
        __400_Bad_Request= 400,
        __401_Unauthorized= 401,
        __402_Payment_Required= 402,
        __403_Forbidden= 403,
        __404_Not_Found= 404,
        __405_Method_Not_Allowed= 405,
        __406_Not_Acceptable= 406,
        __407_Proxy_Authentication_Required= 407,
        __408_Request_Timeout= 408,
        __409_Conflict= 409,
        __410_Gone= 410,
        __411_Length_Required= 411,
        __412_Precondition_Failed= 412,
        __413_Request_Entity_Too_Large= 413,
        __414_Request_URI_Too_Large= 414,
        __415_Unsupported_Media_Type= 415,
        __416_Requested_Range_Not_Satisfiable= 416,
        __417_Expectation_Failed= 417,
        __422_Unprocessable_Entity= 422,
        __423_Locked= 423,
        __424_Failed_Dependency= 424,
        __500_Internal_Server_Error= 500,
        __501_Not_Implemented= 501,
        __502_Bad_Gateway= 502,
        __503_Service_Unavailable= 503,
        __504_Gateway_Timeout= 504,
        __505_HTTP_Version_Not_Supported= 505,
        __507_Insufficient_Storage= 507,
        __999_Unknown = 999;

    public static final HttpStatus CACHE = new HttpStatus();
    
    private HttpStatus()
    {
        add(Continue, __100_Continue);
        add(Switching_Protocols, __101_Switching_Protocols);
        add(Processing, __102_Processing);
        add(OK, __200_OK);
        add(Created, __201_Created);
        add(Accepted, __202_Accepted);
        add(Non_Authoritative_Information, __203_Non_Authoritative_Information);
        add(No_Content, __204_No_Content);
        add(Reset_Content, __205_Reset_Content);
        add(Partial_Content, __206_Partial_Content);
        add(Multi_Status, __207_Multi_Status);
        add(Multiple_Choices, __300_Multiple_Choices);
        add(Moved_Permanently, __301_Moved_Permanently);
        add(Moved_Temporarily, __302_Moved_Temporarily);
        add(Found, __302_Found);
        add(See_Other, __303_See_Other);
        add(Not_Modified, __304_Not_Modified);
        add(Use_Proxy, __305_Use_Proxy);
        add(Bad_Request, __400_Bad_Request);
        add(Unauthorized, __401_Unauthorized);
        add(Payment_Required, __402_Payment_Required);
        add(Forbidden, __403_Forbidden);
        add(Not_Found, __404_Not_Found);
        add(Method_Not_Allowed, __405_Method_Not_Allowed);
        add(Not_Acceptable, __406_Not_Acceptable);
        add(Proxy_Authentication_Required, __407_Proxy_Authentication_Required);
        add(Request_Timeout, __408_Request_Timeout);
        add(Conflict, __409_Conflict);
        add(Gone, __410_Gone);
        add(Length_Required, __411_Length_Required);
        add(Precondition_Failed, __412_Precondition_Failed);
        add(Request_Entity_Too_Large, __413_Request_Entity_Too_Large);
        add(Request_URI_Too_Large, __414_Request_URI_Too_Large);
        add(Unsupported_Media_Type, __415_Unsupported_Media_Type);
        add(Requested_Range_Not_Satisfiable, __416_Requested_Range_Not_Satisfiable);
        add(Expectation_Failed, __417_Expectation_Failed);
        add(Unprocessable_Entity, __422_Unprocessable_Entity);
        add(Locked, __423_Locked);
        add(Failed_Dependency, __424_Failed_Dependency);
        add(Internal_Server_Error, __500_Internal_Server_Error);
        add(Not_Implemented, __501_Not_Implemented);
        add(Bad_Gateway, __502_Bad_Gateway);
        add(Service_Unavailable, __503_Service_Unavailable);
        add(Gateway_Timeout, __504_Gateway_Timeout);
        add(HTTP_Version_Not_Supported, __505_HTTP_Version_Not_Supported);
        add(Insufficient_Storage, __507_Insufficient_Storage);
        add(Unknown,__999_Unknown);
    }
}
