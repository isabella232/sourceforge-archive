// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP;

import com.mortbay.Util.Code;

public interface Connection
{
    public ChunkableInputStream getInputStream();
    public ChunkableOutputStream getOutputStream();
    public String getProtocol();
    public String getHost();
    public int getPort();
};
