// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;
import java.io.*;

/* ------------------------------------------------------------ */
/** Filtered OutputStream that summarized throughput on stderr
 * <p>
 *
 * @see filterOutputStream
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class DumpFilterOutputStream extends SummaryFilterOutputStream
{
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param out 
     */
    public DumpFilterOutputStream(OutputStream out)
    {
        super(out,null,0);
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor. 
     * @param out 
     */
    public DumpFilterOutputStream(OutputStream out,
                                  String name)
    {
        super(out,name,0);
    }

}







