// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP.Handler.Servlet;

import com.mortbay.HTTP.ChunkableOutputStream;
import java.io.IOException;
import javax.servlet.ServletOutputStream;


class ServletOut extends ServletOutputStream
{
    ChunkableOutputStream _out;

    /* ------------------------------------------------------------ */
    ServletOut(ChunkableOutputStream out)
    {
        _out=out;
    }

    /* ------------------------------------------------------------ */
    public void write(int ch)
        throws IOException
    {
        _out.write(ch);
    }
}
