// ========================================================================
// Copyright (c) 2000 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package org.mortbay.jetty.servlet;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import org.mortbay.util.IO;


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
        lock=os;
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
            this.out=IO.getNullStream();
        }
    }
}
