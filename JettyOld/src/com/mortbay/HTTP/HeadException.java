// ========================================================================
// Copyright (c) 1998 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;

import java.io.IOException;

public class HeadException extends IOException
{
    private HeadException()
    {
        super("HEAD Exception");
    }

    public static final HeadException instance = new HeadException();
};
