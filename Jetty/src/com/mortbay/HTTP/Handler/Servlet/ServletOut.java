// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP.Handler.Servlet;

import com.sun.java.util.collections.*;
import com.mortbay.HTTP.*;
import com.mortbay.Util.*;
import java.io.*;
import java.util.*;
import javax.servlet.http.*;
import javax.servlet.*;


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
