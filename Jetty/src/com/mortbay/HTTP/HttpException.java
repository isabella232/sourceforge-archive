// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;
import java.io.IOException;


public class HttpException extends IOException
{
    private int _code;

    public int getCode()
    {
        return _code;
    }
    
    public String getReason()
    {
        return (String)HttpResponse.__statusMsg.get(new Integer(_code));
    }
    
    public HttpException()
    {
        _code=HttpResponse.__400_Bad_Request ;
    }
    
    public HttpException(int code)
    {
        _code=code;
    }
    
    public HttpException(int code, String message)
    {
        super(message);
        _code=code;
    }

    public String toString()
    {
        String message=getMessage();
        String reason=getReason();
        return "HttpException("+_code+","+reason+","+message+")";
    }
}

