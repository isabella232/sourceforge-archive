// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.HTTP.Handler.Servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.io.FilterOutputStream;
import com.mortbay.Util.IO;
import javax.servlet.ServletOutputStream;


/* ------------------------------------------------------------ */
/** Servlet PrintWriter.
 * This writer can be disabled.
 * It is crying out for optimization.
 *
 * @version $Revision$
 * @author Greg Wilkins (gregw)
 */
class ServletWriter extends PrintWriter
{
    Filter filter;
    
    /* ------------------------------------------------------------ */
    ServletWriter(OutputStream os, String encoding)
        throws IOException
    {
        super(IO.getNullWriter());
        filter=new Filter(os);
        out=(new OutputStreamWriter(filter,encoding));
    }

    /* ------------------------------------------------------------ */
    public void disable()
    {
        filter.disable();
    }
    
    /* ------------------------------------------------------------ */
    private static class Filter extends FilterOutputStream
    {
        Filter(OutputStream os)
        {
            super(os);
        }
        void disable()
        {
            out=IO.getNullStream();
        }
    }
}
