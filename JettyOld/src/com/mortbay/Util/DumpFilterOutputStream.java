// ========================================================================
// Copyright (c) 1999 Mort Bay Consulting (Australia) Pty. Ltd.
// $Id$
// ========================================================================

package com.mortbay.Util;
import java.io.*;
import com.mortbay.Base.Code;

/* ------------------------------------------------------------ */
/** Filtered OutputStream that summarized throughput on stderr
 * <p>
 *
 * @see filterOutputStream
 * @version 1.0 Tue Jan 26 1999
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

};







