// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;
import java.io.IOException;


public class HttpException extends IOException
{
    int _code;

    public HttpException()
    {
        super("Unknown");
        _code=400;
    }
    
    public HttpException(int code)
    {
        super("Unknown");
        _code=code;
    }
    
    public HttpException(int code, String message)
    {
        super(message);
        _code=code;
    }
    
};
