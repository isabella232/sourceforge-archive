// ========================================================================
// Copyright (c) 1997 MortBay Consulting, Sydney
// $Id$
// ========================================================================

package com.mortbay.Util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;


/* ------------------------------------------------------------ */
/** File Log Sink.
 * @deprecated Use WriterLogSink
 * @version $Id$
 * @author Greg Wilkins (gregw)
 */
public class FileLogSink extends WriterLogSink
{
    /*-------------------------------------------------------------------*/
    private String _fileName=null;
    
    /* ------------------------------------------------------------ */
    /** Constructor.
     * @deprecated Use WriterLogSink
     */
    public FileLogSink()
        throws IOException
    {
    	super(System.getProperty("LOG_FILE","log.txt"));
    }
    
    /* ------------------------------------------------------------ */
    /** Constructor.
     * @deprecated Use WriterLogSink
     */
    public FileLogSink(String filename)
        throws IOException
    {
        super(filename);
    }
    
}




