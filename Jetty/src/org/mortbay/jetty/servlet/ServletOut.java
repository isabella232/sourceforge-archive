// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.servlet;

import java.io.IOException;
import java.io.OutputStream;
import org.mortbay.util.IO;
import javax.servlet.ServletOutputStream;


class ServletOut extends ServletOutputStream
{
    OutputStream _out;

    /* ------------------------------------------------------------ */
    ServletOut(OutputStream out)
    {
        _out=out;
    }

    /* ------------------------------------------------------------ */
    public void write(int ch)
        throws IOException
    {
        _out.write(ch);
    }
    
    /* ------------------------------------------------------------ */
    public void write(byte[]b)
        throws IOException
    {
        _out.write(b);
    }
    
    /* ------------------------------------------------------------ */
    public void write(byte[]b,int o,int l)
        throws IOException
    {
        _out.write(b,o,l);
    }

    /* ------------------------------------------------------------ */
    public void flush()
        throws IOException
    {
        _out.flush();
    }
    
    /* ------------------------------------------------------------ */
    public void close()
        throws IOException
    {
        _out.close();
    }
    
    /* ------------------------------------------------------------ */
    public void disable()
        throws IOException
    {
        _out=IO.getNullStream();
    }
}
